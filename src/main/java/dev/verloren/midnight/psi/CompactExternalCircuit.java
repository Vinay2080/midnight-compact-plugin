package dev.verloren.midnight.psi;

/**
 * PSI element interface representing an external circuit signature declared within a contract interface.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents an abstract circuit prototype declared inside an external contract specification:
 * <pre>
 * export contract Token {
 *   circuit transfer(to: Address, amount: Uint&lt;64&gt;): [];
 * }
 * </pre>
 * Fulfills the role of an abstract method declaration:
 * <ul>
 *   <li><b>Interface Specification:</b> Dictates circuit names and purity constraints that implementing contracts must fulfill.</li>
 *   <li><b>Purity Checking:</b> Exposes {@link #isPure()} to check if marked with {@code pure}.</li>
 *   <li><b>Navigation &amp; Symbol Search:</b> Implements {@link CompactNamedElement}, enabling navigation and refactoring.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link CompactExternalContractDeclaration#getCircuits()} when discovering contract interface methods.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitExternalCircuit(CompactExternalCircuit)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #isPure()} or {@link #getName()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactExternalContractDeclaration
 * @see CompactExternalCircuitImpl
 * @see CompactCircuitDefinition
 */
public interface CompactExternalCircuit extends CompactNamedElement {

  /**
   * Checks whether this external circuit signature is declared with the {@code pure} modifier.
   *
   * @return {@code true} if declared pure; {@code false} otherwise
   */
  boolean isPure();
}
