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
    private JPanel settingsPanel;

    public MyOllamaEnhancerSettings() {
        apiEndpointField = new JTextField(20);
        ollamaModelField = new JTextField(20);

        settingsPanel = new JPanel(new FlowLayout());
        settingsPanel.add(new JLabel("API Endpoint:"));
        settingsPanel.add(apiEndpointField);
        settingsPanel.add(new JLabel("Ollama Model:"));
        settingsPanel.add(ollamaModelField);
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
        return settingsPanel;
    }

    @Override
    public boolean isModified() {
        return !apiEndpointField.getText().equals(DataHolder.getApiEndpoint())
                || !ollamaModelField.getText().equals(DataHolder.getOllamaModel());
    }

    @Override
    public void apply() {
        setApiEndpoint(apiEndpointField.getText());
        setOllamaModel(ollamaModelField.getText());
    }

    @Override
    public void reset() {
        apiEndpointField.setText(DataHolder.getApiEndpoint());
        ollamaModelField.setText(DataHolder.getOllamaModel());
    }

    private void setApiEndpoint(String apiEndpoint) {
        PropertiesComponent.getInstance().setValue(DataHolder.getApiEndpointKey(), apiEndpoint);
    }

    private void setOllamaModel(String ollamaModel) {
        PropertiesComponent.getInstance().setValue(DataHolder.getOllamaModelKey(), ollamaModel);
    }

    @Override
    public void disposeUIResources() {
        settingsPanel = null;
    }
}