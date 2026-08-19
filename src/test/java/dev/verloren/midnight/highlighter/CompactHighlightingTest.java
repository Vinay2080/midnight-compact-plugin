package dev.verloren.midnight.highlighter;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;

import java.util.List;

/**
 * Comprehensive automated test suite for Compact semantic and syntax highlighting.
 */
public class CompactHighlightingTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactParserDefinition()
    );
    LanguageAnnotators.INSTANCE.addExplicitExtension(
        CompactLanguage.INSTANCE,
        new CompactHighlightingAnnotator()
    );
  }

  // =========================================================================
  // 1. Declarations Highlighting Tests
  // =========================================================================

  public void testDeclarationHighlighting() {
    String code = """
        pragma language_version ^0.20.0;

        /// Documentation for contract
        export contract TokenContract {
            pure circuit balance(owner: Field): Field;
        }

        export ledger totalPlayers: Uint<8>;
        export const MAX_CAPACITY: Field = 100;

        constructor() {}

        witness generateSalt(): Bytes<32>;

        export circuit registerPlayer(id: Field): [] {
            const localLimit = MAX_CAPACITY;
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "TokenContract", CompactHighlighterColors.CONTRACT_DECLARATION);
    assertHasHighlight(highlights, "totalPlayers", CompactHighlighterColors.LEDGER_DECLARATION);
    assertHasHighlight(highlights, "MAX_CAPACITY", CompactHighlighterColors.CONSTANT_DECLARATION);
    assertHasHighlight(highlights, "generateSalt", CompactHighlighterColors.WITNESS_DECLARATION);
    assertHasHighlight(highlights, "registerPlayer", CompactHighlighterColors.CIRCUIT_DECLARATION);
    assertHasHighlight(highlights, "id", CompactHighlighterColors.PARAMETER_DECLARATION);
    assertHasHighlight(highlights, "localLimit", CompactHighlighterColors.LOCAL_VARIABLE_DECLARATION);
    assertHasHighlight(highlights, "language_version", CompactHighlighterColors.PRAGMA);
    assertHasHighlight(highlights, "20.0", CompactHighlighterColors.VERSION);
  }

  public void testTypesAndGenericsDeclarationHighlighting() {
    String code = """
        export enum Status {
            Pending,
            Active,
            Completed
        }

        export struct Container<T> {
            item: T,
            tag: Field
        }

        export type AccountId = Field;
        export module MathUtils {}
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "Status", CompactHighlighterColors.ENUM_DECLARATION);
    assertHasHighlight(highlights, "Pending", CompactHighlighterColors.ENUM_MEMBER_DECLARATION);
    assertHasHighlight(highlights, "Active", CompactHighlighterColors.ENUM_MEMBER_DECLARATION);
    assertHasHighlight(highlights, "Completed", CompactHighlighterColors.ENUM_MEMBER_DECLARATION);
    assertHasHighlight(highlights, "Container", CompactHighlighterColors.STRUCT_DECLARATION);
    assertHasHighlight(highlights, "T", CompactHighlighterColors.TYPE_PARAMETER);
    assertHasHighlight(highlights, "item", CompactHighlighterColors.FIELD_DECLARATION);
    assertHasHighlight(highlights, "tag", CompactHighlighterColors.FIELD_DECLARATION);
    assertHasHighlight(highlights, "AccountId", CompactHighlighterColors.TYPE_ALIAS_DECLARATION);
    assertHasHighlight(highlights, "MathUtils", CompactHighlighterColors.MODULE_DECLARATION);
  }

  // =========================================================================
  // 2. Calls & Usages Highlighting Tests
  // =========================================================================

  public void testCallsAndUsagesHighlighting() {
    String code = """
        export enum Mode { Fast, Safe }
        export struct Point { x: Field, y: Field }
        export const DEFAULT_VAL = 42;

        witness fetchEntropy(): Field;
        circuit compute(p: Point, m: Mode): Field {
            const secret = fetchEntropy();
            const val = DEFAULT_VAL;
            const xCoord = p.x;
            const isFast = m == Mode.Fast;
            assert(isFast, "Must be fast");
            return secret + val + xCoord;
        }

        circuit caller(): [] {
            const res = compute(Point { x: 1, y: 2 }, Mode.Safe);
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "fetchEntropy", CompactHighlighterColors.WITNESS_CALL);
    assertHasHighlight(highlights, "DEFAULT_VAL", CompactHighlighterColors.CONSTANT_USAGE);
    assertHasHighlight(highlights, "p", CompactHighlighterColors.PARAMETER_USAGE);
    assertHasHighlight(highlights, "x", CompactHighlighterColors.FIELD_ACCESS);
    assertHasHighlight(highlights, "Mode", CompactHighlighterColors.ENUM_DECLARATION);
    assertHasHighlight(highlights, "Fast", CompactHighlighterColors.ENUM_MEMBER_ACCESS);
    assertHasHighlight(highlights, "Safe", CompactHighlighterColors.ENUM_MEMBER_ACCESS);
    assertHasHighlight(highlights, "assert", CompactHighlighterColors.BUILTIN_FUNCTION);
    assertHasHighlight(highlights, "compute", CompactHighlighterColors.CIRCUIT_CALL);
  }

  // =========================================================================
  // 3. String Escapes & Comments Highlighting Tests
  // =========================================================================

  public void testStringEscapesHighlighting() {
    String code = """
        circuit test(): [] {
            const validStr = "Hello\\nWorld\\t\\x41\\u{1F600}";
            const invalidStr = "Bad escape: \\q";
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "\\n", CompactHighlighterColors.VALID_STRING_ESCAPE);
    assertHasHighlight(highlights, "\\t", CompactHighlighterColors.VALID_STRING_ESCAPE);
    assertHasHighlight(highlights, "\\x41", CompactHighlighterColors.VALID_STRING_ESCAPE);
    assertHasHighlight(highlights, "\\u{1F600}", CompactHighlighterColors.VALID_STRING_ESCAPE);
    assertHasHighlight(highlights, "\\q", CompactHighlighterColors.INVALID_STRING_ESCAPE);
  }

  public void testDocCommentHighlighting() {
    String code = """
        /// This is a doc comment
        circuit docTest(): [] {}
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "/// This is a doc comment", CompactHighlighterColors.DOC_COMMENT);
  }

  // =========================================================================
  // 4. Cross-File Reference Highlighting Tests
  // =========================================================================

  public void testCrossFileHighlighting() {
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
    String code = """
        import { GameState } from './GameState';

        export circuit checkGame(): [] {
            assert(
                GameState.PLAYING == GameState.PLAYING,
                "Game is not currently playing"
            );
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "GameState", CompactHighlighterColors.IMPORTED_SYMBOL);
    assertHasHighlight(highlights, "GameState", CompactHighlighterColors.ENUM_DECLARATION);
    assertHasHighlight(highlights, "PLAYING", CompactHighlighterColors.ENUM_MEMBER_ACCESS);
    assertHasHighlight(highlights, "assert", CompactHighlighterColors.BUILTIN_FUNCTION);
  }

  // =========================================================================
  // 5. Semantic Scenarios Highlighting Tests
  // =========================================================================

  public void testDiscloseAndLocalVariableHighlighting() {
    String code = """
        export circuit shoot(x: Field): [] {
            const currentShot = disclose(x);
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "export", CompactHighlighterColors.MODIFIER);
    assertHasHighlight(highlights, "shoot", CompactHighlighterColors.CIRCUIT_DECLARATION);
    assertHasHighlight(highlights, "x", CompactHighlighterColors.PARAMETER_DECLARATION);
    assertHasHighlight(highlights, "currentShot", CompactHighlighterColors.LOCAL_VARIABLE_DECLARATION);
    assertHasHighlight(highlights, "disclose", CompactHighlighterColors.BUILTIN_FUNCTION);
    assertHasHighlight(highlights, "x", CompactHighlighterColors.PARAMETER_USAGE);
  }

  public void testAssertAndEnumMemberHighlighting() {
    String code = """
        export enum BoardState { UNSET, SET }
        export circuit check(board2State: BoardState): [] {
            assert(board2State == BoardState.SET, "Board not set");
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "BoardState", CompactHighlighterColors.ENUM_DECLARATION);
    assertHasHighlight(highlights, "board2State", CompactHighlighterColors.PARAMETER_DECLARATION);
    assertHasHighlight(highlights, "assert", CompactHighlighterColors.BUILTIN_FUNCTION);
    assertHasHighlight(highlights, "board2State", CompactHighlighterColors.PARAMETER_USAGE);
    assertHasHighlight(highlights, "BoardState", CompactHighlighterColors.ENUM_DECLARATION);
    assertHasHighlight(highlights, "SET", CompactHighlighterColors.ENUM_MEMBER_ACCESS);
  }

  public void testWriteAccessHighlighting() {
    String code = """
        export ledger totalPlayers: Uint<8>;
        export circuit update(count: Uint<8>): [] {
            totalPlayers = count;
            const temp = count;
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "totalPlayers", CompactHighlighterColors.LEDGER_DECLARATION);
    assertHasHighlight(highlights, "totalPlayers", CompactHighlighterColors.LEDGER_WRITE);
    assertHasHighlight(highlights, "count", CompactHighlighterColors.PARAMETER_USAGE);
    assertHasHighlight(highlights, "temp", CompactHighlighterColors.LOCAL_VARIABLE_DECLARATION);
  }

  public void testStructLiteralAndFieldHighlighting() {
    String code = """
        export struct Point { x: Field, y: Field }
        export circuit origin(): Point {
            return Point { x: 0, y: 0 };
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "Point", CompactHighlighterColors.STRUCT_DECLARATION);
    assertHasHighlight(highlights, "x", CompactHighlighterColors.FIELD_DECLARATION);
    assertHasHighlight(highlights, "y", CompactHighlighterColors.FIELD_DECLARATION);
  }

  public void testStandardLibraryAndBuiltinTypesHighlighting() {
    String code = """
        export ledger userCount: Counter;
        export ledger userSet: Set<Field>;
        export ledger userMap: Map<Field, Uint<64>>;
        export circuit hashData(data: Bytes<32>): Field {
            return transientHash(data);
        }
        """;
    myFixture.configureByText(CompactFileType.INSTANCE, code);
    List<HighlightInfo> highlights = myFixture.doHighlighting();

    assertHasHighlight(highlights, "Counter", CompactHighlighterColors.BUILTIN_TYPE);
    assertHasHighlight(highlights, "Set", CompactHighlighterColors.BUILTIN_TYPE);
    assertHasHighlight(highlights, "Map", CompactHighlighterColors.BUILTIN_TYPE);
    assertHasHighlight(highlights, "transientHash", CompactHighlighterColors.BUILTIN_FUNCTION);
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private static void assertHasHighlight(List<HighlightInfo> highlights, String text, TextAttributesKey expectedKey) {
    boolean found = highlights.stream().anyMatch(h ->
        text.equals(h.getText()) && expectedKey.equals(h.forcedTextAttributesKey)
    );
    assertTrue("Expected text '" + text + "' to be highlighted with " + expectedKey.getExternalName()
        + ", but was not. All highlights: " + highlights.stream().map(h -> "'" + h.getText() + "':" + (h.forcedTextAttributesKey != null ? h.forcedTextAttributesKey.getExternalName() : "null")).toList(), found);
  }
}
