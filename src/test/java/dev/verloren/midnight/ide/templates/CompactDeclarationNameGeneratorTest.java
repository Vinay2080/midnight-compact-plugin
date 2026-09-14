package dev.verloren.midnight.ide.templates;

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;

/**
 * Unit tests covering generalized declaration auto-numbering and scope analysis.
 *
 * <p>Verifies:
 * <ul>
 *   <li>First declaration of each type begins with index 1 (e.g. {@code circuit1}, {@code witness1}, {@code struct1}).</li>
 *   <li>Sequential declarations increment correctly ({@code circuit1} &rarr; {@code circuit2}).</li>
 *   <li>Gaps in numbering are backfilled with the lowest available positive integer (e.g. 1 and 3 present &rarr; 2).</li>
 *   <li>Explicitly named declarations are preserved without alteration.</li>
 *   <li>Name collisions in scope are avoided.</li>
 *   <li>Multiple declaration types coexist without interfering with each other's sequence.</li>
 *   <li>Scope isolation allows identical local numbering across sibling modules.</li>
 *   <li>Extensible custom declaration types generate properly numbered names.</li>
 * </ul>
 * </p>
 */
public class CompactDeclarationNameGeneratorTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  public void testFirstDeclarationOfEachType() {
    myFixture.configureByText(CompactFileType.INSTANCE, "");
    PsiElement context = myFixture.getFile();

    assertEquals("circuit1", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.CIRCUIT, context));
    assertEquals("witness1", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.WITNESS, context));
    assertEquals("struct1", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.STRUCT, context));
    assertEquals("enum1", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.ENUM, context));
    assertEquals("module1", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.MODULE, context));
    assertEquals("contract1", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.CONTRACT, context));
    assertEquals("type1", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.TYPE, context));
    assertEquals("ledger1", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.LEDGER, context));
    assertEquals("const1", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.CONST, context));
  }

  public void testSequentialDeclarations() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export circuit circuit1(): Void {}
        """
    );
    PsiElement context = myFixture.getFile();

    String nextCircuit = CompactDeclarationNameGenerator.generateName(CompactDeclarationType.CIRCUIT, context);
    assertEquals("circuit2", nextCircuit);
  }

  public void testSequentialDeclarationsMultipleExisting() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export circuit circuit1(): Void {}
        export circuit circuit2(): Void {}
        """
    );
    PsiElement context = myFixture.getFile();

    String nextCircuit = CompactDeclarationNameGenerator.generateName(CompactDeclarationType.CIRCUIT, context);
    assertEquals("circuit3", nextCircuit);
  }

  public void testGapsInNumberingFilledWithLowestAvailableInteger() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export circuit circuit1(): Void {}
        export circuit circuit3(): Void {}
        """
    );
    PsiElement context = myFixture.getFile();

    // circuit1 and circuit3 exist -> lowest available positive integer is 2
    String nextCircuit = CompactDeclarationNameGenerator.generateName(CompactDeclarationType.CIRCUIT, context);
    assertEquals("circuit2", nextCircuit);
  }

  public void testGapsAtStartFilledFirst() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export circuit circuit2(): Void {}
        export circuit circuit3(): Void {}
        """
    );
    PsiElement context = myFixture.getFile();

    // circuit2 and circuit3 exist, but circuit1 does not -> lowest available is 1
    String nextCircuit = CompactDeclarationNameGenerator.generateName(CompactDeclarationType.CIRCUIT, context);
    assertEquals("circuit1", nextCircuit);
  }

  public void testPreservesExplicitlyProvidedNames() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export circuit circuit1(): Void {}
        """
    );
    PsiElement context = myFixture.getFile();

    assertEquals("customCircuitName",
        CompactDeclarationNameGenerator.generateName(CompactDeclarationType.CIRCUIT, "customCircuitName", context));
    assertEquals("myWitness",
        CompactDeclarationNameGenerator.generateName(CompactDeclarationType.WITNESS, "myWitness", context));
  }

  public void testMultipleDeclarationTypesCoexisting() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export circuit circuit1(): Void {}
        witness witness1(): Field;
        struct struct1 {}
        """
    );
    PsiElement context = myFixture.getFile();

    // circuit1 exists -> next circuit is circuit2
    assertEquals("circuit2", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.CIRCUIT, context));
    // witness1 exists -> next witness is witness2
    assertEquals("witness2", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.WITNESS, context));
    // struct1 exists -> next struct is struct2
    assertEquals("struct2", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.STRUCT, context));
    // enum1 does not exist -> first enum is enum1
    assertEquals("enum1", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.ENUM, context));
    // ledger1 does not exist -> first ledger is ledger1
    assertEquals("ledger1", CompactDeclarationNameGenerator.generateName(CompactDeclarationType.LEDGER, context));
  }

  public void testScopeIsolationBetweenModules() {
    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export module ModuleA {
          export circuit circuit1(): Void {}
        }

        export module ModuleB {
          <caret>
        }
        """
    );
    PsiElement caretContext = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(caretContext);

    // Inside ModuleB, circuit1 from ModuleA is not in this module's direct scope -> should be circuit1
    String nextInModuleB = CompactDeclarationNameGenerator.generateName(CompactDeclarationType.CIRCUIT, caretContext);
    assertEquals("circuit1", nextInModuleB);
  }

  public void testExtensibilityCustomDeclarationType() {
    CompactDeclarationType.registerCustomType("oracle", "oracle");

    myFixture.configureByText(CompactFileType.INSTANCE,
        """
        export oracle oracle1;
        """
    );
    PsiElement context = myFixture.getFile();

    String nextOracle = CompactDeclarationNameGenerator.generateName("oracle", context);
    assertEquals("oracle2", nextOracle);
  }

  public void testExtensibilityResolveBaseName() {
    assertEquals("circuit", CompactDeclarationType.resolveBaseName("circuit"));
    assertEquals("circuit", CompactDeclarationType.resolveBaseName("cir"));
    assertEquals("witness", CompactDeclarationType.resolveBaseName("witness"));
    assertEquals("ledger", CompactDeclarationType.resolveBaseName("export ledger"));
    assertEquals("contract", CompactDeclarationType.resolveBaseName("cct"));

    CompactDeclarationType.registerCustomType("channel", "chan");
    assertEquals("chan", CompactDeclarationType.resolveBaseName("channel"));
  }
}
