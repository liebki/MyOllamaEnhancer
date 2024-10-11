package de.liebki.myollamaenhancer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import javax.swing.*;

public class FeedbackOptions {

    public static void showErrorNoSelectedCode() {
        JOptionPane.showMessageDialog(null, "You have to select code before using this.", DataHolder.getTitle(), JOptionPane.WARNING_MESSAGE);
    }

    public static void showErrorExecution() {
        JOptionPane.showMessageDialog(null, "An error occurred while executing ollama.", DataHolder.getTitle(), JOptionPane.WARNING_MESSAGE);
    }

    public static void showErrorOllamaInactive() {
        JOptionPane.showMessageDialog(null, "An error occurred, is ollama really active?", DataHolder.getTitle(), JOptionPane.WARNING_MESSAGE);
    }

    public static String getCustomUserPrompt() {
        return JOptionPane.showInputDialog(null, "Your custom Instructions:", DataHolder.getTitle(), JOptionPane.PLAIN_MESSAGE);
    }

    public static void showResponseInToolWindow(String response, Project activeProject) {
        ErrorToolWindow.setDisplayText(response);
        ToolWindow toolWindow = ToolWindowManager.getInstance(activeProject).getToolWindow("ErrorToolWindow");

        if (toolWindow != null) {
            toolWindow.getContentManager().removeAllContents(true);
            ErrorToolWindow errorToolWindow = new ErrorToolWindow();
            errorToolWindow.createToolWindowContent(activeProject, toolWindow);

            toolWindow.show(null);
        }
    }

    public static String showComboboxOptionPrompt() {
        JLabel label = new JLabel("Please select a option:");

        JComboBox<String> comboBox = new ComboBox<>(DataHolder.getComboOptions());
        comboBox.setSelectedIndex(0);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(label);
        panel.add(comboBox);

        int resultSelection = JOptionPane.showConfirmDialog(
                null,
                panel,
                DataHolder.getTitle(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (resultSelection == JOptionPane.OK_OPTION) {
            return (String) comboBox.getSelectedItem();
        } else {
            return null;
        }
    }

}
