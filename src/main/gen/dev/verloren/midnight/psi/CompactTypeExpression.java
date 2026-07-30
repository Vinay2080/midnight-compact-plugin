// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CompactTypeExpression extends PsiElement {

  @Nullable
  CompactTref getTref();

  @NotNull
  List<CompactTsize> getTsizeList();

  @Nullable
  CompactTypeExpression getTypeExpression();

}
