package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactImportReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class CompactImportDeclarationImpl extends CompactPsiElement implements CompactImportDeclaration {
  public CompactImportDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitImportDeclaration(this);
  }

  @Override
  public @Nullable PsiReference getReference() {
    PsiElement moduleIdentifier = getModuleIdentifier();
    if (moduleIdentifier == null) {
      return null;
    }
    int start = moduleIdentifier.getTextRange().getStartOffset() - getTextRange().getStartOffset();
    return new CompactImportReference(this, TextRange.from(start, moduleIdentifier.getTextLength()), CompactImportReference.Kind.MODULE);
  }

  public @Nullable PsiElement getModuleIdentifier() {
    ASTNode[] identifiers = getNode().getChildren(TokenSet.create(CompactTokenTypes.IDENTIFIER));
    return identifiers.length == 0 ? null : identifiers[identifiers.length - 1].getPsi();
  }

  public @Nullable String getModuleName() {
    PsiElement moduleIdentifier = getModuleIdentifier();
    return moduleIdentifier == null ? null : moduleIdentifier.getText();
  }

  public @Nullable String getPrefix() {
    ASTNode prefixNode = getNode().findChildByType(dev.verloren.midnight.parser.CompactElementTypes.IMPORT_PREFIX);
    if (prefixNode == null) {
      return null;
    }
    ASTNode identifier = prefixNode.findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getText();
  }

  public @NotNull Collection<CompactImportElementImpl> getImportElements() {
    return PsiTreeUtil.findChildrenOfType(this, CompactImportElementImpl.class);
  }
}