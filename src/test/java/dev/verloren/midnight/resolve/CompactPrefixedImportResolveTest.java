package dev.verloren.midnight.resolve;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.CompactCircuitDefinition;
import dev.verloren.midnight.psi.CompactFile;
import dev.verloren.midnight.psi.CompactImportDeclarationImpl;
import dev.verloren.midnight.psi.CompactStructDefinition;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class CompactPrefixedImportResolveTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  public void testPrefixedRelativeImportResolvesCircuit() {
    myFixture.addFileToProject(
        "utils/Utils.compact",
        """
        export pure circuit isContractAddress(account: Field): Boolean {
          return true;
        }
        """
    );
    PsiFile test1File = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import "./utils/Utils" prefix Utils_;

        const result = <caret>Utils_isContractAddress(account);
        """
    );

    // 1. Verify import path "./utils/Utils" resolves to Utils.compact
    CompactImportDeclarationImpl importDecl = PsiTreeUtil.findChildOfType(test1File, CompactImportDeclarationImpl.class);
    assertNotNull("Import declaration must exist", importDecl);
    PsiReference pathRef = importDecl.getReference();
    assertNotNull("Import declaration must have a reference on the path string", pathRef);
    PsiElement pathTarget = pathRef.resolve();
    assertNotNull("Import path './utils/Utils' must resolve to Utils.compact", pathTarget);
    assertTrue("Target of import path must be CompactFile", pathTarget instanceof CompactFile);
    assertEquals("Utils.compact", ((CompactFile) pathTarget).getName());

    // 2. Verify prefix "Utils_" is correctly associated with that import
    assertEquals("Utils_", importDecl.getPrefix());

    // 3 & 4 & 5. Verify Utils_isContractAddress resolves to isContractAddress circuit in Utils.compact
    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Reference at caret should exist on Utils_isContractAddress", ref);
    PsiElement target = ref.resolve();
    assertNotNull("Utils_isContractAddress must resolve to the exported circuit declaration", target);
    assertTrue("Resolved target must be CompactCircuitDefinition", target instanceof CompactCircuitDefinition);
    CompactCircuitDefinition circuit = (CompactCircuitDefinition) target;
    assertEquals("isContractAddress", circuit.getName());
    assertEquals("Utils.compact", target.getContainingFile().getName());
  }

  public void testPrefixedRelativeImportWithExtensionResolvesCircuit() {
    myFixture.addFileToProject(
        "utils/Utils.compact",
        """
        export pure circuit isContractAddress(account: Field): Boolean {
          return true;
        }
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import "./utils/Utils.compact" prefix Utils_;

        circuit run(): Void {
          const result = <caret>Utils_isContractAddress(account);
        }
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    PsiElement target = ref.resolve();
    assertNotNull("Utils_isContractAddress with .compact in import path must resolve", target);
    assertTrue(target instanceof CompactCircuitDefinition);
    assertEquals("isContractAddress", ((CompactCircuitDefinition) target).getName());
    assertEquals("Utils.compact", target.getContainingFile().getName());
  }

  public void testPrefixedImportPreservesUnderscoreInExportedSymbol() {
    myFixture.addFileToProject(
        "utils/Utils.compact",
        """
        export pure circuit _internalCheck(account: Field): Boolean {
          return true;
        }
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import "./utils/Utils" prefix Utils_;

        circuit run(): Void {
          const result = <caret>Utils__internalCheck(account);
        }
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    PsiElement target = ref.resolve();
    assertNotNull("Utils__internalCheck must resolve to _internalCheck", target);
    assertTrue(target instanceof CompactCircuitDefinition);
    assertEquals("_internalCheck", ((CompactCircuitDefinition) target).getName());
    assertEquals("Utils.compact", target.getContainingFile().getName());
  }

  public void testPrefixedRelativeImportResolvesStructType() {
    myFixture.addFileToProject(
        "utils/Types.compact",
        """
        export struct AccountInfo {
          owner: Field,
          balance: Uint<64>,
        }
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import "./utils/Types" prefix Types_;

        circuit check(info: <caret>Types_AccountInfo): Void {}
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    PsiElement target = ref.resolve();
    assertNotNull("Types_AccountInfo type reference must resolve to struct AccountInfo", target);
    assertTrue(target instanceof CompactStructDefinition);
    assertEquals("AccountInfo", ((CompactStructDefinition) target).getName());
    assertEquals("Types.compact", target.getContainingFile().getName());
  }

  public void testPrefixedRelativeImportResolvesModuleExportedCircuit() {
    myFixture.addFileToProject(
        "utils/Helpers.compact",
        """
        module AddressLib {
          export circuit validateAddress(addr: Field): Boolean {
            return true;
          }
        }
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import "./utils/Helpers" prefix Lib_;

        circuit run(): Void {
          const valid = <caret>Lib_validateAddress(0);
        }
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull(ref);
    PsiElement target = ref.resolve();
    assertNotNull("Lib_validateAddress must resolve to validateAddress in AddressLib inside Helpers.compact", target);
    assertTrue(target instanceof CompactCircuitDefinition);
    assertEquals("validateAddress", ((CompactCircuitDefinition) target).getName());
    assertEquals("Helpers.compact", target.getContainingFile().getName());
  }

  public void testImportedEnumWithExtensionPath() {
    myFixture.addFileToProject(
        "GameState.compact",
        """
        export enum GameState {
            WAITING,
            PLAYING,
            FINISHED,
        }
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { GameState } from './GameState.compact';

        export circuit checkGame(): [] {
            const state = GameState.<caret>WAITING;
        }
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("WAITING should have reference at caret", ref);
    PsiElement target = ref.resolve();
    assertNotNull("WAITING should resolve across file with .compact path", target);
    assertTrue(target instanceof dev.verloren.midnight.psi.CompactEnumMemberImpl);
    assertEquals("WAITING", ((dev.verloren.midnight.psi.CompactEnumMemberImpl) target).getName());
    assertEquals("GameState.compact", target.getContainingFile().getName());
  }
}

