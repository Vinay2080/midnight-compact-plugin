package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection flagging mutations to {@code sealed ledger} fields outside {@code constructor}.
 *
 * <p>In the Compact compiler ({@code check-sealed-fields.ss}), sealed ledger fields
 * can only be initialized and mutated during contract deployment within the {@code constructor}.
 * Modifying sealed fields in circuits is strictly prohibited.</p>
 */
public class CompactSealedFieldMutationInspection extends LocalInspectionTool {

  @Override
  public String getStaticDescription() {
    return "Reports modifications and assignments to sealed ledger fields outside constructor.";
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
          checkAssignment(element, holder);
        }
      }
    };
  }

  private static void checkAssignment(@NotNull PsiElement assignExpr, @NotNull ProblemsHolder holder) {
    // If inside constructor, mutating sealed field is legal
    if (PsiTreeUtil.getParentOfType(assignExpr, CompactConstructorDeclaration.class) != null) {
      return;
    }

    // Find the target on the LHS
    PsiElement firstChild = assignExpr.getFirstChild();
    if (firstChild == null) {
      return;
    }

    CompactReferenceExprImpl targetRef;
    if (firstChild instanceof CompactReferenceExprImpl) {
      targetRef = (CompactReferenceExprImpl) firstChild;
    } else {
      targetRef = PsiTreeUtil.findChildOfType(firstChild, CompactReferenceExprImpl.class);
    }

    if (targetRef != null) {
      PsiElement resolved = targetRef.resolve();
      if (resolved instanceof CompactLedgerDeclaration ledger) {
        if (ledger.isSealed()) {
          String fieldName = ledger.getName() != null ? ledger.getName() : "field";
          holder.registerProblem(
              targetRef,
              "Cannot modify sealed ledger field '" + fieldName + "' outside constructor",
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING
          );
        }
      }
    }
  }
}
