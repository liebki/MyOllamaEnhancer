package de.liebki.myollamaenhancer.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import de.liebki.myollamaenhancer.configuration.MyOllamaEnhancerSettingsService;
import de.liebki.myollamaenhancer.types.OllamaOption;
import de.liebki.myollamaenhancer.windows.ErrorToolWindow;
import de.liebki.myollamaenhancer.windows.ExplainToolWindow;

import javax.swing.*;
import java.util.function.Supplier;

public enum DialogUtil {
    ;

    private static void showMessageDialogOnEDT(final String message, final String title, final int messageType) {
        DialogUtil.showOnEDT(() -> JOptionPane.showMessageDialog(null, message, title, messageType));
    }

    public static void showError(final String message, final String title) {
        DialogUtil.showMessageDialogOnEDT(message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showWarning(final String message, final String title) {
        DialogUtil.showMessageDialogOnEDT(message, title, JOptionPane.WARNING_MESSAGE);
    }

    public static void showInfo(final String message, final String title) {
        DialogUtil.showMessageDialogOnEDT(message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private static <T> T runDialogOnEDT(final Supplier<T> dialogSupplier) {
        if (SwingUtilities.isEventDispatchThread()) {
            return dialogSupplier.get();
        } else {
            Object[] result = new Object[1];
            try {
                SwingUtilities.invokeAndWait(() -> result[0] = dialogSupplier.get());
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            return (T) result[0];
        }
    }

    public static int showOptionDialog(final String message, final String title, final String[] options, final int defaultOption) {
        return DialogUtil.runDialogOnEDT(() -> JOptionPane.showOptionDialog(
                null,
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[defaultOption]
        ));
    }

    public static String showInputDialog(final String message, final String title) {
        return DialogUtil.runDialogOnEDT(() -> JOptionPane.showInputDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE));
    }

    public static void showErrorNoSelection() {
        DialogUtil.showWarning("You have to select something to input, before using this.", MyOllamaEnhancerSettingsService.TITLE);
    }

    public static void showErrorNoInput() {
        DialogUtil.showWarning("You have to input a stacktrace for this to work.", MyOllamaEnhancerSettingsService.TITLE);
    }

    public static void showErrorExecution() {
        DialogUtil.showError("An error occurred while executing ollama.", MyOllamaEnhancerSettingsService.TITLE);
    }

    public static void showErrorOllamaInactive() {
        DialogUtil.showError("An error occurred, is ollama really active?", MyOllamaEnhancerSettingsService.TITLE);
    }

    public static String getStacktraceFromUser() {
        return DialogUtil.showInputDialog("Please input the whole stacktrace:", MyOllamaEnhancerSettingsService.TITLE);
    }

    public static String showComboboxOptionPrompt() {
        final JLabel label = new JLabel("Please select a option:");
        final JComboBox<String> comboBox = new ComboBox<>(OllamaOption.getEnumValueArray());
        comboBox.setSelectedIndex(0);
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(label);
        panel.add(comboBox);
        final int resultSelection = JOptionPane.showConfirmDialog(
                null,
                panel,
                MyOllamaEnhancerSettingsService.TITLE,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (JOptionPane.OK_OPTION == resultSelection) {
            return (String) comboBox.getSelectedItem();
        } else {
            return null;
        }
    }

    public static void showResponseInToolWindow(final String response, final Project activeProject) {
        ErrorToolWindow.setDisplayText(response);
        final ToolWindow toolWindow = ToolWindowManager.getInstance(activeProject).getToolWindow("ErrorToolWindow");
        if (null != toolWindow) {
            toolWindow.getContentManager().removeAllContents(true);
            final ErrorToolWindow errorToolWindow = new ErrorToolWindow();
            errorToolWindow.createToolWindowContent(activeProject, toolWindow);
            toolWindow.show(null);
        }
    }

    public static void showResponseInExplainWindow(final String response, final Project activeProject) {
        ExplainToolWindow.setDisplayText(response, activeProject);
        final ToolWindow toolWindow = ToolWindowManager.getInstance(activeProject).getToolWindow("ExplainToolWindow");
        if (null != toolWindow) {
            toolWindow.getContentManager().removeAllContents(true);
            final ExplainToolWindow explainToolWindow = new ExplainToolWindow();
            explainToolWindow.createToolWindowContent(activeProject, toolWindow);
            toolWindow.show(null);
        }
    }

    private static void showOnEDT(final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

} 