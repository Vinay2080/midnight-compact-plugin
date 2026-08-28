package dev.verloren.midnight.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.settings.MidnightSettingsState;
import org.jetbrains.annotations.NotNull;

/**
 * Automatically creates and matches {@link CompactRunConfiguration} instances from context (e.g. current file, gutter run icon, top run bar).
 */
public class CompactRunConfigurationProducer extends LazyRunConfigurationProducer<CompactRunConfiguration> {

  @Override
  public @NotNull ConfigurationFactory getConfigurationFactory() {
    return CompactConfigurationType.getInstance().getConfigurationFactories()[0];
  }

  @Override
  protected boolean setupConfigurationFromContext(
      @NotNull CompactRunConfiguration configuration,
      @NotNull ConfigurationContext context,
      @NotNull Ref<PsiElement> sourceElement
  ) {
    PsiElement location = context.getPsiLocation();
    if (location == null) {
      return false;
    }

    PsiFile psiFile = location instanceof PsiFile ? (PsiFile) location : location.getContainingFile();
    if (psiFile == null || !(psiFile instanceof CompactFile || psiFile.getFileType() instanceof dev.verloren.midnight.CompactFileType || psiFile.getName().endsWith(".compact"))) {
      return false;
    }

    VirtualFile vFile = psiFile.getVirtualFile() != null ? psiFile.getVirtualFile() : psiFile.getViewProvider().getVirtualFile();
    if (vFile == null) {
      return false;
    }

    sourceElement.set(location);

    configuration.setName("Compile " + vFile.getName());
    configuration.setCompactFilePath(vFile.getPath());

    MidnightSettingsState state = MidnightSettingsState.getInstance();
    String baseOutputDir = (state != null && state.defaultOutputDir != null && !state.defaultOutputDir.trim().isEmpty())
        ? state.defaultOutputDir.trim()
        : "gen";
    String outputDir = CompactToolchainUtil.deriveOutputDirectory(psiFile.getProject(), vFile.getPath(), baseOutputDir);
    configuration.setOutputDirectory(outputDir);

    boolean skipZk = state == null || state.skipZkDefault;
    configuration.setSkipZk(skipZk);

    return true;
  }

  @Override
  public boolean isConfigurationFromContext(
      @NotNull CompactRunConfiguration configuration,
      @NotNull ConfigurationContext context
  ) {
    PsiElement location = context.getPsiLocation();
    if (location == null) {
      return false;
    }

    PsiFile psiFile = location instanceof PsiFile ? (PsiFile) location : location.getContainingFile();
    if (psiFile == null || !(psiFile instanceof CompactFile || psiFile.getFileType() instanceof dev.verloren.midnight.CompactFileType || psiFile.getName().endsWith(".compact"))) {
      return false;
    }

    VirtualFile vFile = psiFile.getVirtualFile() != null ? psiFile.getVirtualFile() : psiFile.getViewProvider().getVirtualFile();
    if (vFile == null) {
      return false;
    }

    String currentPath = vFile.getPath().replace('\\', '/');
    String configPath = configuration.getCompactFilePath().replace('\\', '/');

    return currentPath.equals(configPath);
  }
}
