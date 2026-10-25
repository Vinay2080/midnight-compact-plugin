package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactExpression;
import dev.verloren.midnight.psi.CompactMemberExprImpl;
import dev.verloren.midnight.psi.CompactReferenceExprImpl;
import dev.verloren.midnight.reference.CompactEnumMemberReference;
import dev.verloren.midnight.reference.CompactStructFieldReference;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection flagging unresolved identifiers, imported symbols, enum members, and struct fields in Compact source code.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Forms the core static unresolved symbol diagnostic for the plugin:
 * <ul>
 *   <li><b>Highlighting Style:</b> Marks unresolved tokens with {@link ProblemHighlightType#LIKE_UNKNOWN_SYMBOL}
 *       (typically rendered as red wavy underlines in the editor).</li>
 *   <li><b>Scope of Analysis:</b>
 *       <ol>
 *         <li>Unresolved reference expressions ({@link CompactReferenceExprImpl}).</li>
 *         <li>Unresolved selectively imported symbols ({@link dev.verloren.midnight.psi.CompactImportElementImpl}).</li>
 *         <li>Unresolved enum variant references ({@link CompactEnumMemberReference}).</li>
 *         <li>Unresolved struct fields ({@link CompactStructFieldReference}).</li>
 *       </ol>
 *   </li>
 * </ul>
 * </p>
 *
 * <h3>Edge Case & Broken Syntax Mitigations</h3>
 * <ul>
 *   <li><b>Error Element Guard:</b> Completely skips subtrees containing {@link PsiErrorElement}
 *       to prevent reporting cascade errors while the user is actively typing broken syntax.</li>
 *   <li><b>Unknown Base Suppression:</b> In struct field access (e.g. {@code obj.field}), unresolved field
 *       errors are only reported if {@code obj}'s type is known and resolved (not {@link CompactPrimitiveType#UNKNOWN}).
 *       This avoids false-positive field errors when the root variable itself is unresolved or invalid.</li>
 * </ul>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> Executes within {@code @RequiresReadLock} on daemon worker threads.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.reference.CompactReferenceBase
 * @see dev.verloren.midnight.reference.CompactStructFieldReference
 * @see dev.verloren.midnight.reference.CompactEnumMemberReference
 */
public class CompactUnresolvedReferenceInspection extends LocalInspectionTool {

  @Override
  public String getStaticDescription() {
    return "Reports unresolved references in Compact source code.";
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        // Step 1: Suppress false-positives in broken syntax subtrees
        if (element instanceof PsiErrorElement || PsiTreeUtil.getParentOfType(element, PsiErrorElement.class) != null) {
          return;
        }

        // Step 2: Check unresolved reference expressions, imported symbols, and member accesses
        switch (element) {
          case CompactReferenceExprImpl refExpr -> {
            PsiReference ref = refExpr.getReference();
            if (ref != null && ref.resolve() == null) {
              holder.registerProblem(
                      refExpr,
                      "Unresolved reference '" + ref.getCanonicalText() + "'",
                      ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
              );
            }
          }
          case dev.verloren.midnight.psi.CompactImportElementImpl importElement -> {
            PsiReference ref = importElement.getReference();
            if (ref != null && ref.resolve() == null) {
              PsiElement id = importElement.getNameIdentifier();
              PsiElement target = id != null ? id : importElement;
              holder.registerProblem(
                      target,
                      "Unresolved imported symbol '" + importElement.getName() + "'",
                      ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
              );
            }
          }
          case CompactMemberExprImpl memberExpr -> {
            PsiReference ref = memberExpr.getReference();
            if (ref instanceof CompactEnumMemberReference) {
              if (ref.resolve() == null) {
                PsiElement memberId = memberExpr.getMemberIdentifier();
                PsiElement target = memberId != null ? memberId : memberExpr;
                holder.registerProblem(
                        target,
                        "Unresolved enum member '" + ref.getCanonicalText() + "'",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                );
              }
            } else if (ref instanceof CompactStructFieldReference) {
              CompactExpression base = memberExpr.getBaseExpression();
              if (base != null) {
                CompactType baseType = base.getType();
                // Step 3: Only report unresolved field if base type is known and not UNKNOWN
                if (!CompactPrimitiveType.UNKNOWN.equals(baseType) && ref.resolve() == null) {
                  PsiElement memberId = memberExpr.getMemberIdentifier();
                  PsiElement target = memberId != null ? memberId : memberExpr;
                  holder.registerProblem(
                          target,
                          "Unresolved struct field '" + ref.getCanonicalText() + "'",
                          ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                  );
                }
              }
            }
          }
          default -> {
          }
        }
      }
    };
  }
}
