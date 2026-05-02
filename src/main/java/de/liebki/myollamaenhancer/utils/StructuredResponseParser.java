package de.liebki.myollamaenhancer.utils;

import com.google.gson.*;

/**
 * Utility class for parsing structured responses from different schemas.
 * Provides fallback mechanisms for when structured output is not properly formatted.
 */
public enum StructuredResponseParser {
    ;

    private static final Gson gson = new Gson();

    /**
     * Parses a structured response to extract the code field.
     *
     * @param response The JSON response from the model
     * @return The extracted code or the original response if parsing fails
     */
    public static String parseCodeResponse(final String response) {
        return StructuredResponseParser.parseStructuredResponse(response, "code");
    }

    /**
     * Parses a structured response to extract the explanation field.
     *
     * @param response The JSON response from the model
     * @return The extracted explanation or the original response if parsing fails
     */
    public static String parseExplanationResponse(final String response) {
        return StructuredResponseParser.parseStructuredResponse(response, "explanation");
    }

    /**
     * Parses a structured response to extract the formatted field.
     *
     * @param response The JSON response from the model
     * @return The extracted formatted content or the original response if parsing fails
     */
    public static String parseFormattedResponse(final String response) {
        return StructuredResponseParser.parseStructuredResponse(response, "formatted");
    }

    /**
     * Generic method to parse structured responses and extract a specific field.
     *
     * @param response  The JSON response from the model
     * @param fieldName The name of the field to extract
     * @return The extracted field value or the original response if parsing fails
     */
    public static String parseStructuredResponse(final String response, final String fieldName) {
        try {
            // Clean the response - remove any leading/trailing whitespace and ensure it's valid JSON
            final String cleanedResponse = response.trim();

            // Try to parse as JSON
            final JsonElement jsonElement = JsonParser.parseString(cleanedResponse);
            if (jsonElement.isJsonObject()) {
                final JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject.has(fieldName)) {
                    final JsonElement fieldElement = jsonObject.get(fieldName);
                    if (fieldElement.isJsonPrimitive()) {
                        return fieldElement.getAsString();
                    } else {
                        // If it's not a primitive, convert to string representation
                        return StructuredResponseParser.gson.toJson(fieldElement);
                    }
                }
            }

            // If no structured format found, return the original response
            return response;

        } catch (final JsonSyntaxException e) {
            // If JSON parsing fails, try to extract using regex as fallback
            return StructuredResponseParser.extractWithRegex(response, fieldName);
        } catch (final Exception e) {
            // If any other parsing fails, return the original response
            return response;
        }
    }

    /**
     * Fallback method using regex to extract field values when JSON parsing fails.
     * This handles cases where the response might be malformed JSON.
     */
    private static String extractWithRegex(final String response, final String fieldName) {
        try {
            // Pattern to match the field: "fieldName": "value" or "fieldName": value
            final String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*(?:\\\\\"[^\"]*)*)\"";
            final java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
            final java.util.regex.Matcher matcher = regex.matcher(response);

            if (matcher.find()) {
                String extractedValue = matcher.group(1);
                // Unescape common JSON escape sequences
                extractedValue = extractedValue.replace("\\n", "\n")
                        .replace("\\t", "\t")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
                return extractedValue;
            }

            return response;
        } catch (final Exception e) {
            return response;
        }
    }
} 