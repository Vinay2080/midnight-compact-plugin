package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Semantic type representing a generic type parameter or type variable in Compact
 * (e.g. {@code T}, {@code #T}, {@code TVal}, {@code TErr}, {@code A}, {@code B}).
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Represents unbound or polymorphic type parameters:
 * <ul>
 *   <li><b>Unification:</b> During type inference, unbound type variables can unify with any concrete
 *       type, returning {@code true} from {@link #isAssignableTo(CompactType)}.</li>
 *   <li><b>Prefix Normalization:</b> Compact syntax permits type variables with a leading hash prefix
 *       (e.g., {@code #T}). {@link #cleanName()} strips the {@code #} prefix so that {@code #T} and
 *       {@code T} are considered identical during resolution and substitution.</li>
 *   <li><b>Substitution:</b> Replaced with concrete types by {@link CompactTypeSubstitutor#substitute(CompactTypeVariable)}.</li>
 * </ul>
 * </p>
 *
 * @see dev.verloren.midnight.type.CompactType
 * @see dev.verloren.midnight.type.CompactTypeSubstitutor
 * @see dev.verloren.midnight.type.CompactTypeInferenceUtil#isTypeVariable(String)
 */
public record CompactTypeVariable(
    @NotNull String name
) implements CompactType {

  /**
   * Compact constructor validating that the variable name is non-blank.
   */
  public CompactTypeVariable {
    if (name.isBlank()) {
      throw new IllegalArgumentException("Type variable name cannot be blank");
    }
  }

  /**
   * Unbound type variables can unify with any type.
   *
   * @param other target type
   * @return unconditionally {@code true}
   */
  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    // An unbound type variable can unify with any type or another type variable
    return true;
  }

  /**
   * Returns the variable name stripped of any leading {@code #} prefix.
   *
   * @return normalized variable name (e.g. {@code "T"} for {@code "#T"})
   */
  public @NotNull String cleanName() {
    return name.startsWith("#") ? name.substring(1) : name;
  }

  /**
   * Equality considers normalized names so that {@code #T} equals {@code T}.
   */
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
