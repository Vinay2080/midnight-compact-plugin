package dev.verloren.midnight.editor;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;

import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactElementTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides real-time navigation breadcrumbs for Compact source files.
 */
public class CompactBreadcrumbsProvider implements BreadcrumbsProvider {

  @Override
  public Language @NotNull [] getLanguages() {
    return new Language[]{CompactLanguage.INSTANCE};
  }

  @Override
  public boolean acceptElement(@NotNull PsiElement element) {
    return switch (element) {
      case CompactExternalContractDeclaration _,
           CompactContractImplementsDeclaration _,
           CompactModuleDefinition _,
           CompactCircuitDefinition _,
           CompactWitnessDeclaration _,
           CompactConstructorDeclaration _,
           CompactLedgerDeclaration _,
           CompactStructDefinition _,
           CompactEnumDefinition _ -> true;
      default -> element.getNode() != null && (
          element.getNode().getElementType() == CompactElementTypes.IF_STATEMENT ||
          element.getNode().getElementType() == CompactElementTypes.FOR_STATEMENT
      );
    };
  }

  @Override
  public @NotNull String getElementInfo(@NotNull PsiElement element) {
    return switch (element) {
      case CompactExternalContractDeclaration contract -> {
        String name = contract.getName();
        yield name != null ? "contract " + name : "contract";
      }
      case CompactContractImplementsDeclaration _ -> "contract";
      case CompactModuleDefinition module -> {
        String name = module.getName();
        yield name != null ? "module " + name : "module";
      }
      case CompactCircuitDefinition circuit -> {
        String name = circuit.getName();
        yield name != null ? "circuit " + name : "circuit";
      }
      case CompactWitnessDeclaration witness -> {
        String name = witness.getName();
        yield name != null ? "witness " + name : "witness";
      }
      case CompactConstructorDeclaration _ -> "constructor";
      case CompactLedgerDeclaration _ -> "ledger";
      case CompactStructDefinition struct -> {
        String name = struct.getName();
        yield name != null ? "struct " + name : "struct";
      }
      case CompactEnumDefinition enumDef -> {
        String name = enumDef.getName();
        yield name != null ? "enum " + name : "enum";
      }
      default -> {
        if (element.getNode() != null) {
          if (element.getNode().getElementType() == CompactElementTypes.IF_STATEMENT) {
            yield "if";
          }
          if (element.getNode().getElementType() == CompactElementTypes.FOR_STATEMENT) {
            yield "for";
          }
        }
        yield element.getText();
      }
    };
  }

  @Override
  public @Nullable String getElementTooltip(@NotNull PsiElement element) {
    return getElementInfo(element);
  }
}
