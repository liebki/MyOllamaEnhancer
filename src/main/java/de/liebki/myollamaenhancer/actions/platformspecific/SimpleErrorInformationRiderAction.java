package de.liebki.myollamaenhancer.platformspecific;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import de.liebki.myollamaenhancer.actions.ActionBase;
import de.liebki.myollamaenhancer.utils.DialogUtil;
import de.liebki.myollamaenhancer.utils.ErrorExplanationService;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import org.jetbrains.annotations.NotNull;

public class SimpleErrorInformationRiderAction extends ActionBase implements DumbAware {
    private static final Logger LOG = Logger.getInstance(SimpleErrorInformationRiderAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project activeProject = e.getProject();
        final String userCopiedStacktrace = DialogUtil.getStacktraceFromUser();

        if (null != userCopiedStacktrace && !userCopiedStacktrace.isEmpty()) {
            // Notify start and show progress indicator for error explanation generation
            NotificationUtil.info(activeProject, "Generating error explanation...");
            ProgressManager.getInstance().run(new Task.Modal(activeProject, "Generating Error Explanation...", true) {
                @Override
                public void run(@NotNull final ProgressIndicator indicator) {
                    try {
                        indicator.setIndeterminate(true);
                        indicator.setText("Analyzing error and generating explanation...");
                        indicator.setText2("This may take a moment");

                        ErrorExplanationService.explainError(activeProject, userCopiedStacktrace, (extractedResponse, rawResponse) -> {
                            LOG.debug("Raw structured output: " + rawResponse);
                            LOG.debug("Extracted explanation: " + extractedResponse);

                            ApplicationManager.getApplication().invokeLater(() -> {
                                DialogUtil.showResponseInToolWindow(extractedResponse, activeProject);
                                NotificationUtil.info(activeProject, "Explanation ready");
                            });
                        });
                    } catch (final Exception ex) {
                        final String errorMsg = null != ex.getMessage() ? ex.getMessage() : "Unknown error";
                        LOG.error("Error in background task: " + errorMsg, ex);
                        
                        ApplicationManager.getApplication().invokeLater(() -> {
                            DialogUtil.showError("Failed to generate error explanation: " + errorMsg, "Error");
                        });
                    }
                }
                
                @Override
                public void onCancel() {
                    LOG.info("Canceled by user");
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.info(activeProject, "Error explanation was canceled.");
                    });
                }
            });
        } else {
            DialogUtil.showErrorNoInput();
        }

    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }


}