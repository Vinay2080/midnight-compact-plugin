package dev.verloren.midnight.annotator;

import com.intellij.lang.annotation.HighlightSeverity;
import org.jetbrains.annotations.NotNull;

public record CompactCompilerDiagnostic(
    @NotNull String filePath,
    int line,
    int column,
    int endLine,
    int endColumn,
    @NotNull String message,
    boolean isError,
    @NotNull HighlightSeverity severity
) {
  public CompactCompilerDiagnostic(
      @NotNull String filePath,
      int line,
      int column,
      @NotNull String message,
      boolean isError
  ) {
    this(
        filePath,
        line,
        column,
        line,
        Math.max(column + 1, column),
        message,
        isError,
        isError ? HighlightSeverity.ERROR : HighlightSeverity.WARNING
    );
  }
}
