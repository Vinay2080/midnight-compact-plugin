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

/**
 * Inspection flagging local {@code const} variable bindings that have zero usages in their lexical scope.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered under {@code <localInspection>} in {@code plugin.xml}:
 * <ul>
 *   <li><b>Dead Code Detection:</b> Employs IntelliJ's index-accelerated {@link ReferencesSearch}
 *       bounded to the element's local {@link CompactNamedElement#getUseScope()} to confirm whether
 *       a local variable has any referencing sites.</li>
 *   <li><b>Highlighting Style:</b> Marks unused variables with {@link ProblemHighlightType#LIKE_UNUSED_SYMBOL}
 *       (rendered as grayed-out text in the editor).</li>
 *   <li><b>Quick Fix:</b> Offers {@link CompactRemoveUnusedVariableFix} to safely delete the dead statement.</li>
 * </ul>
 * </p>
 *
 * <h3>Edge Case Mitigations & Naming Conventions</h3>
 * <ul>
 *   <li><b>Underscore Ignored Variables:</b> Identifiers starting with an underscore (e.g. {@code _dummy},
 *       {@code _unused}) are intentionally ignored to allow conventional intentional discards.</li>
 *   <li><b>Parameter Exclusion:</b> Function, circuit, and constructor parameters are excluded from
 *       this local variable check.</li>
 *   <li><b>Callable Body Bounded:</b> Only variables declared inside a callable body block
 *       ({@link CompactBlock}) are evaluated.</li>
 * </ul>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> {@link ReferencesSearch} and PSI tree navigation require
 *       an active Read Action ({@code @RequiresReadLock}) on background daemon threads.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.inspection.fix.CompactRemoveUnusedVariableFix
 * @see dev.verloren.midnight.psi.CompactConstBindingImpl
 */
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
          // Step 1: Ignore parameters
          if (isParameter(element)) {
            return;
          }

          // Step 2: Ensure variable is declared within a local code block
          if (PsiTreeUtil.getParentOfType(element, CompactBlock.class) == null) {
            return;
          }

          // Step 3: Ensure block belongs to a callable (circuit, witness, constructor)
          if (!isInsideCallable(element)) {
            return;
          }

          CompactNamedElement namedElement = (CompactNamedElement) element;
          String name = namedElement.getName();
          // Step 4: Ignore unnamed or underscore-prefixed intentional discards (e.g. '_')
          if (name == null || name.isEmpty() || name.startsWith("_")) {
            return;
          }

          // Step 5: Search for references within the variable's lexical use-scope
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

  /**
   * Distinguishes local variables from callable parameters.
   */
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

  /**
   * Tests whether an element is inside a callable declaration body.
   */
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
