package dev.verloren.midnight.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.psi.CompactNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Refactoring support provider enabling in-place rename operations for Compact-named declarations.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.lang.refactoringSupport} extension point
 * for {@link dev.verloren.midnight.CompactLanguage}. Instructs IntelliJ's refactoring engine whether a
 * declaration can be renamed directly within the editor buffer using an in-place template box
 * (rather than opening an intrusive modal dialogue):
 * <ul>
 *   <li><b>In-Place Renaming:</b> Enabled for all declarations implementing {@link CompactNamedElement}
 *       (including circuits, witnesses, contracts, modules, structs, struct fields, enums, enum members,
 *       parameters, and local bindings).</li>
 *   <li><b>Live Synchronization:</b> Integrates with IntelliJ's {@code MemberInplaceRenamer} to update
 *       all intra-file references synchronously as the developer types the new identifier.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by {@code com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler} when the user
 * initiates a rename action (Shift+F6 / Refactor | Rename) on an element in the editor.
 * If {@link #isMemberInplaceRenameAvailable} returns {@code true}, an in-place editor template is spawned;
 * otherwise, the platform falls back to the modal {@code RenameDialog}.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Called on the Event Dispatch Thread (EDT) when the rename action is invoked.</li>
 *   <li><b>Read Action:</b> Requires a platform Read Action ({@code @RequiresReadLock}) while evaluating the
 *       target {@link PsiElement} type.</li>
 *   <li><b>Write Actions:</b> This method only evaluates availability. The actual renaming is committed later
 *       inside a platform {@code WriteCommandAction}.</li>
 * </ul>
 *
 * <h3>Refactoring Invocation Flow</h3>
 * <pre>
 *   [User presses Shift+F6 on symbol]
 *                   │
 *                   ▼
 *   [CompactRefactoringSupportProvider]
 *                   │
 *   isMemberInplaceRenameAvailable(elementToRename, context)
 *                   │
 *          ┌────────┴────────┐
 *          ▼                 ▼
 *   [CompactNamedElement]  [Other Elements]
 *          │                 │
 *          ▼                 ▼
 *    In-Place Inline     Modal Rename
 *    Editor Renamer      Dialog Fallback
 * </pre>
 *
 * @see dev.verloren.midnight.psi.CompactNamedElement
 * @see dev.verloren.midnight.refactoring.CompactNamesValidator
 * @see com.intellij.lang.refactoring.RefactoringSupportProvider
 */
public class CompactRefactoringSupportProvider extends RefactoringSupportProvider {

  /**
   * Determines whether in-place inline renaming is permitted for the specified declaration.
   *
   * @param elementToRename the PSI element targeted for renaming
   * @param context         the PSI element under the caret where the action was invoked, or null
   * @return true if the element implements {@link CompactNamedElement}; false otherwise
   */
  @Override
  public boolean isMemberInplaceRenameAvailable(@NotNull PsiElement elementToRename, @Nullable PsiElement context) {
    // Step 1: Permit in-place editor renaming for any named declaration in Compact files
    return elementToRename instanceof CompactNamedElement;
  }
}