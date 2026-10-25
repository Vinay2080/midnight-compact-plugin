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
import dev.verloren.midnight.type.CompactTypeInferenceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves struct field access expressions (e.g. {@code point.x}) to their {@link CompactStructFieldImpl} declaration.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Invoked when an identifier is used as the selector in a dot-access member expression:
 * <ul>
 *   <li><b>Navigation & Highlighting:</b> Powers Ctrl+Click navigation from field access expressions
 *       to the exact field declaration in the corresponding struct definition.</li>
 *   <li><b>Rename Refactoring:</b> Ensures that renaming a struct field updates all dot-access references
 *       throughout the project.</li>
 *   <li><b>Code Inspections:</b> Validates that the accessed field exists on the inferred type of the base
 *       expression, flagging unresolved fields with inspection warnings.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> {@link #resolveInner()} must be executed inside a Read Action
 *       ({@code @RequiresReadLock}).</li>
 *   <li><b>Background Execution:</b> Runs on background daemon threads during inspections or on the EDT
 *       during interactive navigation.</li>
 * </ul>
 *
 * <h3>Resolution Pipeline Diagram</h3>
 * <pre>
 *   [Member Expr: point.x]
 *             │
 *             ▼
 *    [Infer Base Type: Point]
 *             │
 *             ▼
 *   [Extract Raw Type Name]
 *             │
 *             ▼
 *    [Resolve Struct Def (handling aliases & imports)]
 *             │
 *             ▼
 *   [Scan Struct Fields for Match: 'x']
 * </pre>
 *
 * <h3>Edge Case Mitigations</h3>
 * <ul>
 *   <li><b>Type Alias Unwrapping:</b> If the inferred type refers to a type alias
 *       ({@link dev.verloren.midnight.psi.CompactTypeDefinitionImpl}), the reference unwraps the target type
 *       transitively to locate the underlying {@link CompactStructDefinitionImpl}.</li>
 *   <li><b>Parameterized Struct Handling:</b> Uses {@link CompactTypeInferenceUtil#getRawTypeName(String)}
 *       to strip generic type arguments (e.g., {@code Point<Field>} becomes {@code Point}) prior to
 *       type resolution.</li>
 *   <li><b>Import Unwrapping:</b> If the struct definition was imported selectively, resolves the import
 *       source before scanning fields.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.reference.CompactReferenceBase
 * @see dev.verloren.midnight.psi.CompactMemberExprImpl
 * @see dev.verloren.midnight.psi.CompactStructFieldImpl
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 * @see dev.verloren.midnight.type.CompactTypeInferenceUtil
 */
public class CompactStructFieldReference extends CompactReferenceBase {

  /**
   * Constructs a struct field reference.
   *
   * @param element        the member access expression host
   * @param rangeInElement text range of the field selector within the element
   */
  public CompactStructFieldReference(@NotNull CompactMemberExprImpl element, @NotNull TextRange rangeInElement) {
    super(element, rangeInElement);
  }

  /**
   * Resolves the struct field declaration by inferring the base expression's type and locating
   * matching fields within the resolved struct.
   *
   * @return array of resolve results matching the field declaration
   */
  @Override
  protected ResolveResult @NotNull [] resolveInner() {
    CompactMemberExprImpl element = (CompactMemberExprImpl) getElement();
    // Step 1: Guard against incomplete syntax where base expression is missing
    if (element.getBaseExpression() == null) return ResolveResult.EMPTY_ARRAY;

    // Step 2: Infer static type of base expression and obtain raw type name
    CompactType baseType = element.getBaseExpression().getType();
    String typeName = baseType.name();
    String rawTypeName = CompactTypeInferenceUtil.getRawTypeName(typeName);

    // Step 3: Resolve type definitions matching the base type name
    List<CompactNamedElement> typeDefs = CompactResolveUtil.resolveType(rawTypeName, element);
    List<CompactNamedElement> fields = new ArrayList<>();

    // Step 4: Inspect resolved type definitions, unwrapping imports and aliases
    for (CompactNamedElement typeDef : typeDefs) {
      if (typeDef instanceof dev.verloren.midnight.psi.CompactImportElementImpl importElement) {
        CompactNamedElement source = CompactResolveUtil.resolveImportElementSource(importElement);
        if (source != null) {
          typeDef = source;
        }
      }
      // Handle type alias definitions pointing to structs
      if (typeDef instanceof dev.verloren.midnight.psi.CompactTypeDefinitionImpl typeAlias) {
        CompactType targetType = typeAlias.getType();
        String rawTargetName = CompactTypeInferenceUtil.getRawTypeName(targetType.name());
        List<CompactNamedElement> aliasTargets = CompactResolveUtil.resolveType(rawTargetName, element);
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
      // Handle direct struct definitions
      if (typeDef instanceof CompactStructDefinitionImpl) {
        for (CompactStructFieldImpl field : PsiTreeUtil.findChildrenOfType(typeDef, CompactStructFieldImpl.class)) {
          if (getValue().equals(field.getName())) {
            fields.add(field);
          }
        }
      }
    }

    // Step 5: Convert matching field elements to ResolveResult array
    return toResults(fields);
  }
}
