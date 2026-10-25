package dev.verloren.midnight.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.stdlib.CompactStdlibService;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Handles "Go to Declaration" (Ctrl+B / Cmd+B / Ctrl+Click) navigation actions in Compact files.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.gotoDeclarationHandler} extension point.
 * Complements standard PSI reference resolution by providing a multi-tiered resolution pipeline that handles:
 * <ul>
 *   <li><b>Direct PSI References:</b> Resolves references attached directly to the hovered/selected PSI element.</li>
 *   <li><b>Parent Expression References:</b> Resolves references attached to parent nodes (e.g. leaf tokens
 *       inside {@link dev.verloren.midnight.psi.CompactReferenceExpr}, {@link dev.verloren.midnight.psi.CompactTypeReference},
 *       or {@link dev.verloren.midnight.psi.CompactCallExpr}).</li>
 *   <li><b>Standard Library Navigation:</b> Maps the magic identifier {@code "CompactStandardLibrary"}
 *       to the bundled virtual file managed by {@link CompactStdlibService}.</li>
 *   <li><b>Fallback Semantic Resolution:</b> Queries {@link CompactResolveUtil#resolveValue} and
 *       {@link CompactResolveUtil#resolveType} across local scopes, imports, and top-level definitions.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by {@code com.intellij.codeInsight.navigation.actions.GotoDeclarationAction} whenever
 * the user triggers navigation (Ctrl+B or mouse click). If this handler returns non-empty targets,
 * the platform navigates immediately (or displays a disambiguation popup if multiple targets match).</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Called on the Event Dispatch Thread (EDT) or pooled background threads
 *       by navigation actions and editor hover listeners.</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while traversing PSI
 *       references and lexical scopes.</li>
 *   <li><b>Write Actions:</b> Read-only resolution; never acquires write actions.</li>
 * </ul>
 *
 * <h3>Resolution Pipeline Diagram</h3>
 * <pre>
 *   [User presses Ctrl+B / Ctrl+Click]
 *                  │
 *                  ▼
 *     [CompactGotoDeclarationHandler]
 *                  │
 *                  ├─► Tier 1: Check direct references on sourceElement (ref.resolve())
 *                  │
 *                  ├─► Tier 2: Check parent references (parent.getReferences())
 *                  │
 *                  └─► Tier 3: Fallback for IDENTIFIER token:
 *                            ├── "CompactStandardLibrary" ──► CompactStdlibService.getStandardLibraryFiles()
 *                            ├── Value Namespace ──────────► CompactResolveUtil.resolveValue(...)
 *                            └── Type Namespace ───────────► CompactResolveUtil.resolveType(...)
 * </pre>
 *
 * @see dev.verloren.midnight.resolve.CompactResolveUtil
 * @see dev.verloren.midnight.stdlib.CompactStdlibService
 * @see com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
 */
public class CompactGotoDeclarationHandler implements GotoDeclarationHandler {

  /**
   * Resolves the navigation target PSI elements for the specified source element and caret offset.
   *
   * @param sourceElement the PSI element under the caret, or null
   * @param offset        the caret offset in the editor document
   * @param editor        the active editor instance
   * @return array of target PSI declaration elements, or null if unresolvable
   */
  @Override
  public PsiElement @Nullable [] getGotoDeclarationTargets(
      @Nullable PsiElement sourceElement,
      int offset,
      Editor editor
  ) {
    if (sourceElement == null || !sourceElement.isValid() || sourceElement.getLanguage() != CompactLanguage.INSTANCE) {
      return null;
    }

    // Step 1: Inspect references directly declared on the sourceElement
    for (PsiReference ref : sourceElement.getReferences()) {
      PsiElement target = ref.resolve();
      if (target != null) {
        return new PsiElement[]{target};
      }
    }

    // Step 2: Inspect references on direct parent (e.g. identifier token within a reference expression or call)
    PsiElement parent = sourceElement.getParent();
    if (parent != null) {
      for (PsiReference ref : parent.getReferences()) {
        PsiElement target = ref.resolve();
        if (target != null) {
          return new PsiElement[]{target};
        }
      }
    }

    // Step 3: Handle raw identifier tokens via semantic namespaces or standard library lookups
    if (sourceElement.getNode() != null && sourceElement.getNode().getElementType() == CompactTokenTypes.IDENTIFIER) {
      String name = sourceElement.getText();

      // Step 3a: Navigate to bundled Compact standard library file if referencing standard library service
      if ("CompactStandardLibrary".equals(name)) {
        List<CompactFile> stdlib = CompactStdlibService.getInstance(sourceElement.getProject()).getStandardLibraryFiles();
        if (!stdlib.isEmpty()) {
          return new PsiElement[]{stdlib.getFirst()};
        }
      }

      // Step 3b: Resolve against value declarations (circuits, witnesses, ledgers, variables, constants)
      List<CompactNamedElement> values = CompactResolveUtil.resolveValue(name, sourceElement);
      if (!values.isEmpty()) {
        return values.toArray(PsiElement.EMPTY_ARRAY);
      }

      // Step 3c: Resolve against type declarations (structs, enums, type aliases, contracts)
      List<CompactNamedElement> types = CompactResolveUtil.resolveType(name, sourceElement);
      if (!types.isEmpty()) {
        return types.toArray(PsiElement.EMPTY_ARRAY);
      }
    }

    return null;
  }
}
