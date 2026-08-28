package dev.verloren.midnight.editor;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;

import com.intellij.psi.PsiElement;
import dev.verloren.midnight.lexer.CompactTokenTypes;

import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactLedgerDeclaration;
import dev.verloren.midnight.psi.CompactWitnessDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Line marker provider adding visual gutter indicators for ZK boundaries, private witnesses, circuits, and ledger fields.
 */
public class CompactLineMarkerProvider implements LineMarkerProvider {

  @Override
  public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
    if (element.getNode() == null) {
      return null;
    }

    // 1. Check for a disclosed keyword token
    if (element.getNode().getElementType() == CompactTokenTypes.DISCLOSE) {
      return new LineMarkerInfo<>(
          element,
          element.getTextRange(),
          AllIcons.Nodes.KeymapEditor,
          _ -> "Zero-Knowledge boundary: disclosing private witness data into circuit",
          null,
          GutterIconRenderer.Alignment.RIGHT,
          () -> "ZK Disclosure Boundary"
      );
    }

    // 2. Check identifier tokens of declarations
    if (element.getNode().getElementType() == CompactTokenTypes.IDENTIFIER) {
      PsiElement parent = element.getParent();
      if (parent instanceof CompactWitnessDeclaration) {
        return new LineMarkerInfo<>(
            element,
            element.getTextRange(),
            AllIcons.Nodes.AbstractMethod,
            _ -> "Private off-chain witness query '" + element.getText() + "'",
            null,
            GutterIconRenderer.Alignment.RIGHT,
            () -> "Private Witness Query"
        );
      }
      if (parent instanceof CompactCircuitDefinition circuit && circuit.isExported()) {
        return new LineMarkerInfo<>(
            element,
            element.getTextRange(),
            AllIcons.Actions.Lightning,
            _ -> "Exported on-chain ZK circuit '" + element.getText() + "'",
            null,
            GutterIconRenderer.Alignment.RIGHT,
            () -> "Exported ZK Circuit"
        );
      }
      if (parent instanceof CompactLedgerDeclaration ledger) {
        String typeDesc = ledger.isSealed() ? "Sealed on-chain ledger state" : "On-chain ledger state";
        return new LineMarkerInfo<>(
            element,
            element.getTextRange(),
            AllIcons.Nodes.DataTables,
            _ -> typeDesc + " '" + element.getText() + "'",
            null,
            GutterIconRenderer.Alignment.RIGHT,
            () -> "Ledger State"
        );
      }
    }

    return null;
  }
}
