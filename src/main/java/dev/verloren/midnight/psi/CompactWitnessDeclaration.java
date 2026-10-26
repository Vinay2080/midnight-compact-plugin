package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PSI element interface representing a witness function declaration in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents a witness declaration (e.g. {@code witness getSecretKey(id: Field): Bytes<32>;}).
 * In zero-knowledge architecture, witness functions supply private, off-chain data directly to circuits:
 * <ul>
 *   <li><b>Parameter Collection:</b> Exposes {@link #getParameters()} for declared input parameters.</li>
 *   <li><b>Return Type Specification:</b> Exposes {@link #getReturnTypeElement()} for the private witness data type.</li>
 *   <li><b>Export Visibility:</b> Exposes {@link #isExported()} indicating cross-module accessibility.</li>
 *   <li><b>Navigation &amp; Symbol Search:</b> Implements {@link CompactNamedElement}, rendering a Function icon in Structure View.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil#resolveValue} when resolving witness invocations.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactIdentifierCompletionProvider} when suggesting witness functions.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitWitnessDeclaration(CompactWitnessDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getParameters()}, {@link #getReturnTypeElement()}, or {@link #getType()}
 *       requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactWitnessDeclarationImpl
 * @see CompactCircuitDefinition
 * @see CompactNamedElement
 */
public interface CompactWitnessDeclaration extends CompactNamedElement {

  /**
   * Retrieves all parameters and pattern-bound arguments of this witness declaration.
   *
   * @return list of parameter and pattern elements; never {@code null}
   */
  @NotNull List<CompactNamedElement> getParameters();

  /**
   * Retrieves the explicit return type syntax element of this witness declaration.
   *
   * @return the {@link CompactTypeElement} specifying the return type, or {@code null} if omitted
   */
  @Nullable CompactTypeElement getReturnTypeElement();

  /**
   * Checks whether this witness is marked with the {@code export} keyword.
   *
   * @return {@code true} if exported; {@code false} otherwise
   */
  boolean isExported();
}

