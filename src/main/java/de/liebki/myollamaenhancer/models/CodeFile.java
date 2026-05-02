package de.liebki.myollamaenhancer.models;

public class CodeFile {
    public long id;
    public String fileName;
    public String folder;
    public String code;
    public boolean enabled = true;

    @Override
    public String toString() {
        return "CodeFile{" +
                "id=" + this.id +
                ", fileName='" + this.fileName + '\'' +
                ", folder='" + this.folder + '\'' +
                ", code='" + this.code + '\'' +
                ", enabled=" + this.enabled +
                '}';
    }
}