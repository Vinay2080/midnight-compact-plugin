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
import org.jetbrains.annotations.Nullable;

/**
 * Automatically creates and matches {@link CompactRunConfiguration} instances from context (e.g., current file, gutter run icon, top run bar).
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
    CompactFileInfo info = findCompactFileInfo(context);
    if (info == null) {
      return false;
    }

    sourceElement.set(info.location());

    configuration.setName("Compile " + info.vFile().getName());
    configuration.setCompactFilePath(info.vFile().getPath());

    MidnightSettingsState state = MidnightSettingsState.getInstance();
    String baseOutputDir = (state != null && state.defaultOutputDir != null && !state.defaultOutputDir.trim().isEmpty())
        ? state.defaultOutputDir.trim()
        : "gen";
    String outputDir = CompactToolchainUtil.deriveOutputDirectory(info.psiFile().getProject(), info.vFile().getPath(), baseOutputDir);
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
    CompactFileInfo info = findCompactFileInfo(context);
    if (info == null) {
      return false;
    }

    String currentPath = info.vFile().getPath().replace('\\', '/');
    String configPath = configuration.getCompactFilePath().replace('\\', '/');

    return currentPath.equals(configPath);
  }

  private record CompactFileInfo(@NotNull PsiElement location, @NotNull PsiFile psiFile, @NotNull VirtualFile vFile) {}

  private static @Nullable CompactFileInfo findCompactFileInfo(@NotNull ConfigurationContext context) {
    PsiElement location = context.getPsiLocation();
    if (location == null) {
      return null;
    }

    PsiFile psiFile = location instanceof PsiFile f ? f : location.getContainingFile();
    if (psiFile == null || !(psiFile instanceof CompactFile || psiFile.getFileType() instanceof dev.verloren.midnight.CompactFileType || psiFile.getName().endsWith(".compact"))) {
      return null;
    }

    VirtualFile vFile = psiFile.getVirtualFile() != null ? psiFile.getVirtualFile() : psiFile.getViewProvider().getVirtualFile();
    if (vFile == null) {
      return null;
    }

    return new CompactFileInfo(location, psiFile, vFile);
  }
}
