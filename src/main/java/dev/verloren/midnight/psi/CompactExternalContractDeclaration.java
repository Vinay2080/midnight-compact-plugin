package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;
import java.util.List;

public interface CompactExternalContractDeclaration extends CompactNamedElement {

  /**
   * Returns all circuit signatures declared directly inside this external contract interface.
   */
  @NotNull List<CompactExternalCircuit> getCircuits();
}
