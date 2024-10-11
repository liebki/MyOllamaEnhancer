package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import de.liebki.myollamaenhancer.*;
import org.jetbrains.annotations.NotNull;

public class SimpleErrorInformationAction extends ActionBase {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final String selectedStacktrace = ActionBase.getSelectedText(e);
        final Project activeProject = e.getProject();

        if (selectedStacktrace != null) {
            String sysPrompt = DataHolder.getSimpleErrorExplainPrompt();
            OllamaAPIUtil.enhanceCodeInBackground(e.getProject(), selectedStacktrace, sysPrompt, response -> FeedbackOptions.showResponseInToolWindow(response, activeProject));
        } else {
            FeedbackOptions.showErrorNoSelectedCode();
        }

    }
}