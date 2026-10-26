package dev.verloren.midnight.ide.templates;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * High-performance deterministic resolver and alias generator for Compact declaration triggers.
 *
 * <p>Translates user-typed abbreviation prefixes, keyword snippets, and compound modifier constructs
 * into strongly-typed {@link CompactDeclarationType} targets. It serves two distinct roles in the IDE:</p>
 * <ol>
 *   <li><b>Live Template Trigger Resolution:</b> Used by {@code dev.verloren.midnight.ide.templates.CompactDeclarationContext}
 *       to inspect the prefix immediately preceding the caret to determine if a declaration live template is active.</li>
 *   <li><b>Completion Lookup Alias Generation:</b> Used by {@link dev.verloren.midnight.completion.providers.CompactKeywordCompletionProvider}
 *       to register synthetic trigger strings with IntelliJ's {@code PrefixMatcher}, enabling shorthand popup matches.</li>
 * </ol>
 *
 * <h3>Supported Syntax Combinations</h3>
 * <ul>
 *   <li><b>Bare Keyword Prefixes:</b> Substring matching on base names (e.g. {@code ci}, {@code cir}, {@code circuit} &rarr; {@code CIRCUIT}).</li>
 *   <li><b>Canonical Abbreviation Shortcuts:</b> Defined shorthands (e.g. {@code str} &rarr; {@code STRUCT}, {@code led} &rarr; {@code LEDGER}, {@code cct} &rarr; {@code CONTRACT}).</li>
 *   <li><b>Compact Modifier Combinations:</b> Compound words without spaces (e.g. {@code exci}, {@code expcir}, {@code exportcircuit} &rarr; exported {@code CIRCUIT}).</li>
 *   <li><b>Separated Modifier Combinations:</b> Spaced keywords (e.g. {@code export circ}, {@code exp wit}, {@code ex cct} &rarr; exported declaration).</li>
 *   <li><b>Grammar Legality Filtering:</b> Strictly validates export compatibility (e.g. {@code const} rejects export combinations since Compact does not permit exported constants).</li>
 * </ul>
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> Pure domain resolver and prefix catalogue; collaborator to completion and live template engines.</li>
 *   <li><b>Calling Pipeline:</b>
 *     <ul>
 *       <li>Invoked synchronously by {@code TemplateManager} / {@code CustomLiveTemplate} on the EDT or background threads during context resolution.</li>
 *       <li>Invoked by {@code CodeCompletionHandlerBase} via {@link dev.verloren.midnight.completion.providers.CompactKeywordCompletionProvider}
 *           across background pooled worker threads during basic code completion.</li>
 *     </ul>
 *   </li>
 *   <li><b>Threading Model:</b> Thread-safe and completely stateless. All methods are pure static functions requiring no locks,
 *       though typically executed within an ambient Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Cancellation Support:</b> Operates purely in-memory with O(1) bounded collections; guarantees zero-allocation bottlenecks
 *       and cooperative cancellation without blocking.</li>
 * </ul>
 *
 * <h3>Resolution &amp; Disambiguation Pipeline</h3>
 * <pre>{@code
 *   User Input (e.g. "exci", "export circ", "cir")
 *              │
 *              ▼
 *      [Normalize & Validate] (trim, lowercase, min length >= 2)
 *              │
 *              ├─► Space detected? ────► Validate modifier part in EXPORT_PREFIXES
 *              │                                  │
 *              └─► No space? ──────────► Scan compact prefix in EXPORT_PREFIXES
 *                                                 │
 *              ┌──────────────────────────────────┘
 *              ▼
 *      [findMatchingType(remainder)]
 *              │
 *              ├── 1. Collect all candidates where base name or abbrev starts with remainder
 *              ├── 2. Disambiguate if isExported: filter out non-exportable types (e.g. CONST vs CONTRACT)
 *              ├── 3. Priority tie-breaker: exact baseName match > exact abbrev match > first candidate
 *              └── 4. Validate legality: isExported && !type.isExportable() ? null : TriggerResult
 * }</pre>
 *
 * <h3>Design Decisions &amp; Memory Safety</h3>
 * <ul>
 *   <li><b>Stateless Utility Class:</b> Enforces a {@code private} constructor and pure static functions to eliminate object allocation.</li>
 *   <li><b>Zero Persistent State:</b> Never retains references to {@code Project}, {@code Editor}, or {@code PsiElement}, preventing memory leaks.</li>
 *   <li><b>Ordered Set Guarantees:</b> {@link #generateLookupStrings(CompactDeclarationType, boolean)} uses {@link LinkedHashSet}
 *       to guarantee deterministic ordering in IntelliJ's completion ranking.</li>
 * </ul>
 *
 * @see CompactDeclarationType
 * @see dev.verloren.midnight.completion.providers.CompactKeywordCompletionProvider
 */
public final class CompactDeclarationTriggerResolver {

  /**
   * Immutable result holding the resolved declaration construct and its export modifier status.
   *
   * @param declarationType the target Compact declaration construct (circuit, struct, ledger, etc.)
   * @param isExported      {@code true} if the trigger included an export modifier prefix; {@code false} otherwise
   */
  public record TriggerResult(
      @NotNull CompactDeclarationType declarationType,
      boolean isExported
  ) {}

  /**
   * Curated mapping from declaration types to recognized ergonomic abbreviations.
   *
   * <p>Immutable and static to guarantee thread safety and remove allocation during high-frequency keystrokes.</p>
   */
  private static final Map<CompactDeclarationType, List<String>> TYPE_ABBREVIATIONS = Map.of(
      CompactDeclarationType.CIRCUIT, List.of("cir"),
      CompactDeclarationType.WITNESS, List.of("wit"),
      CompactDeclarationType.STRUCT, List.of("str"),
      CompactDeclarationType.ENUM, List.of("en"),
      CompactDeclarationType.MODULE, List.of("mod"),
      CompactDeclarationType.CONTRACT, List.of("cct", "ccti", "con"),
      CompactDeclarationType.LEDGER, List.of("led", "ledg"),
      CompactDeclarationType.TYPE, List.of("ty"),
      CompactDeclarationType.CONST, List.of()
  );

  /**
   * Permitted prefixes recognized as an export modifier.
   *
   * <p>Ordered from longest to shortest to ensure greedy matching in compact prefix forms.</p>
   */
  private static final List<String> EXPORT_PREFIXES = List.of("export", "exp", "ex");

  /**
   * Private constructor to prevent instantiation of this static utility class.
   */
  private CompactDeclarationTriggerResolver() {}

  /**
   * Resolves a raw user-typed trigger into a validated {@link TriggerResult}.
   *
   * <p>Handles whitespace trimming, lowercasing, separated tokens ({@code "export cir"}),
   * compact tokens ({@code "exci"}), and language syntax constraints (rejecting non-exportable items).</p>
   *
   * @param raw the user-typed trigger string
   * @return the resolved trigger result, or {@code null} if no valid declaration matches or is grammatically illegal
   */
  public static @Nullable TriggerResult resolve(@Nullable String raw) {
    if (raw == null) {
      return null;
    }
    // Step 1: Normalize input (lowercased in root locale to avoid Turkish-i anomalies)
    String s = raw.toLowerCase(Locale.ROOT).trim();
    if (s.length() < 2) {
      return null;
    }

    boolean isExported = false;
    String remainder = s;

    // Step 2: Check for a separated modifier form: e.g. "export circ", "exp cir", "ex ci"
    int firstSpace = s.indexOf(' ');
    if (firstSpace != -1) {
      String modPart = s.substring(0, firstSpace).trim();
      String declPart = s.substring(firstSpace + 1).trim();
      if (EXPORT_PREFIXES.contains(modPart)) {
        isExported = true;
        remainder = declPart;
      } else {
        return null;
      }
    } else {
      // Check for compact modifier form: e.g. "exportcir", "expcir", "excir", "exci"
      for (String prefix : EXPORT_PREFIXES) {
        if (s.startsWith(prefix) && s.length() > prefix.length()) {
          String candidateRemainder = s.substring(prefix.length());
          if (candidateRemainder.length() >= 2 && matchesAnyType(candidateRemainder)) {
            isExported = true;
            remainder = candidateRemainder;
            break;
          }
        }
      }
    }

    if (remainder.length() < 2) {
      return null;
    }

    // Step 5: Resolve declaration type target with conflict disambiguation
    CompactDeclarationType matchedType = findMatchingType(remainder, isExported);
    if (matchedType == null) {
      return null;
    }

    // Step 6: Strict legality check: respect which declaration types legally support export
    if (isExported && !matchedType.isExportable()) {
      return null;
    }

    return new TriggerResult(matchedType, isExported);
  }

  /**
   * Fast-path check determining whether {@code declPrefix} matches at least one known declaration construct.
   *
   * @param declPrefix prefix candidate to evaluate
   * @return {@code true} if at least one construct matches; {@code false} otherwise
   */
  private static boolean matchesAnyType(@NotNull String declPrefix) {
    for (CompactDeclarationType type : CompactDeclarationType.values()) {
      if (matchesDeclaration(declPrefix, type)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Discovers and disambiguate the appropriate {@link CompactDeclarationType} for a declaration prefix.
   *
   * <p>Applies layered disambiguation rules:</p>
   * <ol>
   *   <li>Filters out non-exportable types if {@code isExported} is true (e.g. {@code con} could match {@code contract} or {@code const}).</li>
   *   <li>Prefers exact base-name matches over abbreviations.</li>
   *   <li>Prefers exact abbreviation matches over partial prefix matches.</li>
   *   <li>Falls back to the first registered matching candidate.</li>
   * </ol>
   *
   * @param declPrefix the isolated declaration prefix (e.g. {@code "cir"}, {@code "str"}, {@code "con"})
   * @param isExported whether the declaration is prefixed with an export modifier
   * @return matched declaration type, or {@code null} if no types match
   */
  private static @Nullable CompactDeclarationType findMatchingType(@NotNull String declPrefix, boolean isExported) {
    List<CompactDeclarationType> matches = new ArrayList<>();
    for (CompactDeclarationType type : CompactDeclarationType.values()) {
      if (matchesDeclaration(declPrefix, type)) {
        matches.add(type);
      }
    }

    if (matches.isEmpty()) {
      return null;
    }

    if (matches.size() == 1) {
      return matches.getFirst();
    }

    // Disambiguate conflicts:
    // If exported, filter out non-exportable types (e.g. 'con' could match CONTRACT or CONST, but CONST is not exportable)
    if (isExported) {
      matches.removeIf(t -> !t.isExportable());
      if (matches.size() == 1) {
        return matches.getFirst();
      }
    }

    // Exact base name match takes top priority
    for (CompactDeclarationType t : matches) {
      if (t.getBaseName().equals(declPrefix)) {
        return t;
      }
    }

    // Exact abbreviation match takes second priority
    for (CompactDeclarationType t : matches) {
      List<String> abbrevs = TYPE_ABBREVIATIONS.getOrDefault(t, List.of());
      if (abbrevs.contains(declPrefix)) {
        return t;
      }
    }

    return matches.getFirst();
  }

  /**
   * Checks whether a candidate prefix matches either the base name or any defined abbreviations of a type.
   *
   * @param declPrefix prefix snippet to evaluate
   * @param type       target declaration type
   * @return {@code true} if base name or any abbreviation starts with {@code declPrefix}
   */
  private static boolean matchesDeclaration(@NotNull String declPrefix, @NotNull CompactDeclarationType type) {
    String baseName = type.getBaseName();
    if (baseName.startsWith(declPrefix)) {
      return true;
    }
    List<String> abbrevs = TYPE_ABBREVIATIONS.getOrDefault(type, List.of());
    for (String abbrev : abbrevs) {
      if (abbrev.startsWith(declPrefix)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Generates all valid lookup trigger strings for a given declaration type to populate IntelliJ's {@code PrefixMatcher}.
   *
   * <p>Produces bare substrings (at least 2 characters), abbreviation prefixes, and if {@code isExported} is true,
   * Cartesian permutations with all recognized export prefixes (compact and spaced forms).</p>
   *
   * @param type       the declaration type
   * @param isExported whether to generate export-prefixed variants
   * @return ordered set of lookup strings for IntelliJ's completion matcher
   */
  public static @NotNull Set<String> generateLookupStrings(@NotNull CompactDeclarationType type, boolean isExported) {
    if (isExported && !type.isExportable()) {
      return Collections.emptySet();
    }

    // Step 1: Collect all bare prefix combinations (base name + abbreviations)
    Set<String> barePrefixes = new LinkedHashSet<>();
    String baseName = type.getBaseName();
    for (int len = 2; len <= baseName.length(); len++) {
      barePrefixes.add(baseName.substring(0, len));
    }

    List<String> abbrevs = TYPE_ABBREVIATIONS.getOrDefault(type, List.of());
    for (String abbrev : abbrevs) {
      for (int len = 2; len <= abbrev.length(); len++) {
        barePrefixes.add(abbrev.substring(0, len));
      }
    }

    if (!isExported) {
      return barePrefixes;
    }

    // Step 2: Expand Cartesian product of export modifiers with bare prefixes for both compact and spaced forms
    Set<String> exportLookups = new LinkedHashSet<>();
    for (String bp : barePrefixes) {
      exportLookups.add("ex" + bp);
      exportLookups.add("exp" + bp);
      exportLookups.add("export" + bp);
      exportLookups.add("export " + bp);
      exportLookups.add("exp " + bp);
      exportLookups.add("ex " + bp);
    }

    return exportLookups;
  }
}
