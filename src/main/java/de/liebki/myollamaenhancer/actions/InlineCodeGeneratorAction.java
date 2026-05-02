package de.liebki.myollamaenhancer.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import de.liebki.myollamaenhancer.utils.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;

public class InlineCodeGeneratorAction extends ActionBase implements DumbAware {
    private static final Logger LOG = Logger.getInstance(InlineCodeGeneratorAction.class);

    private static final String GEN_MARKER = "@gen";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (null == project || null == editor) {
            return;
        }

        // Get the language from the current PsiFile (more reliable than extension/plugins like TextMate)
        String language = this.getLanguageOfFile(project);
        if (null == language || language.isEmpty()) {
            language = "Java"; // Default fallback
        }

        // Check if a @gen block exists
        if (this.isInGenBlock(editor, language)) {
            // Check if cursor is inside the @gen block
            if (this.isCursorInsideGenBlock(editor, language)) {
                // Process the existing @gen block
                this.processGenBlock(editor, project, language);
            } else {
                // Cursor is outside the block, delete it
                this.removeAnyExistingGenBlocks(editor, language);
            }
        } else {
            // No block exists, create a new one
            this.createGenBlock(editor, language);
        }
    }

    private String getGenBlockStart(final String language) {
        final String prefix = LanguageUtils.getCommentPrefix(language);
        final String suffix = LanguageUtils.getCommentSuffix(language);
        return prefix + InlineCodeGeneratorAction.GEN_MARKER + suffix + "\n";
    }

    private String getGenBlockEnd(final String language) {
        final String prefix = LanguageUtils.getCommentPrefix(language);
        final String suffix = LanguageUtils.getCommentSuffix(language);
        return "\n" + prefix + InlineCodeGeneratorAction.GEN_MARKER + suffix;
    }

    /**
     * Finds the start and end indices of the first @gen block in the document for the given language.
     * @return int[]{startIndex, endIndex} or null if not found
     */
    private int[] findGenBlockBounds(final Document document, final String language) {
        final String documentText = document.getText();
        final String blockStart = this.getGenBlockStart(language);
        final String blockEnd = this.getGenBlockEnd(language);
        final int startIndex = documentText.indexOf(blockStart);
        if (-1 == startIndex) return null;
        final int endIndex = documentText.indexOf(blockEnd, startIndex);
        if (-1 == endIndex) return null;
        return new int[]{startIndex, endIndex};
    }

    /**
     * Extracts the prompt from a gen block, removing the comment prefix if present.
     */
    private String extractPromptFromGenBlock(final Document document, final int startIndex, final int endIndex, final String language) {
        final String documentText = document.getText();
        final int promptStart = startIndex + this.getGenBlockStart(language).length();
        String prompt = documentText.substring(promptStart, endIndex).trim();
        final String promptPrefix = LanguageUtils.getCommentPrefix(language);
        if (prompt.startsWith(promptPrefix)) {
            prompt = prompt.substring(promptPrefix.length()).trim();
        }
        return prompt;
    }

    private boolean isInGenBlock(final Editor editor, final String language) {
        final Document document = editor.getDocument();
        final int[] bounds = this.findGenBlockBounds(document, language);
        return null != bounds;
    }

    private boolean isCursorInsideGenBlock(final Editor editor, final String language) {
        final Document document = editor.getDocument();
        final int cursorOffset = editor.getCaretModel().getOffset();
        final int[] bounds = this.findGenBlockBounds(document, language);
        if (null == bounds) return false;
        final int startIndex = bounds[0];
        final int endIndex = bounds[1];
        final String blockEnd = this.getGenBlockEnd(language);
        return cursorOffset >= startIndex && cursorOffset <= endIndex + blockEnd.length();
    }

    private void createGenBlock(final Editor editor, final String language) {
        final Document document = editor.getDocument();
        final int offset = editor.getCaretModel().getOffset();

        WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
            final String blockStart = this.getGenBlockStart(language);
            final String blockEnd = this.getGenBlockEnd(language);
            final String promptPrefix = LanguageUtils.getCommentPrefix(language);
            final String genBlock = "\n" + blockStart + promptPrefix + "Type your prompt here" + blockEnd + "\n";

            document.insertString(offset, genBlock);

            // Position cursor in the middle line (after the comment line)
            final int promptStart = offset + blockStart.length() + promptPrefix.length() + 1; // +1 for newline
            editor.getCaretModel().moveToOffset(promptStart);

            // Select the placeholder text for easy replacement
            editor.getSelectionModel().setSelection(promptStart, promptStart + "Type your prompt here".length());

            // Add key listener for ESC to abort
            this.addEscapeListener(editor, language);
        });
    }

    private void addEscapeListener(final Editor editor, final String language) {
        editor.getContentComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
                    // Remove the @gen block
                    InlineCodeGeneratorAction.this.removeGenBlock(editor, language);
                    editor.getContentComponent().removeKeyListener(this);
                }
            }
        });
    }

    private void removeGenBlock(final Editor editor, final String language) {
        final Document document = editor.getDocument();
        final int[] bounds = this.findGenBlockBounds(document, language);
        if (null != bounds) {
            final int startIndex = bounds[0];
            final int endIndex = bounds[1];
            WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                final int endOfBlock = endIndex + this.getGenBlockEnd(language).length();
                document.deleteString(startIndex, endOfBlock);
            });
        }
    }

    private void removeAnyExistingGenBlocks(final Editor editor, final String language) {
        final Document document = editor.getDocument();
        while (true) {
            final int[] bounds = this.findGenBlockBounds(document, language);
            if (null == bounds) break;
            final int startIndex = bounds[0];
            final int endIndex = bounds[1];
            WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
                final int endOfBlock = endIndex + this.getGenBlockEnd(language).length();
                document.deleteString(startIndex, endOfBlock);
            });
        }
    }

    private void processGenBlock(final Editor editor, final Project project, final String language) {
        final Document document = editor.getDocument();
        final int[] bounds = this.findGenBlockBounds(document, language);
        if (null == bounds) return;
        final int startIndex = bounds[0];
        final int endIndex = bounds[1];
        final String prompt = this.extractPromptFromGenBlock(document, startIndex, endIndex, language);
        if (prompt.isEmpty() || "Type your prompt here".equals(prompt)) {
            return; // No valid prompt
        }

        // Notify start and show progress indicator for code generation
        NotificationUtil.info(project, "Generating code from @gen prompt...");
        ProgressManager.getInstance().run(new Task.Modal(project, "Generating Code...", true) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Generating code from prompt...");
                    indicator.setText2("This may take a moment");

                    // Build the system prompt for code generation
                    final String systemPrompt = StructuredPromptEnhancer.buildCodeGenerationSystemPrompt(language);

                    // Use structured output for better reliability
                    final Map<String, Object> structuredOutput = StructuredOutputSchemas.createCodeGenerationSchema();
                    final String enhancedPrompt = StructuredPromptEnhancer.enhanceForCodeGeneration(systemPrompt);

                    // Use the Ollama API to generate code
                    OllamaAPIUtil.generateStructuredOllamaResponse(project, enhancedPrompt, prompt, structuredOutput, responseObj -> {
                        // Log the raw structured output from the model
                        String response = responseObj.visibleContent();
                        LOG.debug("Raw structured output: " + response);

                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (null != response && !response.trim().isEmpty()) {
                                // Parse the structured response to extract the code
                                final String extractedCode = StructuredResponseParser.parseCodeResponse(response);
                                LOG.debug("Extracted code: " + extractedCode);
                                // Use the model's output directly without any cleaning
                                InlineCodeGeneratorAction.this.replaceGenBlockWithCode(editor, extractedCode, language);
                                NotificationUtil.info(project, "Inserted generated code");
                            }
                        });
                    });
                } catch (final Exception ex) {
                    InlineCodeGeneratorAction.this.errorLog(ex, getClass());
                    editor.getContentComponent().setCursor(Cursor.getDefaultCursor());
                }
            }
            
            @Override
            public void onCancel() {
                InlineCodeGeneratorAction.this.informUserCanceled(project, getClass(), "Code generation was cancelled.");
                editor.getContentComponent().setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private void replaceGenBlockWithCode(final Editor editor, final String code, final String language) {
        final Document document = editor.getDocument();
        final int[] bounds = this.findGenBlockBounds(document, language);
        if (null == bounds) return;
        final int startIndex = bounds[0];
        final int endIndex = bounds[1];
        WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
            // Replace the entire @gen block with the generated code
            final int endOfBlock = endIndex + this.getGenBlockEnd(language).length();
            document.replaceString(startIndex, endOfBlock, "\n" + code + "\n");
            // Position cursor after the generated code
            final int newOffset = startIndex + code.length() + 2; // +2 for newlines
            editor.getCaretModel().moveToOffset(newOffset);
        });
    }



    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);

        // Only enable if we have a project and editor
        final boolean enabled = this.validateProjectAndEditor(e);
        e.getPresentation().setEnabledAndVisible(enabled);

        if (enabled) {
            e.getPresentation().setText("Generate Code");
            e.getPresentation().setDescription("Generate code snippets inline using AI");
        }
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }


} 