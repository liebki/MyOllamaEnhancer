package de.liebki.myollamaenhancer.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to generate a regular expression from an example string and a capture goal,
 * using the configured Ollama model with structured output. It validates the regex
 * and retries a few times if invalid.
 */
public enum RegexGenerationService {
    ;

    /**
     * Generate a regex. Runs work on a background task and invokes the callback on EDT.
     *
     * @param project IntelliJ project
     * @param exampleString the example input string
     * @param captureGoal description or literal of what to capture
     * @param maxRetries number of retries if generated regex is invalid
     * @param onResult callback (regex, rawModelResponse)
     */
    public static void generateRegex(final Project project,
                                     final String exampleString,
                                     final String captureGoal,
                                     final String extraInstructions,
                                     final int maxRetries,
                                     final BiConsumer<String, String> onResult) {

        ProgressManager.getInstance().run(new Task.Modal(project, "Generating regex with Ollama...", true) {
            @Override
            public void run(final ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Contacting model and validating regex...");

                final Map<String, Object> schema = StructuredOutputSchemas.createRegexGenerationSchema();
                final String sysPrompt = RegexGenerationService.buildSystemPromptForRegex();

                final GenerationResult result = RegexGenerationService.performGenerationAttempts(
                        indicator, schema, sysPrompt, exampleString, captureGoal, extraInstructions, maxRetries);

                String finalRegex = null != result.bestRegex ? result.bestRegex : "";
                String finalRaw = (null == result.lastRaw ? "" : result.lastRaw) +
                        (null != result.lastValidationError ? "\n\n[Validation] " + result.lastValidationError : "");
                ApplicationManager.getApplication().invokeLater(() -> onResult.accept(finalRegex, finalRaw));
            }
        });
    }

    // Builds the system prompt for generally usable regex generation.
    private static String buildSystemPromptForRegex() {
        return StructuredPromptEnhancer.enhanceForRegexGeneration(
                "Return ONLY JSON with field 'regex'. No prose. No code fences. " +
                "Produce a generally usable regex (portable where reasonable). Prefer simple, broadly supported constructs: literals, character classes, \\d/\\w/\\s, grouping ( ... ) and non-capturing (?: ... ), alternation |, anchors ^ $, and numeric quantifiers {n} {n,m} + * ?. " +
                "Prefer wrapping the desired capture in a capturing group so it can be extracted exactly; use non-capturing groups for other grouping where helpful. Avoid engine-specific features unless necessary.");
    }

    // Encapsulates the attempt loop and state management.
    private static GenerationResult performGenerationAttempts(final ProgressIndicator indicator,
                                                              final Map<String, Object> schema,
                                                              final String sysPrompt,
                                                              final String exampleString,
                                                              final String captureGoal,
                                                              final String extraInstructions,
                                                              final int maxRetries) {
        String bestRegex = null;
        String lastRaw = null;
        String lastValidationError = null;
        Boolean lastGoalOk = null;
        String lastCandidate = null;

        String correctiveHint = "";
        final int totalAttempts = Math.max(8, maxRetries);
        for (int attempt = 0; attempt < totalAttempts; attempt++) {
            if (indicator.isCanceled()) return new GenerationResult(bestRegex, lastRaw, lastValidationError);

            RegexGenerationService.updateAttemptIndicator(indicator, attempt, totalAttempts);

            final String userPrompt = RegexGenerationService.buildUserPromptWithHint(exampleString, captureGoal, extraInstructions, correctiveHint);
            final AttemptOutcome outcome = RegexGenerationService.tryOneAttempt(schema, sysPrompt, userPrompt, exampleString, captureGoal, extraInstructions, attempt, totalAttempts);

            lastRaw = outcome.lastRaw;
            lastValidationError = outcome.validationError;
            lastGoalOk = outcome.goalOk;
            lastCandidate = outcome.candidate;
            if (outcome.goalOk) {
                bestRegex = outcome.candidate;
                break;
            }

            correctiveHint = buildCorrectiveHint(outcome.candidate, outcome.validationError, exampleString, captureGoal);
        }

        return new GenerationResult(bestRegex, lastRaw, lastValidationError);
    }

    // Updates indicator text and logs the current attempt
    private static void updateAttemptIndicator(final ProgressIndicator indicator, final int attempt, final int totalAttempts) {
        indicator.setText("Attempt " + (attempt + 1) + " of " + totalAttempts);
        System.out.println("[RegexGenerationService] Attempt " + (attempt + 1));
    }

    // Builds the user prompt including any corrective hint
    private static String buildUserPromptWithHint(final String exampleString, final String captureGoal, final String extraInstructions, final String correctiveHint) {
        final String base = buildUserPrompt(exampleString, captureGoal, extraInstructions);
        return base + (correctiveHint.isEmpty() ? "" : ("\n\n" + correctiveHint));
    }

    // Performs a single model attempt and returns its outcome
    private static AttemptOutcome tryOneAttempt(final Map<String, Object> schema,
                                                final String sysPrompt,
                                                final String userPrompt,
                                                final String exampleString,
                                                final String captureGoal,
                                                final String extraInstructions,
                                                final int attempt,
                                                final int totalAttempts) {
        String lastRaw = null;
        String candidate = null;
        String validationError = null;
        boolean goalOk = false;
        try {
            // IMPORTANT: Correct parameter order is (userPrompt, sysPrompt, schema)
            final OllamaAPIUtil.ThinkContent response = OllamaAPIUtil.generateStructuredOllamaResponseSync(userPrompt, sysPrompt, schema);
            lastRaw = response.visibleContent();
            candidate = StructuredResponseParser.parseStructuredResponse(lastRaw, "regex");
            System.out.println("[RegexGenerationService] Candidate regex: " + candidate);

            validationError = getRegexValidationError(candidate);
            boolean finalAttempt = attempt == (totalAttempts - 1);
            goalOk = null == validationError && matchesGoal(candidate, exampleString, captureGoal, finalAttempt);
            System.out.println("[RegexGenerationService] Validation: " + (null == validationError ? "OK" : ("FAIL - " + validationError)) +
                    ", GoalMatch=" + goalOk);
        } catch (final Exception e) {
            System.out.println("[RegexGenerationService] Error in attempt: " + e.getMessage());
        }

        return new AttemptOutcome(lastRaw, candidate, validationError, goalOk);
    }

    // Outcome for a single generation attempt
        private record AttemptOutcome(String lastRaw, String candidate, String validationError, boolean goalOk) {

    }

    // Simple holder for generation outcomes
        private record GenerationResult(String bestRegex, String lastRaw, String lastValidationError) {

    }

    private static String buildUserPrompt(final String example, final String capture, final String extra) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Example:\n").append(example).append("\n\n");
        sb.append("Capture goal (exact string to match as a full match on the example):\n").append(capture).append("\n\n");
        if (null != extra && !extra.isBlank()) {
            sb.append("Extra instructions (user intent/context):\n").append(extra).append("\n\n");
        }
        sb.append("Instructions:\n");
        sb.append("- Return JSON with key 'regex' only. No slashes, no flags.\n");
        sb.append("- Prefer simple, broadly supported constructs.\n");
        sb.append("- Escape regex metacharacters as needed.\n");
        sb.append("- Use non-capturing groups for grouping that should not be captured.\n");
        sb.append("- The regex will be considered a success ONLY if it produces a FULL match on the example exactly equal to the capture goal string.\n");
        return sb.toString();
    }

    private static boolean isValidRegex(final String regex) {
        return null == RegexGenerationService.getRegexValidationError(regex);
    }

    private static String getRegexValidationError(final String regex) {
        if (null == regex || regex.isBlank()) return "Empty regex";
        try {
            Pattern.compile(regex);
            return null;
        } catch (final Exception e) {
            return e.getMessage();
        }
    }

    private static String normalize(final String s) {
        if (null == s) return "";
        // Trim and collapse any whitespace sequences to a single space
        return s.trim().replaceAll("\\s+", " ");
    }

    private static boolean matchesGoal(final String regex, final String example, final String captureGoal, final boolean finalAttempt) {
        try {
            if (null == captureGoal || captureGoal.isBlank()) return false; // explicit goal required
            String goal = captureGoal;
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(example);
            boolean anyMatch = false;
            while (m.find()) {
                anyMatch = true;
                String full = m.group(0);
                if (goal.equals(full)) return true; // success only if a full match equals the goal exactly
            }
            if (anyMatch) RegexGenerationService.debugGoalMatch(m, goal);
            return false;
        } catch (final Exception e) {
            return false;
        }
    }

    // Safely read a group
    private static String safeGroup(final Matcher m, final int idx) {
        try { return m.group(idx); } catch (final Exception ignore) { return null; }
    }

    // Checks if the regex pattern text contains any non-word symbol from the goal (either as raw or escaped)
    private static boolean patternContainsNonWordFromGoal(final String regex, final String goal) {
        if (null == regex || null == goal) return false;
        final String nonWord = goal.replaceAll("[A-Za-z0-9_]", "");
        if (nonWord.isEmpty()) return true; // only word chars; no special requirement
        for (final char c : nonWord.toCharArray()) {
            final String ch = String.valueOf(c);
            if (regex.contains(ch)) return true;
            final String escaped = "\\\\" + Pattern.quote(ch);
            if (regex.matches(".*" + escaped + ".*")) return true;
        }
        return false;
    }

    // Compile the regex and return a matcher positioned at the first match, or null.
    private static Matcher getFirstMatch(final String regex, final String example) {
        final Pattern p = Pattern.compile(regex);
        final Matcher m = p.matcher(example);
        return m.find() ? m : null;
    }

    private static boolean hasAtLeastOneGroup(final Matcher m) {
        return 1 <= m.groupCount();
    }

    private static boolean hasAnyGroupEqualToGoal(final Matcher m, final String goal, final String normGoal, final boolean finalAttempt) {
        if (1 > m.groupCount()) return false;
        for (int i = 1; i <= m.groupCount(); i++) {
            final String gi = m.group(i);
            if (RegexGenerationService.groupMatchesGoal(gi, goal, normGoal)) return true;
            if (finalAttempt && RegexGenerationService.groupMatchesGoalIgnoreCase(gi, goal, normGoal)) return true;
        }
        return false;
    }

    private static boolean groupMatchesGoal(final String gi, final String goal, final String normGoal) {
        if (null == gi) return false;
        if (gi.equals(goal)) return true;
        final String ngi = normalize(gi);
        return ngi.equals(normGoal);
    }

    private static boolean groupMatchesGoalIgnoreCase(final String gi, final String goal, final String normGoal) {
        if (null == gi) return false;
        if (gi.equalsIgnoreCase(goal)) return true;
        final String ngi = normalize(gi);
        return ngi.equalsIgnoreCase(normGoal);
    }

    private static boolean fullMatchContainsGoal(final Matcher m, final String goal, final String normGoal) {
        final String full = m.group(0);
        return null != full && (full.contains(goal) || normalize(full).contains(normGoal));
    }

    private static void debugGoalMatch(final Matcher m, final String goal) {
        try {
            final StringBuilder dbg = new StringBuilder("[GoalMatch] groupCount=" + m.groupCount());
            for (int i = 1; i <= m.groupCount(); i++) {
                dbg.append(", g").append(i).append("=").append(m.group(i));
            }
            dbg.append(", full=").append(m.group(0)).append(", goal=").append(goal);
            System.out.println(dbg);
        } catch (final Exception ignore) {}
    }
    
    private static String buildCorrectiveHint(final String candidate,
                                              final String validationError,
                                              final String example,
                                              final String captureGoal) {
        String msg;
        msg = RegexGenerationService.portabilityMessage(candidate);
        if (null != msg) return msg;

        msg = RegexGenerationService.plainLiteralMessage(candidate, captureGoal);
        if (null != msg) return msg;

        msg = RegexGenerationService.missingLiteralSymbolsMessage(candidate, captureGoal, example);
        if (null != msg) return msg;

        msg = RegexGenerationService.invalidQuantifierMessage(candidate);
        if (null != msg) return msg;

        msg = RegexGenerationService.illegalRepetitionMessage(validationError);
        if (null != msg) return msg;

        msg = RegexGenerationService.lookaroundMessage(candidate);
        if (null != msg) return msg;

        msg = RegexGenerationService.groupingCountMessage(candidate, example);
        if (null != msg) return msg;

        msg = RegexGenerationService.literalGoalGroupingMessage(candidate, example, captureGoal);
        if (null != msg) return msg;

        return "Ensure there is EXACTLY ONE capturing group around the desired capture.";
    }

    // If goal includes literal non-word symbols (e.g., #, @, -) and candidate pattern appears to omit them,
    // nudge the model to include them inside the single capturing group.
    private static String missingLiteralSymbolsMessage(final String candidate, final String captureGoal, final String example) {
        if (null == captureGoal || captureGoal.isBlank() || null == candidate) return null;
        // Only applies when the goal literal appears in the example
        if (null == example || !example.contains(captureGoal)) return null;
        final String nonWord = captureGoal.replaceAll("[A-Za-z0-9_]", "");
        if (nonWord.isEmpty()) return null;
        // If candidate clearly has no mention of the non-word literal chars, hint to include them
        boolean missingAny = false;
        for (final char c : nonWord.toCharArray()) {
            final String s = Pattern.quote(String.valueOf(c));
            if (!candidate.contains(String.valueOf(c)) && !candidate.matches(".*\\\\" + s + ".*")) {
                missingAny = true;
                break;
            }
        }
        if (missingAny) {
            return "The goal includes literal symbol(s) from the example (e.g., '" + nonWord + "'). Include these inside the SINGLE capturing group so that group(1) equals the exact literal '" + captureGoal + "'. Escape metacharacters where needed.";
        }
        return null;
    }

    private static String portabilityMessage(final String candidate) {
        final String portability = getPortabilityError(candidate);
        if (null != portability) {
            return portability + " Replace with portable constructs: literals, [..] classes, \\d/\\w/\\s, (?:...) non-capturing, one capturing group around target, numeric quantifiers {n} or {n,m}.";
        }
        return null;
    }

    private static String plainLiteralMessage(final String candidate, final String captureGoal) {
        if (null != candidate) {
            final String trimmed = candidate.trim();
            final String goal = null == captureGoal ? "" : captureGoal;
            try {
                if (trimmed.equals(goal) || trimmed.equals(Pattern.quote(goal)) || !hasRegexMeta(trimmed)) {
                    return "Do NOT return a plain literal. Provide a portable regex using character classes/quantifiers (e.g., \\d, \\w, {n}). If you use groups, prefer a SINGLE capturing group around the target and non-capturing (?:...) for other groups.";
                }
            } catch (final Exception ignore) {}
        }
        return null;
    }

    private static String invalidQuantifierMessage(final String candidate) {
        if (null != candidate && candidate.matches(".*\\{\\s*[^0-9,}].*")) {
            return "Fix invalid quantifiers: use only numeric quantifiers {n} or {n,m}. Do not use {\\w} or any non-numeric inside braces.";
        }
        return null;
    }

    private static String illegalRepetitionMessage(final String validationError) {
        if (null != validationError && validationError.toLowerCase().contains("illegal repetition")) {
            return "Previous attempt had invalid repetition. Use only {n} or {n,m} for quantifiers. Avoid constructs like {\\w}.";
        }
        return null;
    }

    private static String lookaroundMessage(final String candidate) {
        if (null != candidate && (candidate.contains("(?<=") || candidate.contains("(?<!") || candidate.contains("(?=") || candidate.contains("(?!"))) {
            return "Avoid lookarounds unless necessary. Prefer straightforward patterns with a single capturing group around the target and non-capturing groups for the rest.";
        }
        return null;
    }

    private static String groupingCountMessage(final String candidate, final String example) {
        try {
            if (null != candidate && null != example) {
                final Matcher m = Pattern.compile(candidate).matcher(example);
                if (m.find()) {
                    if (0 == m.groupCount()) {
                        return "Prefer ONE capturing group around the desired substring. Use (?:...) for other groups if needed.";
                    }
                    if (1 < m.groupCount()) {
                        return "Prefer a SINGLE capturing group. Convert other groups to non-capturing (?:...) where possible.";
                    }
                }
            }
        } catch (final Exception ignore) {
            // If it doesn't compile, other hints already cover
        }
        return null;
    }

    private static String literalGoalGroupingMessage(final String candidate, final String example, final String captureGoal) {
        if (null != captureGoal && !captureGoal.isBlank() && null != example && example.contains(captureGoal)) {
            try {
                if (null != candidate) {
                    final Matcher m = Pattern.compile(candidate).matcher(example);
                    if (m.find() && 1 < m.groupCount()) {
                        return "There must be EXACTLY ONE capturing group equal to the literal '" + captureGoal + "'. Convert all other groups to non-capturing (?:...).";
                    }
                }
            } catch (final Exception ignore) {}
            return "Ensure there is EXACTLY ONE capturing group that equals the literal '" + captureGoal + "' within the provided example string.";
        }
        return null;
    }

    private static boolean hasRegexMeta(final String s) {
        // Heuristic: does the string contain typical regex metacharacters or escapes/quantifiers
        if (null == s || s.isBlank()) return false;
        return s.matches(".*[\\\\\\.\\[\\]\\(\\)\\{\\}\\+\\*\\?\\|\\^\\$].*");
    }

    // Returns a message if the pattern appears to use non-portable constructs, otherwise null
    private static String getPortabilityError(final String pattern) {
        if (null == pattern || pattern.isBlank()) return "Empty pattern. Provide a portable regex pattern.";
        final String s = pattern;
        // Lookbehind
        if (s.matches(".*\\(\\?<=.*") || s.matches(".*\\(\\?<!.*")) return "Avoid lookbehind; it is not portable across JS/RE2.";
        // Named groups and named backrefs
        if (s.matches(".*\\(\\?<[^>]+>.*") || s.matches(".*\\\\k<[^>]+>.*")) return "Avoid named groups/backreferences; not portable.";
        // Inline flags
        if (s.matches(".*\\(\\?[imsxU-].*")) return "Avoid inline flags like (?i); not portable in pattern-only context.";
        // Atomic groups
        if (s.matches(".*\\(\\?>.*")) return "Avoid atomic groups (?>); not portable.";
        // Branch reset groups (?|)
        if (s.matches(".*\\(\\?\\|.*")) return "Avoid branch reset groups (?|); not portable.";
        // Conditionals (?(...))
        if (s.matches(".*\\(\\?\\(.*")) return "Avoid conditional groups (?(..)); not portable.";
        // Possessive quantifiers: *+, ++, ?+
        if (s.matches(".*(?:\\*\\+|\\+\\+|\\?\\+).*")) return "Avoid possessive quantifiers (*+, ++, ?+); not portable.";
        // Backreferences (numeric)
        if (s.matches(".*\\\\[1-9].*")) return "Avoid backreferences (\\1 etc.); not portable to RE2.";
        // Unicode properties \p{..}
        if (s.matches(".*\\\\p\\{[^}]+}.*")) return "Avoid Unicode properties (\\p{..}); not portable to all engines.";
        return null;
    }
}
