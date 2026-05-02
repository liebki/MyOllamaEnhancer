package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import de.liebki.myollamaenhancer.utils.*;
import de.liebki.myollamaenhancer.commentgenerator.LanguageHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugAddCommentsAction extends ActionBase implements DumbAware {
    private static final Logger LOG = Logger.getInstance(DebugAddCommentsAction.class);

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        final Project project = e.getProject();
        LOG.info("project=" + project);

        VirtualFile[] virtualFileRef = {this.getVirtualFileFromEvent(e) };
        LOG.info("virtualFile=" + virtualFileRef[0]);

        // Fallback: try to get file from editor/document if virtualFile is null
        if (null == virtualFileRef[0]) {
            final Editor editor = this.getEditorFromEvent(e);
            LOG.info("editor=" + editor);

            if (null != editor) {
                final Document document = editor.getDocument();
                virtualFileRef[0] = FileDocumentManager.getInstance().getFile(document);
                LOG.info("fallback virtualFile from editor=" + virtualFileRef[0]);
            }
        }

        if (null == project || null == virtualFileRef[0]) {
            LOG.warn("No project or file in context. Aborting.");
            return;
        }

        final String fileExtension = virtualFileRef[0].getExtension();
        LOG.info("fileExtension=" + fileExtension);

        final String fileContent = this.getFileContentFromEditorOrVirtualFile(project, virtualFileRef[0]);
        LOG.info("fileContent is null? " + (null == fileContent));

        final LanguageHandler handler = this.findLanguageHandler(fileExtension);

        if (null == handler) {
            LOG.warn("No handler found for extension: " + fileExtension);
            return;
        }

        LOG.info("Handler found: " + handler.getClass().getSimpleName());

        // Notify start and run progress task
        NotificationUtil.info(project, "Adding method comments...");
        ProgressManager.getInstance().run(new Task.Modal(project, "Adding Comments with Ollama", true) {
            boolean hasFailures;

            @Override
            public void run(final ProgressIndicator indicator) {
                try {
                    LOG.info("Background task started");
                    final List<MethodInfo> methods = handler.findMethods(fileContent);

                    LOG.info("Methods found: " + methods.size());
                    final String newContent = fileContent;

                    final Map<MethodInfo, String> methodToComment = new HashMap<>();
                    for (final MethodInfo method : methods) {
                        indicator.checkCanceled();
                        LOG.debug("Processing method at offset: " + method.startOffset);
                        LOG.debug("Code:\n" + method.methodSource);

                        final OllamaAPIUtil.ThinkContent ollamaResponse = OllamaAPIUtil.generateStructuredOllamaResponseSync(
                                method.methodSource,
                                "Generate a concise, natural-language method documentation comment.",
                                StructuredOutputSchemas.createUniversalCommentSchema()
                        );

                        LOG.debug("Ollama response: " + ollamaResponse);
                        final String rawComment = OllamaCommentParser.extractCommentText(ollamaResponse.visibleContent());

                        LOG.debug("Extracted comment: " + rawComment);
                        methodToComment.put(method, rawComment.trim());
                    }

                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        String[] updatedContent = { newContent };
                        // Insert comments in reverse order of method start offsets
                        methods.stream()
                                .sorted((m1, m2) -> Integer.compare(m2.startOffset, m1.startOffset))
                                .forEach(method -> {
                                    final String comment = methodToComment.get(method);
                                    LOG.debug("Inserting comment for method at offset: " + method.startOffset);
                                    updatedContent[0] = handler.insertComment(updatedContent[0], method, comment);
                                });

                        LOG.info("Replacing file content");
                        DebugAddCommentsAction.this.replaceFileContent(project, virtualFileRef[0], updatedContent[0]);
                    });

                } catch (final Exception ex) {
                    DebugAddCommentsAction.this.errorLog(ex, getClass());
                    this.hasFailures = true;
                }

                // Show warning dialog if there were API failures
                if (this.hasFailures) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        DialogUtil.showWarning(
                                "Some methods could not be commented due to Ollama API issues. " +
                                        "Please ensure your Ollama server is running and the model is available. " +
                                        "You may need to pull the model first!",
                                "Ollama API Issues"
                        );
                    });
                } else {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.info(project, "All possible Comments were inserted");
                    });
                }

            }

            @Override
            public void onCancel() {
                NotificationUtil.info(project, "Add comments was canceled.");
            }

        });
    }

    @Override
    public void update(final AnActionEvent e) {
        final VirtualFile file = this.getVirtualFileFromEvent(e);
        e.getPresentation().setEnabledAndVisible(null != file && !file.isDirectory());
    }
} 