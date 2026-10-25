package dev.verloren.midnight.structure;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extension point factory creating Structure View builders for Compact smart contract files.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.lang.psiStructureViewFactory} extension
 * point for {@link dev.verloren.midnight.CompactLanguage}. Provides the IntelliJ Platform with a
 * {@link TreeBasedStructureViewBuilder} capable of rendering the hierarchical syntax and contract
 * architecture of Compact source files (pragmas, imports/exports, contracts, modules, circuits, witnesses,
 * ledgers, structs, and enums).</p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by the platform structure view infrastructure under multiple circumstances:
 * <ul>
 *   <li><b>Structure Tool Window (Alt+7 / Cmd+7):</b> {@code com.intellij.ide.structureView.impl.StructureViewComposite}
 *       requests a builder to render the persistent sidebar tree alongside the editor.</li>
 *   <li><b>File Structure Popup (Ctrl+F12 / Cmd+F12):</b> {@code com.intellij.ide.actions.ViewStructureAction}
 *       requests a builder for rapid keyboard-driven member lookup and navigation.</li>
 *   <li><b>Navbar &amp; Breadcrumb Integration:</b> File structure models assist in navigating file-level hierarchy.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Instantiated on the Event Dispatch Thread (EDT) or pooled background threads.</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while inspecting the
 *       underlying {@link PsiFile}. The resulting {@link StructureViewModel} handles subsequent tree updates
 *       on background threads with read locks.</li>
 *   <li><b>Write Actions:</b> Never executes write actions. Purely declarative adapter factory.</li>
 * </ul>
 *
 * <h3>Architectural Hierarchy</h3>
 * <pre>
 *   [Structure Tool Window / Ctrl+F12]
 *                   │
 *                   ▼
 *    [CompactStructureViewFactory]
 *                   │ produces
 *                   ▼
 *      [TreeBasedStructureViewBuilder]
 *                   │ creates
 *                   ▼
 *      [CompactStructureViewModel] ──► [CompactStructureViewElement (Root: CompactFile)]
 * </pre>
 *
 * @see dev.verloren.midnight.structure.CompactStructureViewModel
 * @see dev.verloren.midnight.structure.CompactStructureViewElement
 * @see com.intellij.lang.PsiStructureViewFactory
 * @see com.intellij.ide.structureView.TreeBasedStructureViewBuilder
 */
public class CompactStructureViewFactory implements PsiStructureViewFactory {

  /**
   * Constructs a tree-based structure view builder for the specified Compact source file.
   *
   * @param psiFile the Compact source file to visualize
   * @return a {@link TreeBasedStructureViewBuilder} producing a {@link CompactStructureViewModel}, or null
   */
  @Override
  public @Nullable StructureViewBuilder getStructureViewBuilder(@NotNull PsiFile psiFile) {
    // Step 1: Return an anonymous TreeBasedStructureViewBuilder bound to the target PSI file
    return new TreeBasedStructureViewBuilder() {
      @Override
      public @NotNull StructureViewModel createStructureViewModel(@Nullable Editor editor) {
        // Step 2: Instantiate the Compact-specific view model encapsulating the file root and editor
        return new CompactStructureViewModel(psiFile, editor);
      }
    };
  }
}
