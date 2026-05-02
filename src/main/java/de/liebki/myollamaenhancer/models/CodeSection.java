package de.liebki.myollamaenhancer.models;

public class CodeSection {
    public long id;
    public String fileName;
    public String folder;
    public String code;
    public boolean enabled = true;

    @Override
    public String toString() {
        return "CodeSection{" +
                "id=" + this.id +
                ", fileName='" + this.fileName + '\'' +
                ", folder='" + this.folder + '\'' +
                ", code='" + this.code + '\'' +
                ", enabled=" + this.enabled +
                '}';
    }
}