package dev.verloren.midnight.run;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
   * {@code [prefix] path.compact:line[:col]}.
   * Supports Windows drive letters ({@code C:\...}) and Unix relative/absolute paths.
   */
  private static final Pattern DIAGNOSTIC_PATTERN =
          Pattern.compile("(?:^|(?<=[\\s\\[]))([a-zA-Z]:[\\\\/][^:\\r\\n]+?\\.compact|[^:\\r\\n\\s][^:\\r\\n]*?\\.compact):(\\d+)(?::(\\d+))?", Pattern.CASE_INSENSITIVE);

  /**
   * Pattern matching Chez Scheme exceptions emitted by the compiler backend:
   * {@code path.compact line X, char Y}.
   */
  private static final Pattern SCHEME_EXCEPTION_PATTERN =
          Pattern.compile("([a-zA-Z]:[\\\\/][^:\\r\\n]+?\\.compact|[^:\\r\\n\\s][^:\\r\\n]*?\\.compact)\\s+line\\s+(\\d+),?\\s+char\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

  /**
   * The active project context used for file resolution and navigation.
   */
  private final Project project;

  /**
   * Constructs a new console filter for the given project.
   *
   * @param project current project
   */
  public CompactConsoleFilter(@NotNull Project project) {
    this.project = project;
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
   */
  private @Nullable Result createFilterResult(
          @NotNull String line,
          int entireLength,
          @NotNull Matcher matcher
  ) {
    String rawPath = matcher.group(FILE_GROUP);
    String normalizedPath = FileUtil.toSystemIndependentName(rawPath);
    VirtualFile file = resolveFile(normalizedPath);
    if (file == null) {
      return null;
    }

    int lineNumber = Math.max(0, Integer.parseInt(matcher.group(LINE_GROUP)) - 1);
    int columnNumber = matcher.group(COL_GROUP) != null
            ? Math.max(0, Integer.parseInt(matcher.group(COL_GROUP)) - 1)
            : 0;

    int lineStart = entireLength - line.length();
    int matchStart = lineStart + matcher.start(FILE_GROUP);
    int endGroup = matcher.group(COL_GROUP) != null ? COL_GROUP : LINE_GROUP;
    int matchEnd = lineStart + matcher.end(endGroup);

    OpenFileHyperlinkInfo hyperlink = new OpenFileHyperlinkInfo(project, file, lineNumber, columnNumber);
    return new Result(matchStart, matchEnd, hyperlink);
  }

  /**
   * Resolves a target {@link VirtualFile} from a normalized file path using multi-tier fallback strategies.
   */
  private @Nullable VirtualFile resolveFile(@NotNull String normalizedPath) {
    // 1. Resolve relative to project directory (supports both production LocalFileSystem and test TempFileSystem)
    VirtualFile baseDir = ProjectUtil.guessProjectDir(project);
    if (baseDir != null) {
      VirtualFile file = resolveFromBaseDir(baseDir, normalizedPath);
      if (file != null) {
        return file;
      }
    }

    // 2. Direct path lookup on local file system (absolute paths)
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(normalizedPath);
    if (file != null) {
      return file;
    }

    // 3. WSL path translation fallback (/mnt/c/... -> C:/...)
    if (normalizedPath.startsWith("/mnt/")) {
      String winPath = FileUtil.toSystemIndependentName(CompactToolchainUtil.toWindowsPath(normalizedPath));
      file = LocalFileSystem.getInstance().findFileByPath(winPath);
      if (file != null) {
        return file;
      }
    }

    // 4. Fallback to project.getBasePath() string resolution on LocalFileSystem
    String basePath = project.getBasePath();
    if (basePath != null) {
      String baseNormalized = FileUtil.toSystemIndependentName(basePath);
      return LocalFileSystem.getInstance().findFileByPath(baseNormalized + "/" + normalizedPath);
    }

    return null;
  }

  /**
   * Resolves a relative path or synthetic drive path against the project base directory.
   */
  private @Nullable VirtualFile resolveFromBaseDir(@NotNull VirtualFile baseDir, @NotNull String normalizedPath) {
    VirtualFile file = baseDir.findFileByRelativePath(normalizedPath);
    if (file != null) {
      return file;
    }

    if (normalizedPath.startsWith("/")) {
      file = baseDir.findFileByRelativePath(normalizedPath.substring(1));
      if (file != null) {
        return file;
      }
    }

    // Drive-letter relative fallback for synthetic paths (e.g. C:/mock/contracts/... -> mock/contracts/...)
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

    return baseDir.getFileSystem().findFileByPath(normalizedPath);
  }
}
