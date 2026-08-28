package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;

import com.intellij.openapi.util.TextRange;

import com.intellij.psi.PsiReference;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactIncludeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;




/**
 * PSI representation of a Compact {@code include "filename.compact";} declaration.
 *
 * <p>Resolves the target Compact file relative to the containing directory or project
 * content roots. Results are cached via {@link CachedValuesManager} tied to
 * {@link PsiModificationTracker#MODIFICATION_COUNT}.</p>
 */
public class CompactIncludeDeclarationImpl extends CompactPsiElement implements CompactIncludeDeclaration {
  public CompactIncludeDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitIncludeDeclaration(this);
  }

  @Override
  public @Nullable PsiReference getReference() {
    ASTNode stringNode = getNode().findChildByType(CompactTokenTypes.STRING_LITERAL);
    if (stringNode == null) {
      return null;
    }
    int start = stringNode.getStartOffset() - getTextRange().getStartOffset();
    return new CompactIncludeReference(this, TextRange.from(start, stringNode.getTextLength()));
  }

  public @Nullable String getIncludePath() {
    return CompactPsiUtil.extractStringLiteralValue(this);
  }

  public @Nullable CompactFile resolveIncludedFile() {
    return CachedValuesManager.getCachedValue(this, () ->
        CachedValueProvider.Result.create(doResolveIncludedFile(), PsiModificationTracker.MODIFICATION_COUNT)
    );
  }

  private @Nullable CompactFile doResolveIncludedFile() {
    String path = getIncludePath();
    if (path == null || path.isBlank()) {
      return null;
    }
    return CompactPsiUtil.resolveRelativeCompactFile(this, path);
  }
}