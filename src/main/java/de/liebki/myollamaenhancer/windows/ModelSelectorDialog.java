package de.liebki.myollamaenhancer.windows;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import de.liebki.myollamaenhancer.configuration.MyOllamaEnhancerSettingsService;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.response.Model;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModelSelectorDialog extends JDialog {
    private final Project project;
    private final JList<String> modelList;
    private final DefaultListModel<String> listModel;
    private final JButton selectButton;
    private final JButton cancelButton;
    private final JButton refreshButton;
    private final SearchTextField searchField;
    private final JLabel countLabel;
    private final JLabel selectedLabel;
    private List<String> allModelNames = new ArrayList<>();

    public ModelSelectorDialog(Project project) {
        this.project = project;
        setTitle("Select LLM Model");
        setModal(true);
        setSize(420, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top: count (left), search (center), refresh (right)
        JPanel topPanel = new JPanel(new BorderLayout());
        searchField = new SearchTextField();
        searchField.setOpaque(false);
        searchField.getTextEditor().setToolTipText("Filter models by name");
        topPanel.setBorder(JBUI.Borders.empty(8, 8, 4, 8));
        countLabel = new JLabel(" ");
        countLabel.setBorder(JBUI.Borders.emptyRight(8));
        topPanel.add(countLabel, BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);

        refreshButton = new JButton("Refresh");
        topPanel.add(refreshButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center: list with custom renderer
        listModel = new DefaultListModel<>();
        modelList = new JBList<>(listModel);
        modelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modelList.setVisibleRowCount(12);
        modelList.setFixedCellHeight(-1); // allow variable-height cells
        modelList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            SimpleColoredComponent comp = new SimpleColoredComponent();
            comp.setOpaque(true);
            String display = StringUtil.shortenTextWithEllipsis(value, 80, 10, true);
            // Render main text
            comp.append(display, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            // Show tag/version part in gray if present (e.g., name:tag)
            int colonIdx = value.indexOf(':');
            if (0 < colonIdx && colonIdx < value.length() - 1) {
                String tag = value.substring(colonIdx + 1);
                comp.append("  ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                comp.append("[" + StringUtil.shortenTextWithEllipsis(tag, 30, 5, true) + "]", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
            comp.setToolTipText(value);
            comp.setBorder(JBUI.Borders.empty(6, 8));
            if (isSelected) {
                comp.setBackground(list.getSelectionBackground());
                comp.setForeground(list.getSelectionForeground());
            } else {
                comp.setBackground(list.getBackground());
                comp.setForeground(list.getForeground());
            }
            return comp;
        });
        new ListSpeedSearch<>(modelList);
        JScrollPane scrollPane = new JBScrollPane(modelList);
        scrollPane.setBorder(JBUI.Borders.empty(0, 8));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom: selected (left, smaller) and actions (right)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        selectedLabel = new JLabel(" ");
        selectedLabel.setBorder(JBUI.Borders.empty(6, 8));
        selectedLabel.setFont(JBFont.small());
        bottomPanel.add(selectedLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel();
        selectButton = new JButton("Select");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(selectButton);
        buttonPanel.add(cancelButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        selectButton.addActionListener(e -> onSelect());
        cancelButton.addActionListener(e -> onCancel());
        refreshButton.addActionListener(e -> loadModels());
        modelList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (2 == e.getClickCount() && SwingUtilities.isLeftMouseButton(e)) {
                    onSelect();
                }
            }
        });
        modelList.addListSelectionListener(e -> updateStatus());

        // Filter as user types
        searchField.getTextEditor().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter(); }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter(); }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter(); }
        });

        // Keyboard shortcuts: ESC to cancel, Enter to select
        getRootPane().setDefaultButton(selectButton);
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> onSelect(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        loadModels();

        // Preselect saved model if present
        SwingUtilities.invokeLater(() -> {
            String saved = MyOllamaEnhancerSettingsService.getInstance().getOllamaModel();
            if (null != saved && !saved.isEmpty()) {
                modelList.setSelectedValue(saved, true);
            }
            updateStatus();
        });
    }

    private void loadModels() {
        try {
            Ollama ollama = new Ollama(MyOllamaEnhancerSettingsService.getInstance().getApiEndpoint());
            List<Model> models = ollama.listModels();

            allModelNames = models.stream().map(Model::getName).collect(Collectors.toList());
            applyFilter();
            countLabel.setText(allModelNames.isEmpty() ? "No models found" : allModelNames.size() + " models loaded");
            System.out.println("[ModelSelectorDialog] Loaded " + allModelNames.size() + " models.");

        } catch (Exception ex) {
            NotificationUtil.error(project, "Failed to load models: " + ex.getMessage());
            countLabel.setText("Failed to load models");
        }
    }

    private void applyFilter() {
        String query = searchField.getText().trim().toLowerCase();
        listModel.clear();
        if (query.isEmpty()) {
            for (String name : allModelNames) listModel.addElement(name);
        } else {
            for (String name : allModelNames) {
                if (name.toLowerCase().contains(query)) listModel.addElement(name);
            }
        }
        updateStatus();
    }

    private void updateStatus() {
        int total = allModelNames.size();
        int visible = listModel.getSize();
        String q = searchField.getText();
        String selected = modelList.getSelectedValue();
        String left = (null == q || q.isEmpty()) ? (total + " models") : (visible + "/" + total + " match");
        countLabel.setText(left);
        if (null != selected) {
            String truncated = StringUtil.shortenTextWithEllipsis(selected, 60, 5, true);
            selectedLabel.setText("Selected: " + truncated);
            selectedLabel.setToolTipText(selected);
        } else {
            selectedLabel.setText(" ");
            selectedLabel.setToolTipText(null);
        }
    }

    private void onSelect() {
        String selectedModel = modelList.getSelectedValue();
        if (null != selectedModel) {
            MyOllamaEnhancerSettingsService.getInstance().setOllamaModel(selectedModel);
            NotificationUtil.info(project, "Selected model: " + selectedModel);
            dispose();
        } else {
            NotificationUtil.warn(project, "Please select a model.");
        }
    }

    private void onCancel() {
        dispose();
    }
}
