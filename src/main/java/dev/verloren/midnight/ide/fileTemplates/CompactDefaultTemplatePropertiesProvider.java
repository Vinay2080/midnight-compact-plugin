package dev.verloren.midnight.ide.fileTemplates;

import com.intellij.ide.fileTemplates.DefaultTemplatePropertiesProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import dev.verloren.midnight.run.CompactToolchainUtil;
import dev.verloren.midnight.version.CompactVersionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;

/**
 * Injects dynamic Compact compiler and language version properties into File and Code Templates.
 * Provides {@code COMPACT_LANGUAGE_VERSION}, {@code LANGUAGE_VERSION}, {@code COMPACT_COMPILER_VERSION},
 * and {@code COMPILER_VERSION} based on the project's active or configured toolchain.
 */
public final class CompactDefaultTemplatePropertiesProvider implements DefaultTemplatePropertiesProvider {

  public static final String COMPACT_LANGUAGE_VERSION = "COMPACT_LANGUAGE_VERSION";
  public static final String LANGUAGE_VERSION = "LANGUAGE_VERSION";
  public static final String COMPACT_COMPILER_VERSION = "COMPACT_COMPILER_VERSION";
  public static final String COMPILER_VERSION = "COMPILER_VERSION";

  public static final String DEFAULT_FALLBACK_COMPILER_VERSION =
      CompactVersionManager.KNOWN_VERSIONS.getFirst(); // "0.34.0"
  public static final String DEFAULT_FALLBACK_LANGUAGE_VERSION =
      CompactVersionManager.getLanguageVersionForToolchain(DEFAULT_FALLBACK_COMPILER_VERSION); // "0.26.0"

  @Override
  public void fillProperties(@NotNull PsiDirectory directory, @NotNull Properties props) {
    fillVersionProperties(directory.getProject(), props);
  }

  /**
   * Fills template properties with active or fallback Compact language and compiler versions.
   */
  public static void fillVersionProperties(@Nullable Project project, @NotNull Properties props) {
    String langVer = resolveLanguageVersion(project);
    String compilerVer = resolveCompilerVersion(project);

    props.setProperty(COMPACT_LANGUAGE_VERSION, langVer);
    props.setProperty(LANGUAGE_VERSION, langVer);
    props.setProperty(COMPACT_COMPILER_VERSION, compilerVer);
    props.setProperty(COMPILER_VERSION, compilerVer);
  }

  /**
   * Resolves the active Compact language version for the given project, mapping toolchain release
   * versions to source language specifications, or falling back to the latest supported release (0.26.0).
   */
  public static @NotNull String resolveLanguageVersion(@Nullable Project project) {
    if (project != null) {
      String activeCompilerVer = CompactToolchainUtil.getActiveCompilerVersion(project);
      if (activeCompilerVer != null && !activeCompilerVer.isBlank()) {
        return CompactVersionManager.getLanguageVersionForToolchain(activeCompilerVer);
      }
    }
    return DEFAULT_FALLBACK_LANGUAGE_VERSION;
  }

  /**
   * Resolves the active compiler toolchain version for the given project, or falls back to 0.34.0.
   */
  public static @NotNull String resolveCompilerVersion(@Nullable Project project) {
    if (project != null) {
      String activeCompilerVer = CompactToolchainUtil.getActiveCompilerVersion(project);
      if (activeCompilerVer != null && !activeCompilerVer.isBlank()) {
        return CompactVersionManager.cleanVersion(activeCompilerVer);
      }
    }
    return DEFAULT_FALLBACK_COMPILER_VERSION;
  }
}
