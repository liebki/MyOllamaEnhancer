package de.liebki.myollamaenhancer.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.vfs.VirtualFile;
import de.liebki.myollamaenhancer.utils.FileValidationUtils;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import de.liebki.myollamaenhancer.utils.DuckDbService;
import org.jetbrains.annotations.NotNull;

public class AutoReembedOnSaveListener implements FileDocumentManagerListener {

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        final VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (null == virtualFile || virtualFile.isDirectory())
            return;
        
        if (!FileValidationUtils.isValidFile(virtualFile)) 
            return;

        final Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
        if (null == project)
            return;

        final String currentContent = document.getText();

        final String taskTitle = "Refreshing Knowledge Base for " + virtualFile.getName();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, taskTitle, true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(taskTitle);
                indicator.setText2("Updating knowledge base entry...");
                try {
                    DuckDbService.reembedIfPresent(project, virtualFile, currentContent);
                    ApplicationManager.getApplication().invokeLater(() ->
                            NotificationUtil.info(project, "Refreshed: " + virtualFile.getName()));
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            NotificationUtil.error(project, "Failed to refresh: " + ex.getMessage()));
                }
            }
        });
    }
}
