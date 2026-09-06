package dev.verloren.midnight.version;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing semantic versions and validating pragma version constraints.
 */
public final class CompactSemVerUtil {
  private static final Pattern SEMVER_PATTERN = Pattern.compile(
      "^v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-([0-9A-Za-z.-]+))?"
  );

  private static final Pattern CONSTRAINT_PATTERN = Pattern.compile(
      "(>=|<=|>|<|==|=|\\^|~)?\\s*v?(\\d+\\.\\d+(?:\\.\\d+)?(?:-[0-9A-Za-z.-]+)?)"
  );

  public record SemVer(int major, int minor, int patch, @Nullable String preRelease) implements Comparable<SemVer> {
    @Override
    public int compareTo(@NotNull SemVer o) {
      if (this.major != o.major) {
        return Integer.compare(this.major, o.major);
      }
      if (this.minor != o.minor) {
        return Integer.compare(this.minor, o.minor);
      }
      if (this.patch != o.patch) {
        return Integer.compare(this.patch, o.patch);
      }
      if (this.preRelease == null && o.preRelease != null) {
        return 1; // Release > pre-release
      }
      if (this.preRelease != null && o.preRelease == null) {
        return -1;
      }
      if (this.preRelease != null) {
        return this.preRelease.compareTo(o.preRelease);
      }
      return 0;
    }

    @Override
    public @NonNull String toString() {
      String base = major + "." + minor + "." + patch;
      return preRelease != null ? base + "-" + preRelease : base;
    }
  }

  private CompactSemVerUtil() {
  }

  /**
   * Parses a SemVer string into a {@link SemVer} object.
   */
  public static @Nullable SemVer parse(@Nullable String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    Matcher m = SEMVER_PATTERN.matcher(text.trim());
    if (!m.find()) {
      return null;
    }
    int major = Integer.parseInt(m.group(1));
    int minor = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
    int patch = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
    String pre = m.group(4);
    return new SemVer(major, minor, patch, pre);
  }

  /**
   * Extracts the bare version number (e.g. "0.26.0") from a pragma expression (e.g. ">= 0.26.0").
   */
  public static @Nullable String extractVersion(@Nullable String constraintText) {
    if (constraintText == null) return null;
    Matcher m = CONSTRAINT_PATTERN.matcher(constraintText.trim());
    if (m.find()) {
      return m.group(2);
    }
    return null;
  }

  /**
   * Checks whether the compiler version satisfies the given pragma constraint expression.
   */
  public static boolean satisfiesConstraint(@NotNull String compilerVersion, @NotNull String constraintText) {
    SemVer comp = parse(compilerVersion);
    if (comp == null) {
      return true; // Cannot determine, do not falsely flag
    }

    Matcher m = CONSTRAINT_PATTERN.matcher(constraintText.trim());
    if (!m.find()) {
      return true;
    }

    String op = m.group(1);
    String reqVerStr = m.group(2);
    SemVer req = parse(reqVerStr);
    if (req == null) {
      return true;
    }

    if (op == null || op.isEmpty() || op.equals("==") || op.equals("=")) {
      return comp.compareTo(req) == 0;
    }

    return switch (op) {
      case ">=" -> comp.compareTo(req) >= 0;
      case ">" -> comp.compareTo(req) > 0;
      case "<=" -> comp.compareTo(req) <= 0;
      case "<" -> comp.compareTo(req) < 0;
      case "^" -> {
        if (comp.compareTo(req) < 0) yield false;
        if (req.major() > 0) {
          yield comp.major() == req.major();
        } else if (req.minor() > 0) {
          yield comp.minor() == req.minor();
        } else {
          yield comp.patch() == req.patch();
        }
      }
      case "~" -> comp.compareTo(req) >= 0 && comp.major() == req.major() && comp.minor() == req.minor();
      default -> true;
    };
  }
}
