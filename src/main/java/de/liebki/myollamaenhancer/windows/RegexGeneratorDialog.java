package de.liebki.myollamaenhancer.windows;

import com.intellij.openapi.project.Project;
import com.intellij.ui.Gray;
import com.intellij.util.ui.JBUI;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import de.liebki.myollamaenhancer.utils.RegexGenerationService;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class RegexGeneratorDialog extends JDialog {

    private final Project project;

    private final JTextArea exampleArea = new JTextArea(5, 40);
    private final JTextArea captureArea = new JTextArea(3, 40);
    private final JTextField resultField = new JTextField();
    private final JTextArea extraHelpArea = new JTextArea(2, 40);
    private final JSpinner retriesSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 10, 1));
    private final JButton generateButton = new JButton("Generate");
    private final JButton copyButton = new JButton("Copy Regex");
    private final JLabel statusLabel = new JLabel(" ");

    public static void open(final Project project) {
        final RegexGeneratorDialog dlg = new RegexGeneratorDialog(project);
        dlg.setVisible(true);
    }

    public RegexGeneratorDialog(final Project project) {
        this.project = project;
        setTitle("Regex Generator (by Example)");
        setModal(true);
        setSize(640, 380);
        setLocationRelativeTo(null);

        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.insets = JBUI.insets(6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        int row = 0;

        // Header
        final JLabel header = new JLabel("Regex Generator");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18.0f));
        c.gridx = 0; c.gridy = row; c.gridwidth = 3; c.weightx = 1; panel.add(header, c); row++;

        // Example label + area
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0; panel.add(new JLabel("Example string:"), c);
        c.gridx = 1; c.gridy = row;
        row++;
        c.gridwidth = 2; c.weightx = 1; panel.add(new JScrollPane(exampleArea), c);

        // Capture label + area
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0; panel.add(new JLabel("What to capture:"), c);
        c.gridx = 1; c.gridy = row;
        row++;
        c.gridwidth = 2; c.weightx = 1; panel.add(new JScrollPane(captureArea), c);

        // Extra help instructions (optional)
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0; panel.add(new JLabel("Extra help instructions (optional):"), c);
        c.gridx = 1; c.gridy = row;
        row++;
        extraHelpArea.setLineWrap(true);
        extraHelpArea.setWrapStyleWord(true);
        c.gridwidth = 2; c.weightx = 1; panel.add(new JScrollPane(extraHelpArea), c);

        // Retries
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0; panel.add(new JLabel("Max retries:"), c);
        c.gridx = 1; c.gridy = row;
        row++;
        c.gridwidth = 1; c.weightx = 1; panel.add(retriesSpinner, c);

        // Result
        resultField.setEditable(false);
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0; panel.add(new JLabel("Result regex:"), c);
        c.gridx = 1; c.gridy = row;
        row++;
        c.gridwidth = 2; c.weightx = 1; panel.add(resultField, c);

        // Status line
        statusLabel.setForeground(Gray._90);
        c.gridx = 0; c.gridy = row; c.gridwidth = 3; c.weightx = 1; panel.add(statusLabel, c); row++;

        // Buttons
        final JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(generateButton);
        btnPanel.add(copyButton);
        c.gridx = 0; c.gridy = row;
        row++;
        c.gridwidth = 3; c.weightx = 1; panel.add(btnPanel, c);

        copyButton.setEnabled(false);

        generateButton.addActionListener(e -> onGenerate());
        copyButton.addActionListener(e -> onCopy());

        // ESC to close
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setContentPane(panel);
    }

    private void onGenerate() {
        final String example = exampleArea.getText();
        final String capture = captureArea.getText();
        final String extra = extraHelpArea.getText();

        if (null == example || example.isBlank()) {
            NotificationUtil.warn(project, "Please provide an example string.");
            return;
        }
        if (null == capture || capture.isBlank()) {
            NotificationUtil.warn(project, "Please provide what to capture.");
            return;
        }

        setBusy(true);
        resultField.setText("");
        statusLabel.setText("Generating...");

        // Dev debug logs
        System.out.println("[RegexGeneratorDialog] Start generation | example.length=" + example.length() + ", retries=" + retriesSpinner.getValue());

        final int retries = (int) retriesSpinner.getValue();
        RegexGenerationService.generateRegex(project, example, capture, extra, retries, (regex, raw) -> {
            setBusy(false);
            // Dev debug logs of raw response and validation
            if (null != raw && !raw.isBlank()) {
                System.out.println("[RegexGeneratorDialog] Raw model/validation info:\n" + raw);
            }
            if (null == regex || regex.isBlank()) {
                NotificationUtil.warn(project, "No success: none of the generated regexes matched your goal on the example.");
                copyButton.setEnabled(false);
                resultField.setText("");
                statusLabel.setText("No success. Try refining the goal or example.");
            } else {
                resultField.setText(regex);
                copyButton.setEnabled(true);
                NotificationUtil.info(project, "Success: regex matches the goal on your example.");
                statusLabel.setText("Success. Ready to use.");
                System.out.println("[RegexGeneratorDialog] Final regex: " + regex);
            }
        });
    }

    private void onCopy() {
        final String regex = resultField.getText();
        if (null == regex || regex.isBlank()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(regex), null);
        NotificationUtil.info(project, "Copied regex to clipboard.");
    }

    private void setBusy(final boolean busy) {
        generateButton.setEnabled(!busy);
    }
}
