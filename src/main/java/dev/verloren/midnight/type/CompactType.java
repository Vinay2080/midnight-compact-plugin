package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;

/**
 * Base semantic type interface in the Compact plugin's type system.
 *
 * <p>Represents types for expressions, variables, and return types.
 * Provides {@link #name()} and {@link #isAssignableTo(CompactType)}.</p>
 */
public interface CompactType {
  @NotNull String name();

  default boolean isAssignableTo(@NotNull CompactType other) {
    return this.equals(other);
  }
}
