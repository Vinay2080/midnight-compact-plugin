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
 * Structure View tree model for Compact smart contract source files.
 *
 * <h3>Subsystem Role &amp; Architecture</h3>
 * <p>Extends IntelliJ's {@link StructureViewModelBase} and implements {@link StructureViewModel.ElementInfoProvider}.
 * Serves as the structural bridge between the Compact PSI abstract syntax tree and the visual tree displayed
 * in the Structure Tool Window and File Structure popup (Ctrl+F12). It defines:
 * <ul>
 *   <li><b>Suitable Element Classes:</b> The set of 17 Compact PSI declaration classes eligible for structural indexing and tree navigation.</li>
 *   <li><b>Sorting Strategies:</b> Configures {@link Sorter#ALPHA_SORTER} allowing developers to toggle between declaration order and alphabetical order.</li>
 *   <li><b>Expansion Semantics:</b> Enforces that {@link CompactFile} roots always render with expansion controls ({@link #isAlwaysShowsPlus}).</li>
 *   <li><b>Leaf Classification:</b> Categorizes terminal declarations (circuits, witnesses, struct fields, enum members, constants, pragmas)
 *       as terminal leaves to eliminate redundant tree expansion passes ({@link #isAlwaysLeaf}).</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Query methods ({@link #isAlwaysShowsPlus}, {@link #isAlwaysLeaf}) are invoked
 *       either on the Event Dispatch Thread (EDT) or pooled background threads during tree construction and filtering.</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while querying element types and values.</li>
 *   <li><b>Memory Constraints:</b> Keeps a weak/transient reference to the editor and root file. Never stores UI nodes
 *       or mutable collections in static state.</li>
 * </ul>
 *
 * <h3>Node Classification Hierarchy</h3>
 * <pre>
 *   [CompactStructureViewModel]
 *             │
 *             ├── Root Node: [CompactFile] (alwaysShowsPlus = true)
 *             │
 *             ├── Branch Containers (can expand):
 *             │     ├── CompactExternalContractDeclaration
 *             │     ├── CompactContractImplementsDeclaration
 *             │     ├── CompactModuleDefinition
 *             │     ├── CompactStructDefinition
 *             │     └── CompactEnumDefinition
 *             │
 *             └── Terminal Leaves (isAlwaysLeaf = true):
 *                   ├── Circuits &amp; Witnesses: CompactCircuitDefinition, CompactWitnessDeclaration, CompactConstructorDeclaration
 *                   ├── Members &amp; Fields: CompactStructFieldImpl, CompactEnumMemberImpl, CompactConstBindingImpl
 *                   ├── Types: CompactTypeDefinition
 *                   └── Directives: CompactPragmaForm, CompactIncludeDeclaration, CompactImportDeclaration, CompactExportDeclaration
 * </pre>
 *
 * @see dev.verloren.midnight.structure.CompactStructureViewElement
 * @see dev.verloren.midnight.structure.CompactStructureViewFactory
 * @see com.intellij.ide.structureView.StructureViewModelBase
 * @see com.intellij.ide.structureView.StructureViewModel.ElementInfoProvider
 */
public class CompactStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {

  /**
   * Initializes the structure view model with the root Compact source file and active editor.
   *
   * @param psiFile the root Compact source file
   * @param editor  the active editor instance, or null if viewed outside an open editor (e.g. preview)
   */
  @SuppressWarnings("this-escape")
  public CompactStructureViewModel(@NotNull PsiFile psiFile, @Nullable Editor editor) {
    // Step 1: Initialize base model with the file root wrapped in a CompactStructureViewElement
    super(psiFile, editor, new CompactStructureViewElement(psiFile));

    // Step 2: Register all declaration and directive PSI classes recognized by the structure view
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

    // Step 3: Register alphabetical sorter enabling toolbar toggling between file and alphabetical ordering
    withSorters(Sorter.ALPHA_SORTER);
  }

  /**
   * Determines whether the given element should always show a plus/expansion icon even before children are loaded.
   *
   * @param element the tree element to evaluate
   * @return true if the element represents the root {@link CompactFile}; false otherwise
   */
  @Override
  public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
    return element.getValue() instanceof CompactFile;
  }

  /**
   * Identifies whether the given element represents a terminal leaf node in the Structure tree.
   *
   * <p>Terminal leaves cannot have child elements in the structure view, preventing unnecessary PSI traversals
   * into their internals (such as expression statements or parameter lists).</p>
   *
   * @param element the tree element to evaluate
   * @return true if the element is classified as a terminal leaf; false if it can contain structural children
   */
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
