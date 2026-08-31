package dev.verloren.midnight.stdlib;

import com.intellij.openapi.project.Project;

import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;


import java.util.List;


/**
 * Manages the bundled Compact Standard Library and ZKIR primitives.
 *
 * @deprecated Use {@link CompactStdlibService#getInstance(Project)} instead for thread-safe access.
 */
@Deprecated
public final class CompactStandardLibraryProvider {

  private CompactStandardLibraryProvider() {
  }

  public static @NotNull List<CompactFile> getStandardLibraryFiles(@NotNull Project project) {
    return CompactStdlibService.getInstance(project).getStandardLibraryFiles();
  }
}
