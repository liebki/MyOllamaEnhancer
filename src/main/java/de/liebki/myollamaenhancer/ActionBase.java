package de.liebki.myollamaenhancer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class ActionBase extends AnAction {

    public String getLanguageOfFile(Project project) {
        PsiFile psiFile = ActionBase.getPsiFile(project);
        if (psiFile == null) {
            return null;
        }
        return psiFile.getLanguage().getDisplayName();
    }

    @Nullable
    private static PsiFile getPsiFile(Project project) {
        VirtualFile virtualFile = ActionBase.getVirtualFile(project);
        if (virtualFile == null) {
            return null;
        }
        return PsiManager.getInstance(project).findFile(virtualFile);
    }

    public String getFileContent(Project project) {
        Document document = ActionBase.getDocument(project);
        if (document == null) {
            return null;
        }
        return document.getText();
    }

    @Nullable
    public static Document getDocument(Project project) {
        VirtualFile virtualFile = ActionBase.getVirtualFile(project);
        if (virtualFile == null) {
            return null;
        }
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        return fileDocumentManager.getDocument(virtualFile);
    }

    public static String getSelectedText(@NotNull AnActionEvent e) {
        String selectedText;
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (editor != null && (selectedText = editor.getSelectionModel().getSelectedText()) != null && !selectedText.isEmpty()) {
            return selectedText;
        }
        return null;
    }

    public void replaceSelectedText(@NotNull AnActionEvent e, String newText) {
        String selectedText;
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (editor != null && (selectedText = editor.getSelectionModel().getSelectedText()) != null && !selectedText.isEmpty()) {
            WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                editor.getDocument().replaceString(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd(), newText + (selectedText.endsWith("\n") ? "\n" : ""));
                ActionBase.reformatSelectedText(e.getProject(), editor);
            });
        }
    }

    @Nullable
    private static VirtualFile getVirtualFile(Project project) {
        TextEditor textEditor = (TextEditor) FileEditorManager.getInstance(project).getSelectedEditor();
        if (textEditor == null) {
            return null;
        }
        return textEditor.getFile();
    }

    private static void reformatSelectedText(Project project, Editor editor) {
        PsiFile psiFile = PsiDocumentManager.getInstance(Objects.requireNonNull(project)).getPsiFile(editor.getDocument());
        if (psiFile != null) {
            CodeStyleManager.getInstance(project).reformatText(psiFile, editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd());
        }
    }

}