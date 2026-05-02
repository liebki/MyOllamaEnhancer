package de.liebki.myollamaenhancer.commentgenerator;

import de.liebki.myollamaenhancer.utils.CodeRegion;
import de.liebki.myollamaenhancer.utils.FileExtensions;
import de.liebki.myollamaenhancer.utils.MethodInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSharpLanguageHandler implements LanguageHandler {
    @Override
    public boolean isApplicable(final String fileExtension) {
        return FileExtensions.isCSharpExt(fileExtension);
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
                    // Only treat as a method if a complete signature was seen (must include ')')
                    final String accumulated = sigBuffer.toString();
                    if (accumulated.contains(")")) {
                        this.addMethodInfoForBraceOnSameLine(offset, line, sigStartOffset, fileContent, methods);
                    }
                    // Reset regardless
                    sigBuffer.setLength(0);
                }
            }

            braceDepth = LanguageHandlerUtils.updateBraceDepth(line, braceDepth);
            offset += line.length() + 1;
        }
        return methods;
    }

    @Override
    public String insertComment(final String fileContent, final MethodInfo method, final String commentText) {
        return LanguageHandlerUtils.insertCSharpComment(fileContent, method, commentText);
    }

    /**
     * For .razor files, only process C# code blocks (e.g., @code { ... })
     * For .cs files, process the whole file.
     */
    @Override
    public List<CodeRegion> findCodeRegions(final String fileContent, final String fileExtension) {
        final List<CodeRegion> regions = new ArrayList<>();
        if ("razor".equalsIgnoreCase(fileExtension)) {
            final Pattern codeBlockPattern = Pattern.compile("@code[ \t]*\\{", Pattern.CASE_INSENSITIVE);
            final Matcher matcher = codeBlockPattern.matcher(fileContent);

            while (matcher.find()) {
                final int start = matcher.end();
                final int end = LanguageHandlerUtils.findClosingBrace(fileContent, matcher.end() - 1);

                regions.add(new CodeRegion(start, end, "cs"));
            }
        } else {
            regions.add(new CodeRegion(0, fileContent.length(), "cs"));
        }
        return regions;
    }

    private boolean isPossibleMethodSignatureStart(final String trimmed, final int braceDepth, final StringBuilder sigBuffer) {
        // Consider potential method starts when inside a type body (works for both file-scoped and block-scoped namespaces)
        if (braceDepth < 1 || !sigBuffer.isEmpty()) return false;

        // Exclude type/namespace declarations
        final String low = trimmed.toLowerCase();
        if (low.startsWith("namespace") || low.contains(" class ") || low.startsWith("class ") ||
            low.contains(" interface ") || low.startsWith("interface ") ||
            low.contains(" struct ") || low.startsWith("struct ") ||
            low.contains(" record ") || low.startsWith("record ") ||
            low.startsWith("enum ") || low.contains(" enum ")) {
            return false;
        }

        // Exclude control-flow starts
        if (low.startsWith("if ") || low.startsWith("if(") ||
            low.startsWith("for ") || low.startsWith("for(") ||
            low.startsWith("foreach ") || low.startsWith("foreach(") ||
            low.startsWith("while ") || low.startsWith("while(") ||
            low.startsWith("switch ") || low.startsWith("switch(") ||
            low.startsWith("lock ") || low.startsWith("lock(") ||
            low.startsWith("using ") || low.startsWith("using(") ||
            low.startsWith("try") || low.startsWith("catch") || low.startsWith("finally") ||
            low.startsWith("else") || low.startsWith("do ") || low.equals("do")) {
            return false;
        }

        // Always allow starts with modifiers (method name can be on next lines)
        if (trimmed.startsWith("public") || trimmed.startsWith("protected") || trimmed.startsWith("private") ||
            trimmed.startsWith("internal") || trimmed.startsWith("static") || trimmed.startsWith("virtual") ||
            trimmed.startsWith("override") || trimmed.startsWith("async") || trimmed.startsWith("extern")) {
            return true;
        }

        // If line starts with a return type (primitive/custom), require that it already contains '(' to avoid picking variable declarations
        if (trimmed.startsWith("void") || trimmed.startsWith("int") || trimmed.startsWith("bool") ||
            trimmed.startsWith("string") || trimmed.startsWith("char") || trimmed.startsWith("float") ||
            trimmed.startsWith("double") || trimmed.startsWith("decimal") || trimmed.startsWith("byte") ||
            trimmed.startsWith("short") || trimmed.startsWith("long") || trimmed.startsWith("object") ||
            trimmed.matches("[a-zA-Z0-9_<>\\[\\]]+")) {
            return trimmed.contains("(");
        }

        return false;
    }

    private boolean shouldAccumulateSignature(final StringBuilder sigBuffer, final int braceDepth, final String trimmed) {
        if (!sigBuffer.isEmpty()) return true;
        if (braceDepth < 1) return false;

        // Exclude type/namespace declarations while accumulating
        final String low = trimmed.toLowerCase();
        if (low.startsWith("namespace") || low.contains(" class ") || low.startsWith("class ") ||
            low.contains(" interface ") || low.startsWith("interface ") ||
            low.contains(" struct ") || low.startsWith("struct ") ||
            low.contains(" record ") || low.startsWith("record ") ||
            low.startsWith("enum ") || low.contains(" enum ")) {
            return false;
        }

        // Exclude control-flow starts while accumulating
        if (low.startsWith("if ") || low.startsWith("if(") ||
            low.startsWith("for ") || low.startsWith("for(") ||
            low.startsWith("foreach ") || low.startsWith("foreach(") ||
            low.startsWith("while ") || low.startsWith("while(") ||
            low.startsWith("switch ") || low.startsWith("switch(") ||
            low.startsWith("lock ") || low.startsWith("lock(") ||
            low.startsWith("using ") || low.startsWith("using(") ||
            low.startsWith("try") || low.startsWith("catch") || low.startsWith("finally") ||
            low.startsWith("else") || low.startsWith("do ") || low.equals("do")) {
            return false;
        }

        // Start accumulating if line begins with a modifier or if a return-type line already contains '(' (to avoid variable decls)
        if (trimmed.startsWith("public") || trimmed.startsWith("protected") || trimmed.startsWith("private") ||
            trimmed.startsWith("internal") || trimmed.startsWith("static") || trimmed.startsWith("virtual") ||
            trimmed.startsWith("override") || trimmed.startsWith("async") || trimmed.startsWith("extern")) {
            return true;
        }

        if (trimmed.startsWith("void") || trimmed.startsWith("int") || trimmed.startsWith("bool") ||
            trimmed.startsWith("string") || trimmed.startsWith("char") || trimmed.startsWith("float") ||
            trimmed.startsWith("double") || trimmed.startsWith("decimal") || trimmed.startsWith("byte") ||
            trimmed.startsWith("short") || trimmed.startsWith("long") || trimmed.startsWith("object") ||
            trimmed.matches("[a-zA-Z0-9_<>\\[\\]]+")) {
            return trimmed.contains("(");
        }

        return false;
    }

    private void processLookaheadForMethodBody(final String[] lines, final int i, final int offset, final int sigStartOffset, final String fileContent, final List<MethodInfo> methods) {
        int lookahead = i + 1;
        int lookaheadOffset = offset + lines[i].length() + 1;

        while (lookahead < lines.length) {
            final String la = lines[lookahead].trim();
            if (la.isEmpty() || la.startsWith("//") || la.startsWith("/*") || la.startsWith("@") || la.startsWith("[")) {
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

    private void addMethodInfo(final List<MethodInfo> methods, final String fileContent, final int start, final int end, final int bodyStart) {
        final String methodText = fileContent.substring(start, end);
        methods.add(new MethodInfo(start, end, bodyStart, methodText));
    }

    private static class LookaheadResult {
        int start;
        int end;
        int bodyStart;
        LookaheadResult(final int start, final int end, final int bodyStart) {
            this.start = start;
            this.end = end;
            this.bodyStart = bodyStart;
        }
    }
} 