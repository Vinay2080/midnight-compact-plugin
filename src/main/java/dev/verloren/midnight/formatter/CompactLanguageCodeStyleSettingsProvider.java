package dev.verloren.midnight.formatter;

import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import dev.verloren.midnight.CompactLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configures the canonical code style settings and indent options for Compact smart contract files.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.langCodeStyleSettingsProvider} extension
 * point for {@link dev.verloren.midnight.CompactLanguage}. Establishes the default indentation and formatting
 * standards across all projects opening Compact source files:
 * <ul>
 *   <li><b>2-Space Soft Indentation:</b> Sets {@code INDENT_SIZE = 2}, {@code TAB_SIZE = 2},
 *       {@code CONTINUATION_INDENT_SIZE = 2}, and {@code USE_TAB_CHARACTER = false} to adhere to the
 *       official Midnight Compact coding standards.</li>
 *   <li><b>Settings Preview:</b> Provides a representative Compact contract snippet ({@link #getCodeSample})
 *       used in the IDE's Code Style preview pane (Settings &gt; Editor &gt; Code Style &gt; Compact).</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by IntelliJ's code style settings infrastructure during project initialization to configure
 * default code style settings, and when rendering the language-specific Code Style configuration panels.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Called on the Event Dispatch Thread (EDT) or pooled initialization threads.</li>
 *   <li><b>Statelessness:</b> Maintains zero state; purely configures settings instances.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.formatter.CompactFormattingModelBuilder
 * @see dev.verloren.midnight.formatter.CompactBlock
 * @see com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
 */
public class CompactLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

  /**
   * Returns the language instance associated with these code style settings.
   *
   * @return {@link CompactLanguage#INSTANCE}
   */
  @Override
  public @NotNull Language getLanguage() {
    return CompactLanguage.INSTANCE;
  }

  /**
   * Configures canonical 2-space soft tab indentation defaults for Compact smart contract files.
   *
   * @param commonSettings general code style settings
   * @param indentOptions  indent options specifying indent size, continuation indent, and tab characters
   */
  @Override
  public void customizeDefaults(
      @NotNull CommonCodeStyleSettings commonSettings,
      @NotNull CommonCodeStyleSettings.IndentOptions indentOptions
  ) {
    // Step 1: Enforce 2-space canonical indentation and disable tab characters
    indentOptions.INDENT_SIZE = 2;
    indentOptions.TAB_SIZE = 2;
    indentOptions.CONTINUATION_INDENT_SIZE = 2;
    indentOptions.USE_TAB_CHARACTER = false;
  }

  /**
   * Supplies the preview code sample displayed in the IDE's Code Style preferences panel.
   *
   * @param settingsType settings category being previewed
   * @return sample Compact source code snippet
   */
  @Override
  public @Nullable String getCodeSample(@NotNull SettingsType settingsType) {
    return "circuit increment(): [] {\n  round.increment(1);\n}\n";
  }
}
