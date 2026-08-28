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
 * Inspection validating constraints inside {@code constructor} declarations.
 *
 * <p>In the Compact compiler ({@code reject-constructor-emit.ss} and {@code reject-constructor-cc-calls.ss}):
 * <ul>
 *   <li>Constructors cannot emit events ({@code emit}).</li>
 *   <li>Constructors cannot perform cross-contract calls ({@code cc-calls}).</li>
 * </ul>
 * </p>
 */
public class CompactConstructorRestrictionInspection extends LocalInspectionTool {

  @Override
  public String getStaticDescription() {
    return "Reports operations inside constructor that are forbidden during contract deployment (such as event emission or cross-contract calls).";
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof PsiErrorElement || PsiTreeUtil.getParentOfType(element, PsiErrorElement.class) != null) {
          return;
        }

        if (element instanceof CompactConstructorDeclaration constructor) {
          checkConstructor(constructor, holder);
        }
      }
    };
  }

  private static void checkConstructor(@NotNull CompactConstructorDeclaration constructor, @NotNull ProblemsHolder holder) {
    CompactBlock body = constructor.getBody();
    if (body == null) {
      return;
    }

    for (PsiElement child : PsiTreeUtil.findChildrenOfType(body, PsiElement.class)) {
      if (child instanceof PsiErrorElement) {
        continue;
      }

      // Check emit expression
      if (child.getNode() != null && child.getNode().getElementType() == CompactElementTypes.EMIT_EXPR) {
        holder.registerProblem(
            child,
            "Constructor cannot emit events",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
      }

      // Check cross-contract calls
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
