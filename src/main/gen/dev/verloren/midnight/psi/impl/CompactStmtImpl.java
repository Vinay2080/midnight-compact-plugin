// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static dev.verloren.midnight.psi.CompactTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import dev.verloren.midnight.psi.*;

public class CompactStmtImpl extends ASTWrapperPsiElement implements CompactStmt {

  public CompactStmtImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitStmt(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CompactExprSeq getExprSeq() {
    return findChildByClass(CompactExprSeq.class);
  }

  @Override
  @Nullable
  public CompactStmt getStmt() {
    return findChildByClass(CompactStmt.class);
  }

  @Override
  @Nullable
  public CompactStmt0 getStmt0() {
    return findChildByClass(CompactStmt0.class);
  }

}
