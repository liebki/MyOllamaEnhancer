package de.liebki.myollamaenhancer.commentgenerator;

import de.liebki.myollamaenhancer.utils.CodeRegion;
import de.liebki.myollamaenhancer.utils.MethodInfo;
import de.liebki.myollamaenhancer.js.JavaScriptLanguageHandler;
import de.liebki.myollamaenhancer.ts.TypeScriptLanguageHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for language handlers that work with files containing script tags
 * (like Svelte, Vue, etc.). This class provides common functionality for
 * extracting script regions and processing them with appropriate language handlers.
 */
public abstract class ScriptBasedLanguageHandler implements LanguageHandler {
    
    private final JavaScriptLanguageHandler jsHandler;
    private final TypeScriptLanguageHandler tsHandler;
    
    protected ScriptBasedLanguageHandler() {
        jsHandler = new JavaScriptLanguageHandler();
        tsHandler = new TypeScriptLanguageHandler();
    }
    
    /**
     * Get the file extension this handler is applicable for
     */
    protected abstract String getFileExtension();
    
    /**
     * Get the framework name for logging purposes
     */
    protected abstract String getFrameworkName();

    @Override
    public List<MethodInfo> findMethods(final String fileContent) {
        final List<MethodInfo> methods = new ArrayList<>();
        final List<CodeRegion> scriptRegions = this.findCodeRegions(fileContent, this.getFileExtension());
        
        System.out.println(this.getFrameworkName() + " findMethods - Found " + scriptRegions.size() + " script regions");
        
        for (final CodeRegion region : scriptRegions) {
            final String scriptContent = fileContent.substring(region.startOffset(), region.endOffset());
            final String language = region.language();
            
            System.out.println("Processing script region - Language: " + language + ", Content: " + scriptContent.substring(0, Math.min(200, scriptContent.length())));
            
            final List<MethodInfo> regionMethods;
            if ("ts".equals(language)) {
                regionMethods = this.tsHandler.findMethods(scriptContent);
            } else {
                regionMethods = this.jsHandler.findMethods(scriptContent);
            }
            
            System.out.println("Found " + regionMethods.size() + " methods in " + language + " script");
            
            // Adjust offsets to account for the script tag position in the original file
            for (final MethodInfo method : regionMethods) {
                final MethodInfo adjustedMethod = new MethodInfo(
                    region.startOffset() + method.startOffset,
                    region.startOffset() + method.endOffset,
                    region.startOffset() + method.bodyStartIndex,
                    method.methodSource
                );
                methods.add(adjustedMethod);
            }
        }
        
        System.out.println("Total methods found in " + this.getFrameworkName() + " file: " + methods.size());
        return methods;
    }

    @Override
    public String insertComment(final String fileContent, final MethodInfo method, final String commentText) {
        // Find which script region this method belongs to
        final List<CodeRegion> scriptRegions = this.findCodeRegions(fileContent, this.getFileExtension());
        
        for (final CodeRegion region : scriptRegions) {
            if (method.startOffset >= region.startOffset() && method.startOffset < region.endOffset()) {
                // Extract the script content
                final String scriptContent = fileContent.substring(region.startOffset(), region.endOffset());
                
                // Create adjusted method info for the script content
                final MethodInfo adjustedMethod = new MethodInfo(
                    method.startOffset - region.startOffset(),
                    method.endOffset - region.startOffset(),
                    method.bodyStartIndex - region.startOffset(),
                    method.methodSource
                );
                
                // Use shared JSDoc comment insertion logic directly
                final String updatedScriptContent = LanguageHandlerUtils.insertJSDocComment(scriptContent, adjustedMethod, commentText);
                
                // Reconstruct the file with updated script content
                final String before = fileContent.substring(0, region.startOffset());
                final String after = fileContent.substring(region.endOffset());
                return before + updatedScriptContent + after;
            }
        }
        
        // Fallback to default implementation if no script region found
        return LanguageHandler.super.insertComment(fileContent, method, commentText);
    }

    @Override
    public List<CodeRegion> findCodeRegions(final String fileContent, final String fileExtension) {
        final List<CodeRegion> regions = new ArrayList<>();
        
        // Use shared script tag pattern
        final Pattern scriptPattern = LanguageHandlerUtils.SCRIPT_TAG_PATTERN;
        
        final Matcher matcher = scriptPattern.matcher(fileContent);
        System.out.println(this.getFrameworkName() + " file content length: " + fileContent.length());
        System.out.println("Looking for script tags in " + this.getFrameworkName() + " file...");
        
        while (matcher.find()) {
            String language = matcher.group(1);
            final String scriptContent = matcher.group(2);

            if (null == language) {
                language = "js";
            }

            System.out.println("Found script tag - Language: " + language + ", Content length: " + scriptContent.length());
            System.out.println("Script content preview: " + scriptContent.substring(0, Math.min(100, scriptContent.length())));
            
            final int start = matcher.start(2);
            final int end = matcher.end(2);
            
            regions.add(new CodeRegion(start, end, language));
        }
        
        System.out.println("Total script regions found: " + regions.size());
        return regions;
    }
} 