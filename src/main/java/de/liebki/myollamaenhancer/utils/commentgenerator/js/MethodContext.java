package de.liebki.myollamaenhancer.js;

public class MethodContext {
    private int braceDepth;
    private int offset;
    private boolean inClass;
    private boolean inObject;
    private int sigStartOffset;
    private final StringBuilder sigBuffer = new StringBuilder();

    public int getBraceDepth() {
        return this.braceDepth;
    }

    public void setBraceDepth(final int braceDepth) {
        this.braceDepth = braceDepth;
    }

    public int getOffset() {
        return this.offset;
    }

    public void incrementOffset(final int amount) {
        offset += amount;
    }

    public boolean isInClass() {
        return this.inClass;
    }

    public void setInClass(final boolean inClass) {
        this.inClass = inClass;
    }

    public boolean isInObject() {
        return this.inObject;
    }

    public void setInObject(final boolean inObject) {
        this.inObject = inObject;
    }

    public int getSigStartOffset() {
        return this.sigStartOffset;
    }

    public void setSigStartOffset(final int sigStartOffset) {
        this.sigStartOffset = sigStartOffset;
    }

    public StringBuilder appendToSigBuffer(final String str) {
        return this.sigBuffer.append(str);
    }

    public boolean isSigBufferEmpty() {
        return 0 == sigBuffer.length();
    }

    public void clearSigBuffer() {
        this.sigBuffer.setLength(0);
    }
}
