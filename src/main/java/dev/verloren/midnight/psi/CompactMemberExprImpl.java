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

/**
 * PSI implementation of a member access expression wrapping an AST composite node.
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Subsystem Role</h3>
 * <p>Instantiated by {@link CompactElementFactory#createElement(ASTNode)} when parsing
 * {@link dev.verloren.midnight.parser.CompactElementTypes#MEMBER_EXPR}.
 * Handles dot-notation member navigation ({@code base.member}) for both:
 * <ul>
 *   <li><b>Enum Member Access:</b> Qualified access to enumeration variants (e.g. {@code State.Active}).</li>
 *   <li><b>Struct Field Access:</b> Instance field access on struct values (e.g. {@code account.balance}).</li>
 * </ul>
 * </p>
 *
 * <h3>Generic Field Specialization</h3>
 * <p>When accessing fields of generic struct types (e.g. {@code Box<Field>.value}):
 * <ol>
 *   <li>Infers the specialized {@link CompactType} of the base expression {@link #getBaseExpression()}.</li>
 *   <li>Extracts type argument tokens via {@link dev.verloren.midnight.type.CompactTypeInferenceUtil#parseGenericArgs}.</li>
 *   <li>Maps parent struct generic type parameters to argument types via {@link dev.verloren.midnight.type.CompactTypeInferenceUtil#buildGenericSubstitution}.</li>
 *   <li>Substitutes type variables in the field's declared type via {@link dev.verloren.midnight.type.CompactTypeInferenceUtil#substituteGenerics}.</li>
 * </ol>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Calling {@link #getType()}, {@link #getReference()}, and navigation helpers requires
 *       an active platform Read Action ({@code @RequiresReadLock}).</li>
 *   <li><b>Write Action:</b> Read-only expression; modifications occur via editor commands.</li>
 * </ul>
 *
 * @see CompactExpression
 * @see CompactStructFieldReference
 * @see CompactEnumMemberReference
 */
public class CompactMemberExprImpl extends CompactPsiElement implements CompactExpression {
  /**
   * Constructs a member access expression PSI node wrapping the AST composite node.
   *
   * @param node the AST node representing the member expression
   */
  public CompactMemberExprImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Resolves the static {@link CompactType} of the accessed member, applying generic substitutions if needed.
   *
   * @return the resolved member {@link CompactType}, or {@link CompactPrimitiveType#UNKNOWN} if unresolvable
   */
  @Override
  public @NotNull CompactType getType() {
    PsiReference ref = getReference();
    if (ref != null) {
      PsiElement resolved = ref.resolve();
      CompactType rawType = null;
      if (resolved instanceof CompactNamedElement named) {
        rawType = named.getType();
      } else if (resolved instanceof CompactTypeElement typeElement) {
        rawType = typeElement.getType();
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

  /**
   * Resolves the reference targeting the right-hand member identifier.
   *
   * <p>Inspects the left-hand base qualifier:
   * <ul>
   *   <li>If the base resolves to a {@link CompactEnumDefinition}, returns a {@link CompactEnumMemberReference}.</li>
   *   <li>If the base is an expression, returns a {@link CompactStructFieldReference}.</li>
   * </ul>
   * </p>
   *
   * @return the resolved {@link PsiReference}, or {@code null} if the member cannot be identified
   */
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

  /**
   * Locates the right-hand member identifier leaf node within this member expression.
   *
   * @return the member {@link PsiElement} identifier, or {@code null} if absent
   */
  public @Nullable PsiElement getMemberIdentifier() {
    ASTNode[] nodes = getNode().getChildren(TokenSet.create(CompactTokenTypes.IDENTIFIER));
    PsiElement[] identifiers = new PsiElement[nodes.length];
    for (int i = 0; i < nodes.length; i++) {
      identifiers[i] = nodes[i].getPsi();
    }
    return identifiers.length == 0 ? null : identifiers[identifiers.length - 1];
  }

  /**
   * Locates the left-hand base identifier leaf node within this member expression's base expression.
   *
   * @return the base {@link PsiElement} identifier, or {@code null} if absent
   */
  private @Nullable PsiElement getBaseIdentifier() {
    CompactExpression base = getBaseExpression();
    if (base == null) {
      return null;
    }
    ASTNode identifier = base.getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return identifier == null ? null : identifier.getPsi();
  }

  /**
   * Returns the qualifying left-hand base expression.
   *
   * @return the base {@link CompactExpression}, or {@code null} if absent
   */
  public @Nullable CompactExpression getBaseExpression() {
    for (PsiElement child : getChildren()) {
      if (child instanceof CompactExpression) {
        return (CompactExpression) child;
      }
    }
    return null;
  }

  /**
   * Returns all PSI references attached to this member expression.
   *
   * @return an array containing the member reference, or {@link PsiReference#EMPTY_ARRAY}
   */
  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiReference ref = getReference();
    return ref != null ? new PsiReference[]{ref} : PsiReference.EMPTY_ARRAY;
  }
}
