package dev.verloren.midnight.psi;

import org.jetbrains.annotations.Nullable;

public interface CompactTypeDefinition extends CompactNamedElement {
  @Nullable CompactTypeElement getTargetTypeElement();
}