package dev.verloren.midnight.annotator;

import com.intellij.lang.annotation.HighlightSeverity;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable value record representing a single diagnostic issue emitted by the Compact compiler ({@code compactc}).
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Transports structured compiler error and warning information between the execution and presentation layers:
 * <ul>
 *   <li><b>Parser Output:</b> Created by {@link CompactCompilerOutputParser#parse(String)} after executing
 *       {@code compactc} against a source file or temporary shadow document buffer.</li>
 *   <li><b>External Annotator Presentation:</b> Consumed by {@link CompactExternalAnnotator#apply}
 *       to translate line/column coordinates into IntelliJ editor {@link com.intellij.openapi.util.TextRange}
 *       spans and register editor annotations with matching {@link HighlightSeverity}.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading & Immutability Contract</h3>
 * <ul>
 *   <li><b>Thread Safety:</b> As an immutable Java record, instances can be safely constructed in
 *       background worker threads ({@link CompactExternalAnnotator#doAnnotate}) and passed to the EDT
 *       during annotation application ({@link CompactExternalAnnotator#apply}).</li>
 *   <li><b>No PSI Retention:</b> Contains only primitive coordinate integers and strings. Holds no
 *       references to {@link com.intellij.psi.PsiElement} or {@link com.intellij.openapi.editor.Document},
 *       preventing memory leaks.</li>
 * </ul>
 *
 * @param filePath  path of the source file where the diagnostic occurred (may be relative, Windows, or WSL format)
 * @param line      1-based start line index
 * @param column    1-based start column index
 * @param endLine   1-based end line index
 * @param endColumn 1-based end column index
 * @param message   human-readable compiler error or warning explanation
 * @param isError   {@code true} if this diagnostic is a compilation error; {@code false} if a warning or info
 * @param severity  IntelliJ platform highlight severity (e.g. {@link HighlightSeverity#ERROR} or {@link HighlightSeverity#WARNING})
 *
 * @see dev.verloren.midnight.annotator.CompactCompilerOutputParser
 * @see dev.verloren.midnight.annotator.CompactExternalAnnotator
 */
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

  /**
   * Convenience constructor creating a single-point diagnostic that defaults to spanning
   * at least one character at the given line and column.
   *
   * @param filePath source file path
   * @param line     1-based line number
   * @param column   1-based column number
   * @param message  compiler diagnostic message
   * @param isError  {@code true} for compilation error; {@code false} for warning
   */
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
