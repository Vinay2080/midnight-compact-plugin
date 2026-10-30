package dev.verloren.midnight.run;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
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
 * <p>Implements {@link Filter} to intercept standard output and standard error lines printed in the execution
 * console during Compact contract compilation. When the compiler outputs an error, warning, or location tag
 * matching the canonical format {@code "file.compact:line:column"}, this filter creates an {@link OpenFileHyperlinkInfo}
 * enabling developers to jump directly to the precise source coordinate with a single click.</p>
 *
 * <h3>Pattern Matching Pipeline</h3>
 * <pre>
 *   Console Output Line: "contracts/counter.compact:12:5: error: undefined symbol 'count'"
 *                                 │
 *                                 ▼
 *   [CompactConsoleFilter.applyFilter]
 *                                 │
 *                                 ├─► Match regex: ([a-zA-Z0-9_./\\-]+?\.compact):(\d+):(\d+)
 *                                 │     Group 1: "contracts/counter.compact" (file path)
 *                                 │     Group 2: "12"                        (1-based line)
 *                                 │     Group 3: "5"                         (1-based column)
 *                                 │
 *                                 ▼
 *   [VirtualFile Resolution]
 *                                 │
 *                                 ├─► LocalFileSystem.findFileByPath(filePath)
 *                                 └─► Fallback: LocalFileSystem.findFileByPath(projectBase + "/" + filePath)
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
 * </ul>
 *
 * @see Filter
 * @see OpenFileHyperlinkInfo
 * @see CompactRunProfileState
 */
public class CompactConsoleFilter implements Filter {

  /**
   * Regular expression pattern matching Compact file paths with line and column numbers:
   * e.g., {@code "src/contract.compact:10:4"}.
   */
  private static final Pattern ERROR_PATTERN = Pattern.compile("([a-zA-Z0-9_./\\\\-]+?\\.compact):(\\d+):(\\d+)(.*)");

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
   * @param line text of the current console line
   * @param entireLength total character offset of the console buffer up to the end of {@code line}
   * @return {@link Result} hyperlink spanning the match, or {@code null} if no coordinate found
   */
  @Override
  public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
    // 1. Scan line for Compact file diagnostic coordinates
    Matcher matcher = ERROR_PATTERN.matcher(line);
    if (matcher.find()) {
      String filePath = matcher.group(1);
      // Convert 1-based compiler output indices into 0-based IntelliJ editor offsets
      int lineNumber = Math.max(0, Integer.parseInt(matcher.group(2)) - 1);
      int columnNumber = Math.max(0, Integer.parseInt(matcher.group(3)) - 1);

      // 2. Resolve VirtualFile via an absolute path or project-relative path
      VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
      if (file == null && project.getBasePath() != null) {
        file = LocalFileSystem.getInstance().findFileByPath(project.getBasePath() + "/" + filePath);
      }

      // 3. Create interactive OpenFileHyperlinkInfo if a virtual file exists on disk
      if (file != null) {
        int matchStart = entireLength - line.length() + matcher.start();
        int matchEnd = entireLength - line.length() + matcher.end(3);
        OpenFileHyperlinkInfo hyperlink = new OpenFileHyperlinkInfo(project, file, lineNumber, columnNumber);
        return new Result(matchStart, matchEnd, hyperlink);
      }
    }
    return null;
  }
}
