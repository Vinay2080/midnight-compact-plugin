package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Objects;

/**
 * Semantic type representing a declared user or standard library struct type with fields.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Represents nominal struct declarations:
 * <ul>
 *   <li><b>Member Resolution:</b> Provides the field map consumed by
 *       {@link dev.verloren.midnight.reference.CompactStructFieldReference} and
 *       {@link dev.verloren.midnight.completion.providers.CompactMemberAccessCompletionProvider}.</li>
 *   <li><b>Literal Completion:</b> Field map is inspected by
 *       {@link dev.verloren.midnight.completion.providers.CompactStructLiteralCompletionProvider}
 *       to compute missing field templates.</li>
 *   <li><b>Field Transformation:</b> {@link #withFields(Map)} supports specialized or substituted
 *       versions of the struct type where generic fields are instantiated with concrete types.</li>
 * </ul>
 * </p>
 *
 * @see dev.verloren.midnight.type.CompactType
 * @see dev.verloren.midnight.type.CompactParameterizedType
 * @see dev.verloren.midnight.psi.CompactStructDefinition
 */
public record CompactStructType(
    @NotNull String name,
    @NotNull Map<String, CompactType> fields
) implements CompactType {

  /**
   * Returns a copy of this struct type with the given field map.
   *
   * @param newFields updated field mapping
   * @return new {@link CompactStructType} instance
   */
  public @NotNull CompactStructType withFields(@NotNull Map<String, CompactType> newFields) {
    return new CompactStructType(name, newFields);
  }

  /**
   * Evaluates nominal assignability to a target type.
   *
   * @param other target type to test assignment against
   * @return {@code true} if names match or if {@code other} is a matching type variable or parameterized type
   */
  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    if (this.equals(other)) {
      return true;
    }
    return switch (other) {
      case CompactTypeVariable _ -> true;
      case CompactStructType otherStruct -> name.equals(otherStruct.name());
      case CompactParameterizedType param -> name.equals(param.rawName());
      case CompactPrimitiveType(String otherName) -> name.equals(otherName);
      default -> false;
    };
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
