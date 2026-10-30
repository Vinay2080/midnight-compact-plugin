package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;

/**
 * Sealed base semantic type interface in the Compact language plugin's type system.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * This interface forms the core algebraic type model for Compact smart contract verification:
 * <ul>
 *   <li><b>Type Inference & Expression Typing:</b> Returned by {@link dev.verloren.midnight.psi.CompactExpression#getType()}
 *       to compute static types for identifiers, binary expressions, call invocations, and struct literals.</li>
 *   <li><b>Inspections & Semantic Analysis:</b> Queried by {@code CompactTypeMismatchInspection} and annotators
 *       to verify type compatibility, detect illegal assignments, and flag operand mismatches before compilation.</li>
 *   <li><b>Code Completion & Smart Casting:</b> Used by completion providers
 *       ({@link dev.verloren.midnight.completion.providers.CompactMemberAccessCompletionProvider},
 *       {@link dev.verloren.midnight.completion.providers.CompactStructLiteralCompletionProvider})
 *       to filter candidates based on expected parameter types and member access origins.</li>
 *   <li><b>Reference Resolution:</b> Referenced by {@link dev.verloren.midnight.reference.CompactStructFieldReference}
 *       to determine the base struct definition when resolving member field accessors.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading & Immutability Contract</h3>
 * <ul>
 *   <li><b>Thread Safety:</b> All implementations are immutable records or stateless value classes.
 *       Instances can be freely shared across threads without locks.</li>
 *   <li><b>No PSI Retention:</b> Implementations represent abstract type models and DO NOT retain
 *       references to mutable {@link com.intellij.psi.PsiElement} or {@link com.intellij.openapi.project.Project}
 *       instances, preventing memory leaks during long-running IDE sessions.</li>
 *   <li><b>Read Action Requirement:</b> While the type objects themselves are pure memory structures,
 *       inferring them from PSI expressions via {@link dev.verloren.midnight.psi.CompactExpression#getType()}
 *       requires an active IntelliJ Read Action ({@code @RequiresReadLock}).</li>
 * </ul>
 *
 * <h3>Sealed Algebraic Hierarchy Visual Diagram</h3>
 * <pre>
 *                       CompactType (sealed interface)
 *                                     │
 *   ┌───────────────┬─────────────────┼────────────────┬───────────────┐
 *   ▼               ▼                 ▼                ▼               ▼
 * [Primitive]     [Uint]        [NumericLiteral] [Parameterized]    [Struct]
 * Boolean, Field  Uint&lt;N&gt;           0x1F, 42         Vector&lt;32,T&gt;     User struct
 * Bytes, Opaque   Uint&lt;min..max&gt;
 *                   │
 *   ┌───────────────┴─────────────────┐
 *   ▼                                 ▼
 * [Enum]                        [Function]            [TypeVariable]
 * User enum                     (A, B) -&gt; R           T, #T, TVal
 * </pre>
 *
 * <h3>Type System Invariants & Subtyping</h3>
 * <ul>
 *   <li><b>Structural vs Nominal:</b> Structs and Enums are primarily nominal, while unsigned integers
 *       support interval/range-based subtyping ({@link CompactUintType#isAssignableTo(CompactType)}).</li>
 *   <li><b>Function Subtyping:</b> {@link CompactFunctionType} adheres to standard subtyping:
 *       parameter positions are <i>contravariant</i>, while return positions are <i>covariant</i>.</li>
 *   <li><b>Unbound Generics:</b> {@link CompactTypeVariable} represents an uninstantiated type parameter
 *       that unifies with any concrete type during type inference.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.type.CompactPrimitiveType
 * @see dev.verloren.midnight.type.CompactUintType
 * @see dev.verloren.midnight.type.CompactNumericLiteralType
 * @see dev.verloren.midnight.type.CompactParameterizedType
 * @see dev.verloren.midnight.type.CompactStructType
 * @see dev.verloren.midnight.type.CompactEnumType
 * @see dev.verloren.midnight.type.CompactFunctionType
 * @see dev.verloren.midnight.type.CompactTypeVariable
 * @see dev.verloren.midnight.type.CompactTypeSubstitutor
 * @see dev.verloren.midnight.type.CompactTypeInferenceUtil
 */
public sealed interface CompactType permits
    CompactPrimitiveType,
    CompactUintType,
    CompactNumericLiteralType,
    CompactParameterizedType,
    CompactStructType,
    CompactEnumType,
    CompactFunctionType,
    CompactTypeVariable {

  /**
   * Returns the canonical human-readable textual representation of this type (e.g., {@code "Field"},
   * {@code "Uint<32>"}, {@code "Vector<32, Field>"}, or {@code "(Field) -> Boolean"}).
   *
   * @return non-null string name of the type
   */
  @NotNull String name();

  /**
   * Determines whether this type is assignable to (a subtype of) another target type.
   *
   * <p>Default implementation requires exact equality, but specialized subtypes override this
   * to implement widening, range containment, covariance/contravariance, and generic unification.</p>
   *
   * @param other target type to test assignment compatibility against
   * @return {@code true} if an expression of this type can be safely used in a context expecting {@code other}
   */
  default boolean isAssignableTo(@NotNull CompactType other) {
    return this.equals(other);
  }

  /**
   * Replaces all occurrences of type variables in this type with concrete types
   * according to the provided {@link CompactTypeSubstitutor}.
   *
   * @param substitutor mapping of type variables to replacement types
   * @return substituted type, or {@code this} if no substitutions were made
   */
  default @NotNull CompactType substitute(@NotNull CompactTypeSubstitutor substitutor) {
    return substitutor.substitute(this);
  }
}
