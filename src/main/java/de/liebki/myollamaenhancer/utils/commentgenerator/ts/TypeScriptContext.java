package de.liebki.myollamaenhancer.ts;

/**
 * Context class to manage state during TypeScript code parsing.
 * Encapsulates the state needed for method detection in TypeScript files.
 */
public class TypeScriptContext {
    private int braceDepth;
    private int offset;
    private boolean inClass;
    private boolean inObject;
    private boolean inInterface;
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

    public boolean isInInterface() {
        return this.inInterface;
    }

    public void setInInterface(final boolean inInterface) {
        this.inInterface = inInterface;
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

    public void clearSigBuffer() {
        this.sigBuffer.setLength(0);
    }

    public boolean isSigBufferEmpty() {
        return 0 == sigBuffer.length();
    }

    public String getSigBufferContent() {
        return this.sigBuffer.toString();
    }
}
