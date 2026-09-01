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

public class CompactRunLineMarkerContributor extends RunLineMarkerContributor {

  @Override
  public @Nullable Info getInfo(@NotNull PsiElement element) {
    if (element.getNode() != null && element.getNode().getElementType() == CompactTokenTypes.IDENTIFIER) {
      PsiElement parent = element.getParent();
      if (parent instanceof CompactExternalContractDeclaration) {
        return new Info(
            AllIcons.Actions.Execute,
            ExecutorAction.getActions(0),
            ignored -> "Compile Contract '" + element.getText() + "'"
        );
      }
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
