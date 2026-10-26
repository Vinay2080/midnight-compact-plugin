package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * PSI element interface representing a namespace or module definition in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents a named module boundary (e.g. {@code module Crypto { ... }}).
 * Acts as both a named declaration (via {@link CompactNamedElement}) and a container
 * for nested symbols:
 * <ul>
 *   <li><b>Module Scoping:</b> Encapsulates functions, types, and constants under a distinct module namespace.</li>
 *   <li><b>Member Enumeration:</b> Exposes {@link #getMembers()} to retrieve all named member declarations
 *       declared within the module block.</li>
 *   <li><b>Navigation &amp; Symbol Search:</b> Appears in "Go to Symbol" (Ctrl+Alt+Shift+N) with module icons,
 *       and provides qualification for member resolution.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.resolve.CompactResolveUtil} during qualified symbol lookup and module resolution.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactIdentifierCompletionProvider} when suggesting module names or member accesses.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitModuleDefinition(CompactModuleDefinition)} during inspection passes.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getMembers()} or {@link #getName()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming or structural modifications must execute within a {@link com.intellij.openapi.command.WriteCommandAction} on the EDT.</li>
 * </ul>
 *
 * @see CompactNamedElement
 * @see CompactModuleDefinitionImpl
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 */
public interface CompactModuleDefinition extends CompactNamedElement {

  /**
   * Retrieves all named declarations (circuits, types, consts, structs, enums) defined inside this module.
   *
   * @return collection of named member elements; never {@code null}
   */
  @NotNull Collection<CompactNamedElement> getMembers();
}