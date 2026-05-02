package de.liebki.myollamaenhancer.utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central registry for all file extensions used across the plugin.
 *
 * - CODE_EXTENSIONS: broad set used for filename validation and chat display
 * - HANDLER_EXTENSIONS: extensions relevant for language handlers
 * - LANGUAGE_MAP: mapping of extensions to human-readable language names
 */
public enum FileExtensions {
    ;

    // Broad set of extensions we consider as valid code- or text-like files in UI/regex
    public static final Set<String> CODE_EXTENSIONS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
        "java",
        "py","pyw","pyi","py3","pyz",
        "js","jsx","mjs","cjs","ts","tsx","mts","cts",
        "c","cpp","cc","cxx","h","hpp","hh","hxx","ipp","tpp","inl","txx","ixx",
        "cs","csx",
        "rb","go","rs","kt","kts","php","php3","php4","php5","phtml","phps","inc",
        "groovy","gvy","gy","gsh","scala",
        "m","mm",
        "graphql","gql",
        "less","styl","stylus",
        "tf","tfvars","hcl",
        "clj","cljs","cljc","edn",
        "ex","exs","erl","hrl",
        "bzl",
        "cmake",
        "html","htm","css","scss","sass","json","yml","yaml","xml","toml","ini","cfg","conf","env",
        "md","txt","sh","bat","zsh","pl","swift","dart","r","lua","sql","vue","svelte",
        "gradle","make","mk","dockerfile","properties","proto","lock","config","pdf","csv"
    )));

    // Extensions considered by LanguageHandlers
    public static final List<String> HANDLER_EXTENSIONS = Collections.unmodifiableList(Arrays.asList(
        "java", "py", "cs", "js", "ts", "svelte", "vue",
        "go", "rs", "php",
        // C / C++ family (headers included)
        "c", "cpp", "cc", "cxx", "h", "hpp", "hh", "hxx"
    ));

    // Map from extension to language name (used by LanguageUtils)
    private static final Map<String, String> LANGUAGE_MAP;
    static {
        final Map<String, String> m = new HashMap<>();
        m.put("java", "Java");
        m.put("py", "Python");
        m.put("pyw", "Python");
        m.put("pyi", "Python");
        m.put("py3", "Python");
        m.put("pyz", "Python");
        m.put("cs", "C#");
        m.put("csx", "C#");
        m.put("rs", "Rust");
        m.put("go", "Go");
        m.put("js", "JavaScript");
        m.put("jsx", "JavaScript");
        m.put("mjs", "JavaScript");
        m.put("cjs", "JavaScript");
        m.put("ts", "TypeScript");
        m.put("tsx", "TypeScript");
        m.put("mts", "TypeScript");
        m.put("cts", "TypeScript");
        m.put("svelte", "Svelte");
        m.put("vue", "Vue");
        m.put("php", "PHP");
        m.put("php3", "PHP");
        m.put("php4", "PHP");
        m.put("php5", "PHP");
        m.put("phtml", "PHP");
        m.put("phps", "PHP");
        m.put("inc", "PHP");
        m.put("c", "C");
        m.put("cpp", "C++");
        m.put("cc", "C++");
        m.put("cxx", "C++");
        m.put("h", "C++");
        m.put("hpp", "C++");
        m.put("hh", "C++");
        m.put("hxx", "C++");
        m.put("ipp", "C++");
        m.put("tpp", "C++");
        m.put("inl", "C++");
        m.put("txx", "C++");
        m.put("ixx", "C++");
        LANGUAGE_MAP = Collections.unmodifiableMap(m);
    }

    /**
     * Returns a compiled filename regex string like:
     * ^[A-Za-z0-9][A-Za-z0-9._-]*\.(?i)(ext1|ext2|...)$
     */
    public static String fileNameRegex() {
        final String alternation = CODE_EXTENSIONS.stream()
                .map(ext -> ext.replace("|", "\\|"))
                .collect(Collectors.joining("|"));
        return "^[A-Za-z0-9][A-Za-z0-9._-]*\\.(?i)(" + alternation + ")$";
    }

    /**
     * Returns the language name for a given file extension, or null if unknown.
     */
    public static String languageForExtension(final String extensionLowercase) {
        if (null == extensionLowercase) return null;
        return LANGUAGE_MAP.get(extensionLowercase);
    }

    // ----------------------------
    // Centralized extension checks
    // ----------------------------
    private static boolean eq(final String ext, final String... candidates) {
        if (null == ext) return false;
        for (final String c : candidates) {
            if (ext.equalsIgnoreCase(c)) return true;
        }
        return false;
    }

    public static boolean isJavaExt(final String ext) {
        return eq(ext, "java");
    }

    public static boolean isPythonExt(final String ext) {
        return eq(ext, "py");
    }

    public static boolean isJavaScriptExt(final String ext) {
        return eq(ext, "js", "jsx");
    }

    public static boolean isTypeScriptExt(final String ext) {
        return eq(ext, "ts", "tsx");
    }

    public static boolean isCSharpExt(final String ext) {
        return eq(ext, "cs", "razor");
    }

    public static boolean isGoExt(final String ext) {
        return eq(ext, "go");
    }

    public static boolean isRustExt(final String ext) {
        return eq(ext, "rs");
    }

    public static boolean isPhpExt(final String ext) {
        // Keep aligned with PHPHandler support
        return eq(ext, "php", "php3", "php4", "php5", "php7", "php8", "phps");
    }

    public static boolean isCxxFamilyExt(final String ext) {
        return eq(ext, "c", "cpp", "cc", "cxx", "h", "hpp", "hh", "hxx");
    }

    public static boolean isVueExt(final String ext) {
        return eq(ext, "vue");
    }

    public static boolean isSvelteExt(final String ext) {
        return eq(ext, "svelte");
    }
}
