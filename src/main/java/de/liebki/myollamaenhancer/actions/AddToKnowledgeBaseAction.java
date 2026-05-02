package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import de.liebki.myollamaenhancer.utils.DuckDbService;
import de.liebki.myollamaenhancer.events.KnowledgeBaseChangedTopic;
import org.jetbrains.annotations.NotNull;

/**
 * Action to add the current file content to the knowledge database.
 * Available in the editor context menu.
 */
public class AddToKnowledgeBaseAction extends ActionBase implements DumbAware {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final VirtualFile virtualFile = this.getVirtualFileFromEvent(e);
        
        if (null == project || null == virtualFile) {
            return;
        }

        if (!this.isValidFileForProcessing(virtualFile)) {
            return;
        }

        // Block duplicates: do not allow adding the same file twice
        if (DuckDbService.isFileInKnowledgeBase(project, virtualFile)) {
            this.showErrorDialog(project,
                    "This file is already in the Knowledge Base.\nIf you want to refresh its stored content, simply save the file to refresh the knowledge base entry.",
                    "Duplicate File Detected");
            NotificationUtil.info(project, "Skipped: File already exists in Knowledge Base");
            return;
        }

        // Notify start (non-blocking) and show progress indicator for adding file to knowledge base
        NotificationUtil.info(project, "Adding file to Knowledge Base...");
        ProgressManager.getInstance().run(new Task.Modal(project, "Adding to Knowledge Base...", true) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(false);
                    indicator.setFraction(0.0);

                    indicator.setText("Adding file to knowledge base...");
                    indicator.checkCanceled();
                    // Delegate to DuckDB-backed service
                    DuckDbService.addFileToKnowledgeBase(project, virtualFile, null);
                    
                    indicator.setFraction(0.5);
                    indicator.setText("Adding methods to knowledge base...");

                    indicator.checkCanceled();
                    DuckDbService.addMethodsToKnowledgeBase(project, virtualFile, null);
                    
                    indicator.setFraction(1.0);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        // Show very important success via dialog, plus a lightweight notification.
                        AddToKnowledgeBaseAction.this.showInfoDialog(project, "File successfully added to knowledge base.", "Success");
                        NotificationUtil.info(project, "Added the file to the Knowledge Base");
                        // Publish KB changed event
                        ApplicationManager.getApplication().getMessageBus()
                                .syncPublisher(KnowledgeBaseChangedTopic.TOPIC)
                                .knowledgeBaseChanged(project);
                    });
                } catch (final Exception ex) {
                    AddToKnowledgeBaseAction.this.errorLog(ex, getClass());
                }
            }
            
            @Override
            public void onCancel() {
                AddToKnowledgeBaseAction.this.informUserCanceled(project, getClass(), "Adding to knowledge base was cancelled.");
            }
        });
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        // Enable only if we have a project and a valid file is selected
        e.getPresentation().setEnabledAndVisible(this.validateProjectAndFile(e));
    }
} 