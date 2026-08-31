package dev.verloren.midnight.annotator;

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

    CompactCompilerDiagnostic d2 = diagnostics.get(1);
    assertEquals("src/contract.compact", d2.filePath());
    assertEquals(28, d2.line());
    assertEquals(10, d2.column());
    assertEquals("Circuit 'main' contains unused binding", d2.message());
    assertFalse("Second diagnostic should be warning", d2.isError());
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
}
