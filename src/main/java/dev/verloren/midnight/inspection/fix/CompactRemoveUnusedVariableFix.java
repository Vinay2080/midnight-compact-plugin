package dev.verloren.midnight.inspection.fix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactConstBindingImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Quick-fix for safely removing unused local {@code const} variable bindings.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Offered by {@link dev.verloren.midnight.inspection.CompactUnusedLocalVariableInspection}:
 * <ul>
 *   <li><b>Intention / Quick-Fix Action:</b> Displayed in the Alt+Enter menu on dead local variables.</li>
 *   <li><b>Tree Traversal:</b> Walks up the AST to locate the containing {@link CompactElementTypes#CONST_STATEMENT}
 *       and cleanly deletes it from the containing block statement list.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Write Action Mandatory:</b> {@link #applyFix(Project, ProblemDescriptor)} is executed on the
 *       Event Dispatch Thread (EDT) inside a {@link com.intellij.openapi.command.WriteCommandAction}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.inspection.CompactUnusedLocalVariableInspection
 * @see dev.verloren.midnight.psi.CompactConstBindingImpl
 */
public class CompactRemoveUnusedVariableFix implements LocalQuickFix {

  /**
   * Name of the variable to be removed (used in the user-visible menu label).
   */
  private final String variableName;

  /**
   * Constructs the remove quick-fix with the variable's identifier name.
   *
   * @param variableName name of the unused variable
   */
  public CompactRemoveUnusedVariableFix(@NotNull String variableName) {
    this.variableName = variableName;
  }

  @Override
  public @NotNull String getName() {
    return "Remove unused variable '" + variableName + "'";
  }

  @Override
  public @NotNull String getFamilyName() {
    return "Remove unused variable";
  }

  /**
   * Deletes the enclosing const statement from the PSI tree.
   *
   * @param project    current active project
   * @param descriptor problem descriptor identifying the unused variable identifier
   */
  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element == null) {
      return;
    }

    // Step 1: Walk up ancestors to find the containing CONST_STATEMENT node
    PsiElement current = element;
    while (current != null) {
      if (current.getNode() != null && current.getNode().getElementType() == CompactElementTypes.CONST_STATEMENT) {
        current.delete();
        return;
      }
      if (current instanceof CompactConstBindingImpl) {
        PsiElement parent = current.getParent();
        if (parent != null && parent.getNode() != null && parent.getNode().getElementType() == CompactElementTypes.CONST_STATEMENT) {
          parent.delete();
          return;
        }
      }
      current = current.getParent();
    }
  }
}
