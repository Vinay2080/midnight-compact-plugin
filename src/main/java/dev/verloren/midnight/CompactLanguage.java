package dev.verloren.midnight;

import com.intellij.lang.Language;

public class CompactLanguage extends Language {

    public static final CompactLanguage INSTANCE = new CompactLanguage ();

    private CompactLanguage () {
        super("Compact");
    }
}