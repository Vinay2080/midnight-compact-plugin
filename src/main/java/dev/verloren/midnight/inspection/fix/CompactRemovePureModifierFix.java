package dev.verloren.midnight.inspection.fix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * Quick-fix to remove the {@code pure} modifier from a Compact circuit declaration.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Offered by {@link dev.verloren.midnight.inspection.CompactPureCircuitInspection} when a circuit marked
 * {@code pure} violates purity constraints (e.g. accesses ledger state, calls witnesses, or emits events):
 * <ul>
 *   <li><b>Intention / Quick-Fix Action:</b> Displayed in the Alt+Enter lightbulb menu.</li>
 *   <li><b>AST Mutation:</b> Deletes the {@link CompactTokenTypes#PURE} leaf token and any adjacent
 *       trailing whitespace from the circuit declaration's AST node.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Write Action Mandatory:</b> {@link #applyFix(Project, ProblemDescriptor)} is executed on the
 *       Event Dispatch Thread (EDT) within an IntelliJ platform {@link com.intellij.openapi.command.WriteCommandAction}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.inspection.CompactPureCircuitInspection
 * @see dev.verloren.midnight.psi.CompactCircuitDefinition
 */
public class CompactRemovePureModifierFix implements LocalQuickFix {

  @Override
  public @NotNull String getName() {
    return "Remove 'pure' modifier";
  }

  @Override
  public @NotNull String getFamilyName() {
    return "Remove modifier";
  }

  /**
   * Applies the fix by removing the {@code pure} modifier token and its trailing whitespace from the AST.
   *
   * @param project    current active project
   * @param descriptor problem descriptor identifying the violating circuit or AST element
   */
  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element == null) {
      return;
    }
    // Step 1: Locate the enclosing circuit definition
    CompactCircuitDefinition circuit = PsiTreeUtil.getParentOfType(element, CompactCircuitDefinition.class, false);
    if (circuit != null) {
      // Step 2: Locate the 'pure' leaf token inside the circuit's AST node
      ASTNode pureNode = circuit.getNode().findChildByType(CompactTokenTypes.PURE);
      if (pureNode != null) {
        // Step 3: Remove trailing whitespace to maintain clean code formatting
        ASTNode next = pureNode.getTreeNext();
        if (next != null && next.getElementType() == com.intellij.psi.TokenType.WHITE_SPACE) {
          circuit.getNode().removeChild(next);
        }
        // Step 4: Remove the 'pure' token
        circuit.getNode().removeChild(pureNode);
      }
    }
  }
}
