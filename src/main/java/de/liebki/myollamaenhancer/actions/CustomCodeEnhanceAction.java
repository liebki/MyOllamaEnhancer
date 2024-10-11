package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import de.liebki.myollamaenhancer.*;
import org.jetbrains.annotations.NotNull;

public class CustomCodeEnhanceAction extends ActionBase {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final String selectedCode = ActionBase.getSelectedText(e);

        if (selectedCode != null) {
            String sysPrompt = FeedbackOptions.getCustomUserPrompt();
            if(sysPrompt == null)
                return;

            OllamaAPIUtil.enhanceCodeInBackground(e.getProject(), selectedCode, sysPrompt, response -> this.replaceSelectedText(e, response));

        } else {
            FeedbackOptions.showErrorNoSelectedCode();
        }

    }
}