package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Semantic type representing a generic or parameterized type in Compact
 * (e.g. {@code Either<Field, Boolean>}, {@code Vector<32, Uint<8>>}, {@code Result<TVal, TErr>}).
 */
public record CompactParameterizedType(
    @NotNull String rawName,
    @NotNull List<CompactType> typeArguments
) implements CompactType {

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
