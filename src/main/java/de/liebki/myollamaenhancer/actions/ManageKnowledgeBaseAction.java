package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import de.liebki.myollamaenhancer.windows.KnowledgeBaseManagementWindow;
import org.jetbrains.annotations.NotNull;

public class ManageKnowledgeBaseAction extends ActionBase implements DumbAware {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Project project = e.getProject();
        if (null != project) {
            // Show progress indicator while initializing the knowledge base manager window
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Initializing Knowledge Base Manager...", true) {
                @Override
                public void run(@NotNull final ProgressIndicator indicator) {
                    try {
                        indicator.setIndeterminate(true);
                        indicator.setText("Initializing knowledge base manager...");
                        
                        // Create and show the window on the EDT
                        ApplicationManager.getApplication().invokeLater(() -> {
                            final KnowledgeBaseManagementWindow window = new KnowledgeBaseManagementWindow(project);
                            window.show();
                        });
                    } catch (final Exception ex) {
                        ManageKnowledgeBaseAction.this.errorLog(ex, getClass());
                    }
                }

                @Override
                public void onCancel() {
                    NotificationUtil.info(project, "Knowledge Base Manager opening canceled.");
                }

            });
        }
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(null != e.getProject());
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

} 