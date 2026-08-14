package dev.verloren.midnight.lexer;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class CompactLexer extends LexerBase {

  private static final Map<String, IElementType> KEYWORDS = Map.ofEntries(
          Map.entry("pragma", CompactTokenTypes.PRAGMA),
          Map.entry("export", CompactTokenTypes.EXPORT),
          Map.entry("from", CompactTokenTypes.FROM),
          Map.entry("import", CompactTokenTypes.IMPORT),
          Map.entry("module", CompactTokenTypes.MODULE),
          Map.entry("prefix", CompactTokenTypes.PREFIX),
          Map.entry("assert", CompactTokenTypes.ASSERT),
          Map.entry("as", CompactTokenTypes.AS),
          Map.entry("circuit", CompactTokenTypes.CIRCUIT),
          Map.entry("const", CompactTokenTypes.CONST),
          Map.entry("constructor", CompactTokenTypes.CONSTRUCTOR),
          Map.entry("contract", CompactTokenTypes.CONTRACT),
          Map.entry("default", CompactTokenTypes.DEFAULT),
          Map.entry("disclose", CompactTokenTypes.DISCLOSE),
          Map.entry("else", CompactTokenTypes.ELSE),
          Map.entry("enum", CompactTokenTypes.ENUM),
          Map.entry("fold", CompactTokenTypes.FOLD),
          Map.entry("for", CompactTokenTypes.FOR),
          Map.entry("if", CompactTokenTypes.IF),
          Map.entry("implements", CompactTokenTypes.IMPLEMENTS),
          Map.entry("include", CompactTokenTypes.INCLUDE),
          Map.entry("ledger", CompactTokenTypes.LEDGER),
          Map.entry("map", CompactTokenTypes.MAP),
          Map.entry("new", CompactTokenTypes.NEW),
          Map.entry("of", CompactTokenTypes.OF),
          Map.entry("pad", CompactTokenTypes.PAD),
          Map.entry("pure", CompactTokenTypes.PURE),
          Map.entry("return", CompactTokenTypes.RETURN),
          Map.entry("sealed", CompactTokenTypes.SEALED),
          Map.entry("slice", CompactTokenTypes.SLICE),
          Map.entry("struct", CompactTokenTypes.STRUCT),
          Map.entry("type", CompactTokenTypes.TYPE),
          Map.entry("witness", CompactTokenTypes.WITNESS),
          Map.entry("emit", CompactTokenTypes.EMIT),
          Map.entry("external", CompactTokenTypes.EXTERNAL),
          Map.entry("let", CompactTokenTypes.LET),
          Map.entry("true", CompactTokenTypes.TRUE),
          Map.entry("false", CompactTokenTypes.FALSE)
  );
  private static final Map<String, IElementType> BUILTIN_TYPES = Map.ofEntries(
          Map.entry("Boolean", CompactTokenTypes.BOOLEAN_TYPE),
          Map.entry("Bytes", CompactTokenTypes.BYTES_TYPE),
          Map.entry("Field", CompactTokenTypes.FIELD_TYPE),
          Map.entry("Opaque", CompactTokenTypes.OPAQUE_TYPE),
          Map.entry("Uint", CompactTokenTypes.UINT_TYPE),
          Map.entry("Vector", CompactTokenTypes.VECTOR_TYPE),
          Map.entry("JubjubScalar", CompactTokenTypes.JUBJUB_SCALAR_TYPE),
          Map.entry("Secp256k1Base", CompactTokenTypes.SECP256K1_BASE_TYPE),
          Map.entry("Secp256k1Scalar", CompactTokenTypes.SECP256K1_SCALAR_TYPE)
  );
  private static final Map<String, IElementType> BOOLEAN_LITERALS = Map.ofEntries(
          Map.entry("true", CompactTokenTypes.TRUE),
          Map.entry("false", CompactTokenTypes.FALSE)
  );
  private CharSequence buffer = "";
  private int endOffset;
  private int position;
  private int tokenStart;
  private int tokenEnd;
  private IElementType tokenType;

  @Override
  public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
    this.buffer = buffer;
    this.endOffset = endOffset;
    this.position = startOffset;
    this.tokenStart = startOffset;
    this.tokenEnd = startOffset;
    this.tokenType = null;

    advance();
  }

  @Override
  public int getState() {
    return 0;
  }

  @Override
  public @Nullable IElementType getTokenType() {
    return tokenType;
  }

  @Override
  public int getTokenStart() {
    return tokenStart;
  }

  @Override
  public int getTokenEnd() {
    return tokenEnd;
  }

  @Override
  public void advance() {
    if (position >= endOffset) {
      tokenStart = endOffset;
      tokenEnd = endOffset;
      tokenType = null;
      return;
    }

    tokenStart = position;
    char ch = buffer.charAt(position);

    if (Character.isWhitespace(ch)) {
      lexWhitespace();
      return;
    }

    if (isIdentifierStart(ch)) {
      lexIdentifierOrKeyword();
      return;
    }

    if (Character.isDigit(ch)) {
      lexNumberOrVersion();
      return;
    }

    if (ch == '"' || ch == '\'') {
      lexString(ch);
      return;
    }
    switch (ch) {
      case ';':
        finishHelper(CompactTokenTypes.SEMICOLON);
        return;
      case '(':
        finishHelper(CompactTokenTypes.LPAREN);
        return;
      case ')':
        finishHelper(CompactTokenTypes.RPAREN);
        return;
      case ',':
        finishHelper(CompactTokenTypes.COMMA);
        return;
      case '[':
        finishHelper(CompactTokenTypes.LBRACKET);
        return;
      case ']':
        finishHelper(CompactTokenTypes.RBRACKET);
        return;
      case '{':
        finishHelper(CompactTokenTypes.LBRACE);
        return;
      case '}':
        finishHelper(CompactTokenTypes.RBRACE);
        return;
      case ':':
        finishHelper(CompactTokenTypes.COLON);
        return;
      case '?':
        finishHelper(CompactTokenTypes.QUESTION);
        return;
      case '%':
        finishHelper(CompactTokenTypes.PERCENT);
        return;
      case '*':
        finishHelper(CompactTokenTypes.STAR);
        return;
      case '#':
        finishHelper(CompactTokenTypes.HASH);
        return;
      case '/':
        lexComment();
        return;
      case '>':
      case '<':
      case '!':
      case '&':
      case '|':
      case '=':
      case '+':
      case '-':
      case '.':
        lexOperator();
        return;
      default:
        finishHelper(TokenType.BAD_CHARACTER);
    }

    finishHelper(CompactTokenTypes.BAD_CHARACTER);
  }

  @Override
  public @NotNull CharSequence getBufferSequence() {
    return buffer;
  }

  @Override
  public int getBufferEnd() {
    return endOffset;
  }

  private void lexWhitespace() {
    while (position < endOffset && Character.isWhitespace(buffer.charAt(position))) {
      position++;
    }
    finish(TokenType.WHITE_SPACE);
  }

  private static boolean isIdentifierStart(char ch) {
    return Character.isLetter(ch) || ch == '_' || ch == '$';
  }

  private void lexIdentifierOrKeyword() {
    do {
      position++;
    } while (position < endOffset && isIdentifierPart(buffer.charAt(position)));

    String text = buffer.subSequence(tokenStart, position).toString();

    IElementType token = KEYWORDS.get(text);

    if (token != null) {
      finish(token);
      return;
    }

    token = BUILTIN_TYPES.get(text);
    if (token != null) {
      finish(token);
      return;
    }
    token = BOOLEAN_LITERALS.get(text);
    if (token != null) {
      finish(token);
      return;
    }
    finish(CompactTokenTypes.IDENTIFIER);
  }

  private void lexNumberOrVersion() {

    // Hex: 0x...
    if (buffer.charAt(position) == '0'
            && position + 1 < endOffset
            && (buffer.charAt(position + 1) == 'x' || buffer.charAt(position + 1) == 'X')) {

      position += 2;

      while (position < endOffset && isHexDigit(buffer.charAt(position))) {
        position++;
      }

      finish(CompactTokenTypes.HEX_LITERAL);
      return;
    }

    // Binary: 0b...
    if (buffer.charAt(position) == '0'
            && position + 1 < endOffset
            && (buffer.charAt(position + 1) == 'b' || buffer.charAt(position + 1) == 'B')) {

      position += 2;

      while (position < endOffset
              && (buffer.charAt(position) == '0' || buffer.charAt(position) == '1')) {
        position++;
      }

      finish(CompactTokenTypes.BINARY_LITERAL);
      return;
    }

    // Octal: 0o... or 0O...
    if (buffer.charAt(position) == '0'
            && position + 1 < endOffset
            && (buffer.charAt(position + 1) == 'o' || buffer.charAt(position + 1) == 'O')) {

      position += 2;

      while (position < endOffset
              && (buffer.charAt(position) >= '0' && buffer.charAt(position) <= '7')) {
        position++;
      }

      finish(CompactTokenTypes.OCTAL_LITERAL);
      return;
    }

    // Consume decimal digits
    consumeDigits();

    boolean isVersion = false;

    while (position < endOffset && buffer.charAt(position) == '.') {
      if (position + 1 < endOffset && buffer.charAt(position + 1) == '.') {
        // Range operator '..' follows - stop parsing decimal/version number here!
        break;
      }
      isVersion = true;
      position++;

      if (position >= endOffset || !Character.isDigit(buffer.charAt(position))) {
        consumeMalformedVersionTail();
        finish(CompactTokenTypes.INVALID_VERSION);
        return;
      }

      consumeDigits();
    }

    if (position < endOffset && isIdentifierStart(buffer.charAt(position))) {
      consumeMalformedVersionTail();
      finish(CompactTokenTypes.INVALID_VERSION);
      return;
    }

    if (isVersion) {
      finish(CompactTokenTypes.VERSION_LITERAL);
      return;
    }

    finish(CompactTokenTypes.DECIMAL_LITERAL);
  }

  private void lexString(char quote) {
    position++; // consume opening quote

    while (position < endOffset) {
      char ch = buffer.charAt(position);

      // Handle escape sequences
      if (ch == '\\') {
        position++;

        // '\' at end of file -> unterminated string
        if (position >= endOffset) {
          finish(CompactTokenTypes.UNTERMINATED_STRING);
          return;
        }

        position++; // consume escaped character
        continue;
      }

      // Closing quote
      if (ch == quote) {
        position++;
        finish(CompactTokenTypes.STRING_LITERAL);
        return;
      }

      // Newline before closing quote -> invalid
      if (ch == '\n' || ch == '\r') {
        finish(CompactTokenTypes.UNTERMINATED_STRING);
        return;
      }

      position++;
    }

    // EOF before closing quote
    finish(CompactTokenTypes.UNTERMINATED_STRING);
  }

  private void finishHelper(IElementType tokenType) {
    position++;
    finish(tokenType);
  }

  private void lexComment() {
    position++; // consume '/'

    if (position >= endOffset) {
      finish(CompactTokenTypes.SLASH);
      return;
    }

    char ch = buffer.charAt(position);

    // Line comment: //
    if (ch == '/') {

      do {
        position++;
      } while (position < endOffset
              && buffer.charAt(position) != '\n'
              && buffer.charAt(position) != '\r');

      finish(CompactTokenTypes.LINE_COMMENT);
      return;
    }

    // Block comment: /* ... */
    if (ch == '*') {
      position++;

      while (position < endOffset) {

        // Nested block comments are invalid.
        if (buffer.charAt(position) == '/'
                && position + 1 < endOffset
                && buffer.charAt(position + 1) == '*') {

          finish(CompactTokenTypes.UNTERMINATED_BLOCK_COMMENT);
          return;
        }

        if (buffer.charAt(position) == '*'
                && position + 1 < endOffset
                && buffer.charAt(position + 1) == '/') {

          position += 2;
          finish(CompactTokenTypes.BLOCK_COMMENT);
          return;
        }

        position++;
      }

      // Unterminated block comment.
      finish(CompactTokenTypes.UNTERMINATED_BLOCK_COMMENT);
      return;
    }

    // Bare '/' is SLASH operator
    finish(CompactTokenTypes.SLASH);
  }

  private void lexOperator() {
    char ch = buffer.charAt(position);

    switch (ch) {
      case '>':
        position++;
        if (position < endOffset && buffer.charAt(position) == '=') {
          finishHelper(CompactTokenTypes.GTE);
        } else {
          finish(CompactTokenTypes.GT);
        }
        return;

      case '<':
        position++;
        if (position < endOffset && buffer.charAt(position) == '=') {
          finishHelper(CompactTokenTypes.LTE);
        } else {
          finish(CompactTokenTypes.LT);
        }
        return;

      case '!':
        position++;
        if (position < endOffset && buffer.charAt(position) == '=') {
          finishHelper(CompactTokenTypes.NEQ);
        } else {
          finish(CompactTokenTypes.NOT);
        }
        return;

      case '&':
        position++;
        if (position < endOffset && buffer.charAt(position) == '&') {
          finishHelper(CompactTokenTypes.ANDAND);
        } else {
          finish(TokenType.BAD_CHARACTER);
        }
        return;

      case '|':
        position++;
        if (position < endOffset && buffer.charAt(position) == '|') {
          finishHelper(CompactTokenTypes.OROR);
        } else {
          finish(TokenType.BAD_CHARACTER);
        }
        return;

      case '=':
        position++;

        if (position < endOffset && buffer.charAt(position) == '=') {
          finishHelper(CompactTokenTypes.EQEQ);
        } else if (position < endOffset && buffer.charAt(position) == '>') {
          finishHelper(CompactTokenTypes.ARROW);
        } else {
          finish(CompactTokenTypes.ASSIGN);
        }
        return;

      case '+':
        position++;
        if (position < endOffset && buffer.charAt(position) == '=') {
          finishHelper(CompactTokenTypes.PLUS_ASSIGN);
        } else {
          finish(CompactTokenTypes.PLUS);
        }
        return;

      case '-':
        position++;
        if (position < endOffset && buffer.charAt(position) == '=') {
          finishHelper(CompactTokenTypes.MINUS_ASSIGN);
        } else {
          finish(CompactTokenTypes.MINUS);
        }
        return;
      case '.':
        position++;
        if (position < endOffset && buffer.charAt(position) == '.') {
          if (position + 1 < endOffset && buffer.charAt(position + 1) == '.') {
            position += 2;
            finish(CompactTokenTypes.SPREAD);
          } else {
            finishHelper(CompactTokenTypes.RANGE);
          }
        } else {
          finish(CompactTokenTypes.DOT);
        }
        return;
      default:
        position++;
        finish(TokenType.BAD_CHARACTER);
    }
  }

  private void finish(IElementType tokenType) {
    tokenEnd = position;
    this.tokenType = tokenType;
  }

  private static boolean isIdentifierPart(char ch) {
    return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$';
  }

  private static boolean isHexDigit(char ch) {
    return Character.isDigit(ch)
            || (ch >= 'a' && ch <= 'f')
            || (ch >= 'A' && ch <= 'F');
  }

  private void consumeDigits() {
    while (position < endOffset && Character.isDigit(buffer.charAt(position))) {
      position++;
    }
  }

  private void consumeMalformedVersionTail() {
    while (position < endOffset) {
      char ch = buffer.charAt(position);
      if (!isIdentifierPart(ch) && ch != '.') {
        return;
      }
      position++;
    }
  }

}

