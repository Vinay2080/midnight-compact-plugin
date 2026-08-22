package dev.verloren.midnight.formatter;

import com.intellij.lang.LanguageFormatting;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import org.jetbrains.annotations.NotNull;

public class CompactFormatterTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
    LanguageFormatting.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactFormattingModelBuilder()
    );
    com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions indentOptions =
        com.intellij.application.options.CodeStyle.getSettings(getProject()).getIndentOptions(CompactFileType.INSTANCE);
    if (indentOptions != null) {
      indentOptions.INDENT_SIZE = 2;
      indentOptions.TAB_SIZE = 2;
      indentOptions.CONTINUATION_INDENT_SIZE = 2;
      indentOptions.USE_TAB_CHARACTER = false;
    }
  }

  private void doFormatTest(@NotNull String input, @NotNull String expected) {
    myFixture.configureByText(CompactFileType.INSTANCE, input);
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile());
    });
    assertEquals(expected, myFixture.getFile().getText());
  }

  private void doIdempotenceTest(@NotNull String input) {
    myFixture.configureByText(CompactFileType.INSTANCE, input);
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile());
    });
    String formattedOnce = myFixture.getFile().getText();
    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile());
    });
    String formattedTwice = myFixture.getFile().getText();
    assertEquals(formattedOnce, formattedTwice);
  }

  // =========================================================================
  // Category A: Basic Formatting
  // =========================================================================

  public void testFormatImportWhitespace() {
    doFormatTest(
        "import                                                  CompactStandardLibrary;\n",
        "import CompactStandardLibrary;\n"
    );
  }

  public void testFormatPragmaWhitespace() {
    doFormatTest(
        "pragma language_version   >=                                                                                          0.15                ;\n",
        "pragma language_version >= 0.15;\n"
    );
  }

  public void testFormatIncludeDeclaration() {
    doFormatTest(
        "include      \"std.compact\"   ;\n",
        "include \"std.compact\";\n"
    );
  }

  public void testFormatLedgerDeclaration() {
    doFormatTest(
        "export   ledger   round   :   Counter  ;\n",
        "export ledger round: Counter;\n"
    );
  }

  public void testFormatWitnessDeclaration() {
    doFormatTest(
        "witness   private$secret_key ( ) :   Bytes < 32 > ;\n",
        "witness private$secret_key(): Bytes<32>;\n"
    );
  }

  public void testFormatTypeAlias() {
    doFormatTest(
        "export   new   type   Amount  =   Uint < 64 > ;\n",
        "export new type Amount = Uint<64>;\n"
    );
  }

  public void testFormatExportForm() {
    doFormatTest(
        "export { Maybe };\n",
        "export { Maybe };\n"
    );
  }

  public void testFormatImplementsDeclaration() {
    doFormatTest(
        "contract   implements   Token ;\n",
        "contract implements Token;\n"
    );
  }

  public void testFormatMultipleTopLevelDeclarations() {
    doFormatTest(
        """
        import CompactStandardLibrary;
        
        
        
        export ledger counter: Counter;
        
        
        
        export circuit get(): Field {
        return 1;
        }
        """,
        """
        import CompactStandardLibrary;
        
        export ledger counter: Counter;
        
        export circuit get(): Field {
          return 1;
        }
        """
    );
  }

  // =========================================================================
  // Category B: Nested Blocks & Control Flow
  // =========================================================================

  public void testFormatNestedIf() {
    doFormatTest(
        """
        circuit test(): [] {
        if(p > z) {
        assert(1 > 2, "err");
        if(p > z) {
        assert(1 > 2, "err");
        }
        }
        }
        """,
        """
        circuit test(): [] {
          if (p > z) {
            assert(1 > 2, "err");
            if (p > z) {
              assert(1 > 2, "err");
            }
          }
        }
        """
    );
  }

  public void testFormatNestedFor() {
    doFormatTest(
        """
        circuit test(): [] {
        for(const i of 1..10) {
        assert(1 > 2, "err");
        for(const j of 1..5) {
        assert(3 > 4, "err2");
        }
        }
        }
        """,
        """
        circuit test(): [] {
          for (const i of 1..10) {
            assert(1 > 2, "err");
            for (const j of 1..5) {
              assert(3 > 4, "err2");
            }
          }
        }
        """
    );
  }

  public void testFormatIfElseChain() {
    doFormatTest(
        """
        circuit successor(state: PublicState): PublicState {
        if(state == PublicState.setup) {
        return PublicState.commit;
        } else if(state == PublicState.commit) {
        return PublicState.reveal;
        } else {
        return PublicState.final;
        }
        }
        """,
        """
        circuit successor(state: PublicState): PublicState {
          if (state == PublicState.setup) {
            return PublicState.commit;
          } else if (state == PublicState.commit) {
            return PublicState.reveal;
          } else {
            return PublicState.final;
          }
        }
        """
    );
  }

  public void testFormatContractBody() {
    doFormatTest(
        """
        export contract Token {
        pure circuit balance(owner: Field): Field;
        circuit transfer(to: Field, amount: Uint<64>): Boolean;
        }
        """,
        """
        export contract Token {
          pure circuit balance(owner: Field): Field;
          circuit transfer(to: Field, amount: Uint<64>): Boolean;
        }
        """
    );
  }

  public void testFormatModuleBody() {
    doFormatTest(
        """
        export module Math {
        export circuit add(a: Field, b: Field): Field {
        return a + b;
        }
        }
        """,
        """
        export module Math {
          export circuit add(a: Field, b: Field): Field {
            return a + b;
          }
        }
        """
    );
  }

  // =========================================================================
  // Category C: Declarations & Signatures
  // =========================================================================

  public void testFormatStructFields() {
    doFormatTest(
        """
        struct Val {
        x: Field,
        y: Boolean,
        f: Field,
        }
        """,
        """
        struct Val {
          x: Field,
          y: Boolean,
          f: Field,
        }
        """
    );
  }

  public void testFormatEnumMembers() {
    doFormatTest(
        """
        enum PublicState {
        setup,
        commit,
        reveal,
        final,
        }
        """,
        """
        enum PublicState {
          setup,
          commit,
          reveal,
          final,
        }
        """
    );
  }

  public void testFormatConstBindings() {
    doFormatTest(
        """
        constructor(x: Field, y: Field, z: Field) {
        const p = x + y + z;
        const a = 1, b = 2;
        }
        """,
        """
        constructor(x: Field, y: Field, z: Field) {
          const p = x + y + z;
          const a = 1, b = 2;
        }
        """
    );
  }

  public void testFormatCircuitSignature() {
    doFormatTest(
        "export pure circuit mint<T>(to: Field, amount: Uint<64>): Boolean {\nreturn true;\n}\n",
        """
        export pure circuit mint<T>(to: Field, amount: Uint<64>): Boolean {
          return true;
        }
        """
    );
  }

  // =========================================================================
  // Category D: Expressions
  // =========================================================================

  public void testFormatBinaryOperators() {
    doFormatTest(
        """
        circuit test(): [] {
        const a = 1+2*3-4/2%1;
        const b = a==b&&c!=d||e<=f&&g>=h;
        }
        """,
        """
        circuit test(): [] {
          const a = 1 + 2 * 3 - 4 / 2 % 1;
          const b = a == b && c != d || e <= f && g >= h;
        }
        """
    );
  }

  public void testFormatCallAndMemberAccess() {
    doFormatTest(
        """
        circuit test(): [] {
        round.increment(1);
        c.decrement(amount);
        const y = p.a.a.a + p.b.b.b;
        }
        """,
        """
        circuit test(): [] {
          round.increment(1);
          c.decrement(amount);
          const y = p.a.a.a + p.b.b.b;
        }
        """
    );
  }

  public void testFormatCastExpression() {
    doFormatTest(
        """
        circuit test(): [] {
        const f = default<Uint<32>> as Field;
        }
        """,
        """
        circuit test(): [] {
          const f = default<Uint<32>> as Field;
        }
        """
    );
  }

  public void testFormatTernaryExpression() {
    doFormatTest(
        """
        circuit test(ballot: PermissibleVotes): Bytes<32> {
        return ballot == PermissibleVotes.yes ? pad(32, "yes") : pad(32, "no");
        }
        """,
        """
        circuit test(ballot: PermissibleVotes): Bytes<32> {
          return ballot == PermissibleVotes.yes ? pad(32, "yes") : pad(32, "no");
        }
        """
    );
  }

  public void testFormatTupleExpression() {
    doFormatTest(
        """
        circuit test(): [] {
        const p = [1, 2, 3,];
        const g = [bob(1, 2,), bob(1, 2,),];
        }
        """,
        """
        circuit test(): [] {
          const p = [1, 2, 3,];
          const g = [bob(1, 2,), bob(1, 2,),];
        }
        """
    );
  }

  public void testFormatStructLiteral() {
    doFormatTest(
        """
        circuit test(): [] {
        const f = mariusz { 1, 2, 3, };
        }
        """,
        """
        circuit test(): [] {
          const f = mariusz { 1, 2, 3, };
        }
        """
    );
  }

  public void testFormatGenericGenericsSpacing() {
    doFormatTest(
        """
        circuit test(): [] {
        const g = default<Vector<1, Boolean>>;
        }
        """,
        """
        circuit test(): [] {
          const g = default<Vector<1, Boolean>>;
        }
        """
    );
  }

  // =========================================================================
  // Category E: Comments Preservation
  // =========================================================================

  public void testFormatPreservesLineComments() {
    doFormatTest(
        """
        // Header comment
        // Line 2
        
        import CompactStandardLibrary;
        
        // Before circuit
        circuit test(): [] {
          // Inside circuit
          const x = 1;
        }
        """,
        """
        // Header comment
        // Line 2
        
        import CompactStandardLibrary;
        
        // Before circuit
        circuit test(): [] {
          // Inside circuit
          const x = 1;
        }
        """
    );
  }

  public void testFormatPreservesBlockComments() {
    doFormatTest(
        """
        /* Block comment
           multiline */
        import CompactStandardLibrary;
        
        circuit test(): [] {
          /* inside */
          const x = 1;
        }
        """,
        """
        /* Block comment
           multiline */
        import CompactStandardLibrary;
        
        circuit test(): [] {
          /* inside */
          const x = 1;
        }
        """
    );
  }

  // =========================================================================
  // Category F: Incomplete and Malformed Code
  // =========================================================================

  public void testFormatIncompleteMissingClosingBrace() {
    doFormatTest(
        """
        circuit test(): [] {
        const x = 1;
        """,
        """
        circuit test(): [] {
          const x = 1;
        """
    );
  }

  public void testFormatIncompleteMissingClosingParen() {
    doFormatTest(
        """
        circuit test(): [] {
        if (x > 1 {
        return 1;
        }
        }
        """,
        """
        circuit test(): [] {
          if (x > 1 {
            return 1;
          }
        }
        """
    );
  }

  public void testFormatIncompleteMissingSemicolon() {
    doFormatTest(
        """
        circuit test(): [] {
        const x = 1
        const y = 2;
        }
        """,
        """
        circuit test(): [] {
          const x = 1
          const y = 2;
        }
        """
    );
  }

  public void testFormatEmptyFile() {
    doFormatTest("", "");
  }

  public void testFormatOnlyComments() {
    doFormatTest(
        "// Just a comment\n",
        "// Just a comment\n"
    );
  }

  // =========================================================================
  // Category G: Idempotence Tests
  // =========================================================================

  public void testIdempotenceSimple() {
    doIdempotenceTest(
        """
        import CompactStandardLibrary;
        
        export ledger round: Counter;
        
        export circuit increment(): [] {
          round.increment(1);
        }
        """
    );
  }

  // =========================================================================
  // Category H: Special Expressions (disclose, emit, map, fold, slice, lambda)
  // =========================================================================

  public void testFormatDiscloseAndEmit() {
    doFormatTest(
        """
        circuit test(v: Field): [] {
        const x = disclose(v);
        emit(x);
        }
        """,
        """
        circuit test(v: Field): [] {
          const x = disclose(v);
          emit(x);
        }
        """
    );
  }

  public void testFormatMapFoldSlice() {
    doFormatTest(
        """
        circuit test(): [] {
        const s = slice<32>(a, 0, 10);
        const m = map(f, arr);
        const r = fold(g, 0, arr);
        }
        """,
        """
        circuit test(): [] {
          const s = slice<32>(a, 0, 10);
          const m = map(f, arr);
          const r = fold(g, 0, arr);
        }
        """
    );
  }

  public void testFormatDestructuredPattern() {
    doFormatTest(
        """
        circuit test(): [] {
        const [a, b, c] = [1, 2, 3];
        const { x, y: z } = point;
        }
        """,
        """
        circuit test(): [] {
          const [a, b, c] = [1, 2, 3];
          const { x, y: z } = point;
        }
        """
    );
  }

  // =========================================================================
  // Category I: Official Contracts & Examples Regression
  // =========================================================================

  public void testFormatCounterContract() {
    doFormatTest(
        """
        import   CompactStandardLibrary;
        
        export ledger   round: Counter;
        
        export circuit increment():   [] {
        round.increment(1);
        }
        """,
        """
        import CompactStandardLibrary;
        
        export ledger round: Counter;
        
        export circuit increment(): [] {
          round.increment(1);
        }
        """
    );
  }

  public void testFormatTinyContract() {
    doFormatTest(
        """
        import CompactStandardLibrary;
        
        enum STATE { unset, set }
        
        ledger authority: Bytes<32>;
        export ledger value: Field;
        ledger state: STATE;
        
        constructor(v: Field) {
        const sk = private$secret_key();
        authority = public_key(sk);
        value = disclose(v);
        state = STATE.set;
        }
        
        witness private$secret_key(): Bytes<32>;
        
        circuit in_state(s: STATE): Boolean {
        return state == s;
        }
        
        circuit get(): Maybe<Field> {
        return in_state(STATE.set) ? some<Field>(value) : none<Field>();
        }
        """,
        """
        import CompactStandardLibrary;
        
        enum STATE { unset, set }
        
        ledger authority: Bytes<32>;
        export ledger value: Field;
        ledger state: STATE;
        
        constructor(v: Field) {
          const sk = private$secret_key();
          authority = public_key(sk);
          value = disclose(v);
          state = STATE.set;
        }
        
        witness private$secret_key(): Bytes<32>;
        
        circuit in_state(s: STATE): Boolean {
          return state == s;
        }
        
        circuit get(): Maybe<Field> {
          return in_state(STATE.set) ? some<Field>(value) : none<Field>();
        }
        """
    );
  }

  public void testFormatElectionContract() {
    doFormatTest(
        """
        export { vote$commit, vote$reveal, advance, set_topic, add_voter };
        
        import CompactStandardLibrary;
        
        enum PublicState { setup, commit, reveal, final, }
        
        circuit ballot_repr(ballot: PermissibleVotes): Bytes<32> {
        return ballot == PermissibleVotes.yes ? pad(32, "yes") : pad(32, "no");
        }
        
        circuit commitment_nullifier(sk: Bytes<32>): Bytes<32> {
        return disclose(persistentHash<Vector<2, Bytes<32>>>([pad(32, "lares:election:cm-nul:"), sk]));
        }
        """,
        """
        export { vote$commit, vote$reveal, advance, set_topic, add_voter };
        
        import CompactStandardLibrary;
        
        enum PublicState { setup, commit, reveal, final, }
        
        circuit ballot_repr(ballot: PermissibleVotes): Bytes<32> {
          return ballot == PermissibleVotes.yes ? pad(32, "yes") : pad(32, "no");
        }
        
        circuit commitment_nullifier(sk: Bytes<32>): Bytes<32> {
          return disclose(persistentHash<Vector<2, Bytes<32>>>([pad(32, "lares:election:cm-nul:"), sk]));
        }
        """
    );
  }

  // =========================================================================
  // Category J: Import Declarations Formatting
  // =========================================================================

  public void testFormatSingleLineImportMultipleSymbols() {
    doFormatTest(
        "import { Foo, Bar } from \"module\";\n",
        "import { Foo, Bar } from \"module\";\n"
    );
  }

  public void testFormatSingleLineImportUnformatted() {
    doFormatTest(
        "import   {   Foo  ,   Bar   }   from   \"module\"   ;\n",
        "import { Foo, Bar } from \"module\";\n"
    );
  }

  public void testFormatSingleLineImportSingleSymbol() {
    doFormatTest(
        "import { Foo } from \"module\";\n",
        "import { Foo } from \"module\";\n"
    );
  }

  public void testFormatSingleLineImportWithAliases() {
    doFormatTest(
        "import { Foo as MyFoo, Bar as MyBar } from \"module\";\n",
        "import { Foo as MyFoo, Bar as MyBar } from \"module\";\n"
    );
  }

  public void testFormatMultilineImportMultipleSymbols() {
    doFormatTest(
        """
        import {
        CompactTypeBytes,
        CompactTypeVector,
        convertFieldToBytes,
        persistentHash
        } from '@midnight-ntwrk/compact-runtime';
        """,
        """
        import {
          CompactTypeBytes,
          CompactTypeVector,
          convertFieldToBytes,
          persistentHash
        } from '@midnight-ntwrk/compact-runtime';
        """
    );
  }

  public void testFormatMultilineImportSingleSymbol() {
    doFormatTest(
        """
        import {
        Foo
        } from "module";
        """,
        """
        import {
          Foo
        } from "module";
        """
    );
  }

  public void testFormatMultilineImportPackage() {
    doFormatTest(
        """
        import {
        Foo,
        Bar
        } from "vitest";
        """,
        """
        import {
          Foo,
          Bar
        } from "vitest";
        """
    );
  }

  public void testFormatMultilineImportRelative() {
    doFormatTest(
        """
        import {
        Foo,
        Bar
        } from "./file.compact";
        """,
        """
        import {
          Foo,
          Bar
        } from "./file.compact";
        """
    );
  }

  public void testFormatMultilineImportWithAliases() {
    doFormatTest(
        """
        import {
        Foo as MyFoo,
        Bar as MyBar
        } from "module";
        """,
        """
        import {
          Foo as MyFoo,
          Bar as MyBar
        } from "module";
        """
    );
  }

  public void testFormatMultilineImportTrailingComma() {
    doFormatTest(
        """
        import {
        Foo,
        Bar,
        } from "module";
        """,
        """
        import {
          Foo,
          Bar,
        } from "module";
        """
    );
  }

  public void testFormatMultilineImportFollowedByOtherDeclarations() {
    doFormatTest(
        """
        import {
        Foo,
        Bar
        } from "./file.compact";
        
        export ledger round: Counter;
        
        export circuit get(): Field {
        return 1;
        }
        """,
        """
        import {
          Foo,
          Bar
        } from "./file.compact";
        
        export ledger round: Counter;
        
        export circuit get(): Field {
          return 1;
        }
        """
    );
  }

  public void testFormatMultilineImportInsideModule() {
    doFormatTest(
        """
        export module Math {
        import {
        Foo,
        Bar
        } from "module";
        
        export circuit add(a: Field, b: Field): Field {
        return a + b;
        }
        }
        """,
        """
        export module Math {
          import {
            Foo,
            Bar
          } from "module";
        
          export circuit add(a: Field, b: Field): Field {
            return a + b;
          }
        }
        """
    );
  }

  public void testFormatMultilineImportIdempotence() {
    doIdempotenceTest(
        """
        import {
          Foo,
          Bar
        } from "vitest";
        """
    );
  }
}
