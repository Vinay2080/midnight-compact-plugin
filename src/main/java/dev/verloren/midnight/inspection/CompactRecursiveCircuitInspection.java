package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;


import java.util.HashSet;
import java.util.Set;

/**
 * Inspection detecting recursive circuit calls (both direct self-recursion and mutual recursion).
 *
 * <p>In the Compact compiler ({@code reject-recursive-circuits.ss}), Zero-Knowledge proving circuits
 * require bounded static unrolling into arithmetic constraint systems. Recursive circuits cannot be
 * flattened or synthesized into proving keys and are strictly rejected at compile time.</p>
 */
public class CompactRecursiveCircuitInspection extends LocalInspectionTool {

  private static final int MAX_SEARCH_DEPTH = 10;

  @Override
  public String getStaticDescription() {
    return "Reports recursive circuit calls. Zero-knowledge proving circuits cannot be recursive.";
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
          checkCircuitRecursion(circuit, holder);
        }
      }
    };
  }

  private static void checkCircuitRecursion(@NotNull CompactCircuitDefinition circuit, @NotNull ProblemsHolder holder) {
    CompactBlock body = circuit.getBody();
    if (body == null) {
      return;
    }

    String circuitName = circuit.getName() != null ? circuit.getName() : "<anonymous>";

    for (CompactCallExprImpl call : PsiTreeUtil.findChildrenOfType(body, CompactCallExprImpl.class)) {
      if (PsiTreeUtil.getParentOfType(call, PsiErrorElement.class) != null) {
        continue;
      }

      PsiElement resolved = call.resolveCallee();
      if (resolved instanceof CompactCircuitDefinition targetCircuit) {
        // Direct self-recursion
        if (targetCircuit.isEquivalentTo(circuit) || (targetCircuit.getName() != null && targetCircuit.getName().equals(circuit.getName()))) {
          holder.registerProblem(
              call,
              "Circuit '" + circuitName + "' cannot be recursive; recursion is forbidden in ZK circuits",
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING
          );
        } else {
          // Mutual / indirect recursion
          Set<CompactCircuitDefinition> visited = new HashSet<>();
          visited.add(circuit);
          if (canReachTarget(targetCircuit, circuit, visited, 0)) {
            holder.registerProblem(
                call,
                "Mutual recursion detected: call chain leads back to circuit '" + circuitName + "'",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            );
          }
        }
      } else {
        CompactReferenceExprImpl ref = PsiTreeUtil.findChildOfType(call, CompactReferenceExprImpl.class);
        if (ref != null && circuit.getName() != null && circuit.getName().equals(ref.getText())) {
          holder.registerProblem(
              call,
              "Circuit '" + circuitName + "' cannot be recursive; recursion is forbidden in ZK circuits",
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING
          );
        }
      }

    }
  }

  private static boolean canReachTarget(
      @NotNull CompactCircuitDefinition current,
      @NotNull CompactCircuitDefinition target,
      @NotNull Set<CompactCircuitDefinition> visited,
      int depth
  ) {
    if (depth > MAX_SEARCH_DEPTH || !visited.add(current)) {
      return false;
    }

    CompactBlock body = current.getBody();
    if (body == null) {
      return false;
    }

    for (CompactCallExprImpl call : PsiTreeUtil.findChildrenOfType(body, CompactCallExprImpl.class)) {
      PsiElement resolved = call.resolveCallee();
      if (resolved instanceof CompactCircuitDefinition nextCircuit) {
        if (nextCircuit.isEquivalentTo(target) || (nextCircuit.getName() != null && nextCircuit.getName().equals(target.getName()))) {
          return true;
        }
        if (canReachTarget(nextCircuit, target, visited, depth + 1)) {
          return true;
        }
      }
    }

    return false;
  }
}
