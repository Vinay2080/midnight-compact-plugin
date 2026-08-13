package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CompactModuleDefinitionImpl extends CompactNamedElementImpl implements CompactModuleDefinition {
  public CompactModuleDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitModuleDefinition(this);
  }

  public @NotNull Collection<CompactNamedElement> getMembers() {
    return PsiTreeUtil.findChildrenOfType(this, CompactNamedElement.class);
  }

  public @NotNull PsiElement[] getBodyElements() {
    return getChildren();
  }
}