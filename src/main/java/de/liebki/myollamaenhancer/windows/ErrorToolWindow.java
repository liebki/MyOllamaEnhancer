package de.liebki.myollamaenhancer.windows;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.*;
import java.awt.*;

public class ErrorToolWindow implements ToolWindowFactory, DumbAware {

    private static String displayText = "No error to display, please use the 'Analyze Stacktrace' feature.";

    public static void setDisplayText(final String text) {
        ErrorToolWindow.displayText = text;
    }

    @Override
    public final void createToolWindowContent(Project project, ToolWindow toolWindow) {
        final JLabel labelTitle = new JLabel("Possible Explanation:");
        labelTitle.setFont(new Font("Arial", Font.PLAIN, 15));

        final JTextArea textAreaErrorExplanation = new JTextArea();
        textAreaErrorExplanation.setText(ErrorToolWindow.displayText);

        textAreaErrorExplanation.setEditable(false);
        textAreaErrorExplanation.setFont(new Font("Arial", Font.PLAIN, 14));

        textAreaErrorExplanation.setLineWrap(true);
        textAreaErrorExplanation.setWrapStyleWord(true);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(labelTitle, BorderLayout.NORTH);

        panel.add(new JBScrollPane(textAreaErrorExplanation), BorderLayout.CENTER);
        final ContentFactory contentFactory = ContentFactory.getInstance();

        final Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
