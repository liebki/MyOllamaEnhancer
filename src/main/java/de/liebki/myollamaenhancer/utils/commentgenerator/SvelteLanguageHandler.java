package de.liebki.myollamaenhancer.commentgenerator;
import de.liebki.myollamaenhancer.utils.FileExtensions;

public class SvelteLanguageHandler extends ScriptBasedLanguageHandler {
    
    @Override
    public boolean isApplicable(final String fileExtension) {
        return FileExtensions.isSvelteExt(fileExtension);
    }
    
    @Override
    protected String getFileExtension() {
        return "svelte";
    }
    
    @Override
    protected String getFrameworkName() {
        return "Svelte";
    }
} 