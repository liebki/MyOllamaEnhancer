package de.liebki.myollamaenhancer.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public enum ExportUtil {
    ;

    private static String cachedMarkdownFormattedContent;

    public static void exportToMarkdown(final String content, final Project project, final boolean applyMarkdownFormatting) {
        if (applyMarkdownFormatting) {
            // Show progress indicator for markdown formatting
            ProgressManager.getInstance().run(new Task.Modal(project, "Formatting Markdown...", true) {
                @Override
                public void run(@NotNull final ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText("Reformatting content with markdown syntax...");

                    try {
                        // Use cached formatted content if available, otherwise format it
                        if (null == cachedMarkdownFormattedContent) {
                            ExportUtil.cachedMarkdownFormattedContent = ExportUtil.formatWithMarkdown(content, project);
                        }

                        // Export the formatted content on the EDT
                        ApplicationManager.getApplication().invokeLater(() -> ExportUtil.performMarkdownExport(ExportUtil.cachedMarkdownFormattedContent, project));

                    } catch (final Exception e) {
                        ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project,
                                "Failed to format markdown: " + e.getMessage(),
                                "Formatting Error"));
                    }
                }
            });
        } else {
            // No formatting needed, export directly
            ExportUtil.performMarkdownExport(content, project);
        }
    }

    private static void performMarkdownExport(final String contentToExport, final Project project) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Explanation to Markdown");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File("explanation_" + ExportUtil.getTimestamp() + ".md"));

        final int result = fileChooser.showSaveDialog(null);
        if (JFileChooser.APPROVE_OPTION == result) {
            final File selectedFile = fileChooser.getSelectedFile();
            try {
                final String markdownContent = "# File Explanation\n\n" +
                        "Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n" +
                        contentToExport;

                try (final FileWriter writer = new FileWriter(selectedFile, StandardCharsets.UTF_8)) {
                    writer.write(markdownContent);
                }

                Messages.showInfoMessage(project,
                        "Explanation exported successfully to:\n" + selectedFile.getAbsolutePath(),
                        "Export Successful");

            } catch (final IOException e) {
                Messages.showErrorDialog(project,
                        "Failed to export explanation: " + e.getMessage(),
                        "Export Error");
            }
        }
    }

    private static String formatWithMarkdown(final String content, final Project project) {
        try {
            final Map<String, Object> structuredOutput = StructuredOutputSchemas.createMarkdownFormattingSchema();
            final String systemPrompt = "You are a markdown formatter. Convert plain text to markdown format using appropriate syntax.";

            final String enhancedPrompt = StructuredPromptEnhancer.enhanceForMarkdownFormatting(systemPrompt);
            final OllamaAPIUtil.ThinkContent response = OllamaAPIUtil.generateStructuredOllamaResponseSync(enhancedPrompt, content, structuredOutput);
            
            System.out.println("[ExportUtil] Raw structured output: " + response);
            final String extractedFormatted = StructuredResponseParser.parseFormattedResponse(response.visibleContent());
            
            System.out.println("[ExportUtil] Extracted formatted content: " + extractedFormatted);
            return null != extractedFormatted ? extractedFormatted : content;

        } catch (final Exception e) {
            System.out.println("[ExportUtil] Error formatting markdown: " + e.getMessage());
            return content; // Return original content if formatting fails
        }
    }


    public static void clearMarkdownCache() {
        ExportUtil.cachedMarkdownFormattedContent = null;
    }

    public static void exportToText(final String content, final Project project) {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Explanation to Text");

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File("explanation_" + ExportUtil.getTimestamp() + ".txt"));

        final int result = fileChooser.showSaveDialog(null);
        if (JFileChooser.APPROVE_OPTION == result) {
            final File selectedFile = fileChooser.getSelectedFile();
            try {
                final String textContent = "File Explanation\n" +
                        "Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n" +
                        content;

                try (final FileWriter writer = new FileWriter(selectedFile, StandardCharsets.UTF_8)) {
                    writer.write(textContent);
                }

                Messages.showInfoMessage(project,
                        "Explanation exported successfully to:\n" + selectedFile.getAbsolutePath(),
                        "Export Successful");

            } catch (final IOException e) {
                Messages.showErrorDialog(project,
                        "Failed to export explanation: " + e.getMessage(),
                        "Export Error");
            }
        }
    }

    public static void copyToClipboard(final String content) {
        try {
            final StringSelection stringSelection = new StringSelection(content);
            final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            clipboard.setContents(stringSelection, null);
            DialogUtil.showInfo(
                    "Explanation copied to clipboard!",
                    "Copy Successful"
            );

        } catch (final Exception e) {
            DialogUtil.showError(
                    "Failed to copy to clipboard: " + e.getMessage(),
                    "Copy Error"
            );
        }
    }

    private static String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
} 