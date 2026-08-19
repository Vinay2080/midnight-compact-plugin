package dev.verloren.midnight.psi;

import org.jetbrains.annotations.Nullable;

public interface CompactLedgerDeclaration extends CompactNamedElement {
  @Nullable CompactTypeElement getTypeElement();
}