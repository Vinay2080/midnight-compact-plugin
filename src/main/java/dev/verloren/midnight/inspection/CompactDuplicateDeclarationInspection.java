package dev.verloren.midnight.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Inspection reporting duplicate declaration identifiers declared within the same lexical scope.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered under {@code <localInspection>} in {@code plugin.xml}:
 * <ul>
 *   <li><b>Daemon Execution:</b> Invoked during on-the-fly highlighting to warn developers when
 *       re-declaring existing variable, circuit, or type identifiers.</li>
 *   <li><b>Namespace Isolation:</b> Strictly partitions declarations into two distinct buckets:
 *       {@code valueDeclarations} (variables, parameters, circuits, ledger, witnesses) and
 *       {@code typeDeclarations} (structs, enums, type aliases, contracts). Sharing a name across
 *       different namespaces is legal in Compact grammar.</li>
 *   <li><b>Scope Attribution:</b> Correctly maps every {@link CompactNamedElement} to its exact owning
 *       lexical container (e.g. struct fields belong to their struct, local variables belong to their block).</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action Mandatory:</b> Executes within a Read Action ({@code @RequiresReadLock}) on pooled
 *       daemon worker threads.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.resolve.CompactResolveUtil.Namespace
 * @see dev.verloren.midnight.psi.CompactNamedElement
 */
public class CompactDuplicateDeclarationInspection extends LocalInspectionTool {

  @Override
  public String getStaticDescription() {
    return "Reports duplicate declarations within the same scope in Compact source code.";
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof PsiErrorElement || PsiTreeUtil.getParentOfType(element, PsiErrorElement.class) != null) {
          return;
        }

        if (isScopeContainer(element)) {
          checkScope(element, holder);
        }
      }
    };
  }

  /**
   * Tests whether an element acts as an isolated declaration container.
   */
  private static boolean isScopeContainer(PsiElement element) {
    return element instanceof CompactFile
        || element instanceof CompactModuleDefinition
        || element instanceof CompactBlock
        || element instanceof CompactStructDefinition
        || element instanceof CompactEnumDefinition
        || element instanceof CompactCircuitDefinition
        || element instanceof CompactWitnessDeclaration
        || element instanceof CompactConstructorDeclaration
        || element instanceof CompactExternalContractDeclaration;
  }

  /**
   * Collects all direct declarations in a scope container and reports collisions within each namespace.
   */
  private static void checkScope(@NotNull PsiElement scope, @NotNull ProblemsHolder holder) {
    Map<String, List<CompactNamedElement>> valueDeclarations = new LinkedHashMap<>();
    Map<String, List<CompactNamedElement>> typeDeclarations = new LinkedHashMap<>();

    // Step 1: Scan all named elements within the scope
    for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(scope, CompactNamedElement.class)) {
      if (named instanceof CompactImportElementImpl
          || named instanceof CompactGenericParameterImpl
          || (named instanceof CompactParameterImpl && PsiTreeUtil.getParentOfType(named, CompactStructFieldImpl.class) != null)) {
        continue;
      }
      if (PsiTreeUtil.getParentOfType(named, PsiErrorElement.class) != null) {
        continue;
      }

      // Step 2: Ensure the declaration directly belongs to this specific scope level
      PsiElement declarationScope = getDeclarationScope(named);
      if (declarationScope == scope) {
        String name = named.getName();
        if (name == null || name.isEmpty()) {
          continue;
        }

        // Step 3: Partition into value or type namespace buckets
        Map<String, List<CompactNamedElement>> targetMap = isTypeDeclaration(named) ? typeDeclarations : valueDeclarations;
        targetMap.computeIfAbsent(name, _ -> new ArrayList<>()).add(named);
      }
    }

    // Step 4: Report duplicates (first declaration is considered primary; subsequent ones are flagged)
    reportDuplicates(valueDeclarations, holder);
    reportDuplicates(typeDeclarations, holder);
  }

  /**
   * Registers problem annotations for any identifier with more than one declaration in the scope.
   */
  private static void reportDuplicates(Map<String, List<CompactNamedElement>> map, ProblemsHolder holder) {
    for (Map.Entry<String, List<CompactNamedElement>> entry : map.entrySet()) {
      List<CompactNamedElement> duplicates = entry.getValue();
      if (duplicates.size() > 1) {
        for (int i = 1; i < duplicates.size(); i++) {
          CompactNamedElement duplicate = duplicates.get(i);
          PsiElement nameId = duplicate.getNameIdentifier();
          PsiElement target = nameId != null ? nameId : duplicate;
          holder.registerProblem(
              target,
              "Duplicate declaration '" + entry.getKey() + "'",
              ProblemHighlightType.GENERIC_ERROR_OR_WARNING
          );
        }
      }
    }
  }

  /**
   * Classifies an element as a type declaration.
   */
  private static boolean isTypeDeclaration(CompactNamedElement element) {
    return element instanceof CompactTypeDefinition
        || element instanceof CompactStructDefinition
        || element instanceof CompactEnumDefinition
        || element instanceof CompactExternalContractDeclaration;
  }

  /**
   * Determines the exact owning scope container for a given declaration element.
   */
  private static PsiElement getDeclarationScope(CompactNamedElement element) {
    if (element instanceof CompactStructFieldImpl) {
      return PsiTreeUtil.getParentOfType(element, CompactStructDefinition.class);
    }
    if (element instanceof CompactEnumMemberImpl) {
      return PsiTreeUtil.getParentOfType(element, CompactEnumDefinition.class);
    }
    if (element instanceof CompactParameterImpl || isPatternParameter(element)) {
      CompactStructFieldImpl field = PsiTreeUtil.getParentOfType(element, CompactStructFieldImpl.class);
      if (field != null) {
        return PsiTreeUtil.getParentOfType(element, CompactStructDefinition.class);
      }
      return PsiTreeUtil.getParentOfType(element,
          CompactCircuitDefinition.class,
          CompactWitnessDeclaration.class,
          CompactConstructorDeclaration.class,
          CompactExternalContractDeclaration.class);
    }
    if (element instanceof CompactConstBindingImpl || element instanceof CompactPatternImpl) {
      CompactBlock block = PsiTreeUtil.getParentOfType(element, CompactBlock.class);
      if (block != null) {
        return block;
      }
      PsiElement callable = PsiTreeUtil.getParentOfType(element,
          CompactCircuitDefinition.class,
          CompactWitnessDeclaration.class,
          CompactConstructorDeclaration.class);
      if (callable != null) {
        return callable;
      }
      CompactModuleDefinition module = PsiTreeUtil.getParentOfType(element, CompactModuleDefinition.class);
      if (module != null) {
        return module;
      }
      return element.getContainingFile();
    }

    CompactModuleDefinition module = PsiTreeUtil.getParentOfType(element, CompactModuleDefinition.class);
    if (module != null && element != module) {
      return module;
    }
    return element.getContainingFile();
  }

  /**
   * Checks whether a pattern binding is acting as a parameter in a callable parameter list.
   */
  private static boolean isPatternParameter(@NotNull PsiElement declaration) {
    if (!(declaration instanceof CompactPatternImpl)) {
      return false;
    }
    return hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.PATTERN_PARAMETER_LIST)
        || hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.SIMPLE_PARAMETER_LIST)
        || hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.ARROW_PARAMETER_LIST);
  }

  /**
   * Walks upward testing for a specific AST element type without crossing block boundaries.
   */
  private static boolean hasAncestorOfType(@NotNull PsiElement element, @NotNull com.intellij.psi.tree.IElementType type) {
    for (PsiElement parent = element.getParent(); parent != null; parent = parent.getParent()) {
      if (parent.getNode() != null && parent.getNode().getElementType() == type) {
        return true;
      }
      if (parent instanceof CompactBlock) {
        return false;
      }
    }
    return false;
  }
}
