package dev.verloren.midnight.psi;

import com.intellij.psi.PsiElement;

/**
 * PSI element interface representing an import declaration statement in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents module or symbol import statements (e.g. {@code import { Foo, Bar as Baz } from "module";}).
 * Connects consumer files to foreign declarations:
 * <ul>
 *   <li><b>Selective Symbol Importing:</b> Contains child {@link CompactImportElementImpl} nodes that expose
 *       imported symbols to local file resolution.</li>
 *   <li><b>Module &amp; File Reference Anchor:</b> Anchors a {@link dev.verloren.midnight.reference.CompactImportReference}
 *       pointing to either another Compact file or a standard library module.</li>
 *   <li><b>Prefix Namespacing:</b> Supports import prefixes (e.g. {@code import * as M from "mod";}).</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} when building file-level symbol tables.</li>
 *   <li>{@link dev.verloren.midnight.inspection.CompactUnresolvedReferenceInspection} when validating imported symbols.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitImportDeclaration(CompactImportDeclaration)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> All query methods and file resolution require an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Adding, modifying, or removing imports requires a Write Action inside a
 *       {@link com.intellij.openapi.command.WriteCommandAction} on the EDT.</li>
 * </ul>
 *
 * @see CompactImportDeclarationImpl
 * @see CompactImportElementImpl
 * @see dev.verloren.midnight.reference.CompactImportReference
 */
public interface CompactImportDeclaration extends PsiElement {
}