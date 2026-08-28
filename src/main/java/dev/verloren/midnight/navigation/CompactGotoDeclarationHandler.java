package dev.verloren.midnight.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Handles Ctrl+B / Ctrl+Click navigation to declaration targets in Compact files,
 * including bundled standard library and ZKIR primitives.
 */
public class CompactGotoDeclarationHandler implements GotoDeclarationHandler {

  @Override
  public PsiElement @Nullable [] getGotoDeclarationTargets(
      @Nullable PsiElement sourceElement,
      int offset,
      Editor editor
  ) {
    if (sourceElement == null || !sourceElement.isValid() || sourceElement.getLanguage() != CompactLanguage.INSTANCE) {
      return null;
    }

    // 1. Try references directly on the sourceElement
    for (PsiReference ref : sourceElement.getReferences()) {
      PsiElement target = ref.resolve();
      if (target != null) {
        return new PsiElement[]{target};
      }
    }

    // 2. Try reference on the direct parent (e.g. CompactReferenceExpr, CompactTypeReference, CompactCallExpr)
    PsiElement parent = sourceElement.getParent();
    if (parent != null) {
      for (PsiReference ref : parent.getReferences()) {
        PsiElement target = ref.resolve();
        if (target != null) {
          return new PsiElement[]{target};
        }
      }
    }

    // 3. If sourceElement is an identifier token, resolve through semantic namespaces
    if (sourceElement.getNode() != null && sourceElement.getNode().getElementType() == CompactTokenTypes.IDENTIFIER) {
      String name = sourceElement.getText();
      // Try value resolution (circuits, witnesses, ledgers, variables)
      List<CompactNamedElement> values = CompactResolveUtil.resolveValue(name, sourceElement);
      if (!values.isEmpty()) {
        return values.toArray(PsiElement.EMPTY_ARRAY);
      }

      // Try type resolution (structs, enums, type aliases, contracts)
      List<CompactNamedElement> types = CompactResolveUtil.resolveType(name, sourceElement);
      if (!types.isEmpty()) {
        return types.toArray(PsiElement.EMPTY_ARRAY);
      }
    }

    return null;
  }
}
