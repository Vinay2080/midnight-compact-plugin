package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Semantic type representing a generic type parameter or type variable in Compact
 * (e.g. {@code T}, {@code #T}, {@code TVal}, {@code TErr}, {@code A}, {@code B}).
 */
public record CompactTypeVariable(
    @NotNull String name
) implements CompactType {

  public CompactTypeVariable {
    if (name.isBlank()) {
      throw new IllegalArgumentException("Type variable name cannot be blank");
    }
  }

  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    // An unbound type variable can unify with any type or another type variable
    return true;
  }

  public @NotNull String cleanName() {
    return name.startsWith("#") ? name.substring(1) : name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactTypeVariable that = (CompactTypeVariable) o;
    return cleanName().equals(that.cleanName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(cleanName());
  }

  @Override
  public @NonNull String toString() {
    return name;
  }
}
