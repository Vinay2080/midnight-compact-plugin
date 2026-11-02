package dev.verloren.midnight.run;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Reproduction and regression tests for {@link CompactConsoleFilter}.
 */
public class CompactConsoleFilterTest extends BasePlatformTestCase {

  private CompactConsoleFilter filter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    filter = new CompactConsoleFilter(getProject());
    myFixture.addFileToProject("contracts/counter.compact", "contract Counter {}");
  }

  /**
   * Verifies that CompactConsoleFilter implements DumbAware so console links remain interactive during indexing.
   */
  @SuppressWarnings("ConstantValue")
  public void testDumbAwareContract() {
    assertTrue("CompactConsoleFilter must implement DumbAware", DumbAware.class.isAssignableFrom(CompactConsoleFilter.class));
  }

  /**
   * Verifies filtering of standard Unix-style relative path diagnostics.
   */
  public void testRelativeDiagnostic() {
    String line = "contracts/counter.compact:12:5: error: undefined symbol 'count'";
    Filter.Result result = filter.applyFilter(line, line.length());

    assertNotNull("Filter result should not be null for relative path", result);
    assertFalse(result.getResultItems().isEmpty());
    assertEquals(0, result.getResultItems().getFirst().getHighlightStartOffset());
    assertEquals("contracts/counter.compact:12:5".length(), result.getResultItems().getFirst().getHighlightEndOffset());
    assertNotNull(result.getFirstHyperlinkInfo());
    assertTrue(result.getFirstHyperlinkInfo() instanceof OpenFileHyperlinkInfo);

    OpenFileHyperlinkInfo hyperlinkInfo = (OpenFileHyperlinkInfo) result.getFirstHyperlinkInfo();
    assertNotNull(hyperlinkInfo.getDescriptor());
    assertEquals(11, hyperlinkInfo.getDescriptor().getLine());
    assertEquals(4, hyperlinkInfo.getDescriptor().getColumn());
  }

  /**
   * Reproduction test: Verifies that Windows absolute paths with drive letters (e.g. C:/... or C:\...)
   * are matched and hyperlinked correctly without failing on the drive colon.
   */
  public void testWindowsAbsolutePathDiagnostic() {
    VirtualFile file = myFixture.addFileToProject("mock/contracts/counter.compact", "circuit test() {}").getVirtualFile();
    assertNotNull(file);

    String line = "C:/mock/contracts/counter.compact:8:3: error: invalid circuit";
    Filter.Result result = filter.applyFilter(line, line.length());

    assertNotNull("Filter result should not be null for Windows absolute path with drive letter", result);
    assertFalse(result.getResultItems().isEmpty());
    assertEquals(0, result.getResultItems().getFirst().getHighlightStartOffset());
    assertEquals("C:/mock/contracts/counter.compact:8:3".length(), result.getResultItems().getFirst().getHighlightEndOffset());
  }

  /**
   * Reproduction test: Verifies that Chez Scheme compiler exception format:
   * "Exception in ...: path.compact line X, char Y: message" is matched and hyperlinked.
   */
  public void testChezSchemeExceptionFormat() {
    String line = "Exception in compile: contracts/counter.compact line 14, char 7: syntax error";
    Filter.Result result = filter.applyFilter(line, line.length());

    assertNotNull("Filter result should not be null for Chez Scheme exception format", result);
    assertNotNull(result.getFirstHyperlinkInfo());
    assertTrue(result.getFirstHyperlinkInfo() instanceof OpenFileHyperlinkInfo);

    OpenFileHyperlinkInfo hyperlinkInfo = (OpenFileHyperlinkInfo) result.getFirstHyperlinkInfo();
    assertNotNull(hyperlinkInfo.getDescriptor());
    assertEquals(13, hyperlinkInfo.getDescriptor().getLine());
    assertEquals(6, hyperlinkInfo.getDescriptor().getColumn());
  }

  /**
   * Companion test: Diagnostic with line number only (missing column number).
   */
  public void testLineOnlyWithoutColumnDiagnostic() {
    String line = "contracts/counter.compact:20: error: unexpected end of input";
    Filter.Result result = filter.applyFilter(line, line.length());

    assertNotNull("Filter result should support diagnostics without explicit column", result);
    assertNotNull(result.getFirstHyperlinkInfo());
    assertTrue(result.getFirstHyperlinkInfo() instanceof OpenFileHyperlinkInfo);

    OpenFileHyperlinkInfo hyperlinkInfo = (OpenFileHyperlinkInfo) result.getFirstHyperlinkInfo();
    assertNotNull(hyperlinkInfo.getDescriptor());
    assertEquals(19, hyperlinkInfo.getDescriptor().getLine());
    assertEquals(0, hyperlinkInfo.getDescriptor().getColumn());
  }

  /**
   * Negative assertion: Non-diagnostic text should return null without error.
   */
  public void testUnrelatedOutputIgnored() {
    String line = "Compiling 1 contract with compactc v0.24.0...";
    Filter.Result result = filter.applyFilter(line, line.length());
    assertNull("Unrelated compiler output should not produce hyperlinks", result);
  }

  /**
   * Verifies that getFirstErrorHyperlink tracks the primary error coordinate and does not overwrite it on subsequent errors.
   */
  public void testFirstErrorHyperlinkCapturedAndPreserved() {
    assertNull("Initially first error hyperlink must be null", filter.getFirstErrorHyperlink());

    String error1 = "contracts/counter.compact:5:2: error: syntax error";
    filter.applyFilter(error1, error1.length());

    OpenFileHyperlinkInfo firstError = filter.getFirstErrorHyperlink();
    assertNotNull("First error hyperlink must be recorded", firstError);
    assertNotNull(firstError.getDescriptor());
    assertEquals("First error line should match line 5 (0-indexed 4)", 4, firstError.getDescriptor().getLine());
    assertEquals("First error col should match col 2 (0-indexed 1)", 1, firstError.getDescriptor().getColumn());

    // Emit a second error on a different line
    String error2 = "contracts/counter.compact:18:10: error: type mismatch";
    filter.applyFilter(error2, error1.length() + error2.length());

    OpenFileHyperlinkInfo preservedError = filter.getFirstErrorHyperlink();
    assertNotNull("Preserved error must not be null", preservedError);
    assertNotNull(preservedError.getDescriptor());
    assertEquals("First error coordinate must be preserved and not overwritten by subsequent errors",
            4, preservedError.getDescriptor().getLine());
    assertEquals("First error column must be preserved",
            1, preservedError.getDescriptor().getColumn());
  }

  /**
   * Reproduction test: Verifies that a bare filename emitted by the compiler (e.g. CompileTargetContract.compact)
   * is resolved when the file resides in a subdirectory and targetFilePath is provided.
   */
  public void testBareFilenameInSubdirectoryResolvedViaTargetFile() {
    VirtualFile file = myFixture.addFileToProject("manual testing/14_compiler_run_action/CompileTargetContract.compact", "contract Target {}").getVirtualFile();
    assertNotNull(file);

    CompactConsoleFilter targetFilter = new CompactConsoleFilter(getProject(), file.getPath());
    String line = "CompileTargetContract.compact:14:5: error: missing semicolon";
    Filter.Result result = targetFilter.applyFilter(line, line.length());

    assertNotNull("Filter result must resolve bare filename when file is in a subdirectory", result);
    assertNotNull(result.getFirstHyperlinkInfo());
    assertTrue(result.getFirstHyperlinkInfo() instanceof OpenFileHyperlinkInfo);

    OpenFileHyperlinkInfo hyperlinkInfo = (OpenFileHyperlinkInfo) result.getFirstHyperlinkInfo();
    assertNotNull(hyperlinkInfo.getDescriptor());
    assertEquals(file, hyperlinkInfo.getDescriptor().getFile());
    assertEquals(13, hyperlinkInfo.getDescriptor().getLine());
    assertEquals(4, hyperlinkInfo.getDescriptor().getColumn());
  }

  /**
   * User-Defined Mirror Test: Verifies that a bare filename in a nested directory is resolved
   * even when targetFilePath is not provided by searching the project tree.
   */
  public void testBareFilenameInSubdirectoryResolvedViaProjectTreeSearch() {
    VirtualFile file = myFixture.addFileToProject("contracts/nested/MyCustomResultContract.compact", "struct Result<TVal, TErr> {}").getVirtualFile();
    assertNotNull(file);

    String line = "MyCustomResultContract.compact:22:8: error: type mismatch";
    Filter.Result result = filter.applyFilter(line, line.length());

    assertNotNull("Filter result must resolve bare filename by searching project tree", result);
    assertNotNull(result.getFirstHyperlinkInfo());
    assertTrue(result.getFirstHyperlinkInfo() instanceof OpenFileHyperlinkInfo);

    OpenFileHyperlinkInfo hyperlinkInfo = (OpenFileHyperlinkInfo) result.getFirstHyperlinkInfo();
    assertNotNull(hyperlinkInfo.getDescriptor());
    assertEquals(file, hyperlinkInfo.getDescriptor().getFile());
    assertEquals(21, hyperlinkInfo.getDescriptor().getLine());
    assertEquals(7, hyperlinkInfo.getDescriptor().getColumn());
  }

  /**
   * Verifies that range diagnostics (e.g. file.compact:12:5-10) are supported and parsed correctly.
   */
  public void testDiagnosticRangeFormat() {
    String line = "contracts/counter.compact:12:5-10: error: undefined symbol 'count'";
    Filter.Result result = filter.applyFilter(line, line.length());

    assertNotNull("Filter result should support range diagnostics with end column", result);
    assertNotNull(result.getFirstHyperlinkInfo());
    assertTrue(result.getFirstHyperlinkInfo() instanceof OpenFileHyperlinkInfo);

    OpenFileHyperlinkInfo hyperlinkInfo = (OpenFileHyperlinkInfo) result.getFirstHyperlinkInfo();
    assertNotNull(hyperlinkInfo.getDescriptor());
    assertEquals(11, hyperlinkInfo.getDescriptor().getLine());
    assertEquals(4, hyperlinkInfo.getDescriptor().getColumn());
  }
}
