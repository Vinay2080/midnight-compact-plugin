package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;


public interface CompactReferenceExpr extends CompactExpression {
  @Nullable PsiElement resolve();
}