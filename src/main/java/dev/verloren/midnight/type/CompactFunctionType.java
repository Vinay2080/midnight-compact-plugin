package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Semantic type representing a circuit, witness, or function signature type.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Encapsulates callable signatures across the Compact language model:
 * <ul>
 *   <li><b>Signature Display:</b> Formats parameter list and return type for parameter info tooltips
 *       and code completion lookup item tail text.</li>
 *   <li><b>Subtyping & Variance:</b> Implements formal subtyping where parameter positions are
 *       <i>contravariant</i> and return positions are <i>covariant</i>.</li>
 *   <li><b>Substitution:</b> Transformed by {@link CompactTypeSubstitutor#substitute} when generic
 *       functions or circuits are instantiated.</li>
 * </ul>
 * </p>
 *
 * <h3>Subtyping Variance Visual Diagram</h3>
 * <pre>
 *   (TargetParam)  ---&gt; [isAssignableTo] ---&gt; (SourceParam) [Contravariant]
 *   (SourceReturn) ---&gt; [isAssignableTo] ---&gt; (TargetReturn) [Covariant]
 * </pre>
 *
 * @see dev.verloren.midnight.type.CompactType
 * @see dev.verloren.midnight.type.CompactTypeSubstitutor
 */
public record CompactFunctionType(
    @NotNull List<CompactType> parameterTypes,
    @NotNull CompactType returnType
) implements CompactType {

  /**
   * Generates formatted function signature string (e.g. {@code "(Field, Uint<32>) -> Boolean"}).
   */
  @Override
  public @NotNull String name() {
    String params = parameterTypes.stream()
        .map(CompactType::name)
        .collect(Collectors.joining(", "));
    return "(" + params + ") -> " + returnType.name();
  }

  /**
   * Evaluates assignability adhering to function subtyping principles.
   *
   * @param other target function type
   * @return {@code true} if parameters are contravariant and the return type is covariant
   */
  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    if (this.equals(other)) {
      return true;
    }
    if (other instanceof CompactFunctionType(List<CompactType> types, CompactType type)) {
      if (parameterTypes.size() != types.size()) {
        return false;
      }
      // Step 1: Contravariant parameter checking (target parameter must be assignable to source parameter)
      for (int i = 0; i < parameterTypes.size(); i++) {
        if (!types.get(i).isAssignableTo(parameterTypes.get(i))) {
          return false;
        }
      }
      // Step 2: Covariant return type checking (source return type must be assignable to target return type)
      return returnType.isAssignableTo(type);
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactFunctionType that = (CompactFunctionType) o;
    return Objects.equals(parameterTypes, that.parameterTypes) && Objects.equals(returnType, that.returnType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameterTypes, returnType);
  }

  @Override
  public @NonNull String toString() {
    return name();
  }
}
