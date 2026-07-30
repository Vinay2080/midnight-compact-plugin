// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CompactPattern extends PsiElement {

  @NotNull
  List<CompactPattern> getPatternList();

  @NotNull
  List<CompactPatternStructElt> getPatternStructEltList();

}
