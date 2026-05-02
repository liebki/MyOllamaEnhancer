package de.liebki.myollamaenhancer.commentgenerator;

import de.liebki.myollamaenhancer.utils.MethodInfo;

import java.util.regex.Pattern;

public enum LanguageHandlerUtils {
    ;

    public static int findClosingBrace(final String text, final int openBracePos) {
        int depth = 1;
        for (int i = openBracePos + 1; i < text.length(); i++) {
            final char c = text.charAt(i);
            if ('{' == c) depth++;
            else if ('}' == c) depth--;
            if (0 == depth) return i + 1;
        }

        return text.length();
    }

    /**
     * Shared comment insertion logic for JavaScript and TypeScript languages
     */
    public static String insertJSDocComment(final String fileContent, final MethodInfo method, final String commentText) {
        if (null == commentText || commentText.trim().isEmpty()) {
            return fileContent; // Return unchanged if no comment text
        }
        
        int lineStart = method.startOffset;
        while (0 < lineStart && '\n' != fileContent.charAt(lineStart - 1)) {
            lineStart--;
        }

        // Detect indentation
        int indentEnd = lineStart;
        while (indentEnd < fileContent.length() && (' ' == fileContent.charAt(indentEnd) || '\t' == fileContent.charAt(indentEnd))) {
            indentEnd++;
        }

        final String indent = fileContent.substring(lineStart, indentEnd);
        final StringBuilder indentedComment = new StringBuilder();

        indentedComment.append(indent).append("/**\n");
        for (final String line : commentText.split("\n")) {
            indentedComment.append(indent).append(" * ").append(line).append("\n");
        }

        indentedComment.append(indent).append(" */\n");
        final String before = fileContent.substring(0, lineStart);

        final String after = fileContent.substring(lineStart);
        return before + indentedComment + after;
    }

    /**
     * Shared comment insertion logic for C# XML documentation
     */
    public static String insertCSharpComment(final String fileContent, final MethodInfo method, final String commentText) {
        if (null == commentText || commentText.trim().isEmpty()) {
            return fileContent; // Return unchanged if no comment text
        }
        
        // Find the start of the line for the method declaration
        int lineStart = method.startOffset;
        while (0 < lineStart && '\n' != fileContent.charAt(lineStart - 1)) {
            lineStart--;
        }
        // Detect indentation
        int indentEnd = lineStart;
        while (indentEnd < fileContent.length() && (' ' == fileContent.charAt(indentEnd) || '\t' == fileContent.charAt(indentEnd))) {
            indentEnd++;
        }

        final String indent = fileContent.substring(lineStart, indentEnd);
        final StringBuilder indentedComment = new StringBuilder();

        indentedComment.append(indent).append("/// <summary>\n");
        for (final String line : commentText.split("\n")) {
            indentedComment.append(indent).append("/// ").append(line).append("\n");
        }

        indentedComment.append(indent).append("/// </summary>\n");
        final String before = fileContent.substring(0, lineStart);

        final String after = fileContent.substring(lineStart);
        return before + indentedComment + after;
    }

    public static String insertGoComment(final String fileContent, final MethodInfo method, final String commentText) {
        return LanguageHandlerUtils.insertLineComment(fileContent, method, commentText, "// ");
    }

    public static String insertRustDocComment(final String fileContent, final MethodInfo method, final String commentText) {
        return LanguageHandlerUtils.insertLineComment(fileContent, method, commentText, "/// ");
    }

    /**
     * Shared comment insertion logic for PHPDoc comments (identical to JSDoc)
     */
    public static String insertPHPDocComment(final String fileContent, final MethodInfo method, final String commentText) {
        return LanguageHandlerUtils.insertJSDocComment(fileContent, method, commentText);
    }

    /**
     * Private helper for inserting line comments with a given prefix (used by Go and Rust)
     */
    private static String insertLineComment(final String fileContent, final MethodInfo method, final String commentText, final String prefix) {
        if (null == commentText || commentText.trim().isEmpty()) {
            return fileContent; // Return unchanged if no comment text
        }
        
        int lineStart = method.startOffset;
        while (0 < lineStart && '\n' != fileContent.charAt(lineStart - 1)) {
            lineStart--;
        }
        
        // Detect indentation
        int indentEnd = lineStart;
        while (indentEnd < fileContent.length() && (' ' == fileContent.charAt(indentEnd) || '\t' == fileContent.charAt(indentEnd))) {
            indentEnd++;
        }

        final String indent = fileContent.substring(lineStart, indentEnd);
        final String before = fileContent.substring(0, lineStart);

        final String after = fileContent.substring(lineStart);
        final StringBuilder commentBlock = new StringBuilder();

        for (final String line : commentText.split("\n")) {
            commentBlock.append(indent).append(prefix).append(line).append("\n");
        }

        return before + commentBlock + after;
    }

    public static int updateBraceDepth(final String line, int braceDepth) {
        for (final char c : line.toCharArray()) {
            if ('{' == c) braceDepth++;
            if ('}' == c) braceDepth--;
        }

        return braceDepth;
    }

    /**
     * Shared script tag extraction pattern for frameworks like Svelte, Vue, etc.
     */
    public static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile(
        "<script\\s*(?:setup)?\\s*(?:lang\\s*=\\s*[\"'](ts|js)[\"'])?\\s*>([\\s\\S]*?)</script>",
        Pattern.CASE_INSENSITIVE
    );

}
