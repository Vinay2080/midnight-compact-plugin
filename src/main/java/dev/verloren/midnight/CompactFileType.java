package dev.verloren.midnight;

import com.intellij.openapi.fileTypes.LanguageFileType;
import dev.verloren.midnight.icons.MidnightIcons;
import org.jspecify.annotations.NonNull;

import javax.swing.*;

/**
 * Represents the Compact source file type ({@code .compact}) in IntelliJ IDEA.
 *
 * <p>Binds the file extension to {@link CompactLanguage#INSTANCE} and provides
 * file metadata, descriptions, and the Midnight file icon displayed in the project tree
 * and editor tabs.</p>
 */
public class CompactFileType extends LanguageFileType {

  public static final CompactFileType INSTANCE = new CompactFileType();

  private CompactFileType() {
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