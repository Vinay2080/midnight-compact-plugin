package dev.verloren.midnight.documentation;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.stdlib.CompactStdlibService;

public class CompactPragmaAndStdlibDocumentationTest extends BasePlatformTestCase {

  private CompactDocumentationProvider docProvider;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
    docProvider = new CompactDocumentationProvider();
  }

  public void testCompactStandardLibraryImportDoc() {
    String code = """
        import <caret>CompactStandardLibrary;
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(
        myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull("Hover on CompactStandardLibrary should find element", element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull("Doc should be generated for CompactStandardLibrary", doc);
    assertTrue(doc.contains("CompactStandardLibrary"));
    assertTrue(doc.contains("standard zero-knowledge utility library") || doc.contains("Midnight Compact Standard Library"));
  }

  public void testCompactStandardLibraryFileDoc() {
    CompactFile stdlibFile = CompactStdlibService.getInstance(getProject()).getStandardLibraryFiles().getFirst();
    assertNotNull(stdlibFile);

    String doc = docProvider.generateDoc(stdlibFile, null);
    assertNotNull("Doc should be generated for standard-library.compact", doc);
    assertTrue(doc.contains("standard library CompactStandardLibrary"));
    assertTrue(doc.contains("standard zero-knowledge utility library"));
  }

  public void testLanguageVersionPragmaDocumentation() {
    String code = """
        pragma <caret>language_version >= 0.26.0;
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("language_version"));
    assertTrue(doc.contains("Specifies the required version of the Compact language specification"));
    assertTrue(doc.contains("Directive:"));
    assertTrue(doc.contains("Constraint:"));
    assertTrue(doc.contains("&gt;= 0.26.0"));
    assertTrue(doc.contains("Allowed Settings:"));
  }

  public void testCompilerVersionPragmaDocumentation() {
    String code = """
        pragma <caret>compiler_version >= 0.26.0;
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("compiler_version"));
    assertTrue(doc.contains("Specifies the required version of the Compact compiler"));
    assertTrue(doc.contains("compactc"));
    assertTrue(doc.contains("Directive:"));
    assertTrue(doc.contains("Constraint:"));
  }

  public void testPragmaWithDocCommentDocumentation() {
    String code = """
        /// Target compiler version for production rollout
        pragma <caret>compiler_version >= 0.26.0;
        """;
    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    PsiElement element = docProvider.getCustomDocumentationElement(myFixture.getEditor(), file, file.findElementAt(myFixture.getCaretOffset()), myFixture.getCaretOffset());
    assertNotNull(element);

    String doc = docProvider.generateDoc(element, null);
    assertNotNull(doc);
    assertTrue(doc.contains("Target compiler version for production rollout"));
    assertTrue(doc.contains("compiler_version"));
  }

  public void testDocumentationElementForLookupItem() {
    PsiElement langElement = docProvider.getDocumentationElementForLookupItem(getPsiManager(), "language_version", null);
    assertNotNull(langElement);
    String langDoc = docProvider.generateDoc(langElement, null);
    assertNotNull(langDoc);
    assertTrue(langDoc.contains("language_version"));
    assertTrue(langDoc.contains("Specifies the required version of the Compact language specification"));

    PsiElement compElement = docProvider.getDocumentationElementForLookupItem(getPsiManager(), "compiler_version", null);
    assertNotNull(compElement);
    String compDoc = docProvider.generateDoc(compElement, null);
    assertNotNull(compDoc);
    assertTrue(compDoc.contains("compiler_version"));
    assertTrue(compDoc.contains("Specifies the required version of the Compact compiler"));
  }
}
