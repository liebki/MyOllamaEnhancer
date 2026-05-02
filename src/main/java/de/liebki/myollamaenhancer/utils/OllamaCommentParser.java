package de.liebki.myollamaenhancer.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public enum OllamaCommentParser {
    ;

    public static String extractCommentText(final String ollamaResponse) {
        try {
            final JsonElement jsonElement = JsonParser.parseString(ollamaResponse.trim());
            if (jsonElement.isJsonObject()) {
                final JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject.has("comment_text")) {
                    final JsonElement commentElement = jsonObject.get("comment_text");
                    if (commentElement.isJsonPrimitive()) {
                        return commentElement.getAsString();
                    } else {
                        return commentElement.toString();
                    }
                }
            }
        } catch (final JsonSyntaxException e) {
            // Ignore and fall through
        }
        return "";
    }
} 