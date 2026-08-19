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
    Object value = element.getValue();
    return value instanceof CompactFile;
  }

  @Override
  public boolean isAlwaysLeaf(StructureViewTreeElement element) {
    Object value = element.getValue();
    return value instanceof CompactStructFieldImpl
        || value instanceof CompactEnumMemberImpl
        || value instanceof CompactConstBindingImpl
        || value instanceof CompactTypeDefinition
        || value instanceof CompactPragmaForm
        || value instanceof CompactIncludeDeclaration
        || value instanceof CompactImportDeclaration
        || value instanceof CompactExportDeclaration
        || value instanceof CompactCircuitDefinition
        || value instanceof CompactWitnessDeclaration
        || value instanceof CompactConstructorDeclaration;
  }
}
