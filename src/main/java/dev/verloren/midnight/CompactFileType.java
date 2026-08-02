package dev.verloren.midnight;

import com.intellij.openapi.fileTypes.LanguageFileType;
import dev.verloren.midnight.icons.MidnightIcons;
import org.jspecify.annotations.NonNull;

import javax.swing.*;

public class CompactFileType extends LanguageFileType {

    public static final CompactFileType INSTANCE = new CompactFileType ();

    private CompactFileType () {
        super(CompactLanguage.INSTANCE);
    }

    @Override
    public @NonNull String getName() {
        return "Compact";
    }

    @Override
    public @NonNull String getDescription() {
        return "Compact language file";
    }

    @Override
    public @NonNull String getDefaultExtension() {
        return "compact";
    }

    @Override
    public Icon getIcon() {
        return MidnightIcons.FILE;
    }
}