package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactConstructorDeclaration;
import dev.verloren.midnight.psi.CompactLedgerDeclaration;
import dev.verloren.midnight.psi.CompactModuleDefinition;
import dev.verloren.midnight.psi.CompactReferenceExprImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection flagging mutations to {@code sealed ledger} fields outside {@code constructor}.
 *
 * <h3>Compiler Rule Grounding</h3>
 * <p>
 * Implements the verification rules of Compact compiler pass {@code check-sealed-fields.ss}:
 * <ul>
 *   <li><b>Immutable After Deployment:</b> In Midnight contracts, fields declared with the {@code sealed}
 *       modifier (e.g. {@code sealed ledger admin: Address;}) can only be written during initial contract deployment
 *       within the {@code constructor}.</li>
 *   <li><b>Post-Deployment Immutability:</b> Mutating sealed fields inside standard contract circuits
 *       is strictly illegal and will cause the compiler to reject the contract.</li>
 * </ul>
 * </p>
 *
 * <h3>Module Initialization Special Case</h3>
 * <p>
 * Compact syntax restricts {@code constructor} to top-level contract definitions. Modules cannot
 * declare constructors directly. Reusable library modules (e.g., token standards) often initialize
 * sealed fields inside an initialization circuit intended to be invoked by the deploying contract's
 * constructor. As such, assignments within a {@link CompactModuleDefinition} are exempt from this inspection.
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> Executes within {@code @RequiresReadLock} on daemon worker threads.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.psi.CompactLedgerDeclaration#isSealed()
 * @see dev.verloren.midnight.psi.CompactConstructorDeclaration
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

  /**
   * Evaluates an assignment expression to detect forbidden mutations to sealed ledger variables.
   */
  private static void checkAssignment(@NotNull PsiElement assignExpr, @NotNull ProblemsHolder holder) {
    // Step 1: If inside constructor, mutating sealed field is legal
    if (PsiTreeUtil.getParentOfType(assignExpr, CompactConstructorDeclaration.class) != null) {
      return;
    }

    // Step 2: Exempt library module circuits intended for constructor execution
    if (PsiTreeUtil.getParentOfType(assignExpr, CompactModuleDefinition.class) != null) {
      return;
    }

    // Step 3: Find the LHS target reference expression
    PsiElement firstChild = assignExpr.getFirstChild();
    if (firstChild == null) {
      return;
    }

    CompactReferenceExprImpl targetRef;
    if (firstChild instanceof CompactReferenceExprImpl ref) {
      targetRef = ref;
    } else {
      targetRef = PsiTreeUtil.findChildOfType(firstChild, CompactReferenceExprImpl.class);
    }

    // Step 4: Resolve LHS target and flag if it is a sealed ledger declaration
    if (targetRef != null) {
      PsiElement resolved = targetRef.resolve();
      if (resolved instanceof CompactLedgerDeclaration ledger && ledger.isSealed()) {
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
