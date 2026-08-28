package dev.verloren.midnight.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * Registers {@link PsiReferenceProvider}s for Compact identifier leaf elements.
 *
 * <p>Enables direct reference resolution, Ctrl+B / Ctrl+Click declaration navigation,
 * and find usages on identifier tokens in expression and type positions.</p>
 */
public class CompactReferenceContributor extends PsiReferenceContributor {

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
        PlatformPatterns.psiElement(CompactTokenTypes.IDENTIFIER).withLanguage(CompactLanguage.INSTANCE),
        new PsiReferenceProvider() {
          @Override
          public PsiReference @NotNull [] getReferencesByElement(
              @NotNull PsiElement element,
              @NotNull ProcessingContext context
          ) {
            PsiElement parent = element.getParent();
            if (parent == null) {
              return PsiReference.EMPTY_ARRAY;
            }

            // Declaration name identifiers are definitions, not references
            if (parent instanceof CompactNamedElement named && element == named.getNameIdentifier()) {
              return PsiReference.EMPTY_ARRAY;
            }

            TextRange range = TextRange.from(0, element.getTextLength());

            if (parent instanceof CompactTypeReferenceImpl) {
              return new PsiReference[]{new CompactTypeReference(element, range)};
            }
            if (parent instanceof CompactStructLiteralExprImpl) {
              return new PsiReference[]{new CompactTypeReference(element, range)};
            }
            if (parent instanceof CompactReferenceExprImpl || parent instanceof CompactCallExprImpl) {
              return new PsiReference[]{new CompactValueReference(element, range)};
            }
            return switch (parent) {
              case CompactMemberExprImpl memberExpr ->
                      new PsiReference[]{new CompactStructFieldReference(memberExpr, range)};
              case CompactImportElementImpl _ ->
                      new PsiReference[]{new CompactImportReference(parent, range, CompactImportReference.Kind.IMPORT_ELEMENT)};
              case CompactExpression _ -> new PsiReference[]{new CompactValueReference(element, range)};
              default -> PsiReference.EMPTY_ARRAY;
            };

          }
        }
    );
  }
}
