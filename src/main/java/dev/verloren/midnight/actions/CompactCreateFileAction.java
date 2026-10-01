package dev.verloren.midnight.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import dev.verloren.midnight.CompactBundle;
import dev.verloren.midnight.icons.MidnightIcons;
import dev.verloren.midnight.ide.fileTemplates.CompactDefaultTemplatePropertiesProvider;
import dev.verloren.midnight.ide.fileTemplates.CompactFileTemplateGroupFactory;
import dev.verloren.midnight.refactoring.CompactNamesValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Action allowing users to create new Compact source files from templates via the New context menu.
 */
public class CompactCreateFileAction extends CreateFileFromTemplateAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(CompactCreateFileAction.class);
  public static final String LAST_TEMPLATE_PROPERTY = "dev.verloren.midnight.template.last";
  private static final String COMPACT_EXTENSION = ".compact";
  private static final CompactNamesValidator NAMES_VALIDATOR = new CompactNamesValidator();

  public CompactCreateFileAction() {
    super(
        CompactBundle.messagePointer("action.dev.verloren.midnight.actions.CompactCreateFileAction.text"),
        CompactBundle.messagePointer("action.dev.verloren.midnight.actions.CompactCreateFileAction.description"),
        MidnightIcons.FILE
    );
  }

  @Override
  protected void buildDialog(
      @NotNull Project project,
      @NotNull PsiDirectory directory,
      @NotNull CreateFileFromTemplateDialog.Builder builder
  ) {
    InputValidator fileValidator = createFileValidator();
    InputValidator identifierValidator = createIdentifierValidator(project);

    builder
        .setTitle(CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.dialog.title"))
        .addKind(
            CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.kind.file"),
            MidnightIcons.FILE,
            CompactFileTemplateGroupFactory.COMPACT_FILE,
            fileValidator
        )
        .addKind(
            CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.kind.contract"),
            MidnightIcons.FILE,
            CompactFileTemplateGroupFactory.COMPACT_CONTRACT,
            identifierValidator
        )
        .addKind(
            CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.kind.module"),
            MidnightIcons.FILE,
            CompactFileTemplateGroupFactory.COMPACT_MODULE,
            identifierValidator
        )
        .addKind(
            CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.kind.interface"),
            MidnightIcons.FILE,
            CompactFileTemplateGroupFactory.COMPACT_INTERFACE,
            identifierValidator
        )
        .setValidator(fileValidator);
  }

  private static @NotNull InputValidator createFileValidator() {
    return new InputValidatorEx() {
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
  }

  private static @NotNull InputValidator createIdentifierValidator(@NotNull Project project) {
    return new InputValidatorEx() {
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
  }

  @Override
  public String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
    return CompactBundle.message(
        "action.dev.verloren.midnight.actions.CompactCreateFileAction.action.name",
        newName
    );
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
    FileTemplate template;
    try {
      template = templateManager.getInternalTemplate(templateName);
    } catch (ProcessCanceledException pce) {
      throw pce;
    } catch (Exception e) {
      LOG.warn("Compact file template not found: " + templateName, e);
      return null;
    }

    String trimmed = name.trim();
    String cleanName = stripCompactExtension(trimmed);
    String simpleName = extractSimpleName(cleanName);

    Map<String, String> extraProperties = buildTemplateProperties(project, simpleName);

    try {
      return createFileFromTemplate(
          cleanName,
          template,
          dir,
          getDefaultTemplateProperty(),
          true,
          extraProperties,
          extraProperties
      );
    } catch (ProcessCanceledException pce) {
      throw pce;
    } catch (Exception e) {
      LOG.warn("Failed to create Compact file from template " + templateName, e);
      return null;
    }
  }

  private static @NotNull Map<String, String> buildTemplateProperties(@NotNull Project project, @NotNull String simpleName) {
    String langVer = CompactDefaultTemplatePropertiesProvider.resolveLanguageVersion(project);
    String compilerVer = CompactDefaultTemplatePropertiesProvider.resolveCompilerVersion(project);
    return Map.of(
        FileTemplate.ATTRIBUTE_NAME, simpleName,
        CompactDefaultTemplatePropertiesProvider.COMPACT_LANGUAGE_VERSION, langVer,
        CompactDefaultTemplatePropertiesProvider.LANGUAGE_VERSION, langVer,
        CompactDefaultTemplatePropertiesProvider.COMPACT_COMPILER_VERSION, compilerVer,
        CompactDefaultTemplatePropertiesProvider.COMPILER_VERSION, compilerVer
    );
  }

  @Override
  protected void postProcess(
      @NotNull PsiFile createdElement,
      String templateName,
      Map<String, String> customProperties
  ) {
    super.postProcess(createdElement, templateName, customProperties);
    Project project = createdElement.getProject();
    WriteCommandAction.runWriteCommandAction(project, () -> {
      CodeStyleManager.getInstance(project).reformat(createdElement);
    });

    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null && editor.getDocument() == createdElement.getViewProvider().getDocument()) {
      int targetOffset = determineInitialCaretOffset(createdElement);
      if (targetOffset >= 0 && targetOffset <= editor.getDocument().getTextLength()) {
        editor.getCaretModel().moveToOffset(targetOffset);
      }
    }
  }

  public static int determineInitialCaretOffset(@NotNull PsiFile file) {
    String text = file.getText();
    int lbrace = text.indexOf('{');
    int rbrace = text.lastIndexOf('}');
    if (lbrace != -1 && rbrace > lbrace) {
      int nextLine = text.indexOf('\n', lbrace);
      if (nextLine != -1 && nextLine < rbrace) {
        int afterIndent = nextLine + 1;
        while (afterIndent < rbrace && (text.charAt(afterIndent) == ' ' || text.charAt(afterIndent) == '\t')) {
          afterIndent++;
        }
        return afterIndent;
      }
      return lbrace + 1;
    }
    return text.length();
  }

  public static @NotNull String stripCompactExtension(@NotNull String name) {
    if (name.toLowerCase(Locale.ROOT).endsWith(COMPACT_EXTENSION)) {
      return name.substring(0, name.length() - COMPACT_EXTENSION.length());
    }
    return name;
  }

  public static @NotNull String extractSimpleName(@NotNull String name) {
    String clean = stripCompactExtension(name);
    int lastSlash = Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\'));
    return lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;
  }

  public static @Nullable String validateFileName(@Nullable String inputString) {
    if (inputString == null || inputString.trim().isEmpty()) {
      return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.empty");
    }
    String trimmed = inputString.trim();
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c < 32 || c == '*' || c == '?' || c == ':' || c == '<' || c == '>' || c == '|' || c == '"') {
        return CompactBundle.message(
            "action.dev.verloren.midnight.actions.CompactCreateFileAction.error.illegal.char",
            c < 32 ? String.format("\\u%04x", (int) c) : String.valueOf(c)
        );
      }
    }
    if (trimmed.startsWith("/") || trimmed.startsWith("\\")) {
      return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.starts.with.separator");
    }
    if (trimmed.endsWith("/") || trimmed.endsWith("\\")) {
      return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.ends.with.separator");
    }

    String[] segments = trimmed.replace('\\', '/').split("/");
    for (String segment : segments) {
      String segTrim = segment.trim();
      if (segTrim.isEmpty()) {
        return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.empty.segment");
      }
      if (".".equals(segTrim) || "..".equals(segTrim)) {
        return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.traversal");
      }
    }

    String simpleName = extractSimpleName(trimmed);
    if (simpleName.isEmpty()) {
      return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.empty");
    }
    return null;
  }

  public static @Nullable String validateIdentifier(@Nullable String inputString, @Nullable Project project) {
    String fileError = validateFileName(inputString);
    if (fileError != null) {
      return fileError;
    }
    String simpleName = extractSimpleName(inputString.trim());
    if (NAMES_VALIDATOR.isKeyword(simpleName, project)) {
      return CompactBundle.message(
          "action.dev.verloren.midnight.actions.CompactCreateFileAction.error.keyword",
          simpleName
      );
    }
    if (!NAMES_VALIDATOR.isIdentifier(simpleName, project)) {
      return CompactBundle.message(
          "action.dev.verloren.midnight.actions.CompactCreateFileAction.error.invalid.identifier",
          simpleName
      );
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
