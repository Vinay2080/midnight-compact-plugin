package dev.verloren.midnight.ide.templates;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Macro;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Live template macro enabling data type completion lookups in declaration templates.
 *
 * <p>Usage in live templates:
 * <ul>
 *   <li>{@code compactType("State")} &rarr; defaults to {@code "State"}, opens type dropdown with built-in &amp; custom types.</li>
 *   <li>{@code compactType("Void")} &rarr; defaults to {@code "Void"}, opens type dropdown with built-in &amp; custom types.</li>
 *   <li>{@code compactType()} &rarr; defaults to {@code "Field"}, opens type dropdown with built-in &amp; custom types.</li>
 * </ul>
 * </p>
 */
public class CompactTypeMacro extends Macro {

  @Override
  public String getName() {
    return "compactType";
  }

  @Override
  public String getPresentableName() {
    return "compactType(defaultType)";
  }

  @Override
  public @Nullable Result calculateResult(Expression @NotNull [] params, ExpressionContext context) {
    String defaultType = "Field";
    if (params.length > 0) {
      Result paramResult = params[0].calculateResult(context);
      if (paramResult != null && !paramResult.toString().isEmpty()) {
        defaultType = paramResult.toString();
      }
    }
    return new TextResult(defaultType);
  }

  @Override
  public @Nullable Result calculateQuickResult(Expression @NotNull [] params, ExpressionContext context) {
    return calculateResult(params, context);
  }

  @Override
  public LookupElement @Nullable [] calculateLookupItems(Expression @NotNull [] params, ExpressionContext context) {
    String defaultType = "Field";
    if (params.length > 0) {
      Result paramResult = params[0].calculateResult(context);
      if (paramResult != null && !paramResult.toString().isEmpty()) {
        defaultType = paramResult.toString();
      }
    }
    return new CompactTypeExpression(defaultType).calculateLookupItems(context);
  }
}
