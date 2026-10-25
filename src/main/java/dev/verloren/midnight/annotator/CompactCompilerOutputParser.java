package dev.verloren.midnight.annotator;

import com.intellij.lang.annotation.HighlightSeverity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust compiler output and traceback parser for the Compact language compiler ({@code compactc}).
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Invoked in the second phase ({@code doAnnotate}) of {@link CompactExternalAnnotator}:
 * <ul>
 *   <li><b>Process Output Parsing:</b> Ingests raw combined stdout and stderr streams from
 *       {@code compactc} process executions.</li>
 *   <li><b>Diagnostic Normalization:</b> Converts diverse error formats into uniform
 *       {@link CompactCompilerDiagnostic} records with accurate 1-based line/column coordinates.</li>
 * </ul>
 * </p>
 *
 * <h3>Supported Compiler Output Patterns</h3>
 * <p>
 * The parser uses dual regular expressions to match both standard GNU-style compiler diagnostics
 * and Chez Scheme internal compiler traceback exceptions:
 * <ol>
 *   <li><b>Standard Diagnostics:</b> {@code path.compact:line:col: warning/error: message}
 *       <br>Matches standard front-end syntax and type-checking diagnostics.</li>
 *   <li><b>Chez Scheme Compiler Exceptions:</b> {@code Exception in ...: path.compact line X, char Y: message}
 *       <br>Captures unhandled Scheme macro or pass errors, accumulating multi-line stack or explanation
 *       text until the next diagnostic begins.</li>
 * </ol>
 * </p>
 *
 * <h3>Cross-Platform Path Handling</h3>
 * <ul>
 *   <li>Supports Windows drive paths (e.g. {@code C:\path\file.compact}).</li>
 *   <li>Supports WSL and Unix relative/absolute paths (e.g. {@code /tmp/.../file.compact}).</li>
 * </ul>
 *
 * @see dev.verloren.midnight.annotator.CompactCompilerDiagnostic
 * @see dev.verloren.midnight.annotator.CompactExternalAnnotator
 */
public final class CompactCompilerOutputParser {

  /**
   * Pattern matching standard compiler diagnostics:
   * {@code [prefix] path.compact:line:col: [severity:] message}.
   */
  private static final Pattern DIAGNOSTIC_PATTERN =
      Pattern.compile("(?:^|(?<=[\\s\\[]))([a-zA-Z]:[\\\\/][^:\\r\\n]+?\\.compact|[^:\\r\\n\\s][^:\\r\\n]*?\\.compact):(\\d+):(\\d+):?\\s*(?:(warning|error|info):\\s*)?(.*)", Pattern.CASE_INSENSITIVE);

  /**
   * Pattern matching Chez Scheme exceptions emitted by the compiler backend:
   * {@code Exception in ...: path.compact line X, char Y: message}.
   */
  private static final Pattern EXCEPTION_PATTERN =
      Pattern.compile("(?:Exception(?:\\s+in\\s+[^:]+)?:\\s*)?([a-zA-Z]:[\\\\/][^:\\r\\n]+?\\.compact|[^:\\r\\n\\s][^:\\r\\n]*?\\.compact)\\s+line\\s+(\\d+),?\\s+char\\s+(\\d+):?\\s*(.*)", Pattern.CASE_INSENSITIVE);

  /**
   * Private constructor preventing external instantiation of this stateless utility class.
   */
  private CompactCompilerOutputParser() {
  }

  /**
   * Parses the combined stdout/stderr output from a {@code compactc} execution into structured diagnostics.
   *
   * @param output raw terminal output from the compiler process
   * @return ordered list of parsed {@link CompactCompilerDiagnostic} instances
   */
  public static @NotNull List<CompactCompilerDiagnostic> parse(@NotNull String output) {
    List<CompactCompilerDiagnostic> diagnostics = new ArrayList<>();
    String[] lines = output.split("\\r?\\n");
    int i = 0;

    // Step-by-step sequential line iteration
    while (i < lines.length) {
      String line = lines[i].trim();
      if (line.isEmpty()) {
        i++;
        continue;
      }

      // Branch 1: Check standard GNU-style compiler diagnostics
      Matcher diagMatcher = DIAGNOSTIC_PATTERN.matcher(line);
      if (diagMatcher.find()) {
        String filePath = diagMatcher.group(1);
        int lineNum = Math.max(1, Integer.parseInt(diagMatcher.group(2)));
        int colNum = Math.max(1, Integer.parseInt(diagMatcher.group(3)));
        String severity = diagMatcher.group(4);
        String message = diagMatcher.group(5).trim();

        // Determine severity level (default to ERROR if omitted)
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

      // Branch 2: Check Scheme exception format with multi-line message accumulation
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

        // Accumulate continuation lines belonging to the same exception
        i++;
        while (i < lines.length) {
          String nextLine = lines[i].trim();
          // Stop accumulating if the next line starts a new diagnostic or exception
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
