package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.version.CompactSemVerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactPragmaFormImpl extends CompactPsiElement implements CompactPragmaForm {
  public CompactPragmaFormImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable PsiElement getPragmaIdentifier() {
    ASTNode identifier = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getPsi();
  }

  @Override
  public @Nullable String getPragmaName() {
    PsiElement id = getPragmaIdentifier();
    return id != null ? id.getText() : null;
  }

  @Override
  public @Nullable String getConstraintText() {
    PsiElement id = getPragmaIdentifier();
    if (id == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    PsiElement sibling = id.getNextSibling();
    while (sibling != null) {
      if (sibling.getNode().getElementType() == CompactTokenTypes.SEMICOLON) {
        break;
      }
      sb.append(sibling.getText());
      sibling = sibling.getNextSibling();
    }
    String text = sb.toString().trim();
    return text.isEmpty() ? null : text;
  }

  @Override
  public @Nullable String getRequiredVersion() {
    return CompactSemVerUtil.extractVersion(getConstraintText());
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitPragmaForm(this);
  }
}
