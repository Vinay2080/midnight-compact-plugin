package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CompactCircuitDefinitionImpl extends CompactNamedElementImpl implements CompactCircuitDefinition {
  public CompactCircuitDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull List<CompactNamedElement> getParameters() {
    List<CompactNamedElement> params = new ArrayList<>();
    for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(this, CompactNamedElement.class)) {
      if (named instanceof CompactParameterImpl || isPatternParameter(named)) {
        params.add(named);
      }
    }
    return params;
  }

  private static boolean isPatternParameter(@NotNull CompactNamedElement declaration) {
    if (!(declaration instanceof CompactPatternImpl)) {
      return false;
    }
    return hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.PATTERN_PARAMETER_LIST)
            || hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.ARROW_PARAMETER_LIST);
  }

  private static boolean hasAncestorOfType(@NotNull PsiElement element, @NotNull com.intellij.psi.tree.IElementType type) {
    for (PsiElement parent = element.getParent(); parent != null; parent = parent.getParent()) {
      if (parent.getNode() != null && parent.getNode().getElementType() == type) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nullable CompactBlock getBody() {
    return PsiTreeUtil.findChildOfType(this, CompactBlock.class);
  }

  @Override
  public @Nullable CompactTypeElement getReturnTypeElement() {
    return PsiTreeUtil.findChildOfType(this, CompactTypeElement.class);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitCircuitDefinition(this);
  }
}