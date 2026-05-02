package de.liebki.myollamaenhancer.utils;

public class MethodInfo {
    public int startOffset;
    public int endOffset;
    public int bodyStartIndex; // Where the method body starts (needed for Python)
    public String methodSource; // The method's text

    public MethodInfo(final int startIndex, final int endIndex, final int bodyStartIndex, final String methodSource) {
        startOffset = startIndex;
        endOffset = endIndex;
        this.bodyStartIndex = bodyStartIndex;
        this.methodSource = methodSource;
    }
} 