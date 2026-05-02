package de.liebki.myollamaenhancer.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts common LaTeX snippets to Swing-safe HTML using Unicode, <sup>, and <sub>.
 * This is NOT a full LaTeX renderer, but improves readability in JEditorPane.
 */
public enum LatexUnicodeUtility {
    ;

    // Precompiled patterns
    private static final Pattern SQRT_N = Pattern.compile("\\\\sqrt\\[(\\d+)\\]\\{([^}]*)\\}");
    private static final Pattern SQRT = Pattern.compile("\\\\sqrt\\{([^}]*)\\}");
    private static final Pattern FRAC = Pattern.compile("\\\\frac\\{([^}]*)\\}\\{([^}]*)\\}");

    // Superscripts/subscripts: brace and simple one-char variants
    private static final Pattern SUP_BRACE = Pattern.compile("\\^\\{([^}]*)\\}");
    private static final Pattern SUP_SIMPLE = Pattern.compile("\\^([A-Za-z0-9+\\-])");
    private static final Pattern SUB_BRACE = Pattern.compile("_\\{([^}]*)\\}");
    private static final Pattern SUB_SIMPLE = Pattern.compile("_([A-Za-z0-9])");

    // Delimiters
    private static final Pattern DOLLAR_BLOCK = Pattern.compile("(?s)\\$\\$([\\s\\S]*?)\\$\\$");
    private static final Pattern BRACKET_BLOCK = Pattern.compile("\\\\\\[([\\s\\S]*?)\\\\\\]");
    private static final Pattern PAREN_INLINE = Pattern.compile("\\\\\\(([^)]*?)\\\\\\)");
    private static final Pattern DOLLAR_INLINE = Pattern.compile("(?<!&)\\$([^$\n]+)\\$");

    private static final Pattern BOXED = Pattern.compile("\\\\boxed\\{([^}]*)\\}");

    private static final Map<String, String> MACROS;
    static {
        MACROS = new LinkedHashMap<>();
        // Greek letters (lowercase)
        MACROS.put("\\\\alpha", "α");
        MACROS.put("\\\\beta", "β");
        MACROS.put("\\\\gamma", "γ");
        MACROS.put("\\\\delta", "δ");
        MACROS.put("\\\\epsilon", "ε");
        MACROS.put("\\\\zeta", "ζ");
        MACROS.put("\\\\eta", "η");
        MACROS.put("\\\\theta", "θ");
        MACROS.put("\\\\iota", "ι");
        MACROS.put("\\\\kappa", "κ");
        MACROS.put("\\\\mu", "μ");
        MACROS.put("\\\\nu", "ν");
        MACROS.put("\\\\xi", "ξ");
        MACROS.put("\\\\lambda", "λ");
        MACROS.put("\\\\pi", "π");
        MACROS.put("\\\\rho", "ρ");
        MACROS.put("\\\\sigma", "σ");
        MACROS.put("\\\\tau", "τ");
        MACROS.put("\\\\upsilon", "υ");
        MACROS.put("\\\\phi", "φ");
        MACROS.put("\\\\psi", "ψ");
        MACROS.put("\\\\omega", "ω");
        MACROS.put("\\\\varepsilon", "ϵ");
        MACROS.put("\\\\varphi", "ϕ");
        MACROS.put("\\\\varsigma", "ς");
        MACROS.put("\\\\vartheta", "ϑ");

        // Greek letters (uppercase common)
        MACROS.put("\\\\Gamma", "Γ");
        MACROS.put("\\\\Delta", "Δ");
        MACROS.put("\\\\Theta", "Θ");
        MACROS.put("\\\\Lambda", "Λ");
        MACROS.put("\\\\Xi", "Ξ");
        MACROS.put("\\\\Pi", "Π");
        MACROS.put("\\\\Sigma", "Σ");
        MACROS.put("\\\\Upsilon", "Υ");
        MACROS.put("\\\\Phi", "Φ");
        MACROS.put("\\\\Psi", "Ψ");
        MACROS.put("\\\\Omega", "Ω");
        // Relations and operators
        MACROS.put("\\\\pm", "±");
        MACROS.put("\\\\mp", "∓");
        MACROS.put("\\\\times", "×");
        MACROS.put("\\\\div", "÷");
        MACROS.put("\\\\cdot", "⋅");
        MACROS.put("\\\\ast", "∗");
        MACROS.put("\\\\circ", "∘");
        MACROS.put("\\\\leq", "≤");
        MACROS.put("\\\\geq", "≥");
        MACROS.put("\\\\neq", "≠");
        MACROS.put("\\\\equiv", "≡");
        MACROS.put("\\\\approx", "≈");
        MACROS.put("\\\\propto", "∝");
        MACROS.put("\\\\pm", "±");
        MACROS.put("\\\\sum", "∑");
        MACROS.put("\\\\int", "∫");
        MACROS.put("\\\\prod", "∏");
        MACROS.put("\\\\angle", "∠");
        MACROS.put("\\\\perp", "⊥");
        MACROS.put("\\\\dagger", "†");
        MACROS.put("\\\\ddagger", "‡");
        MACROS.put("\\\\prime", "′");
        MACROS.put("\\\\infty", "∞");
        MACROS.put("\\\\ldots", "…");
        MACROS.put("\\\\forall", "∀");
        MACROS.put("\\\\exists", "∃");
        MACROS.put("\\\\therefore", "∴");
        MACROS.put("\\\\because", "∵");
        MACROS.put("\\\\nabla", "∇");
        MACROS.put("\\\\partial", "∂");
        MACROS.put("\\\\in", "∈");
        MACROS.put("\\\\notin", "∉");
        MACROS.put("\\\\subset", "⊂");
        MACROS.put("\\\\subseteq", "⊆");
        MACROS.put("\\\\supset", "⊃");
        MACROS.put("\\\\supseteq", "⊇");
        MACROS.put("\\\\cap", "∩");
        MACROS.put("\\\\cup", "∪");
        MACROS.put("\\\\wedge", "∧");
        MACROS.put("\\\\vee", "∨");
        MACROS.put("\\\\neg", "¬");

        // Arrows
        MACROS.put("\\\\rightarrow", "→");
        MACROS.put("\\\\leftarrow", "←");
        MACROS.put("\\\\Rightarrow", "⇒");
        MACROS.put("\\\\Leftarrow", "⇐");
        MACROS.put("\\\\Leftrightarrow", "⇔");
        MACROS.put("\\\\to", "→");
        MACROS.put("\\\\gets", "←");
        MACROS.put("\\\\uparrow", "↑");
        MACROS.put("\\\\downarrow", "↓");
        MACROS.put("\\\\leftrightarrow", "↔");
        MACROS.put("\\\\mapsto", "↦");

        // Misc letters/symbols
        MACROS.put("\\\\aleph", "ℵ");
        MACROS.put("\\\\imath", "ı");
        MACROS.put("\\\\jmath", "ȷ");
        MACROS.put("\\\\ell", "ℓ");
    }

    public static String convert(final String input) {
        if (null == input || input.isEmpty()) return input;
        String s = input;

        s = LatexUnicodeUtility.removeMathDelimiters(s);
        s = LatexUnicodeUtility.normalizeFractionCommands(s);
        s = LatexUnicodeUtility.gracefullyFixMalformedFrac(s);
        s = LatexUnicodeUtility.unboxBoxed(s);
        s = LatexUnicodeUtility.convertRoots(s);
        s = LatexUnicodeUtility.convertFractionsIteratively(s);
        s = LatexUnicodeUtility.applySupSub(s);
        s = LatexUnicodeUtility.applyMacroReplacements(s);
        s = LatexUnicodeUtility.normalizeMinusSigns(s);

        return s;
    }

    // Step 1: Remove math delimiters while preserving inner content
    private static String removeMathDelimiters(String s) {
        s = replaceAll(DOLLAR_BLOCK, s, m -> m.group(1));
        s = replaceAll(BRACKET_BLOCK, s, m -> m.group(1));
        s = replaceAll(PAREN_INLINE, s, m -> m.group(1));
        s = replaceAll(DOLLAR_INLINE, s, m -> m.group(1));
        return s;
    }

    // Step 2: Normalize \dfrac/\tfrac to \frac
    private static String normalizeFractionCommands(String s) {
        s = s.replaceAll("\\\\dfrac", "\\\\frac");
        s = s.replaceAll("\\\\tfrac", "\\\\frac");
        return s;
    }

    // Step 3: Gracefully handle a common malformed input like \dfrac{1{2}} -> 1⁄2
    private static String gracefullyFixMalformedFrac(final String s) {
        return s.replaceAll("\\\\frac\\{([0-9]+)\\{([0-9]+)\\}\\}", "$1⁄$2");
    }

    // Step 4: Unbox \boxed{...}
    private static String unboxBoxed(final String s) {
        return replaceAll(BOXED, s, m -> m.group(1));
    }

    // Step 5: Convert roots (nth-root first, then sqrt)
    private static String convertRoots(String s) {
        s = replaceAll(SQRT_N, s, m -> {
            String n = m.group(1);
            String body = m.group(2);
            switch (n) {
                case "3": return "∛(" + body + ")";
                case "4": return "∜(" + body + ")";
                default: return "<sup>" + escapeHtml(n) + "</sup>√(" + body + ")";
            }
        });
        s = replaceAll(SQRT, s, m -> "√(" + m.group(1) + ")");
        return s;
    }

    // Step 6: Convert fractions iteratively to handle nesting and complexity
    private static String convertFractionsIteratively(String s) {
        String prev;
        do {
            prev = s;
            s = LatexUnicodeUtility.replaceFractionsOnce(s);
        } while (!s.equals(prev));
        return s;
    }

    // Performs a single pass replacement of \frac occurrences in the string.
    private static String replaceFractionsOnce(final String s) {
        return replaceAll(FRAC, s, m -> {
            final String num = m.group(1).trim();
            final String den = m.group(2).trim();
            return LatexUnicodeUtility.formatFraction(num, den);
        });
    }

    // Formats a fraction, using vulgar fractions for simple numeric pairs, otherwise complexity-aware formatting.
    private static String formatFraction(final String num, final String den) {
        if (num.matches("\\d+") && den.matches("\\d+")) {
            return LatexUnicodeUtility.formatNumericFraction(num, den);
        }
        return LatexUnicodeUtility.formatComplexityAware(num, den);
    }

    // Reduces numeric fractions and uses Unicode vulgar glyphs when available.
    private static String formatNumericFraction(final String num, final String den) {
        String[] reduced = reduceNumeric(num, den);
        String vulgar = toVulgarFraction(reduced[0], reduced[1]);
        return null != vulgar ? vulgar : (reduced[0] + "⁄" + reduced[1]);
    }

    // Wraps numerator/denominator with parentheses when complex and joins with a fraction slash.
    private static String formatComplexityAware(String num, String den) {
        boolean numComplex = isComplex(num);
        boolean denComplex = isComplex(den);
        if (numComplex) num = "(" + num + ")";
        if (denComplex) den = "(" + den + ")";
        return num + "⁄" + den;
    }

    // Step 7: Apply superscripts and subscripts
    private static String applySupSub(String s) {
        s = replaceAll(SUP_BRACE, s, m -> "<sup>" + escapeHtml(m.group(1)) + "</sup>");
        s = replaceAll(SUP_SIMPLE, s, m -> "<sup>" + escapeHtml(m.group(1)) + "</sup>");
        s = replaceAll(SUB_BRACE, s, m -> "<sub>" + escapeHtml(m.group(1)) + "</sub>");
        s = replaceAll(SUB_SIMPLE, s, m -> "<sub>" + escapeHtml(m.group(1)) + "</sub>");
        return s;
    }

    // Step 8: Apply macro replacements
    private static String applyMacroReplacements(String s) {
        for (Map.Entry<String, String> e : MACROS.entrySet()) {
            s = s.replaceAll(e.getKey(), e.getValue());
        }
        return s;
    }

    // Step 9: Normalize minus signs in arithmetic contexts
    private static String normalizeMinusSigns(String s) {
        s = s.replaceAll("(?<=\\d)- (?=\\d)", " − ");
        s = s.replaceAll("(?<=\\d)-(?=\\d)", "−");
        return s;
    }

    private static boolean isComplex(String expr) {
        return expr.matches(".*[+\\-*/^ ].*");
    }

    // Reduce numeric fraction strings using GCD
    private static String[] reduceNumeric(String num, String den) {
        try {
            long n = Long.parseLong(num);
            long d = Long.parseLong(den);
            if (0 == d) return new String[] { num, den }; // avoid div by zero
            long g = gcd(Math.abs(n), Math.abs(d));
            n /= g;
            d /= g;
            // Keep minus sign on numerator only
            if (0 > d) { n = -n; d = -d; }
            return new String[] { Long.toString(n), Long.toString(d) };
        } catch (NumberFormatException ex) {
            return new String[] { num, den };
        }
    }

    private static long gcd(long a, long b) {
        while (0 != b) {
            long t = b;
            b = a % b;
            a = t;
        }
        return 0 == a ? 1 : a;
    }

    // Map simple numeric pairs to Unicode vulgar fractions
    private static String toVulgarFraction(String num, String den) {
        if (!num.matches("\\d+") || !den.matches("\\d+")) return null;
        switch (num + "/" + den) {
            case "1/2": return "½";
            case "1/3": return "⅓";
            case "2/3": return "⅔";
            case "1/4": return "¼";
            case "3/4": return "¾";
            case "1/5": return "⅕";
            case "2/5": return "⅖";
            case "3/5": return "⅗";
            case "4/5": return "⅘";
            case "1/6": return "⅙";
            case "5/6": return "⅚";
            case "1/8": return "⅛";
            case "3/8": return "⅜";
            case "5/8": return "⅝";
            case "7/8": return "⅞";
            default: return null;
        }
    }

    private static String escapeHtml(String text) {
        if (null == text) return null;
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    // Functional interface to avoid Java 8 dependency in call sites
    private interface Replacer { String replace(Matcher m); }

    private static String replaceAll(Pattern p, String input, Replacer r) {
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String rep = r.replace(m);
            // Ensure backslashes in replacement are literal
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
