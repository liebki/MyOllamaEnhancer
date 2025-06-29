package de.liebki.myollamaenhancer;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class MyOllamaEnhancerSettings implements Configurable {

    private JTextField apiEndpointField;
    private JTextField ollamaModelField;
    private JTextField ollamaApiTimeoutField;
    private JPanel settingsPanel;

    public MyOllamaEnhancerSettings() {
        apiEndpointField = new JTextField(20);
        ollamaModelField = new JTextField(20);
        ollamaApiTimeoutField = new JTextField(20);

        settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        settingsPanel.add(new JLabel("API Endpoint:"), gbc);

        gbc.gridx = 1;
        settingsPanel.add(apiEndpointField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        settingsPanel.add(new JLabel("Ollama Model:"), gbc);

        gbc.gridx = 1;
        settingsPanel.add(ollamaModelField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        settingsPanel.add(new JLabel("API Timeout (seconds):"), gbc);

        gbc.gridx = 1;
        settingsPanel.add(ollamaApiTimeoutField, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        apiEndpointField.setPreferredSize(new Dimension(200, apiEndpointField.getPreferredSize().height));
        ollamaModelField.setPreferredSize(new Dimension(200, ollamaModelField.getPreferredSize().height));
        ollamaApiTimeoutField.setPreferredSize(new Dimension(200, ollamaApiTimeoutField.getPreferredSize().height));
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return DataHolder.getTitle();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        apiEndpointField.setText(DataHolder.getApiEndpoint());
        ollamaModelField.setText(DataHolder.getOllamaModel());
        ollamaApiTimeoutField.setText(String.valueOf(DataHolder.getApiTimeout()));
        return settingsPanel;
    }

    @Override
    public boolean isModified() {
        return !apiEndpointField.getText().equals(DataHolder.getApiEndpoint()) || !ollamaModelField.getText().equals(DataHolder.getOllamaModel()) || !ollamaApiTimeoutField.getText().equals(DataHolder.getApiTimeout());
    }

    @Override
    public void apply() {
        setApiEndpoint(apiEndpointField.getText());
        setOllamaModel(ollamaModelField.getText());
        setApiTimeout(Integer.valueOf(ollamaApiTimeoutField.getText()));
    }

    @Override
    public void reset() {
        apiEndpointField.setText(DataHolder.getApiEndpoint());
        ollamaModelField.setText(DataHolder.getOllamaModel());
    }

    private void setApiEndpoint(String apiEndpoint) {
        PropertiesComponent.getInstance().setValue(DataHolder.getApiEndpointKey(), apiEndpoint);
    }

    private void setApiTimeout(int apiTimeout) {
        PropertiesComponent.getInstance().setValue(DataHolder.getApiTimeoutKey(), apiTimeout, 20);
    }

    private void setOllamaModel(String ollamaModel) {
        PropertiesComponent.getInstance().setValue(DataHolder.getOllamaModelKey(), ollamaModel);
    }

    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}