package de.liebki.myollamaenhancer.actions;

import com.intellij.lang.Language;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.codeStyle.CodeStyleManager;
import de.liebki.myollamaenhancer.utils.FileValidationUtils;
import de.liebki.myollamaenhancer.utils.NotificationUtil;
import de.liebki.myollamaenhancer.commentgenerator.*;
import de.liebki.myollamaenhancer.js.JavaScriptLanguageHandler;
import de.liebki.myollamaenhancer.php.PHPHandler;
import de.liebki.myollamaenhancer.ts.TypeScriptLanguageHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class ActionBase extends AnAction {
    private static final Logger LOG = Logger.getInstance(ActionBase.class);

    /**
     * Returns the PsiFile for the currently selected file in the given project, or null if unavailable.
     *
     * @param project The current project
     * @return The PsiFile or null
     */
    @Nullable
    protected PsiFile getPsiFile(final Project project) {
        final VirtualFile virtualFile = this.getVirtualFile(project);
        if (null == virtualFile) {
            return null;
        }
        return PsiManager.getInstance(project).findFile(virtualFile);
    }

    /**
     * Returns the Document for the currently selected file in the given project, or null if unavailable.
     *
     * @param project The current project
     * @return The Document or null
     */
    @Nullable
    protected Document getDocument(final Project project) {
        final VirtualFile virtualFile = this.getVirtualFile(project);
        if (null == virtualFile) {
            return null;
        }
        final FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        return fileDocumentManager.getDocument(virtualFile);
    }

    /**
     * Returns the currently selected text in the editor from the action event, or null if nothing is selected.
     *
     * @param e The action event
     * @return The selected text or null
     */
    protected String getSelectedText(@NotNull final AnActionEvent e) {
        final String selectedText;
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (null != editor && null != (selectedText = editor.getSelectionModel().getSelectedText()) && !selectedText.isEmpty()) {
            return selectedText;
        }
        return null;
    }

    /**
     * Returns the VirtualFile for the currently selected file in the project view, or the active editor file if none selected.
     *
     * @param project The current project
     * @return The VirtualFile or null if no file is selected or open in editor
     */
    @Nullable
    protected VirtualFile getVirtualFile(final Project project) {
        // First try to get the selected file from the project view
        VirtualFile selectedFile = getSelectedFileFromProjectView(project);
        
        // If no file is selected in project view, fall back to the active editor file
        if (null == selectedFile) {
            // Use selected text editor and map its document to a VirtualFile to avoid unsafe casts
            final Editor selectedEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (null != selectedEditor) {
                final Document document = selectedEditor.getDocument();
                selectedFile = FileDocumentManager.getInstance().getFile(document);
            }
        }
        
        return selectedFile;
    }
    
    /**
     * Gets the currently selected file in the project view.
     *
     * @param project The current project
     * @return The selected VirtualFile or null if no file is selected
     */
    @Nullable
    protected VirtualFile getSelectedFileFromProjectView(Project project) {
        if (null == project) {
            return null;
        }
        
        // Get the current selection from the project view
        VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
        if (0 < selectedFiles.length) {
            return selectedFiles[0];
        }
        
        // Fallback to the current file in the editor
        Editor selectedEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (null != selectedEditor) {
            Document document = selectedEditor.getDocument();
            return FileDocumentManager.getInstance().getFile(document);
        }
        
        return null;
    }

    /**
     * Reformats the currently selected text in the editor using the project's code style.
     *
     * @param project The current project
     * @param editor  The editor instance
     */
    protected void reformatSelectedText(final Project project, final Editor editor) {
        final PsiFile psiFile = PsiDocumentManager.getInstance(Objects.requireNonNull(project)).getPsiFile(editor.getDocument());
        if (null != psiFile) {
            CodeStyleManager.getInstance(project).reformatText(psiFile, editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd());
        }
    }

    /**
     * Returns the display name of the language for the currently selected file in the given project, or null if unavailable.
     *
     * @param project The current project
     * @return The language display name or null
     */
    public String getLanguageOfFile(final Project project) {
        final PsiFile psiFile = this.getPsiFile(project);
        if (null == psiFile) {
            return null;
        }

        // Prefer the base language from the file's view provider (handles templated/multi-language files)
        final FileViewProvider viewProvider = psiFile.getViewProvider();
        final Language baseLanguage = null != viewProvider ? viewProvider.getBaseLanguage() : psiFile.getLanguage();
        String displayName = (null != baseLanguage ? baseLanguage : psiFile.getLanguage()).getDisplayName();

        // Some files may be identified as TextMate/Plain text depending on plugins; fall back to
        // extension-based mapping for better reliability in those cases.
        if (null != displayName) {
            final String dn = displayName.toLowerCase();
            if ("textmate".equals(dn) || "plain text".equals(dn) || "text".equals(dn)) {
                final VirtualFile vf = this.getVirtualFile(project);
                if (null != vf) {
                    final String mapped = de.liebki.myollamaenhancer.utils.LanguageUtils.getLanguageFromVirtualFile(vf);
                    if (null != mapped && !mapped.isEmpty()) {
                        return mapped;
                    }
                }
            }
        }

        return displayName;
    }

    /**
     * Returns the full text content of the currently selected file in the given project, or null if unavailable.
     *
     * @param project The current project
     * @return The file content as a string, or null
     */
    public String getFileContent(final Project project) {
        final Document document = this.getDocument(project);
        if (null == document) {
            return null;
        }
        return document.getText();
    }

    /**
     * Returns the file content from the editor if available, otherwise reads directly from the given VirtualFile.
     *
     * @param project      The current project
     * @param virtualFile  The VirtualFile to read from if editor content is unavailable
     * @return The file content as a string, or null
     */
    public String getFileContentFromEditorOrVirtualFile(final Project project, final VirtualFile virtualFile) {
        final String content = this.getFileContent(project);
        if (null != content && !content.isEmpty()) {
            return content;
        }
        if (null != virtualFile) {
            try {
                final byte[] bytes = virtualFile.contentsToByteArray();
                return new String(bytes, virtualFile.getCharset());
            } catch (final Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Replaces the entire content of the given VirtualFile with new content, using a write command action.
     *
     * @param project     The current project
     * @param virtualFile The VirtualFile to update
     * @param newContent  The new content to set
     */
    public void replaceFileContent(final Project project, final VirtualFile virtualFile, final String newContent) {
        if (null == project || null == virtualFile || null == newContent) return;
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
                if (null != document) {
                    document.setText(newContent);
                }
            });
        });
    }

    /**
     * Replaces the currently selected text in the editor with the given new text, preserving trailing newlines and reformatting.
     *
     * @param e      The action event
     * @param newText The new text to insert
     */
    public void replaceSelectedText(@NotNull final AnActionEvent e, final String newText) {
        final String selectedText;
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (null != editor && null != (selectedText = editor.getSelectionModel().getSelectedText()) && !selectedText.isEmpty()) {
            WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                editor.getDocument().replaceString(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd(), newText + (selectedText.endsWith("\n") ? "\n" : ""));
                this.reformatSelectedText(e.getProject(), editor);
            });
        }
    }

    /**
     * Returns the file path of the currently selected file from the action event, or null if unavailable.
     *
     * @param e The action event
     * @return The file path as a string, or null
     */
    protected String getCurrentFilePath(final AnActionEvent e) {
        try {
            final VirtualFile vf = e.getData(CommonDataKeys.VIRTUAL_FILE);
            return null != vf ? vf.getPath() : null;
        } catch (final Exception ex) {
            return null;
        }
    }

    // ========== COMMON UTILITIES FOR ALL ACTIONS ==========

    /**
     * Get the standard list of language handlers used across multiple actions.
     * This centralizes the handler creation and ensures consistency.
     *
     * @return List of language handlers
     */
    protected List<LanguageHandler> getStandardLanguageHandlers() {
        return Arrays.asList(
                new JavaLanguageHandler(),
                new PythonLanguageHandler(),
                new CSharpLanguageHandler(),
                new JavaScriptLanguageHandler(),
                new TypeScriptLanguageHandler(),
                new SvelteLanguageHandler(),
                new VueLanguageHandler(),
                new GoLanguageHandler(),
                new RustLanguageHandler(),
                new PHPHandler(),
                new CxxLanguageHandler()
        );
    }

    /**
     * Find the appropriate language handler for a given file extension.
     *
     * @param fileExtension The file extension to find a handler for
     * @return The language handler or null if none found
     */
    protected LanguageHandler findLanguageHandler(final String fileExtension) {
        return this.getStandardLanguageHandlers().stream()
                .filter(h -> h.isApplicable(fileExtension))
                .findFirst()
                .orElse(null);
    }

    /**
     * Read file content from a VirtualFile with proper error handling.
     *
     * @param virtualFile The file to read
     * @return The file content as a string, or null if reading failed
     */
    protected String readFileContent(final VirtualFile virtualFile) {
        if (null == virtualFile) {
            return null;
        }
        
        try {
            return new String(virtualFile.contentsToByteArray(), virtualFile.getCharset());
        } catch (final IOException ex) {
            LOG.warn("Error reading file content: " + ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Check if a file is valid for processing (not null, not directory, has supported extension).
     *
     * @param virtualFile The file to check
     * @return true if the file is valid for processing
     */
    protected boolean isValidFileForProcessing(final VirtualFile virtualFile) {
        return FileValidationUtils.isValidFile(virtualFile);
    }

    /**
     * Show an error dialog with a standard message format.
     *
     * @param project The current project
     * @param message The error message
     * @param title The dialog title
     */
    protected void showErrorDialog(final Project project, final String message, final String title) {
        ApplicationManager.getApplication().invokeLater(() -> 
            Messages.showErrorDialog(project, message, title));
    }

    /**
     * Show an info dialog with a standard message format.
     *
     * @param project The current project
     * @param message The info message
     * @param title The dialog title
     */
    protected void showInfoDialog(final Project project, final String message, final String title) {
        ApplicationManager.getApplication().invokeLater(() -> 
            Messages.showInfoMessage(project, message, title));
    }

    /**
     * Show a confirmation dialog with Yes/No options.
     *
     * @param project The current project
     * @param message The confirmation message
     * @param title The dialog title
     * @return true if user clicked Yes, false otherwise
     */
    protected boolean showConfirmationDialog(final Project project, final String message, final String title) {
        final int result = Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon());
        return Messages.YES == result;
    }

    /**
     * Common validation for actions that require a project and valid file.
     *
     * @param e The action event
     * @return true if the action should be enabled
     */
    protected boolean validateProjectAndFile(final AnActionEvent e) {
        final Project project = e.getProject();
        final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        return null != project && null != virtualFile && this.isValidFileForProcessing(virtualFile);
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
    }

    /**
     * Common validation for actions that require a project and directory.
     *
     * @param e The action event
     * @return true if the action should be enabled
     */
    protected boolean validateProjectAndDirectory(final AnActionEvent e) {
        final Project project = e.getProject();
        final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        return null != project && null != virtualFile && virtualFile.isDirectory();
    }

    /**
     * Common validation for actions that require a project and editor.
     *
     * @param e The action event
     * @return true if the action should be enabled
     */
    protected boolean validateProjectAndEditor(final AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        return null != project && null != editor;
    }

    /**
     * Common validation for actions that require selected text.
     *
     * @param e The action event
     * @return true if the action should be enabled
     */
    protected boolean validateSelectedText(final AnActionEvent e) {
        final String selectedText = this.getSelectedText(e);
        return null != selectedText && !selectedText.trim().isEmpty();
    }

    /**
     * Get VirtualFile from action event with null safety.
     * Uses only the file provided by the action event (e.g., Project View right-click).
     * No fallback to the active editor file.
     *
     * @param e The action event
     * @return The VirtualFile or null
     */
    protected VirtualFile getVirtualFileFromEvent(final AnActionEvent e) {
        // 1) Single selected file from the event
        final VirtualFile vf = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (null != vf) return vf;

        // 2) Multiple selection from the event — take the first
        final VirtualFile[] vfs = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (null != vfs && 0 < vfs.length) return vfs[0];

        // 3) No event-provided file
        return null;
    }

    /**
     * Get Editor from action event with null safety.
     *
     * @param e The action event
     * @return The Editor or null
     */
    protected Editor getEditorFromEvent(final AnActionEvent e) {
        return e.getData(CommonDataKeys.EDITOR);
    }


    protected void errorLog(final Exception ex, final Class action) {
        if (ex instanceof ProcessCanceledException) {
            throw (ProcessCanceledException) ex;
        } else {
            final String errorMsg = null != ex.getMessage() ? ex.getMessage() : "Unknown error";
            Logger.getInstance(action).error(action.getName() + " Error in background task: " + errorMsg, ex);
        }
    }

    protected void informUserCanceled(final Project project, final Class action, final String msg) {
        Logger.getInstance(action).info("[" + action.getName() + "] Canceled by user");
        ApplicationManager.getApplication().invokeLater(() -> NotificationUtil.error(project, msg));
    }
}