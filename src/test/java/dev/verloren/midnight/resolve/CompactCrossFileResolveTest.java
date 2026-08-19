package dev.verloren.midnight.resolve;

import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.reference.CompactIncludeReference;

public class CompactCrossFileResolveTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  public void testDirectCrossFileResolve() {
    myFixture.addFileToProject(
        "A.compact",
        """
        circuit helper(): Void {}
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        include "A.compact";
        circuit main(): Void {
          <caret>helper();
        }
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("helper should have reference at caret", ref);
    PsiElement target = ref.resolve();
    assertNotNull("helper should resolve across files", target);
    assertTrue(target instanceof CompactCircuitDefinition);
    assertEquals("helper", ((CompactCircuitDefinition) target).getName());
    assertEquals("A.compact", target.getContainingFile().getName());
  }

  public void testCrossFileStructTypeResolve() {
    myFixture.addFileToProject(
        "Types.compact",
        """
        struct Point {
          x: Field,
          y: Field,
        }
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        include "Types.compact";
        circuit test(p: <caret>Point): Void {}
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Point type should have reference at caret", ref);
    PsiElement target = ref.resolve();
    assertNotNull("Point type should resolve across files", target);
    assertTrue(target instanceof CompactStructDefinition);
    assertEquals("Point", ((CompactStructDefinition) target).getName());
    assertEquals("Types.compact", target.getContainingFile().getName());
  }

  public void testCrossFileShadowingByLocal() {
    myFixture.addFileToProject(
        "A.compact",
        """
        const LIMIT = 10;
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        include "A.compact";
        const LIMIT = 20;
        circuit test(): Void {
          const x = <caret>LIMIT;
        }
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("LIMIT should have reference at caret", ref);
    PsiElement target = ref.resolve();
    assertNotNull("LIMIT should resolve", target);
    assertEquals("Local declaration must shadow external file", myFixture.getFile().getName(), target.getContainingFile().getName());
  }

  public void testCrossFileNamespaceSeparation() {
    myFixture.addFileToProject(
        "Types.compact",
        """
        struct Config {
          id: Field,
        }
        """
    );
    PsiFile mainFile = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        include "Types.compact";
        const Config = 100;
        circuit test(c: <caret>Config): Void {
          const v = Config;
        }
        """
    );

    // 1. Type resolution: c: Config resolves to struct in Types.compact
    PsiReference typeRef = myFixture.getReferenceAtCaretPosition();
    assertNotNull(typeRef);
    PsiElement typeTarget = typeRef.resolve();
    assertNotNull("Config type must resolve", typeTarget);
    assertTrue("Type Config must resolve to struct in Types.compact", typeTarget instanceof CompactStructDefinition);
    assertEquals("Types.compact", typeTarget.getContainingFile().getName());

    // 2. Value resolution: const v = Config resolves to const in Main file
    CompactReferenceExprImpl valRefExpr = PsiTreeUtil.findChildrenOfType(mainFile, CompactReferenceExprImpl.class).stream()
        .filter(e -> "Config".equals(e.getText()))
        .findFirst()
        .orElse(null);
    assertNotNull("Value reference Config should exist", valRefExpr);
    assertNotNull(valRefExpr.getReference());
    PsiElement valTarget = valRefExpr.getReference().resolve();
    assertNotNull("Value reference Config must resolve", valTarget);
    assertEquals("Value Config must resolve to const in Main file", mainFile.getName(), valTarget.getContainingFile().getName());
  }

  public void testTransitiveIncludes() {
    myFixture.addFileToProject(
        "Base.compact",
        """
        circuit baseHelper(): Void {}
        """
    );
    myFixture.addFileToProject(
        "Middle.compact",
        """
        include "Base.compact";
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        include "Middle.compact";
        circuit run(): Void {
          <caret>baseHelper();
        }
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("baseHelper should have reference at caret", ref);
    PsiElement target = ref.resolve();
    assertNotNull("baseHelper should resolve transitively from Base.compact", target);
    assertEquals("Base.compact", target.getContainingFile().getName());
  }

  public void testCircularIncludeResilience() {
    myFixture.addFileToProject(
        "A.compact",
        """
        include "B.compact";
        const A_VAL = 1;
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        include "A.compact";
        const B_VAL = 2;
        circuit test(): Void {
          const a = <caret>A_VAL;
        }
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("A_VAL should have reference at caret", ref);
    PsiElement target = ref.resolve();
    assertNotNull("Circular include should resolve without crashing", target);
    assertEquals("A.compact", target.getContainingFile().getName());
  }

  public void testMissingIncludeResilience() {
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        include "nonexistent.compact";
        const LOCAL = 10;
        circuit test(): Void {
          const x = <caret>LOCAL;
        }
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("LOCAL should have reference at caret", ref);
    PsiElement target = ref.resolve();
    assertNotNull("Missing include must not prevent local symbol resolution", target);
    assertEquals("LOCAL", ((CompactNamedElement) target).getName());
  }

  public void testIncludeReferenceNavigation() {
    myFixture.addFileToProject(
        "Target.compact",
        """
        const VAL = 42;
        """
    );
    PsiFile file = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        include "<caret>Target.compact";
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Include statement should have reference at caret", ref);
    assertInstanceOf(ref, CompactIncludeReference.class);
    PsiElement target = ref.resolve();
    assertNotNull("Include reference should resolve to Target.compact file", target);
    assertTrue(target instanceof CompactFile);
    assertEquals("Target.compact", ((CompactFile) target).getName());
  }

  public void testCrossFileModuleImport() {
    myFixture.addFileToProject(
        "Lib.compact",
        """
        module Math {
          export circuit square(x: Field): Field {
            return x * x;
          }
        }
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        include "Lib.compact";
        import { square } from Math;
        circuit run(): Void {
          <caret>square(5);
        }
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("square should have reference at caret", ref);
    PsiElement target = ref.resolve();
    assertNotNull("Imported square from cross-file module should resolve", target);
    if (target instanceof CompactImportElementImpl) {
      target = CompactResolveUtil.resolveImportElementSource((CompactImportElementImpl) target);
    }
    assertNotNull(target);
    assertEquals("Lib.compact", target.getContainingFile().getName());
  }

  public void testCrossFileTypeInference() {
    myFixture.addFileToProject(
        "Types.compact",
        """
        struct Point {
          x: Field,
          y: Field,
        }
        """
    );
    PsiFile mainFile = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        include "Types.compact";
        circuit test(p: Point): Void {
          const xVal = p.x;
        }
        """
    );

    CompactMemberExprImpl memberExpr = PsiTreeUtil.findChildOfType(mainFile, CompactMemberExprImpl.class);
    assertNotNull(memberExpr);
    assertEquals("Field", memberExpr.getType().name());
  }

  public void testImportedEnumAndEnumMemberResolution() {
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
    PsiFile gameFile = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { GameState } from './GameState';

        export circuit checkGame(): [] {
            assert(
                GameState.PLAYING == GameState.PLAYING,
                "Game is not currently playing"
            );
        }
        """
    );

    // 1. GameState in import resolves to enum declaration in GameState.compact
    CompactImportElementImpl importElement = PsiTreeUtil.findChildOfType(gameFile, CompactImportElementImpl.class);
    assertNotNull("import element GameState should exist", importElement);
    PsiReference importRef = importElement.getReference();
    assertNotNull("import element GameState should have a reference", importRef);
    PsiElement importTarget = importRef.resolve();
    assertNotNull("import element GameState should resolve to GameState.compact declaration", importTarget);
    assertTrue(importTarget instanceof CompactEnumDefinition);
    assertEquals("GameState", ((CompactEnumDefinition) importTarget).getName());
    assertEquals("GameState.compact", importTarget.getContainingFile().getName());

    // 2. Import path './GameState' resolves to GameState.compact file
    CompactImportDeclarationImpl importDecl = PsiTreeUtil.findChildOfType(gameFile, CompactImportDeclarationImpl.class);
    assertNotNull(importDecl);
    PsiReference pathRef = importDecl.getReference();
    assertNotNull(pathRef);
    PsiElement pathTarget = pathRef.resolve();
    assertNotNull("Import path './GameState' should resolve to GameState.compact file", pathTarget);
    assertTrue(pathTarget instanceof CompactFile);
    assertEquals("GameState.compact", ((CompactFile) pathTarget).getName());

    // 3. GameState in GameState.PLAYING resolves to the imported enum symbol
    CompactReferenceExprImpl baseRefExpr = PsiTreeUtil.findChildrenOfType(gameFile, CompactReferenceExprImpl.class).stream()
        .filter(e -> "GameState".equals(e.getText()))
        .findFirst()
        .orElse(null);
    assertNotNull(baseRefExpr);
    assertNotNull(baseRefExpr.getReference());
    PsiElement baseTarget = baseRefExpr.getReference().resolve();
    assertNotNull("GameState qualifier should resolve", baseTarget);
    if (baseTarget instanceof CompactImportElementImpl) {
      baseTarget = CompactResolveUtil.resolveImportElementSource((CompactImportElementImpl) baseTarget);
    }
    assertNotNull("Unwrapped base target should resolve to CompactEnumDefinition", baseTarget);
    assertTrue(baseTarget instanceof CompactEnumDefinition);
    assertEquals("GameState.compact", baseTarget.getContainingFile().getName());

    // 4. PLAYING in GameState.PLAYING resolves to CompactEnumMemberImpl in GameState.compact
    CompactMemberExprImpl memberExpr = PsiTreeUtil.findChildrenOfType(gameFile, CompactMemberExprImpl.class).stream()
        .findFirst()
        .orElse(null);
    assertNotNull("GameState.PLAYING member expr should exist", memberExpr);
    PsiReference memberRef = memberExpr.getReference();
    assertNotNull("GameState.PLAYING should have CompactEnumMemberReference", memberRef);
    assertInstanceOf(memberRef, dev.verloren.midnight.reference.CompactEnumMemberReference.class);
    PsiElement memberTarget = memberRef.resolve();
    assertNotNull("PLAYING should resolve to enum member in GameState.compact", memberTarget);
    assertTrue(memberTarget instanceof CompactEnumMemberImpl);
    assertEquals("PLAYING", ((CompactEnumMemberImpl) memberTarget).getName());
    assertEquals("GameState.compact", memberTarget.getContainingFile().getName());
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
    assertTrue(target instanceof CompactEnumMemberImpl);
    assertEquals("WAITING", ((CompactEnumMemberImpl) target).getName());
    assertEquals("GameState.compact", target.getContainingFile().getName());
  }
}
