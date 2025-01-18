package de.liebki.myollamaenhancer;

import java.util.Arrays;

public enum OllamaOption {

    SIMPLE_CODE("Simple Code","Just enhance this code, do not include anything but the thing that is asked for, this is {0} Code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code."),
    ERROR_EXPLAIN_SIMPLE("Explain Simple","Identify problems in this stack trace, just explain where in the codebase the problem could be located. Do not include any additional comments or explanations, just explain where the error is found and what it is about."),
    ERROR_EXPLAIN_BULLET ("Explain Bullet" ,"Identify problems in this stack trace, just explain with bullet points where in the codebase the problem could be located. Do not include any additional comments or explanations, just explain where the error is found and what it is about."),

    READABILITY("Better Readability", "Look at the following {0} code and enhance it for readability. Make necessary changes and provide only the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code."),
    BUGS("Fix Bugs/Problems", "Look at the following {0} code, identify and fix bugs or problems. Make the necessary changes and provide only the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code."),
    PERFORMANCE("Enhance Performance", "Look at the following {0} code and try to enhance the performance. Make the necessary changes and provide only the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code."),
    UNIT_TESTS("Create Unit Tests", "Look at the following {0} code and create unit tests. Create a comment and provide the unit tests code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code."),
    SIMPLIFY("Simplify", "Look at the following {0} code and make it most simple as possible. Make the necessary changes and provide only the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code."),
    COMMENT_CODE("Add Comment", "Look at this {0} code and generate a short but concise comment which explains the primary purpose. Include only the comment and no additional information."),
    FIX_BROKEN("Fix Code not working", "Look at the following {0} code, identify the problem, why has the code an error. Make the necessary changes and only provide the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code.");

    OllamaOption(String comboOption, String prompt) {
        this.comboOption = comboOption;
        this.prompt = prompt;
    }

    private String comboOption;
    private String prompt;

    public String getComboOption() {
        return comboOption;
    }

    public String getPrompt() {
        return prompt;
    }

    public static OllamaOption getOption(String comboOption) {
        for (OllamaOption ollamaOption : OllamaOption.values()) {
            if (ollamaOption.comboOption.equals(comboOption)) return ollamaOption;
        }
        throw new IllegalArgumentException("Invalid Option");
    }

    public static String[] getArray(){
        return Arrays.stream(OllamaOption.values())
                .filter(opt -> !opt.equals(SIMPLE_CODE) && !opt.equals(ERROR_EXPLAIN_SIMPLE)&& !opt.equals(ERROR_EXPLAIN_BULLET))
                .map(opt -> opt.comboOption)
                .toArray(String[]::new);
    }

}