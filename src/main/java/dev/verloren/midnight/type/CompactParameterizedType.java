package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Semantic type representing a generic or parameterized type in Compact
 * (e.g. {@code Either<Field, Boolean>}, {@code Vector<32, Uint<8>>}, {@code Result<TVal, TErr>}).
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Models compound generic type signatures containing type arguments:
 * <ul>
 *   <li><b>Type Construction:</b> Created by {@link CompactTypeInferenceUtil#parseType(String)}
 *       when parsing signatures containing angle brackets ({@code <...>}).</li>
 *   <li><b>Code Completion:</b> Queried by {@link dev.verloren.midnight.completion.providers.CompactStructLiteralCompletionProvider}
 *       and {@link dev.verloren.midnight.completion.CompactEitherInsertHandler} to inspect generic type parameters
 *       and insert matching template expressions.</li>
 *   <li><b>Substitution:</b> Transformed during type instantiation by {@link CompactTypeSubstitutor#substitute}.</li>
 * </ul>
 * </p>
 *
 * <h3>Subtyping & Invariance Rules</h3>
 * <ul>
 *   <li><b>Raw Name Equality:</b> Both parameterized types must share identical {@link #rawName()}.</li>
 *   <li><b>Argument Matching:</b> Each type argument must match. Unbound {@link CompactTypeVariable} instances
 *       in either operand unify successfully.</li>
 *   <li><b>Nominal Struct Compatibility:</b> Assignable to a {@link CompactStructType} matching {@link #rawName()}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.type.CompactType
 * @see dev.verloren.midnight.type.CompactTypeVariable
 * @see dev.verloren.midnight.type.CompactTypeSubstitutor
 */
public record CompactParameterizedType(
    @NotNull String rawName,
    @NotNull List<CompactType> typeArguments
) implements CompactType {

  /**
   * Generates formatted type signature including all type arguments (e.g. {@code "Vector<32, Field>"}).
   */
  @Override
  public @NotNull String name() {
    if (typeArguments.isEmpty()) {
      return rawName;
    }
    String args = typeArguments.stream()
        .map(CompactType::name)
        .collect(Collectors.joining(", "));
    return rawName + "<" + args + ">";
  }

  /**
   * Evaluates assignability against another target type.
   *
   * @param other target type to test assignment against
   * @return {@code true} if this parameterized type can be assigned to {@code other}
   */
  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    if (this.equals(other)) {
      return true;
    }
    if (other instanceof CompactTypeVariable) {
      return true;
    }
    if (other instanceof CompactParameterizedType otherParam) {
      if (!rawName.equals(otherParam.rawName()) || typeArguments.size() != otherParam.typeArguments().size()) {
        return false;
      }
      for (int i = 0; i < typeArguments.size(); i++) {
        CompactType a = typeArguments.get(i);
        CompactType b = otherParam.typeArguments().get(i);
        // Type variables unify with anything
        if (a instanceof CompactTypeVariable || b instanceof CompactTypeVariable) {
          continue;
        }
        if (!a.isAssignableTo(b)) {
          return false;
        }
      }
      return true;
    }
    if (other instanceof CompactStructType otherStruct) {
      return rawName.equals(otherStruct.name());
    }
    if (other instanceof CompactPrimitiveType(String otherName)) {
      return CompactTypeInferenceUtil.isGenericAssignable(name(), otherName);
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactParameterizedType that = (CompactParameterizedType) o;
    return Objects.equals(rawName, that.rawName) && Objects.equals(typeArguments, that.typeArguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawName, typeArguments);
  }

  @Override
  public @NonNull String toString() {
    return name();
  }
}
