package dev.verloren.midnight.annotator;

import com.intellij.lang.annotation.HighlightSeverity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompactCompilerOutputParser {
  private static final Pattern DIAGNOSTIC_PATTERN =
      Pattern.compile("(?:^|(?<=[\\s\\[]))([a-zA-Z]:[\\\\/][^:\r\n]+?\\.compact|[^:\r\n\\s][^:\r\n]*?\\.compact):(\\d+):(\\d+):?\\s*(?:(warning|error|info):\\s*)?(.*)", Pattern.CASE_INSENSITIVE);

  private static final Pattern EXCEPTION_PATTERN =
      Pattern.compile("(?:Exception(?:\\s+in\\s+[^:]+)?:\\s*)?([a-zA-Z]:[\\\\/][^:\r\n]+?\\.compact|[^:\r\n\\s][^:\r\n]*?\\.compact)\\s+line\\s+(\\d+),?\\s+char\\s+(\\d+):?\\s*(.*)", Pattern.CASE_INSENSITIVE);

  private CompactCompilerOutputParser() {
  }

  public static @NotNull List<CompactCompilerDiagnostic> parse(@NotNull String output) {
    List<CompactCompilerDiagnostic> diagnostics = new ArrayList<>();
    String[] lines = output.split("\r?\n");
    int i = 0;
    while (i < lines.length) {
      String line = lines[i].trim();
      if (line.isEmpty()) {
        i++;
        continue;
      }

      Matcher diagMatcher = DIAGNOSTIC_PATTERN.matcher(line);
      if (diagMatcher.find()) {
        String filePath = diagMatcher.group(1);
        int lineNum = Math.max(1, Integer.parseInt(diagMatcher.group(2)));
        int colNum = Math.max(1, Integer.parseInt(diagMatcher.group(3)));
        String severity = diagMatcher.group(4);
        String message = diagMatcher.group(5).trim();

        HighlightSeverity highlightSeverity = HighlightSeverity.ERROR;
        boolean isError = true;
        if (severity != null) {
          if (severity.equalsIgnoreCase("warning")) {
            highlightSeverity = HighlightSeverity.WARNING;
            isError = false;
          } else if (severity.equalsIgnoreCase("info")) {
            highlightSeverity = HighlightSeverity.INFORMATION;
            isError = false;
          }
        }

        diagnostics.add(new CompactCompilerDiagnostic(
            filePath,
            lineNum,
            colNum,
            lineNum,
            colNum + 1,
            message,
            isError,
            highlightSeverity
        ));
        i++;
        continue;
      }

      Matcher excMatcher = EXCEPTION_PATTERN.matcher(line);
      if (excMatcher.find()) {
        String filePath = excMatcher.group(1);
        int lineNum = Math.max(1, Integer.parseInt(excMatcher.group(2)));
        int colNum = Math.max(1, Integer.parseInt(excMatcher.group(3)));
        String inlineMessage = excMatcher.group(4).trim();
        StringBuilder messageBuilder = new StringBuilder();
        if (!inlineMessage.isEmpty()) {
          messageBuilder.append(inlineMessage);
        }

        i++;
        while (i < lines.length) {
          String nextLine = lines[i].trim();
          if (DIAGNOSTIC_PATTERN.matcher(nextLine).find()
              || EXCEPTION_PATTERN.matcher(nextLine).find()
              || nextLine.startsWith("[compactc]")
              || nextLine.startsWith("Exception in")
              || nextLine.startsWith("Exception:")) {
            break;
          }
          if (!nextLine.isEmpty()) {
            if (!messageBuilder.isEmpty()) {
              messageBuilder.append(" ");
            }
            messageBuilder.append(nextLine);
          }
          i++;
        }

        String fullMessage = messageBuilder.toString().trim();
        if (fullMessage.isEmpty()) {
          fullMessage = "Compiler error at " + filePath + ":" + lineNum + ":" + colNum;
        }

        diagnostics.add(new CompactCompilerDiagnostic(
            filePath,
            lineNum,
            colNum,
            lineNum,
            colNum + 1,
            fullMessage,
            true,
            HighlightSeverity.ERROR
        ));
        continue;
      }

      i++;
    }
    return diagnostics;
  }
}
