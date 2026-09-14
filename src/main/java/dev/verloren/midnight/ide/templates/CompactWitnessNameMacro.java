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
 * Live template macro generating default witness names ({@code witness1}, {@code witness2}, etc.).
 */
public class CompactWitnessNameMacro extends Macro {

  @Override
  public String getName() {
    return "witnessName";
  }

  @Override
  public String getPresentableName() {
    return "witnessName()";
  }

  @Override
  public @Nullable Result calculateResult(Expression @NotNull [] params, ExpressionContext context) {
    PsiElement psiElement = context.getPsiElementAtStartOffset();
    String generated = CompactDeclarationNameGenerator.generateName(CompactDeclarationType.WITNESS, psiElement);
    return new TextResult(generated);
  }

  @Override
  public @Nullable Result calculateQuickResult(Expression @NotNull [] params, ExpressionContext context) {
    return calculateResult(params, context);
  }
}
