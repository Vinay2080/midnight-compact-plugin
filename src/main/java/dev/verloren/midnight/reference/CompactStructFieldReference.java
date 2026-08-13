package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.CompactMemberExprImpl;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.psi.CompactStructDefinitionImpl;
import dev.verloren.midnight.psi.CompactStructFieldImpl;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.type.CompactType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CompactStructFieldReference extends CompactReferenceBase {
  public CompactStructFieldReference(@NotNull CompactMemberExprImpl element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement);
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    CompactMemberExprImpl element = (CompactMemberExprImpl) getElement();
    if (element.getBaseExpression() == null) return ResolveResult.EMPTY_ARRAY;

    CompactType baseType = element.getBaseExpression().getType();
    String typeName = baseType.getName();

    List<CompactNamedElement> typeDefs = CompactResolveUtil.resolveType(typeName, element);
    List<CompactNamedElement> fields = new ArrayList<>();

    for (CompactNamedElement typeDef : typeDefs) {
      if (typeDef instanceof CompactStructDefinitionImpl) {
        for (CompactStructFieldImpl field : PsiTreeUtil.findChildrenOfType(typeDef, CompactStructFieldImpl.class)) {
          if (getValue().equals(field.getName())) {
            fields.add(field);
          }
        }
      }
    }

    return toResults(fields);
  }
}
