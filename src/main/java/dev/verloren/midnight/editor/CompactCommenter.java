package dev.verloren.midnight.editor;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/**
 * Defines line ({@code //}) and block ({@code /* ... *&#47;}) comment tokens for Compact.
 *
 * <p>Used by IntelliJ's Comment with Line Comment (Ctrl+/) and Comment with Block Comment (Ctrl+Shift+/) actions.</p>
 */
public class CompactCommenter implements Commenter {
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
}
