package dev.verloren.midnight;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * Message bundle for Midnight Compact plugin localization and UI strings.
 */
public final class CompactBundle extends DynamicBundle {
  @NonNls
  public static final String BUNDLE = "messages.MyMessageBundle";

  private static final CompactBundle INSTANCE = new CompactBundle();

  private CompactBundle() {
    super(BUNDLE);
  }

  public static @Nls @NotNull String message(
      @NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
      Object @NotNull ... params
  ) {
    return INSTANCE.getMessage(key, params);
  }
}
