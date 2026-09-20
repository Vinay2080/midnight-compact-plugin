package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.reference.CompactEnumMemberReference;
import dev.verloren.midnight.reference.CompactStructFieldReference;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactMemberExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactMemberExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    PsiReference ref = getReference();
    if (ref != null) {
      PsiElement resolved = ref.resolve();
      CompactType rawType = null;
      if (resolved instanceof CompactTypeElement typeElement) {
        rawType = typeElement.getType();
      } else if (resolved instanceof CompactNamedElement named) {
        rawType = named.getType();
      }
      if (rawType != null) {
        CompactExpression base = getBaseExpression();
        if (base != null) {
          CompactType baseType = base.getType();
          java.util.List<String> genericArgs = dev.verloren.midnight.type.CompactTypeInferenceUtil.parseGenericArgs(baseType.name());
          PsiElement container = resolved instanceof CompactStructFieldImpl
              ? com.intellij.psi.util.PsiTreeUtil.getParentOfType(resolved, CompactStructDefinitionImpl.class)
              : null;
          if (container != null && !genericArgs.isEmpty()) {
            java.util.Map<String, String> substitution = dev.verloren.midnight.type.CompactTypeInferenceUtil.buildGenericSubstitution(container, genericArgs);
            String substitutedName = dev.verloren.midnight.type.CompactTypeInferenceUtil.substituteGenerics(rawType.name(), substitution);
            return new CompactPrimitiveType(substitutedName);
          }
        }
        return rawType;
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }

  @Override
  public @Nullable PsiReference getReference() {
    PsiElement member = getMemberIdentifier();
    if (member == null) {
      return null;
    }

    int start = member.getStartOffsetInParent();
    PsiElement baseIdentifier = getBaseIdentifier();
    if (baseIdentifier != null) {
      java.util.List<CompactNamedElement> baseTargets = new java.util.ArrayList<>();
      baseTargets.addAll(CompactResolveUtil.resolveType(baseIdentifier.getText(), this));
      baseTargets.addAll(CompactResolveUtil.resolveValue(baseIdentifier.getText(), this));
      for (CompactNamedElement target : baseTargets) {
        if (target instanceof CompactImportElementImpl) {
          target = CompactResolveUtil.resolveImportElementSource((CompactImportElementImpl) target);
        }
        if (target instanceof CompactEnumDefinition) {
          return new CompactEnumMemberReference(this, TextRange.from(start, member.getTextLength()));
        }
      }
    }

    if (getBaseExpression() != null) {
      return new CompactStructFieldReference(this, TextRange.from(start, member.getTextLength()));
    }

    return null;
  }

  public @Nullable PsiElement getMemberIdentifier() {
    ASTNode[] nodes = getNode().getChildren(TokenSet.create(CompactTokenTypes.IDENTIFIER));
    PsiElement[] identifiers = new PsiElement[nodes.length];
    for (int i = 0; i < nodes.length; i++) {
      identifiers[i] = nodes[i].getPsi();
    }
    return identifiers.length == 0 ? null : identifiers[identifiers.length - 1];
  }

  private @Nullable PsiElement getBaseIdentifier() {
    CompactExpression base = getBaseExpression();
    if (base == null) {
      return null;
    }
    ASTNode identifier = base.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getPsi();
  }

  public @Nullable CompactExpression getBaseExpression() {
    for (PsiElement child : getChildren()) {
      if (child instanceof CompactExpression) {
        return (CompactExpression) child;
      }
    }
    return null;
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}
