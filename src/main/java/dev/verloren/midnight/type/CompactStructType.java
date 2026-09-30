package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Objects;

/**
 * Semantic type representing a declared user or library struct type with fields.
 */
public record CompactStructType(
    @NotNull String name,
    @NotNull Map<String, CompactType> fields
) implements CompactType {

  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    if (this.equals(other)) {
      return true;
    }
    if (other instanceof CompactStructType otherStruct) {
      return name.equals(otherStruct.name());
    }
    if (other instanceof CompactPrimitiveType(String otherName)) {
      return name.equals(otherName);
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactStructType that = (CompactStructType) o;
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
