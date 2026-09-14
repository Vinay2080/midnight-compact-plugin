package dev.verloren.midnight.ide.templates;

import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Macro;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Universal live template macro generating auto-numbered declaration names based on declaration type.
 */
public class CompactDeclarationNameMacro extends Macro {

  @Override
  public String getName() {
    return "compactDeclarationName";
  }

  @Override
  public String getPresentableName() {
    return "compactDeclarationName(declarationType)";
  }

  @Override
  public @Nullable Result calculateResult(Expression @NotNull [] params, ExpressionContext context) {
    String typeKey = null;
    if (params.length > 0) {
      Result paramResult = params[0].calculateResult(context);
      if (paramResult != null) {
        typeKey = paramResult.toString();
      }
    }

    PsiElement psiElement = context.getPsiElementAtStartOffset();
    if (typeKey == null || typeKey.isBlank()) {
      typeKey = deduceDeclarationType(psiElement);
    }

    String baseName = typeKey != null ? CompactDeclarationType.resolveBaseName(typeKey) : "declaration";
    System.out.println("DEBUG calculateResult: psiElement=" + psiElement + " text='" + (psiElement != null ? psiElement.getText() : null) + "'");
    if (psiElement != null) {
      System.out.println("DEBUG calculateResult: scopeRoot=" + CompactDeclarationNameGenerator.findScopeRoot(psiElement) + " scopeText='" + (CompactDeclarationNameGenerator.findScopeRoot(psiElement) != null ? CompactDeclarationNameGenerator.findScopeRoot(psiElement).getText() : null) + "'");
      System.out.println("DEBUG calculateResult: existingNames=" + CompactDeclarationNameGenerator.collectExistingNamesInScope(psiElement));
    }
    String generated = CompactDeclarationNameGenerator.generateName(baseName, psiElement);
    System.out.println("DEBUG calculateResult: generated=" + generated);
    return new TextResult(generated);
  }

  @Override
  public @Nullable Result calculateQuickResult(Expression @NotNull [] params, ExpressionContext context) {
    System.out.println("DEBUG calculateQuickResult called");
    return calculateResult(params, context);
  }

  private static @Nullable String deduceDeclarationType(@Nullable PsiElement element) {
    if (element == null) {
      return null;
    }
    PsiElement prev = element.getPrevSibling();
    while (prev != null && prev.getText().trim().isEmpty()) {
      prev = prev.getPrevSibling();
    }
    if (prev != null) {
      CompactDeclarationType type = CompactDeclarationType.fromKeyword(prev.getText().trim());
      if (type != null) {
        return type.getBaseName();
      }
    }
    return null;
  }
}
