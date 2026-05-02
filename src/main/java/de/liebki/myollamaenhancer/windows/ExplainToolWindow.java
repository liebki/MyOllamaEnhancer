package de.liebki.myollamaenhancer.windows;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import de.liebki.myollamaenhancer.utils.ExportUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ExplainToolWindow implements ToolWindowFactory, DumbAware {


    private static String displayText = "No explanation to display, please use the 'Explain File' feature.";
    private static Project currentProject;

    public static void setDisplayText(final String text, final Project project) {
        ExplainToolWindow.displayText = text;
        ExplainToolWindow.currentProject = project;
        // Clear the markdown cache when new content is set
        ExportUtil.clearMarkdownCache();
    }

    @Override
    public final void createToolWindowContent(Project project, ToolWindow toolWindow) {
        final JLabel labelTitle = new JLabel("Explanation:");
        labelTitle.setFont(new Font("Arial", Font.BOLD, 14));

        final JTextArea textAreaExplanation = new JTextArea();
        textAreaExplanation.setText(ExplainToolWindow.displayText);

        textAreaExplanation.setEditable(false);
        textAreaExplanation.setFont(new Font("Arial", Font.PLAIN, 13));

        textAreaExplanation.setLineWrap(true);
        textAreaExplanation.setWrapStyleWord(true);

        // Create export panel with better structure
        final JPanel exportPanel = this.createExportPanel();

        // Create main panel with BorderLayout
        final JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        mainPanel.add(labelTitle, BorderLayout.NORTH);
        mainPanel.add(new JBScrollPane(textAreaExplanation), BorderLayout.CENTER);
        mainPanel.add(exportPanel, BorderLayout.SOUTH);

        final ContentFactory contentFactory = ContentFactory.getInstance();
        final Content content = contentFactory.createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private JPanel createExportPanel() {
        // Create a titled border panel for export options
        final JPanel exportPanel = new JPanel();
        exportPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Export Options",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12)
        ));

        // Use GridBagLayout for better control
        exportPanel.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(3, 5);

        // Markdown export section
        final JLabel markdownLabel = new JLabel("Markdown Export:");
        markdownLabel.setFont(new Font("Arial", Font.BOLD, 13));

        final JCheckBox markdownFormattingCheckbox = new JCheckBox("Apply formatting");
        markdownFormattingCheckbox.setFont(new Font("Arial", Font.PLAIN, 13));
        markdownFormattingCheckbox.setToolTipText("Reformat explanation with proper markdown syntax");

        final JButton exportMarkdownButton = new JButton("Markdown");
        exportMarkdownButton.setFont(new Font("Arial", Font.PLAIN, 13));
        exportMarkdownButton.setPreferredSize(new Dimension(140, 25));
        exportMarkdownButton.setMinimumSize(new Dimension(120, 25));
        exportMarkdownButton.setMaximumSize(new Dimension(160, 25));

        // Text export section
        final JLabel textLabel = new JLabel("Text Export:");
        textLabel.setFont(new Font("Arial", Font.BOLD, 13));

        final JButton exportTextButton = new JButton("Text");
        exportTextButton.setFont(new Font("Arial", Font.PLAIN, 13));
        exportTextButton.setPreferredSize(new Dimension(140, 25));
        exportTextButton.setMinimumSize(new Dimension(120, 25));
        exportTextButton.setMaximumSize(new Dimension(160, 25));

        // Copy section
        final JLabel copyLabel = new JLabel("Copy:");
        copyLabel.setFont(new Font("Arial", Font.BOLD, 13));

        final JButton copyButton = new JButton("Clipboard");
        copyButton.setFont(new Font("Arial", Font.PLAIN, 13));
        copyButton.setPreferredSize(new Dimension(140, 25));
        copyButton.setMinimumSize(new Dimension(120, 25));
        copyButton.setMaximumSize(new Dimension(160, 25));

        // Add action listeners
        exportMarkdownButton.addActionListener(e -> {
            if (null != currentProject) {
                final boolean applyFormatting = markdownFormattingCheckbox.isSelected();
                ExportUtil.exportToMarkdown(ExplainToolWindow.displayText, ExplainToolWindow.currentProject, applyFormatting);
            }
        });

        exportTextButton.addActionListener(e -> {
            if (null != currentProject) {
                ExportUtil.exportToText(ExplainToolWindow.displayText, ExplainToolWindow.currentProject);
            }
        });

        copyButton.addActionListener(e -> ExportUtil.copyToClipboard(ExplainToolWindow.displayText));

        // Layout components in a structured way with proper constraints
        // Row 1: Markdown section
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        exportPanel.add(markdownLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        exportPanel.add(markdownFormattingCheckbox, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        exportPanel.add(exportMarkdownButton, gbc);

        // Row 2: Text export section
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        exportPanel.add(textLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        exportPanel.add(new JLabel("")); // Empty space

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        exportPanel.add(exportTextButton, gbc);

        // Row 3: Copy section
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        exportPanel.add(copyLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        exportPanel.add(new JLabel("")); // Empty space

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        exportPanel.add(copyButton, gbc);

        // Create a container panel to center the export panel
        final JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(exportPanel, BorderLayout.CENTER);

        return containerPanel;
    }
}
