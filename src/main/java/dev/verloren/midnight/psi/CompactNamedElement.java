package dev.verloren.midnight.psi;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiNameIdentifierOwner;

/**
 * Common unified interface for all named declarations and bindings in the Compact PSI hierarchy.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Serves as the foundational contract bridging Compact language declarations with IntelliJ's
 * symbol management, navigation, refactoring, and indexing infrastructure:
 * <ul>
 *   <li><b>Navigation &amp; Symbol Search:</b> Enables "Go to Symbol" (Ctrl+Alt+Shift+N) and Structure View
 *       via {@link NavigationItem}.</li>
 *   <li><b>In-Place &amp; Dialog Renaming:</b> Enables IntelliJ's refactoring rename subsystem via
 *       {@link PsiNameIdentifierOwner#setName(String)} and {@link PsiNameIdentifierOwner#getNameIdentifier()}.</li>
 *   <li><b>Find Usages &amp; Search Scopes:</b> Integrates with {@link com.intellij.psi.search.searches.ReferencesSearch}
 *       by providing a targeted declaration anchor and use scope (local vs global).</li>
 *   <li><b>Type Queries:</b> Exposes {@link CompactTypeElement#getType()} for semantic type inference,
 *       enabling hover documentation and type-aware completion.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link com.intellij.refactoring.rename.RenameHandler} during refactoring operations.</li>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} during reference resolution and scope traversal.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactValueCompletionProvider} when populating completion lookups.</li>
 *   <li>{@link dev.verloren.midnight.navigation.CompactGotoDeclarationHandler} when resolving jump targets.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Name queries ({@link #getName()}, {@link #getNameIdentifier()}) and type inference
 *       ({@link #getType()}) strictly require a platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming ({@link #setName(String)}) mutates the underlying AST and must always
 *       execute within a {@link com.intellij.openapi.command.WriteCommandAction} on the Event Dispatch Thread (EDT).</li>
 * </ul>
 *
 * <h3>Compact Named Declaration Hierarchy (ASCII)</h3>
 * <pre>
 *                  CompactNamedElement
 *                          │
 *                CompactNamedElementImpl
 *                          │
 *        ┌─────────────────┼──────────────────────────┐
 *        ▼                 ▼                          ▼
 * Top-Level Forms     Contract Members           Local Bindings
 * ├─ CircuitDef       ├─ LedgerDecl              ├─ ParameterImpl
 * ├─ StructDef        ├─ WitnessDecl             ├─ ConstBindingImpl
 * ├─ EnumDef          ├─ ConstructorDecl         └─ PatternImpl
 * └─ ModuleDef        └─ StructFieldImpl
 * </pre>
 *
 * @see PsiNameIdentifierOwner
 * @see NavigationItem
 * @see CompactTypeElement
 * @see CompactNamedElementImpl
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 */
public interface CompactNamedElement extends PsiNameIdentifierOwner, NavigationItem, CompactTypeElement {
}