package dev.verloren.midnight.completion;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.verloren.midnight.lexer.CompactTokenSets;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context classifier determining the semantic grammatical role of the caret position during code completion.
 *
 * <p>Inspects preceding AST leaf tokens, containing PSI ancestor constructs, and line boundaries
 * to categorize the completion point into one of several mutually exclusive {@link Kind} classifications.
 * This classification drives context-aware item dispatching in {@link CompactCompletionContributor}.</p>
 *
 * <h3>IntelliJ Platform Lifecycle &amp; Threading Contract</h3>
 * <ul>
 *   <li><b>Subsystem Role:</b> Context classifier and AST query utility called by
 *       {@link CompactCompletionContributor} during the completion resolution phase.</li>
 *   <li><b>Calling Pipeline:</b> Invoked directly within
 *       {@link com.intellij.codeInsight.completion.CompletionProvider#addCompletions(com.intellij.codeInsight.completion.CompletionParameters, com.intellij.util.ProcessingContext, com.intellij.codeInsight.completion.CompletionResultSet)}.</li>
 *   <li><b>Threading Model:</b> Runs under an active <b>Read Action</b> ({@code @RequiresReadLock})
 *       on background pooled completion threads (IntelliJ Platform 2024+ asynchronous completion
 *       architecture) or on the EDT if completion is triggered synchronously.</li>
 *   <li><b>Cancellation Support:</b> Operates over lightweight AST / PSI traversals. Does not perform
 *       heavy index queries or unbounded external resolution; loops strictly terminate at statement,
 *       block, or file boundaries.</li>
 * </ul>
 *
 * <h3>Design Decisions &amp; Architectural Rationale</h3>
 * <ul>
 *   <li><b>Stateless Utility Class:</b> Declared {@code final} with a private constructor to prevent
 *       instantiation. All methods are pure static query functions operating over transient {@link PsiElement}
 *       nodes.</li>
 *   <li><b>Zero Static Cache / Memory Leak Guards:</b> Intentionally caches nothing in static or instance
 *       fields. PSI nodes, documents, and AST nodes are short-lived and discarded once classification finishes,
 *       avoiding cross-project or cross-editor memory leaks.</li>
 *   <li><b>Fault-Tolerant Tree Walking:</b> Gracefully handles incomplete syntax trees produced by active
 *       typing (e.g. unclosed parentheses, missing semicolons, dangling colons, and IntelliJ's dummy
 *       completion identifier {@code IntellijIdeaRulezzz}).</li>
 * </ul>
 *
 * <h3>Classification Decision Tree</h3>
 * <pre>{@code
 *   Caret PSI Element (position)
 *          │
 *          ├── Inside comment? (PsiComment / COMMENT tokens) ──────────────► Kind.NONE
 *          ├── Preceded by '.' (DOT token)? ────────────────────────────────► Kind.MEMBER
 *          ├── After 'pragma' keyword / form? ─────────────────────────────► Kind.AFTER_PRAGMA
 *          ├── Inside Bytes<...>/Uint<...> type size argument? ────────────► Kind.BYTES_SIZE / UINT_SIZE
 *          ├── Inside Type Reference / Intro (':', 'as', '<', '#', '=')? ──► Kind.TYPE
 *          ├── Preceded by 'export' (with sealed/pure/new)? ───────────────► Kind.AFTER_EXPORT / SEALED / PURE / NEW
 *          ├── Inside Struct or Enum definition body (non-type)? ──────────► Kind.NONE
 *          ├── Start of Statement in Block (after ';', '{', '}')? ─────────► Kind.STATEMENT
 *          ├── Start of Top-Level Declaration? ────────────────────────────► Kind.KEYWORD
 *          ├── Contained in CompactMemberExpr? ─────────────────────────────► Kind.MEMBER
 *          └── Default expression context ─────────────────────────────────► Kind.VALUE
 * }</pre>
 *
 * @see CompactCompletionContributor
 * @see dev.verloren.midnight.completion.providers.CompactKeywordCompletionProvider
 * @see dev.verloren.midnight.completion.providers.CompactTypeCompletionProvider
 * @see dev.verloren.midnight.completion.providers.CompactValueCompletionProvider
 * @see dev.verloren.midnight.completion.providers.CompactMemberAccessCompletionProvider
 */
public final class CompactCompletionContext {

  /**
   * Private constructor enforcing static utility usage.
   */
  private CompactCompletionContext() {
  }

  /**
   * Determines whether the specified PSI element or any of its ancestors represents a comment.
   *
   * @param position the PSI element at the caret position
   * @return {@code true} if the element is part of a line or block comment, {@code false} otherwise
   */
  public static boolean isComment(@NotNull PsiElement position) {
    if (position instanceof PsiComment) {
      return true;
    }
    if (PsiTreeUtil.getParentOfType(position, PsiComment.class, false) != null) {
      return true;
    }
    return position.getNode() != null && CompactTokenSets.COMMENTS.contains(position.getNode().getElementType());
  }

  /**
   * Walks backward through visible PSI leaves starting from the given element, skipping any whitespace
   * and comment tokens to find the nearest preceding semantic syntax leaf.
   *
   * @param position the starting PSI element
   * @return the previous non-comment, non-whitespace leaf element, or {@code null} if none exists
   */
  public static @Nullable PsiElement prevNonCommentLeaf(@NotNull PsiElement position) {
    for (PsiElement p = PsiTreeUtil.prevVisibleLeaf(position); p != null; p = PsiTreeUtil.prevVisibleLeaf(p)) {
      if (!isComment(p)) {
        return p;
      }
    }
    return null;
  }

  /**
   * Classifies the semantic completion context of the caret position.
   *
   * <p>Evaluation follows a strict precedence pipeline to prevent false positives in incomplete ASTs:
   * <ol>
   *   <li><b>Comment Guard:</b> If the cursor is inside a comment, yields {@link Kind#NONE}.</li>
   *   <li><b>Member Access:</b> If preceded by {@code '.'}, yields {@link Kind#MEMBER}.</li>
   *   <li><b>Pragma Form:</b> If immediately following {@code pragma}, yields {@link Kind#AFTER_PRAGMA}.</li>
   *   <li><b>Parameterized Type Sizes:</b> If inside {@code Bytes<...>} or {@code Uint<...>}, yields size kinds.</li>
   *   <li><b>Type Reference Positions:</b> If inside a type reference, after {@code :}, {@code as}, {@code <},
   *       or {@code =} in a type alias, yields {@link Kind#TYPE}.</li>
   *   <li><b>Modifier Chaining:</b> If preceded by {@code export}, {@code sealed}, {@code pure}, or {@code new},
   *       yields corresponding modifier continuation kinds.</li>
   *   <li><b>Struct/Enum Interior Guard:</b> If inside struct/enum body not covered by field types, yields {@link Kind#NONE}.</li>
   *   <li><b>Statement vs Top-Level:</b> Distinguishes between statement start inside {@link CompactBlock}
   *       ({@link Kind#STATEMENT}) vs top-level declaration start ({@link Kind#KEYWORD}).</li>
   *   <li><b>Member Expression Fallback:</b> Checks {@link CompactMemberExprImpl} ancestry.</li>
   *   <li><b>Default Value:</b> Falls back to general expression value completion ({@link Kind#VALUE}).</li>
   * </ol>
   *
   * @param position the leaf PSI element at the active cursor
   * @return the classified {@link Kind} representing the grammatical role
   */
  public static @NotNull Kind classify(@NotNull PsiElement position) {
    // Step 1: Suppress code completion inside single-line or multi-line comments
    if (isComment(position)) {
      return Kind.NONE;
    }

    // Step 2: Member access takes immediate priority if preceded by a dot token '.'
    PsiElement previous = prevNonCommentLeaf(position);
    if (previous != null && previous.getNode() != null && previous.getNode().getElementType() == CompactTokenTypes.DOT) {
      return Kind.MEMBER;
    }

    // Step 3: Check for pragma compiler version directive context
    if (isAfterPragma(position, previous)) {
      return Kind.AFTER_PRAGMA;
    }

    // Step 4: Parameterized type size bounds (e.g. Bytes<32> or Uint<64>)
    Kind sizeKind = CompactCompletionTypeClassifier.checkParameterizedTypeSizeContext(previous);
    if (sizeKind != null) {
      return sizeKind;
    }

    // Step 5: Type reference constructs and type introducers (':', 'as', '<', '#')
    if (PsiTreeUtil.getParentOfType(position, CompactTypeReferenceImpl.class, false) != null) {
      return Kind.TYPE;
    }
    if (CompactCompletionTypeClassifier.isAfterTypeIntro(previous)) {
      return Kind.TYPE;
    }
    if (CompactCompletionTypeClassifier.isTypePosition(position, previous)) {
      return Kind.TYPE;
    }

    // Step 6: Export modifier sequences (export sealed, export pure, etc.)
    if (isExportPreceding(position)) {
      if (previous != null && previous.getNode() != null) {
        IElementType prevType = previous.getNode().getElementType();
        if (prevType == CompactTokenTypes.SEALED) {
          return Kind.AFTER_SEALED;
        }
        if (prevType == CompactTokenTypes.PURE) {
          return Kind.AFTER_PURE;
        }
        if (prevType == CompactTokenTypes.NEW) {
          return Kind.AFTER_NEW;
        }
      }
      return Kind.AFTER_EXPORT;
    }

    // Step 7: Suppress declaration/value completions inside raw struct or enum definitions
    if (PsiTreeUtil.getParentOfType(position, CompactStructDefinition.class, false) != null
        || PsiTreeUtil.getParentOfType(position, CompactEnumDefinition.class, false) != null) {
      return Kind.NONE;
    }

    // Step 8: Distinguish between block statements and top-level declarations
    if (isDeclarationOrStatementStart(previous)) {
      if (PsiTreeUtil.getParentOfType(position, CompactBlock.class, false) != null) {
        return Kind.STATEMENT;
      }
      if (previous != null && previous.getNode() != null) {
        IElementType prevType = previous.getNode().getElementType();
        if (prevType == CompactTokenTypes.EXPORT) {
          return Kind.AFTER_EXPORT;
        }
        if (prevType == CompactTokenTypes.SEALED) {
          return Kind.AFTER_SEALED;
        }
        if (prevType == CompactTokenTypes.PURE) {
          return Kind.AFTER_PURE;
        }
        if (prevType == CompactTokenTypes.NEW) {
          return Kind.AFTER_NEW;
        }
      }
      return Kind.KEYWORD;
    }

    // Step 9: Fallback for member expressions where caret leaf is inside member expression PSI
    if (PsiTreeUtil.getParentOfType(position, CompactMemberExprImpl.class, false) != null) {
      return Kind.MEMBER;
    }

    // Step 10: Default to general value / expression operand context
    return Kind.VALUE;
  }

  /**
   * Checks whether the completion position directly follows a {@code pragma} token or sits within
   * an uncompleted {@link CompactPragmaForm}.
   *
   * @param position the current caret element
   * @param previous the previous non-comment leaf element
   * @return {@code true} if compiler version suggestions should be offered
   */
  public static boolean isAfterPragma(@NotNull PsiElement position, @Nullable PsiElement previous) {
    if (previous != null && previous.getNode() != null && previous.getNode().getElementType() == CompactTokenTypes.PRAGMA) {
      return true;
    }
    CompactPragmaForm pragma = PsiTreeUtil.getParentOfType(position, CompactPragmaForm.class, false);
    if (pragma != null) {
      PsiElement id = pragma.getPragmaIdentifier();
      return id == null || id == position || PsiTreeUtil.isAncestor(id, position, false);
    }
    return false;
  }

  /**
   * Determines whether the cursor is preceded by an {@code export} modifier on the current line or statement.
   *
   * @param position the current caret element
   * @return {@code true} if {@code export} precedes the cursor, {@code false} otherwise
   */
  public static boolean isExportPreceding(@NotNull PsiElement position) {
    return CompactCompletionExportClassifier.isExportPreceding(position);
  }

  /**
   * Fallback line scanner testing whether the text preceding {@code offset} on the current line
   * consists of an {@code export} statement prefix.
   *
   * @param chars     document character sequence
   * @param lineStart start offset of the current line
   * @param offset    caret offset within the document
   * @return {@code true} if the line prefix matches export patterns
   */
  public static boolean hasPrecedingExportOnLine(@NotNull CharSequence chars, int lineStart, int offset) {
    return CompactCompletionExportClassifier.hasPrecedingExportOnLine(chars, lineStart, offset);
  }

  /**
   * Checks whether the preceding leaf indicates the start of a declaration or statement
   * (e.g. after a semicolon, block boundary, or modifier keyword).
   *
   * @param previous nearest preceding non-comment leaf
   * @return {@code true} if preceding token begins a statement/declaration boundary
   */
  private static boolean isDeclarationOrStatementStart(PsiElement previous) {
    if (previous == null) {
      return true;
    }
    if (previous.getNode() == null) {
      return false;
    }
    IElementType type = previous.getNode().getElementType();
    return type == CompactTokenTypes.SEMICOLON
            || type == CompactTokenTypes.LBRACE
            || type == CompactTokenTypes.RBRACE
            || type == CompactTokenTypes.EXPORT
            || type == CompactTokenTypes.SEALED
            || type == CompactTokenTypes.PURE
            || type == CompactTokenTypes.NEW;
  }

  /**
   * Enumeration of distinct syntactic completion targets in Compact files.
   */
  public enum Kind {
    /** Top-level declaration keyword position (e.g. {@code circuit}, {@code contract}, {@code ledger}). */
    KEYWORD,
    /** Statement position within an executable function/circuit block (e.g. {@code const}, {@code if}, {@code return}). */
    STATEMENT,
    /** Immediately following an {@code export} keyword; allows exported declaration keywords and modifiers. */
    AFTER_EXPORT,
    /** Following an {@code export sealed} sequence. */
    AFTER_SEALED,
    /** Following an {@code export pure} sequence. */
    AFTER_PURE,
    /** Following an {@code export new} sequence. */
    AFTER_NEW,
    /** Immediately following a {@code pragma} directive keyword; offers compiler version pragmas. */
    AFTER_PRAGMA,
    /** Position expecting a type identifier or primitive type name (after {@code :}, {@code as}, {@code <}, {@code =}). */
    TYPE,
    /** Inside a {@code Bytes<...>} size parameter; suggests valid byte size integers (e.g. {@code 32}, {@code 64}). */
    BYTES_SIZE,
    /** Inside a {@code Uint<...>} bit-width parameter; suggests valid integer bit sizes (e.g. {@code 8}, {@code 64}). */
    UINT_SIZE,
    /** General expression operand position; suggests local variables, circuits, constructors, literals, and keywords. */
    VALUE,
    /** Following a dot {@code '.'} operator; suggests struct fields, contract state, or module members. */
    MEMBER,
    /** Position where code completion is suppressed (e.g. comments, struct body delimiters). */
    NONE
  }
}
