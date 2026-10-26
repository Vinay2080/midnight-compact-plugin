package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * Base abstract implementation of {@link CompactNamedElement} providing common AST identifier
 * extraction, renaming, search-scoping, and presentation logic for Compact declarations.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Serves as the concrete base class for all named syntax constructs in Compact (circuits,
 * witnesses, ledger variables, structs, enums, parameters, and bindings). It fulfills the
 * contracts of {@link com.intellij.psi.PsiNameIdentifierOwner} and {@link com.intellij.navigation.NavigationItem}:
 * <ul>
 *   <li><b>Name Extraction ({@link #getName()}):</b> Finds the leaf identifier AST child node and returns its text.</li>
 *   <li><b>In-Place Renaming ({@link #setName(String)}):</b> Synthesizes a new leaf identifier via
 *       {@link CompactElementFactory#createIdentifierLeaf} and replaces the existing node in the AST.</li>
 *   <li><b>Text Offset ({@link #getTextOffset()}):</b> Points to the beginning of the identifier token rather than
 *       the entire declaration node, ensuring navigation (Ctrl+B) and caret placement focus directly on the symbol name.</li>
 *   <li><b>Search Scope ({@link #getUseScope()}):</b> Restricts local variables (parameters, generic type parameters,
 *       pattern bindings, block-scoped consts) to a {@link com.intellij.psi.search.LocalSearchScope} of the containing file,
 *       while elevating top-level declarations to {@link com.intellij.psi.search.GlobalSearchScope#projectScope}.</li>
 *   <li><b>Presentation ({@link #getPresentation()}):</b> Supplies standard IntelliJ icons and file location strings
 *       for Structure View, "Go to Symbol", and lookup menus.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE subsystems:
 * <ul>
 *   <li>{@link com.intellij.refactoring.rename.RenameHandler} during symbol renaming.</li>
 *   <li>{@link com.intellij.psi.search.searches.ReferencesSearch} during "Find Usages".</li>
 *   <li>{@link com.intellij.codeInsight.lookup.LookupElementBuilder} during completion list rendering.</li>
 *   <li>{@link com.intellij.ide.structureView.StructureView} when building the file outline.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Read queries execute on background daemon threads or EDT; mutation
 *       ({@link #setName(String)}) runs exclusively on the EDT.</li>
 *   <li><b>Read Action:</b> Methods {@link #getName()}, {@link #getNameIdentifier()}, {@link #getTextOffset()},
 *       and {@link #getUseScope()} require a platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> {@link #setName(String)} mutates the AST and strictly requires a Write Action
 *       within a {@link com.intellij.openapi.command.WriteCommandAction} on the EDT.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Memory Safety</h3>
 * <ul>
 *   <li><b>Dynamic Identifier Lookup:</b> {@link #getNameIdentifier()} queries the AST node's immediate children
 *       on demand rather than caching the child {@link PsiElement} in a field. This prevents stale PSI references
 *       across incremental tree mutations.</li>
 *   <li><b>Search Scope Narrowing:</b> By scoping local declarations to a {@link com.intellij.psi.search.LocalSearchScope},
 *       "Find Usages" skips global project indexing, drastically speeding up local refactorings and usage searches.</li>
 * </ul>
 *
 * @see CompactNamedElement
 * @see CompactElementFactory#createIdentifierLeaf
 * @see com.intellij.psi.PsiNameIdentifierOwner
 */
public abstract class CompactNamedElementImpl extends CompactPsiElement implements CompactNamedElement {

  /**
   * Constructs a named PSI declaration wrapping the specified AST node.
   *
   * @param node the AST node representing the declaration
   */
  protected CompactNamedElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Returns the inferred or declared semantic type for this named element.
   *
   * <p>Subclasses override this method to provide specific type inference (e.g. evaluating
   * explicit type references, return types, or struct definitions). The base implementation
   * defaults to {@link CompactPrimitiveType#UNKNOWN}.</p>
   *
   * @return the semantic {@link CompactType} of this element; never {@code null}
   */
  @Override
  public @NotNull CompactType getType() {
    return CompactPrimitiveType.UNKNOWN;
  }

  /**
   * Renames this element by replacing its leaf identifier token in the AST with a newly synthesized one.
   *
   * <p>Executes during rename refactoring operations. Throws {@link IncorrectOperationException}
   * if the declaration lacks an identifier node (e.g., broken syntax during editing).</p>
   *
   * @param name the new name string to assign to this element
   * @return this element instance post-renaming
   * @throws IncorrectOperationException if no existing identifier token is present to replace
   */
  @Override
  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    // Step 1: Locate the existing identifier AST leaf node
    PsiElement nameIdentifier = getNameIdentifier();
    if (nameIdentifier == null) {
      throw new IncorrectOperationException("Cannot rename Compact element without a name");
    }

    // Step 2: Synthesize a replacement identifier leaf token via the factory
    PsiElement newIdentifier = CompactElementFactory.createIdentifierLeaf(getProject(), name);

    // Step 3: Replace the AST leaf node within the parent AST hierarchy
    nameIdentifier.replace(newIdentifier);
    return this;
  }

  /**
   * Returns the text offset within the containing file where navigation should land.
   *
   * <p>Points to the start of the identifier token (e.g. {@code myCircuit}) rather than
   * the start of leading keywords, pragmas, or doc comments (e.g. {@code export circuit myCircuit}).</p>
   *
   * @return file character offset of the identifier leaf, or base element offset if unidentifiable
   */
  @Override
  public int getTextOffset() {
    PsiElement nameIdentifier = getNameIdentifier();
    return nameIdentifier == null ? super.getTextOffset() : nameIdentifier.getTextOffset();
  }

  /**
   * Calculates the search scope where usages of this named element may occur.
   *
   * <p>Local constructs (parameters, generic type parameters, pattern bindings, and block-level
   * const bindings) are restricted to {@link com.intellij.psi.search.LocalSearchScope} of the
   * containing file. Top-level declarations default to project-wide scope.</p>
   *
   * @return the search scope for finding references to this symbol
   */
  @Override
  public @NotNull com.intellij.psi.search.SearchScope getUseScope() {
    // Step 1: Detect local declarations that cannot be referenced outside this file
    if (this instanceof CompactParameterImpl
            || this instanceof CompactGenericParameterImpl
            || this instanceof CompactPatternImpl
            || (this instanceof CompactConstBindingImpl && com.intellij.psi.util.PsiTreeUtil.getParentOfType(this, CompactBlock.class) != null)) {
      return new com.intellij.psi.search.LocalSearchScope(getContainingFile());
    }

    // Step 2: Top-level declarations are search-scoped to the entire project
    return com.intellij.psi.search.GlobalSearchScope.projectScope(getProject());
  }

  /**
   * Returns the text of the declaration's identifier.
   *
   * @return the identifier string, or {@code null} if the element syntax is incomplete
   */
  @Override
  public @Nullable String getName() {
    PsiElement nameIdentifier = getNameIdentifier();
    return nameIdentifier == null ? null : nameIdentifier.getText();
  }

  /**
   * Finds the first child AST node that represents an {@code IDENTIFIER} token.
   *
   * @return the PSI leaf corresponding to the identifier, or {@code null} if none exists
   */
  @Override
  public @Nullable PsiElement getNameIdentifier() {
    for (ASTNode child : getNode().getChildren(null)) {
      if (child.getElementType() == CompactTokenTypes.IDENTIFIER) {
        return child.getPsi();
      }
    }
    return null;
  }

  /**
   * Produces an {@link com.intellij.navigation.ItemPresentation} used for displaying this element
   * in navigation popups, symbol lists, and Structure View.
   *
   * @return presentation provider mapping declarations to appropriate IntelliJ icons
   */
  @Override
  public com.intellij.navigation.ItemPresentation getPresentation() {
    return new com.intellij.navigation.ItemPresentation() {
      @Override
      public @Nullable String getPresentableText() {
        return getName();
      }

      @Override
      public @Nullable String getLocationString() {
        com.intellij.psi.PsiFile file = getContainingFile();
        return file != null ? "(" + file.getName() + ")" : null;
      }

      @Override
      public javax.swing.@NonNull Icon getIcon(boolean unused) {
        if (CompactNamedElementImpl.this instanceof CompactCircuitDefinition) {
          return com.intellij.icons.AllIcons.Nodes.Method;
        }
        if (CompactNamedElementImpl.this instanceof CompactWitnessDeclaration) {
          return com.intellij.icons.AllIcons.Nodes.Function;
        }
        if (CompactNamedElementImpl.this instanceof CompactLedgerDeclaration
                || CompactNamedElementImpl.this instanceof CompactStructFieldImpl
                || CompactNamedElementImpl.this instanceof CompactEnumMemberImpl) {
          return com.intellij.icons.AllIcons.Nodes.Field;
        }
        switch (CompactNamedElementImpl.this) {
          case CompactStructDefinition _ -> {
            return com.intellij.icons.AllIcons.Nodes.Record;
          }
          case CompactEnumDefinition _ -> {
            return com.intellij.icons.AllIcons.Nodes.Enum;
          }
          case CompactModuleDefinition _ -> {
            return com.intellij.icons.AllIcons.Nodes.Module;
          }
          default -> {
          }
        }

        if (CompactNamedElementImpl.this instanceof CompactExternalContractDeclaration
                || CompactNamedElementImpl.this instanceof CompactContractImplementsDeclaration) {
          return com.intellij.icons.AllIcons.Nodes.Class;
        }
        if (CompactNamedElementImpl.this instanceof CompactTypeDefinition) {
          return com.intellij.icons.AllIcons.Nodes.Type;
        }
        return dev.verloren.midnight.icons.MidnightIcons.FILE;
      }
    };
  }
}
