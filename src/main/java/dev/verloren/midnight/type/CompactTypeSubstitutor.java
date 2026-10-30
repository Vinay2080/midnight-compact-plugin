package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * First-class type substitution engine for substituting type parameters/variables
 * across the sealed {@link CompactType} hierarchy.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Performs deep recursive substitution of generic type variables:
 * <ul>
 *   <li><b>Instantiation:</b> When a generic struct, circuit, or function is instantiated with concrete
 *       type arguments (e.g. {@code Vector<32, Field>}), {@link CompactTypeInferenceUtil#buildSubstitutor}
 *       constructs a substitutor mapping {@code T -> Field}.</li>
 *   <li><b>Deep Traversal:</b> Recursively walks parameterized types, struct fields, and function signatures,
 *       replacing matching {@link CompactTypeVariable} instances while preserving immutable semantics.</li>
 *   <li><b>Pattern Matching Switch:</b> Leverages modern Java pattern matching switch expressions
 *       over the sealed {@link CompactType} interface.</li>
 * </ul>
 * </p>
 *
 * <h3>Hash Prefix Normalization</h3>
 * <p>
 * To ensure seamless substitution regardless of whether type variables were declared with or without
 * the {@code #} prefix (e.g. {@code #T} vs {@code T}), the constructor automatically indexes both forms
 * in its internal substitution map.
 * </p>
 *
 * @see dev.verloren.midnight.type.CompactType
 * @see dev.verloren.midnight.type.CompactTypeVariable
 * @see dev.verloren.midnight.type.CompactTypeInferenceUtil
 */
public final class CompactTypeSubstitutor {

  /**
   * Pre-allocated empty singleton substitutor performing no modifications.
   */
  public static final CompactTypeSubstitutor EMPTY = new CompactTypeSubstitutor(Map.of());

  /**
   * Immutable map of type variable names (both with and without '#' prefix) to replacement types.
   */
  private final Map<String, CompactType> substitutions;

  /**
   * Constructs a substitutor with dual-indexed variable names for prefix tolerance.
   *
   * @param substitutions raw mapping of variable names to replacement types
   */
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

  /**
   * Returns the shared empty substitutor singleton.
   *
   * @return empty {@link CompactTypeSubstitutor}
   */
  public static @NotNull CompactTypeSubstitutor empty() {
    return EMPTY;
  }

  /**
   * Factory method creating a substitutor, returning {@link #EMPTY} if the map is empty.
   *
   * @param map mapping of variable names to replacement types
   * @return active or empty substitutor
   */
  public static @NotNull CompactTypeSubstitutor create(@NotNull Map<String, CompactType> map) {
    return map.isEmpty() ? EMPTY : new CompactTypeSubstitutor(map);
  }

  /**
   * Checks whether this substitutor contains no substitution mappings.
   *
   * @return {@code true} if empty
   */
  public boolean isEmpty() {
    return substitutions.isEmpty();
  }

  /**
   * Substitutes an individual type variable, checking both exact and normalized clean names.
   *
   * @param variable the type variable to resolve
   * @return the mapped replacement type, or {@code variable} if unmapped
   */
  public @NotNull CompactType substitute(@NotNull CompactTypeVariable variable) {
    CompactType direct = substitutions.get(variable.name());
    if (direct != null) {
      return direct;
    }
    CompactType clean = substitutions.get(variable.cleanName());
    return clean != null ? clean : variable;
  }

  /**
   * Recursively substitutes type variables across any {@link CompactType} instance
   * using a modern Java pattern matching switch expressions.
   *
   * @param type source type containing potential type variables
   * @return transformed type with variables replaced
   */
  public @NotNull CompactType substitute(@NotNull CompactType type) {
    if (substitutions.isEmpty()) {
      return type;
    }
    return switch (type) {
      case CompactTypeVariable v -> substitute(v);
      case CompactParameterizedType param -> substituteParameterizedType(param);
      case CompactStructType struct -> substituteStructType(struct);
      case CompactFunctionType func -> substituteFunctionType(func);
      case CompactPrimitiveType p -> p;
      case CompactUintType u -> u;
      case CompactNumericLiteralType n -> n;
      case CompactEnumType e -> e;
    };
  }

  /**
   * Deeply substitutes type arguments inside a parameterized type.
   */
  private @NotNull CompactType substituteParameterizedType(@NotNull CompactParameterizedType param) {
    List<CompactType> newArgs = param.typeArguments().stream()
            .map(this::substitute)
            .toList();
    return new CompactParameterizedType(param.rawName(), newArgs);
  }

  /**
   * Deeply substitutes field types inside a struct type.
   */
  private @NotNull CompactType substituteStructType(@NotNull CompactStructType struct) {
    Map<String, CompactType> newFields = new LinkedHashMap<>();
    for (Map.Entry<String, CompactType> entry : struct.fields().entrySet()) {
      newFields.put(entry.getKey(), substitute(entry.getValue()));
    }
    return new CompactStructType(struct.name(), newFields);
  }

  /**
   * Deeply substitutes parameter and return types inside a function type.
   */
  private @NotNull CompactType substituteFunctionType(@NotNull CompactFunctionType func) {
    List<CompactType> newParams = func.parameterTypes().stream()
            .map(this::substitute)
            .toList();
    CompactType newReturn = substitute(func.returnType());
    return new CompactFunctionType(newParams, newReturn);
  }

  /**
   * Returns the unmodifiable underlying substitution mappings.
   *
   * @return map of variable names to replacement types
   */
  public @NotNull Map<String, CompactType> getMappings() {
    return substitutions;
  }

  /**
   * Creates a fluent builder for incrementally constructing a {@link CompactTypeSubstitutor}.
   *
   * @return new {@link Builder}
   */
  public static @NotNull Builder builder() {
    return new Builder();
  }

  /**
   * Fluent builder for assembling {@link CompactTypeSubstitutor} instances.
   */
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
