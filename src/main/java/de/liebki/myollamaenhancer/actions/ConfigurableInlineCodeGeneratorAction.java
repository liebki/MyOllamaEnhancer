package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;

public class ConfigurableInlineCodeGeneratorAction implements ProjectActivity {

    @Override
    public Object execute(@NotNull final Project project, @NotNull final Continuation<? super Unit> continuation) {
        // Settings > Keymap > Search for "Generate Code" or "InlineCodeGeneratorAction"
        // This project activity ensures the action is available for keymap configuration
        return Unit.INSTANCE;
    }
} 