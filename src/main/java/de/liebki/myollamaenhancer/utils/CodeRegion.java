package de.liebki.myollamaenhancer.utils;

/**
 * @param language e.g., "C#", "HTML"
 */
public record CodeRegion(int startOffset, int endOffset, String language) {

}