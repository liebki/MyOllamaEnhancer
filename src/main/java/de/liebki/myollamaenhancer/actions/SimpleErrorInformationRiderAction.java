package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import de.liebki.myollamaenhancer.ActionBase;
import de.liebki.myollamaenhancer.DataHolder;
import de.liebki.myollamaenhancer.FeedbackOptions;
import de.liebki.myollamaenhancer.OllamaAPIUtil;
import org.jetbrains.annotations.NotNull;

public class SimpleErrorInformationRiderAction extends ActionBase {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Project activeProject = e.getProject();
        String userCopiedStacktrace = FeedbackOptions.getStacktraceFromUser();

        if (userCopiedStacktrace != null) {
            String sysPrompt = DataHolder.getSimpleErrorExplainPrompt();
            OllamaAPIUtil.generateOllamaResponse(e.getProject(), userCopiedStacktrace, sysPrompt, response -> FeedbackOptions.showResponseInToolWindow(response, activeProject));
        } else {
            FeedbackOptions.showErrorNoInput();
        }

    }
}