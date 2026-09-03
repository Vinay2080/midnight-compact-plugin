package dev.verloren.midnight.annotator;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public class CompactExternalAnnotatorTest extends BasePlatformTestCase {

  public void testDiagnosticParserWithErrorsAndWarnings() {
    String output = """
        src/contract.compact:14:5: error: Unbound variable 'secretKey'
        src/contract.compact:28:10: warning: Circuit 'main' contains unused binding
        """;

    List<CompactCompilerDiagnostic> diagnostics = CompactCompilerOutputParser.parse(output);
    assertEquals("Should parse 2 diagnostics", 2, diagnostics.size());

    CompactCompilerDiagnostic d1 = diagnostics.getFirst();
    assertEquals("src/contract.compact", d1.filePath());
    assertEquals(14, d1.line());
    assertEquals(5, d1.column());
    assertEquals("Unbound variable 'secretKey'", d1.message());
    assertTrue("First diagnostic should be error", d1.isError());
    assertEquals(HighlightSeverity.ERROR, d1.severity());

    CompactCompilerDiagnostic d2 = diagnostics.get(1);
    assertEquals("src/contract.compact", d2.filePath());
    assertEquals(28, d2.line());
    assertEquals(10, d2.column());
    assertEquals("Circuit 'main' contains unused binding", d2.message());
    assertFalse("Second diagnostic should be warning", d2.isError());
    assertEquals(HighlightSeverity.WARNING, d2.severity());
  }

  public void testDiagnosticParserIgnoresIrrelevantOutput() {
    String output = """
        [compactc] Starting compilation...
        Compiling contract to ZKIR...
        Done in 240ms.
        """;

    List<CompactCompilerDiagnostic> diagnostics = CompactCompilerOutputParser.parse(output);
    assertTrue("Should not parse diagnostics from normal status logs", diagnostics.isEmpty());
  }

  public void testCompilerExceptionFormatParsed() {
    String output = """
        Exception: leaderboard.compact line 53 char 10:
        no compatible function named some is in scope at this call;
        one function is incompatible with the supplied generic values;
        supplied generic values: <>;
        declared generics for function at <standard library>: <type>
        """;

    List<CompactCompilerDiagnostic> diagnostics = CompactCompilerOutputParser.parse(output);
    assertEquals("Should parse 1 diagnostic for compiler exception", 1, diagnostics.size());

    CompactCompilerDiagnostic d = diagnostics.getFirst();
    assertEquals("leaderboard.compact", d.filePath());
    assertEquals(53, d.line());
    assertEquals(10, d.column());
    assertTrue("Should contain error message details", d.message().contains("no compatible function named some is in scope"));
    assertTrue("Should contain generic mismatch details", d.message().contains("supplied generic values: <>"));
    assertTrue("Diagnostic should be error", d.isError());
  }

  public void testDiagnosticParserWithSpacesInPath() {
    String output = """
        src/my contracts/ledger contract.compact:14:5: error: Unbound variable 'secretKey'
        """;

    List<CompactCompilerDiagnostic> diagnostics = CompactCompilerOutputParser.parse(output);
    assertEquals("Should parse 1 diagnostic for path with spaces", 1, diagnostics.size());

    CompactCompilerDiagnostic d = diagnostics.getFirst();
    assertEquals("src/my contracts/ledger contract.compact", d.filePath());
    assertEquals(14, d.line());
    assertEquals(5, d.column());
    assertEquals("Unbound variable 'secretKey'", d.message());
  }

  public void testDiagnosticParserWithWindowsDrivePath() {
    String output = """
        C:\\Users\\shaki\\Idea Projects\\contract.compact:14:5: error: Unbound variable 'secretKey'
        """;

    List<CompactCompilerDiagnostic> diagnostics = CompactCompilerOutputParser.parse(output);
    assertEquals("Should parse 1 diagnostic for Windows drive path", 1, diagnostics.size());

    CompactCompilerDiagnostic d = diagnostics.getFirst();
    assertEquals("C:\\Users\\shaki\\Idea Projects\\contract.compact", d.filePath());
    assertEquals(14, d.line());
    assertEquals(5, d.column());
    assertEquals("Unbound variable 'secretKey'", d.message());
  }

  public void testCompilerExceptionWithComma() {
    String output = """
        Exception: leaderboard.compact line 53, char 10: type mismatch; expected Field, found Boolean
        """;

    List<CompactCompilerDiagnostic> diagnostics = CompactCompilerOutputParser.parse(output);
    assertEquals("Should parse 1 diagnostic for exception with comma", 1, diagnostics.size());

    CompactCompilerDiagnostic d = diagnostics.getFirst();
    assertEquals("leaderboard.compact", d.filePath());
    assertEquals(53, d.line());
    assertEquals(10, d.column());
    assertEquals("type mismatch; expected Field, found Boolean", d.message());
  }

  public void testCompilerExceptionWithPhasePrefix() {
    String output = """
        Exception in typechecker: leaderboard.compact line 53 char 10: unknown identifier 'foo'
        """;

    List<CompactCompilerDiagnostic> diagnostics = CompactCompilerOutputParser.parse(output);
    assertEquals("Should parse 1 diagnostic for exception with phase prefix", 1, diagnostics.size());

    CompactCompilerDiagnostic d = diagnostics.getFirst();
    assertEquals("leaderboard.compact", d.filePath());
    assertEquals(53, d.line());
    assertEquals(10, d.column());
    assertEquals("unknown identifier 'foo'", d.message());
  }

  public void testIsDiagnosticForFileMatchesTargetFile() {
    PsiFile psiFile = myFixture.configureByText("contract.compact", "circuit main() {}");

    CompactCompilerDiagnostic matchingDiag = new CompactCompilerDiagnostic(
        "contract.compact", 1, 1, "Error in contract", true
    );
    assertTrue("Should match same file name", CompactExternalAnnotator.isDiagnosticForFile(matchingDiag, psiFile));

    CompactCompilerDiagnostic relativeDiag = new CompactCompilerDiagnostic(
        "src/contract.compact", 1, 1, "Error in contract", true
    );
    assertTrue("Should match relative path with same file name", CompactExternalAnnotator.isDiagnosticForFile(relativeDiag, psiFile));

    CompactCompilerDiagnostic foreignDiag = new CompactCompilerDiagnostic(
        "other.compact", 1, 1, "Error in other contract", true
    );
    assertFalse("Should not match different file name", CompactExternalAnnotator.isDiagnosticForFile(foreignDiag, psiFile));
  }

  public void testIsDiagnosticForVirtualFileWslPath() {
    VirtualFile vFile = new LightVirtualFile("C:/projects/midnight/contract.compact");

    CompactCompilerDiagnostic wslDiag = new CompactCompilerDiagnostic(
        "/mnt/c/projects/midnight/contract.compact", 1, 1, "WSL error", true
    );
    assertTrue("Should match WSL path translated to Windows path", CompactExternalAnnotator.isDiagnosticForVirtualFile(wslDiag, vFile));
  }
}
