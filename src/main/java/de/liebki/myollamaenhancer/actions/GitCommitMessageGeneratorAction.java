package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import de.liebki.myollamaenhancer.utils.GitCommitMessageGenerator;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.Collection;

/**
 * Action to generate Git commit messages based on current changes.
 * Available in the VCS menu and editor context menu.
 */
public class GitCommitMessageGeneratorAction extends ActionBase implements DumbAware {
    private static final Logger LOG = Logger.getInstance(GitCommitMessageGeneratorAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();

        if (null == project) {
            LOG.warn("No project found");
            return;
        }

        LOG.info("Starting commit message generation for project: " + project.getName());

        // Get all changes from the change list manager
        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        final Collection<Change> changes = changeListManager.getDefaultChangeList().getChanges();

        LOG.info("Found " + changes.size() + " changes");

        if (changes.isEmpty()) {
            this.showInfoDialog(project, "No changes detected in the repository.", "No Changes");
            LOG.info("No changes to process");
            return;
        }

        // Show progress indicator for generating commit message
        ProgressManager.getInstance().run(new Task.Modal(project, "Generating Commit Message...", true) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Analyzing changes and generating commit message...");
                    indicator.checkCanceled();

                    LOG.info("Starting commit message generation in modal dialog...");
                    final String message = GitCommitMessageGenerator.generateCommitMessage(project, changes);
                    LOG.info("Successfully generated commit message");

                    indicator.checkCanceled();

                    ApplicationManager.getApplication().invokeLater(() -> {
                        // Try to set the commit message directly in the commit dialog if available
                        if (null != message) {
                            CommitMessageI refreshable = VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(e.getDataContext());
                            if (null != refreshable) {
                                refreshable.setCommitMessage(message);
                                LOG.info("Set commit message directly in commit dialog");
                                NotificationUtil.info(project, "Commit message generated");
                            } else {
                                showCommitMessageDialog(project, message);
                                NotificationUtil.info(project, "Commit message generated");
                            }
                        } else {
                            showErrorDialog(project, "Failed to generate commit message: No message was generated.", "Error");
                        }
                    });
                } catch (final Exception ex) {
                    errorLog(ex, getClass());
                }
            }

            @Override
            public void onCancel() {
                NotificationUtil.info(project, "Commit message generation was canceled.");
            }
        });
    }

    /**
     * Shows the commit message dialog with copy option.
     * Must be called on the Event Dispatch Thread.
     */
    private void showCommitMessageDialog(@NotNull Project project, @NotNull String commitMessage) {
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(() -> showCommitMessageDialog(project, commitMessage));
            return;
        }

        LOG.info("Showing commit message dialog");

        // Show dialog with the commit message and copy option
        final int result = Messages.showOkCancelDialog(
                project,
                commitMessage,
                "Generated Commit Message",
                "Copy to Clipboard",
                "Close",
                Messages.getInformationIcon()
        );

        if (Messages.OK == result) {
            // Copy to clipboard using IDE service
            final StringSelection selection = new StringSelection(commitMessage);
            CopyPasteManager.getInstance().setContents(selection);
            this.showInfoDialog(project, "Commit message copied to clipboard.", "Copied");
            LOG.info("Commit message copied to clipboard");
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // This method determines which thread is used to call update()
        // We want it to be called on the EDT to safely access the presentation
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        // Enable only if we have a project and there are changes
        final Project project = e.getProject();
        boolean enabled = false;

        if (null != project) {
            final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            final Collection<Change> changes = changeListManager.getDefaultChangeList().getChanges();

            enabled = !changes.isEmpty();
        }

        e.getPresentation().setEnabledAndVisible(enabled);
    }
}
