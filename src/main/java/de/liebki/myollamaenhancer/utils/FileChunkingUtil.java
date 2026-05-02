package de.liebki.myollamaenhancer.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling large file content by chunking it into smaller pieces.
 */
public enum FileChunkingUtil {
    ;

    // Maximum size of a chunk in characters
    private static final int MAX_CHUNK_SIZE = 8000;
    
    // Overlap between chunks to maintain context
    private static final int CHUNK_OVERLAP = 1000;
    
    /**
     * Splits a large file content into smaller chunks.
     * 
     * @param content The file content to split
     * @return A list of content chunks
     */
    public static List<String> splitIntoChunks(String content) {
        if (null == content || FileChunkingUtil.MAX_CHUNK_SIZE >= content.length()) {
            List<String> singleChunk = new ArrayList<>();
            singleChunk.add(content);
            return singleChunk;
        }
        
        List<String> chunks = new ArrayList<>();
        int start = 0;
        
        while (start < content.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, content.length());
            String chunk = content.substring(start, end);
            chunks.add(chunk);
            
            // Move start position for next chunk, with overlap
            start = end - CHUNK_OVERLAP;
            
            // If we're at the end of the content, break
            if (end == content.length()) {
                break;
            }
        }
        
        return chunks;
    }
    
    /**
     * Determines if a file is considered large and needs chunking.
     * 
     * @param content The file content
     * @return true if the file is large enough to require chunking
     */
    public static boolean isLargeFile(String content) {
        return null != content && FileChunkingUtil.MAX_CHUNK_SIZE < content.length();
    }
}
