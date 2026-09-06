package dev.verloren.midnight.annotator;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import dev.verloren.midnight.settings.MidnightProjectSettings;
import dev.verloren.midnight.settings.MidnightSettingsState;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Quick-fix attached to compiler version mismatch annotations and inspections.
 */
public class CompactSwitchCompilerQuickFix extends BaseIntentionAction implements LocalQuickFix {
  private final String targetVersion;

  public CompactSwitchCompilerQuickFix(@NotNull String targetVersion) {
    this.targetVersion = targetVersion;
  }

  @Override
  public @NotNull String getText() {
    String toolchainVer = CompactVersionManager.resolveToolchainVersionForLanguage(targetVersion);
    if (CompactVersionManager.isVersionInstalled(toolchainVer)) {
      if (toolchainVer.equals(targetVersion)) {
        return "Switch project compiler to Compact " + targetVersion;
      }
      return "Switch project compiler to Compact v" + toolchainVer + " (Language v" + targetVersion + ")";
    }
    if (toolchainVer.equals(targetVersion)) {
      return "Download and switch project compiler to Compact " + targetVersion;
    }
    return "Download and switch project compiler to Compact v" + toolchainVer + " (Language v" + targetVersion + ")";
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
    String toolchainVer = CompactVersionManager.resolveToolchainVersionForLanguage(targetVersion);
    if (CompactVersionManager.isVersionInstalled(toolchainVer)) {
      applySelectedVersion(project, toolchainVer, file);
    } else {
      ProgressManager.getInstance().run(new Task.Backgroundable(project, "Downloading Compact Compiler v" + toolchainVer, true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          boolean success = CompactVersionManager.installVersion(toolchainVer, project.getBasePath(), indicator);
          if (success) {
            applySelectedVersion(project, toolchainVer, file);
          }
        }
      });
    }
  }

  private void applySelectedVersion(@NotNull Project project, @NotNull String toolchainVer, @Nullable PsiFile file) {
    MidnightProjectSettings.getInstance(project).selectedCompilerVersion = toolchainVer;
    String installedExe = CompactVersionManager.getInstalledExecutable(toolchainVer);
    if (installedExe != null) {
      MidnightSettingsState state = MidnightSettingsState.getInstance();
      if (state != null) {
        state.compilerPath = installedExe;
      }
    }
    CompactProblemUtil.clearProblemsAndRestart(project, file != null ? file.getVirtualFile() : null);
  }
}
