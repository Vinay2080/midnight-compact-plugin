package dev.verloren.midnight.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import org.jetbrains.annotations.NotNull;

public class CompactFile extends PsiFileBase {

  public CompactFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, CompactLanguage.INSTANCE);
  }

  @Override
  public @NotNull FileType getFileType() {
    return CompactFileType.INSTANCE;
  }

  @Override
  public String toString() {
    return "Compact File";
  }
}