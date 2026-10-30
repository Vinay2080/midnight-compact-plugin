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
 * Provides interactive hierarchical navigation breadcrumbs along the editor bottom bar for Compact source files.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as a {@code breadcrumbsInfoProvider} extension in {@code plugin.xml}. Implements
 * {@link BreadcrumbsProvider} to compute the lexical breadcrumb chain corresponding to the current caret
 * position. As the developer navigates through nested contracts, modules, circuits, statements, and type
 * declarations, the breadcrumb bar displays the nesting path and allows instant navigation to ancestor constructs.</p>
 *
 * <h3>Breadcrumbs Hierarchy Structure</h3>
 * <pre>
 *   CompactFile
 *       │
 *       ├─► contract ContractName
 *       │       ├─► circuit CircuitName
 *       │       │       ├─► if
 *       │       │       └─► for
 *       │       └─► witness WitnessName
 *       ├─► module ModuleName
 *       ├─► struct StructName
 *       └─► enum EnumName
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Invoked on background read threads or EDT under a platform read lock.
 *       All PSI queries are read-only and non-blocking.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.structure.CompactStructureViewModel
 */
public class CompactBreadcrumbsProvider implements BreadcrumbsProvider {

  /**
   * Returns the array of programming languages supported by this breadcrumb's provider.
   *
   * @return array containing {@link CompactLanguage#INSTANCE}
   */
  @Override
  public Language @NotNull [] getLanguages() {
    return new Language[]{CompactLanguage.INSTANCE};
  }

  /**
   * Determines whether the specified PSI element represents a structural boundary that should
   * appear as a breadcrumb crumb.
   *
   * @param element the candidate PSI element
   * @return {@code true} if {@code element} is a supported declaration or control statement; {@code false} otherwise
   */
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

  /**
   * Produces the short label text displayed on the breadcrumb crumb in the editor UI.
   *
   * @param element the PSI element representing the breadcrumb crumb
   * @return human-readable breadcrumb label (e.g. {@code "circuit mint"}, {@code "struct Point"}, {@code "if"})
   */
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

  /**
   * Returns the tooltip text displayed when hovering over a breadcrumb crumb.
   *
   * @param element the PSI element associated with the crumb
   * @return the tooltip string, defaulting to the element's crumb label
   */
  @Override
  public @Nullable String getElementTooltip(@NotNull PsiElement element) {
    return getElementInfo(element);
  }
}
