package de.liebki.myollamaenhancer.utils;

import com.intellij.openapi.project.Project;

import java.util.Map;
import java.util.function.BiConsumer;

public enum CodeEnhancementService {
    ;

    public enum EnhancementType {
        CUSTOM, OPTION, INLINE, COMMENT
    }

    public static void enhanceCode(final Project project, final String code, final EnhancementType type, final String prompt, final String language, final BiConsumer<String, String> onResult) {
        final String sysPrompt;
        final Map<String, Object> structuredOutput;
        switch (type) {
            case CUSTOM -> {
                sysPrompt = StructuredPromptEnhancer.enhanceForCodeGeneration("You are a " + language + " code enhancement assistant. Your task is to " + prompt);
                structuredOutput = StructuredOutputSchemas.createCodeGenerationSchema();
            }
            case OPTION -> {
                sysPrompt = StructuredPromptEnhancer.enhanceForCodeGeneration(prompt);
                structuredOutput = StructuredOutputSchemas.createCodeGenerationSchema();
            }
            case COMMENT -> {
                sysPrompt = StructuredPromptEnhancer.enhanceForCommentGeneration(prompt);
                structuredOutput = StructuredOutputSchemas.createUniversalCommentSchema();
            }
            case INLINE -> {
                sysPrompt = prompt;
                structuredOutput = StructuredOutputSchemas.createCodeGenerationSchema();
            }
            default -> throw new IllegalArgumentException("Unknown enhancement type");
        }
        OllamaAPIUtil.generateStructuredOllamaResponse(project, sysPrompt, code, structuredOutput, response -> {
            final String extracted;
            if (EnhancementType.COMMENT == type) {
                extracted = StructuredResponseParser.parseStructuredResponse(response.visibleContent(), "comment_text");
            } else {
                extracted = StructuredResponseParser.parseCodeResponse(response.visibleContent());
            }
            onResult.accept(extracted, response.visibleContent());
        });
    }
} 