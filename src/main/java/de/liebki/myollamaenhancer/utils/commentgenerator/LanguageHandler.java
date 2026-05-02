package de.liebki.myollamaenhancer.commentgenerator;

import de.liebki.myollamaenhancer.utils.CodeRegion;
import de.liebki.myollamaenhancer.utils.MethodInfo;

import java.util.List;

public interface LanguageHandler {
    boolean isApplicable(String fileExtension);
    List<MethodInfo> findMethods(String fileContent);
    default String insertComment(final String fileContent, final MethodInfo method, final String commentText) {
        int lineStart = method.startOffset;
        while (0 < lineStart && '\n' != fileContent.charAt(lineStart - 1)) {
            lineStart--;
        }

        final String before = fileContent.substring(0, lineStart);
        final String after = fileContent.substring(lineStart);

        return before + commentText + "\n" + after;
    }

    default List<CodeRegion> findCodeRegions(final String fileContent, final String fileExtension) {
        return List.of(new CodeRegion(0, fileContent.length(), fileExtension));
    }
} 