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
 * Filter parsing standard compiler error outputs (`file.compact:line:column: message`) and providing instant clickable links.
 */
public class CompactConsoleFilter implements Filter {
  private static final Pattern ERROR_PATTERN = Pattern.compile("([a-zA-Z0-9_./\\\\-]+?\\.compact):(\\d+):(\\d+)(.*)");
  private final Project project;

  public CompactConsoleFilter(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
    Matcher matcher = ERROR_PATTERN.matcher(line);
    if (matcher.find()) {
      String filePath = matcher.group(1);
      int lineNumber = Math.max(0, Integer.parseInt(matcher.group(2)) - 1);
      int columnNumber = Math.max(0, Integer.parseInt(matcher.group(3)) - 1);

      VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
      if (file == null && project.getBasePath() != null) {
        file = LocalFileSystem.getInstance().findFileByPath(project.getBasePath() + "/" + filePath);
      }

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
