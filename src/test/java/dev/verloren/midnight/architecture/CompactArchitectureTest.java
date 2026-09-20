package dev.verloren.midnight.architecture;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Architectural compliance test suite enforcing layer isolation, downward-only dependencies,
 * and modularity boundaries in the Midnight Compact plugin codebase.
 */
public class CompactArchitectureTest extends BasePlatformTestCase {

  /**
   * Enforces that core semantic layers (type, resolve, psi, lexer, parser) do not import
   * UI, completion, inspection, or editor layers.
   */
  public void testLayerDependencyInvariants() throws IOException {
    Path srcMain = Path.of("src/main/java/dev/verloren/midnight");
    if (!Files.exists(srcMain)) {
      srcMain = Path.of("midnight-plugin/src/main/java/dev/verloren/midnight");
    }
    assertTrue("Source root must exist: " + srcMain, Files.exists(srcMain));

    List<String> prohibitedUpwardImports = List.of(
        "dev.verloren.midnight.completion.",
        "dev.verloren.midnight.inspection.",
        "dev.verloren.midnight.editor.",
        "dev.verloren.midnight.intention.",
        "dev.verloren.midnight.toolwindow.",
        "dev.verloren.midnight.statusbar."
    );

    List<String> coreSemanticPackages = List.of("type", "resolve", "scope", "symbol", "psi", "lexer", "parser");
    List<String> violations = new ArrayList<>();

    for (String pkg : coreSemanticPackages) {
      Path pkgPath = srcMain.resolve(pkg);
      if (!Files.exists(pkgPath)) {
        continue;
      }
      try (Stream<Path> files = Files.walk(pkgPath)) {
        files.filter(p -> p.toString().endsWith(".java")).forEach(javaFile -> {
          try {
            List<String> lines = Files.readAllLines(javaFile);
            for (int i = 0; i < lines.size(); i++) {
              String line = lines.get(i).trim();
              if (line.startsWith("import ")) {
                for (String prohibited : prohibitedUpwardImports) {
                  if (line.contains(prohibited)) {
                    violations.add(javaFile.getFileName() + ":" + (i + 1) + " imports prohibited layer: " + line);
                  }
                }
              }
            }
          } catch (IOException e) {
            fail("Failed reading " + javaFile + ": " + e.getMessage());
          }
        });
      }
    }

    assertTrue("Architectural Layer Invariant Violations found:\n" + String.join("\n", violations),
        violations.isEmpty());
  }

  /**
   * Verifies that the rule definitions exist and are properly registered in the codebase.
   */
  public void testArchitectureRulesDocumentExists() {
    Path rulesPath = Path.of(".agents/rules/architecture.rules.md");
    if (!Files.exists(rulesPath)) {
      rulesPath = Path.of("midnight-plugin/.agents/rules/architecture.rules.md");
    }
    assertTrue("architecture.rules.md must exist in .agents/rules/", Files.exists(rulesPath));
  }
}
