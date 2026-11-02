package dev.verloren.midnight.run;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Console filter that detects Compact compiler diagnostics and converts location references into clickable editor hyperlinks.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Implements {@link Filter} and {@link DumbAware} to intercept standard output and standard error lines printed in the
 * execution console during Compact contract compilation. When the compiler outputs an error, warning, or diagnostic coordinate
 * matching standard GNU formats ({@code file.compact:line:col}) or Chez Scheme exception formats ({@code file.compact line X, char Y}),
 * this filter creates an {@link OpenFileHyperlinkInfo} enabling developers to jump directly to the source coordinate with a single click.</p>
 *
 * <h3>Pattern Matching Pipeline</h3>
 * <pre>
 *   Console Output Line: "contracts/counter.compact:12:5: error: undefined symbol 'count'"
 *                                 │
 *                                 ▼
 *   [CompactConsoleFilter.applyFilter]
 *                                 │
 *                                 ├─► Match GNU diagnostic: DIAGNOSTIC_PATTERN
 *                                 └─► Match Scheme exception: SCHEME_EXCEPTION_PATTERN
 *                                 │
 *                                 ▼
 *   [VirtualFile Resolution]
 *                                 │
 *                                 ├─► Project relative: ProjectUtil.guessProjectDir(project)
 *                                 ├─► LocalFileSystem: findFileByPath(normalizedPath)
 *                                 └─► WSL translation: CompactToolchainUtil.toWindowsPath(path)
 *                                 │
 *                                 ▼
 *   [Construct OpenFileHyperlinkInfo(project, file, 11, 4)]
 *                                 │
 *                                 ▼
 *   Highlight "contracts/counter.compact:12:5" as interactive clickable link in console
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Console Processing:</b> Invoked on background console reader threads as output streams from the running compiler process.</li>
 *   <li><b>Dumb Mode Resilience:</b> Implements {@link DumbAware} to maintain hyperlink resolution during background indexing.</li>
 * </ul>
 *
 * @see Filter
 * @see DumbAware
 * @see OpenFileHyperlinkInfo
 * @see CompactRunProfileState
 */
public class CompactConsoleFilter implements Filter, DumbAware {

  /**
   * Group index for the file path capture in coordinate regexes.
   */
  private static final int FILE_GROUP = 1;

  /**
   * Group index for the line number capture in coordinate regexes.
   */
  private static final int LINE_GROUP = 2;

  /**
   * Group index for the column number capture in coordinate regexes.
   */
  private static final int COL_GROUP = 3;

  /**
   * Pattern matching standard compiler diagnostics:
   * {@code [prefix] path.compact:line[:col[-endCol]]}.
   * Supports Windows drive letters ({@code C:\...}), Unix paths, and optional column ranges.
   */
  private static final Pattern DIAGNOSTIC_PATTERN =
          Pattern.compile("(?:^|(?<=[\\s\\[]))([a-zA-Z]:[\\\\/][^:\\r\\n]+?\\.compact|[^:\\r\\n\\s][^:\\r\\n]*?\\.compact):(\\d+)(?::(\\d+)(?:-\\d+|:\\d+:\\d+)?)?", Pattern.CASE_INSENSITIVE);

  /**
   * Pattern matching Chez Scheme exceptions emitted by the compiler backend:
   * {@code path.compact line X, char Y}.
   */
  private static final Pattern SCHEME_EXCEPTION_PATTERN =
          Pattern.compile("([a-zA-Z]:[\\\\/][^:\\r\\n]+?\\.compact|[^:\\r\\n\\s][^:\\r\\n]*?\\.compact)\\s+line\\s+(\\d+),?\\s+char\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

  /**
   * Cached reference to the first diagnostic hyperlink encountered during the console stream.
   * Enables automated caret navigation to the primary compiler error upon build failure.
   */
  private final AtomicReference<OpenFileHyperlinkInfo> firstErrorHyperlink = new AtomicReference<>();

  /**
   * The active project context used for file resolution and navigation.
   */
  private final Project project;

  /**
   * Optional path to the primary contract being compiled, used for disambiguating bare filenames.
   */
  private final @Nullable String targetFilePath;

  /**
   * Constructs a new console filter for the given project.
   *
   * @param project current project
   */
  public CompactConsoleFilter(@NotNull Project project) {
    this(project, null);
  }

  /**
   * Constructs a new console filter aware of the target contract file path.
   *
   * @param project        current project
   * @param targetFilePath optional absolute or project-relative path to the compiled contract
   */
  public CompactConsoleFilter(@NotNull Project project, @Nullable String targetFilePath) {
    this.project = project;
    this.targetFilePath = targetFilePath;
  }

  /**
   * Returns the first error hyperlink resolved during this compilation run, if any.
   *
   * @return first {@link OpenFileHyperlinkInfo} resolved, or {@code null} if no error links were generated
   */
  public @Nullable OpenFileHyperlinkInfo getFirstErrorHyperlink() {
    return firstErrorHyperlink.get();
  }

  /**
   * Filters a single output line, returning a clickable {@link Result} hyperlink if a diagnostic coordinate is detected.
   *
   * @param line         text of the current console line
   * @param entireLength total character offset of the console buffer up to the end of {@code line}
   * @return {@link Result} hyperlink spanning the match, or {@code null} if no coordinate found
   */
  @Override
  public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
    Matcher diagMatcher = DIAGNOSTIC_PATTERN.matcher(line);
    if (diagMatcher.find()) {
      return createFilterResult(line, entireLength, diagMatcher);
    }

    Matcher excMatcher = SCHEME_EXCEPTION_PATTERN.matcher(line);
    if (excMatcher.find()) {
      return createFilterResult(line, entireLength, excMatcher);
    }

    return null;
  }

  /**
   * Creates an interactive hyperlink result for the matched coordinate pattern.
   *
   * @param line         full line text from console output
   * @param entireLength cumulative character count in console buffer ending at this line
   * @param matcher      regex matcher containing captured path and coordinate groups
   * @return {@link Result} hyperlink spanning the match, or {@code null} if file cannot be found
   */
  private @Nullable Result createFilterResult(
          @NotNull String line,
          int entireLength,
          @NotNull Matcher matcher
  ) {
    String rawPath = matcher.group(FILE_GROUP);
    String normalizedPath = rawPath.replace('\\', '/');
    VirtualFile file = resolveFile(normalizedPath);
    if (file == null) {
      return null;
    }

    // Convert 1-based compiler line and column coordinates into 0-based IntelliJ editor offsets
    int lineNumber = Math.max(0, Integer.parseInt(matcher.group(LINE_GROUP)) - 1);
    int columnNumber = matcher.group(COL_GROUP) != null
            ? Math.max(0, Integer.parseInt(matcher.group(COL_GROUP)) - 1)
            : 0;

    // Calculate absolute buffer offsets: lineStart is the beginning of 'line' in the full console buffer
    int lineStart = entireLength - line.length();
    int matchStart = lineStart + matcher.start(FILE_GROUP);
    int matchEnd = lineStart + matcher.end();

    OpenFileHyperlinkInfo hyperlink = new OpenFileHyperlinkInfo(project, file, lineNumber, columnNumber);
    firstErrorHyperlink.compareAndSet(null, hyperlink);
    return new Result(matchStart, matchEnd, hyperlink);
  }

  /**
   * Resolves a target {@link VirtualFile} from a normalized file path using multi-tier fallback strategies.
   *
   * @param normalizedPath system-independent file path extracted from compiler output
   * @return resolved {@link VirtualFile}, or {@code null} if unresolvable
   */
  private @Nullable VirtualFile resolveFile(@NotNull String normalizedPath) {
    // 1. Resolve against targetFilePath if provided (e.g. for bare filenames or target-relative paths)
    if (targetFilePath != null && !targetFilePath.isEmpty()) {
      VirtualFile targetFile = resolveTargetFile(targetFilePath, normalizedPath);
      if (targetFile != null) {
        return targetFile;
      }
    }

    // 2. Resolve relative to project directory (supports both production LocalFileSystem and test TempFileSystem)
    VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
    if (baseDir != null) {
      VirtualFile file = resolveFromBaseDir(baseDir, normalizedPath);
      if (file != null) {
        return file;
      }
    }

    // 3. Fallback across system paths (absolute, WSL, and basePath concatenation)
    VirtualFile systemFile = resolveFromSystemPaths(normalizedPath);
    if (systemFile != null) {
      return systemFile;
    }

    // 4. Deep tree search fallback in project baseDir for bare filenames in subdirectories
    if (baseDir != null) {
      String simpleName = normalizedPath.contains("/")
              ? normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1)
              : normalizedPath;
      return findFileInTree(baseDir, simpleName);
    }

    return null;
  }

  /**
   * Resolves paths against absolute filesystem locations, WSL mounts, or basePath concatenation.
   *
   * @param normalizedPath system-independent file path
   * @return matching {@link VirtualFile}, or {@code null} if not found
   */
  private @Nullable VirtualFile resolveFromSystemPaths(@NotNull String normalizedPath) {
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(normalizedPath);
    if (file != null) {
      return file;
    }

    if (normalizedPath.startsWith("/mnt/")) {
      String winPath = CompactToolchainUtil.toWindowsPath(normalizedPath).replace('\\', '/');
      file = LocalFileSystem.getInstance().findFileByPath(winPath);
      if (file != null) {
        return file;
      }
    }

    String basePath = project.getBasePath();
    if (basePath != null) {
      String baseNormalized = basePath.replace('\\', '/');
      return LocalFileSystem.getInstance().findFileByPath(baseNormalized + "/" + normalizedPath);
    }

    return null;
  }

  /**
   * Resolves a diagnostic path against a known target contract file.
   *
   * @param targetPath     configured primary contract path
   * @param normalizedPath diagnostic path to resolve
   * @return matching {@link VirtualFile}, or {@code null} if unresolvable
   */
  private @Nullable VirtualFile resolveTargetFile(@NotNull String targetPath, @NotNull String normalizedPath) {
    String normalizedTarget = targetPath.replace('\\', '/');
    VirtualFile targetVirtualFile = LocalFileSystem.getInstance().findFileByPath(normalizedTarget);
    if (targetVirtualFile == null) {
      VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
      if (baseDir != null) {
        targetVirtualFile = resolveFromBaseDir(baseDir, normalizedTarget);
      }
    }

    if (targetVirtualFile == null) {
      return null;
    }

    // Check if normalizedPath matches target file name directly (e.g. "CompileTargetContract.compact")
    String simpleName = normalizedPath.contains("/")
            ? normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1)
            : normalizedPath;
    if (targetVirtualFile.getName().equalsIgnoreCase(simpleName)) {
      return targetVirtualFile;
    }

    // Check if normalizedPath is relative to the target file's containing folder
    VirtualFile parentDir = targetVirtualFile.getParent();
    return parentDir != null ? parentDir.findFileByRelativePath(normalizedPath) : null;
  }

  /**
   * Iteratively searches directory children for a file matching {@code fileName} using breadth-first search.
   *
   * @param rootDir  root directory to search
   * @param fileName target file name to locate
   * @return matching {@link VirtualFile}, or {@code null} if not found
   */
  private @Nullable VirtualFile findFileInTree(@NotNull VirtualFile rootDir, @NotNull String fileName) {
    ArrayDeque<VirtualFile> queue = new ArrayDeque<>();
    queue.add(rootDir);
    int visited = 0;
    while (!queue.isEmpty() && visited < 500) {
      VirtualFile current = queue.poll();
      visited++;
      for (VirtualFile child : current.getChildren()) {
        if (child.isDirectory()) {
          String name = child.getName();
          if (!name.startsWith(".") && !"build".equals(name) && !"node_modules".equals(name) && !"out".equals(name)) {
            queue.add(child);
          }
        } else if (child.getName().equalsIgnoreCase(fileName)) {
          return child;
        }
      }
    }
    return null;
  }

  /**
   * Resolves a relative path or synthetic drive path against the project base directory.
   *
   * @param baseDir        project base virtual file directory
   * @param normalizedPath system-independent path to locate relative to baseDir
   * @return matching {@link VirtualFile}, or {@code null} if not found
   */
  private @Nullable VirtualFile resolveFromBaseDir(@NotNull VirtualFile baseDir, @NotNull String normalizedPath) {
    // 1. Attempt direct relative path lookup
    VirtualFile file = baseDir.findFileByRelativePath(normalizedPath);
    if (file != null) {
      return file;
    }

    // 2. Strip leading slash if present (e.g. "/src/counter.compact" -> "src/counter.compact")
    if (normalizedPath.startsWith("/")) {
      file = baseDir.findFileByRelativePath(normalizedPath.substring(1));
      if (file != null) {
        return file;
      }
    }

    // 3. Drive-letter relative fallback for synthetic paths (e.g. C:/mock/contracts/... -> mock/contracts/...)
    // Crucial for test environments using TempFileSystem where mock drive letters are prepended
    if (normalizedPath.length() >= 3 && Character.isLetter(normalizedPath.charAt(0)) && normalizedPath.charAt(1) == ':') {
      String relativeFromDrive = normalizedPath.substring(2);
      while (relativeFromDrive.startsWith("/")) {
        relativeFromDrive = relativeFromDrive.substring(1);
      }
      file = baseDir.findFileByRelativePath(relativeFromDrive);
      if (file != null) {
        return file;
      }
    }

    // 4. Attempt resolution on the specific FileSystem backing baseDir
    return baseDir.getFileSystem().findFileByPath(normalizedPath);
  }
}
