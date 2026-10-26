package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PSI element interface representing a circuit function definition in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents a zero-knowledge circuit function (e.g. {@code export circuit transfer(to: Address, amount: Uint<64>): Void { ... }}).
 * Serves as the primary executable unit in Compact smart contracts:
 * <ul>
 *   <li><b>Callable Signature:</b> Exposes {@link #getParameters()} for parameter lists and pattern bindings,
 *       and {@link #getReturnTypeElement()} for return type annotations.</li>
 *   <li><b>Executable Body:</b> Exposes {@link #getBody()} returning the enclosing {@link CompactBlock} AST node.</li>
 *   <li><b>Circuit Modifiers:</b> Provides {@link #isPure()} to check if marked with the {@code pure} keyword
 *       (disallowing state mutations) and {@link #isExported()} to check public export visibility.</li>
 *   <li><b>Navigation &amp; Symbol Search:</b> Implements {@link CompactNamedElement}, rendering a Method icon
 *       in Structure View and "Go to Symbol".</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil#resolveValue} when resolving circuit invocation calls.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactIdentifierCompletionProvider} when suggesting callable circuits.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitCircuitDefinition(CompactCircuitDefinition)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getParameters()}, {@link #getBody()}, {@link #getReturnTypeElement()},
 *       or {@link #getType()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactCircuitDefinitionImpl
 * @see CompactNamedElement
 * @see CompactExternalCircuit
 */
public interface CompactCircuitDefinition extends CompactNamedElement {

  /**
   * Retrieves all declared parameters and pattern-bound arguments of this circuit.
   *
   * @return list of parameter and pattern elements; never {@code null}
   */
  @NotNull List<CompactNamedElement> getParameters();

  /**
   * Retrieves the executable code block body of this circuit.
   *
   * @return the {@link CompactBlock} body, or {@code null} if body is absent or unparseable
   */
  @Nullable CompactBlock getBody();

  /**
   * Retrieves the explicit return type syntax element of this circuit.
   *
   * @return the {@link CompactTypeElement} specifying the return type, or {@code null} if omitted
   */
  @Nullable CompactTypeElement getReturnTypeElement();

  /**
   * Checks whether this circuit is declared with the {@code pure} keyword.
   *
   * @return {@code true} if marked pure; {@code false} otherwise
   */
  boolean isPure();

  /**
   * Checks whether this circuit is marked with the {@code export} keyword.
   *
   * @return {@code true} if exported; {@code false} otherwise
   */
  boolean isExported();
}

