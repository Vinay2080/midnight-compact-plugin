package dev.verloren.midnight.stdlib;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.LocalTimeCounter;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the bundled Compact Standard Library and ZKIR primitives.
 */
public final class CompactStandardLibraryProvider {

  private static final Key<List<CompactFile>> STDLIB_FILES_KEY = Key.create("COMPACT_STDLIB_FILES");

  private CompactStandardLibraryProvider() {
  }

  public static @NotNull List<CompactFile> getStandardLibraryFiles(@NotNull Project project) {
    List<CompactFile> cached = project.getUserData(STDLIB_FILES_KEY);
    if (cached != null && isAllValid(cached)) {
      return cached;
    }

    List<CompactFile> files = new ArrayList<>();
    loadStdlibFile(project, "/stdlib/standard-library.compact", "standard-library.compact", files);
    loadStdlibFile(project, "/stdlib/zkir-v3-library.compact", "zkir-v3-library.compact", files);

    List<CompactFile> unmodifiable = Collections.unmodifiableList(files);
    project.putUserData(STDLIB_FILES_KEY, unmodifiable);
    return unmodifiable;
  }

  private static boolean isAllValid(@NotNull List<CompactFile> files) {
    for (CompactFile f : files) {
      if (f == null || !f.isValid()) {
        return false;
      }
    }
    return !files.isEmpty();
  }

  private static void loadStdlibFile(
      @NotNull Project project,
      @NotNull String resourcePath,
      @NotNull String fileName,
      @NotNull List<CompactFile> out
  ) {
    try (InputStream is = CompactStandardLibraryProvider.class.getResourceAsStream(resourcePath)) {
      if (is != null) {
        String content = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
            .lines().collect(Collectors.joining("\n"));
        CompactFile file = (CompactFile) PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, CompactFileType.INSTANCE, content, LocalTimeCounter.currentTime(), true, false);
        if (file.getVirtualFile() instanceof com.intellij.testFramework.LightVirtualFile lightVirtualFile) {
          lightVirtualFile.setWritable(false);
        }
        out.add(file);
      }
    } catch (Exception ignored) {
    }
  }
}
