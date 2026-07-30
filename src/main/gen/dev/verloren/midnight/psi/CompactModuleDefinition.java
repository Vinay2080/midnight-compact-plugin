// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CompactModuleDefinition extends PsiElement {

  @Nullable
  CompactGparams getGparams();

  @NotNull
  List<CompactProgramElement> getProgramElementList();

}
