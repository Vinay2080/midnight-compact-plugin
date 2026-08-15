package dev.verloren.midnight.inspection.fix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactConstBindingImpl;
import org.jetbrains.annotations.NotNull;

public class CompactRemoveUnusedVariableFix implements LocalQuickFix {
  private final String variableName;

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

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element == null) {
      return;
    }

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
