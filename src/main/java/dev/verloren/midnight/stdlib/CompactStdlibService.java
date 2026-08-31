package dev.verloren.midnight.stdlib;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFileFactory;
import com.intellij.testFramework.LightVirtualFile;
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
 * Thread-safe project-level service managing bundled Compact standard library and ZKIR primitives.
 */
@Service(Service.Level.PROJECT)
public final class CompactStdlibService {
  private final Project project;
  private volatile List<CompactFile> stdlibFiles;
  private final Object lock = new Object();

  public CompactStdlibService(@NotNull Project project) {
    this.project = project;
  }

  public static @NotNull CompactStdlibService getInstance(@NotNull Project project) {
    CompactStdlibService service = project.getService(CompactStdlibService.class);
    return service != null ? service : new CompactStdlibService(project);
  }

  public @NotNull List<CompactFile> getStandardLibraryFiles() {
    List<CompactFile> files = stdlibFiles;
    if (files != null && isAllValid(files)) {
      return files;
    }
    synchronized (lock) {
      files = stdlibFiles;
      if (files != null && isAllValid(files)) {
        return files;
      }
      List<CompactFile> loaded = new ArrayList<>();
      loadStdlibFile(project, "/stdlib/standard-library.compact", "standard-library.compact", loaded);
      loadStdlibFile(project, "/stdlib/zkir-v3-library.compact", "zkir-v3-library.compact", loaded);
      files = Collections.unmodifiableList(loaded);
      stdlibFiles = files;
      return files;
    }
  }

  private static boolean isAllValid(@NotNull List<CompactFile> files) {
    if (files.isEmpty()) {
      return false;
    }
    for (CompactFile f : files) {
      if (f == null || !f.isValid()) {
        return false;
      }
    }
    return true;
  }

  private static void loadStdlibFile(
      @NotNull Project project,
      @NotNull String resourcePath,
      @NotNull String fileName,
      @NotNull List<CompactFile> out
  ) {
    try (InputStream is = CompactStdlibService.class.getResourceAsStream(resourcePath)) {
      if (is != null) {
        String content = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
            .lines().collect(Collectors.joining("\n"));
        CompactFile file = (CompactFile) PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, CompactFileType.INSTANCE, content, 0L, true, false);
        if (file.getVirtualFile() instanceof LightVirtualFile lightVirtualFile) {
          lightVirtualFile.setWritable(false);
        }
        out.add(file);
      }
    } catch (Exception ignored) {
    }
  }
}
