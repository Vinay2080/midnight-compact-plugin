package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PSI element interface representing a contract constructor declaration in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents a contract initialization constructor (e.g. {@code constructor(initialSupply: Uint<64>) { ... }}).
 * Initializes contract ledger state when the contract is deployed:
 * <ul>
 *   <li><b>Parameter Collection:</b> Exposes {@link #getParameters()} for constructor argument declarations.</li>
 *   <li><b>Body Block:</b> Exposes {@link #getBody()} returning the initializing {@link CompactBlock}.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatched to {@link CompactVisitor#visitConstructorDeclaration(CompactConstructorDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} when analysing contract initialization and parameter scopes.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitConstructorDeclaration(CompactConstructorDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getParameters()} or {@link #getBody()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Structural modifications require a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactConstructorDeclarationImpl
 * @see CompactCircuitDefinition
 */
public interface CompactConstructorDeclaration extends PsiElement {

  /**
   * Retrieves all parameters and pattern-bound arguments of this constructor.
   *
   * @return list of constructor parameter named elements; never {@code null}
   */
  @NotNull List<CompactNamedElement> getParameters();

  /**
   * Retrieves the body code block of this constructor.
   *
   * @return the {@link CompactBlock} body, or {@code null} if omitted
   */
  @Nullable CompactBlock getBody();
}

