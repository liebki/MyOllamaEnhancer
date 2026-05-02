package de.liebki.myollamaenhancer.php;

public class PHPContext {

    private int braceDepth;
    private int offset;
    private boolean inClass;
    private boolean inTrait;
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

    public boolean isInTrait() {
        return this.inTrait;
    }

    public void setInTrait(final boolean inTrait) {
        this.inTrait = inTrait;
    }

    public boolean isInInterface() {
        return this.inInterface;
    }

    public void setInInterface(final boolean inInterface) {
        this.inInterface = inInterface;
    }

    public boolean isInClassOrTraitOrInterface() {
        return this.inClass || this.inTrait || this.inInterface;
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
