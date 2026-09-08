package dev.verloren.midnight.structure;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Structure View tree model for Compact source files.
 *
 * <p>Configures suitable PSI classes displayed in the tree (circuits, witnesses, structs, enums,
 * contracts, modules, constants) and defines alphabetical sorting and leaf node behavior.</p>
 */
public class CompactStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {

  @SuppressWarnings("this-escape")
  public CompactStructureViewModel(@NotNull PsiFile psiFile, @Nullable Editor editor) {
    super(psiFile, editor, new CompactStructureViewElement(psiFile));
    withSuitableClasses(
        CompactFile.class,
        CompactPragmaForm.class,
        CompactIncludeDeclaration.class,
        CompactImportDeclaration.class,
        CompactExportDeclaration.class,
        CompactExternalContractDeclaration.class,
        CompactContractImplementsDeclaration.class,
        CompactModuleDefinition.class,
        CompactLedgerDeclaration.class,
        CompactConstructorDeclaration.class,
        CompactCircuitDefinition.class,
        CompactWitnessDeclaration.class,
        CompactStructDefinition.class,
        CompactStructFieldImpl.class,
        CompactEnumDefinition.class,
        CompactEnumMemberImpl.class,
        CompactTypeDefinition.class,
        CompactConstBindingImpl.class
    );
    withSorters(Sorter.ALPHA_SORTER);
  }

  @Override
  public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
    return element.getValue() instanceof CompactFile;
  }

  @Override
  public boolean isAlwaysLeaf(StructureViewTreeElement element) {
    return switch (element.getValue()) {
      case CompactStructFieldImpl _,
           CompactEnumMemberImpl _,
           CompactConstBindingImpl _,
           CompactTypeDefinition _,
           CompactPragmaForm _,
           CompactIncludeDeclaration _,
           CompactImportDeclaration _,
           CompactExportDeclaration _,
           CompactCircuitDefinition _,
           CompactWitnessDeclaration _,
           CompactConstructorDeclaration _ -> true;
      default -> false;
    };
  }
}
