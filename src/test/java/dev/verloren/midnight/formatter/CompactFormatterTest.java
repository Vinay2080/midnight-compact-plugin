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

public class CompactFormatterTest extends CompactFormatterTestBase {

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
}

