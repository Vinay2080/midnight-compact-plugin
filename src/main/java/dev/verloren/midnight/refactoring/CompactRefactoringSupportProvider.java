package dev.verloren.midnight.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.psi.CompactNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Enables in-place renaming for Compact-named declarations.
 *
 * <p>Allows renaming of circuits, witnesses, structs, enums, parameters, and local constants
 * directly in the editor buffer without opening a modal dialog.</p>
 */
public class CompactRefactoringSupportProvider extends RefactoringSupportProvider {
  @Override
  public boolean isMemberInplaceRenameAvailable(@NotNull PsiElement elementToRename, @Nullable PsiElement context) {
    return elementToRename instanceof CompactNamedElement;
  }
}