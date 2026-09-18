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
 * Quick-fix attached to compiler version mismatch annotations and inspections.
 */
public class CompactSwitchCompilerQuickFix extends BaseIntentionAction implements LocalQuickFix {
  private final String targetVersion;
  private final boolean isCompilerPragma;
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

  public @NotNull String getToolchainVersion() {
    if (explicitToolchainVersion != null && !explicitToolchainVersion.isBlank()) {
      return CompactVersionManager.cleanVersion(explicitToolchainVersion);
    }
    return isCompilerPragma
        ? CompactVersionManager.cleanVersion(targetVersion)
        : CompactVersionManager.resolveToolchainVersionForLanguage(targetVersion);
  }

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
