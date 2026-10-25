package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;

/**
 * Semantic type representing a numeric integer literal expression (e.g. {@code 0}, {@code 20}, {@code 0xFF}, {@code 0b1010}).
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Assigned to literal integer expression nodes in the AST:
 * <ul>
 *   <li><b>Type Inference:</b> Returned by {@link dev.verloren.midnight.psi.CompactExpression#getType()}
 *       when the expression is a numeric constant.</li>
 *   <li><b>Polymorphic Literal Coercion:</b> Unlike typed variables, numeric literals in Compact can
 *       freely coerce into {@code Field}, elliptic curve scalar types ({@code JubjubScalar}, {@code Secp256k1Scalar}),
 *       or {@code Uint<N>} provided the literal value fits within the target's numerical bounds.</li>
 *   <li><b>Radix Parsing:</b> Parses decimal, hexadecimal ({@code 0x}), binary ({@code 0b}), and octal ({@code 0o})
 *       literals, automatically stripping visual separator underscores ({@code _}).</li>
 * </ul>
 * </p>
 *
 * <h3>Coercion Hierarchy Diagram</h3>
 * <pre>
 *                       [CompactNumericLiteralType: 42]
 *                                     │
 *                ┌────────────────────┼────────────────────┐
 *                ▼                    ▼                    ▼
 *            [Field]             [Uint&lt;N&gt;]         [Curve Scalars]
 *          (ZK field)         (if fits bounds)    (Jubjub, Secp256k1)
 * </pre>
 *
 * @see dev.verloren.midnight.type.CompactType
 * @see dev.verloren.midnight.type.CompactUintType
 */
public record CompactNumericLiteralType(
    @NotNull String rawText,
    @Nullable BigInteger value
) implements CompactType {

  /**
   * Convenience constructor parsing the literal string into a {@link BigInteger} value.
   *
   * @param rawText literal token text from the PSI node
   */
  public CompactNumericLiteralType(@NotNull String rawText) {
    this(rawText, parseValue(rawText));
  }

  /**
   * Parses integer literal strings in various number bases into a {@link BigInteger}.
   *
   * <p>Supports:
   * <ul>
   *   <li>Hexadecimal: {@code 0x...} or {@code 0X...} (base 16)</li>
   *   <li>Binary: {@code 0b...} or {@code 0B...} (base 2)</li>
   *   <li>Octal: {@code 0o...} or {@code 0O...} (base 8)</li>
   *   <li>Decimal: standard base 10</li>
   * </ul>
   * Underscore digit separators (e.g. {@code 1_000_000}) are stripped before parsing.</p>
   *
   * @param text raw token text
   * @return parsed {@link BigInteger}, or {@code null} if text cannot be parsed
   */
  public static @Nullable BigInteger parseValue(@Nullable String text) {
    if (text == null || text.isEmpty()) return null;
    try {
      String cleanText = text.replace("_", "");
      if (cleanText.startsWith("0x") || cleanText.startsWith("0X")) {
        return new BigInteger(cleanText.substring(2), 16);
      } else if (cleanText.startsWith("0b") || cleanText.startsWith("0B")) {
        return new BigInteger(cleanText.substring(2), 2);
      } else if (cleanText.startsWith("0o") || cleanText.startsWith("0O")) {
        return new BigInteger(cleanText.substring(2), 8);
      } else {
        return new BigInteger(cleanText, 10);
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Default canonical name for numeric literals is {@code "Field"}.
   */
  @Override
  public @NotNull String name() {
    return "Field";
  }

  /**
   * Evaluates assignability of this numeric literal to a target type.
   *
   * <p>Allows coercion to:
   * <ul>
   *   <li>Other numeric literals</li>
   *   <li>{@link CompactUintType} if the literal's value fits in the target's {@code [min, max]} range</li>
   *   <li>{@code Field}, {@code JubjubScalar}, {@code Secp256k1Base}, or {@code Secp256k1Scalar}</li>
   * </ul>
   * </p>
   *
   * @param other target type to test assignment against
   * @return {@code true} if the literal can safely coerce to {@code other}
   */
  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    if (other instanceof CompactNumericLiteralType) {
      return true;
    }
    if (other instanceof CompactUintType uintType) {
      return value == null || uintType.fits(value);
    }
    String name = other.name();
    if ("Field".equals(name) || "JubjubScalar".equals(name) || "Secp256k1Base".equals(name) || "Secp256k1Scalar".equals(name)) {
      return true;
    }
    if (name.startsWith("Uint")) {
      CompactUintType uintType = CompactUintType.parse(name);
      return uintType == null || value == null || uintType.fits(value);
    }
    return false;
  }

  @Override
  public @NonNull String toString() {
    return rawText;
  }
}
