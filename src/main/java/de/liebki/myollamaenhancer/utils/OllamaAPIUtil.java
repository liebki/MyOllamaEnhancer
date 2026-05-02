package de.liebki.myollamaenhancer.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import de.liebki.myollamaenhancer.configuration.MyOllamaEnhancerSettingsService;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.ToolInvocationException;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.models.embed.OllamaEmbedRequest;
import io.github.ollama4j.models.embed.OllamaEmbedResult;
import io.github.ollama4j.models.generate.OllamaGenerateRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum OllamaAPIUtil {
    ;

    private static final Pattern THINK_PATTERN = Pattern.compile("(?s)(.*?)(?:<think>(.*?)</think>)?(.*)");

    private static Ollama ollama;

    public record ThinkContent(String visibleContent, String thinkContent) {}

    /**
     * Parses the response content to extract thinking content and visible content.
     *
     * @param content The raw content from the LLM response
     * @return A ThinkContent object containing separated visible and thinking content
     */
    public static ThinkContent parseThinkContent(String content) {
        if (null == content || content.isEmpty()) {
            return new ThinkContent("", "");
        }

        Matcher matcher = THINK_PATTERN.matcher(content);
        if (matcher.matches()) {
            String beforeThink = null != matcher.group(1) ? matcher.group(1).trim() : "";
            String thinkContent = null != matcher.group(2) ? matcher.group(2).trim() : "";
            String afterThink = null != matcher.group(3) ? matcher.group(3).trim() : "";

            String visibleContent = (beforeThink + " " + afterThink).trim();
            return new ThinkContent(visibleContent, thinkContent);
        }

        return new ThinkContent(content, "");
    }

    private static Ollama getOllamaInstance() {
        if (null == ollama) {
            OllamaAPIUtil.ollama = new Ollama(MyOllamaEnhancerSettingsService.getInstance().getApiEndpoint());
            OllamaAPIUtil.ollama.setRequestTimeoutSeconds(MyOllamaEnhancerSettingsService.getInstance().getApiTimeout());
        }

        return OllamaAPIUtil.ollama;
    }

    /**
     * Checks if Ollama is reachable using the currently configured settings.
     *
     * @return true if reachable, false otherwise
     */
    public static boolean isOllamaActive() {
        try {
            final Ollama ollama = OllamaAPIUtil.getOllamaInstance();
            return ollama.ping();
        } catch (Exception e) {
            System.err.println("[OllamaAPIUtil] Failed to ping Ollama: " + e.getMessage());
            return false;
        }
    }

    /**
     * Pings Ollama using a provided endpoint and timeout without mutating the shared instance.
     *
     * @param endpoint base URL to Ollama, for example http://localhost:11434
     * @param timeoutSeconds request timeout in seconds
     * @return true if reachable, false otherwise
     */
    public static boolean ping(final String endpoint, final int timeoutSeconds) {
        try {
            final Ollama api = new Ollama(endpoint);
            api.setRequestTimeoutSeconds(timeoutSeconds);
            return api.ping();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * This method is for a future use case, to generate content using any LLM without structured output.
     */
    public static void generateOllamaResponse(final Project project, final String userPrompt, final String sysPrompt, final Consumer<ThinkContent> callback) {
        ProgressManager.getInstance().run(new Task.Modal(project, "Please Wait, Ollama Is Working...", true) {

            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Generating response...");

                try {
                    final ThinkContent response = OllamaAPIUtil.generateAndGetOllamaAnswer(userPrompt, sysPrompt);
                    ApplicationManager.getApplication().invokeLater(() -> callback.accept(response));
                } catch (final Exception e) {
                    EnhancedErrorHandlingUtil.handleOllamaException(project, e, "response generation",
                            () -> ApplicationManager.getApplication().invokeLater(() ->
                                    EnhancedErrorHandlingUtil.showErrorDialog(project, "Ollama Error", "Failed to generate response: " + e.getMessage())));
                }
            }
        });
    }

    private static ThinkContent generateAndGetOllamaAnswer(final String userPrompt, final String sysPrompt) throws OllamaException, IOException, InterruptedException, ToolInvocationException {
        final Ollama ollama = OllamaAPIUtil.getOllamaInstance();

        if (!OllamaAPIUtil.isOllamaActive()) {
            DialogUtil.showErrorOllamaInactive();
        }

        final OllamaChatRequest requestModel = OllamaChatRequest.builder()
                .withModel(MyOllamaEnhancerSettingsService.getInstance().getOllamaModel())
                .withMessage(OllamaChatMessageRole.SYSTEM, sysPrompt)
                .withMessage(OllamaChatMessageRole.USER, userPrompt)
                .build();

        final OllamaChatResult chatResult = ollama.chat(requestModel, null);
        final String response = chatResult.getResponseModel().getMessage().getResponse();

        return parseThinkContent(response);
    }

    public static ThinkContent generateOllamaResponseSync(final String userPrompt, final String sysPrompt) throws Exception {
        return OllamaAPIUtil.generateAndGetOllamaAnswer(userPrompt, sysPrompt);
    }

    public static ThinkContent generateStructuredOllamaResponseSync(final String userPrompt, final String sysPrompt, final Map<String, Object> structuredOutput) throws Exception {
        return OllamaAPIUtil.generateAndGetStructuredOllamaAnswer(userPrompt, sysPrompt, structuredOutput);
    }

    public static void generateStructuredOllamaResponse(final Project project, final String userPrompt, final String sysPrompt, final Map<String, Object> structuredOutput, final Consumer<ThinkContent> callback) {
        ProgressManager.getInstance().run(new Task.Modal(project, "Please Wait, Ollama Is Working...", true) {

            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Generating structured response...");

                try {
                    final ThinkContent response = OllamaAPIUtil.generateAndGetStructuredOllamaAnswer(userPrompt, sysPrompt, structuredOutput);
                    ApplicationManager.getApplication().invokeLater(() -> callback.accept(response));
                } catch (final Exception e) {
                    EnhancedErrorHandlingUtil.handleOllamaException(project, e, "structured response generation",
                            () -> ApplicationManager.getApplication().invokeLater(() ->
                                    EnhancedErrorHandlingUtil.showErrorDialog(project, "Ollama Error", "Failed to generate structured response: " + e.getMessage())));
                }
            }
        });
    }

    public static List<Double> generateEmbeddings(final String query) throws Exception {
        final Ollama ollama = OllamaAPIUtil.getOllamaInstance();

        if (!OllamaAPIUtil.isOllamaActive()) {
            throw new IOException("Ollama is not active. Please ensure Ollama is running and accessible.");
        }

        final OllamaEmbedRequest request = new OllamaEmbedRequest("bge-m3:latest", List.of(query));

        final OllamaEmbedResult result = ollama.embed(request);

        return result.getEmbeddings().get(0);
    }

    private static ThinkContent generateAndGetStructuredOllamaAnswer(final String userPrompt, final String sysPrompt, final Map<String, Object> structuredOutput) throws OllamaException, IOException, InterruptedException, ToolInvocationException {
        final Ollama ollama = OllamaAPIUtil.getOllamaInstance();

        if (!OllamaAPIUtil.isOllamaActive()) {
            DialogUtil.showErrorOllamaInactive();
        }

        try {
            final String combinedPrompt = sysPrompt + "\n\n" + userPrompt;
            final OllamaGenerateRequest request = OllamaGenerateRequest.builder()
                    .withModel(MyOllamaEnhancerSettingsService.getInstance().getOllamaModel())
                    .withPrompt(combinedPrompt)
                    .withFormat(structuredOutput)
                    .build();

            final var result = ollama.generate(request, null);

            System.out.println("Structured output response: " + result.getResponse());
            return parseThinkContent(result.getResponse());
        } catch (final Exception e) {
            System.out.println("Structured output not supported, falling back to chat API: " + e.getMessage());
            return OllamaAPIUtil.generateAndGetOllamaAnswer(userPrompt, sysPrompt);
        }
    }

    /**
     * Generate a response from Ollama and return it all at once when complete.
     * This is a non-streaming version that waits for the full response.
     */
    public static void generateStreamingOllamaResponse(final Project project, final String userPrompt, final String sysPrompt,
                                                       final Consumer<String> onToken, final Runnable onComplete, final Consumer<String> onError) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating response...", true) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Generating response...");

                try {
                    if (!OllamaAPIUtil.isOllamaActive()) {
                        ApplicationManager.getApplication().invokeLater(() -> onError.accept("Ollama is not running"));
                        return;
                    }

                    final ThinkContent response = OllamaAPIUtil.generateAndGetOllamaAnswer(userPrompt, sysPrompt);

                    System.out.println("[generateStreamingOllamaResponse] think content: " + response.thinkContent());

                    ApplicationManager.getApplication().invokeLater(() -> {
                        onToken.accept(response.visibleContent);
                        onComplete.run();
                    });
                } catch (final Exception e) {
                    EnhancedErrorHandlingUtil.handleOllamaException(project, e, "streaming response generation", null);
                    ApplicationManager.getApplication().invokeLater(() -> onError.accept("Error: " + e.getMessage()));
                }
            }
        });
    }
}
