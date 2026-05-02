package de.liebki.myollamaenhancer.commentgenerator;
import de.liebki.myollamaenhancer.utils.FileExtensions;

public class VueLanguageHandler extends ScriptBasedLanguageHandler {
    
    @Override
    public boolean isApplicable(final String fileExtension) {
        return FileExtensions.isVueExt(fileExtension);
    }
    
    @Override
    protected String getFileExtension() {
        return "vue";
    }
    
    @Override
    protected String getFrameworkName() {
        return "Vue";
    }
    
} 