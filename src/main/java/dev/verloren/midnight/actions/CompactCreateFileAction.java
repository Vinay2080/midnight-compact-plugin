package dev.verloren.midnight.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.icons.MidnightIcons;
import dev.verloren.midnight.ide.fileTemplates.CompactFileTemplateGroupFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;

/**
 * Action allowing users to create new Compact source files from templates via the New context menu.
 */
public class CompactCreateFileAction extends CreateFileFromTemplateAction implements DumbAware {

  public CompactCreateFileAction() {
    super("Compact File", "Create a new Compact smart contract or module file", MidnightIcons.FILE);
  }

  @Override
  protected void buildDialog(@NotNull Project project, @NotNull PsiDirectory directory, @NotNull CreateFileFromTemplateDialog.Builder builder) {
    builder
            .setTitle("New Compact File")
            .addKind("Empty compact file", MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_FILE)
            .addKind("Compact contract", MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_CONTRACT)
            .addKind("Compact module", MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_MODULE)
            .addKind("Compact interface", MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_INTERFACE);
  }

  @Override
  protected String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
    return "Create Compact File " + newName;
  }

  @Override
  protected @Nullable PsiFile createFile(String name, String templateName, PsiDirectory dir) {
    FileTemplate template = FileTemplateManager.getInstance(dir.getProject()).getInternalTemplate(templateName);
    Properties properties = new Properties(FileTemplateManager.getInstance(dir.getProject()).getDefaultProperties());
    String cleanName = name.endsWith(".compact") ? name.substring(0, name.length() - ".compact".length()) : name;
    properties.setProperty(FileTemplate.ATTRIBUTE_NAME, cleanName);
    String fileName = name.endsWith(".compact") ? name : name + ".compact";
    try {
      PsiElement element = FileTemplateUtil.createFromTemplate(template, fileName, properties, dir);
      return element instanceof PsiFile ? (PsiFile) element : null;
    } catch (Exception e) {
      return super.createFile(name, templateName, dir);
    }
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof CompactCreateFileAction;
  }
}
