package dev.verloren.midnight.highlighter;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jspecify.annotations.NonNull;

public class CompactSyntaxHighlighterFactory extends SyntaxHighlighterFactory {

    @Override
    public @NonNull SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile) {
        return new CompactSyntaxHighlighter();
    }
}
