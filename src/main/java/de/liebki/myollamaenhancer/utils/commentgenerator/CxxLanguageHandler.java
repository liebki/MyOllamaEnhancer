package de.liebki.myollamaenhancer.commentgenerator;

import de.liebki.myollamaenhancer.utils.FileExtensions;
import de.liebki.myollamaenhancer.utils.MethodInfo;

import java.util.ArrayList;
import java.util.List;

public class CxxLanguageHandler implements LanguageHandler {
    @Override
    public boolean isApplicable(final String fileExtension) {
        return FileExtensions.isCxxFamilyExt(fileExtension);
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

            if (this.isPossibleFunctionSignatureStart(trimmed, sigBuffer)) {
                sigStartOffset = offset;
            }

            if (this.shouldAccumulateSignature(sigBuffer, trimmed)) {
                sigBuffer.append(line).append("\n");
                if (trimmed.endsWith("{")) {
                    this.addMethodInfoForBraceOnSameLine(offset, line, sigStartOffset, fileContent, methods);
                    sigBuffer.setLength(0);
                } else if (trimmed.endsWith(")")) {
                    this.processLookaheadForFunctionBody(lines, i, offset, sigStartOffset, fileContent, methods);
                    sigBuffer.setLength(0);
                }
            }

            braceDepth = LanguageHandlerUtils.updateBraceDepth(line, braceDepth);
            offset += line.length() + 1;
        }
        return methods;
    }

    private boolean isPossibleFunctionSignatureStart(final String trimmed, final StringBuilder sigBuffer) {
        // Accept C/C++ function signatures (free functions, static, inline, extern, class/struct methods)
        return sigBuffer.isEmpty() && (
            trimmed.matches("(inline|static|extern)?\\s*[a-zA-Z_][a-zA-Z0-9_:*\\&\\s<>]*\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\([^;]*\\)\\s*(const)?\\s*(override)?\\s*(final)?\\s*(\\{|$)")
        );
    }

    private boolean shouldAccumulateSignature(final StringBuilder sigBuffer, final String trimmed) {
        // Accept C/C++ function signatures
        return !sigBuffer.isEmpty() ||
            trimmed.matches("(inline|static|extern)?\\s*[a-zA-Z_][a-zA-Z0-9_:*\\&\\s<>]*\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\([^;]*\\)\\s*(const)?\\s*(override)?\\s*(final)?\\s*(\\{|$)");
    }

    private void processLookaheadForFunctionBody(final String[] lines, final int i, final int offset, final int sigStartOffset, final String fileContent, final List<MethodInfo> methods) {
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


    @Override
    public String insertComment(final String fileContent, final MethodInfo method, final String commentText) {
        // Use JSDoc-style (/** ... */) for Doxygen compatibility
        return LanguageHandlerUtils.insertJSDocComment(fileContent, method, commentText);
    }
} 