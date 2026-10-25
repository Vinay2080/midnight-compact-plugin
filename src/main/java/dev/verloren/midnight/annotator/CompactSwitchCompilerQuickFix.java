package dev.verloren.midnight.annotator;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Quick-fix and intention action to switch or download a compatible Compact compiler version.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Implements both {@link LocalQuickFix} and {@link com.intellij.codeInsight.intention.IntentionAction}:
 * <ul>
 *   <li><b>Dual Usage:</b> Attached to {@link dev.verloren.midnight.inspection.CompactPragmaVersionInspection}
 *       problem descriptors and available directly in the editor intention menu.</li>
 *   <li><b>Toolchain Integration:</b> Delegates to {@link CompactVersionManager#ensureAndSwitchVersion}
 *       to automatically download missing compiler releases from GitHub or switch active project SDKs.</li>
 *   <li><b>Dynamic User Label:</b> Dynamically renders action text distinguishing between switching to
 *       an already installed version vs downloading and installing a new release.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Invocation:</b> Executed on the Event Dispatch Thread (EDT) when selected by the user.</li>
 *   <li><b>Background Download:</b> Toolchain download operations are spawned in background progress tasks
 *       by {@link CompactVersionManager}.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.version.CompactVersionManager
 * @see dev.verloren.midnight.inspection.CompactPragmaVersionInspection
 */
public class CompactSwitchCompilerQuickFix extends BaseIntentionAction implements LocalQuickFix {

  /**
   * The target language or compiler version string requested.
   */
  private final String targetVersion;

  /**
   * {@code true} if targeting a {@code compiler_version} pragma; {@code false} for {@code language_version}.
   */
  private final boolean isCompilerPragma;

  /**
   * Explicit toolchain release tag override (e.g. {@code "0.2.1"}), or {@code null}.
   */
  private final @Nullable String explicitToolchainVersion;

  public CompactSwitchCompilerQuickFix(@NotNull String targetVersion) {
    this(targetVersion, false, null);
  }

  public CompactSwitchCompilerQuickFix(@NotNull String targetVersion, boolean isCompilerPragma) {
    this(targetVersion, isCompilerPragma, null);
  }

  public CompactSwitchCompilerQuickFix(
      @NotNull String targetVersion,
      boolean isCompilerPragma,
      @Nullable String explicitToolchainVersion
  ) {
    this.targetVersion = targetVersion;
    this.isCompilerPragma = isCompilerPragma;
    this.explicitToolchainVersion = explicitToolchainVersion;
  }

  /**
   * Resolves the concrete compiler toolchain version string.
   */
  public @NotNull String getToolchainVersion() {
    if (explicitToolchainVersion != null && !explicitToolchainVersion.isBlank()) {
      return CompactVersionManager.cleanVersion(explicitToolchainVersion);
    }
    return isCompilerPragma
        ? CompactVersionManager.cleanVersion(targetVersion)
        : CompactVersionManager.resolveToolchainVersionForLanguage(targetVersion);
  }

  /**
   * Renders the dynamic action label depending on whether the compiler is already installed.
   */
  @Override
  public @NotNull String getText() {
    String toolchainVer = getToolchainVersion();
    String displayVer = isCompilerPragma
        ? toolchainVer
        : (explicitToolchainVersion != null
            ? CompactVersionManager.getLanguageVersionForToolchain(toolchainVer)
            : targetVersion);

    if (CompactVersionManager.isVersionInstalled(toolchainVer)) {
      if (toolchainVer.equals(displayVer)) {
        return "Switch project compiler to Compact " + displayVer;
      }
      return "Switch project compiler to Compact v" + toolchainVer + " (Language v" + displayVer + ")";
    }
    if (toolchainVer.equals(displayVer)) {
      return "Download and switch project compiler to Compact " + displayVer;
    }
    return "Download and switch project compiler to Compact v" + toolchainVer + " (Language v" + displayVer + ")";
  }

  @Override
  public @NotNull String getName() {
    return getText();
  }

  @Override
  public @NotNull String getFamilyName() {
    return "Compact compiler version";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiFile file = descriptor.getPsiElement() != null ? descriptor.getPsiElement().getContainingFile() : null;
    invoke(project, null, file);
  }

  /**
   * Invokes the toolchain switch or download routine via {@link CompactVersionManager}.
   */
  @Override
  public void invoke(@NotNull Project project, @Nullable Editor editor, @Nullable PsiFile file) throws IncorrectOperationException {
    if (IntentionPreviewUtils.isIntentionPreviewActive()) {
      return;
    }
    String toolchainVer = getToolchainVersion();
    CompactVersionManager.ensureAndSwitchVersion(project, toolchainVer, file != null ? file.getVirtualFile() : null);
  }

  @Override
  public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
    return IntentionPreviewInfo.EMPTY;
  }

  @Override
  public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    return IntentionPreviewInfo.EMPTY;
  }
}
