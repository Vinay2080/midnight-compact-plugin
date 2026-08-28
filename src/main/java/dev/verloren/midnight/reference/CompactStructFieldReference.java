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

/**
 * Resolves struct field access expressions (e.g. {@code point.x}) to their {@link CompactStructFieldImpl} declaration.
 *
 * <p>Infers the static type of the base expression using {@link dev.verloren.midnight.psi.CompactExpression#getType()},
 * resolves the corresponding struct definition via {@link CompactResolveUtil#resolveType},
 * and matches the field identifier.</p>
 */
public class CompactStructFieldReference extends CompactReferenceBase {
  public CompactStructFieldReference(@NotNull CompactMemberExprImpl element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement);
  }

  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    CompactMemberExprImpl element = (CompactMemberExprImpl) getElement();
    if (element.getBaseExpression() == null) return ResolveResult.EMPTY_ARRAY;

    CompactType baseType = element.getBaseExpression().getType();
    String typeName = baseType.name();

    List<CompactNamedElement> typeDefs = CompactResolveUtil.resolveType(typeName, element);
    List<CompactNamedElement> fields = new ArrayList<>();

    for (CompactNamedElement typeDef : typeDefs) {
      if (typeDef instanceof dev.verloren.midnight.psi.CompactImportElementImpl importElement) {
        CompactNamedElement source = CompactResolveUtil.resolveImportElementSource(importElement);
        if (source != null) {
          typeDef = source;
        }
      }
      if (typeDef instanceof dev.verloren.midnight.psi.CompactTypeDefinitionImpl typeAlias) {
        CompactType targetType = typeAlias.getType();
        List<CompactNamedElement> aliasTargets = CompactResolveUtil.resolveType(targetType.name(), element);
        for (CompactNamedElement aliasTarget : aliasTargets) {
          if (aliasTarget instanceof dev.verloren.midnight.psi.CompactImportElementImpl aliasImport) {
            CompactNamedElement source = CompactResolveUtil.resolveImportElementSource(aliasImport);
            if (source != null) {
              aliasTarget = source;
            }
          }
          if (aliasTarget instanceof CompactStructDefinitionImpl) {
            for (CompactStructFieldImpl field : PsiTreeUtil.findChildrenOfType(aliasTarget, CompactStructFieldImpl.class)) {
              if (getValue().equals(field.getName())) {
                fields.add(field);
              }
            }
          }
        }
      }
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
