package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;

/**
 * PSI element interface representing a file include declaration ({@code include "file.compact";}) in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents a textual or AST-level file inclusion directive in Compact source code.
 * Includes inline all top-level symbols of the target file into the current file's namespace:
 * <ul>
 *   <li><b>File Inclusion:</b> Unlike {@link CompactImportDeclaration}, which selectively brings
 *       specific names or modules into scope, an include makes all declarations in the target file
 *       directly accessible.</li>
 *   <li><b>Reference Anchor:</b> Anchors a {@link dev.verloren.midnight.reference.CompactIncludeReference}
 *       pointing to the referenced physical or virtual Compact file.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil#collectIncludedFiles} when aggregating included file scopes.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitIncludeDeclaration(CompactIncludeDeclaration)} during inspection.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Resolution queries strictly require an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only interface; replaced via Write Action during file refactorings.</li>
 * </ul>
 *
 * @see CompactIncludeDeclarationImpl
 * @see dev.verloren.midnight.reference.CompactIncludeReference
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 */
public interface CompactIncludeDeclaration extends PsiElement {
}