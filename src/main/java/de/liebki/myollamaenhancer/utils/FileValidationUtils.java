package de.liebki.myollamaenhancer.utils;

import com.intellij.openapi.vfs.VirtualFile;
import de.liebki.myollamaenhancer.commentgenerator.*;
import de.liebki.myollamaenhancer.js.JavaScriptLanguageHandler;
import de.liebki.myollamaenhancer.php.PHPHandler;
import de.liebki.myollamaenhancer.ts.TypeScriptLanguageHandler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for validating files that can be added to the knowledge base.
 * Centralizes the logic for determining which file types are supported.
 */
public enum FileValidationUtils {
    ;

    private static final List<LanguageHandler> SUPPORTED_HANDLERS = Arrays.asList(
            new JavaLanguageHandler(),
            new PythonLanguageHandler(),
            new CSharpLanguageHandler(),
            new JavaScriptLanguageHandler(),
            new TypeScriptLanguageHandler(),
            new SvelteLanguageHandler(),
            new VueLanguageHandler(),
            new GoLanguageHandler(),
            new RustLanguageHandler(),
            new PHPHandler(),
            new CxxLanguageHandler()
    );

    public static boolean isValidFile(final VirtualFile virtualFile) {
        if (null == virtualFile || virtualFile.isDirectory()) {
            return false;
        }
        
        final String fileExtension = virtualFile.getExtension();
        if (null == fileExtension) {
            return false;
        }
        
        return FileValidationUtils.SUPPORTED_HANDLERS.stream()
                .anyMatch(handler -> handler.isApplicable(fileExtension));
    }

    public static List<String> getSupportedExtensions() {
        final List<String> base = FileExtensions.HANDLER_EXTENSIONS;
        return FileValidationUtils.SUPPORTED_HANDLERS.stream()
                .flatMap(handler -> base.stream().filter(handler::isApplicable))
                .distinct()
                .collect(Collectors.toList());
    }

    public static String getSupportedFileTypesDescription() {
        final List<String> extensions = FileValidationUtils.getSupportedExtensions();
        return "Supported file types: " + String.join(", ", extensions);
    }
} 