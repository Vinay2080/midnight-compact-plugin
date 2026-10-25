package dev.verloren.midnight.navigation;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Contributor enabling "Navigate | Symbol" (Ctrl+Alt+Shift+N / Cmd+Option+O / Double Shift) across Compact declarations.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.gotoSymbolContributor} extension point.
 * Implements {@link ChooseByNameContributorEx} to provide project-wide and library-wide symbol search
 * across all named Compact language declarations:
 * <ul>
 *   <li><b>Circuits &amp; Witnesses:</b> Contract and module-level circuit and witness definitions.</li>
 *   <li><b>Ledger Declarations:</b> Ledger state fields.</li>
 *   <li><b>Data Models:</b> Structs, struct fields, enums, enum members, and type aliases.</li>
 *   <li><b>Top-Level Constants:</b> Global constants outside local function/circuit bodies.</li>
 *   <li><b>Deliberate Omissions:</b> Local variables/parameters and generic type parameters are intentionally
 *       filtered out ({@link #isSymbol}) to prevent search result pollution.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by IntelliJ's Search Everywhere / GotoSymbol dialog ({@code GotoSymbolModel2}).
 * <ol>
 *   <li>{@link #processNames} generates candidate symbol name suggestions.</li>
 *   <li>{@link #processElementsWithName} resolves selected names to actual {@link NavigationItem} target nodes.</li>
 * </ol>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Always executed on pooled background worker threads by Search Everywhere.</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while querying {@link FileTypeIndex}
 *       and traversing PSI trees.</li>
 *   <li><b>Cancellation Support:</b> Halts traversal immediately when {@code processor.process(...)} returns {@code false}.</li>
 * </ul>
 *
 * <h3>Symbol Filtering Architecture</h3>
 * <pre>
 *   [User presses Ctrl+Alt+Shift+N / Double Shift]
 *                           │
 *                           ▼
 *         [CompactGotoSymbolContributor]
 *                           │
 *                           ├─► [FileTypeIndex.getFiles(CompactFileType, scope)]
 *                           │         │
 *                           │         ▼
 *                           │   Iterate CompactFiles
 *                           │         │
 *                           │         ▼
 *                           └─► Filter: isSymbol(element)
 *                                     ├── EXCLUDE: Parameters &amp; Generic Type Parameters
 *                                     ├── EXCLUDE: Local Block-Scoped Constants
 *                                     └── INCLUDE: Circuits, Witnesses, Ledgers,
 *                                                  Structs, Fields, Enums, Members, Types,
 *                                                  Top-Level / Module Constants
 * </pre>
 *
 * @see dev.verloren.midnight.navigation.CompactGotoClassContributor
 * @see com.intellij.navigation.ChooseByNameContributorEx
 * @see com.intellij.psi.search.FileTypeIndex
 */
public class CompactGotoSymbolContributor implements ChooseByNameContributorEx {

  /**
   * Scans Compact files in the given search scope and feeds distinct eligible symbol names to the processor.
   *
   * @param processor consumer receiving matched symbol name strings; returns false to halt processing
   * @param scope     the search scope determining which project and library files are included
   * @param filter    optional ID filter to skip non-matching index IDs
   */
  @Override
  public void processNames(
      @NotNull Processor<? super String> processor,
      @NotNull GlobalSearchScope scope,
      @Nullable IdFilter filter
  ) {
    // Step 1: Query VFS index for all Compact files matching the scope
    Collection<VirtualFile> files = FileTypeIndex.getFiles(CompactFileType.INSTANCE, scope);
    for (VirtualFile file : files) {
      if (scope.getProject() == null) continue;
      PsiFile psiFile = PsiManager.getInstance(scope.getProject()).findFile(file);
      if (psiFile instanceof CompactFile) {
        // Step 2: Traverse named elements and collect eligible public symbols
        for (CompactNamedElement element : PsiTreeUtil.findChildrenOfType(psiFile, CompactNamedElement.class)) {
          if (isSymbol(element)) {
            String name = element.getName();
            if (name != null && !name.isEmpty()) {
              // Step 3: Emit symbol name to consumer; halt if cancelled
              if (!processor.process(name)) {
                return;
              }
            }
          }
        }
      }
    }
  }

  /**
   * Locates and resolves all symbol declarations matching the specified name within the query parameters.
   *
   * @param name       the target symbol name to search for
   * @param processor  consumer receiving matching {@link NavigationItem} elements; returns false to halt processing
   * @param parameters search parameters containing project, search scope, and cancellation checkers
   */
  @Override
  public void processElementsWithName(
      @NotNull String name,
      @NotNull Processor<? super NavigationItem> processor,
      @NotNull FindSymbolParameters parameters
  ) {
    GlobalSearchScope scope = parameters.getSearchScope();
    Project project = parameters.getProject();
    // Step 1: Query Compact files matching the active search scope
    Collection<VirtualFile> files = FileTypeIndex.getFiles(CompactFileType.INSTANCE, scope);
    for (VirtualFile file : files) {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (psiFile instanceof CompactFile) {
        // Step 2: Match symbols with identical name and yield to processor
        for (CompactNamedElement element : PsiTreeUtil.findChildrenOfType(psiFile, CompactNamedElement.class)) {
          if (isSymbol(element) && name.equals(element.getName())) {
            // Step 3: Emit navigation item target
            if (!processor.process(element)) {
              return;
            }
          }
        }
      }
    }
  }

  /**
   * Determines whether the specified named element qualifies as a public symbol eligible for Navigate | Symbol.
   *
   * <p>Excludes local variables, circuit parameters, generic type parameters, and block-local constants to
   * prevent polluting global navigation indices with transient local symbols.</p>
   *
   * @param element the Compact named element to evaluate
   * @return true if the element is an indexable symbol; false otherwise
   */
  public static boolean isSymbol(@NotNull CompactNamedElement element) {
    return switch (element) {
      // Exclude circuit/witness parameters and generic type variables
      case CompactParameterImpl _, CompactGenericParameterImpl _ -> false;
      // Include constants only if defined outside local function blocks (i.e. file/module/contract level)
      case CompactPatternImpl _, CompactConstBindingImpl _ -> PsiTreeUtil.getParentOfType(element, CompactBlock.class) == null;
      // Include all other named declarations (circuits, witnesses, ledgers, structs, enums, fields, types)
      default -> true;
    };
  }
}
