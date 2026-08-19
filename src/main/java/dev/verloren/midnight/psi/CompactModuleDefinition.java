package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface CompactModuleDefinition extends CompactNamedElement {
  @NotNull Collection<CompactNamedElement> getMembers();
}