package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves Compact identifiers in the {@link CompactResolveUtil.Namespace#TYPE} namespace.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Bound to type position identifier tokens by {@link CompactReferenceContributor}:
 * <ul>
 *   <li><b>Contexts:</b> Used in type annotations (e.g. {@code x: Field}), struct literal headers
 *       (e.g. {@code Point { x: 1, y: 2 }}), type alias targets, enum type references,
 *       contract implementations, and generic type arguments.</li>
 *   <li><b>Navigation & Refactoring:</b> Powers "Go to Declaration" and Rename Refactoring across
 *       all type definitions and usages.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> {@link #resolveInner()} must be executed inside a Read Action
 *       ({@code @RequiresReadLock}).</li>
 *   <li><b>Thread Environment:</b> Runs on background daemon threads during inspections or on the EDT
 *       during interactive navigation.</li>
 * </ul>
 *
 * <h3>Resolution Pipeline</h3>
 * <pre>
 *   [Type Identifier Token]
 *             │
 *             ▼
 *    [CompactResolveUtil.resolveType(name, place)]
 *             │
 *             ▼
 *    [Struct / Enum / Alias / Generic Param / Contract]
 * </pre>
 *
 * @see dev.verloren.midnight.reference.CompactReferenceBase
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 * @see dev.verloren.midnight.resolve.CompactResolveUtil.Namespace#TYPE
 */
public class CompactTypeReference extends CompactReferenceBase {

  /**
   * Constructs a type reference.
   *
   * @param element        the host PSI element containing the type identifier
   * @param rangeInElement relative text range of the type identifier within the host element
   */
  public CompactTypeReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement);
  }

  /**
   * Resolves the type reference in the {@link CompactResolveUtil.Namespace#TYPE} namespace.
   *
   * @return array of resolve results matching the type declaration
   */
  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    return toResults(CompactResolveUtil.resolveType(getValue(), getElement()));
  }
}