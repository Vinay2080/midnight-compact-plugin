package dev.verloren.midnight.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * PSI element interface representing a struct definition in Compact.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Represents a record/struct declaration (e.g. {@code struct Point { x: Field, y: Field }}).
 * Acts as a composite data type container:
 * <ul>
 *   <li><b>Field Access:</b> Exposes {@link #getFields()} to retrieve all declared field definitions
 *       ({@link CompactStructFieldImpl}).</li>
 *   <li><b>Member Completion &amp; Resolution:</b> Referenced during member expressions ({@code point.x})
 *       via {@link dev.verloren.midnight.reference.CompactStructFieldReference}.</li>
 *   <li><b>Navigation &amp; Renaming:</b> Implements {@link CompactNamedElement}, rendering record icons
 *       in Structure View and symbol lists.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline</h3>
 * <p>Invoked across core IDE features:
 * <ul>
 *   <li>{@link dev.verloren.midnight.reference.CompactStructFieldReference} during struct field navigation and member resolution.</li>
 *   <li>{@link dev.verloren.midnight.completion.providers.CompactStructLiteralCompletionProvider} during struct literal generation.</li>
 *   <li>{@link dev.verloren.midnight.psi.CompactVisitor#visitStructDefinition(CompactStructDefinition)}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getFields()} or {@link #getName()} requires an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Renaming or modifying struct fields requires a Write Action on the EDT.</li>
 * </ul>
 *
 * @see CompactStructDefinitionImpl
 * @see CompactStructFieldImpl
 * @see dev.verloren.midnight.reference.CompactStructFieldReference
 */
public interface CompactStructDefinition extends CompactNamedElement {

  /**
   * Retrieves all fields declared inside this struct body.
   *
   * @return list of struct field elements; never {@code null}
   */
  @NotNull List<CompactStructFieldImpl> getFields();
}