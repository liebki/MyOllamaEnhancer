package de.liebki.myollamaenhancer.ts;

import de.liebki.myollamaenhancer.utils.FileExtensions;
import de.liebki.myollamaenhancer.utils.MethodInfo;
import de.liebki.myollamaenhancer.commentgenerator.LanguageHandler;
import de.liebki.myollamaenhancer.commentgenerator.LanguageHandlerUtils;

import java.util.ArrayList;
import java.util.List;

public class TypeScriptLanguageHandler implements LanguageHandler {

    @Override
    public boolean isApplicable(final String fileExtension) {
        return FileExtensions.isTypeScriptExt(fileExtension);
    }

    @Override
    public List<MethodInfo> findMethods(final String fileContent) {
        final List<MethodInfo> methods = new ArrayList<>();
        final String[] lines = fileContent.split("\n", -1);

        final TypeScriptContext context = new TypeScriptContext();
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            final String trimmed = line.trim();

            this.updateContext(trimmed, context);
            this.processLine(lines, i, line, trimmed, fileContent, context, methods);

            this.updateBraceDepthAndOffset(line, context);
        }

        return methods;
    }

    private void updateContext(final String trimmed, final TypeScriptContext context) {
        if (trimmed.startsWith("class ")) {
            context.setInClass(true);
            context.setInObject(false);
            context.setInInterface(false);

        } else if (trimmed.startsWith("interface ")) {
            context.setInClass(false);
            context.setInObject(false);
            context.setInInterface(true);

        } else if (this.isObjectDeclaration(trimmed)) {
            context.setInClass(false);
            context.setInObject(true);
            context.setInInterface(false);

        } else if ("}".equals(trimmed) && 1 == context.getBraceDepth()) {
            context.setInClass(false);
            context.setInObject(false);
            context.setInInterface(false);
        }
    }

    private boolean isObjectDeclaration(final String line) {
        return (line.startsWith("const ") && line.contains("= {") ||
                line.startsWith("let ") && line.contains("= {") ||
                line.startsWith("var ") && line.contains("= {"));
    }

    private void processLine(final String[] lines, final int currentLine, final String line, final String trimmed,
                             final String fileContent, final TypeScriptContext context, final List<MethodInfo> methods) {
        if (this.isPossibleMethodSignatureStart(trimmed, context)) {
            context.setSigStartOffset(context.getOffset());
        }

        if (this.shouldAccumulateSignature(trimmed, context)) {
            context.appendToSigBuffer(line).append("\n");
            this.processSignatureEnd(lines, currentLine, line, trimmed, fileContent, context, methods);
        }
    }

    private void processSignatureEnd(final String[] lines, final int currentLine, final String line, final String trimmed,
                                     final String fileContent, final TypeScriptContext context, final List<MethodInfo> methods) {
        if (trimmed.endsWith(")")) {
            this.processLookaheadForMethodBody(lines, currentLine, context.getOffset(),
                    context.getSigStartOffset(), fileContent, methods);
            context.clearSigBuffer();
        } else if (trimmed.endsWith("{")) {
            this.addMethodInfoForBraceOnSameLine(context.getOffset(), line,
                    context.getSigStartOffset(), fileContent, methods);
            context.clearSigBuffer();
        } else if (trimmed.contains("=>")) {
            this.addMethodInfoForArrowFunction(context.getOffset(), line,
                    context.getSigStartOffset(), fileContent, methods);
            context.clearSigBuffer();
        } else if (trimmed.endsWith(";") && context.isInInterface()) {
            this.addMethodInfoForInterfaceMethod(context.getOffset(), line,
                    context.getSigStartOffset(), fileContent, methods);
            context.clearSigBuffer();
        }
    }

    private void updateBraceDepthAndOffset(final String line, final TypeScriptContext context) {
        final int newBraceDepth = LanguageHandlerUtils.updateBraceDepth(line, context.getBraceDepth());
        context.setBraceDepth(newBraceDepth);

        context.incrementOffset(line.length() + 1);
    }

    private boolean isPossibleMethodSignatureStart(final String trimmed, final TypeScriptContext context) {
        return context.isSigBufferEmpty() && this.isMethodPattern(trimmed, context);
    }

    private boolean shouldAccumulateSignature(final String trimmed, final TypeScriptContext context) {
        return !context.isSigBufferEmpty() || this.isMethodPattern(trimmed, context);
    }

    private boolean isMethodPattern(final String trimmed, final TypeScriptContext context) {
        return this.isFunctionDeclaration(trimmed) ||
                this.isArrowFunction(trimmed) ||
                this.isClassMethod(trimmed, context) ||
                this.isObjectMethod(trimmed, context) ||
                this.isInterfaceMethod(trimmed, context);
    }

    private boolean isFunctionDeclaration(final String line) {
        return line.startsWith("function ") || line.startsWith("async function ");
    }

    private boolean isArrowFunction(final String line) {
        if (!(line.startsWith("const ") || line.startsWith("let ") || line.startsWith("var "))) {
            return false;
        }
        return (line.contains(": (") || line.contains("= (")) && line.contains("=>");
    }

    private boolean isClassMethod(final String line, final TypeScriptContext context) {
        if (!context.isInClass() || 1 != context.getBraceDepth()) {
            return false;
        }
        return line.matches("^(public|private|protected|static)?\\s*\\w+\\s*\\([^)]*\\)\\s*:\\s*[^{]*\\{?$") ||
                line.matches("^\\w+\\s*\\([^)]*\\)\\s*\\{?$");
    }

    private boolean isObjectMethod(final String line, final TypeScriptContext context) {
        if (!context.isInObject() || 1 != context.getBraceDepth()) {
            return false;
        }
        return line.matches("^\\w+\\s*:\\s*function\\s*\\([^)]*\\)\\s*\\{?$") ||
                line.matches("^\\w+\\s*\\([^)]*\\)\\s*\\{?$");
    }

    private boolean isInterfaceMethod(final String line, final TypeScriptContext context) {
        return context.isInInterface() &&
                1 == context.getBraceDepth() &&
                line.matches("^\\w+\\s*\\([^)]*\\)\\s*:\\s*[^;]*;$");
    }

    private void processLookaheadForMethodBody(final String[] lines, final int i, final int offset, final int sigStartOffset, final String fileContent, final List<MethodInfo> methods) {
        int lookahead = i + 1;
        int lookaheadOffset = offset + lines[i].length() + 1;

        while (lookahead < lines.length) {
            final String la = lines[lookahead].trim();
            if (la.isEmpty() || la.startsWith("//") || la.startsWith("/*")) {
                lookaheadOffset += lines[lookahead].length() + 1;
                lookahead++;
                continue;
            }
            if (la.startsWith("{")) {
                final int bodyStart = lookaheadOffset + lines[lookahead].indexOf('{');
                final int start = sigStartOffset;

                final int end = LanguageHandlerUtils.findClosingBrace(fileContent, bodyStart);
                final String methodText = fileContent.substring(start, end);

                methods.add(new MethodInfo(start, end, bodyStart, methodText));
            }

            break;
        }
    }

    private void addMethodInfoForBraceOnSameLine(final int offset, final String line, final int sigStartOffset, final String fileContent, final List<MethodInfo> methods) {
        final int bodyStart = offset + line.indexOf('{');
        final int start = sigStartOffset;

        final int end = LanguageHandlerUtils.findClosingBrace(fileContent, bodyStart);
        final String methodText = fileContent.substring(start, end);

        methods.add(new MethodInfo(start, end, bodyStart, methodText));
    }

    private void addMethodInfoForArrowFunction(final int offset, final String line, final int sigStartOffset, final String fileContent, final List<MethodInfo> methods) {
        // For arrow functions with inline expressions, the entire line is the method
        final int start = sigStartOffset;
        final int end = offset + line.length();

        final String methodText = fileContent.substring(start, end);
        final int bodyStart = start;

        methods.add(new MethodInfo(start, end, bodyStart, methodText));
    }

    private void addMethodInfoForInterfaceMethod(final int offset, final String line, final int sigStartOffset, final String fileContent, final List<MethodInfo> methods) {
        // For interface methods, the entire signature is the method (no body)
        final int start = sigStartOffset;
        final int end = offset + line.length();

        final String methodText = fileContent.substring(start, end);
        final int bodyStart = start;

        methods.add(new MethodInfo(start, end, bodyStart, methodText));
    }

    @Override
    public String insertComment(final String fileContent, final MethodInfo method, final String commentText) {
        return LanguageHandlerUtils.insertJSDocComment(fileContent, method, commentText);
    }
} 