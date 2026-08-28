package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CompactCircuitDefinition extends CompactNamedElement {
  @NotNull List<CompactNamedElement> getParameters();
  @Nullable CompactBlock getBody();
  @Nullable CompactTypeElement getReturnTypeElement();
  boolean isPure();
  boolean isExported();
}

