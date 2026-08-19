package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves qualified enum variant references (e.g. {@code State.Active}) to their {@link CompactEnumMemberImpl} declaration.
 *
 * <p>Extracts the base type name from the preceding member access expression, resolves the enum
 * definition via {@link CompactResolveUtil#resolveType}, and locates the matching variant member.</p>
 */
public class CompactEnumMemberReference extends CompactReferenceBase {
  public CompactEnumMemberReference(@NotNull CompactMemberExprImpl element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement);
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    CompactExpression base = ((CompactMemberExprImpl) getElement()).getBaseExpression();
    if (base == null) {
      return ResolveResult.EMPTY_ARRAY;
    }
    if (base.getNode().findChildByType(CompactTokenTypes.IDENTIFIER) == null) {
      return ResolveResult.EMPTY_ARRAY;
    }
    PsiElement baseIdentifier = Objects.requireNonNull(base.getNode().findChildByType(CompactTokenTypes.IDENTIFIER)).getPsi();
    List<CompactNamedElement> baseTargets = CompactResolveUtil.resolveType(baseIdentifier.getText(), getElement());
    List<CompactNamedElement> members = new ArrayList<>();
    for (CompactNamedElement baseTarget : baseTargets) {
      if (baseTarget instanceof CompactEnumDefinition) {
        for (CompactEnumMemberImpl member : PsiTreeUtil.findChildrenOfType(baseTarget, CompactEnumMemberImpl.class)) {
          if (getValue().equals(member.getName())) {
            members.add(member);
          }
        }
      }
    }
    return toResults(members);
  }
}
