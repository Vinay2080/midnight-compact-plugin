package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactBlock;
import dev.verloren.midnight.psi.CompactCallExprImpl;
import dev.verloren.midnight.psi.CompactConstructorDeclaration;
import dev.verloren.midnight.psi.CompactExternalContractDeclaration;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection validating constraints inside {@code constructor} declarations.
 *
 * <h3>Compiler Rule Grounding</h3>
 * <p>
 * Matches rules enforced by the official Compact compiler
 * (specifically passes {@code reject-constructor-emit.ss} and {@code reject-constructor-cc-calls.ss}):
 * <ul>
 *   <li><b>No Event Emissions:</b> Constructors cannot emit events ({@code emit}). Contract deployment
 *       transactions in Midnight cannot produce event logs.</li>
 *   <li><b>No Cross-Contract Calls:</b> Constructors cannot invoke methods on external contracts
 *       ({@code cc-calls}) because the deploying contract does not yet possess a verified deployed address.</li>
 * </ul>
 * </p>
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <ul>
 *   <li><b>Extension Point:</b> Registered as a {@code <localInspection>} in {@code plugin.xml}.</li>
 *   <li><b>Calling Pipeline:</b> Invoked during background code analysis by {@code LocalInspectionsPass}
 *       managed by IntelliJ's {@code DaemonCodeAnalyzer}.</li>
 *   <li><b>Threading Contract:</b> Runs under a Read Action ({@code @RequiresReadLock}) on pooled background
 *       threads. Cancels cooperatively when the document changes.</li>
 *   <li><b>Broken Syntax Mitigation:</b> Bails out if any ancestor or visited node is a {@link PsiErrorElement}
 *       to prevent noisy false positives during active editing.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.psi.CompactConstructorDeclaration
 * @see dev.verloren.midnight.psi.CompactExternalContractDeclaration
 */
public class CompactConstructorRestrictionInspection extends LocalInspectionTool {

  @Override
  public String getStaticDescription() {
    return "Reports operations inside constructor that are forbidden during contract deployment (such as event emission or cross-contract calls).";
  }

  /**
   * Builds the PSI visitor dispatched by IntelliJ's daemon inspection runner.
   *
   * @param holder     sink for registering inspection problems
   * @param isOnTheFly {@code true} if running interactively inside an open editor
   * @return element visitor checking constructor declarations
   */
  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        // Step 1: Suppress analysis if syntax errors are present
        if (element instanceof PsiErrorElement || PsiTreeUtil.getParentOfType(element, PsiErrorElement.class) != null) {
          return;
        }

        // Step 2: Validate constructor bodies
        if (element instanceof CompactConstructorDeclaration constructor) {
          checkConstructor(constructor, holder);
        }
      }
    };
  }

  /**
   * Scans a constructor body for illegal operations (event emissions and cross-contract calls).
   *
   * @param constructor constructor declaration to inspect
   * @param holder      problems sink
   */
  private static void checkConstructor(@NotNull CompactConstructorDeclaration constructor, @NotNull ProblemsHolder holder) {
    CompactBlock body = constructor.getBody();
    if (body == null) {
      return;
    }

    // Step-by-step AST traversal of constructor statements
    for (PsiElement child : PsiTreeUtil.findChildrenOfType(body, PsiElement.class)) {
      if (child instanceof PsiErrorElement) {
        continue;
      }

      // Step 3: Flag event emissions (EMIT_EXPR)
      if (child.getNode() != null && child.getNode().getElementType() == CompactElementTypes.EMIT_EXPR) {
        holder.registerProblem(
            child,
            "Constructor cannot emit events",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }

      // Step 4: Flag cross-contract calls to external contracts
      if (child instanceof CompactCallExprImpl call) {
        PsiElement resolved = call.resolveCallee();
        if (resolved != null && PsiTreeUtil.getParentOfType(resolved, CompactExternalContractDeclaration.class) != null) {
          holder.registerProblem(
              call,
              "Constructor cannot perform cross-contract calls",
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING
          );
        }
      }
    }
  }
}
