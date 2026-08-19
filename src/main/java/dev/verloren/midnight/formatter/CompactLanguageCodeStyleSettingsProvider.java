package dev.verloren.midnight.formatter;

import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import dev.verloren.midnight.CompactLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configures the canonical code style settings and indent options (2 spaces) for Compact.
 */
public class CompactLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

  @Override
  public @NotNull Language getLanguage() {
    return CompactLanguage.INSTANCE;
  }

  @Override
  public void customizeDefaults(
      @NotNull CommonCodeStyleSettings commonSettings,
      @NotNull CommonCodeStyleSettings.IndentOptions indentOptions
  ) {
    indentOptions.INDENT_SIZE = 2;
    indentOptions.TAB_SIZE = 2;
    indentOptions.CONTINUATION_INDENT_SIZE = 2;
    indentOptions.USE_TAB_CHARACTER = false;
  }

  @Override
  public @Nullable String getCodeSample(@NotNull SettingsType settingsType) {
    return "circuit increment(): [] {\n  round.increment(1);\n}\n";
  }
}
