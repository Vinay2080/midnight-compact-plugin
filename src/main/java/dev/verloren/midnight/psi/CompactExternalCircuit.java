package dev.verloren.midnight.psi;

/**
 * Represents an abstract circuit signature declared within a contract interface ({@link CompactExternalContractDeclaration}).
 *
 * <p>Example:
 * <pre>
 * export contract Token {
 *   circuit transfer(to: Address, amount: Uint<64>): [];
 * }
 * </pre>
 * </p>
 */
public interface CompactExternalCircuit extends CompactNamedElement {

  /**
   * Returns {@code true} if this external circuit is declared with the {@code pure} keyword.
   */
  boolean isPure();
}
