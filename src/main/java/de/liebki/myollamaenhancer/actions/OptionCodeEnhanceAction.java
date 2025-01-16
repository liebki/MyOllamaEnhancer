package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import de.liebki.myollamaenhancer.*;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

public class OptionCodeEnhanceAction extends ActionBase {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final String selectedCode = ActionBase.getSelectedText(e);
        final String codeLanguage = this.getLanguageOfFile(e.getProject());

        if (selectedCode != null) {
            String selectedComboOption = FeedbackOptions.showComboboxOptionPrompt();

            if(selectedComboOption == null)
                return;

            String ollamaPrompt = getOllamaPrompt(selectedComboOption);
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

    private String getOllamaPrompt(String selectedComboOption) {
        return switch (selectedComboOption) {
            case "Better Readability" -> DataHolder.getReadabilityPrompt();
            case "Fix Bugs/Problems" -> DataHolder.getBugsPrompt();
            case "Enhance Performance" -> DataHolder.getPerformancePrompt();
            case "Create Unit Tests" -> DataHolder.getUnitTestsPrompt();
            case "Simplify" -> DataHolder.getSimplifyPrompt();
            case "Add Comment" -> DataHolder.getCommentCodePrompt();
            case "Fix Code not working" -> DataHolder.getFixBrokenPrompt();
            default -> null;
        };
    }

}