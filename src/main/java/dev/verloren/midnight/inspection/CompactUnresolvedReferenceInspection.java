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
 * Inspection flagging unresolved identifiers, enum members, and struct fields in Compact source code.
 *
 * <p>Extends {@link LocalInspectionTool} and registers error annotations with {@link ProblemHighlightType#LIKE_UNKNOWN_SYMBOL}.
 * Guards against reporting false errors on {@link PsiErrorElement} subtrees during editing.</p>
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
        if (element instanceof PsiErrorElement || PsiTreeUtil.getParentOfType(element, PsiErrorElement.class) != null) {
          return;
        }

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
                // Only report unresolved field if base type is known and not UNKNOWN
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
