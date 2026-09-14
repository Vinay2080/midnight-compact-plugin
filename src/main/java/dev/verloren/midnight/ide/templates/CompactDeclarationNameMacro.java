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
 *
 * <p>Usage in live templates:
 * <ul>
 *   <li>{@code compactDeclarationName("circuit")} &rarr; {@code "circuit1"}, {@code "circuit2"}, etc.</li>
 *   <li>{@code compactDeclarationName("witness")} &rarr; {@code "witness1"}, {@code "witness2"}, etc.</li>
 *   <li>{@code compactDeclarationName("struct")} &rarr; {@code "struct1"}, etc.</li>
 *   <li>{@code compactDeclarationName()} &rarr; automatically deduced from preceding keyword in context.</li>
 * </ul>
 * </p>
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
    int startOffset = context.getStartOffset();
    String generated = CompactDeclarationNameGenerator.generateName(baseName, null, psiElement, startOffset);
    return new TextResult(generated);
  }

  @Override
  public @Nullable Result calculateQuickResult(Expression @NotNull [] params, ExpressionContext context) {
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
