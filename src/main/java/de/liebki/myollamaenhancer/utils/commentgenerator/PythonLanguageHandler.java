package de.liebki.myollamaenhancer.commentgenerator;

import de.liebki.myollamaenhancer.utils.FileExtensions;
import de.liebki.myollamaenhancer.utils.MethodInfo;

import java.util.ArrayList;
import java.util.List;

public class PythonLanguageHandler implements LanguageHandler {
    @Override
    public boolean isApplicable(final String fileExtension) {
        return FileExtensions.isPythonExt(fileExtension);
    }

    @Override
    public List<MethodInfo> findMethods(final String fileContent) {
        final List<MethodInfo> methods = new ArrayList<>();
        final List<FunctionBoundary> functionBoundaries = this.collectAllFunctionBoundaries(fileContent);
        
        // Create MethodInfo objects with correct boundaries
        for (int i = 0; i < functionBoundaries.size(); i++) {
            final FunctionBoundary current = functionBoundaries.get(i);
            
            // Determine end offset
            final int endOffset;
            if (i == functionBoundaries.size() - 1) {
                // Last function: end at file end
                endOffset = fileContent.length();
            } else {
                // Not last function: end at next function start
                endOffset = functionBoundaries.get(i + 1).startOffset;
            }

            final String methodSource = fileContent.substring(current.startOffset, endOffset);
            methods.add(new MethodInfo(current.startOffset, endOffset, current.startOffset, methodSource));
        }
        
        return methods;
    }

    private record FunctionBoundary(int startOffset) {

    }
    
    private List<FunctionBoundary> collectAllFunctionBoundaries(final String fileContent) {
        final List<FunctionBoundary> boundaries = new ArrayList<>();
        final String[] lines = fileContent.split("\\n", -1);
        final int lineCount = lines.length;
        int i = 0;
        
        while (i < lineCount) {
            int decoratorStart = -1;
            int j = i;
            
            while (j < lineCount && this.isDecoratorLine(lines[j].trim())) {
                if (-1 == decoratorStart) {
                    decoratorStart = this.calculateOffset(lines, j);
                }
                j++;
            }
            
            // Check if this line is a function definition
            if (j < lineCount && this.isFunctionDefLine(lines[j].trim())) {
                // Found a function - record its start (including decorators)
                final int functionStart = (-1 != decoratorStart) ? decoratorStart : this.calculateOffset(lines, j);
                boundaries.add(new FunctionBoundary(functionStart));
                
                // Skip to the end of the signature
                final int sigEndLine = this.findSignatureEnd(lines, j, lineCount);
                i = sigEndLine + 1; // Move to next line after signature
            } else {
                i++;
            }
        }
        
        return boundaries;
    }
    
    private int calculateOffset(final String[] lines, final int endLine) {
        int offset = 0;
        for (int i = 0; i < endLine && i < lines.length; i++) {
            offset += lines[i].length() + 1;
        }
        return offset;
    }

    private boolean isDecoratorLine(final String trimmed) {
        return trimmed.startsWith("@");
    }

    private boolean isFunctionDefLine(final String trimmed) {
        return trimmed.startsWith("def ") || trimmed.startsWith("async def ");
    }

    private int findSignatureEnd(final String[] lines, final int startLine, final int lineCount) {
        int sigEndLine = startLine;
        int parenDepth = this.countParens(lines[startLine]);
        boolean foundColon = this.endsWithColonAndNoParens(lines[startLine], parenDepth);
        while (!foundColon && sigEndLine + 1 < lineCount) {
            final String nextLine = lines[sigEndLine + 1];
            parenDepth += this.countParens(nextLine);
            if (this.endsWithColonAndNoParens(nextLine, parenDepth)) {
                foundColon = true;
            }
            sigEndLine++;
        }
        return sigEndLine;
    }

    private int countParens(final String line) {
        int depth = 0;
        for (int k = 0; k < line.length(); k++) {
            final char c = line.charAt(k);
            if ('(' == c) depth++;
            if (')' == c) depth--;
        }
        return depth;
    }

    private boolean endsWithColonAndNoParens(final String line, final int parenDepth) {
        return line.trim().endsWith(":") && 0 == parenDepth;
    }

    @Override
    public String insertComment(final String fileContent, final MethodInfo method, final String commentText) {
        final int sigLineEnd = this.findSignatureLineEnd(fileContent, method.startOffset);
        final int insertPos = sigLineEnd + 1;
        final String indent = this.getIndent(fileContent, insertPos);
        if (this.hasExistingDocstring(fileContent, insertPos)) {
            return fileContent;
        }
        final String indentedComment = this.buildIndentedDocstring(indent, commentText);
        final String before = fileContent.substring(0, insertPos);
        final String after = fileContent.substring(insertPos);
        return before + indentedComment + after;
    }

    private int findSignatureLineEnd(final String fileContent, final int startOffset) {
        final int sigLineEnd = fileContent.indexOf('\n', startOffset);
        return -1 == sigLineEnd ? startOffset : sigLineEnd;
    }

    private boolean hasExistingDocstring(final String fileContent, final int insertPos) {
        int checkPos = insertPos;
        while (checkPos < fileContent.length()) {
            int lineEnd = fileContent.indexOf('\n', checkPos);
            if (-1 == lineEnd) lineEnd = fileContent.length();
            final String line = fileContent.substring(checkPos, lineEnd).trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                if ((line.startsWith("\"\"\"") && line.endsWith("\"\"\"")) || (line.startsWith("'''") && line.endsWith("'''"))) {
                    return true;
                } else if (line.startsWith("\"\"\"") || line.startsWith("'''")) {
                    return true;
                }
                break;
            }
            checkPos = lineEnd + 1;
        }
        return false;
    }

    private String buildIndentedDocstring(final String indent, final String commentText) {
        final StringBuilder indentedComment = new StringBuilder();
        indentedComment.append(indent).append("\"\"\"\n");
        for (final String line : commentText.split("\n")) {
            indentedComment.append(indent).append(line).append("\n");
        }
        indentedComment.append(indent).append("\"\"\"\n");
        return indentedComment.toString();
    }

    private String getIndent(final String content, final int pos) {
        final int lineStart = content.lastIndexOf('\n', pos - 1) + 1;
        int i = lineStart;
        while (i < content.length() && (' ' == content.charAt(i) || '\t' == content.charAt(i))) {
            i++;
        }
        return content.substring(lineStart, i);
    }
} 