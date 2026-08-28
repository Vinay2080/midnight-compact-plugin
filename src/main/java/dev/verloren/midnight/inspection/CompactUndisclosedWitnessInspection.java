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
 * Inspection enforcing Witness Protection (WPP) disclosure rules.
 *
 * <p>In the Compact compiler ({@code track-witness-data.ss}), values originating from private
 * {@code witness} calls cannot be assigned to public on-chain {@code ledger} fields unless
 * explicitly wrapped in a {@code disclose(...)} call to prevent accidental data leaks.</p>
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

  private static void checkWitnessAssignment(@NotNull PsiElement assignExpr, @NotNull ProblemsHolder holder) {
    PsiElement[] children = assignExpr.getChildren();
    if (children.length < 2) {
      return;
    }

    PsiElement lhs = children[0];
    PsiElement rhs = children[children.length - 1];

    // Check if LHS targets a ledger declaration
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

    // Check if RHS is wrapped in disclose(...)
    if (isWrappedInDisclose(rhs)) {
      return;
    }

    // Check if RHS is a direct witness call
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

    // Check if RHS is a variable originating from a witness call
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
