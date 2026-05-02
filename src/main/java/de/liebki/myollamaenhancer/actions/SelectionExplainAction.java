package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.liebki.myollamaenhancer.configuration.MyOllamaEnhancerSettingsService;
import de.liebki.myollamaenhancer.utils.*;
import org.jetbrains.annotations.NotNull;

import java.awt.event.InputEvent;
import java.util.Map;

/**
 * Quick explain for selected code in the editor.
 * - Only enabled when there is selected text in an editor
 * - Holding SHIFT while invoking lets the user input additional context
 * - Renders the explanation in the ExplainToolWindow
 */
public class SelectionExplainAction extends ActionBase implements DumbAware {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Project project = e.getProject();
        if (!this.isValidSelectionContext(project, e)) {
            DialogUtil.showWarning("Please select code in an editor to explain.", "No Selection");
            return;
        }

        final String selectedText = this.getSelectedText(e);
        if (null == selectedText || selectedText.trim().isEmpty()) {
            DialogUtil.showWarning("Please select code in an editor to explain.", "No Selection");
            return;
        }

        final VirtualFile vf = this.getVirtualFile(project);
        final String language = this.detectLanguageOrDefault(project);

        final String systemInstructions = this.maybePromptSystemInstructions(e);
        final String finalSystemInstructions = systemInstructions;

        this.startSelectionExplanationTask(project, selectedText, language, vf, finalSystemInstructions);
    }

    // Validates project, editor presence, and selection availability
    private boolean isValidSelectionContext(final Project project, final AnActionEvent e) {
        return null != project && this.validateProjectAndEditor(e) && this.validateSelectedText(e);
    }

    // Detects language for the current file or returns a default
    private String detectLanguageOrDefault(final Project project) {
        return null != getLanguageOfFile(project) ? this.getLanguageOfFile(project) : "source code";
    }

    // Optionally prompts for user instructions when SHIFT is held
    private String maybePromptSystemInstructions(final AnActionEvent e) {
        String systemInstructions = null;
        final InputEvent inputEvent = e.getInputEvent();
        if (null != inputEvent && inputEvent.isShiftDown()) {
            systemInstructions = DialogUtil.showInputDialog(
                "Enter your question or topic for the model (e.g., constraints, focus areas, what to change).\n" +
                "Note: This will be sent as 'User Question or Topic' together with the selected code. A minimal system prompt will instruct the model to answer based on both.",
                MyOllamaEnhancerSettingsService.TITLE
            );
            if (null != systemInstructions) {
                systemInstructions = systemInstructions.trim();
            }
        }
        return systemInstructions;
    }

    // Starts the modal task to generate the explanation for the selection
    private void startSelectionExplanationTask(final Project project,
                                               final String selectedText,
                                               final String language,
                                               final VirtualFile vf,
                                               final String finalSystemInstructions) {
        NotificationUtil.info(project, "Generating explanation for selection...");
        ProgressManager.getInstance().run(new Task.Modal(project, "Explaining Selection...", true) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                try {
                    SelectionExplainAction.this.setupIndicatorForSelection(indicator, language);

                    final String explanation = generateSelectionExplanation(
                        selectedText, language, vf, project, finalSystemInstructions, indicator);

                    SelectionExplainAction.this.showSelectionResultOnEdt(explanation, project);
                } catch (final Exception ex) {
                    SelectionExplainAction.this.errorLog(ex, getClass());
                } finally {
                    indicator.setFraction(1.0);
                    indicator.setText("Done");
                    indicator.setText2("");
                }
            }

            @Override
            public void onCancel() {
                NotificationUtil.info(project, "File explanation was canceled.");
            }
        });
    }

    // Initializes indicator for selection explanation
    private void setupIndicatorForSelection(final ProgressIndicator indicator, final String language) {
        indicator.setIndeterminate(false);
        indicator.setFraction(0.0);
        indicator.setText("Explaining selected code...");
        indicator.setText2("Preparing prompt (" + language + ")...");
    }

    // Shows the resulting explanation on the EDT
    private void showSelectionResultOnEdt(final String explanation, final Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (null == explanation || explanation.trim().isEmpty()) {
                DialogUtil.showError("Failed to generate explanation.", "Error");
            } else {
                DialogUtil.showResponseInExplainWindow(explanation, project);
                NotificationUtil.info(project, "Explanation ready");
            }
        });
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        final boolean enabled = this.validateProjectAndEditor(e) && this.validateSelectedText(e);
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * Minimal system prompt used when the user supplies custom instructions via SHIFT.
     * Guides the model to answer based on both the user's question/topic and the provided code.
     */
    private String buildMinimalQuestionCodePrompt(final String language) {
        final String lang = (null != language && !language.isEmpty()) ? language : "source code";
        return "The user will provide a question or topic and a piece of " + lang + 
                ". Answer the user's question/topic strictly based on the provided code. " +
                "Be concise, actionable, and reference relevant parts of the code. " +
                "If the question concerns feasibility or changes (e.g., renaming methods, changing parameters), " +
                "state whether it's possible and outline the concrete steps or code changes.";
    }

    private String buildSelectionExplainPrompt(final String language, final VirtualFile vf, final Project project, final String systemInstructions) {
        final StringBuilder sb = new StringBuilder();

        if (null != systemInstructions && !systemInstructions.isEmpty()) {
            // SHIFT path: user content will contain ONLY model instructions.
            sb.append("Follow the user's model instructions regarding the selected ")
              .append(language)
              .append(" code. The code snippet itself is NOT included in the user message.\n\n")
              .append("Your response should:\n")
              .append("1. Directly answer the user's instructions/questions.\n")
              .append("2. If the user asks about feasibility (e.g., renaming a method or changing parameters), explain whether it's possible and outline the concrete steps or code changes.\n")
              .append("3. Be concise and actionable.\n\n");
        } else {
            // Normal path: explain the selected snippet.
            sb.append("Provide a clear and thorough explanation of the following selected ")
              .append(language)
              .append(" code. Focus only on this snippet, not the entire file.\n\n")
              .append("Your response should include:\n")
              .append("1. What the code does and how it works.\n")
              .append("2. Roles of important identifiers, functions, classes, or patterns.\n")
              .append("3. Any assumptions, side effects, or edge cases.\n")
              .append("4. How it likely interacts with surrounding code.\n");
        }

        if (null != vf) {
            sb.append("File: ").append(vf.getName()).append("\n");
        }
        if (null != project) {
            sb.append("Project: ").append(project.getName()).append("\n\n");
        }
        sb.append("Your explanation should avoid mentioning any tooling or processing details.");
        return sb.toString();
    }

    private String generateSelectionExplanation(final String selectedText,
                                                final String language,
                                                final VirtualFile vf,
                                                final Project project,
                                                final String systemInstructions,
                                                final ProgressIndicator indicator) {
        try {
            indicator.setFraction(0.2);
            // Build system prompt:
            // - With SHIFT instructions: use a minimal base prompt that explains how to use question + code.
            // - Without SHIFT: use the standard explanation prompt.
            final String prompt = (null != systemInstructions && !systemInstructions.isEmpty())
                    ? buildMinimalQuestionCodePrompt(language)
                    : buildSelectionExplainPrompt(language, vf, project, null);

            final Map<String, Object> structuredOutput = StructuredOutputSchemas.createExplanationSchema();
            final String enhancedPrompt = StructuredPromptEnhancer.enhanceForFileExplanation(prompt);

            indicator.setText("Contacting model...");
            // Build user content:
            // - With SHIFT: include "User Question or Topic" and "Code" sections.
            // - Without SHIFT: only send the code.
            final String userContent = (null != systemInstructions && !systemInstructions.isEmpty())
                    ? "User Question or Topic:\n" + systemInstructions + "\n\nCode:\n" + selectedText
                    : selectedText;

            final OllamaAPIUtil.ThinkContent response = OllamaAPIUtil.generateStructuredOllamaResponseSync(
                    enhancedPrompt,
                    userContent,
                    structuredOutput
            );

            indicator.setFraction(0.8);
            indicator.setText("Parsing response...");
            return StructuredResponseParser.parseExplanationResponse(response.visibleContent());
        } catch (final Exception ex) {
            this.errorLog(ex, getClass());
            return null;
        }
    }
}
