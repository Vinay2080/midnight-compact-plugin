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
 * Extension point contributor that registers {@link PsiReferenceProvider}s for Compact identifier tokens.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered in {@code plugin.xml} under the {@code <psi.referenceContributor>} extension point:
 * <ul>
 *   <li><b>Registration Lifecycle:</b> IntelliJ invokes {@link #registerReferenceProviders(PsiReferenceRegistrar)}
 *       during plugin startup. The registrar binds a {@link PsiReferenceProvider} to leaf tokens matching
 *       {@link CompactTokenTypes#IDENTIFIER} in the {@link CompactLanguage#INSTANCE} language.</li>
 *   <li><b>Query Pipeline:</b> Whenever an IDE subsystem (such as "Go to Declaration", Find Usages,
 *       Parameter Info, or Code Inspections) queries references on a PSI node via
 *       {@link PsiElement#getReferences()} or {@link PsiReferenceService#getReferences(PsiElement, PsiReferenceService.Hints)},
 *       IntelliJ's reference service routes the request to this contributor's provider.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> {@code getReferencesByElement} requires an active Read Action
 *       ({@code @RequiresReadLock}).</li>
 *   <li><b>Background Execution:</b> Reference resolution queries originate from background worker
 *       threads (daemon code analyser) or on the EDT during explicit navigation actions.</li>
 * </ul>
 *
 * <h3>Reference Dispatch Architecture</h3>
 * <pre>
 *   [CompactTokenTypes.IDENTIFIER leaf token]
 *                     │
 *                     ▼
 *        [Inspect parent PSI element]
 *                     │
 *   ┌─────────────────┴───────────────────┬──────────────────────┬──────────────────────┐
 *   ▼                                     ▼                      ▼                      ▼
 * [Declaration Name?]            [Type Reference?]      [Member Access?]       [Expression?]
 *   │ (Yes)                               │ (Yes)                │ (Yes)                │ (Yes)
 *   ▼                                     ▼                      ▼                      ▼
 * [EMPTY_ARRAY]                 [CompactTypeReference]  [CompactStructField-   [CompactValueReference]
 * (Definitions have no refs)                             Reference]
 * </pre>
 *
 * <h3>Edge Case Mitigations</h3>
 * <ul>
 *   <li><b>Declaration Name Suppression:</b> If the identifier is the name token of a declaration
 *       (e.g. the identifier in {@code circuit foo()} or {@code struct Bar}), it represents a *definition*,
 *       not a *reference*. The provider suppresses references by returning {@link PsiReference#EMPTY_ARRAY},
 *       preventing circular self-references.</li>
 *   <li><b>Struct Literal vs Type Reference:</b> Both type annotations ({@link CompactTypeReferenceImpl})
 *       and struct literal headers ({@link CompactStructLiteralExprImpl}) resolve in the TYPE namespace,
 *       instantiating {@link CompactTypeReference}.</li>
 *   <li><b>Member Access Expressions:</b> Identifier leaf inside {@link CompactMemberExprImpl} dispatches
 *       to {@link CompactStructFieldReference} (which also handles enum members via fallback).</li>
 *   <li><b>Import Declarations:</b> Dispatches import elements to {@link CompactImportReference} with
 *       appropriate {@link CompactImportReference.Kind}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.reference.CompactReferenceBase
 * @see dev.verloren.midnight.reference.CompactTypeReference
 * @see dev.verloren.midnight.reference.CompactValueReference
 * @see dev.verloren.midnight.reference.CompactStructFieldReference
 * @see dev.verloren.midnight.reference.CompactImportReference
 */
public class CompactReferenceContributor extends PsiReferenceContributor {

  /**
   * Registers reference providers with the IntelliJ platform reference registrar.
   *
   * @param registrar the platform registrar where reference providers are bound to PSI patterns
   */
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
            // Step 1: Obtain the parent element in the AST
            PsiElement parent = element.getParent();
            if (parent == null) {
              return PsiReference.EMPTY_ARRAY;
            }

            // Step 2: Declaration name identifiers are definitions, not references (suppress self-references)
            if (parent instanceof CompactNamedElement named && element == named.getNameIdentifier()) {
              return PsiReference.EMPTY_ARRAY;
            }

            // Step 3: Compute text range spanning the entire leaf token
            TextRange range = TextRange.from(0, element.getTextLength());

            // Step 4: Type reference positions (explicit type signatures)
            if (parent instanceof CompactTypeReferenceImpl) {
              return new PsiReference[]{new CompactTypeReference(element, range)};
            }

            // Step 5: Struct literal headers (e.g. MyStruct { ... })
            if (parent instanceof CompactStructLiteralExprImpl) {
              return new PsiReference[]{new CompactTypeReference(element, range)};
            }

            // Step 6: Simple reference expressions and function/circuit call expressions
            if (parent instanceof CompactReferenceExprImpl || parent instanceof CompactCallExprImpl) {
              return new PsiReference[]{new CompactValueReference(element, range)};
            }

            // Step 7: Pattern matching on complex parent node types
            return switch (parent) {
              case CompactMemberExprImpl memberExpr ->
                      new PsiReference[]{new CompactStructFieldReference(memberExpr, range)};
              case CompactImportElementImpl _ ->
                      new PsiReference[]{new CompactImportReference(parent, range, CompactImportReference.Kind.IMPORT_ELEMENT)};
              case CompactImportDeclarationImpl importDecl when element == importDecl.getModuleIdentifier() ->
                      new PsiReference[]{new CompactImportReference(element, range, CompactImportReference.Kind.MODULE)};
              case CompactExpression _ -> new PsiReference[]{new CompactValueReference(element, range)};
              default -> PsiReference.EMPTY_ARRAY;
            };

          }
        }
    );
  }
}
