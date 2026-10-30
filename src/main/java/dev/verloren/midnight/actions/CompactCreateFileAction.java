package dev.verloren.midnight.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.util.PropertiesComponent;
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
import com.intellij.util.IncorrectOperationException;
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
 * Action allowing users to create new Compact source files from templates via the "New" context menu.
 *
 * <h3>IntelliJ Platform Subsystem Role & Calling Pipeline</h3>
 * <p>
 * Registered in {@code plugin.xml} under {@code <actions>} (extending {@link CreateFileFromTemplateAction}):
 * <ul>
 *   <li><b>Subsystem Integration:</b> Hooks into IntelliJ's "New..." popup menu ({@code NewGroup}) in the Project View,
 *       Navigation Bar, and File menu.</li>
 *   <li><b>Lifecycle Flow:</b> Triggered on EDT via {@code actionPerformed}. Prompts user via
 *       {@link #buildDialog(Project, PsiDirectory, CreateFileFromTemplateDialog.Builder)}, executes
 *       {@link #createFile(String, String, PsiDirectory)}, and finalizes via {@link #postProcess}.</li>
 *   <li><b>Indexing State:</b> Marked with {@link DumbAware} so files can be created, even while indices are updating.</li>
 * </ul>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>EDT Execution:</b> Dialogue presentation, input validation, and file creation dispatch on the EDT.</li>
 *   <li><b>Write Action:</b> Code formatting in {@link #postProcess} is wrapped in {@link WriteCommandAction}.</li>
 * </ul>
 *
 * <h3>Creation Pipeline</h3>
 * <pre>
 *   [User: New File] ──► [buildDialog (Validate)] ──► [createFile (Template)] ──► [postProcess (Reformat+Caret)]
 * </pre>
 *
 * <h3>Edge Case Mitigations</h3>
 * <ul>
 *   <li><b>Subdirectory Paths:</b> Handles nested creation (e.g. {@code foo/bar/MyContract}) via path resolution.</li>
 *   <li><b>Extension Stripping:</b> Automatically removes {@code .compact} if the user enters it explicitly.</li>
 *   <li><b>Keyword & Identifier Guards:</b> Validates names using {@link CompactNamesValidator} for template kinds.</li>
 *   <li><b>Cancellation Integrity:</b> Re-throws {@link ProcessCanceledException} per SDK contract.</li>
 * </ul>
 *
 * @see com.intellij.ide.actions.CreateFileFromTemplateAction
 * @see com.intellij.ide.fileTemplates.FileTemplateManager
 * @see dev.verloren.midnight.ide.fileTemplates.CompactFileTemplateGroupFactory
 */
public class CompactCreateFileAction extends CreateFileFromTemplateAction implements DumbAware {
  /** Logger instance for diagnosing file creation and template lookup failures. */
  private static final Logger LOG = Logger.getInstance(CompactCreateFileAction.class);

  /** Property key persisted in {@link PropertiesComponent} to remember the most recently used (MRU) template. */
  public static final String LAST_TEMPLATE_PROPERTY = "dev.verloren.midnight.template.last";

  /** Standard file extension for Compact source files. */
  private static final String COMPACT_EXTENSION = ".compact";

  /** Stateless name validator checking identifier syntax and keyword conflicts in Compact language. */
  private static final CompactNamesValidator NAMES_VALIDATOR = new CompactNamesValidator();

  /**
   * Initializes the action presentation with localized bundle keys and file icon.
   */
  public CompactCreateFileAction() {
    super(
        CompactBundle.messagePointer("action.dev.verloren.midnight.actions.CompactCreateFileAction.text"),
        CompactBundle.messagePointer("action.dev.verloren.midnight.actions.CompactCreateFileAction.description"),
        MidnightIcons.FILE
    );
  }

  /**
   * Populates the template selection dialogue with Compact file kinds and input validators.
   *
   * @param project current IntelliJ project context
   * @param directory directory selected in Project View
   * @param builder dialogue builder provided by the platform action system
   */
  @Override
  protected void buildDialog(
      @NotNull Project project,
      @NotNull PsiDirectory directory,
      @NotNull CreateFileFromTemplateDialog.Builder builder
  ) {
    // Step 1: Construct validators for generic files and language construct identifiers
    InputValidator fileValidator = createFileValidator(directory);
    InputValidator identifierValidator = createIdentifierValidator(project, directory);

    // Step 2: Register template options with localized labels and template IDs into the builder
    builder.setTitle(CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.dialog.title"))
        .addKind(CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.kind.file"), MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_FILE, fileValidator)
        .addKind(CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.kind.contract"), MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_CONTRACT, identifierValidator)
        .addKind(CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.kind.module"), MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_MODULE, identifierValidator)
        .addKind(CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.kind.interface"), MidnightIcons.FILE, CompactFileTemplateGroupFactory.COMPACT_INTERFACE, identifierValidator)
        .setValidator(fileValidator);
  }

  /** Creates an {@link InputValidatorEx} checking file name syntax and existing file collisions. */
  private static @NotNull InputValidator createFileValidator(@NotNull PsiDirectory directory) {
    return new InputValidatorEx() {
      @Override public @Nullable String getErrorText(String input) {
        String err = validateFileName(input);
        return err != null ? err : validateFileCollision(input, directory);
      }
      @Override public boolean checkInput(String input) { return getErrorText(input) == null; }
      @Override public boolean canClose(String input) { return checkInput(input); }
    };
  }

  /** Creates an {@link InputValidatorEx} checking keyword collisions and identifier syntax. */
  private static @NotNull InputValidator createIdentifierValidator(@NotNull Project project, @NotNull PsiDirectory directory) {
    return new InputValidatorEx() {
      @Override public @Nullable String getErrorText(String input) {
        String err = validateIdentifier(input, project);
        return err != null ? err : validateFileCollision(input, directory);
      }
      @Override public boolean checkInput(String input) { return getErrorText(input) == null; }
      @Override public boolean canClose(String input) { return checkInput(input); }
    };
  }

  /** Generates a localized action description for the platform command/undo buffer. */
  @Override
  public String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
    return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.action.name", newName);
  }

  /** Returns the property key used by {@link PropertiesComponent} to persist MRU template selection. */
  @Override
  protected String getDefaultTemplateProperty() {
    return LAST_TEMPLATE_PROPERTY;
  }

  /** Retrieves the MRU template name from {@link PropertiesComponent}, sanitizing extension if needed. */
  @Override
  protected @Nullable String getDefaultTemplateName(@NotNull PsiDirectory dir) {
    String property = getDefaultTemplateProperty();
    if (property == null) return null;
    String value = PropertiesComponent.getInstance(dir.getProject()).getValue(property);
    return (value != null && value.endsWith(COMPACT_EXTENSION))
        ? value.substring(0, value.length() - COMPACT_EXTENSION.length())
        : value;
  }

  /**
   * Instantiates a new Compact PSI file from the requested internal template.
   *
   * @param name raw file name or relative path entered by a user
   * @param templateName internal template ID from {@link CompactFileTemplateGroupFactory}
   * @param dir target directory where a file should be created
   * @return newly created {@link PsiFile}, or {@code null} on failure
   * @throws ProcessCanceledException if the platform cancels operation
   */
  @Override
  public @Nullable PsiFile createFile(String name, String templateName, PsiDirectory dir) {
    // Step 1: Guard against null or empty input names
    if (name == null || name.trim().isEmpty()) {
      return null;
    }

    // Step 2: Retrieve internal template definition from FileTemplateManager
    Project project = dir.getProject();
    FileTemplateManager templateManager = FileTemplateManager.getInstance(project);
    FileTemplate template;
    try {
      template = templateManager.getInternalTemplate(templateName);
    } catch (ProcessCanceledException pce) {
      throw pce; // Must re-throw per platform cancellation contract
    } catch (Exception e) {
      LOG.warn("Compact file template not found: " + templateName, e);
      return null;
    }

    // Step 3: Strip extension and isolate simple name for template properties
    String trimmed = name.trim();
    String cleanName = stripCompactExtension(trimmed);
    String simpleName = extractSimpleName(cleanName);

    // Step 4: Populate custom template properties (compiler and language versions)
    Map<String, String> extraProperties = buildTemplateProperties(project, simpleName);

    // Step 5: Delegate to SDK createFileFromTemplate with MRU persistence
    try {
      return createFileFromTemplate(cleanName, template, dir, getDefaultTemplateProperty(), true, extraProperties, extraProperties);
    } catch (ProcessCanceledException | IncorrectOperationException e) {
      throw e;
    } catch (Exception e) {
      LOG.warn("Failed to create Compact file from template " + templateName, e);
      return null;
    }
  }

  /** Builds a custom property map passed to the Velocity template engine during expansion. */
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

  /**
   * Post-processes newly created file: reformats code and positions editor caret.
   * Invoked on EDT after a file is created and opened in the editor.
   */
  @Override
  protected void postProcess(@NotNull PsiFile createdElement, String templateName, Map<String, String> customProperties) {
    // Step 1: Invoke superclass postProcess hooks
    super.postProcess(createdElement, templateName, customProperties);
    Project project = createdElement.getProject();

    // Step 2: Reformat created file via CodeStyleManager inside a WriteCommandAction
    WriteCommandAction.runWriteCommandAction(project, () -> {
      CodeStyleManager.getInstance(project).reformat(createdElement);
    });

    // Step 3: If editor is active, position caret at initial editing location
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null && editor.getDocument() == createdElement.getViewProvider().getDocument()) {
      int targetOffset = determineInitialCaretOffset(createdElement);
      if (targetOffset >= 0 && targetOffset <= editor.getDocument().getTextLength()) {
        editor.getCaretModel().moveToOffset(targetOffset);
      }
    }
  }

  /** Computes initial caret offset inside the first brace block of the created file. */
  public static int determineInitialCaretOffset(@NotNull PsiFile file) {
    // Step 1: Locate enclosing brace block boundaries
    String text = file.getText();
    int lbrace = text.indexOf('{');
    int rbrace = text.lastIndexOf('}');
    if (lbrace != -1 && rbrace > lbrace) {
      // Step 2: Find newline and indent after opening brace
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
    // Step 3: Fallback to document end if no brace block is found
    return text.length();
  }

  /** Strips .compact file extension if present (case-insensitive). */
  public static @NotNull String stripCompactExtension(@NotNull String name) {
    return name.toLowerCase(Locale.ROOT).endsWith(COMPACT_EXTENSION)
        ? name.substring(0, name.length() - COMPACT_EXTENSION.length())
        : name;
  }

  /** Extracts simple file name without leading directory segments. */
  public static @NotNull String extractSimpleName(@NotNull String name) {
    String clean = stripCompactExtension(name);
    int lastSlash = Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\'));
    return lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;
  }

  /** Validates that target a file or directory does not already exist in the target location. */
  public static @Nullable String validateFileCollision(@Nullable String inputString, @NotNull PsiDirectory directory) {
    // Step 1: Normalize input path and isolate simple file name
    if (inputString == null || inputString.trim().isEmpty()) return null;
    String cleanName = stripCompactExtension(inputString.trim());
    String simpleName = extractSimpleName(cleanName);
    if (simpleName.isEmpty()) return null;

    // Step 2: Resolve target directory hierarchy from relative path
    PsiDirectory targetDir = findTargetDirectory(directory, cleanName);
    if (targetDir == null) return null;

    // Step 3: Check for existing file or subdirectory with .compact name
    String fileName = simpleName + COMPACT_EXTENSION;
    if (targetDir.findFile(fileName) != null || targetDir.findSubdirectory(fileName) != null) {
      return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.already.exists", fileName);
    }
    return null;
  }

  /** Traverses or finds the target directory given relative directory path segments. */
  private static @Nullable PsiDirectory findTargetDirectory(@NotNull PsiDirectory baseDir, @NotNull String path) {
    int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    if (lastSlash < 0) return baseDir;
    String[] segments = path.substring(0, lastSlash).replace('\\', '/').split("/");
    PsiDirectory current = baseDir;
    for (String seg : segments) {
      if (!seg.isEmpty()) {
        current = current.findSubdirectory(seg);
        if (current == null) return null;
      }
    }
    return current;
  }

  /** Validates a file path against illegal filesystem characters and path traversal. */
  public static @Nullable String validateFileName(@Nullable String inputString) {
    // Step 1: Check for empty input string
    if (inputString == null || inputString.trim().isEmpty()) {
      return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.empty");
    }
    // Step 2: Validate against illegal characters and control characters (< 32)
    String trimmed = inputString.trim();
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c < 32 || c == '*' || c == '?' || c == ':' || c == '<' || c == '>' || c == '|' || c == '"') {
        return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.illegal.char",
            c < 32 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
      }
    }
    // Step 3: Disallow leading or trailing path separators
    if (trimmed.startsWith("/") || trimmed.startsWith("\\")) {
      return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.starts.with.separator");
    }
    if (trimmed.endsWith("/") || trimmed.endsWith("\\")) {
      return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.ends.with.separator");
    }
    // Step 4: Disallow empty segments or directory traversal ('.' or '..')
    for (String segment : trimmed.replace('\\', '/').split("/")) {
      String segTrim = segment.trim();
      if (segTrim.isEmpty()) {
        return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.empty.segment");
      }
      if (".".equals(segTrim) || "..".equals(segTrim)) {
        return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.traversal");
      }
    }
    // Step 5: Ensure extracted simple file name is non-empty
    return extractSimpleName(trimmed).isEmpty()
        ? CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.empty")
        : null;
  }

  /** Validates that target name is a valid Compact identifier and not a reserved keyword. */
  public static @Nullable String validateIdentifier(@Nullable String inputString, @Nullable Project project) {
    // Step 1: Perform filesystem and path syntax validation
    String fileError = validateFileName(inputString);
    if (fileError != null) return fileError;

    // Step 2: Check against Compact language reserved keywords via NamesValidator
    String simpleName = extractSimpleName(inputString.trim());
    if (NAMES_VALIDATOR.isKeyword(simpleName, project)) {
      return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.keyword", simpleName);
    }

    // Step 3: Verify compliance with Compact identifier lexical grammar
    if (!NAMES_VALIDATOR.isIdentifier(simpleName, project)) {
      return CompactBundle.message("action.dev.verloren.midnight.actions.CompactCreateFileAction.error.invalid.identifier", simpleName);
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
