package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactImportReference;
import dev.verloren.midnight.stdlib.CompactStdlibService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * PSI implementation of a Compact {@code import ...} statement wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#IMPORT_FORM}.
 * Manages import paths, module names, prefixes, and file target resolution:
 * <ul>
 *   <li><b>Reference Creation:</b> Constructs a {@link CompactImportReference} via {@link #getReference()},
 *       supporting file-based paths ({@link dev.verloren.midnight.reference.CompactImportReference.Kind#FILE})
 *       or identifier-based modules ({@link dev.verloren.midnight.reference.CompactImportReference.Kind#MODULE}).</li>
 *   <li><b>Standard Library Integration:</b> Detects standard library identifiers ({@code "CompactStandardLibrary"},
 *       {@code "standard-library"}) and resolves them to the bundled stdlib virtual file via {@link CompactStdlibService}.</li>
 *   <li><b>Cache Invalidation:</b> Employs {@link CachedValuesManager} tied to {@link PsiModificationTracker#MODIFICATION_COUNT}
 *       to cache the resolved {@link CompactFile} across keystrokes.</li>
 *   <li><b>Prefix Aliasing:</b> Inspects any attached {@code IMPORT_PREFIX} child to support namespaced imports.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} when building available scope.</li>
 *   <li>{@link dev.verloren.midnight.navigation.CompactGotoDeclarationHandler} during navigation to imported files/modules.</li>
 *   <li>{@link CompactVisitor#visitImportDeclaration(CompactImportDeclaration)} during inspection passes.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> All query methods and file resolution require an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Deleting or restructuring import statements must run inside a Write Action on the EDT.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Cache Invalidation</h3>
 * <ul>
 *   <li><b>PsiModificationTracker Dependency:</b> Caches target files via {@link CachedValuesManager} keyed to
 *       {@link PsiModificationTracker#MODIFICATION_COUNT}, avoiding costly filesystem walks while immediately
 *       invalidating when project files change.</li>
 *   <li><b>Zero Persistent Storage:</b> Resolved files are never retained in static fields.</li>
 * </ul>
 *
 * @see CompactImportDeclaration
 * @see CompactImportElementImpl
 * @see dev.verloren.midnight.reference.CompactImportReference
 * @see dev.verloren.midnight.stdlib.CompactStdlibService
 */
public class CompactImportDeclarationImpl extends CompactPsiElement implements CompactImportDeclaration {

  /**
   * Constructs an import declaration PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the import statement
   */
  public CompactImportDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Dispatches this element to {@link CompactVisitor#visitImportDeclaration(CompactImportDeclaration)}.
   *
   * @param visitor the Compact-specific visitor inspecting this element
   */
  @Override
  public void accept(@NotNull CompactVisitor visitor) {
    visitor.visitImportDeclaration(this);
  }

  /**
   * Constructs a {@link CompactImportReference} targeting either the path string literal or module identifier.
   *
   * @return import reference, or {@code null} if neither string path nor module identifier is found
   */
  @Override
  public @Nullable PsiReference getReference() {
    // Step 1: Check for a string literal import path (e.g., import ... from "path")
    ASTNode stringNode = getNode().findChildByType(CompactTokenTypes.STRING_LITERAL);
    if (stringNode != null) {
      int start = stringNode.getStartOffset() - getTextRange().getStartOffset();
      return new CompactImportReference(this, TextRange.from(start, stringNode.getTextLength()), CompactImportReference.Kind.FILE);
    }
    // Step 2: Check for a bare module identifier import (e.g., import ModuleName;)
    PsiElement moduleIdentifier = getModuleIdentifier();
    if (moduleIdentifier != null) {
      int start = moduleIdentifier.getTextRange().getStartOffset() - getTextRange().getStartOffset();
      return new CompactImportReference(this, TextRange.from(start, moduleIdentifier.getTextLength()), CompactImportReference.Kind.MODULE);
    }
    return null;
  }

  /**
   * Extracts the unquoted string content from the import path string literal.
   *
   * @return file import path string, or {@code null} if not a path import
   */
  public @Nullable String getImportPath() {
    return CompactPsiUtil.extractStringLiteralValue(this);
  }

  /**
   * Locates the PSI element corresponding to the module identifier token.
   *
   * @return module identifier element, or {@code null} if not found
   */
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

  /**
   * Returns the textual module name specified in this import statement.
   *
   * @return module name, or {@code null} if missing
   */
  public @Nullable String getModuleName() {
    PsiElement moduleIdentifier = getModuleIdentifier();
    return moduleIdentifier == null ? null : moduleIdentifier.getText();
  }

  /**
   * Extracts any import alias prefix (e.g. {@code "M"} in {@code import * as M from "mod"}).
   *
   * @return prefix identifier string, or {@code null} if no prefix exists
   */
  public @Nullable String getPrefix() {
    ASTNode prefixNode = getNode().findChildByType(dev.verloren.midnight.parser.CompactElementTypes.IMPORT_PREFIX);
    if (prefixNode == null) {
      return null;
    }
    ASTNode identifier = prefixNode.findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getText();
  }

  /**
   * Resolves the target {@link CompactFile} with cached memoization.
   *
   * @return resolved {@link CompactFile}, or {@code null} if unresolved
   */
  public @Nullable CompactFile resolveImportedFile() {
    return CachedValuesManager.getCachedValue(this, () ->
        CachedValueProvider.Result.create(doResolveImportedFile(), PsiModificationTracker.MODIFICATION_COUNT)
    );
  }

  /**
   * Performs uncached resolution of the import statement to a target file.
   *
   * @return target file, or {@code null} if not found
   */
  private @Nullable CompactFile doResolveImportedFile() {
    // Step 1: Check if this is a path-based import
    String path = getImportPath();
    if (path == null || path.isBlank()) {
      // Step 2: Check if this is an identifier-based module import
      String moduleName = getModuleName();
      if (moduleName != null && !moduleName.isBlank()) {
        if (isStandardLibrary(moduleName)) {
          List<CompactFile> stdlib = CompactStdlibService.getInstance(getProject()).getStandardLibraryFiles();
          return stdlib.isEmpty() ? null : stdlib.getFirst();
        }
        path = moduleName;
      } else {
        return null;
      }
    } else if (isStandardLibrary(path)) {
      // Step 3: Handle standard library path strings
      List<CompactFile> stdlib = CompactStdlibService.getInstance(getProject()).getStandardLibraryFiles();
      return stdlib.isEmpty() ? null : stdlib.getFirst();
    }
    // Step 4: Fall back to relative file lookup
    return CompactPsiUtil.resolveRelativeCompactFile(this, path);
  }

  /**
   * Tests whether an import name matches any recognized standard library aliases.
   *
   * @param name the import name or path
   * @return {@code true} if matching standard library identifiers; {@code false} otherwise
   */
  private static boolean isStandardLibrary(@NotNull String name) {
    return "CompactStandardLibrary".equals(name)
        || "standard-library".equals(name)
        || "standard-library.compact".equals(name);
  }
}
