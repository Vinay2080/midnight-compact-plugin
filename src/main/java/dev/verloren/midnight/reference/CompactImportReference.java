package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.stdlib.CompactStdlibService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Resolves imported module identifiers, file paths, and selectively imported symbols in Compact import declarations.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Invoked during reference resolution passes when an identifier or string literal appears inside
 * an import statement:
 * <ul>
 *   <li><b>Ctrl+Click Navigation:</b> Allows developers to click on a module name, file path,
 *       or imported symbol name to navigate directly to the definition in the source file or module.</li>
 *   <li><b>Standard Library Navigation:</b> Provides seamless navigation into virtual or bundled
 *       standard library files ({@link CompactStdlibService}) when importing stdlib symbols.</li>
 * </ul>
 * </p>
 *
 * <h3>Supported Import Kinds</h3>
 * <ul>
 *   <li>{@link Kind#MODULE}: Resolves named module identifiers (e.g. {@code Math} in {@code import Math;})
 *       to a {@link CompactModuleDefinition} or {@link CompactFile}.</li>
 *   <li>{@link Kind#FILE}: Resolves relative file paths (e.g. {@code './types'} in {@code import ... from './types';})
 *       to a {@link CompactFile}.</li>
 *   <li>{@link Kind#IMPORT_ELEMENT}: Resolves individual imported symbol names in selective imports
 *       (e.g. {@code GameState} in {@code import { GameState } from './types';}) to the exported declaration.</li>
 * </ul>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> {@link #resolveInner()} must be executed inside a Read Action
 *       ({@code @RequiresReadLock}).</li>
 *   <li><b>Background Execution:</b> Runs on background threads during daemon code analysis or on the EDT
 *       during interactive navigation.</li>
 * </ul>
 *
 * <h3>Resolution Pipeline Diagram</h3>
 * <pre>
 *   [Import Reference: Kind]
 *              │
 *   ┌──────────┼─────────────────────┐
 *   ▼          ▼                     ▼
 * [FILE]    [MODULE]          [IMPORT_ELEMENT]
 *   │          │                     │
 *   ▼          ▼                     ▼
 * [Stdlib / [Stdlib / findModule /  [resolveImportElementSource]
 *  resolve   resolveImportedFile]    │
 *  File]                             ▼
 *                                   [Exported Declaration in File/Module]
 * </pre>
 *
 * @see dev.verloren.midnight.reference.CompactReferenceBase
 * @see dev.verloren.midnight.psi.CompactImportDeclarationImpl
 * @see dev.verloren.midnight.psi.CompactImportElementImpl
 * @see dev.verloren.midnight.stdlib.CompactStdlibService
 */
public class CompactImportReference extends CompactReferenceBase {

  /**
   * Category of import reference determining the resolution strategy.
   */
  private final Kind kind;

  /**
   * Constructs an import reference.
   *
   * @param element        the PSI element hosting the reference (e.g. import element or module identifier)
   * @param rangeInElement relative text range of the reference within the element
   * @param kind           the category of import target to resolve
   */
  public CompactImportReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement, @NotNull Kind kind) {
    super(element, rangeInElement);
    this.kind = kind;
  }

  /**
   * Resolves the import reference based on its configured {@link Kind}.
   *
   * @return array of resolve results (containing a target file, module, or declaration)
   */
  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    CompactImportDeclarationImpl importDecl = getImportDeclaration();

    // Branch 1: FILE kind (resolves file path strings to CompactFile)
    if (kind == Kind.FILE) {
      if (isStandardLibrary(getValue())) {
        List<CompactFile> stdlib = CompactStdlibService.getInstance(getElement().getProject()).getStandardLibraryFiles();
        if (!stdlib.isEmpty()) {
          return toResults(List.of(stdlib.getFirst()));
        }
      }
      CompactFile file = importDecl != null ? importDecl.resolveImportedFile() : null;
      return file == null ? ResolveResult.EMPTY_ARRAY : toResults(List.of(file));
    }

    // Branch 2: MODULE kind (resolves module identifiers to CompactModuleDefinition or CompactFile)
    if (kind == Kind.MODULE) {
      if (isStandardLibrary(getValue())) {
        List<CompactFile> stdlib = CompactStdlibService.getInstance(getElement().getProject()).getStandardLibraryFiles();
        if (!stdlib.isEmpty()) {
          return toResults(List.of(stdlib.getFirst()));
        }
      }
      CompactModuleDefinition module = CompactResolveUtil.findModule(getElement(), getValue());
      if (module != null) {
        return toResults(List.of(module));
      }
      CompactFile file = importDecl != null ? importDecl.resolveImportedFile() : null;
      return file == null ? ResolveResult.EMPTY_ARRAY : toResults(List.of(file));
    }

    // Branch 3: IMPORT_ELEMENT kind (resolves individual symbol within selective import)
    if (kind == Kind.IMPORT_ELEMENT && getElement() instanceof CompactImportElementImpl importElement) {
      CompactNamedElement target = CompactResolveUtil.resolveImportElementSource(importElement);
      return target == null ? ResolveResult.EMPTY_ARRAY : toResults(List.of(target));
    }

    return ResolveResult.EMPTY_ARRAY;
  }

  /**
   * Helper extracting the enclosing {@link CompactImportDeclarationImpl}.
   *
   * @return enclosing import declaration, or {@code null} if not found
   */
  private @Nullable CompactImportDeclarationImpl getImportDeclaration() {
    if (getElement() instanceof CompactImportDeclarationImpl decl) {
      return decl;
    }
    return PsiTreeUtil.getParentOfType(getElement(), CompactImportDeclarationImpl.class);
  }

  /**
   * Detects whether an imported identifier or path refers to the Compact standard library.
   *
   * @param name text to evaluate
   * @return {@code true} if name matches any standard library identifier alias
   */
  private static boolean isStandardLibrary(@NotNull String name) {
    return "CompactStandardLibrary".equals(name)
        || "standard-library".equals(name)
        || "standard-library.compact".equals(name);
  }

  /**
   * Enumeration of import reference categories.
   */
  public enum Kind {
    /**
     * Named module import identifier (e.g. {@code import Math;}).
     */
    MODULE,

    /**
     * File path literal import (e.g. {@code import ... from './types';}).
     */
    FILE,

    /**
     * Individual symbol within a selective import block (e.g. {@code import { Foo } from ...;}).
     */
    IMPORT_ELEMENT
  }
}
