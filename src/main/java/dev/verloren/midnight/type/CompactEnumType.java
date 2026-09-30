package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Semantic type representing a declared user enum type with variants.
 */
public record CompactEnumType(
    @NotNull String name,
    @NotNull List<String> variants
) implements CompactType {

  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    if (this.equals(other)) {
      return true;
    }
    if (other instanceof CompactEnumType otherEnum) {
      return name.equals(otherEnum.name());
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
    CompactEnumType that = (CompactEnumType) o;
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
