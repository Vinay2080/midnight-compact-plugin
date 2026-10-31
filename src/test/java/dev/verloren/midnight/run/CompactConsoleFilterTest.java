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
}
