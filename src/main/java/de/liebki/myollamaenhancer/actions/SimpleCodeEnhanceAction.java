package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import de.liebki.myollamaenhancer.*;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

public class SimpleCodeEnhanceAction extends ActionBase {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e)  {
        final String selectedCode = ActionBase.getSelectedText(e);
        final String codeLanguage = this.getLanguageOfFile(e.getProject());

        if (selectedCode != null) {
            String sysPrompt = MessageFormat.format(OllamaOption.SIMPLE_CODE.getPrompt(), codeLanguage);

            OllamaAPIUtil.generateOllamaResponse(e.getProject(), selectedCode, sysPrompt, response -> this.replaceSelectedText(e, response));
        } else {
            FeedbackOptions.showErrorNoSelection();
        }


    }
}