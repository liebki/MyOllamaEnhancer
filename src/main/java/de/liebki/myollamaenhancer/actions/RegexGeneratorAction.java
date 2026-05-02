package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import de.liebki.myollamaenhancer.windows.RegexGeneratorDialog;
import org.jetbrains.annotations.NotNull;

public class RegexGeneratorAction extends ActionBase implements DumbAware {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (null == project) return;
        ApplicationManager.getApplication().invokeLater(() -> RegexGeneratorDialog.open(project));
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        final boolean enabled = null != e.getProject();
        e.getPresentation().setEnabledAndVisible(enabled);
        if (enabled) {
            e.getPresentation().setText("Regex Generator (by Example)");
            e.getPresentation().setDescription("Open a dialog to generate a regex from an example and target capture using the model.");
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
