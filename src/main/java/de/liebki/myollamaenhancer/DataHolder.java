package de.liebki.myollamaenhancer;

import com.intellij.ide.util.PropertiesComponent;

public class DataHolder {

    private DataHolder() {
    }

    private static final String[] comboOptions = {
            "Better Readability",
            "Fix Bugs/Problems",
            "Fix Code not working",
            "Enhance Performance",
            "Add Comment"
    };

    public static String[] getComboOptions() {
        return comboOptions;
    }

    private static final String SIMPLE_CODE_PROMPT =
            "Just enhance this code, do not include anything but the thing that is asked for, this is {0} Code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code.";

    private static final String ERROR_EXPLAIN_PROMPT_SIMPLE = "Identify problems in this stack trace, just explain where in the codebase the problem could be located. Do not include any additional comments or explanations, just explain where the error is found and what it is about.";

    private static final String ERROR_EXPLAIN_PROMPT_BULLET = "Identify problems in this stack trace, just explain with bullet points where in the codebase the problem could be located. Do not include any additional comments or explanations, just explain where the error is found and what it is about.";

    private static final String READABILITY_PROMPT = "Lookt at the following {0} code and enhance it for readability. Make necessary changes and provide only the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code.";

    private static final String BUGS_PROMPT = "Look at the following {0} code, identify and fix bugs or problems. Make the necessary changes and provide only the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code.";

    private static final String PERFORMANCE_PROMPT = "Look at the following {0} code and try to enhance the performance. Make the necessary changes and provide only the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code.";

    private static final String COMMENT_CODE_PROMPT = "Look at this {0} code and generate a short but concise comment which explains the primary purpose. Include only the comment and no additional informations.";

    private static final String FIX_BROKEN_PROMPT = "Look at the following {0} code, identify the problem, why has the code an error. Make the necessary changes and only provide the updated code. Also do not explain what you did or comment on anything just provide code, don't use formatting, give always the raw code.";

    public static String getSimpleCodePrompt() {
        return SIMPLE_CODE_PROMPT;
    }

    public static String getSimpleErrorExplainPrompt() {
        return ERROR_EXPLAIN_PROMPT_SIMPLE;
    }

    public static String getBulletErrorExplainPrompt() {
        return ERROR_EXPLAIN_PROMPT_BULLET;
    }

    public static String getReadabilityPrompt() {
        return READABILITY_PROMPT;
    }

    public static String getBugsPrompt() {
        return BUGS_PROMPT;
    }

    public static String getPerformancePrompt() {
        return PERFORMANCE_PROMPT;
    }

    public static String getCommentCodePrompt() {
        return COMMENT_CODE_PROMPT;
    }

    public static String getFixBrokenPrompt() {
        return FIX_BROKEN_PROMPT;
    }

    private static final String TITLE = "MyOllamaEnhancer";

    public static String getTitle() {
        return TITLE;
    }

    private static final String API_ENDPOINT_KEY = "de.liebki.myollamaenhancer.settings.apiEndpoint";
    private static final String OLLAMA_MODEL_KEY = "de.liebki.myollamaenhancer.settings.ollamaModel";

    public static String getApiEndpointKey() {
        return API_ENDPOINT_KEY;
    }

    public static String getOllamaModelKey() {
        return OLLAMA_MODEL_KEY;
    }

    public static String getApiEndpoint() {
        // Retrieve the saved API endpoint, returning a default if not found
        return PropertiesComponent.getInstance().getValue(API_ENDPOINT_KEY, "http://localhost:11434/");
    }

    public static String getOllamaModel() {
        // Retrieve the saved Ollama model, returning a default if not found
        return PropertiesComponent.getInstance().getValue(OLLAMA_MODEL_KEY, "llama3:8b-instruct-q6_K");
    }
}