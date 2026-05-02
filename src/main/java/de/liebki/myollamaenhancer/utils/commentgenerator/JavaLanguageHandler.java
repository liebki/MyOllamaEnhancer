package de.liebki.myollamaenhancer.commentgenerator;

import de.liebki.myollamaenhancer.utils.FileExtensions;
import de.liebki.myollamaenhancer.utils.MethodInfo;

import java.util.ArrayList;
import java.util.List;

public class JavaLanguageHandler implements LanguageHandler {
    @Override
    public boolean isApplicable(final String fileExtension) {
        return FileExtensions.isJavaExt(fileExtension);
    }

    @Override
    public List<MethodInfo> findMethods(final String fileContent) {
        final List<MethodInfo> methods = new ArrayList<>();
        final String[] lines = fileContent.split("\n", -1);

        int braceDepth = 0;
        int offset = 0;

        int sigStartOffset = 0;
        final StringBuilder sigBuffer = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            final String trimmed = line.trim();

            if (this.isPossibleMethodSignatureStart(trimmed, braceDepth, sigBuffer)) {
                sigStartOffset = offset;
            }

            if (this.shouldAccumulateSignature(sigBuffer, braceDepth, trimmed)) {
                sigBuffer.append(line).append("\n");
                if (trimmed.endsWith(")")) {
                    this.processLookaheadForMethodBody(lines, i, offset, sigStartOffset, fileContent, methods);
                    sigBuffer.setLength(0);
                } else if (trimmed.endsWith("{")) {
                    this.addMethodInfoForBraceOnSameLine(offset, line, sigStartOffset, fileContent, methods);
                    sigBuffer.setLength(0);
                }
            }

            braceDepth = LanguageHandlerUtils.updateBraceDepth(line, braceDepth);
            offset += line.length() + 1;
        }
        return methods;
    }

    private boolean isPossibleMethodSignatureStart(final String trimmed, final int braceDepth, final StringBuilder sigBuffer) {
        return 1 == braceDepth && sigBuffer.isEmpty() && (trimmed.startsWith("public") || trimmed.startsWith("protected") || trimmed.startsWith("private") || trimmed.startsWith("static") || trimmed.startsWith("final") || trimmed.startsWith("abstract") || trimmed.startsWith("synchronized") || trimmed.startsWith("native") || trimmed.matches("[a-zA-Z0-9_<>\\[\\]]+"));
    }

    private boolean shouldAccumulateSignature(final StringBuilder sigBuffer, final int braceDepth, final String trimmed) {
        return !sigBuffer.isEmpty() || (1 == braceDepth && (trimmed.startsWith("public") || trimmed.startsWith("protected") || trimmed.startsWith("private") || trimmed.startsWith("static") || trimmed.startsWith("final") || trimmed.startsWith("abstract") || trimmed.startsWith("synchronized") || trimmed.startsWith("native") || trimmed.matches("[a-zA-Z0-9_<>\\[\\]]+")));
    }

    private void processLookaheadForMethodBody(final String[] lines, final int i, final int offset, final int sigStartOffset, final String fileContent, final List<MethodInfo> methods) {
        int lookahead = i + 1;
        int lookaheadOffset = offset + lines[i].length() + 1;

        while (lookahead < lines.length) {
            final String la = lines[lookahead].trim();
            if (la.isEmpty() || la.startsWith("//") || la.startsWith("/*") || la.startsWith("@")) {
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
        return LanguageHandlerUtils.insertJSDocComment(fileContent, method, commentText);
    }

} 