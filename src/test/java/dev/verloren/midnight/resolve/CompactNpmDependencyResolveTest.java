package dev.verloren.midnight.resolve;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.inspection.CompactUnresolvedReferenceInspection;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.psi.*;
import dev.verloren.midnight.resolve.npm.CompactNpmSymbolElement;

import java.util.List;

/**
 * Test suite for external npm dependency resolution, package.json inspection,
 * declaration parsing, and symbol navigation in the Compact language plugin.
 */
public class CompactNpmDependencyResolveTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
  }

  private void setupVitestPackage() {
    myFixture.addFileToProject(
        "node_modules/vitest/package.json",
        """
        {
          "name": "vitest",
          "version": "4.1.11",
          "main": "./dist/index.js",
          "types": "./dist/index.d.ts",
          "exports": {
            ".": {
              "types": "./dist/index.d.ts",
              "import": "./dist/index.js"
            }
          }
        }
        """
    );
    myFixture.addFileToProject(
        "node_modules/vitest/dist/index.d.ts",
        """
        export declare function describe(name: string, fn: Function): void;
        export declare function it(name: string, fn: Function): void;
        export declare function test(name: string, fn: Function): void;
        export declare function expect(actual: any): any;
        export declare function beforeEach(fn: Function): void;
        export declare function afterEach(fn: Function): void;
        export declare const suite: any;
        export interface TestSuite {
          name: string;
        }
        """
    );
  }

  private void setupCompactRuntimePackage() {
    myFixture.addFileToProject(
        "node_modules/@midnight-ntwrk/compact-runtime/package.json",
        """
        {
          "name": "@midnight-ntwrk/compact-runtime",
          "version": "0.16.0",
          "main": "./dist/index.js",
          "types": "./dist/index.d.ts",
          "exports": {
            ".": {
              "types": "./dist/index.d.ts",
              "import": "./dist/index.js"
            }
          }
        }
        """
    );
    myFixture.addFileToProject(
        "node_modules/@midnight-ntwrk/compact-runtime/dist/index.d.ts",
        """
        export * from './types';
        export declare function convertFieldToBytes(f: any): any;
        export declare function persistentHash(data: any): any;
        export declare function transientHash(data: any): any;
        """
    );
    myFixture.addFileToProject(
        "node_modules/@midnight-ntwrk/compact-runtime/dist/types.d.ts",
        """
        export declare class CompactTypeBytes {
          size: number;
        }
        export declare class CompactTypeVector {
          length: number;
        }
        export interface CircuitContext<T = any> {
          state: T;
        }
        export type ContractState = any;
        """
    );
  }

  // =========================================================================
  // 1. Valid local Compact import
  // =========================================================================
  public void testValidLocalCompactImport() {
    myFixture.addFileToProject(
        "helper.compact",
        """
        export circuit helperFunc(): Void {}
        """
    );
    PsiFile file = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { helperFunc } from "./helper.compact";
        circuit test(): Void {
          helperFunc();
        }
        """
    );

    CompactImportElementImpl importElement = PsiTreeUtil.findChildOfType(file, CompactImportElementImpl.class);
    assertNotNull("Import element helperFunc should exist", importElement);
    PsiReference ref = importElement.getReference();
    assertNotNull("Import element should have reference", ref);
    PsiElement target = ref.resolve();
    assertNotNull("helperFunc should resolve to local declaration in helper.compact", target);
    assertTrue(target instanceof CompactCircuitDefinition);
    assertEquals("helperFunc", ((CompactCircuitDefinition) target).getName());
    assertEquals("helper.compact", target.getContainingFile().getName());

    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    List<HighlightInfo> highlights = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved"))
        .toList();
    assertTrue("Valid local import should have zero unresolved warnings: " + highlights, highlights.isEmpty());
  }

  // =========================================================================
  // 2. Invalid local Compact import
  // =========================================================================
  public void testInvalidLocalCompactImport() {
    myFixture.addFileToProject(
        "helper.compact",
        """
        export circuit helperFunc(): Void {}
        """
    );
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { nonexistentFunc } from "./helper.compact";
        """
    );

    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    List<HighlightInfo> highlights = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved imported symbol 'nonexistentFunc'"))
        .toList();
    assertEquals("Invalid local import symbol should produce unresolved diagnostic", 1, highlights.size());
  }

  // =========================================================================
  // 3. Valid regular npm package import (vitest)
  // =========================================================================
  public void testValidRegularNpmPackageImport() {
    setupVitestPackage();

    PsiFile file = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import {
            beforeEach,
            describe,
            expect,
            it
        } from "vitest";

        circuit test(): Void {
          describe();
          it();
          expect();
          beforeEach();
        }
        """
    );

    // Verify all 4 imported elements resolve
    var importElements = PsiTreeUtil.findChildrenOfType(file, CompactImportElementImpl.class);
    assertEquals("Should have 4 imported elements", 4, importElements.size());

    for (CompactImportElementImpl element : importElements) {
      PsiReference ref = element.getReference();
      assertNotNull("Import element " + element.getName() + " should have reference", ref);
      PsiElement target = ref.resolve();
      assertNotNull("Imported symbol " + element.getName() + " from vitest should resolve", target);
      assertTrue("Resolved target should be CompactNpmSymbolElement", target instanceof CompactNpmSymbolElement);
      assertEquals(element.getName(), ((CompactNpmSymbolElement) target).getName());
      assertEquals("vitest", ((CompactNpmSymbolElement) target).getPackageName());
    }

    // Verify zero inspection warnings
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    List<HighlightInfo> highlights = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved"))
        .toList();
    assertTrue("Valid vitest imports should have zero unresolved warnings: " + highlights, highlights.isEmpty());
  }

  // =========================================================================
  // 4. Invalid symbol from vitest
  // =========================================================================
  public void testInvalidSymbolFromVitest() {
    setupVitestPackage();

    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { describe, THIS_SYMBOL_DOES_NOT_EXIST } from "vitest";
        """
    );

    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    List<HighlightInfo> highlights = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved imported symbol"))
        .toList();
    assertEquals("Should report exactly 1 unresolved diagnostic for nonexistent symbol", 1, highlights.size());
    assertTrue("Diagnostic must mention THIS_SYMBOL_DOES_NOT_EXIST",
        highlights.getFirst().getDescription().contains("THIS_SYMBOL_DOES_NOT_EXIST"));
  }

  // =========================================================================
  // 5. Valid scoped package import (@midnight-ntwrk/compact-runtime)
  // =========================================================================
  public void testValidScopedPackageImport() {
    setupCompactRuntimePackage();

    PsiFile file = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import {
            CompactTypeBytes,
            CompactTypeVector,
            convertFieldToBytes,
            persistentHash
        } from "@midnight-ntwrk/compact-runtime";

        circuit test(b: CompactTypeBytes): Void {
          convertFieldToBytes();
          persistentHash();
        }
        """
    );

    var importElements = PsiTreeUtil.findChildrenOfType(file, CompactImportElementImpl.class);
    assertEquals("Should have 4 imported elements", 4, importElements.size());

    for (CompactImportElementImpl element : importElements) {
      PsiReference ref = element.getReference();
      assertNotNull("Import element " + element.getName() + " should have reference", ref);
      PsiElement target = ref.resolve();
      assertNotNull("Imported symbol " + element.getName() + " from @midnight-ntwrk/compact-runtime should resolve", target);
      assertTrue("Resolved target should be CompactNpmSymbolElement", target instanceof CompactNpmSymbolElement);
      assertEquals(element.getName(), ((CompactNpmSymbolElement) target).getName());
      assertEquals("@midnight-ntwrk/compact-runtime", ((CompactNpmSymbolElement) target).getPackageName());
    }

    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    List<HighlightInfo> highlights = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved"))
        .toList();
    assertTrue("Valid scoped runtime imports should have zero unresolved warnings: " + highlights, highlights.isEmpty());
  }

  // =========================================================================
  // 6. Invalid symbol from scoped package
  // =========================================================================
  public void testInvalidSymbolFromScopedPackage() {
    setupCompactRuntimePackage();

    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { CompactTypeBytes, NonExistentRuntimeSymbol } from "@midnight-ntwrk/compact-runtime";
        """
    );

    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    List<HighlightInfo> highlights = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved imported symbol"))
        .toList();
    assertEquals("Should report 1 unresolved imported symbol error for NonExistentRuntimeSymbol", 1, highlights.size());
    assertTrue("Diagnostic must mention NonExistentRuntimeSymbol",
        highlights.getFirst().getDescription().contains("NonExistentRuntimeSymbol"));
  }

  // =========================================================================
  // 7. Package not installed
  // =========================================================================
  public void testPackageNotInstalled() {
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { someFunction } from "uninstalled-package";
        """
    );

    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    List<HighlightInfo> highlights = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved imported symbol 'someFunction'"))
        .toList();
    assertEquals("Should report unresolved imported symbol when package is not installed", 1, highlights.size());
  }

  // =========================================================================
  // 8. Multiple imported symbols in single import statement
  // =========================================================================
  public void testMultipleImportedSymbols() {
    setupVitestPackage();

    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import {
            describe,
            BAD_SYMBOL_1,
            it,
            BAD_SYMBOL_2,
            expect
        } from "vitest";
        """
    );

    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    List<HighlightInfo> highlights = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved imported symbol"))
        .toList();
    assertEquals("Should report exactly 2 unresolved errors for BAD_SYMBOL_1 and BAD_SYMBOL_2", 2, highlights.size());
  }

  // =========================================================================
  // 9. Relative import vs external package import in same file
  // =========================================================================
  public void testRelativeImportVsExternalPackageImport() {
    setupVitestPackage();
    myFixture.addFileToProject(
        "localModule.compact",
        """
        export circuit localHelper(): Void {}
        """
    );

    PsiFile file = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { localHelper } from "./localModule.compact";
        import { describe } from "vitest";

        circuit run(): Void {
          localHelper();
          describe();
        }
        """
    );

    // Verify local helper import resolves to CompactCircuitDefinition in localModule.compact
    CompactImportElementImpl localImportElem = PsiTreeUtil.findChildrenOfType(file, CompactImportElementImpl.class).stream()
        .filter(e -> "localHelper".equals(e.getName()))
        .findFirst()
        .orElse(null);
    assertNotNull("localHelper import element should exist", localImportElem);
    assertNotNull(localImportElem.getReference());
    PsiElement localTarget = localImportElem.getReference().resolve();
    assertNotNull("localHelper reference should resolve", localTarget);
    assertTrue("localHelper must resolve to CompactCircuitDefinition", localTarget instanceof CompactCircuitDefinition);
    assertEquals("localModule.compact", localTarget.getContainingFile().getName());

    // Verify describe import resolves to CompactNpmSymbolElement in vitest
    CompactImportElementImpl describeImportElem = PsiTreeUtil.findChildrenOfType(file, CompactImportElementImpl.class).stream()
        .filter(e -> "describe".equals(e.getName()))
        .findFirst()
        .orElse(null);
    assertNotNull("describe import element should exist", describeImportElem);
    assertNotNull(describeImportElem.getReference());
    PsiElement describeTarget = describeImportElem.getReference().resolve();
    assertNotNull("describe reference should resolve", describeTarget);
    assertTrue("describe must resolve to CompactNpmSymbolElement", describeTarget instanceof CompactNpmSymbolElement);
    assertEquals("vitest", ((CompactNpmSymbolElement) describeTarget).getPackageName());

    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    List<HighlightInfo> highlights = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved"))
        .toList();
    assertTrue("Both relative and external package imports must resolve with zero warnings: " + highlights, highlights.isEmpty());
  }

  // =========================================================================
  // 10. Resolution after editing imported symbol (real-time responsiveness)
  // =========================================================================
  public void testResolutionAfterEditingImportedSymbol() {
    setupVitestPackage();

    // 1. Initially valid code
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { describe } from "vitest";
        circuit test(): Void {
          describe();
        }
        """
    );
    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    List<HighlightInfo> initial = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved"))
        .toList();
    assertTrue("Initial valid code should produce zero unresolved diagnostics", initial.isEmpty());

    // 2. Edit to invalid symbol
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { THIS_SYMBOL_DOES_NOT_EXIST } from "vitest";
        circuit test(): Void {
          THIS_SYMBOL_DOES_NOT_EXIST();
        }
        """
    );
    List<HighlightInfo> modified = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved"))
        .toList();
    assertFalse("Modified invalid symbol should produce unresolved diagnostics", modified.isEmpty());

    // 3. Edit back to valid symbol
    myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { describe } from "vitest";
        circuit test(): Void {
          describe();
        }
        """
    );
    List<HighlightInfo> reverted = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved"))
        .toList();
    assertTrue("Reverting to valid symbol should remove unresolved diagnostics", reverted.isEmpty());
  }

  // =========================================================================
  // 11. Navigation on package path string
  // =========================================================================
  public void testPackagePathNavigation() {
    setupVitestPackage();

    PsiFile file = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { describe } from "<caret>vitest";
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Package path string should have reference at caret", ref);
    PsiElement target = ref.resolve();
    assertNotNull("Package path reference should resolve to vitest entry file", target);
    assertTrue(target instanceof PsiFile);
    assertEquals("index.d.ts", ((PsiFile) target).getName());
  }

  // =========================================================================
  // 12. Navigation on imported symbol to .d.ts declaration offset
  // =========================================================================
  public void testImportedSymbolNavigation() {
    setupVitestPackage();

    PsiFile file = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { <caret>describe } from "vitest";
        """
    );

    PsiReference ref = myFixture.getReferenceAtCaretPosition();
    assertNotNull("Imported symbol should have reference at caret", ref);
    PsiElement target = ref.resolve();
    assertNotNull("Imported symbol should resolve to CompactNpmSymbolElement", target);
    assertTrue(target instanceof CompactNpmSymbolElement);
    assertEquals("describe", ((CompactNpmSymbolElement) target).getName());
    assertEquals("index.d.ts", target.getContainingFile().getName());
  }

  // =========================================================================
  // 13. Ambient module declaration parsing in .d.ts
  // =========================================================================
  public void testAmbientModuleDeclarationParsing() {
    myFixture.addFileToProject(
        "node_modules/custom-ambient-lib/package.json",
        """
        {
          "name": "custom-ambient-lib",
          "version": "1.0.0",
          "main": "./index.js"
        }
        """
    );
    myFixture.addFileToProject(
        "node_modules/custom-ambient-lib/ambient.d.ts",
        """
        declare module 'custom-ambient-lib' {
          export function customAmbientFunc(): void;
          export const customAmbientConst: number;
        }
        """
    );

    PsiFile file = myFixture.configureByText(
        CompactFileType.INSTANCE,
        """
        import { customAmbientFunc, customAmbientConst } from "custom-ambient-lib";
        circuit test(): Void {
          customAmbientFunc();
          const x = customAmbientConst;
        }
        """
    );

    myFixture.enableInspections(CompactUnresolvedReferenceInspection.class);
    List<HighlightInfo> highlights = myFixture.doHighlighting().stream()
        .filter(h -> h.getDescription() != null && h.getDescription().contains("Unresolved"))
        .toList();
    assertTrue("Ambient module declared symbols should resolve with zero warnings: " + highlights, highlights.isEmpty());
  }
}
