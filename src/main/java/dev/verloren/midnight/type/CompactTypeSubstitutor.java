package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * First-class type substitution engine for substituting type parameters/variables
 * across the sealed {@link CompactType} hierarchy.
 */
public final class CompactTypeSubstitutor {

  public static final CompactTypeSubstitutor EMPTY = new CompactTypeSubstitutor(Map.of());

  private final Map<String, CompactType> substitutions;

  public CompactTypeSubstitutor(@NotNull Map<String, CompactType> substitutions) {
    Map<String, CompactType> map = new LinkedHashMap<>();
    for (Map.Entry<String, CompactType> entry : substitutions.entrySet()) {
      String key = entry.getKey();
      map.put(key, entry.getValue());
      if (key.startsWith("#")) {
        map.put(key.substring(1), entry.getValue());
      } else {
        map.put("#" + key, entry.getValue());
      }
    }
    this.substitutions = Collections.unmodifiableMap(map);
  }

  public static @NotNull CompactTypeSubstitutor empty() {
    return EMPTY;
  }

  public static @NotNull CompactTypeSubstitutor create(@NotNull Map<String, CompactType> map) {
    return map.isEmpty() ? EMPTY : new CompactTypeSubstitutor(map);
  }

  public boolean isEmpty() {
    return substitutions.isEmpty();
  }

  public @Nullable CompactType substitute(@NotNull CompactTypeVariable variable) {
    CompactType direct = substitutions.get(variable.name());
    if (direct != null) {
      return direct;
    }
    return substitutions.get(variable.cleanName());
  }

  /**
   * Recursively substitutes type variables across any {@link CompactType} instance
   * using modern Java 25 pattern matching switch expressions.
   */
  public @NotNull CompactType substitute(@NotNull CompactType type) {
    if (substitutions.isEmpty()) {
      return type;
    }
    return switch (type) {
      case CompactTypeVariable v -> {
        CompactType sub = substitute(v);
        yield sub != null ? sub : v;
      }
      case CompactParameterizedType param -> substituteParameterizedType(param);
      case CompactStructType struct -> substituteStructType(struct);
      case CompactFunctionType func -> substituteFunctionType(func);
      case CompactPrimitiveType p -> p;
      case CompactUintType u -> u;
      case CompactNumericLiteralType n -> n;
      case CompactEnumType e -> e;
    };
  }

  private @NotNull CompactType substituteParameterizedType(@NotNull CompactParameterizedType param) {
    List<CompactType> newArgs = param.typeArguments().stream()
        .map(this::substitute)
        .toList();
    return new CompactParameterizedType(param.rawName(), newArgs);
  }

  private @NotNull CompactType substituteStructType(@NotNull CompactStructType struct) {
    Map<String, CompactType> newFields = new LinkedHashMap<>();
    for (Map.Entry<String, CompactType> entry : struct.fields().entrySet()) {
      newFields.put(entry.getKey(), substitute(entry.getValue()));
    }
    return new CompactStructType(struct.name(), newFields);
  }

  private @NotNull CompactType substituteFunctionType(@NotNull CompactFunctionType func) {
    List<CompactType> newParams = func.parameterTypes().stream()
        .map(this::substitute)
        .toList();
    CompactType newReturn = substitute(func.returnType());
    return new CompactFunctionType(newParams, newReturn);
  }

  public @NotNull Map<String, CompactType> getMappings() {
    return substitutions;
  }

  public static @NotNull Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final Map<String, CompactType> map = new LinkedHashMap<>();

    public @NotNull Builder put(@NotNull String variableName, @NotNull CompactType type) {
      map.put(variableName, type);
      return this;
    }

    public @NotNull Builder put(@NotNull CompactTypeVariable variable, @NotNull CompactType type) {
      map.put(variable.name(), type);
      return this;
    }

    public @NotNull CompactTypeSubstitutor build() {
      return CompactTypeSubstitutor.create(map);
    }
  }
}
