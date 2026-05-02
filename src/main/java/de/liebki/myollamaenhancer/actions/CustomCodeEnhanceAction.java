package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import de.liebki.myollamaenhancer.utils.CodeEnhancementService;
import de.liebki.myollamaenhancer.utils.CustomPromptHistoryManager;
import de.liebki.myollamaenhancer.utils.DialogUtil;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import de.liebki.myollamaenhancer.windows.CustomPromptHistoryWindow;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;


public class CustomCodeEnhanceAction extends ActionBase implements DumbAware {
    private static final Logger LOG = Logger.getInstance(CustomCodeEnhanceAction.class);

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        if (this.validateSelectedText(e)) {
            this.showCustomPromptWindow(e);
        } else {
            DialogUtil.showErrorNoSelection();
        }
    }

    private void showCustomPromptWindow(@NotNull final AnActionEvent e) {
        final String selectedCode = this.getSelectedText(e);
        final CustomPromptHistoryWindow window = this.createPromptWindow(e, selectedCode);
        window.setVisible(true);
    }

    private CustomPromptHistoryWindow createPromptWindow(@NotNull final AnActionEvent e, final String selectedCode) {
        return new CustomPromptHistoryWindow(e.getProject(), evt -> {
            // Get the window reference directly from the enclosing scope
            final Object src = evt.getSource();
            if (src instanceof JButton button) {
                final java.awt.Component ancestor = button.getTopLevelAncestor();
                if (ancestor instanceof CustomPromptHistoryWindow window) {
                    this.handlePromptSubmission(e, selectedCode, window);
                } else {
                    LOG.warn("Top-level ancestor is not CustomPromptHistoryWindow: " + ancestor);
                }
            } else {
                LOG.warn("Event source is not a JButton: " + src);
            }
        });
    }

    private void handlePromptSubmission(@NotNull final AnActionEvent e, final String selectedCode, final CustomPromptHistoryWindow window) {
        final String prompt = window.getInputPrompt();
        if (this.isPromptEmpty(prompt)) {
            return;
        }

        final boolean isRerun = this.isPromptInHistory(prompt);
        this.enhanceCodeWithPrompt(e, selectedCode, prompt, isRerun, window);
    }

    private boolean isPromptEmpty(final String prompt) {
        return null == prompt || prompt.trim().isEmpty();
    }

    private boolean isPromptInHistory(final String prompt) {
        return CustomPromptHistoryManager.getHistory().stream()
                .anyMatch(entry -> entry.prompt.equals(prompt));
    }

    private void enhanceCodeWithPrompt(@NotNull final AnActionEvent e, final String selectedCode, final String prompt,
                                       final boolean isRerun, final CustomPromptHistoryWindow window) {
        final Project project = e.getProject();
        
        // Notify start and show progress indicator for code enhancement
        NotificationUtil.info(project, "Enhancing code with custom prompt...");
        ProgressManager.getInstance().run(new Task.Modal(project, "Enhancing Code...", true) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Enhancing code with custom prompt...");
                    indicator.setText2("This may take a moment");

                    CodeEnhancementService.enhanceCode(
                            project,
                            selectedCode,
                            CodeEnhancementService.EnhancementType.CUSTOM,
                            prompt,
                            null,
                            (extractedCode, rawResponse) -> {
                                // Show the result on the EDT
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    CustomCodeEnhanceAction.this.handleEnhancementResult(e, prompt, extractedCode, rawResponse, isRerun, window);
                                });
                            }
                    );
                } catch (final Exception ex) {
                    CustomCodeEnhanceAction.this.errorLog(ex, getClass());
                }
            }
            
            @Override
            public void onCancel() {
                NotificationUtil.info(project, "Custom code enhancement was canceled.");
                if (null != window) {
                    window.dispose();
                }
            }
        });
    }

    private void handleEnhancementResult(@NotNull final AnActionEvent e, final String prompt, final String extractedCode,
                                         final String rawResponse, final boolean isRerun, final CustomPromptHistoryWindow window) {
        this.logEnhancementResult(rawResponse, extractedCode);

        if (!isRerun) {
            CustomPromptHistoryManager.addEntry(prompt, rawResponse);
        }

        this.replaceSelectedText(e, extractedCode);
        window.dispose();
        final Project project = e.getProject();
        if (null != project) {
            NotificationUtil.info(project, "Custom enhancement done");
        }
    }

    private void logEnhancementResult(final String rawResponse, final String extractedCode) {
        LOG.debug("Raw structured output: " + rawResponse);
        LOG.debug("Extracted code: " + extractedCode);
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(this.validateSelectedText(e));
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }


}