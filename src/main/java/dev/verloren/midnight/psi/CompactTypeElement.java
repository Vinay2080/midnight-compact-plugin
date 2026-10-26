package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * Unified interface for any PSI element capable of providing a semantic {@link CompactType}.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Implemented by both declaration nodes (via {@link CompactNamedElement}) and explicit type syntax
 * nodes (such as {@link CompactBuiltinTypeImpl} and {@link CompactTypeReferenceImpl}).
 * Acts as the bridge between syntactic type annotations and the plugin's semantic type system:
 * <ul>
 *   <li><b>Syntactic Type Annotations:</b> Allows type references (e.g. {@code Uint<32>}, {@code Vector<2, Field>})
 *       to be evaluated into resolved {@link CompactType} model instances.</li>
 *   <li><b>Declaration Signatures:</b> Allows named elements (variables, fields, functions, circuits)
 *       to expose their declared or inferred type for type checking and hover documentation.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core type analysis routines:
 * <ul>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} during type unification, signature matching,
 *       and assignment verification.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactTypeMismatchInspection} during code validation.</li>
 *   <li>{@link dev.verloren.midnight.documentation.CompactDocumentationProvider} when rendering type tooltips.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only semantic query; never acquires write locks.</li>
 * </ul>
 *
 * @see CompactType
 * @see CompactNamedElement
 * @see CompactExpression
 * @see dev.verloren.midnight.type.CompactTypeInferenceUtil
 */
public interface CompactTypeElement extends PsiElement {

  /**
   * Computes and returns the semantic {@link CompactType} associated with this element.
   *
   * @return the resolved or declared semantic type; never {@code null}
   */
  @NotNull CompactType getType();
}
