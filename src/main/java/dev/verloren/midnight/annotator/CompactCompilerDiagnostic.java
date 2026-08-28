package dev.verloren.midnight.annotator;

import org.jetbrains.annotations.NotNull;

public record CompactCompilerDiagnostic(
    @NotNull String filePath,
    int line,
    int column,
    @NotNull String message,
    boolean isError
) {}
