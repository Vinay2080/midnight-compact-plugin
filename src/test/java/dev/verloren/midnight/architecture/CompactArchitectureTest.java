package dev.verloren.midnight.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.Assert.assertTrue;

/**
 * Architectural compliance test suite enforcing layer isolation, downward-only dependencies,
 * and modularity boundaries in the Midnight Compact plugin codebase using ArchUnit bytecode analysis.
 *
 * <p>Runs as a pure JVM test without launching the heavy IntelliJ Platform test fixture.</p>
 */
public class CompactArchitectureTest {

  private static final JavaClasses CLASSES = new ClassFileImporter()
      .importPackages("dev.verloren.midnight");

  /**
   * Enforces that core semantic layers (type, resolve, scope, symbol, psi, lexer, parser)
   * do not depend on UI, completion, inspection, editor, or tool window layers.
   */
  @Test
  public void testLayerDependencyInvariants() {
    ArchRule downwardOnlyDependencies = noClasses()
        .that().resideInAnyPackage(
            "dev.verloren.midnight.type..",
            "dev.verloren.midnight.resolve..",
            "dev.verloren.midnight.scope..",
            "dev.verloren.midnight.symbol..",
            "dev.verloren.midnight.psi..",
            "dev.verloren.midnight.lexer..",
            "dev.verloren.midnight.parser.."
        )
        .should().dependOnClassesThat().resideInAnyPackage(
            "dev.verloren.midnight.completion..",
            "dev.verloren.midnight.inspection..",
            "dev.verloren.midnight.editor..",
            "dev.verloren.midnight.intention..",
            "dev.verloren.midnight.toolwindow..",
            "dev.verloren.midnight.statusbar.."
        )
        .because("Core semantic layers must remain independent of UI and editor layers");

    downwardOnlyDependencies.check(CLASSES);
  }

  /**
   * Enforces that the lexer and parser do not depend on high-level semantic type or resolution layers.
   */
  @Test
  public void testLexerParserIsolation() {
    ArchRule lexerParserIsolation = noClasses()
        .that().resideInAnyPackage(
            "dev.verloren.midnight.lexer..",
            "dev.verloren.midnight.parser.."
        )
        .should().dependOnClassesThat().resideInAnyPackage(
            "dev.verloren.midnight.type..",
            "dev.verloren.midnight.resolve..",
            "dev.verloren.midnight.inspection.."
        )
        .because("Lexer and parser must depend only on tokens, element types, and AST structures");

    lexerParserIsolation.check(CLASSES);
  }

  /**
   * Verifies that the rule definitions exist and are properly registered in the codebase.
   */
  @Test
  public void testArchitectureRulesDocumentExists() {
    Path rulesPath = Path.of(".agents/rules/architecture.rules.md");
    if (!Files.exists(rulesPath)) {
      rulesPath = Path.of("midnight-plugin/.agents/rules/architecture.rules.md");
    }
    assertTrue("architecture.rules.md must exist in .agents/rules/", Files.exists(rulesPath));
  }
}
