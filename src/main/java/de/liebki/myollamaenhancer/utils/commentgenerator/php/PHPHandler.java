package de.liebki.myollamaenhancer.php;

import de.liebki.myollamaenhancer.utils.FileExtensions;
import de.liebki.myollamaenhancer.utils.MethodInfo;
import de.liebki.myollamaenhancer.commentgenerator.LanguageHandler;
import de.liebki.myollamaenhancer.commentgenerator.LanguageHandlerUtils;

import java.util.ArrayList;
import java.util.List;

public class PHPHandler implements LanguageHandler {

    @Override
    public boolean isApplicable(final String fileExtension) {
        return FileExtensions.isPhpExt(fileExtension);
    }

    @Override
    public List<MethodInfo> findMethods(final String fileContent) {
        final List<MethodInfo> methods = new ArrayList<>();
        final String[] lines = fileContent.split("\n", -1);
        final PHPContext context = new PHPContext();

        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            final String trimmed = line.trim();

            this.updateContext(trimmed, context);
            this.processLine(lines, i, line, trimmed, fileContent, context, methods);
            this.updateBraceDepthAndOffset(line, context);
        }

        return methods;
    }

    private void updateContext(final String trimmed, final PHPContext context) {
        if (trimmed.startsWith("class ")) {
            context.setInClass(true);
            context.setInTrait(false);
            context.setInInterface(false);
        } else if (trimmed.startsWith("trait ")) {
            context.setInClass(false);
            context.setInTrait(true);
            context.setInInterface(false);
        } else if (trimmed.startsWith("interface ")) {
            context.setInClass(false);
            context.setInTrait(false);
            context.setInInterface(true);
        } else if ("}".equals(trimmed) && 1 == context.getBraceDepth()) {
            context.setInClass(false);
            context.setInTrait(false);
            context.setInInterface(false);
        }
    }

    private void processLine(final String[] lines, final int currentLine, final String line, final String trimmed,
                             final String fileContent, final PHPContext context, final List<MethodInfo> methods) {
        if (this.isPossibleFunctionSignatureStart(trimmed, context)) {
            context.setSigStartOffset(context.getOffset());
        }

        if (this.shouldAccumulateSignature(trimmed, context)) {
            context.appendToSigBuffer(line).append("\n");
            this.processSignatureEnd(lines, currentLine, line, trimmed, fileContent, context, methods);
        }
    }

    private void processSignatureEnd(final String[] lines, final int currentLine, final String line, final String trimmed,
                                     final String fileContent, final PHPContext context, final List<MethodInfo> methods) {
        if (trimmed.endsWith("{")) {
            this.addMethodInfoForBraceOnSameLine(context.getOffset(), line,
                    context.getSigStartOffset(), fileContent, methods);
            context.clearSigBuffer();
        } else if (trimmed.endsWith(")")) {
            this.processLookaheadForFunctionBody(lines, currentLine, context.getOffset(),
                    context.getSigStartOffset(), fileContent, methods);
            context.clearSigBuffer();
        }
    }

    private void updateBraceDepthAndOffset(final String line, final PHPContext context) {
        final int newBraceDepth = LanguageHandlerUtils.updateBraceDepth(line, context.getBraceDepth());
        context.setBraceDepth(newBraceDepth);
        context.incrementOffset(line.length() + 1);
    }

    private boolean isPossibleFunctionSignatureStart(final String trimmed, final PHPContext context) {
        if (!context.isSigBufferEmpty()) return false;

        return this.isRegularFunction(trimmed) ||
                this.isAnonymousFunction(trimmed) ||
                this.isClassMethod(trimmed, context);
    }

    private boolean isRegularFunction(final String line) {
        return line.startsWith("function ");
    }

    private boolean isAnonymousFunction(final String line) {
        return line.contains("= function(");
    }

    private boolean isClassMethod(final String line, final PHPContext context) {
        if (!context.isInClassOrTraitOrInterface() || 1 != context.getBraceDepth()) {
            return false;
        }

        final boolean hasVisibilityModifier = line.contains("public") || line.contains("private") || line.contains("protected");
        final boolean hasOtherModifier = line.contains("static") || line.contains("abstract") || line.contains("final");
        final boolean hasFunctionKeyword = line.contains("function");

        return (hasFunctionKeyword && (hasVisibilityModifier || hasOtherModifier)) ||
                (hasFunctionKeyword && !hasVisibilityModifier && !hasOtherModifier) ||
                line.contains("__construct");
    }

    private boolean shouldAccumulateSignature(final String trimmed, final PHPContext context) {
        if (!context.isSigBufferEmpty()) return true;
        return this.isPossibleFunctionSignatureStart(trimmed, context);
    }

    private void processLookaheadForFunctionBody(final String[] lines, final int i, final int offset, final int sigStartOffset, final String fileContent, final List<MethodInfo> methods) {
        int lookahead = i + 1;
        int lookaheadOffset = offset + lines[i].length() + 1;

        while (lookahead < lines.length) {
            final String la = lines[lookahead].trim();
            if (la.isEmpty() || la.startsWith("//") || la.startsWith("/*") || la.startsWith("#")) {
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

    @Override
    public String insertComment(final String fileContent, final MethodInfo method, final String commentText) {
        return LanguageHandlerUtils.insertPHPDocComment(fileContent, method, commentText);
    }
} 