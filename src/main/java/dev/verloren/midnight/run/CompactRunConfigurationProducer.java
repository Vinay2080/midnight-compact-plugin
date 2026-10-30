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
 * Produces and recognizes {@link CompactRunConfiguration} instances dynamically from the IDE context.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as a {@code runConfigurationProducer} extension in {@code plugin.xml}. Extends
 * {@link LazyRunConfigurationProducer} to automatically synthesize run configurations on-the-fly when
 * a developer clicks the run button on a Compact source file, invokes Run via the context menu in the Project View,
 * or clicks a gutter run marker created by {@link CompactRunLineMarkerContributor}.</p>
 *
 * <h3>Context Resolution Pipeline</h3>
 * <pre>
 *   [User triggers Run in editor / gutter / project tree]
 *                          │
 *                          ▼
 *   [CompactRunConfigurationProducer.setupConfigurationFromContext]
 *                          │
 *                          ├─► findCompactFileInfo(context)
 *                          │     ├─► Check PSI element &amp; containing a file
 *                          │     └─► Verify .compact extension / CompactFileType
 *                          │
 *                          ▼
 *   [Configure CompactRunConfiguration]
 *                          │
 *                          ├─► Set name: "Compile &lt;fileName&gt;"
 *                          ├─► Set compactFilePath: virtualFile.getPath()
 *                          ├─► Derive outputDirectory from MidnightSettingsState
 *                          └─► Configure skipZk from default user preference
 *                          │
 *                          ▼
 *                   Return true (Configuration Ready)
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Read Action:</b> Invoked on EDT or background read threads during context menu creation or gutter icon clicks.
 *       All PSI inspections are read-only.</li>
 * </ul>
 *
 * @see CompactRunConfiguration
 * @see CompactRunLineMarkerContributor
 * @see CompactToolchainUtil
 */
public class CompactRunConfigurationProducer extends LazyRunConfigurationProducer<CompactRunConfiguration> {

  /**
   * Returns the configuration factory used to instantiate new Compact run configurations.
   *
   * @return the primary {@link ConfigurationFactory} from {@link CompactConfigurationType}
   */
  @Override
  public @NotNull ConfigurationFactory getConfigurationFactory() {
    return CompactConfigurationType.getInstance().getConfigurationFactories()[0];
  }

  /**
   * Populates a newly created {@link CompactRunConfiguration} with settings inferred from the current context.
   *
   * @param configuration the configuration instance being initialized
   * @param context the current IDE execution context
   * @param sourceElement reference to be populated with the PSI anchor element
   * @return {@code true} if a Compact file was found and configuration successfully setup; {@code false} otherwise
   */
  @Override
  protected boolean setupConfigurationFromContext(
      @NotNull CompactRunConfiguration configuration,
      @NotNull ConfigurationContext context,
      @NotNull Ref<PsiElement> sourceElement
  ) {
    // 1. Locate Compact file info from the current PSI context
    CompactFileInfo info = findCompactFileInfo(context);
    if (info == null) {
      return false;
    }

    // 2. Anchor the source element for IDE navigation tracking
    sourceElement.set(info.location());

    // 3. Set the display title and target contract file path
    configuration.setName("Compile " + info.vFile().getName());
    configuration.setCompactFilePath(info.vFile().getPath());

    // 4. Derive output directory incorporating global settings preferences
    MidnightSettingsState state = MidnightSettingsState.getInstance();
    String baseOutputDir = (state != null && state.defaultOutputDir != null && !state.defaultOutputDir.trim().isEmpty())
        ? state.defaultOutputDir.trim()
        : "gen";
    String outputDir = CompactToolchainUtil.deriveOutputDirectory(info.psiFile().getProject(), info.vFile().getPath(), baseOutputDir);
    configuration.setOutputDirectory(outputDir);

    // 5. Apply default ZK proving bypass setting
    boolean skipZk = state == null || state.skipZkDefault;
    configuration.setSkipZk(skipZk);

    return true;
  }

  /**
   * Checks whether the given existing configuration was generated from the current context.
   *
   * @param configuration the existing run configuration to test
   * @param context the active IDE execution context
   * @return {@code true} if configuration targets the contract file in {@code context}; {@code false} otherwise
   */
  @Override
  public boolean isConfigurationFromContext(
      @NotNull CompactRunConfiguration configuration,
      @NotNull ConfigurationContext context
  ) {
    CompactFileInfo info = findCompactFileInfo(context);
    if (info == null) {
      return false;
    }

    // Compare paths using normalized forward slashes
    String currentPath = info.vFile().getPath().replace('\\', '/');
    String configPath = configuration.getCompactFilePath().replace('\\', '/');

    return currentPath.equals(configPath);
  }

  /**
   * Lightweight record capturing file and PSI context references for a target Compact file.
   *
   * @param location the specific PSI element triggering the run action
   * @param psiFile they contain a Compact PSI file
   * @param vFile the virtual file on disk
   */
  private record CompactFileInfo(@NotNull PsiElement location, @NotNull PsiFile psiFile, @NotNull VirtualFile vFile) {}

  /**
   * Inspects the provided configuration context to locate an enclosing Compact source file.
   *
   * @param context the IDE context
   * @return {@link CompactFileInfo} if a valid Compact file is identified; {@code null} otherwise
   */
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
