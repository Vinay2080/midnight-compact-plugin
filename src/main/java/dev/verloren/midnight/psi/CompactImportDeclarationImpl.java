package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;

import com.intellij.openapi.util.TextRange;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
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
    ASTNode stringNode = getNode().findChildByType(CompactTokenTypes.STRING_LITERAL);
    if (stringNode != null) {
      int start = stringNode.getStartOffset() - getTextRange().getStartOffset();
      return new CompactImportReference(this, TextRange.from(start, stringNode.getTextLength()), CompactImportReference.Kind.FILE);
    }
    PsiElement moduleIdentifier = getModuleIdentifier();
    if (moduleIdentifier != null) {
      int start = moduleIdentifier.getTextRange().getStartOffset() - getTextRange().getStartOffset();
      return new CompactImportReference(this, TextRange.from(start, moduleIdentifier.getTextLength()), CompactImportReference.Kind.MODULE);
    }
    return null;
  }

  public @Nullable String getImportPath() {
    return CompactPsiUtil.extractStringLiteralValue(this);
  }

  public @Nullable PsiElement getModuleIdentifier() {
    for (PsiElement child : getChildren()) {
      if (child.getNode() != null && child.getNode().getElementType() == CompactTokenTypes.IDENTIFIER) {
        return child;
      }
    }
    ASTNode[] identifiers = getNode().getChildren(TokenSet.create(CompactTokenTypes.IDENTIFIER));
    if (identifiers.length > 0) {
      ASTNode last = identifiers[identifiers.length - 1];
      if (last.getTreeParent() == getNode()) {
        return last.getPsi();
      }
    }
    return null;
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

  public @Nullable CompactFile resolveImportedFile() {
    return CachedValuesManager.getCachedValue(this, () ->
        CachedValueProvider.Result.create(doResolveImportedFile(), PsiModificationTracker.MODIFICATION_COUNT)
    );
  }

  private @Nullable CompactFile doResolveImportedFile() {
    String path = getImportPath();
    if (path == null || path.isBlank()) {
      String moduleName = getModuleName();
      if (moduleName != null && !moduleName.isBlank()) {
        path = moduleName;
      } else {
        return null;
      }
    }
    return CompactPsiUtil.resolveRelativeCompactFile(this, path);
  }
}