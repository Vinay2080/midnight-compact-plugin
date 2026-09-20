package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactCallExprImpl extends CompactPsiElement implements CompactExpression {
  public CompactCallExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull CompactType getType() {
    PsiReference ref = getCalleeReference();
    if (ref != null) {
      PsiElement resolved = ref.resolve();
      CompactType returnType = null;
      if (resolved instanceof CompactCircuitDefinition circuit) {
        returnType = circuit.getType();
      } else if (resolved instanceof CompactWitnessDeclaration witness) {
        returnType = witness.getType();
      } else if (resolved instanceof CompactTypeElement typeElement) {
        returnType = typeElement.getType();
      } else if (resolved instanceof CompactNamedElement named) {
        returnType = named.getType();
      }

      if (returnType != null) {
        ASTNode genArgsNode = getNode().findChildByType(dev.verloren.midnight.parser.CompactElementTypes.GENERIC_ARGUMENT_LIST);
        if (genArgsNode != null && resolved != null) {
          java.util.List<String> genericArgs = dev.verloren.midnight.type.CompactTypeInferenceUtil.parseGenericArgs(genArgsNode.getText());
          java.util.Map<String, String> substitution = dev.verloren.midnight.type.CompactTypeInferenceUtil.buildGenericSubstitution(resolved, genericArgs);
          String substitutedName = dev.verloren.midnight.type.CompactTypeInferenceUtil.substituteGenerics(returnType.name(), substitution);
          return new CompactPrimitiveType(substitutedName);
        }
        return returnType;
      }
    }
    return CompactPrimitiveType.UNKNOWN;
  }

  public @Nullable PsiReference getCalleeReference() {
    CompactReferenceExprImpl refExpr = com.intellij.psi.util.PsiTreeUtil.getChildOfType(this, CompactReferenceExprImpl.class);
    if (refExpr != null) {
      return refExpr.getReference();
    }
    return getReference();
  }

  public @Nullable PsiElement resolveCallee() {
    PsiReference ref = getCalleeReference();
    return ref != null ? ref.resolve() : null;
  }


  @Override
  public @Nullable PsiReference getReference() {
    return CompactPsiUtil.createIdentifierValueReference(this);
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}
