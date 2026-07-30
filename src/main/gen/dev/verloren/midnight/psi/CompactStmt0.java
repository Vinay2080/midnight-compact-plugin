// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CompactStmt0 extends PsiElement {

  @Nullable
  CompactBlock getBlock();

  @NotNull
  List<CompactCbinding> getCbindingList();

  @Nullable
  CompactExprSeq getExprSeq();

  @Nullable
  CompactStmt getStmt();

  @Nullable
  CompactStmt0 getStmt0();

  @NotNull
  List<CompactTsize> getTsizeList();

}
