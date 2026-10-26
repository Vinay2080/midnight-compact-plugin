package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactImportReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI implementation of an imported symbol entry within an import list (e.g., {@code Foo} or {@code Bar as Baz}).
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#IMPORT_ELEMENT}.
 * Acts as a dual-role element:
 * <ul>
 *   <li><b>Local Named Declaration:</b> Implements {@link CompactNamedElementImpl} so that the imported symbol
 *       (or its alias {@code Baz}) can be discovered and resolved by local scopes.</li>
 *   <li><b>Reference to Remote Definition:</b> Holds a {@link CompactImportReference} (kind {@code IMPORT_ELEMENT})
 *       targeting the origin declaration in the imported module or file.</li>
 *   <li><b>Aliasing Support:</b> In {@code Original as Alias}, {@link #getSourceIdentifier()} refers to {@code Original},
 *       while {@link #getNameIdentifier()} returns {@code Alias}. If unaliased, both point to the same token.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil#resolveImportElementSource} when traversing through imports to origin symbols.</li>
 *   <li>{@link dev.verloren.midnight.navigation.CompactGotoDeclarationHandler} during navigation to original declarations.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactUnusedImportInspection} when verifying if this specific imported symbol is referenced.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getReference()}, {@link #getNameIdentifier()}, or {@link #getSourceIdentifier()}
 *       requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactImportDeclarationImpl
 * @see CompactNamedElementImpl
 * @see dev.verloren.midnight.reference.CompactImportReference
 */
public class CompactImportElementImpl extends CompactNamedElementImpl {

  /**
   * Constructs an import element PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the imported item
   */
  public CompactImportElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Returns the local identifier under which this symbol is exposed in the current file.
   *
   * <p>If an alias is defined ({@code Original as Alias}), returns {@code Alias}; otherwise returns {@code Original}.</p>
   *
   * @return local name identifier leaf, or {@code null} if unparseable
   */
  @Override
  public @Nullable PsiElement getNameIdentifier() {
    PsiElement[] identifiers = findIdentifierChildren();
    if (identifiers.length == 0) {
      return null;
    }
    // The last identifier in the node is the local alias (or the only identifier if unaliased)
    return identifiers[identifiers.length - 1];
  }

  /**
   * Collects all child AST leaf elements that are identifier tokens.
   *
   * @return array of child identifier PSI elements; never {@code null}
   */
  private PsiElement @NotNull [] findIdentifierChildren() {
    ASTNode[] nodes = getNode().getChildren(TokenSet.create(CompactTokenTypes.IDENTIFIER));
    PsiElement[] elements = new PsiElement[nodes.length];
    for (int i = 0; i < nodes.length; i++) {
      elements[i] = nodes[i].getPsi();
    }
    return elements;
  }

  /**
   * Returns the identifier corresponding to the foreign source symbol being imported.
   *
   * <p>In {@code Original as Alias}, this points to {@code Original}.</p>
   *
   * @return foreign source identifier leaf, or {@code null} if missing
   */
  public @Nullable PsiElement getSourceIdentifier() {
    PsiElement[] identifiers = findIdentifierChildren();
    return identifiers.length == 0 ? null : identifiers[0];
  }

  /**
   * Creates a {@link CompactImportReference} bound to the source identifier token.
   *
   * @return import reference resolving to the foreign symbol, or {@code null} if no source identifier exists
   */
  @Override
  public @Nullable PsiReference getReference() {
    PsiElement source = getSourceIdentifier();
    if (source == null) {
      return null;
    }
    int start = source.getTextRange().getStartOffset() - getTextRange().getStartOffset();
    return new CompactImportReference(this, TextRange.from(start, source.getTextLength()), CompactImportReference.Kind.IMPORT_ELEMENT);
  }

  /**
   * Returns an array containing the single import reference or empty array.
   *
   * @return array of PSI references; never {@code null}
   */
  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}