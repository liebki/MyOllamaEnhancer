package de.liebki.myollamaenhancer.utils;

import com.google.gson.annotations.SerializedName;
import de.liebki.myollamaenhancer.models.CodeFile;
import de.liebki.myollamaenhancer.models.CodeSection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the result of a retrieval operation, including the content and source information.
 */
public record RetrievalResult(String content, List<SourceInfo> sources) {

    public RetrievalResult(final String content, final List<SourceInfo> sources) {
        this.content = null != content ? content : "";
        this.sources = null != sources ? new ArrayList<>(sources) : new ArrayList<>();
    }

    @Override
    public List<SourceInfo> sources() {
        return new ArrayList<>(this.sources);
    }

    public static class SourceInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        @SerializedName("fileName")
        private String fileName;

        @SerializedName("filePath")
        private String filePath;

        @SerializedName("type")
        private String type; // "file" or "section"

        // Default constructor for Gson
        public SourceInfo() {
            this("", "", "");
        }

        public SourceInfo(final String fileName, final String filePath, final String type) {
            this.fileName = null != fileName ? fileName : "";
            this.filePath = null != filePath ? filePath : "";
            this.type = null != type ? type : "";
        }

        public String getFileName() {
            return this.fileName;
        }

        public String getFilePath() {
            return this.filePath;
        }

        public String getType() {
            return this.type;
        }

        @Override
        public String toString() {
            return String.format("%s (%s)", this.fileName, this.type);
        }
    }

    /**
     * Creates a RetrievalResult from a CodeFile.
     */
    public static RetrievalResult fromCodeFile(final CodeFile codeFile) {
        if (null == codeFile) {
            return new RetrievalResult("", new ArrayList<>());
        }
        final List<SourceInfo> sources = new ArrayList<>();
        sources.add(new SourceInfo(
                codeFile.fileName,
                null != codeFile.folder ? codeFile.folder + "/" + codeFile.fileName : codeFile.fileName,
                "file"
        ));
        return new RetrievalResult(codeFile.code, sources);
    }

    /**
     * Creates a RetrievalResult from a CodeSection.
     */
    public static RetrievalResult fromCodeSection(final CodeSection codeSection) {
        if (null == codeSection) {
            return new RetrievalResult("", new ArrayList<>());
        }
        final List<SourceInfo> sources = new ArrayList<>();
        sources.add(new SourceInfo(
                codeSection.fileName,
                null != codeSection.folder ? codeSection.folder + "/" + codeSection.fileName : codeSection.fileName,
                "section"
        ));
        return new RetrievalResult(codeSection.code, sources);
    }

    /**
     * Combines multiple RetrievalResults into one.
     */
    public static RetrievalResult combine(final List<RetrievalResult> results) {
        final StringBuilder combinedContent = new StringBuilder();
        final List<SourceInfo> combinedSources = new ArrayList<>();

        for (final RetrievalResult result : results) {
            appendResultContent(result, combinedContent);
            addUniqueSources(result, combinedSources);
        }

        return new RetrievalResult(combinedContent.toString(), combinedSources);
    }

    /**
     * Appends the content of a RetrievalResult to the combined content StringBuilder.
     */
    private static void appendResultContent(final RetrievalResult result, final StringBuilder combinedContent) {
        if (null != result && null != result.content && !result.content.isEmpty()) {
            if (0 < combinedContent.length()) {
                combinedContent.append("\n\n");
            }
            combinedContent.append(result.content);
        }
    }

    /**
     * Adds unique sources from a RetrievalResult to the combined sources list.
     */
    private static void addUniqueSources(final RetrievalResult result, final List<SourceInfo> combinedSources) {
        if (null != result && null != result.sources()) {
            for (final SourceInfo source : result.sources()) {
                // Avoid adding duplicate sources
                if (isUniqueSource(source, combinedSources)) {
                    combinedSources.add(source);
                }
            }
        }
    }

    /**
     * Checks if a source is unique compared to existing sources.
     */
    private static boolean isUniqueSource(final SourceInfo source, final List<SourceInfo> existingSources) {
        return existingSources.stream().noneMatch(s ->
                s.getFileName().equals(source.getFileName()) &&
                        s.getType().equals(source.getType()));
    }
}
