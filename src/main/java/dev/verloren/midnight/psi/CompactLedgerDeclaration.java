package dev.verloren.midnight.psi;

import org.jetbrains.annotations.Nullable;

/**
 * PSI element interface representing a contract ledger state variable declaration in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents a state variable stored on-chain in the contract's ledger state
 * (e.g., {@code export ledger balance: Cell<Uint<64>>;}).
 * Manages contract persistent storage declarations:
 * <ul>
 *   <li><b>State Type:</b> Exposes {@link #getTypeElement()} for the declared storage container type (e.g. {@code Cell<T>}, {@code Map<K, V>}).</li>
 *   <li><b>Sealed Modifiers:</b> Exposes {@link #isSealed()} to check if marked with the {@code sealed} keyword
 *       (preventing state tampering or mutation).</li>
 *   <li><b>Export Visibility:</b> Exposes {@link #isExported()} indicating cross-contract read visibility.</li>
 *   <li><b>Navigation &amp; Symbol Search:</b> Implements {@link CompactNamedElement}, rendering a Field icon in Structure View.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil#resolveValue} when resolving state variable reads/writes.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactValueCompletionProvider} when suggesting ledger state fields.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitLedgerDeclaration(CompactLedgerDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getTypeElement()}, {@link #isSealed()}, or {@link #getType()}
 *       requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactLedgerDeclarationImpl
 * @see CompactNamedElement
 */
public interface CompactLedgerDeclaration extends CompactNamedElement {

  /**
   * Retrieves the syntax element specifying this ledger variable's type.
   *
   * @return the {@link CompactTypeElement}, or {@code null} if omitted
   */
  @Nullable CompactTypeElement getTypeElement();

  /**
   * Checks whether this ledger state variable is marked with the {@code sealed} modifier.
   *
   * @return {@code true} if sealed; {@code false} otherwise
   */
  boolean isSealed();

  /**
   * Checks whether this ledger variable is marked with the {@code export} keyword.
   *
   * @return {@code true} if exported; {@code false} otherwise
   */
  boolean isExported();
}
