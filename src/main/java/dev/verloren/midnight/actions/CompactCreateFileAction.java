package dev.verloren.midnight.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.icons.MidnightIcons;
import dev.verloren.midnight.ide.fileTemplates.CompactFileTemplateGroupFactory;
import dev.verloren.midnight.psi.CompactNamedElement;
import dev.verloren.midnight.refactoring.CompactNamesValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Action allowing users to create new Compact source files from templates via the New context menu.
 */
public class CompactCreateFileAction extends CreateFileFromTemplateAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(CompactCreateFileAction.class);
  public static final String LAST_TEMPLATE_PROPERTY = "dev.verloren.midnight.template.last";

  public CompactCreateFileAction() {
    super("Compact File", "Create a new Compact smart contract or module file", MidnightIcons.FILE);
  }

  @Override
  protected void buildDialog(@NotNull Project project, @NotNull PsiDirectory directory, @NotNull CreateFileFromTemplateDialog.Builder builder) {
    InputValidator fileValidator = new InputValidatorEx() {
      @Override
      public @Nullable String getErrorText(String inputString) {
        return validateFileName(inputString);
      }

      @Override
      public boolean checkInput(String inputString) {
        return validateFileName(inputString) == null;
      }

      @Override
      public boolean canClose(String inputString) {
        return checkInput(inputString);
      }
    };

    InputValidator identifierValidator = new InputValidatorEx() {
      @Override
      public @Nullable String getErrorText(String inputString) {
        return validateIdentifier(inputString, project);
      }

      @Override
      public boolean checkInput(String inputString) {
        return validateIdentifier(inputString, project) == null;
      }

      @Override
      public boolean canClose(String inputString) {
        return checkInput(inputString);
      }
    };

    builder
        .setTitle("New Compact File")
        .addKind("Empty compact file", MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_FILE, fileValidator)
        .addKind("Compact contract", MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_CONTRACT, identifierValidator)
        .addKind("Compact module", MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_MODULE, identifierValidator)
        .addKind("Compact interface", MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_INTERFACE, identifierValidator)
        .setValidator(fileValidator);
  }

  @Override
  public String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
    return "Create Compact File " + newName;
  }

  @Override
  protected String getDefaultTemplateProperty() {
    return LAST_TEMPLATE_PROPERTY;
  }

  @Override
  public @Nullable PsiFile createFile(String name, String templateName, PsiDirectory dir) {
    if (name == null || name.trim().isEmpty()) {
      return null;
    }
    Project project = dir.getProject();
    FileTemplateManager templateManager = FileTemplateManager.getInstance(project);
    FileTemplate template = templateManager.findInternalTemplate(templateName);
    if (template == null) {
      template = templateManager.getInternalTemplate(templateName);
    }

    String trimmed = name.trim();
    String cleanName = trimmed.endsWith(".compact") ? trimmed.substring(0, trimmed.length() - ".compact".length()) : trimmed;
    String simpleName = extractSimpleName(cleanName);

    Map<String, String> extraProperties = new HashMap<>();
    extraProperties.put(FileTemplate.ATTRIBUTE_NAME, simpleName);

    try {
      return createFileFromTemplate(
          cleanName,
          template,
          dir,
          getDefaultTemplateProperty(),
          true,
          Collections.emptyMap(),
          extraProperties
      );
    } catch (Exception e) {
      LOG.warn("Failed to create Compact file from template " + templateName, e);
      return null;
    }
  }

  @Override
  protected void postProcess(
      @NotNull PsiFile createdElement,
      String templateName,
      Map<String, String> customProperties
  ) {
    super.postProcess(createdElement, templateName, customProperties);
    Project project = createdElement.getProject();
    CodeStyleManager.getInstance(project).reformat(createdElement);

    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null && editor.getDocument() == createdElement.getViewProvider().getDocument()) {
      CompactNamedElement named = PsiTreeUtil.findChildOfType(createdElement, CompactNamedElement.class);
      if (named != null && named.getNameIdentifier() != null) {
        editor.getCaretModel().moveToOffset(named.getNameIdentifier().getTextRange().getEndOffset());
      }
    }
  }

  public static @NotNull String extractSimpleName(@NotNull String name) {
    String clean = name.endsWith(".compact") ? name.substring(0, name.length() - ".compact".length()) : name;
    int lastSlash = Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\'));
    return lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;
  }

  public static @Nullable String validateFileName(@Nullable String inputString) {
    if (inputString == null || inputString.trim().isEmpty()) {
      return "File name cannot be empty";
    }
    String trimmed = inputString.trim();
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c == '*' || c == '?' || c == ':' || c == '<' || c == '>' || c == '|' || c == '"' || c == 0) {
        return "File name contains illegal character: '" + c + "'";
      }
    }
    if (trimmed.startsWith("/") || trimmed.startsWith("\\")) {
      return "File name cannot start with a path separator";
    }
    if (trimmed.endsWith("/") || trimmed.endsWith("\\")) {
      return "File name cannot end with a path separator";
    }
    if (trimmed.contains("//") || trimmed.contains("\\\\") || trimmed.contains("/\\") || trimmed.contains("\\/")) {
      return "File path cannot contain empty directory segments";
    }
    String simpleName = extractSimpleName(trimmed);
    if (simpleName.isEmpty()) {
      return "File name cannot be empty";
    }
    return null;
  }

  public static @Nullable String validateIdentifier(@Nullable String inputString, @Nullable Project project) {
    String fileError = validateFileName(inputString);
    if (fileError != null) {
      return fileError;
    }
    String simpleName = extractSimpleName(inputString.trim());
    CompactNamesValidator validator = new CompactNamesValidator();
    if (validator.isKeyword(simpleName, project)) {
      return "'" + simpleName + "' is a reserved Compact language keyword";
    }
    if (!validator.isIdentifier(simpleName, project)) {
      return "'" + simpleName + "' is not a valid Compact identifier";
    }
    return null;
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
