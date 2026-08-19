package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Semantic type representing unsigned integer types in Compact (e.g. {@code Uint<8>}, {@code Uint<32>}, {@code Uint<0..256>}, {@code Uint}).
 */
public record CompactUintType(@NotNull String name, @Nullable Integer bits, @NotNull BigInteger min,
                              @NotNull BigInteger max) implements CompactType {
  public static final BigInteger MAX_UINT256 = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE);

  public static @NotNull CompactUintType ofBits(int bits) {
    BigInteger max = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);
    return new CompactUintType("Uint<" + bits + ">", bits, BigInteger.ZERO, max);
  }

  public static @NotNull CompactUintType ofRange(@NotNull BigInteger min, @NotNull BigInteger maxExclusive) {
    BigInteger max = maxExclusive.subtract(BigInteger.ONE);
    return new CompactUintType("Uint<" + min + ".." + maxExclusive + ">", null, min, max);
  }

  public static @Nullable CompactUintType parse(@NotNull String text) {
    if ("Uint".equals(text)) {
      return new CompactUintType("Uint", 256, BigInteger.ZERO, MAX_UINT256);
    }
    if (text.startsWith("Uint<") && text.endsWith(">")) {
      String inner = text.substring(5, text.length() - 1).trim();
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

  public boolean fits(@NotNull BigInteger val) {
    return val.compareTo(min) >= 0 && val.compareTo(max) <= 0;
  }

  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    if (this.equals(other)) return true;
    if (other instanceof CompactUintType otherUint) {
      return this.min.compareTo(otherUint.min) >= 0 && this.max.compareTo(otherUint.max) <= 0;
    }
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
