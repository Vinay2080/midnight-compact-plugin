package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactContractImplementsDeclarationImpl extends CompactPsiElement implements CompactContractImplementsDeclaration {
  public CompactContractImplementsDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitContractImplementsDeclaration(this);
  }

  @Override
  public @Nullable PsiReference getReference() {
    PsiElement typeReference = PsiTreeUtil.findChildOfType(this, CompactTypeReferenceImpl.class);
    return typeReference == null ? null : typeReference.getReference();
  }
}