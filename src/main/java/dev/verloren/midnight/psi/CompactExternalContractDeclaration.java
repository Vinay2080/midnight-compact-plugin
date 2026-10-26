package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * PSI element interface representing an external contract declaration (interface) in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents an external contract signature declaration (e.g., {@code contract Token { ... }}).
 * Acts as an interface or specification for contracts that can be called or implemented:
 * <ul>
 *   <li><b>Contract Signature:</b> Declares circuit signatures accessible across contract boundaries
 *       via {@link #getCircuits()}.</li>
 *   <li><b>Implementation Target:</b> Target of {@link CompactContractImplementsDeclaration#resolveInterface()}
 *       when a contract implements this contract specification.</li>
 *   <li><b>Navigation &amp; Renaming:</b> Implements {@link CompactNamedElement}, enabling standard
 *       IntelliJ renaming, usage searching, and navigation.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} during type resolution and contract implementation verification.</li>
 *   <li>{@link CompactContractImplementsDeclaration#resolveInterface()} when tracing interface implementations.</li>
 *   <li>{@link CompactVisitor#visitExternalContractDeclaration(CompactExternalContractDeclaration)} during inspections.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getCircuits()} or {@link #getName()} strictly requires an active
 *       platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Mutation requires a Write Action inside a {@link com.intellij.openapi.command.WriteCommandAction} on the EDT.</li>
 * </ul>
 *
 * @see CompactNamedElement
 * @see CompactExternalCircuit
 * @see CompactContractImplementsDeclaration
 */
public interface CompactExternalContractDeclaration extends CompactNamedElement {

  /**
   * Returns all circuit signatures declared directly inside this external contract interface.
   *
   * @return list of external circuit signatures; never {@code null}
   */
  @NotNull List<CompactExternalCircuit> getCircuits();
}
