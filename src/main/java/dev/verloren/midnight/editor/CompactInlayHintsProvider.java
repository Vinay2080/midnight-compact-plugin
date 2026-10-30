package dev.verloren.midnight.editor;

import com.intellij.codeInsight.hints.declarative.*;
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
 * Compact source code using IntelliJ's Declarative Inlay Hints API.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as a {@code codeInsight.declarativeInlayProvider} extension for language {@code Compact}
 * in {@code plugin.xml}. Implements {@link InlayHintsProvider} and uses {@link SharedBypassCollector} to
 * inspect call expressions ({@link CompactCallExprImpl}) as the editor renders, inserting unobtrusive
 * parameter name tags (e.g. {@code recipient:}, {@code amount:}) directly in front of argument expressions.</p>
 *
 * <h3>Inlay Resolution Pipeline</h3>
 * <pre>
 *   [Editor Inlay Tree Collector]
 *                 │
 *                 ▼
 *   [CompactHintsCollector.collectFromElement(element)]
 *                 │
 *                 ├─► Not a CompactCallExprImpl? ─► Early Return
 *                 │
 *                 ▼
 *   [CompactInlayHintsProvider.computeHints(call)]
 *                 │
 *                 ├─► call.resolveCallee()
 *                 │     ├─► CompactCircuitDefinition ─────► circuit.getParameters()
 *                 │     ├─► CompactWitnessDeclaration ────► witness.getParameters()
 *                 │     └─► CompactConstructorDeclaration ► constructor.getParameters()
 *                 │
 *                 ├─► getCallArguments(call) (extracts call expression arguments)
 *                 │
 *                 ▼
 *   [Pair Parameters &amp; Arguments]
 *                 │
 *                 ├─► Argument text matches param name? (e.g. foo: foo) ─► Omit (redundant)
 *                 │
 *                 ▼
 *   [sink.addPresentation: InlineInlayPosition(offset, true, 0)]
 *                 │
 *                 ▼
 *          Renders "paramName":
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Collector and hint computation execute within a background read action
 *       managed by IntelliJ's declarative hints' engine.</li>
 * </ul>
 *
 * @see InlayHintsProvider
 * @see CompactCallExprImpl
 */
public class CompactInlayHintsProvider implements InlayHintsProvider {

  /**
   * Creates a new declarative inlay hints collector for the given file and editor.
   *
   * @param file the PSI file being viewed
   * @param editor the active editor instance
   * @return a new {@link CompactHintsCollector} instance
   */
  @Override
  public @Nullable InlayHintsCollector createCollector(@NotNull PsiFile file,
                                                        @NotNull Editor editor) {
    return new CompactHintsCollector();
  }

  /**
   * Declarative hints collector that traverses call expressions and emits inline parameter presentations.
   */
  private static final class CompactHintsCollector implements SharedBypassCollector {

    /**
     * Inspects a single PSI element, computing parameter hints if the element is a call expression.
     *
     * @param element candidate PSI element
     * @param sink inlay presentation sink
     */
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

  /**
   * Resolves the callee for a call expression, maps formal parameters to actual argument expressions,
   * and produces {@link HintInfo} records for non-redundant arguments.
   *
   * @param call the call expression PSI element
   * @return list of parameter hint items containing parameter name and argument start offset
   */
  public static @NotNull List<HintInfo> computeHints(@NotNull CompactCallExprImpl call) {
    // 1. Resolve callee definition to retrieve formal parameter definitions
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

    // 2. Extract actual call arguments from AST children
    List<PsiElement> arguments = getCallArguments(call);
    if (arguments.isEmpty()) {
      return List.of();
    }

    // 3. Align parameters with arguments, discarding matches where the argument text mirrors the parameter name
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

  /**
   * Extracts argument expressions from a call expression node, handling both direct argument lists
   * and comma-delimited expression sequences.
   *
   * @param call the call expression
   * @return list of argument {@link PsiElement} nodes
   */
  public static @NotNull List<PsiElement> getCallArguments(@NotNull CompactCallExprImpl call) {
    List<PsiElement> result = new ArrayList<>();
    boolean first = true;
    for (PsiElement child : call.getChildren()) {
      if (child instanceof CompactExpression) {
        if (first) {
          first = false; // First expression child is the callee reference
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

  /**
   * Checks whether the text of an argument matches its parameter name exactly, in which case
   * rendering a hint would be redundant (e.g. {@code amount: amount}).
   *
   * @param paramName the formal parameter identifier
   * @param arg the actual argument PSI element
   * @return {@code true} if argument text equals {@code paramName}; {@code false} otherwise
   */
  private static boolean isArgumentSameAsParam(@NotNull String paramName,
                                                @NotNull PsiElement arg) {
    return paramName.equals(arg.getText().trim());
  }

  /**
   * Data record representing an inlay hint to be rendered before an argument offset.
   *
   * @param label parameter name text
   * @param offset document character offset where the hint should be anchored
   */
  public record HintInfo(String label, int offset) {}
}
