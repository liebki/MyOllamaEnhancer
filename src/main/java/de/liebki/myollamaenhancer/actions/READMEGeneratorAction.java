package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import de.liebki.myollamaenhancer.utils.READMEGenerator;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;

/**
 * Action to generate a project README based on code analysis.
 * Available in the project context menu.
 */
public class READMEGeneratorAction extends ActionBase implements DumbAware {
    private static final Logger LOG = Logger.getInstance(READMEGeneratorAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        
        if (null == project) {
            LOG.warn("No project found");
            return;
        }

        LOG.info("Starting README generation for project: " + project.getName());

        // Notify start and show blocking modal progress indicator for README generation
        NotificationUtil.info(project, "Generating README...");
        ProgressManager.getInstance().run(new Task.Modal(project, "Generating README...", true) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(false);
                    indicator.setText("Analyzing project and generating README...");
                    indicator.setText2("This may take a moment");

                    final String readmeContent = READMEGenerator.generateREADMEBlocking(project, indicator);

                    // Process result on EDT
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (null != readmeContent && !readmeContent.startsWith("Error")) {
                            showReadmeDialog(project, readmeContent);
                        } else {
                            final String errorMessage = null != readmeContent ? readmeContent : "Failed to generate README: No content was generated.";
                            showErrorDialog(project, errorMessage, "README Generation Error");
                        }
                    });
                    
                } catch (final Exception ex) {
                    READMEGeneratorAction.this.errorLog(ex, getClass());
                }
            }
            
            @Override
            public void onCancel() {
                NotificationUtil.info(project, "README generation was canceled.");
            }
        });
    }

    /**
     * Shows the generated README dialog with options to save or copy.
     *
     * @param project      The current project
     * @param readmeContent The generated README content
     */
    private void showReadmeDialog(@NotNull Project project, @NotNull String readmeContent) {
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(() -> showReadmeDialog(project, readmeContent));
            return;
        }

        LOG.info("Showing README dialog");
        
        // Show dialog with the README content and options
        final int result = Messages.showYesNoCancelDialog(
            project,
            readmeContent,
            "Generated README",
            "Save to File",
            "Copy to Clipboard",
            "Close",
            Messages.getInformationIcon()
        );
        
        switch (result) {
            case Messages.YES: // Save to file
                saveReadmeToFile(project, readmeContent);
                break;
            case Messages.NO: // Copy to clipboard
                final StringSelection selection = new StringSelection(readmeContent);
                CopyPasteManager.getInstance().setContents(selection);
                this.showInfoDialog(project, "README content copied to clipboard.", "Copied");
                LOG.info("README content copied to clipboard");
                break;
            case Messages.CANCEL: // Close
            default:
                // Do nothing
                break;
        }
    }

    /**
     * Saves the generated README content to a file.
     *
     * @param project       The current project
     * @param readmeContent The README content to save
     */
    private void saveReadmeToFile(@NotNull Project project, @NotNull String readmeContent) {
        try {
            final VirtualFile projectDir = project.getBaseDir();
            if (null == projectDir) {
                showErrorDialog(project, "Could not access project directory.", "Error");
                return;
            }

            VirtualFile readmeFile = null;
            
            // Check if README.md already exists
            final VirtualFile existingReadme = projectDir.findChild("README.md");
            if (null != existingReadme) {
                final boolean overwrite = showConfirmationDialog(
                    project, 
                    "A README.md file already exists. Do you want to overwrite it?", 
                    "Overwrite README"
                );
                
                if (!overwrite) {
                    return;
                }
                
                // Overwrite existing README using write action
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        existingReadme.setBinaryContent(readmeContent.getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                readmeFile = existingReadme;
                this.showInfoDialog(project, "README.md has been updated successfully.", "Success");
            } else {
                // Create new README.md using write action
                final VirtualFile newReadme = ApplicationManager.getApplication().runWriteAction((ThrowableComputable<VirtualFile, Exception>) () ->
                    projectDir.createChildData(project, "README.md")
                );
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        newReadme.setBinaryContent(readmeContent.getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                readmeFile = newReadme;
                this.showInfoDialog(project, "README.md has been created successfully.", "Success");
            }
            
            // Open the README file in the editor
            if (null != readmeFile) {
                FileEditorManager.getInstance(project).openFile(readmeFile, true);
            }
            
        } catch (Exception e) {
            LOG.warn("Error saving README: " + e.getMessage(), e);
            showErrorDialog(project, "Error saving README: " + e.getMessage(), "Error");
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // Align with other actions: use background thread for updates
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        // Enable only if we have a project
        final Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(null != project);
    }
}
