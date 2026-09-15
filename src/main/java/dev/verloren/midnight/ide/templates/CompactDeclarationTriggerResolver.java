package dev.verloren.midnight.ide.templates;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Resolves user-typed abbreviation and keyword prefixes into Compact declaration types.
 *
 * <p>Supports:
 * <ul>
 *   <li>Bare keyword prefixes (e.g. {@code ci}, {@code cir}, {@code circuit} &rarr; {@code CIRCUIT})</li>
 *   <li>Witness prefixes (e.g. {@code wi}, {@code wit}, {@code witn}, {@code witness} &rarr; {@code WITNESS})</li>
 *   <li>Compact modifier + declaration combinations (e.g. {@code exci}, {@code excir}, {@code exportci}, {@code exportcir} &rarr; exported {@code CIRCUIT})</li>
 *   <li>Separated modifier + declaration combinations (e.g. {@code export circ}, {@code export wit} &rarr; exported declaration)</li>
 *   <li>Strict filtering for non-exportable constructs (e.g. {@code const} is rejected when combined with {@code export})</li>
 * </ul>
 */
public final class CompactDeclarationTriggerResolver {

  public record TriggerResult(
      @NotNull CompactDeclarationType declarationType,
      boolean isExported
  ) {}

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

  private static final List<String> EXPORT_PREFIXES = List.of("export", "exp", "ex");

  private CompactDeclarationTriggerResolver() {}

  /**
   * Resolves a raw typed trigger into a {@link TriggerResult}.
   *
   * @param raw the user-typed trigger string
   * @return the resolved trigger result, or {@code null} if no valid declaration matches
   */
  public static @Nullable TriggerResult resolve(@Nullable String raw) {
    if (raw == null) {
      return null;
    }
    String s = raw.toLowerCase(Locale.ROOT).trim();
    if (s.length() < 2) {
      return null;
    }

    boolean isExported = false;
    String remainder = s;

    // Check for separated modifier form: e.g. "export circ", "exp cir", "ex ci"
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

    CompactDeclarationType matchedType = findMatchingType(remainder, isExported);
    if (matchedType == null) {
      return null;
    }

    // Strict legality check: respect which declaration types legally support export
    if (isExported && !matchedType.isExportable()) {
      return null;
    }

    return new TriggerResult(matchedType, isExported);
  }

  private static boolean matchesAnyType(@NotNull String declPrefix) {
    for (CompactDeclarationType type : CompactDeclarationType.values()) {
      if (matchesDeclaration(declPrefix, type)) {
        return true;
      }
    }
    return false;
  }

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
   * Generates all valid lookup strings for a given declaration type.
   *
   * @param type       the declaration type
   * @param isExported whether to generate export-prefixed variants
   * @return set of lookup strings for IntelliJ's PrefixMatcher
   */
  public static @NotNull Set<String> generateLookupStrings(@NotNull CompactDeclarationType type, boolean isExported) {
    if (isExported && !type.isExportable()) {
      return Collections.emptySet();
    }

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
