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
 * Live template macro generating default circuit names ({@code circuit1}, {@code circuit2}, etc.).
 */
public class CompactCircuitNameMacro extends Macro {

  @Override
  public String getName() {
    return "circuitName";
  }

  @Override
  public String getPresentableName() {
    return "circuitName()";
  }

  @Override
  public @Nullable Result calculateResult(Expression @NotNull [] params, ExpressionContext context) {
    PsiElement psiElement = context.getPsiElementAtStartOffset();
    String generated = CompactDeclarationNameGenerator.generateName(CompactDeclarationType.CIRCUIT, psiElement);
    return new TextResult(generated);
  }

  @Override
  public @Nullable Result calculateQuickResult(Expression @NotNull [] params, ExpressionContext context) {
    return calculateResult(params, context);
  }
}
