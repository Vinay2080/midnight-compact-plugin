package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Semantic type representing a declared user enum type with variants.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Models nominal enum definitions:
 * <ul>
 *   <li><b>Navigation & Resolution:</b> Powers variant lookup in
 *       {@link dev.verloren.midnight.reference.CompactEnumMemberReference}.</li>
 *   <li><b>Code Completion:</b> Variant list is offered during dot-access completion
 *       ({@code State.<caret>}) via {@link dev.verloren.midnight.completion.providers.CompactMemberAccessCompletionProvider}.</li>
 * </ul>
 * </p>
 *
 * @see dev.verloren.midnight.type.CompactType
 * @see dev.verloren.midnight.reference.CompactEnumMemberReference
 * @see dev.verloren.midnight.psi.CompactEnumDefinition
 */
public record CompactEnumType(
    @NotNull String name,
    @NotNull List<String> variants
) implements CompactType {

  /**
   * Evaluates nominal assignability to a target enum type.
   *
   * @param other target type to test assignment against
   * @return {@code true} if nominal enum names match
   */
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
