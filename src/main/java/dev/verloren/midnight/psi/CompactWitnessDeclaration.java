package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CompactWitnessDeclaration extends CompactNamedElement {
  @NotNull List<CompactNamedElement> getParameters();
  @Nullable CompactTypeElement getReturnTypeElement();
}
