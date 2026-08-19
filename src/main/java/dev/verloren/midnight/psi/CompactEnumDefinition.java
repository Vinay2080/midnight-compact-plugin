package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CompactEnumDefinition extends CompactNamedElement {
  @NotNull List<CompactEnumMemberImpl> getMembers();
}