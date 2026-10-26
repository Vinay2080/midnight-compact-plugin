package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

/**
 * PSI element interface representing a contract implementation clause ({@code implements InterfaceName}).
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents the {@code implements ...} clause attached to a Compact contract definition.
 * Serves as the link connecting a concrete contract to its declared external contract interface:
 * <ul>
 *   <li><b>Type Reference:</b> Holds the {@link CompactTypeReferenceImpl} referencing the implemented contract interface.</li>
 *   <li><b>Interface Resolution:</b> Provides {@link #resolveInterface()} to resolve the referenced type directly
 *       to its corresponding {@link CompactExternalContractDeclaration}.</li>
 *   <li><b>Contract Conformance Inspection:</b> Used by code inspections to verify that all circuits
 *       declared in the interface are properly implemented by the contract.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} when checking contract inheritance and interface conformance.</li>
 *   <li>{@link dev.verloren.midnight.navigation.CompactGotoDeclarationHandler} when navigating from the implements clause.</li>
 *   <li>{@link CompactVisitor#visitContractImplementsDeclaration(CompactContractImplementsDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getTypeReference()}, {@link #getInterfaceName()}, or {@link #resolveInterface()}
 *       strictly requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only interface; modifications occur via parent AST replacements.</li>
 * </ul>
 *
 * @see CompactExternalContractDeclaration
 * @see CompactTypeReferenceImpl
 */
public interface CompactContractImplementsDeclaration extends PsiElement {

  /**
   * Returns the type reference child specifying the implemented interface.
   *
   * @return the {@link CompactTypeReferenceImpl} child, or {@code null} if incomplete syntax
   */
  @Nullable CompactTypeReferenceImpl getTypeReference();

  /**
   * Returns the textual identifier of the implemented contract interface.
   *
   * @return interface name string, or {@code null} if unparseable
   */
  @SuppressWarnings("unused")
  @Nullable String getInterfaceName();

  /**
   * Resolves this implements declaration to the corresponding {@link CompactExternalContractDeclaration}.
   *
   * @return the resolved {@link CompactExternalContractDeclaration}, or {@code null} if unresolved
   */
  @Nullable CompactExternalContractDeclaration resolveInterface();
}
