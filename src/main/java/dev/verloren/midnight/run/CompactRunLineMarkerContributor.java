package dev.verloren.midnight.run;

import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactExternalContractDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Displays clickable run/debug gutter icons alongside contract declarations and circuit definitions.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as a {@code runLineMarkerContributor} extension for language {@code Compact} in {@code plugin.xml}.
 * Extends {@link RunLineMarkerContributor} to render the green run triangle ({@link AllIcons.Actions#Execute})
 * directly in the editor gutter next to contract and circuit headers, offering immediate single-click compilation
 * of the containing Compact contract file.</p>
 *
 * <h3>Gutter Execution Pipeline</h3>
 * <pre>
 *   [Editor Gutter Line Marker Pass]
 *                  │
 *                  ▼
 *   [CompactRunLineMarkerContributor.getInfo(element)]
 *                  │
 *                  ├─► Element is IDENTIFIER token?
 *                  │     ├─► Parent is CompactExternalContractDeclaration?
 *                  │     │     └─► Info: "Compile Contract '&lt;name&gt;'"
 *                  │     │
 *                  │     └─► Parent is CompactCircuitDefinition?
 *                  │           └─► Info: "Compile File for Circuit '&lt;name&gt;'"
 *                  │
 *                  ▼
 *          [ExecutorAction.getActions(0)]
 *                  │
 *                  ▼
 *        Pops up Run / Debug menu
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Invoked on background daemon threads under a platform read lock during gutter rendering.</li>
 * </ul>
 *
 * @see CompactRunConfigurationProducer
 * @see CompactRunConfiguration
 */
public class CompactRunLineMarkerContributor extends RunLineMarkerContributor {

  /**
   * Computes run icon information and executor actions for contract and circuit identifier elements.
   *
   * @param element the candidate leaf PSI element in the editor
   * @return {@link Info} containing the execute icon and run actions, or {@code null} if element is not runnable
   */
  @Override
  public @Nullable Info getInfo(@NotNull PsiElement element) {
    if (element.getNode() != null && element.getNode().getElementType() == CompactTokenTypes.IDENTIFIER) {
      PsiElement parent = element.getParent();

      // 1. Contract declaration gutter marker
      if (parent instanceof CompactExternalContractDeclaration) {
        return new Info(
            AllIcons.Actions.Execute,
            ExecutorAction.getActions(0),
            ignored -> "Compile Contract '" + element.getText() + "'"
        );
      }

      // 2. Circuit definition gutter marker
      if (parent instanceof CompactCircuitDefinition) {
        return new Info(
            AllIcons.Actions.Execute,
            ExecutorAction.getActions(0),
            ignored -> "Compile File for Circuit '" + element.getText() + "'"
        );
      }
    }
    return null;
  }
}
