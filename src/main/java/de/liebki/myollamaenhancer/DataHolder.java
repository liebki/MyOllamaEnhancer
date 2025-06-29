package de.liebki.myollamaenhancer;

import com.intellij.ide.util.PropertiesComponent;

public class DataHolder {

    private DataHolder() {
    }

    private static final String TITLE = "MyOllamaEnhancer";

    public static String getTitle() {
        return TITLE;
    }

    private static final String API_ENDPOINT_KEY = "de.liebki.myollamaenhancer.settings.apiEndpoint";
    private static final String OLLAMA_MODEL_KEY = "de.liebki.myollamaenhancer.settings.ollamaModel";
    private static final String NOTIFICATION_KEY = "de.liebki.myollamaenhancer.settings.notification";
    private static final String API_TIMEOUT_KEY = "de.liebki.myollamaenhancer.settings.apitimeout";

    public static String getApiEndpointKey() {
        return API_ENDPOINT_KEY;
    }

    public static String getApiTimeoutKey() {
        return API_TIMEOUT_KEY;
    }

    public static String getOllamaModelKey() {
        return OLLAMA_MODEL_KEY;
    }

    public static String getApiEndpoint() {
        return PropertiesComponent.getInstance().getValue(API_ENDPOINT_KEY, "http://localhost:11434/");
    }

    public static boolean getNotificationKey() {
        return PropertiesComponent.getInstance().getBoolean(NOTIFICATION_KEY, false);
    }

    public static String getOllamaModel() {
        return PropertiesComponent.getInstance().getValue(OLLAMA_MODEL_KEY, "llama3:8b-instruct-q6_K");
    }

    public static long getApiTimeout() {
        return PropertiesComponent.getInstance().getInt(API_TIMEOUT_KEY, 20);
    }

    public static void setNotificationKey(boolean value) {
        PropertiesComponent.getInstance().setValue(NOTIFICATION_KEY, value);
    }

}