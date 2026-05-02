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
import de.liebki.myollamaenhancer.configuration.MyOllamaEnhancerSettingsService;
import de.liebki.myollamaenhancer.types.OllamaOption;
import de.liebki.myollamaenhancer.utils.CodeEnhancementService;
import de.liebki.myollamaenhancer.utils.DialogUtil;
import de.liebki.myollamaenhancer.utils.LanguageUtils;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

public class OptionCodeEnhanceAction extends ActionBase implements DumbAware {
    private static final Logger LOG = Logger.getInstance(OptionCodeEnhanceAction.class);

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        if (!validateSelectedText(e)) {
            DialogUtil.showErrorNoSelection();
            return;
        }

        final String selectedCode = this.getSelectedText(e);
        final String codeLanguage = this.getLanguageOfFile(e.getProject());

        final String selectedComboOption = OptionCodeEnhanceAction.promptUserForOption();
        if (null == selectedComboOption) {
            return;
        }

        processSelectedOption(e, selectedCode, codeLanguage, selectedComboOption);
    }

    private static String promptUserForOption() {
        return DialogUtil.showComboboxOptionPrompt();
    }

    private void processSelectedOption(final AnActionEvent e, final String selectedCode, final String codeLanguage, final String selectedComboOption) {
        final Project project = e.getProject();
        final OllamaOption selectedOption = OllamaOption.getOption(selectedComboOption);
        final String ollamaPrompt = this.getOllamaPrompt(selectedOption);

        final String sysPrompt = MessageFormat.format(ollamaPrompt, codeLanguage);
        final boolean isGenerateComment = "Add Comment".equals(selectedComboOption);

        final CodeEnhancementService.EnhancementType enhancementType = isGenerateComment ?
                CodeEnhancementService.EnhancementType.COMMENT :
                CodeEnhancementService.EnhancementType.OPTION;

        // Notify start and show progress indicator for code enhancement
        NotificationUtil.info(project, "Enhancing code with option: " + selectedComboOption);
        ProgressManager.getInstance().run(new Task.Modal(project, "Enhancing Code...", true) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Enhancing code with selected option...");
                    indicator.setText2("This may take a moment");

                    CodeEnhancementService.enhanceCode(
                            project,
                            selectedCode,
                            enhancementType,
                            sysPrompt,
                            codeLanguage,
                            (extractedResponse, rawResponse) -> {
                                // Show the result on the EDT
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    OptionCodeEnhanceAction.this.handleEnhancementResponse(e, extractedResponse, rawResponse, selectedCode, isGenerateComment);
                                });
                            }
                    );
                } catch (final Exception ex) {
                    OptionCodeEnhanceAction.this.errorLog(ex, getClass());
                }
            }
            
            @Override
            public void onCancel() {
                NotificationUtil.info(project, "Option code enhancement was cancelled.");
            }
        });
    }

    private void handleEnhancementResponse(final AnActionEvent e, final String extractedResponse, final String rawResponse,
                                           final String selectedCode, final boolean isGenerateComment) {
        LOG.debug("Raw structured output: " + rawResponse);

        if (isGenerateComment) {
            this.handleCommentResponse(e, extractedResponse, selectedCode);
        } else {
            this.handleCodeResponse(e, extractedResponse);
        }
    }

    private void handleCommentResponse(final AnActionEvent e, final String extractedResponse, final String selectedCode) {
        if (null == extractedResponse || extractedResponse.trim().isEmpty()) {
            DialogUtil.showErrorNoSelection();
            return;
        }

        final String formattedComment = LanguageUtils.formatComment(
                extractedResponse.trim().replace("\n", " "),
                this.getLanguageOfFile(e.getProject())
        );

        final String result = formattedComment + "\n" + selectedCode;
        this.replaceSelectedText(e, result);
        NotificationUtil.info(e.getProject(), "Comment added to selection");
    }

    private void handleCodeResponse(final AnActionEvent e, final String extractedResponse) {
        LOG.debug("Extracted code: " + extractedResponse);
        this.replaceSelectedText(e, extractedResponse);
        NotificationUtil.info(e.getProject(), "Code enhanced");
    }

    private String getOllamaPrompt(final OllamaOption selectedComboOption) {
        final MyOllamaEnhancerSettingsService settingsService = MyOllamaEnhancerSettingsService.getInstance();
        return switch (selectedComboOption) {
            case READABILITY -> settingsService.getPromptReadability();
            case BUGS -> settingsService.getPromptBugs();
            case PERFORMANCE -> settingsService.getPromptPerformance();
            case UNIT_TESTS -> settingsService.getPromptUnitTests();
            case SIMPLIFY -> settingsService.getPromptSimplify();
            case COMMENT_CODE -> settingsService.getPromptCommentCode();
            case FIX_BROKEN -> settingsService.getPromptFixBroken();
            default -> "";
        };
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }


}