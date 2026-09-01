package dev.verloren.midnight.editor;

import com.intellij.codeInsight.hints.declarative.HintFormat;
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector;
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.codeInsight.hints.declarative.InlayTreeSink;
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition;
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides inline parameter-name hints for circuit, witness, and constructor call sites in
 * Compact code using the stable Declarative Inlay Hints API.
 */
public class CompactInlayHintsProvider implements InlayHintsProvider {

  @Override
  public @Nullable InlayHintsCollector createCollector(@NotNull PsiFile file,
                                                        @NotNull Editor editor) {
    return new CompactHintsCollector();
  }

  private static final class CompactHintsCollector implements SharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (!(element instanceof CompactCallExprImpl call)) return;

      List<HintInfo> hints = computeHints(call);
      for (HintInfo hint : hints) {
        sink.addPresentation(
            new InlineInlayPosition(hint.offset(), true, (short) 0),
            null,
            null,
            HintFormat.Companion.getDefault(),
            builder -> {
              builder.text(hint.label() + ":", null);
              return kotlin.Unit.INSTANCE;
            }
        );
      }
    }
  }

  public static @NotNull List<HintInfo> computeHints(@NotNull CompactCallExprImpl call) {
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
      return List.of();
    }

    List<PsiElement> arguments = getCallArguments(call);
    if (arguments.isEmpty()) {
      return List.of();
    }

    List<HintInfo> hints = new ArrayList<>();
    int count = Math.min(parameters.size(), arguments.size());
    for (int i = 0; i < count; i++) {
      String paramName = parameters.get(i).getName();
      PsiElement arg = arguments.get(i);
      if (paramName != null && !paramName.isEmpty() && !isArgumentSameAsParam(paramName, arg)) {
        hints.add(new HintInfo(paramName, arg.getTextOffset()));
      }
    }
    return hints;
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
      } else if (child.getNode() != null
          && child.getNode().getElementType() == CompactElementTypes.EXPRESSION_SEQUENCE) {
        for (PsiElement seqChild : child.getChildren()) {
          if (seqChild instanceof CompactExpression) {
            result.add(seqChild);
          }
        }
      }
    }
    return result;
  }

  private static boolean isArgumentSameAsParam(@NotNull String paramName,
                                                @NotNull PsiElement arg) {
    return paramName.equals(arg.getText().trim());
  }

  public record HintInfo(String label, int offset) {}
}
