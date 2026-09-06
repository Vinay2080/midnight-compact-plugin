package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactContractImplementsDeclarationImpl extends CompactPsiElement implements CompactContractImplementsDeclaration {
  public CompactContractImplementsDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable CompactTypeReferenceImpl getTypeReference() {
    return PsiTreeUtil.getChildOfType(this, CompactTypeReferenceImpl.class);
  }

  @Override
  @SuppressWarnings("unused")
  public @Nullable String getInterfaceName() {
    CompactTypeReferenceImpl typeRef = getTypeReference();
    return typeRef != null ? typeRef.getText() : null;
  }

  @Override
  public @Nullable CompactExternalContractDeclaration resolveInterface() {
    CompactTypeReferenceImpl typeRef = getTypeReference();
    if (typeRef != null && typeRef.getReference() != null) {
      PsiElement resolved = typeRef.getReference().resolve();
      if (resolved instanceof CompactExternalContractDeclaration externalContract) {
        return externalContract;
      }
    }
    return null;
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitContractImplementsDeclaration(this);
  }
}
