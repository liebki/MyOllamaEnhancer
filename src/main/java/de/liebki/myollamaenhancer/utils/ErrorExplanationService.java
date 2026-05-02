package de.liebki.myollamaenhancer.utils;

import com.intellij.openapi.project.Project;
import de.liebki.myollamaenhancer.types.OllamaOption;

import java.util.Map;
import java.util.function.BiConsumer;

public enum ErrorExplanationService {
    ;

    public static void explainError(final Project project, final String stacktrace, final BiConsumer<String, String> onResult) {

        final String sysPromptErrorExplain = StructuredPromptEnhancer.enhanceForErrorExplanation(OllamaOption.ERROR_EXPLAIN_SIMPLE.getPrompt());
        final Map<String, Object> structuredOutput = StructuredOutputSchemas.createErrorExplanationSchema();

        OllamaAPIUtil.generateStructuredOllamaResponse(project, sysPromptErrorExplain, stacktrace, structuredOutput, responseObj -> {
            final String response = responseObj.visibleContent();
            final String extracted = StructuredResponseParser.parseExplanationResponse(response);

            onResult.accept(extracted, response);
        });
    }
} 