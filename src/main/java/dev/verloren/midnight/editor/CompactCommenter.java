package dev.verloren.midnight.editor;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.psi.PsiComment;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.Nullable;

/**
 * Defines comment token boundaries, delimiters, and documentation comment recognition for Compact.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Registered as a {@code lang.commenter} extension for language {@code Compact} in {@code plugin.xml}.
 * Implements {@link CodeDocumentationAwareCommenter} (extending {@link com.intellij.lang.Commenter}),
 * which powers IntelliJ's:</p>
 * <ul>
 *   <li><b>Comment / Uncomment Actions:</b> {@code Comment with Line Comment} (Ctrl+/ or Cmd+/) and
 *       {@code Comment with Block Comment} (Ctrl+Shift+/ or Cmd+Shift+/).</li>
 *   <li><b>Automatic Doc Comment Stubs:</b> Generation of doc comment templates when typing
 *       {@code /**} + Enter above declarations.</li>
 *   <li><b>Asterisk Continuation:</b> Automatic prefixing of {@code "*"} on subsequent lines within doc comments.</li>
 *   <li><b>PsiComment Classification:</b> Identification of documentation comments vs regular inline/block comments
 *       for folding, formatting, and documentation presentation.</li>
 * </ul>
 *
 * <h3>Comment Syntax in Compact</h3>
 * <pre>
 *   Line Comment:          // This is a single-line comment
 *   Block Comment:         /* This is a multi-line block comment *&#47;
 *   Doc Comment (Block):   /**
 *                           * Documents contracts, circuits, witnesses, and types
 *                           *&#47;
 *   Doc Comment (Triple):  /// Single-line documentation comment
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Thread Safety:</b> Completely stateless and immutable. Methods may be called from any thread
 *       (EDT or background read threads) without synchronization.</li>
 * </ul>
 *
 * @see CompactDocCommentEnterHandler
 * @see CompactTokenTypes
 */
public class CompactCommenter implements CodeDocumentationAwareCommenter {

  /**
   * Returns the prefix used for single-line comments in Compact ({@code "//"}).
   *
   * @return {@code "//"}
   */
  @Override
  public @Nullable String getLineCommentPrefix() {
    return "//";
  }

  /**
   * Returns the opening delimiter for multi-line block comments in Compact ({@code "/*"}).
   *
   * @return {@code "/*"}
   */
  @Override
  public @Nullable String getBlockCommentPrefix() {
    return "/*";
  }

  /**
   * Returns the closing delimiter for multi-line block comments in Compact ({@code "*&#47;"}).
   *
   * @return {@code "*&#47;"}
   */
  @Override
  public @Nullable String getBlockCommentSuffix() {
    return "*/";
  }

  /**
   * Returns the escaped prefix for commented block comments, or {@code null} if nesting is unsupported.
   *
   * @return {@code null}
   */
  @Override
  public @Nullable String getCommentedBlockCommentPrefix() {
    return null;
  }

  /**
   * Returns the escaped suffix for commented block comments, or {@code null} if nesting is unsupported.
   *
   * @return {@code null}
   */
  @Override
  public @Nullable String getCommentedBlockCommentSuffix() {
    return null;
  }

  /**
   * Returns the lexer token element type representing single-line comments in Compact.
   *
   * @return {@link CompactTokenTypes#LINE_COMMENT}
   */
  @Override
  public @Nullable IElementType getLineCommentTokenType() {
    return CompactTokenTypes.LINE_COMMENT;
  }

  /**
   * Returns the lexer token element type representing standard block comments in Compact.
   *
   * @return {@link CompactTokenTypes#BLOCK_COMMENT}
   */
  @Override
  public @Nullable IElementType getBlockCommentTokenType() {
    return CompactTokenTypes.BLOCK_COMMENT;
  }

  /**
   * Returns the lexer token element type representing documentation comments in Compact.
   *
   * @return {@link CompactTokenTypes#BLOCK_COMMENT}
   */
  @Override
  public @Nullable IElementType getDocumentationCommentTokenType() {
    return CompactTokenTypes.BLOCK_COMMENT;
  }

  /**
   * Returns the opening delimiter for multi-line documentation comments ({@code "/**"}).
   *
   * @return {@code "/**"}
   */
  @Override
  public @Nullable String getDocumentationCommentPrefix() {
    return "/**";
  }

  /**
   * Returns the line continuation prefix inserted by the editor on Enter within a doc comment ({@code "*"}).
   *
   * @return {@code "*"}
   */
  @Override
  public @Nullable String getDocumentationCommentLinePrefix() {
    return "*";
  }

  /**
   * Returns the closing delimiter for multi-line documentation comments ({@code "*&#47;"}).
   *
   * @return {@code "*&#47;"}
   */
  @Override
  public @Nullable String getDocumentationCommentSuffix() {
    return "*/";
  }

  /**
   * Checks whether the given PSI comment element represents a documentation comment.
   *
   * <p>In Compact, both Javadoc-style block comments (starting with {@code "/**"}) and
   * Rust-style line comments (starting with {@code "///"}) serve as documentation comments.</p>
   *
   * @param element the PSI comment element to inspect
   * @return {@code true} if {@code element} starts with {@code "/**"} or {@code "///"}; {@code false} otherwise
   */
  @Override
  public boolean isDocumentationComment(PsiComment element) {
    if (element == null) {
      return false;
    }
    String text = element.getText();
    return text.startsWith("/**") || text.startsWith("///");
  }
}

