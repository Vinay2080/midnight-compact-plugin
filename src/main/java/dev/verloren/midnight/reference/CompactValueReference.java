package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves Compact identifiers in the {@link CompactResolveUtil.Namespace#VALUE} namespace.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Bound to value/expression position identifier tokens by {@link CompactReferenceContributor}:
 * <ul>
 *   <li><b>Contexts:</b> Used by local variables, function/circuit parameters, pattern bindings,
 *       local constants ({@code const x = ...}), circuit definitions, witness declarations,
 *       ledger state declarations ({@code ledger state x: ...}), and enum constructor invocations.</li>
 *   <li><b>Navigation & Refactoring:</b> Powers "Go to Declaration", Find Usages, and Rename Refactoring
 *       for all value-level symbols.</li>
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
 *   [Value Identifier Token]
 *              │
 *              ▼
 *    [CompactResolveUtil.resolveValue(name, place)]
 *              │
 *              ▼
 *    [Innermost Shadowing Match in Lexical Scope]
 * </pre>
 *
 * @see dev.verloren.midnight.reference.CompactReferenceBase
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 * @see dev.verloren.midnight.resolve.CompactResolveUtil.Namespace#VALUE
 */
public class CompactValueReference extends CompactReferenceBase {

  /**
   * Constructs a value reference.
   *
   * @param element        the host PSI element containing the value identifier
   * @param rangeInElement relative text range of the value identifier within the host element
   */
  public CompactValueReference(@NotNull PsiElement element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement);
  }

  /**
   * Resolves the value reference in the {@link CompactResolveUtil.Namespace#VALUE} namespace.
   *
   * @return array of resolve results matching the value declaration
   */
  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    return toResults(CompactResolveUtil.resolveValue(getValue(), getElement()));
  }
}