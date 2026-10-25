package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactBlock;
import dev.verloren.midnight.psi.CompactCallExprImpl;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactReferenceExprImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Inspection detecting recursive circuit calls (both direct self-recursion and mutual recursion).
 *
 * <h3>Zero-Knowledge Domain & Compiler Rules</h3>
 * <p>
 * Grounded in the Compact compiler pass {@code reject-recursive-circuits.ss}:
 * <ul>
 *   <li><b>Static Constraint Unrolling:</b> Zero-Knowledge proving circuits must compile into static,
 *       bounded arithmetic constraint graphs (R1CS / Plonk constraints). Recursive execution paths
 *       cannot be unrolled at compile time and are fundamentally prohibited in ZK circuit semantics.</li>
 *   <li><b>Direct vs Mutual Recursion:</b> Detects both direct self-calls (e.g. {@code circuit A() { A(); }})
 *       and indirect multi-step call chains (e.g. {@code A -> B -> C -> A}) using depth-bounded cycle detection.</li>
 * </ul>
 * </p>
 *
 * <h3>Cycle Detection Visual Diagram</h3>
 * <pre>
 *   [Circuit A] ──calls──&gt; [Circuit B] ──calls──&gt; [Circuit C]
 *        ▲                                             │
 *        └──────────────────calls back─────────────────┘  (Mutual Recursion Detected)
 * </pre>
 *
 * <h3>Threading & Performance Guards</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> Operates under {@code @RequiresReadLock} on daemon worker threads.</li>
 *   <li><b>Cycle Barrier & Depth Bound:</b> Recursion search is bounded by {@link #MAX_SEARCH_DEPTH} (10)
 *       and a {@code Set<CompactCircuitDefinition> visited} barrier to prevent infinite traversal.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.psi.CompactCircuitDefinition
 * @see dev.verloren.midnight.psi.CompactCallExprImpl
 */
public class CompactRecursiveCircuitInspection extends LocalInspectionTool {

  /**
   * Maximum call chain traversal depth to avoid freezing the IDE on extremely deep call graphs.
   */
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

  /**
   * Evaluates all calls inside a circuit body to flag self-recursion or mutual recursion.
   */
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
        // Case 1: Direct self-recursion
        if (targetCircuit.isEquivalentTo(circuit)) {
          holder.registerProblem(
              call,
              "Circuit '" + circuitName + "' cannot be recursive; recursion is forbidden in ZK circuits",
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING
          );
        } else {
          // Case 2: Mutual / indirect recursion through transitive call chains
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
      } else if (resolved == null) {
        // Fallback for incomplete code where callee does not resolve yet but names match
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

  /**
   * Depth-bounded depth-first search for reachability from {@code current} to {@code target}.
   */
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
        if (nextCircuit.isEquivalentTo(target)) {
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
