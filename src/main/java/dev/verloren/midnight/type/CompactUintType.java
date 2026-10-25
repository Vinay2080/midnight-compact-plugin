package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Semantic type representing unsigned integer types in Compact (e.g. {@code Uint<8>}, {@code Uint<32>}, {@code Uint<0..256>}, {@code Uint}).
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Unsigned integers in Compact are first-class arithmetic types with bit-width or range constraints:
 * <ul>
 *   <li><b>Type Parsing:</b> Instantiated by {@link #parse(String)}, {@link #ofBits(int)}, and
 *       {@link #ofRange(BigInteger, BigInteger)} when parsing signatures like {@code Uint<16>} or {@code Uint<1..10>}.</li>
 *   <li><b>Inspection & Range Verification:</b> Used by arithmetic and assignment inspections to ensure
 *       integer literals or smaller integers do not overflow target bounds ({@link #fits(BigInteger)}).</li>
 *   <li><b>Interval Subtyping:</b> {@code Uint<A>} is assignable to {@code Uint<B>} if and only if
 *       the numerical interval {@code [min_A, max_A]} is completely contained within {@code [min_B, max_B]}.</li>
 * </ul>
 * </p>
 *
 * <h3>Range Containment Visual Diagram</h3>
 * <pre>
 *   Target:  [min_B = 0 ═════════════════════════════════════ max_B = 65535]  (Uint&lt;16&gt;)
 *                             ▲                 ▲
 *                             │   Subtype check │
 *                             │   [min_A &gt;= min_B &amp;&amp; max_A &lt;= max_B]
 *                             │                 │
 *   Source:                   [min_A = 0 ═══ max_A = 255]                     (Uint&lt;8&gt;)
 * </pre>
 *
 * <h3>Edge Case Mitigations</h3>
 * <ul>
 *   <li><b>Default Unbounded Uint:</b> Bare {@code "Uint"} defaults to 256 bits with max value
 *       {@code 2^256 - 1} ({@link #MAX_UINT256}).</li>
 *   <li><b>Half-Open Ranges:</b> Ranges in Compact are written as {@code Uint<min..maxExclusive>},
 *       where {@code maxExclusive} is exclusive. The parsed {@link #max} field stores {@code maxExclusive - 1}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.type.CompactType
 * @see dev.verloren.midnight.type.CompactNumericLiteralType
 */
public record CompactUintType(
    @NotNull String name,
    @Nullable Integer bits,
    @NotNull BigInteger min,
    @NotNull BigInteger max
) implements CompactType {

  /**
   * Maximum 256-bit unsigned integer value: {@code 2^256 - 1}.
   */
  public static final BigInteger MAX_UINT256 = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE);

  /**
   * Factory method creating a bit-width constrained unsigned integer type (e.g. {@code Uint<8>}).
   *
   * @param bits bit-width (e.g., 8, 16, 32, 64)
   * @return new {@link CompactUintType} spanning {@code [0, 2^bits - 1]}
   */
  public static @NotNull CompactUintType ofBits(int bits) {
    BigInteger max = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);
    return new CompactUintType("Uint<" + bits + ">", bits, BigInteger.ZERO, max);
  }

  /**
   * Factory method creating an explicit interval-bounded unsigned integer type (e.g. {@code Uint<0..10>}).
   *
   * @param min          inclusive lower bound
   * @param maxExclusive exclusive upper bound
   * @return new {@link CompactUintType} spanning {@code [min, maxExclusive - 1]}
   */
  public static @NotNull CompactUintType ofRange(@NotNull BigInteger min, @NotNull BigInteger maxExclusive) {
    BigInteger max = maxExclusive.subtract(BigInteger.ONE);
    return new CompactUintType("Uint<" + min + ".." + maxExclusive + ">", null, min, max);
  }

  /**
   * Parses a textual type string into a structured {@link CompactUintType}.
   *
   * @param text string to parse (e.g. {@code "Uint"}, {@code "Uint<32>"}, {@code "Uint<0..100>"})
   * @return parsed {@link CompactUintType}, or {@code null} if text does not match Uint grammar
   */
  public static @Nullable CompactUintType parse(@NotNull String text) {
    // Case 1: Bare "Uint" keyword (defaults to 256-bit integer)
    if ("Uint".equals(text)) {
      return new CompactUintType("Uint", 256, BigInteger.ZERO, MAX_UINT256);
    }

    // Case 2: Parameterized "Uint<...>"
    if (text.startsWith("Uint<") && text.endsWith(">")) {
      String inner = text.substring(5, text.length() - 1).trim();
      // Subcase 2a: Range syntax "min..max"
      if (inner.contains("..")) {
        String[] parts = inner.split("\\.\\.");
        if (parts.length == 2) {
          try {
            BigInteger min = new BigInteger(parts[0].trim());
            BigInteger maxExclusive = new BigInteger(parts[1].trim());
            return ofRange(min, maxExclusive);
          } catch (NumberFormatException ignored) {
          }
        }
      } else {
        // Subcase 2b: Bit-width syntax "bits"
        try {
          int bits = Integer.parseInt(inner);
          if (bits > 0) {
            return ofBits(bits);
          }
        } catch (NumberFormatException ignored) {
        }
      }
      return new CompactUintType(text, null, BigInteger.ZERO, MAX_UINT256);
    }
    return null;
  }

  /**
   * Checks whether a specific numerical value falls within this type's inclusive bounds {@code [min, max]}.
   *
   * @param val value to test
   * @return {@code true} if {@code val >= min && val <= max}
   */
  public boolean fits(@NotNull BigInteger val) {
    return val.compareTo(min) >= 0 && val.compareTo(max) <= 0;
  }

  /**
   * Evaluates subtyping assignability via numerical interval containment.
   *
   * <p>A source unsigned integer type is assignable to a target if and only if
   * its numerical range is completely enclosed within the target's range.</p>
   *
   * @param other target type to test assignment against
   * @return {@code true} if every valid value of this type fits into {@code other}
   */
  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    if (this.equals(other)) return true;

    // Check interval containment against another CompactUintType
    if (other instanceof CompactUintType otherUint) {
      return this.min.compareTo(otherUint.min) >= 0 && this.max.compareTo(otherUint.max) <= 0;
    }

    // Check against primitive representations of Uint
    if (other instanceof CompactPrimitiveType(String name1)) {
      if ("Uint".equals(name1)) return true;
      if (name1.equals(this.name)) return true;
      CompactUintType parsed = CompactUintType.parse(name1);
      if (parsed != null) {
        return this.min.compareTo(parsed.min) >= 0 && this.max.compareTo(parsed.max) <= 0;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactUintType that = (CompactUintType) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public @NonNull String toString() {
    return name;
  }
}
