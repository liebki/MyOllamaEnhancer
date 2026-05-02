package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.liebki.myollamaenhancer.utils.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class FileExplainAction extends ActionBase implements DumbAware {
    private static final Logger LOG = Logger.getInstance(FileExplainAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        this.project = e.getProject();
        this.virtualFile = this.getVirtualFileFromEvent(e);

        if (!this.isValidSelection()) {
            DialogUtil.showWarning("Please right-click a file in the Project View to run this action.", "No File Selected");
            return;
        }

        String filePath = virtualFile.getPath();
        String fileContent = this.readFileContent();

        LOG.info("filePath: " + filePath);

        if (null == fileContent || null == filePath) {
            LOG.warn("Could not read file content.");
            DialogUtil.showWarning("Could not read file content. Please ensure the file is accessible.", "File Error");
            return;
        }

        LOG.debug("fileContent (first 500 chars): " + fileContent.substring(0, Math.min(500, fileContent.length())));

        final String explainType = "Logic"; // default explanation type
        String language = this.detectLanguage();

        this.startExplanationTask(fileContent, explainType, language);
    }

    // Validates that a non-directory file is selected from the event
    private boolean isValidSelection() {
        return null != this.project && null != this.virtualFile && !virtualFile.isDirectory();
    }

    // Reads file content directly from VirtualFile under a ReadAction
    private String readFileContent() {
        return ReadAction.compute(() -> {
            try {
                return new String(virtualFile.contentsToByteArray(), virtualFile.getCharset());
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        });
    }

    // Detects language from the selected VirtualFile only
    private String detectLanguage() {
        return ReadAction.compute(() -> {
            try {
                final com.intellij.psi.PsiFile psi = com.intellij.psi.PsiManager.getInstance(project).findFile(virtualFile);
                return null != psi ? psi.getLanguage().getDisplayName() : "source code";
            } catch (Exception ex) {
                return "source code";
            }
        });
    }

    // Starts the modal progress task and orchestrates explanation generation
    private void startExplanationTask(String fileContent, String explainType, String language) {
        NotificationUtil.info(project, "Generating file explanation...");
        ProgressManager.getInstance().run(new Task.Modal(project, "Generating Explanation...", true) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                try {
                    FileExplainAction.this.setupIndicator(indicator, explainType);

                    final boolean isLargeFile = FileChunkingUtil.isLargeFile(fileContent);
                    FileExplainAction.this.updateIndicatorForLargeFile(indicator, isLargeFile);

                    final String explanation = FileExplainAction.this.generateExplanation(fileContent, project, explainType, language);

                    FileExplainAction.this.showResultOnEdt(explanation);
                } catch (final Exception ex) {
                    FileExplainAction.this.errorLog(ex, getClass());
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

    // Initializes indicator texts
    private void setupIndicator(final ProgressIndicator indicator, final String explainType) {
        indicator.setIndeterminate(false);
        indicator.setFraction(0.0);
        indicator.setText("Analyzing " + explainType.toLowerCase() + " of the file...");
        indicator.setText2("Preparing to process file...");
    }

    // Updates indicator if the file is large
    private void updateIndicatorForLargeFile(final ProgressIndicator indicator, final boolean isLargeFile) {
        if (isLargeFile) {
            indicator.setText("Processing large file in chunks...");
            indicator.setText2("This may take a while for large files");
        }
    }

    // Shows the explanation result on the EDT
    private void showResultOnEdt(final String explanation) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (null == explanation) {
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
        final VirtualFile vf = this.getVirtualFileFromEvent(e);

        final boolean isFile = null != vf && !vf.isDirectory();
        e.getPresentation().setEnabledAndVisible(isFile);
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private String generateExplanation(final String fileContent, final Project project, final String explainType, final String language) {
        try {
            final Map<String, Object> structuredOutput = StructuredOutputSchemas.createExplanationSchema();
            final String systemPrompt = this.buildExplanationPrompt(explainType, language);
            final String enhancedPrompt = StructuredPromptEnhancer.enhanceForFileExplanation(systemPrompt);
            
            // Handle large files by chunking
            if (FileChunkingUtil.isLargeFile(fileContent)) {
                LOG.info("Large file detected, processing with chunking");
                return generateExplanationForChunks(fileContent, enhancedPrompt, structuredOutput);
            } else {
                // For smaller files, process normally
                final OllamaAPIUtil.ThinkContent response = OllamaAPIUtil.generateStructuredOllamaResponseSync(enhancedPrompt, fileContent, structuredOutput);
                LOG.debug("Raw structured output: " + response);
                return StructuredResponseParser.parseExplanationResponse(response.visibleContent());
            }

        } catch (final Exception e) {
            LOG.warn("Error generating explanation: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Generate explanation for large files by processing chunks separately and combining results.
     * 
     * @param fileContent The full file content
     * @param enhancedPrompt The system prompt for explanation
     * @param structuredOutput The structured output schema
     * @return Combined explanation from all chunks
     */
    private String generateExplanationForChunks(final String fileContent, final String enhancedPrompt,
                                                final Map<String, Object> structuredOutput) {
        try {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            final List<String> chunks = FileChunkingUtil.splitIntoChunks(fileContent);
            LOG.info("Split file into " + chunks.size() + " chunks");
            
            final String combinedExplanations = this.processAllChunks(chunks, indicator, structuredOutput);
            return this.generateFinalAnalysis(combinedExplanations, structuredOutput);
            
        } catch (final Exception e) {
            LOG.warn("Error generating chunked explanation: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Processes all chunks and returns their combined explanations.
     */
    private String processAllChunks(final List<String> chunks, final ProgressIndicator indicator,
                                    final Map<String, Object> structuredOutput) throws Exception {
        final StringBuilder combinedExplanations = new StringBuilder();
        
        for (int i = 0; i < chunks.size(); i++) {
            this.updateProgress(indicator, i, chunks.size());
            final String chunk = chunks.get(i);

            this.logChunkProcessing(i, chunks.size(), chunk);
            this.addSectionHeader(combinedExplanations, i, chunks.size());
            
            final String explanation = this.processSingleChunk(chunk, structuredOutput);
            if (null != explanation) {
                combinedExplanations.append(explanation).append("\n\n");
            }

            this.checkCancellation(indicator);
        }
        
        return combinedExplanations.toString();
    }

    /**
     * Processes a single chunk and returns its explanation.
     */
    private String processSingleChunk(final String chunk, final Map<String, Object> structuredOutput) throws Exception {
        final String chunkPrompt = this.buildChunkPrompt();
        final OllamaAPIUtil.ThinkContent response = OllamaAPIUtil.generateStructuredOllamaResponseSync(chunkPrompt, chunk, structuredOutput);

        this.logResponseDetails(response.visibleContent());
        return StructuredResponseParser.parseExplanationResponse(response.visibleContent());
    }

    /**
     * Builds the prompt for chunk analysis.
     */
    private String buildChunkPrompt() {
        final String chunkPromptBase = "Analyze this section of code and explain its functionality in detail. " +
                "Focus on what this specific code does, any key components or patterns present, and how it contributes to the overall file.\n\n" +
                "File: " + (null != this.virtualFile ? virtualFile.getName() : "") + "\n" +
                (null != this.project ? "Project: " + project.getName() + "\n" : "") + "\n" +
                "Your explanation should be thorough and cover all important aspects of this code section. " +
                "Aim for at least 2-3 paragraphs of detailed analysis. " +
                "Do not mention anything about file size, chunks, or processing details.\n\n" +
                "Code:\n\n";
        return StructuredPromptEnhancer.enhanceForFileExplanation(chunkPromptBase);
    }

    /**
     * Generates the final analysis from combined chunk explanations.
     */
    private String generateFinalAnalysis(final String combinedExplanations, final Map<String, Object> structuredOutput) throws Exception {
        this.logCombinedContent(combinedExplanations);
        
        final String analysisPrompt = this.buildAnalysisPrompt(combinedExplanations);
        final String systemPrompt = this.buildSystemPrompt();
        
        final OllamaAPIUtil.ThinkContent analysisResponse = OllamaAPIUtil.generateStructuredOllamaResponseSync(
                systemPrompt, analysisPrompt, structuredOutput);

        this.logFinalAnalysis(analysisResponse.visibleContent());
        final String finalExplanation = StructuredResponseParser.parseExplanationResponse(analysisResponse.visibleContent());
        
        return this.ensureDetailedExplanation(finalExplanation, combinedExplanations, structuredOutput);
    }

    /**
     * Ensures the explanation is detailed enough, regenerating if necessary.
     */
    private String ensureDetailedExplanation(final String finalExplanation, final String combinedExplanations,
                                             final Map<String, Object> structuredOutput) throws Exception {
        if (null != finalExplanation && 200 > finalExplanation.length()) {
            System.out.println("[FileExplainAction] Final explanation is brief, generating more detailed version");
            return this.generateDetailedFallback(combinedExplanations, structuredOutput);
        }
        return finalExplanation;
    }

    /**
     * Generates a detailed fallback explanation when the initial one is too brief.
     */
    private String generateDetailedFallback(final String combinedExplanations, final Map<String, Object> structuredOutput) throws Exception {
        final String detailedPrompt = this.buildAnalysisPrompt(combinedExplanations);
        final String detailedSystemPrompt = this.buildSystemPrompt();

        this.logFallbackDetails(detailedPrompt, detailedSystemPrompt);
        
        final OllamaAPIUtil.ThinkContent detailedResponse = OllamaAPIUtil.generateStructuredOllamaResponseSync(
                detailedSystemPrompt, detailedPrompt, structuredOutput);

        this.logDetailedResponse(detailedResponse.visibleContent());
        return StructuredResponseParser.parseExplanationResponse(detailedResponse.visibleContent());
    }

    /**
     * Builds the analysis prompt for synthesizing chunk explanations.
     */
    private String buildAnalysisPrompt(final String combinedExplanations) {
        return "Based on the provided section-by-section explanations of a code file, synthesize a comprehensive overview that covers all important aspects of the complete file. Your response must be detailed and cover the following aspects:\n" +
                "1. Start with a clear one-sentence summary of the file's main purpose.\n" +
                "2. Explain the key components, classes, or functions and their responsibilities, synthesizing information from all sections.\n" +
                "3. Describe how these components interact with each other across different sections of the file.\n" +
                "4. Highlight any important algorithms, patterns, or design decisions that emerge from the complete implementation.\n" +
                "5. Mention any notable dependencies or external integrations used throughout the file.\n" +
                "6. Point out any potential issues or areas for improvement based on the complete implementation.\n\n" +
                "File: " + (null != this.virtualFile ? virtualFile.getName() : "") + "\n" +
                (null != this.project ? "Project: " + project.getName() + "\n" : "") + "\n" +
                "Your explanation should be thorough and cover all important aspects of the complete code file. " +
                "Aim for at least 2-3 paragraphs of detailed analysis that synthesizes the information from all provided sections. " +
                "Do not mention anything about file size, chunks, or processing details.\n\n" +
                "Section explanations to synthesize:\n\n" + combinedExplanations;
    }

    /**
     * Builds the system prompt for analysis.
     */
    private String buildSystemPrompt() {
        return "You are a senior software engineer analyzing a " +
                (null != this.virtualFile ? virtualFile.getFileType().getName() : "source code") + " file. " +
                "Provide a thorough, detailed explanation of this code. Your response should include:\n" +
                "1. A high-level overview of the file's purpose and functionality\n" +
                "2. Detailed explanation of the main components, classes, and functions\n" +
                "3. Description of key algorithms, patterns, or architectural decisions\n" +
                "4. Any important dependencies or interactions with other parts of the system\n" +
                "5. Potential edge cases or error handling approaches\n\n" +
                "Be comprehensive but organized in your explanation. Break down complex concepts and provide examples " +
                "when helpful. If the file is part of a larger system, explain how it fits into the overall architecture.";
    }

    /**
     * Updates progress indicator.
     */
    private void updateProgress(final ProgressIndicator indicator, final int current, final int total) {
        if (null != indicator) {
            if (indicator.isCanceled()) {
                throw new RuntimeException("Operation cancelled by user");
            }
            indicator.setFraction((double) current / total);
            indicator.setText2(String.format("Processing part %d of %d...", current + 1, total));
        }
    }

    /**
     * Logs chunk processing details.
     */
    private void logChunkProcessing(final int chunkIndex, final int totalChunks, final String chunk) {
        final int chunkSizeKB = chunk.length() / 1024;
        LOG.debug("Processing chunk " + (chunkIndex + 1) + " of " + totalChunks + ", size: " + chunkSizeKB + " KB");
    }

    /**
     * Adds section header if multiple chunks exist.
     */
    private void addSectionHeader(final StringBuilder builder, final int chunkIndex, final int totalChunks) {
        if (1 < totalChunks) {
            builder.append("[Section ")
                .append(chunkIndex + 1)
                .append("]\n");
        }
    }

    /**
     * Logs response details.
     */
    private void logResponseDetails(final String response) {
        LOG.debug("Chunk raw response length: " + response.length());
        LOG.debug("Chunk raw response preview: " + (200 < response.length() ? response.substring(0, 200) + "..." : response));
    }

    /**
     * Logs combined content details.
     */
    private void logCombinedContent(final String combinedContent) {
        final int contentLength = combinedContent.length();
        LOG.debug("Combined explanations length: " + contentLength);
        LOG.debug("Combined content preview: " + (500 < combinedContent.length() ? combinedContent.substring(0, 500) + "..." : combinedContent));
    }

    /**
     * Logs final analysis details.
     */
    private void logFinalAnalysis(final String analysisResponse) {
        LOG.debug("Final summary raw response length: " + analysisResponse.length());
        LOG.debug("Final summary raw response preview: " + (200 < analysisResponse.length() ? analysisResponse.substring(0, 200) + "..." : analysisResponse));
    }

    /**
     * Logs fallback details.
     */
    private void logFallbackDetails(final String detailedPrompt, final String detailedSystemPrompt) {
        LOG.debug("Fallback detailed prompt length: " + detailedPrompt.length());
        LOG.debug("Fallback detailed prompt preview: " + (500 < detailedPrompt.length() ? detailedPrompt.substring(0, 500) + "..." : detailedPrompt));
        LOG.debug("Fallback detailed system prompt: " + detailedSystemPrompt);
    }

    /**
     * Logs detailed response.
     */
    private void logDetailedResponse(final String detailedResponse) {
        LOG.debug("Fallback detailed raw response length: " + detailedResponse.length());
        LOG.debug("Fallback detailed raw response preview: " + (200 < detailedResponse.length() ? detailedResponse.substring(0, 200) + "..." : detailedResponse));
    }

    /**
     * Checks for cancellation.
     */
    private void checkCancellation(final ProgressIndicator indicator) {
        if (null != indicator) {
            indicator.checkCanceled();
        }
    }

    private String buildExplanationPrompt(final String explainType, final String language) {
        return "Provide a comprehensive explanation of this " + language + " file. Your response must be detailed and cover the following aspects:\n" +
               "1. Start with a clear one-sentence summary of the file's main purpose.\n" +
               "2. Explain the key components, classes, or functions and their responsibilities.\n" +
               "3. Describe how these components interact with each other.\n" +
               "4. Highlight any important algorithms, patterns, or design decisions.\n" +
               "5. Mention any notable dependencies or external integrations.\n" +
               "6. Point out any potential issues or areas for improvement.\n\n" +
               "File: " + (null != this.virtualFile ? virtualFile.getName() : "") + "\n" +
               (null != this.project ? "Project: " + project.getName() + "\n" : "") + "\n" +
               "Your explanation should be thorough and cover all important aspects of the code. " +
               "Aim for at least 2-3 paragraphs of detailed analysis. " +
               "Do not mention anything about file size, chunks, or processing details.";
    }
    
    // Add these instance variables to store the project and virtual file
    private Project project;
    private VirtualFile virtualFile;


} 