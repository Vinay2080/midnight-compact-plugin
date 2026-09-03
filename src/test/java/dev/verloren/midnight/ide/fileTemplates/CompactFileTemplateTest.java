package dev.verloren.midnight.ide.fileTemplates;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.actions.CompactCreateFileAction;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactConstructorDeclaration;
import dev.verloren.midnight.psi.CompactExternalContractDeclaration;
import dev.verloren.midnight.psi.CompactLedgerDeclaration;
import dev.verloren.midnight.psi.CompactModuleDefinition;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class CompactFileTemplateTest extends BasePlatformTestCase {

  public void testFileTemplateGroupDescriptor() {
    CompactFileTemplateGroupFactory factory = new CompactFileTemplateGroupFactory();
    FileTemplateGroupDescriptor descriptor = factory.getFileTemplatesDescriptor();
    assertNotNull(descriptor);
    assertEquals("Midnight Compact", descriptor.getTitle());
    assertEquals(4, descriptor.getTemplates().size());
  }

  public void testContractTemplateEvaluatesAndParsesCleanly() throws IOException {
    FileTemplate template = FileTemplateManager.getInstance(getProject())
        .getInternalTemplate(CompactFileTemplateGroupFactory.COMPACT_CONTRACT);
    assertNotNull(template);

    Properties props = new Properties();
    props.setProperty(FileTemplate.ATTRIBUTE_NAME, "MyContract");
    String text = template.getText(props);

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, text);
    assertFalse("Contract template produced PSI parse errors: " + getErrors(file), hasErrorElement(file));

    CompactLedgerDeclaration ledger = PsiTreeUtil.findChildOfType(file, CompactLedgerDeclaration.class);
    assertNotNull("Contract template should declare ledger state", ledger);

    CompactConstructorDeclaration constructor = PsiTreeUtil.findChildOfType(file, CompactConstructorDeclaration.class);
    assertNotNull("Contract template should declare constructor", constructor);

    CompactCircuitDefinition circuit = PsiTreeUtil.findChildOfType(file, CompactCircuitDefinition.class);
    assertNotNull("Contract template should declare circuit", circuit);

    myFixture.enableInspections(
        dev.verloren.midnight.inspection.CompactUnresolvedReferenceInspection.class,
        dev.verloren.midnight.inspection.CompactDuplicateDeclarationInspection.class,
        dev.verloren.midnight.inspection.CompactUnusedLocalVariableInspection.class,
        dev.verloren.midnight.inspection.CompactTypeMismatchInspection.class,
        dev.verloren.midnight.inspection.CompactPureCircuitInspection.class,
        dev.verloren.midnight.inspection.CompactSealedFieldMutationInspection.class,
        dev.verloren.midnight.inspection.CompactRecursiveCircuitInspection.class,
        dev.verloren.midnight.inspection.CompactConstructorRestrictionInspection.class,
        dev.verloren.midnight.inspection.CompactUndisclosedWitnessInspection.class
    );
    java.util.List<com.intellij.codeInsight.daemon.impl.HighlightInfo> highlights = myFixture.doHighlighting();
    java.util.List<com.intellij.codeInsight.daemon.impl.HighlightInfo> problems = highlights.stream()
        .filter(h -> h.getSeverity() == com.intellij.lang.annotation.HighlightSeverity.WARNING || h.getSeverity() == com.intellij.lang.annotation.HighlightSeverity.ERROR)
        .toList();
    assertTrue("Found inspection problems: " + problems, problems.isEmpty());
  }

  public void testModuleTemplateEvaluatesAndParsesCleanly() throws IOException {
    FileTemplate template = FileTemplateManager.getInstance(getProject())
        .getInternalTemplate(CompactFileTemplateGroupFactory.COMPACT_MODULE);
    assertNotNull(template);

    Properties props = new Properties();
    props.setProperty(FileTemplate.ATTRIBUTE_NAME, "MyModule");
    String text = template.getText(props);

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, text);
    assertFalse("Module template produced PSI parse errors: " + getErrors(file), hasErrorElement(file));

    CompactModuleDefinition module = PsiTreeUtil.findChildOfType(file, CompactModuleDefinition.class);
    assertNotNull("Module template should define module", module);
    assertEquals("MyModule", module.getName());
  }

  public void testInterfaceTemplateEvaluatesAndParsesCleanly() throws IOException {
    FileTemplate template = FileTemplateManager.getInstance(getProject())
        .getInternalTemplate(CompactFileTemplateGroupFactory.COMPACT_INTERFACE);
    assertNotNull(template);

    Properties props = new Properties();
    props.setProperty(FileTemplate.ATTRIBUTE_NAME, "MyInterface");
    String text = template.getText(props);

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, text);
    assertFalse("Interface template produced PSI parse errors: " + getErrors(file), hasErrorElement(file));

    CompactExternalContractDeclaration contract = PsiTreeUtil.findChildOfType(file, CompactExternalContractDeclaration.class);
    assertNotNull("Interface template should declare external contract", contract);
    assertEquals("MyInterface", contract.getName());
  }

  public void testEmptyFileTemplateEvaluatesAndParsesCleanly() throws IOException {
    FileTemplate template = FileTemplateManager.getInstance(getProject())
        .getInternalTemplate(CompactFileTemplateGroupFactory.COMPACT_FILE);
    assertNotNull(template);

    Properties props = new Properties();
    props.setProperty(FileTemplate.ATTRIBUTE_NAME, "Empty");
    String text = template.getText(props);

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, text);
    assertFalse("Empty file template produced PSI parse errors: " + getErrors(file), hasErrorElement(file));
  }

  public void testCreateFileAction() {
    CompactCreateFileAction action = new CompactCreateFileAction();
    assertEquals("Create Compact File MyContract", action.getActionName(null, "MyContract", CompactFileTemplateGroupFactory.COMPACT_CONTRACT));
    assertEquals(new CompactCreateFileAction(), action);
    assertEquals(action.hashCode(), new CompactCreateFileAction().hashCode());
    assertEquals(CompactCreateFileAction.LAST_TEMPLATE_PROPERTY, "dev.verloren.midnight.template.last");
  }

  public void testCreateFileActionCreatesFile() {
    PsiFile dummy = myFixture.configureByText("dummy.compact", "pragma language_version >= 0.20.0;");
    PsiDirectory dir = dummy.getContainingDirectory();
    assertNotNull(dir);

    CompactCreateFileAction action = new CompactCreateFileAction();
    PsiFile created = action.createFile("Token", CompactFileTemplateGroupFactory.COMPACT_CONTRACT, dir);
    assertNotNull(created);
    assertEquals("Token.compact", created.getName());
    assertFalse("Created file should not have parse errors: " + getErrors(created), hasErrorElement(created));
  }

  public void testCreateFileActionCreatesFileInNestedSubdirectory() {
    PsiFile dummy = myFixture.configureByText("dummy.compact", "pragma language_version >= 0.20.0;");
    PsiDirectory dir = dummy.getContainingDirectory();
    assertNotNull(dir);

    CompactCreateFileAction action = new CompactCreateFileAction();
    PsiFile created = action.createFile("contracts/tokens/NestedToken", CompactFileTemplateGroupFactory.COMPACT_MODULE, dir);
    assertNotNull(created);
    assertEquals("NestedToken.compact", created.getName());
    assertEquals("tokens", created.getContainingDirectory().getName());
    assertEquals("contracts", Objects.requireNonNull(created.getContainingDirectory().getParentDirectory()).getName());
    assertFalse("Nested created file should not have parse errors: " + getErrors(created), hasErrorElement(created));
    assertTrue("Content should declare NestedToken module", created.getText().contains("export module NestedToken"));
  }

  public void testCreateFileActionStripsSuffixForContractName() {
    PsiFile dummy = myFixture.configureByText("dummy.compact", "pragma language_version >= 0.20.0;");
    PsiDirectory dir = dummy.getContainingDirectory();
    assertNotNull(dir);

    CompactCreateFileAction action = new CompactCreateFileAction();
    PsiFile created = action.createFile("TokenSuffix.compact", CompactFileTemplateGroupFactory.COMPACT_INTERFACE, dir);
    assertNotNull(created);
    assertEquals("TokenSuffix.compact", created.getName());
    assertFalse("Suffix stripped file should not have parse errors: " + getErrors(created), hasErrorElement(created));
    assertTrue("Content should declare TokenSuffix contract interface", created.getText().contains("export contract TokenSuffix"));
    assertFalse("Content should not contain .compact in contract identifier", created.getText().contains("export contract TokenSuffix.compact"));
  }

  public void testExtractSimpleName() {
    assertEquals("Token", CompactCreateFileAction.extractSimpleName("Token"));
    assertEquals("Token", CompactCreateFileAction.extractSimpleName("Token.compact"));
    assertEquals("Token", CompactCreateFileAction.extractSimpleName("contracts/Token"));
    assertEquals("Token", CompactCreateFileAction.extractSimpleName("contracts/tokens/Token.compact"));
    assertEquals("Token", CompactCreateFileAction.extractSimpleName("contracts\\tokens\\Token.compact"));
  }

  public void testValidateFileName() {
    assertNotNull("Empty name should be rejected", CompactCreateFileAction.validateFileName(""));
    assertNotNull("Whitespace name should be rejected", CompactCreateFileAction.validateFileName("   "));
    assertNotNull("Illegal char * should be rejected", CompactCreateFileAction.validateFileName("bad*name"));
    assertNotNull("Illegal char ? should be rejected", CompactCreateFileAction.validateFileName("bad?name"));
    assertNotNull("Illegal char : should be rejected", CompactCreateFileAction.validateFileName("bad:name"));
    assertNotNull("Leading slash should be rejected", CompactCreateFileAction.validateFileName("/foo"));
    assertNotNull("Trailing slash should be rejected", CompactCreateFileAction.validateFileName("foo/"));
    assertNotNull("Empty segment should be rejected", CompactCreateFileAction.validateFileName("foo//bar"));

    assertNull("Valid simple name should pass", CompactCreateFileAction.validateFileName("Token"));
    assertNull("Valid name with extension should pass", CompactCreateFileAction.validateFileName("Token.compact"));
    assertNull("Valid subpath should pass", CompactCreateFileAction.validateFileName("contracts/Token"));
  }

  public void testValidateIdentifier() {
    assertNotNull("Reserved keyword 'circuit' should be rejected",
        CompactCreateFileAction.validateIdentifier("circuit", getProject()));
    assertNotNull("Reserved keyword 'witness' should be rejected",
        CompactCreateFileAction.validateIdentifier("witness", getProject()));
    assertNotNull("Reserved keyword 'ledger' should be rejected",
        CompactCreateFileAction.validateIdentifier("ledger", getProject()));
    assertNotNull("Invalid identifier with digits at start should be rejected",
        CompactCreateFileAction.validateIdentifier("123Token", getProject()));
    assertNotNull("Invalid identifier with hyphen should be rejected",
        CompactCreateFileAction.validateIdentifier("my-token", getProject()));

    assertNull("Valid identifier 'MyContract' should pass",
        CompactCreateFileAction.validateIdentifier("MyContract", getProject()));
    assertNull("Valid identifier with extension 'MyContract.compact' should pass",
        CompactCreateFileAction.validateIdentifier("MyContract.compact", getProject()));
    assertNull("Valid subpath identifier 'contracts/MyContract.compact' should pass",
        CompactCreateFileAction.validateIdentifier("contracts/MyContract.compact", getProject()));
  }

  private static boolean hasErrorElement(PsiFile file) {
    return PsiTreeUtil.findChildOfType(file, PsiErrorElement.class) != null;
  }

  private static String getErrors(PsiFile file) {
    PsiErrorElement err = PsiTreeUtil.findChildOfType(file, PsiErrorElement.class);
    return err != null ? err.getErrorDescription() : "none";
  }
}
