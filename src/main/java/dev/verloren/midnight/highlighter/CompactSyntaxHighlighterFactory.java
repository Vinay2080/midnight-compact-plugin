package dev.verloren.midnight.highlighter;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jspecify.annotations.NonNull;

/**
 * Extension point factory creating {@link CompactSyntaxHighlighter} instances for Compact files.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.syntaxHighlighterFactory} extension
 * point for {@link dev.verloren.midnight.CompactLanguage}. Provides the IntelliJ Platform with a
 * factory for producing fresh lexical highlighters whenever a Compact virtual file is opened in an editor,
 * rendered in diff views, or previewed in UI settings panels.</p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by {@code com.intellij.openapi.fileTypes.SyntaxHighlighterFactory#getSyntaxHighlighter}
 * whenever an editor buffer or syntax-highlighted document is initialized for a Compact file.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Called on the Event Dispatch Thread (EDT) or pooled background threads.</li>
 *   <li><b>Statelessness:</b> The factory maintains no mutable state and instantiates a new {@link CompactSyntaxHighlighter}
 *       per invocation, ensuring safe thread isolation.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.highlighter.CompactSyntaxHighlighter
 * @see com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
 */
public class CompactSyntaxHighlighterFactory extends SyntaxHighlighterFactory {

  /**
   * Constructs a new {@link CompactSyntaxHighlighter} for the specified project and virtual file.
   * <p>
   * Invoked by IntelliJ's editor infrastructure when creating an {@code EditorHighlighter}.
   * Both parameters may be {@code null} in preview contexts, settings panels, or headless test runners.
   * </p>
   *
   * @param project     the current project context, or null when opened outside a project or in settings
   * @param virtualFile the virtual file being highlighted, or null for in-memory documents or preview editors
   * @return a fresh {@link CompactSyntaxHighlighter} instance wrapping an independent {@link dev.verloren.midnight.lexer.CompactLexer}
   */
  @Override
  public @NonNull SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile) {
    // Step 1: Instantiate a fresh CompactSyntaxHighlighter wrapping a new CompactLexer
    return new CompactSyntaxHighlighter();
  }
}
