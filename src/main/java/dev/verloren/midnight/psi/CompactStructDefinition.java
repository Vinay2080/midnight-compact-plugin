package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CompactStructDefinition extends CompactNamedElement {
  @NotNull List<CompactStructFieldImpl> getFields();
}