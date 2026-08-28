package dev.verloren.midnight.annotator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompactCompilerOutputParser {
  private static final Pattern DIAGNOSTIC_PATTERN =
      Pattern.compile("([a-zA-Z0-9_./\\\\-]+?\\.compact):(\\d+):(\\d+):?\\s*(?:(warning|error):\\s*)?(.*)", Pattern.CASE_INSENSITIVE);

  private CompactCompilerOutputParser() {
  }

  public static @NotNull List<CompactCompilerDiagnostic> parse(@NotNull String output) {
    List<CompactCompilerDiagnostic> diagnostics = new ArrayList<>();
    for (String rawLine : output.split("\r?\n")) {
      String line = rawLine.trim();
      if (line.isEmpty()) {
        continue;
      }
      Matcher matcher = DIAGNOSTIC_PATTERN.matcher(line);
      if (matcher.find()) {
        String filePath = matcher.group(1);
        int lineNum = Math.max(1, Integer.parseInt(matcher.group(2)));
        int colNum = Math.max(1, Integer.parseInt(matcher.group(3)));
        String severity = matcher.group(4);
        String message = matcher.group(5).trim();
        boolean isError = severity == null || !severity.equalsIgnoreCase("warning");

        diagnostics.add(new CompactCompilerDiagnostic(filePath, lineNum, colNum, message, isError));
      }
    }
    return diagnostics;
  }
}
