package de.liebki.myollamaenhancer.windows;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.ui.components.JBScrollPane;
import de.liebki.myollamaenhancer.utils.GitCommitMessageGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Collection;

/**
 * Window for generating Git commit messages based on current changes.
 * Shows a progress bar while processing changes and displays the generated commit message.
 */
public class GitCommitMessageGeneratorWindow extends JDialog {

    private final Project project;
    private final Collection<Change> changes;
    private final JTextArea commitMessageArea;
    private final JProgressBar progressBar;
    private final JButton generateButton;
    private final JButton copyButton;
    private final JButton closeButton;

    public static void open(final Project project, final Collection<Change> changes) {
        final GitCommitMessageGeneratorWindow window = new GitCommitMessageGeneratorWindow(project, changes);
        window.setVisible(true);
    }

    private GitCommitMessageGeneratorWindow(final Project project, final Collection<Change> changes) {
        super((Frame) null, "Git Commit Message Generator", false);
        this.project = project;
        this.changes = changes;

        this.commitMessageArea = new JTextArea(10, 50);
        this.progressBar = new JProgressBar();
        this.generateButton = new JButton("Generate Commit Message");
        this.copyButton = new JButton("Copy to Clipboard");
        this.closeButton = new JButton("Close");

        this.setupUI();
        this.setupEventHandlers();
    }

    private void setupUI() {
        this.setMinimumSize(new Dimension(600, 400));
        this.setPreferredSize(new Dimension(800, 500));
        this.setSize(800, 500);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        // Enable transparency support with undecorated window
        this.setUndecorated(true);
        this.getRootPane().setOpaque(false);

        // Main panel
        final JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Progress bar
        this.progressBar.setIndeterminate(false);
        this.progressBar.setString("Ready to generate commit message");
        this.progressBar.setStringPainted(true);
        mainPanel.add(this.progressBar, BorderLayout.NORTH);
        
        // Commit message area
        this.commitMessageArea.setEditable(false);
        this.commitMessageArea.setLineWrap(true);
        this.commitMessageArea.setWrapStyleWord(true);
        this.commitMessageArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        final JScrollPane scrollPane = new JBScrollPane(this.commitMessageArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Generated Commit Message"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(this.generateButton);
        buttonPanel.add(this.copyButton);
        buttonPanel.add(this.closeButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.add(mainPanel, BorderLayout.CENTER);
        
        // Initially disable copy button since there's no content yet
        this.copyButton.setEnabled(false);
    }

    private void setupEventHandlers() {
        this.generateButton.addActionListener(e -> this.generateCommitMessage());
        this.copyButton.addActionListener(e -> this.copyToClipboard());
        this.closeButton.addActionListener(e -> this.dispose());
    }

    private void generateCommitMessage() {
        this.generateButton.setEnabled(false);
        this.progressBar.setIndeterminate(true);
        this.progressBar.setString("Processing changes and generating commit message...");
        this.commitMessageArea.setText("");
        this.copyButton.setEnabled(false);
        
        // Run the generation in a background thread
        new Thread(() -> {
            try {
                final String commitMessage = GitCommitMessageGenerator.generateCommitMessage(this.project, this.changes);
                
                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    this.commitMessageArea.setText(commitMessage);
                    this.progressBar.setIndeterminate(false);
                    this.progressBar.setString("Commit message generated successfully");
                    this.generateButton.setEnabled(true);
                    this.copyButton.setEnabled(true);
                });
            } catch (final Exception ex) {
                ex.printStackTrace();
                
                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    this.commitMessageArea.setText("Error generating commit message: " + ex.getMessage());
                    this.progressBar.setIndeterminate(false);
                    this.progressBar.setString("Error occurred");
                    this.generateButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void copyToClipboard() {
        final StringSelection selection = new StringSelection(this.commitMessageArea.getText());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        this.progressBar.setString("Commit message copied to clipboard");
        
        // Reset status message after a delay
        new Timer(2000, e -> this.progressBar.setString("Commit message generated successfully")).start();
    }
}
