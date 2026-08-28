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
 * Contributor enabling "Navigate | Class" (Ctrl + N / Double Shift) across Compact contracts, modules, structs, enums, and types.
 */
public class CompactGotoClassContributor implements ChooseByNameContributorEx {

  @Override
  public void processNames(
      @NotNull Processor<? super String> processor,
      @NotNull GlobalSearchScope scope,
      @Nullable IdFilter filter
  ) {
    Collection<VirtualFile> files = FileTypeIndex.getFiles(CompactFileType.INSTANCE, scope);
    for (VirtualFile file : files) {
      if (scope.getProject() == null) continue;
      PsiFile psiFile = PsiManager.getInstance(scope.getProject()).findFile(file);
      if (psiFile instanceof CompactFile) {
        for (CompactNamedElement element : PsiTreeUtil.findChildrenOfType(psiFile, CompactNamedElement.class)) {
          if (isClassLike(element)) {
            String name = element.getName();
            if (name != null && !name.isEmpty()) {
              if (!processor.process(name)) {
                return;
              }
            }
          }
        }
      }
    }
  }

  @Override
  public void processElementsWithName(
      @NotNull String name,
      @NotNull Processor<? super NavigationItem> processor,
      @NotNull FindSymbolParameters parameters
  ) {
    GlobalSearchScope scope = parameters.getSearchScope();
    Project project = parameters.getProject();
    Collection<VirtualFile> files = FileTypeIndex.getFiles(CompactFileType.INSTANCE, scope);
    for (VirtualFile file : files) {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (psiFile instanceof CompactFile) {
        for (CompactNamedElement element : PsiTreeUtil.findChildrenOfType(psiFile, CompactNamedElement.class)) {
          if (isClassLike(element) && name.equals(element.getName())) {
            if (!processor.process(element)) {
              return;
            }
          }
        }
      }
    }
  }

  public static boolean isClassLike(@NotNull CompactNamedElement element) {
    return element instanceof CompactExternalContractDeclaration
        || element instanceof CompactContractImplementsDeclaration
        || element instanceof CompactModuleDefinition
        || element instanceof CompactStructDefinition
        || element instanceof CompactEnumDefinition
        || element instanceof CompactTypeDefinition;
  }
}
