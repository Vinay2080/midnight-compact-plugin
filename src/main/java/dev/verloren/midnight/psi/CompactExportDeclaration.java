package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;

/**
 * PSI element interface representing an export declaration statement in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents an explicit export directive (e.g. {@code export circuit foo(...) { ... }} or {@code export { A, B };}).
 * Marks child or referenced declarations as public API visible to external modules and files:
 * <ul>
 *   <li><b>Public API Visibility:</b> Declarations marked with {@code export} are exposed for external imports.</li>
 *   <li><b>Visitor Dispatch:</b> Dispatched to {@link CompactVisitor#visitExportDeclaration(CompactExportDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} during module export analysis.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitExportDeclaration(CompactExportDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Traversing exported symbols requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Structural modifications require a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactExportDeclarationImpl
 * @see CompactImportDeclaration
 */
public interface CompactExportDeclaration extends PsiElement {
}