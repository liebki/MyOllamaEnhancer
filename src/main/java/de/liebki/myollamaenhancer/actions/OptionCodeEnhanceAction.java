package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import de.liebki.myollamaenhancer.*;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

import static de.liebki.myollamaenhancer.OllamaOption.*;

public class OptionCodeEnhanceAction extends ActionBase {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final String selectedCode = ActionBase.getSelectedText(e);
        final String codeLanguage = this.getLanguageOfFile(e.getProject());

        if (selectedCode != null) {
            String selectedComboOption = FeedbackOptions.showComboboxOptionPrompt();

            if(selectedComboOption == null)
                return;

            OllamaOption selectedOption = getOption(selectedComboOption);
            String ollamaPrompt = getOllamaPrompt(selectedOption);
            boolean isGenerateComment = selectedComboOption.equals("Add Comment");
            String sysPrompt = MessageFormat.format(ollamaPrompt, codeLanguage);

            OllamaAPIUtil.generateOllamaResponse(e.getProject(), selectedCode, sysPrompt, response -> {
                if (isGenerateComment) {
                    response = response + "\n" + selectedCode;
                }
                this.replaceSelectedText(e, response);
            });

        } else {
            FeedbackOptions.showErrorNoSelection();
        }
    }

    private String getOllamaPrompt(OllamaOption selectedComboOption) {
        return switch (selectedComboOption) {
            case READABILITY -> READABILITY.getPrompt();
            case BUGS -> BUGS.getPrompt();
            case PERFORMANCE -> PERFORMANCE.getPrompt();
            case UNIT_TESTS -> UNIT_TESTS.getPrompt();
            case SIMPLIFY -> SIMPLIFY.getPrompt();
            case COMMENT_CODE -> COMMENT_CODE.getPrompt();
            case FIX_BROKEN -> FIX_BROKEN.getPrompt();
            default -> "";
        };
    }

}