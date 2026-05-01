package de.liebki.myollamaenhancer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Consumer;

public class OllamaAPIUtil {

    private static Ollama ollama;

    private static Ollama getOllamaInstance() {
        if(ollama == null) {
            ollama = new Ollama(DataHolder.getApiEndpoint());
            ollama.setRequestTimeoutSeconds(DataHolder.getApiTimeout());
        }
        return ollama;
    }

    private static boolean isOllamaActive() {
        Ollama ollama = getOllamaInstance();
        try {
            return ollama.ping();
        } catch (OllamaException e) {
            return false;
        }
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

    private static String generateAndGetOllamaAnswer(String userPrompt, String sysPrompt) throws OllamaException {
        Ollama ollama = getOllamaInstance();

        if(!isOllamaActive()) {
            FeedbackOptions.showErrorOllamaInactive();
        }

        // Updated to use the new builder pattern
        OllamaChatRequest requestModel = OllamaChatRequest.builder()
                .withModel(DataHolder.getOllamaModel())
                .withMessage(OllamaChatMessageRole.SYSTEM, sysPrompt)
                .withMessage(OllamaChatMessageRole.USER, userPrompt)
                .build();

        // Updated method call (passing null for the stream handler)
        OllamaChatResult chatResult = ollama.chat(requestModel, null);

        // Updated to get response content (getContent() -> getResponse())
        return chatResult.getResponseModel().getMessage().getResponse();
    }

}
