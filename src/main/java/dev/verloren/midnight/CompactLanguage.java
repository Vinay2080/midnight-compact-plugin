package dev.verloren.midnight;

import com.intellij.lang.Language;

/**
 * Top-level language definition for the Compact smart-contract language.
 *
 * <p>Compact is a domain-specific smart contract language developed for the
 * Midnight privacy-oriented blockchain network. This singleton instance registers
 * the language identity within IntelliJ's platform registry and serves as the
 * key for all language-specific extension points (parser definitions, syntax
 * highlighters, completion contributors, formatters, etc.).</p>
 */
public class CompactLanguage extends Language {

  public static final CompactLanguage INSTANCE = new CompactLanguage();

  private CompactLanguage() {
    super("Compact");
  }
}