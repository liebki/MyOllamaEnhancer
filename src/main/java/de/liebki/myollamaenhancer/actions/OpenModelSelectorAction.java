package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import de.liebki.myollamaenhancer.windows.ModelSelectorDialog;
import org.jetbrains.annotations.NotNull;

public class OpenModelSelectorAction extends AnAction implements DumbAware {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (null == project) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ModelSelectorDialog dialog = new ModelSelectorDialog(project);
            dialog.setVisible(true);
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(null != e.getProject());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
