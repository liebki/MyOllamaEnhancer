package de.liebki.myollamaenhancer.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import de.liebki.myollamaenhancer.utils.DialogUtil;
import de.liebki.myollamaenhancer.utils.DuckDbService;
import de.liebki.myollamaenhancer.utils.OllamaAPIUtil;
import de.liebki.myollamaenhancer.windows.KnowledgeBaseManagementWindow;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class MyOllamaEnhancerSettings implements Configurable {

    private final Project project;
    private final JTextField apiEndpointField;
    private final JTextField ollamaModelField;
    private final JTextField ollamaApiTimeoutField;
    private final JTextArea promptReadabilityField;
    private final JTextArea promptBugsField;
    private final JTextArea promptPerformanceField;
    private final JTextArea promptUnitTestsField;
    private final JTextArea promptSimplifyField;
    private final JTextArea promptCommentCodeField;
    private final JTextArea promptFixBrokenField;
    private final JTextArea promptExplainLogicField;
    private final JTextArea promptExplainStructureField;
    private final JTextArea promptExplainPurposeField;

    private JPanel settingsPanel;
    private final MyOllamaEnhancerSettingsService settingsService;
    private final JButton deleteKnowledgeBaseButton;
    private final JButton manageKnowledgeBaseButton;
    private final JButton testConnectionButton;

    public MyOllamaEnhancerSettings(final Project project) {
        this.project = project;
        this.settingsService = MyOllamaEnhancerSettingsService.getInstance();
        this.apiEndpointField = new JTextField(20);
        this.ollamaModelField = new JTextField(20);
        this.ollamaApiTimeoutField = new JTextField(20);

        this.promptReadabilityField = new JTextArea(5, 40);
        this.promptBugsField = new JTextArea(5, 40);
        this.promptPerformanceField = new JTextArea(5, 40);
        this.promptUnitTestsField = new JTextArea(5, 40);
        this.promptSimplifyField = new JTextArea(5, 40);
        this.promptCommentCodeField = new JTextArea(5, 40);
        this.promptFixBrokenField = new JTextArea(5, 40);
        this.promptExplainLogicField = new JTextArea(5, 40);
        this.promptExplainStructureField = new JTextArea(5, 40);
        this.promptExplainPurposeField = new JTextArea(5, 40);

        this.promptReadabilityField.setLineWrap(true);
        this.promptBugsField.setLineWrap(true);
        this.promptPerformanceField.setLineWrap(true);
        this.promptUnitTestsField.setLineWrap(true);
        this.promptSimplifyField.setLineWrap(true);
        this.promptCommentCodeField.setLineWrap(true);
        this.promptFixBrokenField.setLineWrap(true);
        this.promptExplainLogicField.setLineWrap(true);
        this.promptExplainStructureField.setLineWrap(true);
        this.promptExplainPurposeField.setLineWrap(true);

        this.promptReadabilityField.setWrapStyleWord(true);
        this.promptBugsField.setWrapStyleWord(true);
        this.promptPerformanceField.setWrapStyleWord(true);
        this.promptUnitTestsField.setWrapStyleWord(true);
        this.promptSimplifyField.setWrapStyleWord(true);
        this.promptCommentCodeField.setWrapStyleWord(true);
        this.promptFixBrokenField.setWrapStyleWord(true);
        this.promptExplainLogicField.setWrapStyleWord(true);
        this.promptExplainStructureField.setWrapStyleWord(true);
        this.promptExplainPurposeField.setWrapStyleWord(true);

        this.settingsPanel = new JPanel(new GridBagLayout());
        this.settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        this.settingsPanel.add(new JLabel("API Endpoint:"), gbc);

        gbc.gridx = 1;
        this.settingsPanel.add(this.apiEndpointField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        this.settingsPanel.add(new JLabel("Ollama Model:"), gbc);

        gbc.gridx = 1;
        this.settingsPanel.add(this.ollamaModelField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        this.settingsPanel.add(new JLabel("API Timeout (seconds):"), gbc);

        gbc.gridx = 1;
        this.settingsPanel.add(this.ollamaApiTimeoutField, gbc);

        // Quick Action: Test Connection button (own row below timeout)
        this.testConnectionButton = new JButton("Test Connection");
        this.testConnectionButton.setToolTipText("Ping Ollama using the current endpoint and timeout");
        this.testConnectionButton.addActionListener(e -> {
            final String endpoint = this.apiEndpointField.getText();
            int timeout = 120;
            try {
                timeout = Integer.parseInt(this.ollamaApiTimeoutField.getText());
            } catch (NumberFormatException ignored) {}

            final int finalTimeout = Math.max(1, timeout);
            this.testConnectionButton.setEnabled(false);
            this.testConnectionButton.setText("Testing...");
            this.settingsPanel.revalidate();
            this.settingsPanel.repaint();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                final boolean ok = OllamaAPIUtil.ping(endpoint, finalTimeout);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (ok) {
                        DialogUtil.showInfo("Ollama connection successful", MyOllamaEnhancerSettingsService.TITLE);
                    } else {
                        DialogUtil.showError("Failed to reach Ollama at " + endpoint, MyOllamaEnhancerSettingsService.TITLE);
                    }
                    this.testConnectionButton.setEnabled(true);
                    this.testConnectionButton.setText("Test Connection");
                    this.settingsPanel.revalidate();
                    this.settingsPanel.repaint();
                }, ModalityState.any());
            });
        });
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.settingsPanel.add(this.testConnectionButton, gbc);

        // Add note about keyboard shortcut configuration
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = JBUI.insets(15, 5, 5, 5);
        final JLabel shortcutNote = new JLabel("<html><b>Keyboard Shortcut:</b> Configure the 'Generate Code' action shortcut in <b>Settings > Keymap</b> by searching for 'Generate Code' or 'InlineCodeGeneratorAction'</html>");
        shortcutNote.setForeground(Gray._100);
        this.settingsPanel.add(shortcutNote, gbc);

        // Add prompt fields with expanding scroll panes and more vertical space
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.insets = JBUI.insets(10, 5); // Increase vertical space between elements
        gbc.weighty = 0.3;
        gbc.gridx = 0;
        gbc.gridy = 11;
        this.settingsPanel.add(new JLabel("Prompt: Better Readability"), gbc);
        gbc.gridx = 1;
        this.settingsPanel.add(new JBScrollPane(this.promptReadabilityField), gbc);
        gbc.gridx = 0;
        gbc.gridy = 12;
        this.settingsPanel.add(new JLabel("Prompt: Fix Bugs/Problems"), gbc);
        gbc.gridx = 1;
        this.settingsPanel.add(new JBScrollPane(this.promptBugsField), gbc);
        gbc.gridx = 0;
        gbc.gridy = 13;
        this.settingsPanel.add(new JLabel("Prompt: Enhance Performance"), gbc);
        gbc.gridx = 1;
        this.settingsPanel.add(new JBScrollPane(this.promptPerformanceField), gbc);
        gbc.gridx = 0;
        gbc.gridy = 14;
        this.settingsPanel.add(new JLabel("Prompt: Create Unit Tests"), gbc);
        gbc.gridx = 1;
        this.settingsPanel.add(new JBScrollPane(this.promptUnitTestsField), gbc);
        gbc.gridx = 0;
        gbc.gridy = 15;
        this.settingsPanel.add(new JLabel("Prompt: Simplify"), gbc);
        gbc.gridx = 1;
        this.settingsPanel.add(new JBScrollPane(this.promptSimplifyField), gbc);
        gbc.gridx = 0;
        gbc.gridy = 16;
        this.settingsPanel.add(new JLabel("Prompt: Add Comment"), gbc);
        gbc.gridx = 1;
        this.settingsPanel.add(new JBScrollPane(this.promptCommentCodeField), gbc);
        gbc.gridx = 0;
        gbc.gridy = 17;
        this.settingsPanel.add(new JLabel("Prompt: Fix Code not working"), gbc);
        gbc.gridx = 1;
        this.settingsPanel.add(new JBScrollPane(this.promptFixBrokenField), gbc);
        gbc.gridx = 0;
        gbc.gridy = 18;
        this.settingsPanel.add(new JLabel("Prompt: Explain Logic"), gbc);
        gbc.gridx = 1;
        this.settingsPanel.add(new JBScrollPane(this.promptExplainLogicField), gbc);
        gbc.gridx = 0;
        gbc.gridy = 19;
        this.settingsPanel.add(new JLabel("Prompt: Explain Structure"), gbc);
        gbc.gridx = 1;
        this.settingsPanel.add(new JBScrollPane(this.promptExplainStructureField), gbc);
        gbc.gridx = 0;
        gbc.gridy = 20;
        this.settingsPanel.add(new JLabel("Prompt: Explain Purpose"), gbc);
        gbc.gridx = 1;
        this.settingsPanel.add(new JBScrollPane(this.promptExplainPurposeField), gbc);
        // Remove setPreferredSize for prompt fields to allow expansion

        this.deleteKnowledgeBaseButton = new JButton("Delete Knowledge Base");
        this.deleteKnowledgeBaseButton.setToolTipText("Deletes all code and method embeddings from the knowledge base (irreversible)");
        this.deleteKnowledgeBaseButton.addActionListener(e -> DuckDbService.deleteKnowledgeBase(project, this.settingsPanel));
        gbc.gridx = 0;
        gbc.gridy = 22;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.settingsPanel.add(this.deleteKnowledgeBaseButton, gbc);

        this.manageKnowledgeBaseButton = new JButton("Manage Knowledge Base");
        this.manageKnowledgeBaseButton.setToolTipText("Open knowledge base management window to enable/disable individual documents");
        this.manageKnowledgeBaseButton.addActionListener(e -> {
            final KnowledgeBaseManagementWindow window = new KnowledgeBaseManagementWindow(project);
            window.show();
        });
        gbc.gridx = 0;
        gbc.gridy = 23;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.settingsPanel.add(this.manageKnowledgeBaseButton, gbc);
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return MyOllamaEnhancerSettingsService.TITLE;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        this.reset();
        return this.settingsPanel;
    }

    @Override
    public boolean isModified() {
        return !this.apiEndpointField.getText().equals(this.settingsService.getApiEndpoint()) ||
                !this.ollamaModelField.getText().equals(this.settingsService.getOllamaModel()) ||
                !this.ollamaApiTimeoutField.getText().equals(String.valueOf(this.settingsService.getApiTimeout())) ||
                !this.promptReadabilityField.getText().equals(this.settingsService.getPromptReadability()) ||
                !this.promptBugsField.getText().equals(this.settingsService.getPromptBugs()) ||
                !this.promptPerformanceField.getText().equals(this.settingsService.getPromptPerformance()) ||
                !this.promptUnitTestsField.getText().equals(this.settingsService.getPromptUnitTests()) ||
                !this.promptSimplifyField.getText().equals(this.settingsService.getPromptSimplify()) ||
                !this.promptCommentCodeField.getText().equals(this.settingsService.getPromptCommentCode()) ||
                !this.promptFixBrokenField.getText().equals(this.settingsService.getPromptFixBroken()) ||
                !this.promptExplainLogicField.getText().equals(this.settingsService.getPromptExplainLogic()) ||
                !this.promptExplainStructureField.getText().equals(this.settingsService.getPromptExplainStructure()) ||
                !this.promptExplainPurposeField.getText().equals(this.settingsService.getPromptExplainPurpose());
    }

    @Override
    public void apply() {
        this.settingsService.setApiEndpoint(this.apiEndpointField.getText());
        this.settingsService.setOllamaModel(this.ollamaModelField.getText());
        try {
            this.settingsService.setApiTimeout(Integer.parseInt(this.ollamaApiTimeoutField.getText()));
        } catch (final NumberFormatException e) {
            this.settingsService.setApiTimeout(120); // fallback default aligned with base timeout
        }
        this.settingsService.setPromptReadability(this.promptReadabilityField.getText());
        this.settingsService.setPromptBugs(this.promptBugsField.getText());
        this.settingsService.setPromptPerformance(this.promptPerformanceField.getText());
        this.settingsService.setPromptUnitTests(this.promptUnitTestsField.getText());
        this.settingsService.setPromptSimplify(this.promptSimplifyField.getText());
        this.settingsService.setPromptCommentCode(this.promptCommentCodeField.getText());
        this.settingsService.setPromptFixBroken(this.promptFixBrokenField.getText());
        this.settingsService.setPromptExplainLogic(this.promptExplainLogicField.getText());
        this.settingsService.setPromptExplainStructure(this.promptExplainStructureField.getText());
        this.settingsService.setPromptExplainPurpose(this.promptExplainPurposeField.getText());

    }

    @Override
    public void reset() {
        this.apiEndpointField.setText(this.settingsService.getApiEndpoint());
        this.ollamaModelField.setText(this.settingsService.getOllamaModel());
        this.ollamaApiTimeoutField.setText(String.valueOf(this.settingsService.getApiTimeout()));
        this.promptReadabilityField.setText(this.settingsService.getPromptReadability());
        this.promptBugsField.setText(this.settingsService.getPromptBugs());
        this.promptPerformanceField.setText(this.settingsService.getPromptPerformance());
        this.promptUnitTestsField.setText(this.settingsService.getPromptUnitTests());
        this.promptSimplifyField.setText(this.settingsService.getPromptSimplify());
        this.promptCommentCodeField.setText(this.settingsService.getPromptCommentCode());
        this.promptFixBrokenField.setText(this.settingsService.getPromptFixBroken());
        this.promptExplainLogicField.setText(this.settingsService.getPromptExplainLogic());
        this.promptExplainStructureField.setText(this.settingsService.getPromptExplainStructure());
        this.promptExplainPurposeField.setText(this.settingsService.getPromptExplainPurpose());

    }

    @Override
    public void disposeUIResources() {
        this.settingsPanel = null;
    }

}