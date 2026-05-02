package de.liebki.myollamaenhancer.windows;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import de.liebki.myollamaenhancer.models.CodeFile;
import de.liebki.myollamaenhancer.models.CodeSection;
import de.liebki.myollamaenhancer.utils.DuckDbService;
import de.liebki.myollamaenhancer.events.KnowledgeBaseChangedTopic;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class KnowledgeBaseManagementWindow extends DialogWrapper {
    private final Project project;
    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    private JPanel filesPanel;
    private JPanel sectionsPanel;

    public KnowledgeBaseManagementWindow(final Project project) {
        super(project);
        this.project = project;
        this.setTitle("Knowledge Base Management");
        this.init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        this.mainPanel = new JPanel(new BorderLayout());
        this.mainPanel.setPreferredSize(new Dimension(900, 700));

        this.tabbedPane = new JBTabbedPane();
        
        // Files tab
        this.filesPanel = this.createFilesPanel();
        this.tabbedPane.addTab("Files", this.filesPanel);
        
        // Sections tab
        this.sectionsPanel = this.createSectionsPanel();
        this.tabbedPane.addTab("Code Sections", this.sectionsPanel);

        this.mainPanel.add(this.tabbedPane, BorderLayout.CENTER);
        
        // Add buttons
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> this.refreshData());
        buttonPanel.add(refreshButton);

        this.mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return this.mainPanel;
    }

    private JPanel createFilesPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));

        // Header
        final JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JLabel("Files in Knowledge Base"), BorderLayout.WEST);
        
        final JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> this.selectAllFiles(true));
        final JButton deselectAllButton = new JButton("Deselect All");
        deselectAllButton.addActionListener(e -> this.selectAllFiles(false));
        
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(selectAllButton);
        buttonPanel.add(deselectAllButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        // Files list with proper scroll pane
        final JPanel filesListPanel = new JPanel();
        filesListPanel.setLayout(new BoxLayout(filesListPanel, BoxLayout.Y_AXIS));
        
        final List<CodeFile> files = DuckDbService.getAllCodeFiles(this.project);
        for (final CodeFile file : files) {
            final JPanel filePanel = this.createFilePanel(file);
            filesListPanel.add(filePanel);
            filesListPanel.add(Box.createVerticalStrut(5));
        }

        // Create a scroll pane that handles both vertical and horizontal scrolling properly
        final JBScrollPane scrollPane = new JBScrollPane(filesListPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSectionsPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));

        // Header
        final JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JLabel("Code Sections in Knowledge Base"), BorderLayout.WEST);
        
        final JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> this.selectAllSections(true));
        final JButton deselectAllButton = new JButton("Deselect All");
        deselectAllButton.addActionListener(e -> this.selectAllSections(false));
        
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(selectAllButton);
        buttonPanel.add(deselectAllButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        // Sections list with proper scroll pane
        final JPanel sectionsListPanel = new JPanel();
        sectionsListPanel.setLayout(new BoxLayout(sectionsListPanel, BoxLayout.Y_AXIS));
        
        final List<CodeSection> sections = DuckDbService.getAllCodeSections(this.project);
        for (final CodeSection section : sections) {
            final JPanel sectionPanel = this.createSectionPanel(section);
            sectionsListPanel.add(sectionPanel);
            sectionsListPanel.add(Box.createVerticalStrut(5));
        }

        // Create a scroll pane that handles both vertical and horizontal scrolling properly
        final JBScrollPane scrollPane = new JBScrollPane(sectionsListPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(800, 500));
        
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFilePanel(final CodeFile file) {
        // Use BorderLayout for better control
        final JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150)); // Limit height

        // Left side: Checkbox
        final JCheckBox enabledCheckBox = new JCheckBox("Enabled", file.enabled);
        enabledCheckBox.addActionListener(e -> {
            file.enabled = enabledCheckBox.isSelected();
            DuckDbService.setCodeFileEnabled(file.id, file.enabled, this.project);
            // Publish KB changed event
            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(KnowledgeBaseChangedTopic.TOPIC)
                    .knowledgeBaseChanged(this.project);
        });
        panel.add(enabledCheckBox, BorderLayout.WEST);

        // Center: File info and preview
        final JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        
        // File info at top
        final JPanel fileInfoPanel = new JPanel(new BorderLayout());
        fileInfoPanel.add(new JLabel("File: " + file.fileName), BorderLayout.NORTH);
        fileInfoPanel.add(new JLabel("Path: " + file.folder), BorderLayout.CENTER);
        infoPanel.add(fileInfoPanel, BorderLayout.NORTH);
        
        // Preview area
        final String preview = 100 < file.code.length() ? file.code.substring(0, 100) + "..." : file.code;
        final JTextArea previewArea = new JTextArea(preview);
        previewArea.setEditable(false);
        previewArea.setRows(3);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setBackground(panel.getBackground());
        previewArea.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        final JScrollPane previewScrollPane = new JBScrollPane(previewArea);
        previewScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        previewScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        previewScrollPane.setPreferredSize(new Dimension(400, 80));
        infoPanel.add(previewScrollPane, BorderLayout.CENTER);

        // No embeddings panel; embeddings feature removed.

        panel.add(infoPanel, BorderLayout.CENTER);

        // Right side: Delete button
        final JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> {
            final int result = JOptionPane.showConfirmDialog(
                    this.mainPanel,
                "Are you sure you want to delete this file from the knowledge base?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (JOptionPane.YES_OPTION == result) {
                DuckDbService.deleteCodeFile(file.id, this.project);
                this.refreshData();
                // Publish KB changed event
                ApplicationManager.getApplication().getMessageBus()
                        .syncPublisher(KnowledgeBaseChangedTopic.TOPIC)
                        .knowledgeBaseChanged(this.project);
            }
        });
        panel.add(deleteButton, BorderLayout.EAST);

        return panel;
    }

    private JPanel createSectionPanel(final CodeSection section) {
        // Use BorderLayout for better control
        final JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150)); // Limit height

        // Left side: Checkbox
        final JCheckBox enabledCheckBox = new JCheckBox("Enabled", section.enabled);
        enabledCheckBox.addActionListener(e -> {
            section.enabled = enabledCheckBox.isSelected();
            DuckDbService.setCodeSectionEnabled(section.id, section.enabled, this.project);
            // Publish KB changed event
            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(KnowledgeBaseChangedTopic.TOPIC)
                    .knowledgeBaseChanged(this.project);
        });
        panel.add(enabledCheckBox, BorderLayout.WEST);

        // Center: Section info and preview
        final JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        
        // Section info at top
        final JPanel sectionInfoPanel = new JPanel(new BorderLayout());
        sectionInfoPanel.add(new JLabel("File: " + section.fileName), BorderLayout.NORTH);
        sectionInfoPanel.add(new JLabel("Path: " + section.folder), BorderLayout.CENTER);
        infoPanel.add(sectionInfoPanel, BorderLayout.NORTH);
        
        // Preview area
        final String preview = 100 < section.code.length() ? section.code.substring(0, 100) + "..." : section.code;
        final JTextArea previewArea = new JTextArea(preview);
        previewArea.setEditable(false);
        previewArea.setRows(3);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setBackground(panel.getBackground());
        previewArea.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        final JScrollPane previewScrollPane = new JBScrollPane(previewArea);
        previewScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        previewScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        previewScrollPane.setPreferredSize(new Dimension(400, 80));
        infoPanel.add(previewScrollPane, BorderLayout.CENTER);

        // No embeddings panel; embeddings feature removed.

        panel.add(infoPanel, BorderLayout.CENTER);

        // Right side: Delete button
        final JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> {
            final int result = JOptionPane.showConfirmDialog(
                    this.mainPanel,
                "Are you sure you want to delete this section from the knowledge base?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (JOptionPane.YES_OPTION == result) {
                DuckDbService.deleteCodeSection(section.id, this.project);
                this.refreshData();
                // Publish KB changed event
                ApplicationManager.getApplication().getMessageBus()
                        .syncPublisher(KnowledgeBaseChangedTopic.TOPIC)
                        .knowledgeBaseChanged(this.project);
            }
        });
        panel.add(deleteButton, BorderLayout.EAST);

        return panel;
    }

    private void selectAllFiles(final boolean enabled) {
        final List<CodeFile> files = DuckDbService.getAllCodeFiles(this.project);
        for (final CodeFile file : files) {
            DuckDbService.setCodeFileEnabled(file.id, enabled, this.project);
        }
        this.refreshData();
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(KnowledgeBaseChangedTopic.TOPIC)
                .knowledgeBaseChanged(this.project);
    }

    private void selectAllSections(final boolean enabled) {
        final List<CodeSection> sections = DuckDbService.getAllCodeSections(this.project);
        for (final CodeSection section : sections) {
            DuckDbService.setCodeSectionEnabled(section.id, enabled, this.project);
        }
        this.refreshData();
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(KnowledgeBaseChangedTopic.TOPIC)
                .knowledgeBaseChanged(this.project);
    }

    private void refreshData() {
        // Remove existing panels
        this.tabbedPane.removeAll();
        
        // Recreate panels
        this.filesPanel = this.createFilesPanel();
        this.sectionsPanel = this.createSectionsPanel();

        this.tabbedPane.addTab("Files", this.filesPanel);
        this.tabbedPane.addTab("Code Sections", this.sectionsPanel);
        
        // Refresh the UI
        this.mainPanel.revalidate();
        this.mainPanel.repaint();
    }

    // Embeddings helpers removed.

    
} 