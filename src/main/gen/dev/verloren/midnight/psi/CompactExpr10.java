// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface CompactExpr10 extends PsiElement {

  @NotNull
  List<CompactExpr> getExprList();

  @Nullable
  CompactFun getFun();

  @NotNull
  List<CompactStructArg> getStructArgList();

  @Nullable
  CompactTerm getTerm();

  @Nullable
  CompactTref getTref();

  @Nullable
  CompactTsize getTsize();

  @NotNull
  List<CompactTupleArg> getTupleArgList();

}
