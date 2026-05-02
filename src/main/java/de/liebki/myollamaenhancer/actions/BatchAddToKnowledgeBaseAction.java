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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import de.liebki.myollamaenhancer.utils.*;
import de.liebki.myollamaenhancer.events.KnowledgeBaseChangedTopic;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchAddToKnowledgeBaseAction extends ActionBase implements DumbAware {
    private static final Logger LOG = Logger.getInstance(BatchAddToKnowledgeBaseAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final VirtualFile virtualFile = this.getVirtualFileFromEvent(e);

        if (!this.validateInput(project, virtualFile)) {
            return;
        }

        if (!this.showConfirmationDialog(project)) {
            return;
        }

        this.startBatchProcessing(project, virtualFile);
    }

    private boolean validateInput(final Project project, final VirtualFile virtualFile) {
        if (null == project || null == virtualFile) {
            return false;
        }

        if (!virtualFile.isDirectory()) {
            this.showErrorDialog(project,
                    "Please select a folder to batch add files to the knowledge base.",
                    "Invalid Selection");
            return false;
        }

        return true;
    }

    private boolean showConfirmationDialog(final Project project) {
        final String message = this.buildConfirmationMessage();
        return this.showConfirmationDialog(project, message, "Batch Add to Knowledge Base");
    }

    private String buildConfirmationMessage() {
        return "This will add all supported code files in the selected folder to the knowledge base.\n\n" +
                FileValidationUtils.getSupportedFileTypesDescription() + "\n\n" +
                "This may take some time depending on the number of files. Continue?";
    }

    private void startBatchProcessing(final Project project, final VirtualFile virtualFile) {
        ProgressManager.getInstance().run(new BatchProcessingTask(project, virtualFile));
    }

    private class BatchProcessingTask extends Task.Modal {

        private final VirtualFile targetFolder;
        private final BatchProcessingStats stats;

        public BatchProcessingTask(final Project project, final VirtualFile virtualFile) {
            super(project, "Adding Files to Knowledge Base...", true);
            this.targetFolder = virtualFile;
            this.stats = new BatchProcessingStats();
        }

        @Override
        public void run(@NotNull final ProgressIndicator indicator) {
            try {
                executeProcessing(indicator);
            } catch (final Exception ex) {
                errorLog(ex, getClass());
            }
        }

        @Override
        public void onCancel() {
            // Non-blocking info notification for cancel
            ApplicationManager.getApplication().invokeLater(() ->
                    NotificationUtil.info(getProject(), "Batch add to Knowledge Base was canceled."));
        }

        private void executeProcessing(final ProgressIndicator indicator) {
            if (!performFileCountingPhase(indicator)) {
                return;
            }

            setupProgressTracking(indicator);
            performProcessingPhase(indicator);
            showCompletionSummary();
        }

        private boolean performFileCountingPhase(final ProgressIndicator indicator) {
            indicator.setText("Counting files...");
            countValidFiles(targetFolder, indicator);

            if (0 == this.stats.getTotalFiles()) {
                showNoFilesFoundDialog();
                return false;
            }
            return true;
        }

        private void setupProgressTracking(final ProgressIndicator indicator) {
            indicator.setIndeterminate(false);
            indicator.setFraction(0.0);
        }

        private void performProcessingPhase(final ProgressIndicator indicator) {
            processFolder(targetFolder, indicator);
        }

        private void showNoFilesFoundDialog() {
            ApplicationManager.getApplication().invokeLater(() -> {
                BatchAddToKnowledgeBaseAction.this.showInfoDialog(getProject(),
                        "No supported code files found in the selected folder.",
                        "No Files Found");
            });
        }

        private void showCompletionSummary() {
            ApplicationManager.getApplication().invokeLater(() -> {
                final String summaryMessage = stats.buildSummaryMessage();
                Messages.showInfoMessage(getProject(), summaryMessage, "Batch Processing Complete");
                // Lightweight completion notification
                NotificationUtil.info(getProject(), "Batch add complete: " + stats.processedFiles.size() + " processed, "
                        + stats.errorFiles.size() + " errors, " + stats.skippedFiles.size() + " skipped");
            });
        }

        private void countValidFiles(final VirtualFile folder, final ProgressIndicator indicator) {
            for (final VirtualFile child : folder.getChildren()) {
                indicator.checkCanceled();
                if (child.isDirectory()) {
                    countValidFiles(child, indicator);
                } else if (BatchAddToKnowledgeBaseAction.this.isValidFileForProcessing(child)) {
                    stats.incrementTotalFiles();
                }
            }
        }

        private void processFolder(final VirtualFile folder, final ProgressIndicator indicator) {
            for (final VirtualFile child : folder.getChildren()) {
                indicator.checkCanceled();
                if (child.isDirectory()) {
                    processFolder(child, indicator);
                } else {
                    processFile(child, indicator);
                }
            }
        }

        private void processFile(final VirtualFile virtualFile, final ProgressIndicator indicator) {
            if (!BatchAddToKnowledgeBaseAction.this.isValidFileForProcessing(virtualFile)) {
                stats.addSkippedFile(virtualFile.getName());
                return;
            }

            // Skip duplicates: do not add files that are already present in the Knowledge Base
            if (DuckDbService.isFileInKnowledgeBase(getProject(), virtualFile)) {
                stats.addSkippedFile(virtualFile.getName());
                return;
            }

            updateProgressIndicator(virtualFile, indicator);

            try {
                processFileContent(virtualFile);
                handleSuccessfulProcessing(virtualFile, indicator);
            } catch (final Exception ex) {
                handleProcessingError(virtualFile, ex, indicator);
            }
        }

        private void updateProgressIndicator(final VirtualFile virtualFile, final ProgressIndicator indicator) {
            indicator.setText("Processing: " + virtualFile.getName());
        }

        private void processFileContent(final VirtualFile virtualFile)
                throws IOException {
            addFileToVectorDb(virtualFile);
            addMethodsToVectorDb(virtualFile);
        }

        private void handleSuccessfulProcessing(final VirtualFile virtualFile, final ProgressIndicator indicator) {
            stats.addProcessedFile(virtualFile.getName());
            updateProgressFraction(indicator);
            // Notify for every file added
            ApplicationManager.getApplication().invokeLater(() ->
                    {
                        NotificationUtil.info(getProject(), "Added to Knowledge Base: " + virtualFile.getName());
                        // Publish KB changed event per file
                        ApplicationManager.getApplication().getMessageBus()
                                .syncPublisher(KnowledgeBaseChangedTopic.TOPIC)
                                .knowledgeBaseChanged(getProject());
                    }
            );
        }

        private void handleProcessingError(final VirtualFile virtualFile, final Exception ex, final ProgressIndicator indicator) {
            LOG.warn("Error processing file: " + virtualFile.getName(), ex);
            stats.addErrorFile(virtualFile.getName());
            updateProgressFraction(indicator);
        }

        private void updateProgressFraction(final ProgressIndicator indicator) {
            indicator.setFraction(stats.getProgressFraction());
        }

        private void addFileToVectorDb(final VirtualFile virtualFile)
                throws IOException {
            DuckDbService.addFileToKnowledgeBase(getProject(), virtualFile, null);
        }

        private void addMethodsToVectorDb(final VirtualFile virtualFile)
                throws IOException {
            DuckDbService.addMethodsToKnowledgeBase(getProject(), virtualFile, null);
        }

        private String extractFolderPath(final VirtualFile virtualFile) {
            return null != virtualFile.getParent() ? virtualFile.getParent().getPath() : "";
        }
    }

    private static class BatchProcessingStats {
        private final List<String> processedFiles = new ArrayList<>();
        private final List<String> skippedFiles = new ArrayList<>();
        private final List<String> errorFiles = new ArrayList<>();
        private final AtomicInteger totalFiles = new AtomicInteger(0);
        private final AtomicInteger processedCount = new AtomicInteger(0);

        public void incrementTotalFiles() {
            totalFiles.incrementAndGet();
        }

        public int getTotalFiles() {
            return totalFiles.get();
        }

        public void addProcessedFile(final String fileName) {
            processedFiles.add(fileName);
            processedCount.incrementAndGet();
        }

        public void addSkippedFile(final String fileName) {
            skippedFiles.add(fileName);
        }

        public void addErrorFile(final String fileName) {
            errorFiles.add(fileName);
            processedCount.incrementAndGet();
        }

        public double getProgressFraction() {
            return (double) processedCount.get() / totalFiles.get();
        }

        public String buildSummaryMessage() {
            final StringBuilder message = new StringBuilder();
            message.append("Batch processing completed!\n\n");
            message.append("Files processed: ").append(processedFiles.size()).append("\n");

            if (!errorFiles.isEmpty()) {
                message.append("Files with errors: ").append(errorFiles.size()).append("\n");
            }

            if (!skippedFiles.isEmpty()) {
                message.append("Files skipped: ").append(skippedFiles.size()).append("\n");
            }

            if (!errorFiles.isEmpty()) {
                message.append("\nFiles with errors:\n");
                for (final String errorFile : errorFiles) {
                    message.append("• ").append(errorFile).append("\n");
                }
            }

            return message.toString();
        }
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        // Enable only if we have a project and a directory is selected
        e.getPresentation().setEnabledAndVisible(this.validateProjectAndDirectory(e));
    }
}