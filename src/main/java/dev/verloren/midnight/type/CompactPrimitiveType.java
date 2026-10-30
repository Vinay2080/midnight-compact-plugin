package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Represents a primitive or nominal scalar type in Compact (e.g. {@code Boolean}, {@code Field}, {@code Bytes}).
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Forms the fundamental primitive building blocks of the Compact type system:
 * <ul>
 *   <li><b>Singleton Pre-allocation:</b> Standard builtins ({@link #BOOLEAN}, {@link #FIELD},
 *       {@link #BYTES}, {@link #OPAQUE}, {@link #UNKNOWN}) are pre-instantiated as public static
 *       constants to eliminate allocation overhead during typing and inspection passes.</li>
 *   <li><b>Binary & Unary Inference:</b> Returned directly by {@link CompactTypeInferenceUtil#inferBinaryExprType}
 *       and {@link CompactTypeInferenceUtil#inferUnaryExprType} for boolean logic and comparison operators.</li>
 *   <li><b>Parsing:</b> Instantiated via {@link #fromName(String)} during textual type parsing.</li>
 * </ul>
 * </p>
 *
 * <h3>Subtyping & Assignability Rules</h3>
 * <ul>
 *   <li><b>Uint Delegation:</b> If this primitive type represents a {@code Uint} variant (e.g., {@code "Uint<32>"}),
 *       it parses and delegates assignability to {@link CompactUintType#isAssignableTo(CompactType)}.</li>
 *   <li><b>Byte Prefix Compatibility:</b> {@code "Bytes"} is compatible with sized byte arrays like {@code "Bytes<32>"}.</li>
 *   <li><b>Type Variable Unification:</b> Assignable to any {@link CompactTypeVariable}.</li>
 *   <li><b>Generic Name Matching:</b> Delegates to {@link CompactTypeInferenceUtil#isGenericAssignable}
 *       when dealing with parameterized primitive aliases.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.type.CompactType
 * @see dev.verloren.midnight.type.CompactUintType
 * @see dev.verloren.midnight.type.CompactTypeInferenceUtil
 */
public record CompactPrimitiveType(String name) implements CompactType {

  /**
   * Pre-allocated singleton representing the boolean type ({@code Boolean}).
   */
  public static final CompactPrimitiveType BOOLEAN = new CompactPrimitiveType("Boolean");

  /**
   * Pre-allocated singleton representing the native ZK scalar field element type ({@code Field}).
   */
  public static final CompactPrimitiveType FIELD = new CompactPrimitiveType("Field");

  /**
   * Pre-allocated singleton representing the byte sequence type ({@code Bytes}).
   */
  public static final CompactPrimitiveType BYTES = new CompactPrimitiveType("Bytes");

  /**
   * Pre-allocated singleton representing opaque or uninterpreted host data ({@code Opaque}).
   */
  public static final CompactPrimitiveType OPAQUE = new CompactPrimitiveType("Opaque");

  /**
   * Pre-allocated singleton representing an unresolved, invalid, or placeholder type ({@code Unknown}).
   */
  public static final CompactPrimitiveType UNKNOWN = new CompactPrimitiveType("Unknown");

  /**
   * Resolves a canonical builtin type string to its corresponding pre-allocated singleton instance.
   *
   * @param name textual type name to match
   * @return matching singleton {@link CompactPrimitiveType}, or {@code null} if not a standard builtin
   */
  public static @Nullable CompactPrimitiveType fromName(@NotNull String name) {
    return switch (name) {
      case "Boolean" -> BOOLEAN;
      case "Field" -> FIELD;
      case "Bytes" -> BYTES;
      case "Opaque" -> OPAQUE;
      case "Unknown" -> UNKNOWN;
      default -> null;
    };
  }

  @Override
  public @NotNull String name() {
    return name;
  }

  /**
   * Evaluates assignability of this primitive or nominal type to another target type.
   *
   * @param other target type to test assignment compatibility against
   * @return {@code true} if assignment is valid under Compact subtyping rules
   */
  @Override
  public boolean isAssignableTo(@NotNull CompactType other) {
    // Step 1: Exact type equality
    if (this.equals(other)) return true;

    // Step 2: Delegate to Uint range checking if this type represents an unsigned integer
    if (name != null && name.startsWith("Uint")) {
      CompactUintType uint = CompactUintType.parse(name);
      if (uint != null) {
        return uint.isAssignableTo(other);
      }
    }

    // Step 3: Bytes and sized Bytes compatibility (e.g. Bytes assignable to/from Bytes<N>)
    if ("Bytes".equals(name) && other.name().startsWith("Bytes")) return true;
    if (name != null && name.startsWith("Bytes") && "Bytes".equals(other.name())) return true;

    // Step 4: Unbound type variables can unify with any primitive type
    if (other instanceof CompactTypeVariable) {
      return true;
    }

    // Step 5: Generic parameter assignability when raw type names match
    if (this.name != null) {
      String baseThis = CompactTypeInferenceUtil.getRawTypeName(this.name);
      String baseOther = CompactTypeInferenceUtil.getRawTypeName(other.name());
      if (baseThis.equals(baseOther) && (this.name.contains("<") || other.name().contains("<"))) {
        return CompactTypeInferenceUtil.isGenericAssignable(this.name, other.name());
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactPrimitiveType that = (CompactPrimitiveType) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public @NonNull String toString() {
    return name;
  }
}
