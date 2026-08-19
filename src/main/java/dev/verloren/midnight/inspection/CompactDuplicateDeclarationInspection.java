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

import java.util.*;

/**
 * Inspection reporting duplicate declaration identifiers declared in the same lexical scope.
 *
 * <p>Separates value declarations from type declarations to allow legal name sharing across distinct namespaces,
 * while flagging duplicate siblings in modules, blocks, structs, and contracts.</p>
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

  private static void checkScope(@NotNull PsiElement scope, @NotNull ProblemsHolder holder) {
    Map<String, List<CompactNamedElement>> valueDeclarations = new LinkedHashMap<>();
    Map<String, List<CompactNamedElement>> typeDeclarations = new LinkedHashMap<>();

    for (CompactNamedElement named : PsiTreeUtil.findChildrenOfType(scope, CompactNamedElement.class)) {
      if (named instanceof CompactImportElementImpl
          || named instanceof CompactGenericParameterImpl
          || (named instanceof CompactParameterImpl && PsiTreeUtil.getParentOfType(named, CompactStructFieldImpl.class) != null)) {
        continue;
      }
      if (PsiTreeUtil.getParentOfType(named, PsiErrorElement.class) != null) {
        continue;
      }

      PsiElement declarationScope = getDeclarationScope(named);
      if (declarationScope == scope) {
        String name = named.getName();
        if (name == null || name.isEmpty()) {
          continue;
        }

        Map<String, List<CompactNamedElement>> targetMap = isTypeDeclaration(named) ? typeDeclarations : valueDeclarations;
        targetMap.computeIfAbsent(name, k -> new ArrayList<>()).add(named);
      }
    }

    reportDuplicates(valueDeclarations, holder);
    reportDuplicates(typeDeclarations, holder);
  }

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

  private static boolean isTypeDeclaration(CompactNamedElement element) {
    return element instanceof CompactTypeDefinition
        || element instanceof CompactStructDefinition
        || element instanceof CompactEnumDefinition
        || element instanceof CompactExternalContractDeclaration;
  }

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

  private static boolean isPatternParameter(@NotNull PsiElement declaration) {
    if (!(declaration instanceof CompactPatternImpl)) {
      return false;
    }
    return hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.PATTERN_PARAMETER_LIST)
        || hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.SIMPLE_PARAMETER_LIST)
        || hasAncestorOfType(declaration, dev.verloren.midnight.parser.CompactElementTypes.ARROW_PARAMETER_LIST);
  }

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
