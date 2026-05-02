package de.liebki.myollamaenhancer.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class providing structured output schemas for different generation tasks.
 * These schemas help reduce prompt usage and ensure consistent, reliable output.
 */
public enum StructuredOutputSchemas {
    ;

    /**
     * Creates a schema for code generation tasks.
     *
     * @return JSON schema for code generation
     */
    public static Map<String, Object> createCodeGenerationSchema() {
        final Result result = StructuredOutputSchemas.getResult();

        final Map<String, Object> codeField = new HashMap<>();
        codeField.put("type", "string");
        codeField.put("description", "The generated or enhanced code");
        result.properties().put("code", codeField);

        result.schema().put("type", "object");
        result.schema().put("properties", result.properties());
        result.schema().put("required", new String[]{"code"});

        return result.schema();
    }

    /**
     * Creates a schema for error explanation tasks.
     *
     * @return JSON schema for error explanations
     */
    public static Map<String, Object> createErrorExplanationSchema() {
        final Result result = StructuredOutputSchemas.getResult();

        final Map<String, Object> explanationField = new HashMap<>();
        explanationField.put("type", "string");
        explanationField.put("description", "The error explanation");
        result.properties().put("explanation", explanationField);

        result.schema().put("type", "object");
        result.schema().put("properties", result.properties());
        result.schema().put("required", new String[]{"explanation"});

        return result.schema();
    }

    /**
     * Creates a schema for markdown formatting tasks.
     *
     * @return JSON schema for markdown formatting
     */
    public static Map<String, Object> createMarkdownFormattingSchema() {
        final Result result = StructuredOutputSchemas.getResult();

        final Map<String, Object> formattedField = new HashMap<>();
        formattedField.put("type", "string");
        formattedField.put("description", "The formatted markdown text without code.");
        result.properties().put("formatted", formattedField);

        result.schema().put("type", "object");
        result.schema().put("properties", result.properties());
        result.schema().put("required", new String[]{"formatted"});

        return result.schema();
    }

    /**
     * Creates a schema for regex generation tasks.
     *
     * @return JSON schema for regex generation
     */
    public static Map<String, Object> createRegexGenerationSchema() {
        final Result result = StructuredOutputSchemas.getResult();

        final Map<String, Object> regexField = new HashMap<>();
        regexField.put("type", "string");
        regexField.put("description", "A valid regular expression string for the requested capture, without delimiters or flags.");
        result.properties().put("regex", regexField);

        result.schema().put("type", "object");
        result.schema().put("properties", result.properties());
        result.schema().put("required", new String[]{"regex"});

        return result.schema();
    }

    /**
     * Creates a schema for universal comment generation (for all languages).
     *
     * @return JSON schema for comment generation
     */
    public static Map<String, Object> createUniversalCommentSchema() {
        final Result result = StructuredOutputSchemas.getResult();

        final Map<String, Object> commentTextField = new HashMap<>();
        commentTextField.put("type", "string");
        commentTextField.put("description", "A concise, natural-language description of what the method/function does. No comment syntax, just the text.");
        result.properties().put("comment_text", commentTextField);

        final Map<String, Object> paramDescriptionsField = new HashMap<>();
        paramDescriptionsField.put("type", "array");
        paramDescriptionsField.put("items", Collections.singletonMap("type", "string"));
        paramDescriptionsField.put("description", "Descriptions of each parameter. If there are no parameters, return an empty array [].");
        result.properties().put("param_descriptions", paramDescriptionsField);

        final Map<String, Object> returnValueField = new HashMap<>();
        returnValueField.put("type", "string");
        returnValueField.put("description", "Description of the return value. If there is no return value, return an empty string ''.");
        result.properties().put("return_value", returnValueField);

        result.schema().put("type", "object");
        result.schema().put("properties", result.properties());
        result.schema().put("required", new String[]{"comment_text", "param_descriptions", "return_value"});

        return result.schema();
    }

    /**
     * Creates a schema for file explanation tasks.
     *
     * @return JSON schema for file explanation
     */
    public static Map<String, Object> createExplanationSchema() {
        final Result result = StructuredOutputSchemas.getResult();

        final Map<String, Object> explanationField = new HashMap<>();
        explanationField.put("type", "string");
        explanationField.put("description", "The explanation of the file");
        result.properties().put("explanation", explanationField);

        result.schema().put("type", "object");
        result.schema().put("properties", result.properties());
        result.schema().put("required", new String[]{"explanation"});

        return result.schema();
    }

    private static @NotNull Result getResult() {
        final Map<String, Object> schema = new HashMap<>();
        final Map<String, Object> properties = new HashMap<>();
        return new Result(schema, properties);
    }

    private record Result(Map<String, Object> schema, Map<String, Object> properties) {

    }
} 