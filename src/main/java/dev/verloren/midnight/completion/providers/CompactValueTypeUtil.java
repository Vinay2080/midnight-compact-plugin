package dev.verloren.midnight.completion.providers;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.intention.CompactSpecifyTypeExplicitlyIntention;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactConstBindingImpl;
import dev.verloren.midnight.psi.CompactConstructorDeclaration;
import dev.verloren.midnight.psi.CompactEnumDefinition;
import dev.verloren.midnight.psi.CompactEnumMemberImpl;
import dev.verloren.midnight.psi.CompactImportElementImpl;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.psi.CompactParameterImpl;
import dev.verloren.midnight.psi.CompactPatternImpl;
import dev.verloren.midnight.psi.CompactPsiUtil;
import dev.verloren.midnight.psi.CompactStructFieldImpl;
import dev.verloren.midnight.psi.CompactTypeElement;
import dev.verloren.midnight.psi.CompactWitnessDeclaration;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.type.CompactPrimitiveType;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Type inference and compatibility utility supporting type-directed value completion.
 */
public final class CompactValueTypeUtil {

  private CompactValueTypeUtil() {}

  /**
   * Infers the expected type at the current cursor by inspecting surrounding grammatical constructs.
   *
   * @param position the leaf PSI element at the caret
   * @return inferred {@link CompactType}, or {@code null} if unconstrained
   */
  public static @Nullable CompactType getExpectedType(@NotNull PsiElement position) {
    PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
    if (prev != null && prev.getNode() != null && prev.getNode().getElementType() == CompactTokenTypes.COLON) {
      PsiElement idBeforeColon = PsiTreeUtil.prevVisibleLeaf(prev);
      if (idBeforeColon != null && "is_left".equals(idBeforeColon.getText())) {
        return CompactPrimitiveType.BOOLEAN;
      }
    }
    if (prev != null && prev.getNode() != null && prev.getNode().getElementType() == CompactTokenTypes.LPAREN) {
      PsiElement beforeParen = PsiTreeUtil.prevVisibleLeaf(prev);
      if (beforeParen != null && beforeParen.getNode() != null) {
        IElementType tt = beforeParen.getNode().getElementType();
        if (tt == CompactTokenTypes.IF || tt == CompactTokenTypes.ASSERT) {
          return CompactPrimitiveType.BOOLEAN;
        }
      }
    }
    if (isReturnContext(position)) {
      PsiElement enclosing = PsiTreeUtil.getParentOfType(position,
          CompactCircuitDefinition.class,
          CompactWitnessDeclaration.class,
          CompactConstructorDeclaration.class);
      CompactType callableRt = CompactPsiUtil.getCallableReturnType(enclosing);
      if (!CompactPrimitiveType.UNKNOWN.equals(callableRt)) {
        return callableRt;
      }
    }
    CompactConstBindingImpl binding = PsiTreeUtil.getParentOfType(position, CompactConstBindingImpl.class);
    if (binding != null) {
      CompactTypeElement typeElem = CompactSpecifyTypeExplicitlyIntention.getDeclaredTypeElement(binding);
      if (typeElem != null) {
        return typeElem.getType();
      }
    }
    return null;
  }

  /**
   * Tests whether {@code position} is situated in a return expression slot.
   *
   * @param position the current caret element
   * @return {@code true} if inside a return context
   */
  public static boolean isReturnContext(@NotNull PsiElement position) {
    PsiElement prev = PsiTreeUtil.prevVisibleLeaf(position);
    if (prev != null && prev.getNode() != null && prev.getNode().getElementType() == CompactTokenTypes.RETURN) {
      return true;
    }
    PsiElement returnStmt = PsiTreeUtil.findFirstParent(position, false,
        p -> p.getNode() != null && p.getNode().getElementType() == CompactElementTypes.RETURN_STATEMENT);
    if (returnStmt != null) {
      return true;
    }
    for (PsiElement p = prev; p != null; p = PsiTreeUtil.prevVisibleLeaf(p)) {
      if (p.getNode() == null) break;
      IElementType tt = p.getNode().getElementType();
      if (tt == CompactTokenTypes.RETURN) {
        return true;
      }
      if (tt == CompactTokenTypes.SEMICOLON || tt == CompactTokenTypes.LBRACE || tt == CompactTokenTypes.RBRACE) {
        break;
      }
    }
    return false;
  }

  /**
   * Extracts the evaluated {@link CompactType} of a named candidate declaration.
   *
   * @param element the candidate named element
   * @return resolved type of the candidate
   */
  public static @NotNull CompactType getCandidateType(@NotNull CompactNamedElement element) {
    if (element instanceof CompactImportElementImpl importElem) {
      CompactNamedElement resolved = CompactResolveUtil.resolveImportElementSource(importElem);
      if (resolved != null) {
        return getCandidateType(resolved);
      }
    }
    CompactType callableRt = CompactPsiUtil.getCallableReturnType(element);
    if (!CompactPrimitiveType.UNKNOWN.equals(callableRt)) {
      return callableRt;
    }
    return switch (element) {
      case CompactParameterImpl param -> param.getType();
      case CompactConstBindingImpl constBinding -> constBinding.getType();
      case CompactPatternImpl pattern -> pattern.getType();
      case CompactStructFieldImpl field -> field.getType();
      case CompactEnumMemberImpl member -> member.getType();
      case CompactEnumDefinition enumDef ->
          new CompactPrimitiveType(enumDef.getName() != null ? enumDef.getName() : "Enum");
      default -> element.getType();
    };
  }

  /**
   * Checks whether a candidate expression type is assignable or compatible with the expected type.
   *
   * @param candidateType the type of the item being suggested
   * @param expectedType  the contextual expected type constraint
   * @return {@code true} if types are compatible
   */
  public static boolean isTypeCompatible(@NotNull CompactType candidateType, @Nullable CompactType expectedType) {
    if (expectedType == null || CompactPrimitiveType.UNKNOWN.equals(expectedType)) {
      return true;
    }
    if (CompactPrimitiveType.UNKNOWN.equals(candidateType)) {
      return true;
    }
    String expectedName = expectedType.name();
    String candidateName = candidateType.name();
    if ("Void".equalsIgnoreCase(expectedName)) {
      return "Void".equalsIgnoreCase(candidateName);
    }
    if ("Void".equalsIgnoreCase(candidateName)) {
      return false;
    }
    if (candidateType.isAssignableTo(expectedType) || expectedType.isAssignableTo(candidateType)) {
      return true;
    }
    if (expectedName.equalsIgnoreCase(candidateName)) {
      return true;
    }
    if ("Field".equalsIgnoreCase(expectedName) && ("Field".equalsIgnoreCase(candidateName) || "Uint".equalsIgnoreCase(candidateName))) {
      return true;
    }
    return (expectedName.startsWith("Uint") && candidateName.startsWith("Uint"))
        || (expectedName.startsWith("Vector") && candidateName.startsWith("Vector"));
  }
}
