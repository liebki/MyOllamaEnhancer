package de.liebki.myollamaenhancer.utils;

public enum StructuredPromptEnhancer {
    ;

    public static String enhanceForCodeGeneration(final String originalPrompt) {
        return originalPrompt + "\n\n" +
                "IMPORTANT: You must respond with valid JSON containing only the 'code' field. " +
                "Respond with ONLY the code - no explanations, comments, or markdown formatting.";
    }

    public static String enhanceForCommentGeneration(final String originalPrompt) {
        return originalPrompt + "\n\n" +
                "IMPORTANT: You must respond with valid JSON containing only the 'comment' field. " +
                "Respond with ONLY the comment - no explanations or additional text.";
    }

    public static String enhanceForErrorExplanation(final String originalPrompt) {
        return originalPrompt + "\n\n" +
                "IMPORTANT: You must respond with valid JSON containing only the 'explanation' field. " +
                "Respond with ONLY the explanation - no additional text.";
    }

    public static String enhanceForFileExplanation(final String originalPrompt) {
        return originalPrompt + "\n\n" +
                "IMPORTANT: You must respond with valid JSON containing only the 'explanation' field. " +
                "Your explanation must explicitly address any user-provided 'Extra Informations' in the input (e.g., questions or requested changes). " +
                "Respond with ONLY the explanation - no additional text outside the JSON field.";
    }

    public static String enhanceForMarkdownFormatting(final String originalPrompt) {
        return originalPrompt + "\n\n" +
                "IMPORTANT: You must respond with valid JSON containing only the 'formatted' field. " +
                "Format the text with markdown syntax (## for headers, ** for bold, - for lists). " +
                "Only format existing text - dont add any new content or code, unless explicitly shown in the text..";
    }

    public static String enhanceForRegexGeneration(final String originalPrompt) {
        return originalPrompt + "\n\n" +
                "IMPORTANT: You must respond with valid JSON containing only the 'regex' field. " +
                "Return a single valid regular expression string, without delimiters or flags, that captures exactly what is requested. " +
                "Avoid verbose modes or comments; do not include slashes or code blocks; no explanation.";
    }

    /**
     * Builds a system prompt for code generation for the specified language.
     * The prompt instructs the LLM to generate only the requested code, without wrappers, explanations, or formatting.
     *
     * @param language The programming language for which to generate code
     * @return The system prompt string
     */
    public static String buildCodeGenerationSystemPrompt(final String language) {
        return switch (language) {
            case "Java" ->
                    "You are a Java code generator for inline code generation inside an existing file. Generate clean, well-formatted Java code based on the user's request. IMPORTANT: The generated code will be inserted directly into an existing Java file, so do NOT wrap code in a class or method unless specifically requested. If the user asks for a method, generate only the method (not a class). If the user asks for a class, generate only the class. Do not add package statements, imports, or any extra wrappers unless requested. Return ONLY the code, no explanations or comments unless specifically requested. Do not use ```java or ``` blocks. Do not explain what the code does. Do not add any text before or after the code. Just return the pure Java code.";
            case "Python" ->
                    "You are a Python code generator for inline code generation inside an existing file. Generate clean, well-formatted Python code based on the user's request. IMPORTANT: The generated code will be inserted directly into an existing Python file, so do NOT add module-level code, imports, or wrappers unless specifically requested. If the user asks for a function, generate only the function. If the user asks for a class, generate only the class. Return ONLY the code, no explanations or comments unless specifically requested. Do not use ```python or ``` blocks. Do not explain what the code does. Do not add any text before or after the code. Just return the pure Python code.";
            case "Go" ->
                    "You are a Go code generator for inline code generation inside an existing file. Generate clean, well-formatted Go code based on the user's request. IMPORTANT: The generated code will be inserted directly into an existing Go file, so do NOT add package statements, imports, or wrappers unless specifically requested. If the user asks for a function, generate only the function. If the user asks for a struct, generate only the struct. Return ONLY the code, no explanations or comments unless specifically requested. Do not use ```go or ``` blocks. Do not explain what the code does. Do not add any text before or after the code. Just return the pure Go code.";
            default ->
                    "You are a code generator for inline code generation inside an existing file. Generate clean, well-formatted code based on the user's request. IMPORTANT: The generated code will be inserted directly into an existing file, so do NOT add wrappers, imports, or module-level code unless specifically requested. Return ONLY the code, no explanations or comments unless specifically requested. Do not use code blocks or markdown formatting. Do not explain what the code does. Do not add any text before or after the code. Just return the pure code.";
        };
    }
} 