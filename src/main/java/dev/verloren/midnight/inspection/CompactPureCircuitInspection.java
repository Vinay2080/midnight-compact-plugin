package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.inspection.fix.CompactRemovePureModifierFix;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection enforcing purity constraints on {@code pure circuit} declarations.
 *
 * <h3>Compiler Rule Grounding</h3>
 * <p>
 * Grounded in the official Compact compiler pass {@code identify-pure-circuits.ss}:
 * <ul>
 *   <li><b>No Ledger Access:</b> Pure circuits cannot read or write on-chain {@code ledger} state variables.</li>
 *   <li><b>No Witness Calls:</b> Pure circuits cannot invoke {@code witness} declarations.</li>
 *   <li><b>No Event Emissions:</b> Pure circuits cannot emit contract events ({@code emit}).</li>
 *   <li><b>No Impure Circuit Calls:</b> Pure circuits cannot invoke other circuits unless they are also marked {@code pure}.</li>
 * </ul>
 * </p>
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <ul>
 *   <li><b>Extension Point:</b> Registered as a {@code <localInspection>} in {@code plugin.xml}.</li>
 *   <li><b>Daemon Execution:</b> Invoked during background analysis by {@code LocalInspectionsPass}.</li>
 *   <li><b>Quick-Fix:</b> Provides {@link CompactRemovePureModifierFix} to remove the {@code pure} keyword
 *       if the developer intends to keep side-effecting operations.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.inspection.fix.CompactRemovePureModifierFix
 * @see dev.verloren.midnight.psi.CompactCircuitDefinition#isPure()
 */
public class CompactPureCircuitInspection extends LocalInspectionTool {

  @Override
  public String getStaticDescription() {
    return "Reports operations inside pure circuits that violate purity constraints (ledger access, witness calls, event emissions, or impure circuit invocations).";
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof PsiErrorElement || PsiTreeUtil.getParentOfType(element, PsiErrorElement.class) != null) {
          return;
        }

        if (element instanceof CompactCircuitDefinition circuit) {
          if (circuit.isPure()) {
            checkPureCircuit(circuit, holder);
          }
        }
      }
    };
  }

  /**
   * Scans a pure circuit's body for impure operations.
   */
  private static void checkPureCircuit(@NotNull CompactCircuitDefinition circuit, @NotNull ProblemsHolder holder) {
    CompactBlock body = circuit.getBody();
    if (body == null) {
      return;
    }

    String circuitName = circuit.getName() != null ? circuit.getName() : "<anonymous>";
    CompactRemovePureModifierFix removePureFix = new CompactRemovePureModifierFix();

    for (PsiElement child : PsiTreeUtil.findChildrenOfType(body, PsiElement.class)) {
      if (child instanceof PsiErrorElement) {
        continue;
      }

      // Step 1: Check emit expression
      if (child.getNode() != null && child.getNode().getElementType() == CompactElementTypes.EMIT_EXPR) {
        holder.registerProblem(
            child,
            "Pure circuit '" + circuitName + "' cannot emit events",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            removePureFix
        );
      }

      // Step 2: Check call expression (witness calls or non-pure circuit calls)
      if (child instanceof CompactCallExprImpl call) {
        PsiElement resolved = call.resolveCallee();
        if (resolved instanceof CompactWitnessDeclaration witness) {
          String witName = witness.getName() != null ? witness.getName() : "witness";
          holder.registerProblem(
              call,
              "Pure circuit '" + circuitName + "' cannot invoke witness '" + witName + "'",
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
              removePureFix
          );
        } else if (resolved instanceof CompactCircuitDefinition otherCircuit) {
          if (!otherCircuit.isPure() && !otherCircuit.isEquivalentTo(circuit)) {
            String otherName = otherCircuit.getName() != null ? otherCircuit.getName() : "circuit";
            holder.registerProblem(
                call,
                "Pure circuit '" + circuitName + "' cannot invoke non-pure circuit '" + otherName + "'",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                removePureFix
            );
          }
        }
      }

      // Step 3: Check ledger access via reference expression
      if (child instanceof CompactReferenceExprImpl ref) {
        if (ref.getParent() instanceof CompactCallExprImpl) {
          continue;
        }
        PsiElement resolved = ref.resolve();
        if (resolved instanceof CompactLedgerDeclaration ledger) {
          String ledgerName = ledger.getName() != null ? ledger.getName() : "ledger";
          holder.registerProblem(
              ref,
              "Pure circuit '" + circuitName + "' cannot access ledger state '" + ledgerName + "'",
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
              removePureFix
          );
        }
      }
    }
  }
}
