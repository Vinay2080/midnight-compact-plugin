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
 * Contributor enabling "Navigate | Class" (Ctrl+N / Cmd+O / Double Shift) across Compact contract constructs.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.gotoClassContributor} extension point.
 * Implements {@link ChooseByNameContributorEx} to index and serve all top-level and modular "class-like"
 * definitions in Compact smart contracts:
 * <ul>
 *   <li><b>External Contract Interfaces:</b> {@link CompactExternalContractDeclaration}</li>
 *   <li><b>Contract Implementations:</b> {@link CompactContractImplementsDeclaration}</li>
 *   <li><b>Modules:</b> {@link CompactModuleDefinition}</li>
 *   <li><b>Structs:</b> {@link CompactStructDefinition}</li>
 *   <li><b>Enums:</b> {@link CompactEnumDefinition}</li>
 *   <li><b>Type Aliases:</b> {@link CompactTypeDefinition}</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by IntelliJ's Search Everywhere / ChooseByName subsystem ({@code ChooseByNameModelEx},
 * {@code GotoClassModel2}). When the user opens the Class navigation popup:
 * <ol>
 *   <li>The platform invokes {@link #processNames} to populate candidate completion names.</li>
 *   <li>As the user selects or types a name, the platform calls {@link #processElementsWithName}
 *       to resolve matching {@link NavigationItem} target elements.</li>
 * </ol>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Always executed on pooled background worker threads managed by the Search Everywhere subsystem.</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while querying {@link FileTypeIndex}
 *       and traversing PSI trees.</li>
 *   <li><b>Cancellation Support:</b> Respects early termination when {@code processor.process(...)} returns {@code false},
 *       instantly halting traversal if the search dialog closes or query changes.</li>
 * </ul>
 *
 * <h3>Search Hierarchy</h3>
 * <pre>
 *   [User presses Ctrl+N / Double Shift]
 *                     │
 *                     ▼
 *      [CompactGotoClassContributor]
 *                     │
 *                     ├─► [FileTypeIndex.getFiles(CompactFileType, scope)]
 *                     │         │
 *                     │         ▼
 *                     │   Iterate CompactFiles
 *                     │         │
 *                     │         ▼
 *                     └─► Filter: isClassLike(element)
 *                               ├── Contracts (External &amp; Implementation)
 *                               ├── Modules
 *                               ├── Structs
 *                               ├── Enums
 *                               └── Type Aliases
 * </pre>
 *
 * @see dev.verloren.midnight.navigation.CompactGotoSymbolContributor
 * @see com.intellij.navigation.ChooseByNameContributorEx
 * @see com.intellij.psi.search.FileTypeIndex
 */
public class CompactGotoClassContributor implements ChooseByNameContributorEx {

  /**
   * Scans Compact files in the given search scope and feeds distinct class-like declaration names to the processor.
   *
   * @param processor consumer receiving matched declaration name strings; returns false to halt processing
   * @param scope     the search scope determining which project and library files are included
   * @param filter    optional ID filter to skip non-matching index IDs
   */
  @Override
  public void processNames(
      @NotNull Processor<? super String> processor,
      @NotNull GlobalSearchScope scope,
      @Nullable IdFilter filter
  ) {
    // Step 1: Query the VFS file type index for all Compact files within the active search scope
    Collection<VirtualFile> files = FileTypeIndex.getFiles(CompactFileType.INSTANCE, scope);
    for (VirtualFile file : files) {
      if (scope.getProject() == null) continue;
      PsiFile psiFile = PsiManager.getInstance(scope.getProject()).findFile(file);
      if (psiFile instanceof CompactFile) {
        // Step 2: Traverse named elements within the file and collect class-like constructs
        for (CompactNamedElement element : PsiTreeUtil.findChildrenOfType(psiFile, CompactNamedElement.class)) {
          if (isClassLike(element)) {
            String name = element.getName();
            if (name != null && !name.isEmpty()) {
              // Step 3: Yield name to processor; abort immediately if search was cancelled or completed
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
   * Locates and resolves all class-like declarations matching the specified name within the query parameters.
   *
   * @param name       the target name to search for
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
    // Step 1: Query Compact files within the requested search scope
    Collection<VirtualFile> files = FileTypeIndex.getFiles(CompactFileType.INSTANCE, scope);
    for (VirtualFile file : files) {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (psiFile instanceof CompactFile) {
        // Step 2: Locate class-like elements matching the exact target name
        for (CompactNamedElement element : PsiTreeUtil.findChildrenOfType(psiFile, CompactNamedElement.class)) {
          if (isClassLike(element) && name.equals(element.getName())) {
            // Step 3: Yield element navigation item to consumer
            if (!processor.process(element)) {
              return;
            }
          }
        }
      }
    }
  }

  /**
   * Determines whether the specified Compact named element qualifies as a "class-like" construct
   * eligible for indexing in the "Navigate | Class" index.
   *
   * @param element the Compact named element to test
   * @return true if the element represents a contract, module, struct, enum, or type alias; false otherwise
   */
  public static boolean isClassLike(@NotNull CompactNamedElement element) {
    return switch (element) {
      case CompactExternalContractDeclaration _,
           CompactContractImplementsDeclaration _,
           CompactModuleDefinition _,
           CompactStructDefinition _,
           CompactEnumDefinition _,
           CompactTypeDefinition _ -> true;
      default -> false;
    };
  }
}
