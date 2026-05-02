package de.liebki.myollamaenhancer.utils;

import com.intellij.openapi.vfs.VirtualFile;

public enum LanguageUtils {
    ;

    public static String getLanguageFromVirtualFile(final VirtualFile virtualFile) {
        final String fileName = virtualFile.getName();
        String extension = "";

        final int lastDotIndex = fileName.lastIndexOf('.');
        if (0 < lastDotIndex) {
            extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return FileExtensions.languageForExtension(extension);
    }

    /**
     * Gets the comment prefix for a given programming language.
     *
     * @param language The programming language
     * @return The comment prefix (e.g., "// " for Java, "# " for Python)
     */
    public static String getCommentPrefix(final String language) {
        return switch (language) {
            case "Java", "C#", "C++", "C", "JavaScript", "TypeScript", "Go", "Rust", "PHP" -> "// ";
            case "Python", "Ruby", "Shell", "Bash" -> "# ";
            case "SQL" -> "-- ";
            case "HTML", "XML" -> "<!-- ";
            case "CSS", "SCSS", "SASS" -> "/* ";
            default -> throw new IllegalStateException("Unexpected value: " + language);
        };
    }

    /**
     * Gets the comment suffix for a given programming language.
     *
     * @param language The programming language
     * @return The comment suffix (e.g., "" for Java, " -->" for HTML)
     */
    public static String getCommentSuffix(final String language) {
        return switch (language) {
            case "HTML", "XML" -> " -->";
            case "CSS", "SCSS", "SASS" -> " */";
            default -> "";
        };
    }

    /**
     * Formats a comment with the appropriate syntax for the given language.
     *
     * @param comment  The comment text
     * @param language The programming language
     * @return The formatted comment with proper syntax
     */
    public static String formatComment(final String comment, final String language) {
        final String prefix = LanguageUtils.getCommentPrefix(language);
        final String suffix = LanguageUtils.getCommentSuffix(language);
        return prefix + comment + suffix;
    }
} 