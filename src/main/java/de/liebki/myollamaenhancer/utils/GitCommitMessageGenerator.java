package de.liebki.myollamaenhancer.utils;

import com.intellij.diff.comparison.ComparisonManager;
import com.intellij.diff.comparison.ComparisonPolicy;
import com.intellij.diff.fragments.LineFragment;
import com.intellij.openapi.progress.DumbProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class to generate Git commit messages based on VCS changes.
 */
public enum GitCommitMessageGenerator {
    ;

    /**
     * Get the diff content for a change.
     *
     * @param change The VCS change object
     * @return The diff content as a string, or null if not available
     */
    private static String getDiffContent(final Change change) {
        try {
            final ContentRevision beforeRevision = change.getBeforeRevision();
            final ContentRevision afterRevision = change.getAfterRevision();

            final String filePath = extractFilePath(beforeRevision, afterRevision);

            if (null == beforeRevision && null == afterRevision) {
                return null;
            }

            if (null == beforeRevision) {
                return formatNewFileDiff(filePath, afterRevision);
            }

            if (null == afterRevision) {
                return formatDeletedFileDiff(filePath, beforeRevision);
            }

            final String beforeContent = beforeRevision.getContent();
            final String afterContent = afterRevision.getContent();

            if (null != beforeContent && beforeContent.equals(afterContent)) {
                return formatIdenticalContentDiff(filePath, afterContent);
            }

            return formatModifiedFileDiff(filePath, beforeContent, afterContent);
        } catch (final Exception e) {
            System.err.println("[GitCommitMessageGenerator] Error getting diff content: " + e.getMessage());
            return null;
        }
    }

    private static String extractFilePath(final ContentRevision beforeRevision, final ContentRevision afterRevision) {
        return null != afterRevision ? afterRevision.getFile().getPath() :
                (null != beforeRevision ? beforeRevision.getFile().getPath() : "Unknown file");
    }

    private static String formatNewFileDiff(final String filePath, final ContentRevision afterRevision) throws VcsException {
        final StringBuilder diff = new StringBuilder();
        diff.append("File: ").append(filePath).append(" (NEW FILE)\n\n");
        diff.append("Content:\n");

        final String content = afterRevision.getContent();
        if (null != content) {
            final String[] lines = content.split("\n");
            for (final String line : lines) {
                diff.append("+").append(line).append("\n");
            }
        }

        return diff.toString();
    }

    private static String formatDeletedFileDiff(final String filePath, final ContentRevision beforeRevision) throws VcsException {
        final StringBuilder diff = new StringBuilder();
        diff.append("File: ").append(filePath).append(" (DELETED FILE)\n\n");
        diff.append("Previous content:\n");

        final String content = beforeRevision.getContent();
        if (null != content) {
            final String[] lines = content.split("\n");
            for (final String line : lines) {
                diff.append("-").append(line).append("\n");
            }
        }

        return diff.toString();
    }

    private static String formatIdenticalContentDiff(final String filePath, final String afterContent) {
        final StringBuilder diff = new StringBuilder();
        diff.append("File: ").append(filePath).append(" (NEW FILE with identical content to previous version)\n\n");
        diff.append("Content:\n");

        final String[] lines = afterContent.split("\n");
        for (final String line : lines) {
            diff.append("+").append(line).append("\n");
        }

        return diff.toString();
    }

    private static String formatModifiedFileDiff(final String filePath, final String beforeContent, final String afterContent) {
        final StringBuilder diff = new StringBuilder();
        diff.append("File: ").append(filePath).append(" (MODIFIED FILE)\n\n");
        diff.append("Changes (lines starting with '-' were removed, lines starting with '+' were added):\n");

        final String unifiedDiff = generateUnifiedDiff(beforeContent, afterContent);
        diff.append(unifiedDiff);

        return diff.toString();
    }

    /**
     * Generate a unified diff between two texts using IntelliJ's built-in ComparisonManager.
     *
     * @param before The text before changes
     * @param after  The text after changes
     * @return A unified diff representation
     */
    private static String generateUnifiedDiff(final String before, final String after) {
        try {
            final ComparisonManager comparisonManager = ComparisonManager.getInstance();
            final java.util.List<LineFragment> fragments = comparisonManager.compareLines(
                    null != before ? before : "",
                    null != after ? after : "",
                    ComparisonPolicy.DEFAULT,
                    new DumbProgressIndicator()
            );

            final StringBuilder diff = new StringBuilder();
            final String[] beforeLines = null != before ? before.split("\n") : new String[0];
            final String[] afterLines = null != after ? after.split("\n") : new String[0];

            for (final LineFragment fragment : fragments) {
                final int startLine1 = fragment.getStartLine1();
                final int endLine1 = fragment.getEndLine1();
                final int startLine2 = fragment.getStartLine2();
                final int endLine2 = fragment.getEndLine2();

                for (int i = startLine1; i < endLine1; i++) {
                    diff.append("-").append(beforeLines[i]).append("\n");
                }
                for (int i = startLine2; i < endLine2; i++) {
                    diff.append("+").append(afterLines[i]).append("\n");
                }
            }

            return diff.toString();
        } catch (final Exception e) {
            System.err.println("[GitCommitMessageGenerator] Error generating unified diff: " + e.getMessage());
            return "Error generating reliable diff.";
        }
    }

    /**
     * Generate a commit message based on the provided changes.
     *
     * @param project The current project
     * @param changes The collection of changes to process
     * @return A generated commit message
     * @throws Exception if there's an error during generation
     */
    public static String generateCommitMessage(final Project project, final Collection<Change> changes) throws Exception {
        System.out.println("[GitCommitMessageGenerator] Starting commit message generation for " + changes.size() + " changes");

        final List<String> fileSummaries = new ArrayList<>();

        // Process each change
        int changeIndex = 0;
        for (final Change change : changes) {
            changeIndex++;
            System.out.println("[GitCommitMessageGenerator] Processing change " + changeIndex + " of " + changes.size());

            final String fileSummary = processChange(project, change);
            if (null != fileSummary && !fileSummary.isEmpty()) {
                System.out.println("[GitCommitMessageGenerator] Generated summary: " + fileSummary);
                fileSummaries.add(fileSummary);
            } else {
                System.out.println("[GitCommitMessageGenerator] No summary generated for change");
            }
        }

        System.out.println("[GitCommitMessageGenerator] Combining " + fileSummaries.size() + " file summaries");

        // Combine summaries into a commit message
        final String commitMessage = combineSummaries(fileSummaries);
        System.out.println("[GitCommitMessageGenerator] Final commit message: " + commitMessage);

        return commitMessage;
    }

    /**
     * Process a single change to generate a summary.
     *
     * @param project The current project
     * @param change  The change to process
     * @return A summary of the change
     * @throws Exception if there's an error during processing
     */
    private static String processChange(final Project project, final Change change) throws Exception {
        // Get file path
        final String filePath = getFilePath(change);
        if (null == filePath) {
            return null;
        }

        // Generate a summary for this file using the diff
        return generateFileSummary(project, filePath, change);
    }

    private static String getFilePath(final Change change) {
        final ContentRevision beforeRevision = change.getBeforeRevision();
        final ContentRevision afterRevision = change.getAfterRevision();

        return null != afterRevision ? afterRevision.getFile().getPath() :
                (null != beforeRevision ? beforeRevision.getFile().getPath() : null);
    }

    /**
     * Generate a summary for a file change using the LLM.
     *
     * @param project  The current project
     * @param filePath The path of the file that changed
     * @param change   The VCS change object containing the diff
     * @return A summary of the changes
     * @throws Exception if there's an error during generation
     */
    private static String generateFileSummary(final Project project, final String filePath, final Change change) throws Exception {
        System.out.println("[GitCommitMessageGenerator] Generating file summary for: " + filePath);

        final String diffContent = getDiffContent(change);
        System.out.println("[GitCommitMessageGenerator] Diff content length: " + (null != diffContent ? diffContent.length() : 0));

        if (null == diffContent || diffContent.isEmpty()) {
            return "Updated " + filePath;
        }

        final String userPrompt = "File: " + filePath + "\n\n" +
                "Diff:\n" + diffContent + "\n\n" +
                "Please provide a concise, high-level summary of the changes in this file.";

        final String sysPrompt = "You are an expert at summarizing code changes for Git commits. Your task is to write a single, high-level sentence summarizing the provided diff.\n\n" +
                "Follow these rules:\n" +
                "- Your summary must be a single, concise sentence.\n" +
                "- Focus on the overall change (e.g., 'refactored the user authentication flow', 'added new utility functions for data processing', 'fixed a null pointer exception').\n" +
                "- IMPORTANT: Do not list every single new function or variable. Summarize the changes at a high level. For example, instead of 'added functions a, b, and c', say 'added several new utility functions'.\n" +
                "- Be specific about the *purpose* of the change, not just the action.\n\n" +
                "Example summaries:\n" +
                "- Refactored the data processing logic for performance.\n" +
                "- Added several new utility functions for string and array manipulation.\n" +
                "- Corrected a null pointer exception in the user validation service.\n\n" +
                "IMPORTANT: Respond with ONLY the summary sentence. Do not add any extra text, explanations, or formatting.";

        System.out.println("[GitCommitMessageGenerator] Calling LLM for file summary");

        OllamaAPIUtil.ThinkContent resultObj = OllamaAPIUtil.generateOllamaResponseSync(userPrompt, sysPrompt);
        String result = resultObj.visibleContent();

        System.out.println("[GitCommitMessageGenerator] LLM response for file summary: " + result);

        if (null != result && !result.isEmpty()) {
            final String[] lines = result.split("\n");
            for (final String line : lines) {
                final String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) {
                    return trimmedLine;
                }
            }
        }

        return "Updated " + filePath;
    }

    /**
     * Combine file summaries into a conventional commit message.
     *
     * @param fileSummaries The list of file summaries
     * @return A conventional commit message
     * @throws Exception if there's an error during combination
     */
    private static String combineSummaries(final List<String> fileSummaries) throws Exception {
        if (null == fileSummaries || fileSummaries.isEmpty()) {
            return "chore: no changes detected";
        }

        System.out.println("[GitCommitMessageGenerator] Combining " + fileSummaries.size() + " file summaries");

        final String userPrompt = "Here are the summaries of the file changes:\n" +
                String.join("\n", fileSummaries);

        final String sysPrompt = "You are an expert at writing conventional commit messages. Your task is to combine file summaries into a single, well-formatted commit message.\n\n" +
                "The output MUST follow this structure:\n" +
                "1. A short, concise subject line in the format 'type: subject' (e.g., 'feat: add user authentication').\n" +
                "2. A blank line.\n" +
                "3. A high-level, bulleted list summarizing the main changes. Each bullet point should start with a '*' and a space.\n\n" +
                "CRITICAL RULES:\n" +
                "- Your response MUST be based SOLELY on the provided file summaries. DO NOT make up or assume any changes that are not explicitly mentioned.\n" +
                "- The subject line must accurately reflect the actual changes in the file summaries.\n" +
                "- Each bullet point must directly correspond to a specific change mentioned in the file summaries.\n" +
                "- If the file summaries mention utility functions, the commit message should be about those utility functions, NOT about UI, styling, or any other unrelated changes.\n" +
                "- Use present tense and imperative mood (e.g., 'add' not 'added').\n" +
                "- If summarizing multiple unrelated changes, use 'chore:' as the type.\n\n" +
                "IMPORTANT: Your response must ONLY include the structured commit message. Do not add any extra text, explanations, or formatting. Never include sentences like 'Note:' or 'Reasoning:' or any other meta-commentary.";

        System.out.println("[GitCommitMessageGenerator] Calling LLM to combine summaries");

        // Call the LLM
        OllamaAPIUtil.ThinkContent resultObj = OllamaAPIUtil.generateOllamaResponseSync(userPrompt, sysPrompt);
        String result = resultObj.visibleContent();

        System.out.println("[GitCommitMessageGenerator] LLM response for combined message: " + result);

        // Ensure the commit message follows conventional format
        if (null != result && !result.isEmpty()) {
            result = formatCommitMessage(result);
        }

        return result;
    }
    
    /**
     * Format the commit message to ensure it follows conventional commit format.
     *
     * @param message The raw commit message from the LLM
     * @return A properly formatted commit message
     */
    private static String formatCommitMessage(final String message) {
        if (null == message || message.isEmpty()) {
            return "chore: minor changes";
        }
        
        String formattedMessage = message.trim();
        formattedMessage = ensureColonSpacing(formattedMessage);
        formattedMessage = ensureSpaceAfterColon(formattedMessage);
        formattedMessage = ensureLowercaseAfterColon(formattedMessage);
        
        return formattedMessage;
    }

    private static String ensureColonSpacing(final String message) {
        if (message.contains(":") && !message.contains(": ")) {
            return message.replace(":", ": ");
        }
        return message;
    }

    private static String ensureSpaceAfterColon(final String message) {
        if (message.contains(":") && message.indexOf(':') < message.length() - 1) {
            final int colonIndex = message.indexOf(':');
            if (colonIndex + 1 < message.length() && ' ' != message.charAt(colonIndex + 1)) {
                return message.substring(0, colonIndex + 1) + " " + message.substring(colonIndex + 1);
            }
        }
        return message;
    }

    private static String ensureLowercaseAfterColon(final String message) {
        if (message.contains(": ") && message.indexOf(": ") + 2 < message.length()) {
            final int startIndex = message.indexOf(": ") + 2;
            if (startIndex < message.length()) {
                final char firstChar = message.charAt(startIndex);
                if (Character.isUpperCase(firstChar)) {
                    return message.substring(0, startIndex) + 
                           Character.toLowerCase(firstChar) + 
                           message.substring(startIndex + 1);
                }
            }
        }
        return message;
    }
}
