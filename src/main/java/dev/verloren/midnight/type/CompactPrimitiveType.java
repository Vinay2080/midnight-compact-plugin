package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Represents a primitive or nominal scalar type in Compact (e.g. {@code Boolean}, {@code Field}, {@code Bytes}).
 *
 * <p>Contains singleton constants for standard builtins ({@link #BOOLEAN}, {@link #FIELD},
 * {@link #BYTES}, {@link #OPAQUE}, {@link #UNKNOWN}).</p>
 */
public record CompactPrimitiveType(String name) implements CompactType {
  public static final CompactPrimitiveType BOOLEAN = new CompactPrimitiveType("Boolean");
  public static final CompactPrimitiveType FIELD = new CompactPrimitiveType("Field");
  public static final CompactPrimitiveType BYTES = new CompactPrimitiveType("Bytes");
  public static final CompactPrimitiveType OPAQUE = new CompactPrimitiveType("Opaque");
  public static final CompactPrimitiveType UNKNOWN = new CompactPrimitiveType("Unknown");

  @Override
  public @NotNull String name() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactPrimitiveType that = (CompactPrimitiveType) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public @NonNull String toString() {
    return name;
  }
}
