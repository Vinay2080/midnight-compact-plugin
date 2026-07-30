// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import dev.verloren.midnight.psi.CompactExprSeq;
import dev.verloren.midnight.psi.CompactStmt;
import dev.verloren.midnight.psi.CompactStmt0;
import dev.verloren.midnight.psi.CompactVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactStmtImpl extends ASTWrapperPsiElement implements CompactStmt {

  public CompactStmtImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CompactVisitor) accept((CompactVisitor) visitor);
    else super.accept(visitor);
  }

  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitStmt(this);
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
