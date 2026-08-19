package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base implementation of {@link CompactNamedElement} for all Compact declarations.
 *
 * <p>Provides core lifecycle behaviors required by IntelliJ:
 * <ul>
 *   <li><b>Name Extraction ({@link #getName()}):</b> Finds the leaf identifier AST child node and returns its text.</li>
 *   <li><b>Renaming ({@link #setName(String)}):</b> Synthesizes a new leaf identifier via {@link CompactElementFactory#createIdentifierLeaf} and replaces the existing node in the AST.</li>
 *   <li><b>Text Offset ({@link #getTextOffset()}):</b> Points to the beginning of the identifier token rather than the entire declaration node (essential for navigation and caret positioning).</li>
 *   <li><b>Search Scope ({@link #getUseScope()}):</b> Scopes local declarations (parameters, local consts, pattern bindings) to {@link com.intellij.psi.search.LocalSearchScope} of the containing file, and top-level declarations to {@link com.intellij.psi.search.GlobalSearchScope#projectScope}.</li>
 * </ul>
 * </p>
 */
public abstract class CompactNamedElementImpl extends CompactPsiElement implements CompactNamedElement {
  protected CompactNamedElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    return CompactPrimitiveType.UNKNOWN;
  }

  @Override
  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    PsiElement nameIdentifier = getNameIdentifier();
    if (nameIdentifier == null) {
      throw new IncorrectOperationException("Cannot rename Compact element without a name");
    }
    PsiElement newIdentifier = CompactElementFactory.createIdentifierLeaf(getProject(), name);
    nameIdentifier.replace(newIdentifier);
    return this;
  }

  @Override
  public int getTextOffset() {
    PsiElement nameIdentifier = getNameIdentifier();
    return nameIdentifier == null ? super.getTextOffset() : nameIdentifier.getTextOffset();
  }

  @Override
  public @NotNull com.intellij.psi.search.SearchScope getUseScope() {
    if (this instanceof CompactParameterImpl
            || this instanceof CompactGenericParameterImpl
            || this instanceof CompactPatternImpl
            || (this instanceof CompactConstBindingImpl && com.intellij.psi.util.PsiTreeUtil.getParentOfType(this, CompactBlock.class) != null)) {
      return new com.intellij.psi.search.LocalSearchScope(getContainingFile());
    }
    return com.intellij.psi.search.GlobalSearchScope.projectScope(getProject());
  }

  @Override
  public @Nullable String getName() {
    PsiElement nameIdentifier = getNameIdentifier();
    return nameIdentifier == null ? null : nameIdentifier.getText();
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    for (ASTNode child : getNode().getChildren(null)) {
      if (child.getElementType() == CompactTokenTypes.IDENTIFIER) {
        return child.getPsi();
      }
    }
    return null;
  }
}