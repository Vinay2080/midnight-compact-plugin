package dev.verloren.midnight.psi;

import org.jetbrains.annotations.Nullable;

/**
 * PSI element interface representing a type alias definition ({@code type Alias = TargetType;}) in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents a custom type alias declaration in Compact.
 * Acts as a named declaration and type target:
 * <ul>
 *   <li><b>Type Alias Resolution:</b> Binds a new type identifier to an existing builtin or composite type,
 *       exposed via {@link #getTargetTypeElement()}.</li>
 *   <li><b>Navigation &amp; Symbol Search:</b> Appears in "Go to Symbol" (Ctrl+Alt+Shift+N) with a type icon.</li>
 *   <li><b>Type Inference:</b> Evaluates to the target type via {@link #getType()}.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil#resolveType} during type reference resolution.</li>
 *   <li>{@link dev.verloren.midnight.type.CompactTypeInferenceUtil} when expanding type aliases.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitTypeDefinition(CompactTypeDefinition)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getTargetTypeElement()} or {@link #getType()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactTypeDefinitionImpl
 * @see CompactNamedElement
 * @see CompactTypeElement
 */
public interface CompactTypeDefinition extends CompactNamedElement {

  /**
   * Returns the RHS type element being aliased.
   *
   * @return target type element, or {@code null} if incomplete syntax
   */
  @Nullable CompactTypeElement getTargetTypeElement();
}