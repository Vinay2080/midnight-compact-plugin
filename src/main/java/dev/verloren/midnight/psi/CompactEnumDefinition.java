package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * PSI element interface representing an enum definition in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents an enumeration type declaration (e.g. {@code enum Color { Red, Green, Blue }}).
 * Acts as an algebraic sum type container:
 * <ul>
 *   <li><b>Member Enumeration:</b> Exposes {@link #getMembers()} to retrieve all defined enum variants ({@link CompactEnumMemberImpl}).</li>
 *   <li><b>Resolution Target:</b> Target of member expressions (e.g. {@code Color.Red}) via
 *       {@link dev.verloren.midnight.reference.CompactEnumMemberReference}.</li>
 *   <li><b>Navigation &amp; Renaming:</b> Implements {@link CompactNamedElement}, rendering an Enum icon in Structure View.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.reference.CompactEnumMemberReference} when resolving enum members.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactMemberCompletionProvider} when suggesting enum variants.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitEnumDefinition(CompactEnumDefinition)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getMembers()} or {@link #getName()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming via {@link #setName(String)} requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactEnumDefinitionImpl
 * @see CompactEnumMemberImpl
 * @see dev.verloren.midnight.reference.CompactEnumMemberReference
 */
public interface CompactEnumDefinition extends CompactNamedElement {

  /**
   * Retrieves all member variants declared inside this enum.
   *
   * @return list of enum member variants; never {@code null}
   */
  @NotNull List<CompactEnumMemberImpl> getMembers();
}