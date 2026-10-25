package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.inspection.fix.CompactWrapWithDiscloseFix;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection enforcing Witness Protection Policy (WPP) disclosure boundaries.
 *
 * <h3>Zero-Knowledge Security Grounding</h3>
 * <p>
 * Implements the security checks defined by Compact compiler pass {@code track-witness-data.ss}:
 * <ul>
 *   <li><b>Data Leakage Prevention:</b> In Midnight zero-knowledge applications, {@code witness} calls
 *       provide private, off-chain client secrets. Assigning private witness values directly to public
 *       on-chain {@code ledger} state variables creates an unintentional privacy leak.</li>
 *   <li><b>Explicit Disclosure Boundary:</b> The developer must explicitly wrap private data in a
 *       {@code disclose(...)} expression (e.g. {@code $ledgerVar = disclose(privateWitnessCall());})
 *       to acknowledge and authorize publishing the value to the blockchain.</li>
 * </ul>
 * </p>
 *
 * <h3>Quick Fix</h3>
 * <p>
 * Provides {@link CompactWrapWithDiscloseFix} to automatically wrap the offending expression in
 * {@code disclose(...)} when triggered by the developer.
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> Executes within {@code @RequiresReadLock} on daemon worker threads.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.inspection.fix.CompactWrapWithDiscloseFix
 * @see dev.verloren.midnight.psi.CompactWitnessDeclaration
 * @see dev.verloren.midnight.psi.CompactLedgerDeclaration
 */
public class CompactUndisclosedWitnessInspection extends LocalInspectionTool {

  @Override
  public String getStaticDescription() {
    return "Reports private witness data assigned to public ledger fields without explicit disclose(...) wrapping.";
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof PsiErrorElement || PsiTreeUtil.getParentOfType(element, PsiErrorElement.class) != null) {
          return;
        }

        if (element.getNode() != null && element.getNode().getElementType() == CompactElementTypes.ASSIGN_EXPR) {
          checkWitnessAssignment(element, holder);
        }
      }
    };
  }

  /**
   * Inspects assignment expressions to ensure private witness data assigned to ledger fields is wrapped in disclose(...).
   */
  private static void checkWitnessAssignment(@NotNull PsiElement assignExpr, @NotNull ProblemsHolder holder) {
    PsiElement[] children = assignExpr.getChildren();
    if (children.length < 2) {
      return;
    }

    PsiElement lhs = children[0];
    PsiElement rhs = children[children.length - 1];

    // Step 1: Check if LHS targets an on-chain ledger declaration
    CompactReferenceExprImpl lhsRef = lhs instanceof CompactReferenceExprImpl
        ? (CompactReferenceExprImpl) lhs
        : PsiTreeUtil.findChildOfType(lhs, CompactReferenceExprImpl.class);

    if (lhsRef == null) {
      return;
    }

    PsiElement target = lhsRef.resolve();
    if (!(target instanceof CompactLedgerDeclaration)) {
      return;
    }

    // Step 2: Check if RHS is already wrapped in disclose(...)
    if (isWrappedInDisclose(rhs)) {
      return;
    }

    // Step 3: Check if RHS is a direct witness invocation
    if (rhs instanceof CompactCallExprImpl rhsCall) {
      PsiElement callee = rhsCall.resolveCallee();
      if (callee instanceof CompactWitnessDeclaration) {
        holder.registerProblem(
            rhs,
            "Private witness data cannot be assigned to ledger state without 'disclose(...)'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            new CompactWrapWithDiscloseFix()
        );
        return;
      }
    }

    // Step 4: Check if RHS is a variable originating from a witness call
    CompactReferenceExprImpl rhsRef = rhs instanceof CompactReferenceExprImpl
        ? (CompactReferenceExprImpl) rhs
        : PsiTreeUtil.findChildOfType(rhs, CompactReferenceExprImpl.class);

    if (rhsRef != null) {
      PsiElement varDecl = rhsRef.resolve();
      CompactConstBindingImpl constBinding = varDecl instanceof CompactConstBindingImpl
          ? (CompactConstBindingImpl) varDecl
          : PsiTreeUtil.getParentOfType(varDecl, CompactConstBindingImpl.class);

      if (constBinding != null) {
        CompactCallExprImpl initCall = PsiTreeUtil.findChildOfType(constBinding, CompactCallExprImpl.class);
        if (initCall != null) {
          PsiElement callee = initCall.resolveCallee();
          if (callee instanceof CompactWitnessDeclaration && !isWrappedInDisclose(initCall)) {
            holder.registerProblem(
                rhs,
                "Private witness data cannot be assigned to ledger state without 'disclose(...)'",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                new CompactWrapWithDiscloseFix()
            );
          }
        }
      }
    }
  }

  /**
   * Tests whether an element is enclosed directly within a disclose(...) expression.
   */
  private static boolean isWrappedInDisclose(@NotNull PsiElement element) {
    if (element.getNode() != null && element.getNode().getElementType() == CompactElementTypes.DISCLOSE_EXPR) {
      return true;
    }
    return PsiTreeUtil.getParentOfType(element, CompactPsiElement.class) != null
        && element.getParent() != null
        && element.getParent().getNode() != null
        && element.getParent().getNode().getElementType() == CompactElementTypes.DISCLOSE_EXPR;
  }
}
