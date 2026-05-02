package de.liebki.myollamaenhancer.utils;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utility class to generate project README files based on code analysis.
 */
public enum READMEGenerator {
    ;

    // File extensions to include in README generation
    private static final String[] INCLUDED_EXTENSIONS = {
            "java", "py", "js", "ts", "jsx", "tsx", "html", "css", "scss", "sql", "xml", "yml", "yaml", "json", "md", "php", "php3", "php4", "php5", "php6", "php7", "php8", "phps"
    };

    // Directories to exclude from README generation
    private static final String[] EXCLUDED_DIRECTORIES = {
            "node_modules", "build", "dist", "target", ".git", ".idea", ".vscode", "out", "bin", "intellijPlatform", "gradle", ".gradle"
    };

    /**
     * Generate a README for the project asynchronously.
     *
     * @param project  The current project
     * @param callback The callback to receive the generated README
     */
    public static void generateREADME(final Project project, final Consumer<String> callback) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating README...", true) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Analyzing project structure...");

                try {
                    final String readmeContent = generateREADMEContent(project, indicator);
                    callback.accept(readmeContent);
                } catch (final Exception e) {
                    callback.accept("Error generating README: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Generate a README for the project synchronously.
     * This is intended for callers that already run within a progress task (e.g., Task.Modal)
     * and want to block until generation is complete while reporting progress via the provided indicator.
     *
     * @param project   The current project
     * @param indicator Progress indicator to update and to observe cancellation
     * @return Generated README content
     * @throws Exception if there's an error during generation
     */
    public static String generateREADMEBlocking(final Project project, final ProgressIndicator indicator) throws Exception {
        return generateREADMEContent(project, indicator);
    }

    /**
     * Generate README content by analyzing the project structure.
     *
     * @param project   The current project
     * @param indicator Progress indicator for UI updates
     * @return Generated README content
     * @throws Exception if there's an error during generation
     */
    private static String generateREADMEContent(final Project project, final ProgressIndicator indicator) throws Exception {
        final String projectBasePath = project.getBasePath();
        if (null == projectBasePath) {
            throw new IllegalStateException("Project base path is null");
        }

        final Path projectPath = Paths.get(projectBasePath);
        final List<VirtualFile> files = new ArrayList<>();
        collectProjectFiles(projectPath, files, indicator);

        indicator.setText("Generating file summaries...");
        final List<String> fileSummaries = new ArrayList<>();
        
        // Process files with progress indication
        for (int i = 0; i < files.size(); i++) {
            if (indicator.isCanceled()) {
                return "README generation cancelled by user.";
            }
            
            final VirtualFile file = files.get(i);
            indicator.setFraction((double) i / files.size());
            indicator.setText2("Processing " + file.getName());
            
            final String summary = generateFileSummary(file);
            if (null != summary && !summary.isEmpty()) {
                fileSummaries.add(summary);
            }
        }

        indicator.setText("Generating final README...");
        indicator.setFraction(0.9);
        
        return combineSummariesIntoREADME(fileSummaries, project.getName());
    }

    /**
     * Collect all relevant project files recursively.
     *
     * @param directory The directory to traverse
     * @param files     The list to populate with files
     * @param indicator Progress indicator
     */
    private static void collectProjectFiles(final Path directory, final List<VirtualFile> files, final ProgressIndicator indicator) {
        if (READMEGenerator.shouldSkipCollection(indicator)) return;
        
        try {
            final VirtualFile virtualDir = READMEGenerator.getVirtualDirectory(directory);
            if (READMEGenerator.isInvalidDirectory(virtualDir)) return;

            if (READMEGenerator.isExcludedDirectory(directory)) return;

            READMEGenerator.processDirectoryContents(virtualDir, files, indicator);
            
        } catch (final Exception e) {
            System.err.println("Error collecting files from directory " + directory + ": " + e.getMessage());
        }
    }

    /**
     * Checks if collection should be skipped due to cancellation.
     */
    private static boolean shouldSkipCollection(final ProgressIndicator indicator) {
        return null != indicator && indicator.isCanceled();
    }

    /**
     * Gets the virtual file representation of a directory.
     */
    private static VirtualFile getVirtualDirectory(final Path directory) {
        return VirtualFileManager.getInstance().findFileByNioPath(directory);
    }

    /**
     * Checks if the directory is invalid (null or not a directory).
     */
    private static boolean isInvalidDirectory(final VirtualFile virtualDir) {
        return null == virtualDir || !virtualDir.isDirectory();
    }

    /**
     * Checks if the directory should be excluded based on its name.
     */
    private static boolean isExcludedDirectory(final Path directory) {
        final String dirName = directory.getFileName().toString();
        for (final String excludedDir : EXCLUDED_DIRECTORIES) {
            if (dirName.equals(excludedDir)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Processes all contents of a directory.
     */
    private static void processDirectoryContents(final VirtualFile virtualDir, final List<VirtualFile> files,
                                                 final ProgressIndicator indicator) {
        final VirtualFile[] children = virtualDir.getChildren();
        if (null == children) return;
        
        for (final VirtualFile child : children) {
            if (READMEGenerator.shouldSkipCollection(indicator)) return;
            READMEGenerator.processChildFile(child, files, indicator);
        }
    }

    /**
     * Processes a single child file or directory.
     */
    private static void processChildFile(final VirtualFile child, final List<VirtualFile> files,
                                         final ProgressIndicator indicator) {
        if (child.isDirectory()) {
            READMEGenerator.processSubdirectory(child, files, indicator);
        } else {
            READMEGenerator.processRegularFile(child, files);
        }
    }

    /**
     * Processes a subdirectory recursively.
     */
    private static void processSubdirectory(final VirtualFile child, final List<VirtualFile> files,
                                            final ProgressIndicator indicator) {
        collectProjectFiles(child.toNioPath(), files, indicator);
    }

    /**
     * Processes a regular file and adds it to the list if it should be included.
     */
    private static void processRegularFile(final VirtualFile child, final List<VirtualFile> files) {
        if (shouldIncludeFile(child)) {
            files.add(child);
        }
    }

    /**
     * Determine if a file should be included in README generation.
     *
     * @param file The file to check
     * @return true if the file should be included
     */
    private static boolean shouldIncludeFile(final VirtualFile file) {
        // Skip hidden files
        if (file.getName().startsWith(".")) {
            return false;
        }

        // Check file extension
        final String extension = file.getExtension();
        if (null == extension || extension.isEmpty()) {
            return false;
        }

        for (final String includedExt : INCLUDED_EXTENSIONS) {
            if (includedExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Generate a summary for a file using the LLM.
     *
     * @param file The file to summarize
     * @return A summary of the file
     * @throws Exception if there's an error during generation
     */
    private static String generateFileSummary(final VirtualFile file) throws Exception {
        try {
            // Read file content
            final String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
            
            // For very large files, we'll use just the beginning and end to understand the structure
            // This helps us avoid getting lost in implementation details
            final String fileContent;
            if (10000 < content.length()) { // ~10KB limit
                System.out.println("[READMEGenerator] Large file detected: " + file.getName() + ", using beginning and end");
                final int segmentSize = 4000;
                final String beginning = content.substring(0, Math.min(segmentSize, content.length()));
                final String end = segmentSize < content.length() ?
                    content.substring(Math.max(content.length() - segmentSize, segmentSize)) : "";
                fileContent = beginning + "\n...\n" + end;
            } else {
                fileContent = content;
            }
            
            // Create user prompt
            final String userPrompt = "File: " + file.getPath() + "\n\nContent:\n" + fileContent;
            
            // Create system prompt focused on core functionality
            final String sysPrompt = "You are an expert technical documentation writer. Your task is to analyze a code file and provide an accurate summary of its main purpose and key functionality.\n\n" +
                    "Please provide your response in this EXACT format:\n" +
                    "File: " + file.getName() + "\n" +
                    "Purpose: [Brief description of what this file does - focus on its main purpose]\n" +
                    "Key Features:\n" +
                    "- [Bullet point 1 with specific functionality]\n" +
                    "- [Bullet point 2 with specific functionality]\n" +
                    "- [Additional bullet points as needed]\n\n" +
                    "Rules:\n" +
                    "1. Focus on what the code ACTUALLY does, not what you assume it might do\n" +
                    "2. Identify the main purpose and core features\n" +
                    "3. Be concise but specific\n" +
                    "4. Use clear, descriptive language\n" +
                    "5. Do NOT make things up or hallucinate\n" +
                    "6. Do NOT include implementation details, configuration, or utility functions unless they are core to the file's purpose\n" +
                    "7. Respond ONLY with the format above, nothing else";

            // Call the LLM
            System.out.println("[READMEGenerator] Calling LLM to summarize file: " + file.getName());
            final OllamaAPIUtil.ThinkContent result = OllamaAPIUtil.generateOllamaResponseSync(userPrompt, sysPrompt);
            System.out.println("[READMEGenerator] LLM response for " + file.getName() + ": " + result);
            System.out.println(result.thinkContent());
            return result.visibleContent();
        } catch (final IOException e) {
            System.err.println("Error reading file " + file.getPath() + ": " + e.getMessage());
            return null;
        } catch (final Exception e) {
            System.err.println("Error generating summary for file " + file.getPath() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Combine file summaries into a structured README.
     *
     * @param fileSummaries The list of file summaries
     * @param projectName   The name of the project
     * @return A structured README
     * @throws Exception if there's an error during combination
     */
    private static String combineSummariesIntoREADME(final List<String> fileSummaries, final String projectName) throws Exception {
        if (fileSummaries.isEmpty()) {
            return "# " + projectName + "\n\nNo files found or processed for README generation.";
        }

        // Combine summaries into a single prompt
        final StringBuilder combinedSummaries = new StringBuilder();
        for (final String summary : fileSummaries) {
            if (null != summary && !summary.isEmpty()) {
                combinedSummaries.append(summary).append("\n\n");
            }
        }

        final String userPrompt = "Project Name: " + projectName + "\n\nFile Summaries:\n" + combinedSummaries;

        final String sysPrompt = "You are an expert technical documentation writer. Your task is to create an accurate and focused README.md file based on file summaries.\n\n" +
                "Create a well-structured README with these sections:\n" +
                "1. # Project Title - Use the provided project name\n" +
                "2. ## Overview - A clear description of what the project does (1-2 sentences)\n" +
                "3. ## Features - Bullet points of core features based on the file summaries\n" +
                "4. ## Installation - Simple installation instructions\n" +
                "5. ## Usage - Basic usage examples\n" +
                "6. ## Project Structure - Brief summary of important files and directories\n" +
                "7. ## Contributing - Generic contribution guidelines\n" +
                "8. ## License - Generic open source license note\n\n" +
                "Follow these rules:\n" +
                "1. Base your response ONLY on the provided file summaries\n" +
                "2. Focus on the core functionality and main purpose of the project\n" +
                "3. Do NOT make things up or hallucinate\n" +
                "4. Be concise but informative\n" +
                "5. Use clear, descriptive language\n" +
                "6. Respond ONLY with the README content in Markdown format\n" +
                "7. Do NOT add any extra text, explanations, or formatting\n" +
                "8. Use proper Markdown formatting with appropriate headers";

        System.out.println("[READMEGenerator] Calling LLM to combine summaries into README");

        // Call the LLM
        final OllamaAPIUtil.ThinkContent result = OllamaAPIUtil.generateOllamaResponseSync(userPrompt, sysPrompt);
        System.out.println("[READMEGenerator] LLM response for combined README: " + result.visibleContent());
        System.out.println(result.thinkContent());
        return null != result ? result.visibleContent() : "# " + projectName + "\n\nREADME could not be generated due to an error.";
    }
}
