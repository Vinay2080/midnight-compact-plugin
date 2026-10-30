package dev.verloren.midnight.intention;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.psi.CompactPragmaForm;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.version.CompactSemVerUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.SequencedMap;

/**
 * Context intention action to switch or download the required Compact compiler version based on pragma directives.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered under {@code <intentionAction>} in {@code plugin.xml}:
 * <ul>
 *   <li><b>Contextual Availability:</b> Appears in the Alt+Enter menu when the caret is placed
 *       on an unsatisfied {@code pragma language_version} or {@code pragma compiler_version} form.</li>
 *   <li><b>Smart Target Resolution:</b> Checks locally installed compilers first (sorted newest first)
 *       to find any compiler satisfying the SemVer constraint; if none are installed, resolves
 *       the downloadable release tag from GitHub.</li>
 *   <li><b>Asynchronous Toolchain Switch:</b> Delegates to {@link CompactVersionManager#ensureAndSwitchVersion}
 *       which performs network downloads on background tasks and reconfigures project toolchain settings.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>No Automatic Write Action:</b> Overrides {@link #startInWriteAction()} to return {@code false}
 *       because switching or downloading a compiler requires background modal tasks and user dialogues.</li>
 *   <li><b>Read Action for Availability:</b> {@link #isAvailable} runs under {@code @RequiresReadLock} on the EDT.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.version.CompactVersionManager
 * @see dev.verloren.midnight.psi.CompactPragmaForm
 */
public class CompactSwitchCompilerVersionIntention extends PsiElementBaseIntentionAction {

  /**
   * Value record holding resolved toolchain information for display and switching.
   */
  private record TargetVersionResolution(
      @NotNull String toolchainVer,
      @NotNull String displayVer,
      boolean isInstalled
  ) {}

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getFamilyName() {
    return "Compact compiler version";
  }

  /**
   * Overridden to false because toolchain download and setup run asynchronously outside write actions.
   */
  @Override
  public boolean startInWriteAction() {
    return false;
  }

  /**
   * Checks whether the intention is available on an unsatisfied pragma form.
   */
  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    CompactPragmaForm pragma = PsiTreeUtil.getParentOfType(element, CompactPragmaForm.class, false);
    if (pragma == null) {
      return false;
    }

    String reqVer = pragma.getRequiredVersion();
    if (reqVer == null || reqVer.isEmpty()) {
      return false;
    }

    PsiElement id = pragma.getPragmaIdentifier();
    String idText = id != null ? id.getText() : "language_version";
    boolean isCompilerPragma = "compiler_version".equals(idText);

    // Check if the currently active compiler already satisfies pragma
    String activeVer = CompactToolchainUtil.getActiveCompilerVersion(project);
    if (activeVer != null) {
      String constraint = pragma.getConstraintText();
      if (constraint == null || constraint.isEmpty()) {
        constraint = reqVer;
      }
      String targetVer = isCompilerPragma ? activeVer : CompactVersionManager.getLanguageVersionForToolchain(activeVer);
      if (CompactSemVerUtil.satisfiesConstraint(targetVer, constraint)) {
        return false;
      }
    }

    TargetVersionResolution target = resolveTarget(pragma, activeVer, reqVer, isCompilerPragma);
    String versionLabel = target.toolchainVer().equals(target.displayVer())
        ? target.displayVer()
        : target.displayVer() + " (v" + target.toolchainVer() + ")";
    if (target.isInstalled()) {
      setText("Switch project compiler to Compact " + versionLabel);
    } else {
      setText("Download and use Compact " + versionLabel);
    }
    return true;
  }

  /**
   * Triggers compiler toolchain download or switch.
   */
  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    if (IntentionPreviewUtils.isIntentionPreviewActive()) {
      return;
    }
    CompactPragmaForm pragma = PsiTreeUtil.getParentOfType(element, CompactPragmaForm.class, false);
    if (pragma == null) {
      return;
    }
    String reqVer = pragma.getRequiredVersion();
    if (reqVer == null || reqVer.isEmpty()) {
      return;
    }

    PsiElement id = pragma.getPragmaIdentifier();
    String idText = id != null ? id.getText() : "language_version";
    boolean isCompilerPragma = "compiler_version".equals(idText);
    String activeVer = CompactToolchainUtil.getActiveCompilerVersion(project);

    TargetVersionResolution target = resolveTarget(pragma, activeVer, reqVer, isCompilerPragma);
    VirtualFile vFile = element.getContainingFile() != null ? element.getContainingFile().getVirtualFile() : null;
    CompactVersionManager.ensureAndSwitchVersion(project, target.toolchainVer(), vFile);
  }

  /**
   * Determines the optimal target compiler version matching the pragma constraint.
   */
  private static @NotNull TargetVersionResolution resolveTarget(
      @NotNull CompactPragmaForm pragma,
      @Nullable String activeVer,
      @NotNull String reqVer,
      boolean isCompilerPragma
  ) {
    String directToolchain = isCompilerPragma
        ? CompactVersionManager.cleanVersion(reqVer)
        : CompactVersionManager.resolveToolchainVersionForLanguage(reqVer);

    if (CompactVersionManager.isVersionInstalled(directToolchain)) {
      return new TargetVersionResolution(directToolchain, reqVer, true);
    }

    // Check if any installed toolchain satisfies the pragma constraint (highest version first)
    String constraint = pragma.getConstraintText();
    if (constraint == null || constraint.isBlank()) {
      constraint = reqVer;
    }

    SequencedMap<String, String> installed = CompactVersionManager.getInstalledVersions();
    List<String> sortedToolchains = new ArrayList<>(installed.keySet());
    sortedToolchains.sort(CompactSemVerUtil.DESCENDING_COMPARATOR);

    for (String instToolchain : sortedToolchains) {
      String cleanInst = CompactVersionManager.cleanVersion(instToolchain);
      if (activeVer != null && cleanInst.equals(CompactVersionManager.cleanVersion(activeVer))) {
        continue;
      }
      String checkVer = isCompilerPragma ? cleanInst : CompactVersionManager.getLanguageVersionForToolchain(cleanInst);
      if (CompactSemVerUtil.satisfiesConstraint(checkVer, constraint)) {
        return new TargetVersionResolution(cleanInst, checkVer, true);
      }
    }

    return new TargetVersionResolution(directToolchain, reqVer, false);
  }

  @Override
  public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    return IntentionPreviewInfo.EMPTY;
  }
}
