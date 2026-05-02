package de.liebki.myollamaenhancer.types;

import java.util.Arrays;

public enum OllamaOption {


    ERROR_EXPLAIN_SIMPLE("Explain Simple", "Identify problems in this stack trace, just explain where in the codebase the problem could be located. Do not include any additional comments or explanations, just explain where the error is found and what it is about."),

    READABILITY("Better Readability", "Look at the following {0} code and enhance it for readability. Make necessary changes and provide only the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code."),
    BUGS("Fix Bugs/Problems", "Look at the following {0} code, identify and fix bugs or problems. Make the necessary changes and provide only the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code."),
    PERFORMANCE("Enhance Performance", "Look at the following {0} code and try to enhance the performance. Make the necessary changes and provide only the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code."),
    UNIT_TESTS("Create Unit Tests", "Look at the following {0} code and create unit tests. Create a comment and provide the unit tests code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code."),
    SIMPLIFY("Simplify", "Look at the following {0} code and make it most simple as possible. Make the necessary changes and provide only the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code."),
    COMMENT_CODE("Add Comment", "Look at this {0} code and generate a short but concise comment which explains the primary purpose. Include only the comment and no additional information."),
    FIX_BROKEN("Fix Code not working", "Look at the following {0} code, identify the problem, why has the code an error. Make the necessary changes and only provide the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code.");

    private static final String[] ENUM_VALUE_ARRAY = Arrays.stream(values())
            .filter(opt -> ERROR_EXPLAIN_SIMPLE != opt)
            .map(opt -> opt.comboOption)
            .toArray(String[]::new);
    private final String comboOption;
    private final String prompt;

    OllamaOption(final String comboOption, final String prompt) {
        this.comboOption = comboOption;
        this.prompt = prompt;
    }

    public static OllamaOption getOption(final String comboOption) {
        for (final OllamaOption ollamaOption : values()) {
            if (ollamaOption.comboOption.equals(comboOption)) return ollamaOption;
        }
        throw new IllegalArgumentException("Invalid Option");
    }

    public static String[] getEnumValueArray() {
        return OllamaOption.ENUM_VALUE_ARRAY;
    }

    public String getPrompt() {
        return this.prompt;
    }

}