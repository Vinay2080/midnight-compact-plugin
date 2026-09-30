package dev.verloren.midnight.formatter;

/**
 * Advanced formatting tests: comments preservation, error tolerance, idempotence,
 * special expressions, and full contracts.
 */
public class CompactFormatterAdvancedTest extends CompactFormatterTestBase {

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

  public void testIdempotenceComplex() {
    doIdempotenceTest(
        """
        export { vote$commit, vote$reveal, advance, set_topic, add_voter };
        
        import CompactStandardLibrary;
        
        enum PublicState { setup, commit, reveal, final, }
        
        circuit ballot_repr(ballot: PermissibleVotes): Bytes<32> {
          return ballot == PermissibleVotes.yes ? pad(32, "yes") : pad(32, "no");
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
}
