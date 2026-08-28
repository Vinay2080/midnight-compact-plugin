package dev.verloren.midnight.editor;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.psi.PsiComment;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.Nullable;

/**
 * Defines line ({@code //}), block ({@code /* ... *&#47;}), and documentation
 * ({@code /** ... *&#47;}, {@code ///}) comment tokens and behavior for Compact.
 *
 * <p>Implements {@link CodeDocumentationAwareCommenter} so IntelliJ provides automatic doc comment
 * generation, asterisk line prefixes on Enter, and doc comment recognition.</p>
 */
public class CompactCommenter implements CodeDocumentationAwareCommenter {
  @Override
  public @Nullable String getLineCommentPrefix() {
    return "//";
  }

  @Override
  public @Nullable String getBlockCommentPrefix() {
    return "/*";
  }

  @Override
  public @Nullable String getBlockCommentSuffix() {
    return "*/";
  }

  @Override
  public @Nullable String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Override
  public @Nullable String getCommentedBlockCommentSuffix() {
    return null;
  }

  @Override
  public @Nullable IElementType getLineCommentTokenType() {
    return CompactTokenTypes.LINE_COMMENT;
  }

  @Override
  public @Nullable IElementType getBlockCommentTokenType() {
    return CompactTokenTypes.BLOCK_COMMENT;
  }

  @Override
  public @Nullable IElementType getDocumentationCommentTokenType() {
    return CompactTokenTypes.BLOCK_COMMENT;
  }

  @Override
  public @Nullable String getDocumentationCommentPrefix() {
    return "/**";
  }

  @Override
  public @Nullable String getDocumentationCommentLinePrefix() {
    return "*";
  }

  @Override
  public @Nullable String getDocumentationCommentSuffix() {
    return "*/";
  }

  @Override
  public boolean isDocumentationComment(PsiComment element) {
    if (element == null) {
      return false;
    }
    String text = element.getText();
    return text.startsWith("/**") || text.startsWith("///");
  }
}

