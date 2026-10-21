package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;

/**
 * Sealed base semantic type interface in the Compact plugin's type system.
 *
 * <p>Represents types for expressions, variables, and return types.
 * Permits primitive types, unsigned integers, numeric literal types, parameterized types,
 * struct types, enum types, function types, and type variables.</p>
 */
public sealed interface CompactType permits
    CompactPrimitiveType,
    CompactUintType,
    CompactNumericLiteralType,
    CompactParameterizedType,
    CompactStructType,
    CompactEnumType,
    CompactFunctionType,
    CompactTypeVariable {

  @NotNull String name();

  default boolean isAssignableTo(@NotNull CompactType other) {
    return this.equals(other);
  }

  default @NotNull CompactType substitute(@NotNull CompactTypeSubstitutor substitutor) {
    return substitutor.substitute(this);
  }
}
