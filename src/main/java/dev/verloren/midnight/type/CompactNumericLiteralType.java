package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Semantic type representing a numeric integer literal expression (e.g. {@code 0}, {@code 20}, {@code 0xFF}).
 *
 * <p>Numeric literals can be assigned to {@code Field} or {@code Uint<N>} provided the literal value
 * fits within the target type's bounds.</p>
 */
public class CompactNumericLiteralType implements CompactType {
  private final @NotNull String rawText;
  private final @Nullable BigInteger value;

  public CompactNumericLiteralType(@NotNull String rawText) {
    this(rawText, parseValue(rawText));
  }

  public CompactNumericLiteralType(@NotNull String rawText, @Nullable BigInteger value) {
    this.rawText = rawText;
    this.value = value;
  }

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

  public @NotNull String getRawText() {
    return rawText;
  }

  public @Nullable BigInteger getValue() {
    return value;
  }

  @Override
  public @NotNull String name() {
    return "Field";
  }

  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    if (other instanceof CompactNumericLiteralType) {
      return true;
    }
    if (other instanceof CompactUintType uintType) {
      if (value == null) return true;
      return uintType.fits(value);
    }
    if (other instanceof CompactPrimitiveType primitive) {
      if (CompactPrimitiveType.FIELD.equals(primitive)
          || "JubjubScalar".equals(primitive.name())
          || "Secp256k1Base".equals(primitive.name())
          || "Secp256k1Scalar".equals(primitive.name())) {
        return true;
      }
      if (primitive.name().startsWith("Uint")) {
        CompactUintType uintType = CompactUintType.parse(primitive.name());
        if (uintType != null) {
          if (value == null) return true;
          return uintType.fits(value);
        }
        return true;
      }
      return false;
    }
    if ("Field".equals(other.name())) {
      return true;
    }
    if (other.name().startsWith("Uint")) {
      CompactUintType uintType = CompactUintType.parse(other.name());
      if (uintType != null) {
        if (value == null) return true;
        return uintType.fits(value);
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactNumericLiteralType that = (CompactNumericLiteralType) o;
    return Objects.equals(rawText, that.rawText) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawText, value);
  }

  @Override
  public String toString() {
    return rawText;
  }
}
