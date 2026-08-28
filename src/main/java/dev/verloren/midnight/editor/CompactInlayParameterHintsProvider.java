package dev.verloren.midnight.editor;

import com.intellij.codeInsight.hints.InlayInfo;
import com.intellij.codeInsight.hints.InlayParameterHintsProvider;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Provides inline parameter name hints for circuit, witness, and constructor call sites in Compact code.
 */
@SuppressWarnings("UnstableApiUsage")
public class CompactInlayParameterHintsProvider implements InlayParameterHintsProvider {

  @Override
  public @NotNull List<InlayInfo> getParameterHints(@NotNull PsiElement element) {
    if (!(element instanceof CompactCallExprImpl call)) {
      return Collections.emptyList();
    }

    PsiElement resolved = call.resolveCallee();
    List<CompactNamedElement> parameters = null;
    if (resolved instanceof CompactCircuitDefinition circuit) {
      parameters = circuit.getParameters();
    } else if (resolved instanceof CompactWitnessDeclaration witness) {
      parameters = witness.getParameters();
    } else if (resolved instanceof CompactConstructorDeclaration constructor) {
      parameters = constructor.getParameters();
    }

    if (parameters == null || parameters.isEmpty()) {
      return Collections.emptyList();
    }

    List<PsiElement> arguments = getCallArguments(call);
    if (arguments.isEmpty()) {
      return Collections.emptyList();
    }

    List<InlayInfo> hints = new ArrayList<>();
    int count = Math.min(parameters.size(), arguments.size());
    for (int i = 0; i < count; i++) {
      CompactNamedElement param = parameters.get(i);
      String paramName = param.getName();
      PsiElement arg = arguments.get(i);
      if (paramName != null && !paramName.isEmpty() && !isArgumentSameAsParam(paramName, arg)) {
        hints.add(new InlayInfo(paramName, arg.getTextOffset()));
      }
    }
    return hints;
  }

  private static boolean isArgumentSameAsParam(@NotNull String paramName, @NotNull PsiElement arg) {
    String text = arg.getText().trim();
    return paramName.equals(text);
  }

  public static @NotNull List<PsiElement> getCallArguments(@NotNull CompactCallExprImpl call) {
    List<PsiElement> result = new ArrayList<>();
    boolean first = true;
    for (PsiElement child : call.getChildren()) {
      if (child instanceof CompactExpression) {
        if (first) {
          first = false;
        } else {
          result.add(child);
        }
      } else if (child.getNode() != null && child.getNode().getElementType() == CompactElementTypes.EXPRESSION_SEQUENCE) {
        for (PsiElement seqChild : child.getChildren()) {
          if (seqChild instanceof CompactExpression) {
            result.add(seqChild);
          }
        }
      }
    }
    return result;
  }

  @Override
  public @NotNull Set<String> getDefaultBlackList() {
    return Collections.emptySet();
  }
}
