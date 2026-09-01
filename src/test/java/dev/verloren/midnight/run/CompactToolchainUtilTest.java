package dev.verloren.midnight.run;

import com.intellij.execution.ExecutionException;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.settings.MidnightSettingsState;

import java.io.File;

public class CompactToolchainUtilTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    MidnightSettingsState state = MidnightSettingsState.getInstance();
    if (state != null) {
      state.compilerPath = "";
    }
  }

  public void testCustomSettingsPathResolution() {
    MidnightSettingsState state = MidnightSettingsState.getInstance();
    assertNotNull(state);

    File temp = null;
    try {
      temp = File.createTempFile("mock-compactc", ".cmd");
      state.compilerPath = temp.getAbsolutePath();

      String resolved = CompactToolchainUtil.getCompilerExecutablePath(getProject());
      assertNotNull("Configured compiler path should be resolved", resolved);
      assertEquals(temp.getAbsolutePath(), resolved);
    } catch (Exception e) {
      fail(e.getMessage());
    } finally {
      if (temp != null) {
        temp.delete();
      }
      state.compilerPath = "";
    }
  }

  public void testToWslPathConversions() {
    assertEquals("/mnt/c/Users/shaki/contract.compact", CompactToolchainUtil.toWslPath("C:\\Users\\shaki\\contract.compact"));
    assertEquals("/mnt/d/projects/output", CompactToolchainUtil.toWslPath("D:/projects/output"));
    assertEquals("/home/verloren/.local/bin/compact", CompactToolchainUtil.toWslPath("\\\\wsl$\\Ubuntu\\home\\verloren\\.local\\bin\\compact"));
    assertEquals("/home/verloren/.local/bin/compact", CompactToolchainUtil.toWslPath("\\\\wsl.localhost\\Ubuntu\\home\\verloren\\.local\\bin\\compact"));
    assertEquals("/home/verloren/project/contract.compact", CompactToolchainUtil.toWslPath("/home/verloren/project/contract.compact"));
    assertEquals("gen", CompactToolchainUtil.toWslPath("gen"));
    assertEquals("--vscode", CompactToolchainUtil.toWslPath("--vscode"));
  }

  public void testParseConfiguredWslPaths() {
    CompactToolchainUtil.ToolchainInfo info1 = CompactToolchainUtil.parseConfiguredPath("/home/verloren/.local/bin/compact");
    assertNotNull(info1);
    assertTrue("Should be recognized as WSL", info1.isWsl());
    assertEquals("/home/verloren/.local/bin/compact", info1.executablePath());
    assertTrue("Should be recognized as compact CLI tool", info1.isCompactCli());

    CompactToolchainUtil.ToolchainInfo info2 = CompactToolchainUtil.parseConfiguredPath("\\\\wsl$\\Ubuntu\\home\\verloren\\.local\\bin\\compact");
    assertNotNull(info2);
    assertTrue(info2.isWsl());
    assertEquals("Ubuntu", info2.wslDistribution());
    assertEquals("/home/verloren/.local/bin/compact", info2.executablePath());
    assertTrue(info2.isCompactCli());

    CompactToolchainUtil.ToolchainInfo info3 = CompactToolchainUtil.parseConfiguredPath("wsl:Ubuntu:/usr/local/bin/compactc");
    assertNotNull(info3);
    assertTrue(info3.isWsl());
    assertEquals("Ubuntu", info3.wslDistribution());
    assertEquals("/usr/local/bin/compactc", info3.executablePath());
    assertFalse("Direct compactc is not compact CLI wrapper", info3.isCompactCli());

    // Test Windows system binary exclusion
    File system32Compact = new File("C:\\Windows\\System32\\compact.exe");
    assertTrue("C:\\Windows\\System32\\compact.exe should be identified as system binary on Windows",
        !com.intellij.openapi.util.SystemInfo.isWindows || CompactToolchainUtil.isWindowsSystemExecutable(system32Compact));
  }

  public void testCreateCommandLineWslCompactCli() throws ExecutionException {
    MidnightSettingsState state = MidnightSettingsState.getInstance();
    assertNotNull(state);
    state.compilerPath = "/home/verloren/.local/bin/compact";

    try {
      var cmd = CompactToolchainUtil.createCommandLine(
          getProject(),
          java.util.List.of("--vscode", "--skip-zk", "C:\\Users\\shaki\\contract.compact", "gen"),
          "C:\\Users\\shaki"
      );

      assertEquals("wsl.exe", cmd.getExePath());
      var params = cmd.getParametersList().getList();
      assertTrue("Should invoke -e", params.contains("-e"));
      assertTrue("Should invoke /home/verloren/.local/bin/compact", params.contains("/home/verloren/.local/bin/compact"));
      assertTrue("Should insert compile subcommand", params.contains("compile"));
      assertTrue("Should convert Windows path to WSL /mnt/c/...", params.contains("/mnt/c/Users/shaki/contract.compact"));
      assertTrue("Should retain gen output dir", params.contains("gen"));
    } finally {
      state.compilerPath = "";
    }
  }

  public void testExecutionWithDiscoveredToolchain() {
    CompactToolchainUtil.ToolchainInfo info = CompactToolchainUtil.getToolchainInfo(getProject());
    assertNotNull("Compiler toolchain should be detected on host or WSL", info);
    assertTrue("Toolchain executable should not be empty", info.isValid());

    CompactConfigurationType type = new CompactConfigurationType();
    CompactRunConfiguration config = new CompactRunConfiguration(getProject(), type.getConfigurationFactories()[0], "TestRun");

    try {
      var cmd = CompactToolchainUtil.createCommandLine(getProject(), config.buildCommandLineArgs(), null);
      assertNotNull("CommandLine should be created", cmd);
      assertNotNull("Executable should be set", cmd.getExePath());
    } catch (ExecutionException e) {
      fail("CommandLine creation should not fail when toolchain is detected: " + e.getMessage());
    }
  }

  public void testDeriveOutputDirectory() {
    assertEquals("gen/calculator", CompactToolchainUtil.deriveOutputDirectory(getProject(), "circuits/calculator.compact"));
    assertEquals("gen/counter", CompactToolchainUtil.deriveOutputDirectory(getProject(), "circuits/counter.compact"));
    assertEquals("gen/bboard", CompactToolchainUtil.deriveOutputDirectory(getProject(), "circuits/bboard.compact"));
    assertEquals("gen/calculator", CompactToolchainUtil.deriveOutputDirectory(getProject(), "src/calculator.compact"));
    assertEquals("gen/bboard", CompactToolchainUtil.deriveOutputDirectory(getProject(), "contracts/bboard.compact"));
    assertEquals("gen/token", CompactToolchainUtil.deriveOutputDirectory(getProject(), "compact/token.compact"));
    assertEquals("gen/calculator", CompactToolchainUtil.deriveOutputDirectory(getProject(), "calculator.compact"));

    // Nested directories under source roots
    assertEquals("gen/tokens/erc20", CompactToolchainUtil.deriveOutputDirectory(getProject(), "circuits/tokens/erc20.compact"));
    assertEquals("gen/v1/counter", CompactToolchainUtil.deriveOutputDirectory(getProject(), "circuits/v1/counter.compact"));
    assertEquals("gen/v2/counter", CompactToolchainUtil.deriveOutputDirectory(getProject(), "circuits/v2/counter.compact"));

    // Windows backslash paths
    assertEquals("gen/calculator", CompactToolchainUtil.deriveOutputDirectory(getProject(), "circuits\\calculator.compact"));
    assertEquals("gen/nested/contract", CompactToolchainUtil.deriveOutputDirectory(getProject(), "src\\nested\\contract.compact"));

    // Distinct non-standard folders prevent collisions
    assertEquals("gen/moduleA/calc", CompactToolchainUtil.deriveOutputDirectory(getProject(), "moduleA/calc.compact"));
    assertEquals("gen/moduleB/calc", CompactToolchainUtil.deriveOutputDirectory(getProject(), "moduleB/calc.compact"));

    // Custom base output directory
    assertEquals("dist/out/calculator", CompactToolchainUtil.deriveOutputDirectory(getProject(), "circuits/calculator.compact", "dist/out"));
    assertEquals("custom/gen/counter", CompactToolchainUtil.deriveOutputDirectory(getProject(), "circuits/counter.compact", "custom/gen/"));
  }
}
