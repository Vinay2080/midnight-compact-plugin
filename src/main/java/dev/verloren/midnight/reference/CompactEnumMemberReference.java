package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves qualified enum variant references (e.g. {@code State.Active}) to their {@link CompactEnumMemberImpl} declaration.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Invoked during reference resolution passes when an identifier appears as the selector
 * in a member access expression where the qualifier is an enum type or enum value:
 * <ul>
 *   <li><b>Navigation & Highlighting:</b> Powers Ctrl+Click navigation from enum variant usages
 *       back to their definition inside a {@code enum} block.</li>
 *   <li><b>Rename Refactoring:</b> Ensures that renaming an enum variant in its declaration
 *       updates all qualified access sites throughout the codebase.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> {@link #resolveInner()} must be executed inside a Read Action
 *       ({@code @RequiresReadLock}).</li>
 *   <li><b>Background Execution:</b> Resolution typically occurs on background daemon threads or
 *       during synchronous user navigation on the EDT.</li>
 * </ul>
 *
 * <h3>Resolution Pipeline</h3>
 * <pre>
 *   [Member Expr: State.Active]
 *               │
 *               ▼
 *     [Extract Base: State]
 *               │
 *               ▼
 *   [Resolve Base in TYPE & VALUE]
 *               │
 *               ▼
 *    [Unwrap Imports if needed]
 *               │
 *               ▼
 *   [Match Enum Variant: Active]
 * </pre>
 *
 * <h3>Edge Case Mitigations</h3>
 * <ul>
 *   <li><b>Dual-Namespace Base Lookup:</b> Enums in Compact can be referenced as types or as values
 *       (constructors). Base lookup queries both {@link CompactResolveUtil#resolveType} and
 *       {@link CompactResolveUtil#resolveValue} to guarantee resolution regardless of context.</li>
 *   <li><b>Import Unwrapping:</b> If the base enum was brought into scope via a selective import
 *       ({@link CompactImportElementImpl}), it is transparently unwrapped to its source
 *       {@link CompactEnumDefinition} before searching for variant members.</li>
 *   <li><b>Deduplication:</b> Uses a {@code Set<PsiElement> seen} barrier to avoid duplicate variant
 *       resolution when an enum appears in multiple scope layers.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.reference.CompactReferenceBase
 * @see dev.verloren.midnight.psi.CompactEnumDefinition
 * @see dev.verloren.midnight.psi.CompactEnumMemberImpl
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 */
public class CompactEnumMemberReference extends CompactReferenceBase {

  /**
   * Constructs an enum member reference anchored to a member access expression.
   *
   * @param element        the enclosing member access expression
   * @param rangeInElement text range of the variant member identifier within the element
   */
  public CompactEnumMemberReference(@NotNull CompactMemberExprImpl element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement);
  }

  /**
   * Resolves the referenced enum variant by inspecting the member access base expression.
   *
   * @return array of resolve results matching the enum variant declaration
   */
  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    // Step 1: Extract base expression of the member access (e.g., 'State' in 'State.Active')
    CompactExpression base = ((CompactMemberExprImpl) getElement()).getBaseExpression();
    if (base == null) {
      return ResolveResult.EMPTY_ARRAY;
    }

    // Step 2: Ensure base has an identifier token
    if (base.getNode().findChildByType(CompactTokenTypes.IDENTIFIER) == null) {
      return ResolveResult.EMPTY_ARRAY;
    }
    PsiElement baseIdentifier = Objects.requireNonNull(base.getNode().findChildByType(CompactTokenTypes.IDENTIFIER)).getPsi();

    // Step 3: Resolve the base identifier in both TYPE and VALUE namespaces
    List<CompactNamedElement> baseTargets = new ArrayList<>();
    baseTargets.addAll(CompactResolveUtil.resolveType(baseIdentifier.getText(), getElement()));
    baseTargets.addAll(CompactResolveUtil.resolveValue(baseIdentifier.getText(), getElement()));

    // Step 4: Traverse matching base targets, unwrap imports, and match enum variant members
    List<CompactNamedElement> members = new ArrayList<>();
    java.util.Set<PsiElement> seen = new java.util.HashSet<>();
    for (CompactNamedElement baseTarget : baseTargets) {
      if (baseTarget instanceof CompactImportElementImpl) {
        baseTarget = CompactResolveUtil.resolveImportElementSource((CompactImportElementImpl) baseTarget);
      }
      if (baseTarget instanceof CompactEnumDefinition && seen.add(baseTarget)) {
        for (CompactEnumMemberImpl member : PsiTreeUtil.findChildrenOfType(baseTarget, CompactEnumMemberImpl.class)) {
          if (getValue().equals(member.getName())) {
            members.add(member);
          }
        }
      }
    }
    return toResults(members);
  }
}
