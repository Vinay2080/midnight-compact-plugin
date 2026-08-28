package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;

/**
 * Semantic type representing a numeric integer literal expression (e.g. {@code 0}, {@code 20}, {@code 0xFF}).
 *
 * <p>Numeric literals can be assigned to {@code Field} or {@code Uint<N>} provided the literal value
 * fits within the target type's bounds.</p>
 */
public record CompactNumericLiteralType(@NotNull String rawText, @Nullable BigInteger value) implements CompactType {
  public CompactNumericLiteralType(@NotNull String rawText) {
    this(rawText, parseValue(rawText));
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
