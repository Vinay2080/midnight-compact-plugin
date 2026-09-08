package dev.verloren.midnight.annotator;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
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

  public CompactSwitchCompilerQuickFix(@NotNull String targetVersion) {
    this(targetVersion, false);
  }

  public CompactSwitchCompilerQuickFix(@NotNull String targetVersion, boolean isCompilerPragma) {
    this.targetVersion = targetVersion;
    this.isCompilerPragma = isCompilerPragma;
  }

  @Override
  public @NotNull String getText() {
    String toolchainVer = isCompilerPragma
        ? CompactVersionManager.cleanVersion(targetVersion)
        : CompactVersionManager.resolveToolchainVersionForLanguage(targetVersion);

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
    String toolchainVer = isCompilerPragma
        ? CompactVersionManager.cleanVersion(targetVersion)
        : CompactVersionManager.resolveToolchainVersionForLanguage(targetVersion);
    CompactVersionManager.ensureAndSwitchVersion(project, toolchainVer, file != null ? file.getVirtualFile() : null);
  }
}
