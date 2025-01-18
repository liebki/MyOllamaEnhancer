package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import de.liebki.myollamaenhancer.*;
import org.jetbrains.annotations.NotNull;

public class BulletErrorInformationRiderAction extends ActionBase {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Project activeProject = e.getProject();
        String userCopiedStacktrace = FeedbackOptions.getStacktraceFromUser();

        if (userCopiedStacktrace != null) {
            String sysPrompt = OllamaOption.ERROR_EXPLAIN_BULLET.getPrompt();
            OllamaAPIUtil.generateOllamaResponse(e.getProject(), userCopiedStacktrace, sysPrompt, response -> FeedbackOptions.showResponseInToolWindow(response, activeProject));
        } else {
            FeedbackOptions.showErrorNoInput();
        }

    }
}