package de.liebki.myollamaenhancer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.chat.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Consumer;

public class OllamaAPIUtil {

    private static OllamaAPI ollamaAPI;

    private static OllamaAPI getOllamaInstance() {
        if(ollamaAPI == null) {
            ollamaAPI = new OllamaAPI(DataHolder.getApiEndpoint());
        }
        return ollamaAPI;
    }

    private static boolean isOllamaActive() {
        OllamaAPI ollamaAPI = getOllamaInstance();
        return ollamaAPI.ping();
    }

    public static void generateOllamaResponse(Project project, String userPrompt, String sysPrompt, Consumer<String> callback) {
        ProgressManager.getInstance().run(new Task.Modal(project, "Please Wait, Ollama Is Working...", true) {

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Generating response...");

                try {
                    String ollamaResponse = generateAndGetOllamaAnswer(userPrompt, sysPrompt);
                    ApplicationManager.getApplication().invokeLater(() -> callback.accept(ollamaResponse));
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(FeedbackOptions::showErrorExecution);
                }
            }
        });
    }

    private static String generateAndGetOllamaAnswer(String userPrompt, String sysPrompt) throws OllamaBaseException, IOException, InterruptedException {
        OllamaAPI ollamaAPI = getOllamaInstance();

        if(!isOllamaActive()) {
            FeedbackOptions.showErrorOllamaInactive();
        }

        OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(DataHolder.getOllamaModel());
        OllamaChatRequest requestModel = builder.withMessage(OllamaChatMessageRole.SYSTEM, sysPrompt)
                .withMessage(OllamaChatMessageRole.USER, userPrompt)
                .build();

        OllamaChatResult chatResult = ollamaAPI.chat(requestModel);
        return chatResult.getResponseModel().getMessage().getContent();
    }

}
