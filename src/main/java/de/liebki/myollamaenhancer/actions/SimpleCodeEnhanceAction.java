package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import de.liebki.myollamaenhancer.ActionBase;
import de.liebki.myollamaenhancer.DataHolder;
import de.liebki.myollamaenhancer.FeedbackOptions;
import de.liebki.myollamaenhancer.OllamaAPIUtil;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

public class SimpleCodeEnhanceAction extends ActionBase {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e)  {
        final String selectedCode = ActionBase.getSelectedText(e);
        final String codeLanguage = this.getLanguageOfFile(e.getProject());

        if (selectedCode != null) {
            String sysPrompt = MessageFormat.format(DataHolder.getSimpleCodePrompt(), codeLanguage);

            OllamaAPIUtil.generateOllamaResponse(e.getProject(), selectedCode, sysPrompt, response -> this.replaceSelectedText(e, response));
        } else {
            FeedbackOptions.showErrorNoSelection();
        }


    }
}