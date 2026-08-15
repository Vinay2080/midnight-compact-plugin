package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.inspection.fix.CompactRemoveUnusedVariableFix;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;

public class CompactUnusedLocalVariableInspection extends LocalInspectionTool {

  @Override
  public String getStaticDescription() {
    return "Reports unused local variables in Compact callable bodies.";
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof PsiErrorElement || PsiTreeUtil.getParentOfType(element, PsiErrorElement.class) != null) {
          return;
        }

        if (element instanceof CompactConstBindingImpl || element instanceof CompactPatternImpl) {
          if (isParameter(element)) {
            return;
          }

          if (PsiTreeUtil.getParentOfType(element, CompactBlock.class) == null) {
            return;
          }

          if (!isInsideCallable(element)) {
            return;
          }

          CompactNamedElement namedElement = (CompactNamedElement) element;
          String name = namedElement.getName();
          if (name == null || name.isEmpty() || name.startsWith("_")) {
            return;
          }

          // Check if there are any references in file use-scope
          boolean hasUsages = ReferencesSearch.search(namedElement, namedElement.getUseScope()).findFirst() != null;
          if (!hasUsages) {
            PsiElement nameId = namedElement.getNameIdentifier();
            PsiElement target = nameId != null ? nameId : namedElement;
            holder.registerProblem(
                target,
                "Unused local variable '" + name + "'",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                new CompactRemoveUnusedVariableFix(name)
            );
          }
        }
      }
    };
  }

  private static boolean isParameter(@NotNull PsiElement element) {
    if (element instanceof CompactParameterImpl) {
      return true;
    }
    for (PsiElement p = element.getParent(); p != null; p = p.getParent()) {
      if (p.getNode() != null) {
        com.intellij.psi.tree.IElementType type = p.getNode().getElementType();
        if (type == dev.verloren.midnight.parser.CompactElementTypes.PATTERN_PARAMETER_LIST
            || type == dev.verloren.midnight.parser.CompactElementTypes.SIMPLE_PARAMETER_LIST
            || type == dev.verloren.midnight.parser.CompactElementTypes.ARROW_PARAMETER_LIST
            || type == dev.verloren.midnight.parser.CompactElementTypes.TYPED_PATTERN) {
          return true;
        }
      }
      if (p instanceof CompactBlock) {
        return false;
      }
    }
    return false;
  }

  private static boolean isInsideCallable(@NotNull PsiElement element) {
    for (PsiElement p = element.getParent(); p != null; p = p.getParent()) {
      if (p instanceof CompactCircuitDefinition
          || p instanceof CompactWitnessDeclaration
          || p instanceof CompactConstructorDeclaration) {
        return true;
      }
      if (p instanceof CompactFile) {
        return false;
      }
    }
    return false;
  }
}
