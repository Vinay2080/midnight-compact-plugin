package dev.verloren.midnight.annotator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompactCompilerOutputParser {
  private static final Pattern DIAGNOSTIC_PATTERN =
      Pattern.compile("([a-zA-Z0-9_./\\\\-]+?\\.compact):(\\d+):(\\d+):?\\s*(?:(warning|error):\\s*)?(.*)", Pattern.CASE_INSENSITIVE);

  private static final Pattern EXCEPTION_PATTERN =
      Pattern.compile("(?:Exception:\\s*)?([a-zA-Z0-9_./\\\\-]+?\\.compact)\\s+line\\s+(\\d+)\\s+char\\s+(\\d+):?\\s*(.*)", Pattern.CASE_INSENSITIVE);

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
        boolean isError = severity == null || !severity.equalsIgnoreCase("warning");

        diagnostics.add(new CompactCompilerDiagnostic(filePath, lineNum, colNum, message, isError));
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
          if (nextLine.isEmpty()
              || DIAGNOSTIC_PATTERN.matcher(nextLine).find()
              || EXCEPTION_PATTERN.matcher(nextLine).find()
              || nextLine.startsWith("[compactc]")) {
            break;
          }
          if (!messageBuilder.isEmpty()) {
            messageBuilder.append(" ");
          }
          messageBuilder.append(nextLine);
          i++;
        }

        String fullMessage = messageBuilder.toString().trim();
        if (fullMessage.isEmpty()) {
          fullMessage = "Compiler error at " + filePath + ":" + lineNum + ":" + colNum;
        }

        diagnostics.add(new CompactCompilerDiagnostic(filePath, lineNum, colNum, fullMessage, true));
        continue;
      }

      i++;
    }
    return diagnostics;
  }
}
