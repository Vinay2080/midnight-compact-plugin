package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Semantic type representing a circuit, witness, or function signature type.
 */
public record CompactFunctionType(
    @NotNull List<CompactType> parameterTypes,
    @NotNull CompactType returnType
) implements CompactType {

  @Override
  public @NotNull String name() {
    String params = parameterTypes.stream()
        .map(CompactType::name)
        .collect(Collectors.joining(", "));
    return "(" + params + ") -> " + returnType.name();
  }

  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    if (this.equals(other)) {
      return true;
    }
    if (other instanceof CompactFunctionType otherFn) {
      if (parameterTypes.size() != otherFn.parameterTypes().size()) {
        return false;
      }
      for (int i = 0; i < parameterTypes.size(); i++) {
        // Contravariant parameter checking
        if (!otherFn.parameterTypes().get(i).isAssignableTo(parameterTypes.get(i))) {
          return false;
        }
      }
      // Covariant return type checking
      return returnType.isAssignableTo(otherFn.returnType());
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
