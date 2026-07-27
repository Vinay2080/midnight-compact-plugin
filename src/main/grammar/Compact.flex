package dev.verloren.midnight.lexer;

import com.intellij.psi.tree.IElementType;

%%

%public
%class CompactLexer
%implements FlexLexer
%unicode
%final
%function advance
%type IElementType

%eof{
    return;
%eof}


WHITE_SPACE = [ \t\r\n]+

DIGIT = [0-9]
LETTER = [A-Za-z]
IDENTIFIER = ({LETTER}|[_$])({LETTER}|{DIGIT}|[_$])*

DECIMAL = 0|[1-9]{DIGIT}*
BINARY = 0[bB][01]+
OCTAL = 0[oO][0-7]+
HEX = 0[xX][0-9a-fA-F]+

VERSION = {DECIMAL}("."{DECIMAL})+

STRING_DOUBLE = \"([^\"\\\r\n]|\\.)*\"
STRING_SINGLE = \'([^\'\\\r\n]|\\.)*\'

LINE_COMMENT = "//".*
BLOCK_COMMENT = "/*"([^*]|\*+[^*/])*\*+"/"

%%

/* ========================================================= */
/* Whitespace                                                 */
/* ========================================================= */

{WHITE_SPACE} { return CompactTokenTypes.WHITE_SPACE; }

/* ========================================================= */
/* Comments                                                   */
/* ========================================================= */

{LINE_COMMENT}  { return CompactTokenTypes.LINE_COMMENT; }
{BLOCK_COMMENT} { return CompactTokenTypes.BLOCK_COMMENT; }



/* ========================================================= */
/* Keywords                                                   */
/* ========================================================= */

"export"       { return CompactTokenTypes.EXPORT; }
"from"         { return CompactTokenTypes.FROM; }
"import"       { return CompactTokenTypes.IMPORT; }
"module"       { return CompactTokenTypes.MODULE; }
"prefix"       { return CompactTokenTypes.PREFIX; }

"assert"       { return CompactTokenTypes.ASSERT; }
"as"           { return CompactTokenTypes.AS; }
"circuit"      { return CompactTokenTypes.CIRCUIT; }
"const"        { return CompactTokenTypes.CONST; }
"constructor"  { return CompactTokenTypes.CONSTRUCTOR; }
"contract"     { return CompactTokenTypes.CONTRACT; }
"default"      { return CompactTokenTypes.DEFAULT; }
"disclose"     { return CompactTokenTypes.DISCLOSE; }
"else"         { return CompactTokenTypes.ELSE; }
"enum"         { return CompactTokenTypes.ENUM; }
"fold"         { return CompactTokenTypes.FOLD; }
"for"          { return CompactTokenTypes.FOR; }
"if"           { return CompactTokenTypes.IF; }
"include"      { return CompactTokenTypes.INCLUDE; }
"ledger"       { return CompactTokenTypes.LEDGER; }
"map"          { return CompactTokenTypes.MAP; }
"new"          { return CompactTokenTypes.NEW; }
"of"           { return CompactTokenTypes.OF; }
"pad"          { return CompactTokenTypes.PAD; }
"pragma"       { return CompactTokenTypes.PRAGMA; }
"pure"         { return CompactTokenTypes.PURE; }
"return"       { return CompactTokenTypes.RETURN; }
"sealed"       { return CompactTokenTypes.SEALED; }
"slice"        { return CompactTokenTypes.SLICE; }
"struct"       { return CompactTokenTypes.STRUCT; }
"type"         { return CompactTokenTypes.TYPE; }
"witness"      { return CompactTokenTypes.WITNESS; }

/* ========================================================= */
/* Built-in Types                                             */
/* ========================================================= */

"Boolean" { return CompactTokenTypes.BOOLEAN_TYPE; }
"Bytes"   { return CompactTokenTypes.BYTES_TYPE; }
"Field"   { return CompactTokenTypes.FIELD_TYPE; }
"Opaque"  { return CompactTokenTypes.OPAQUE_TYPE; }
"Uint"    { return CompactTokenTypes.UINT_TYPE; }
"Vector"  { return CompactTokenTypes.VECTOR_TYPE; }

/* ========================================================= */
/* Boolean Literals                                           */
/* ========================================================= */

"true"  { return CompactTokenTypes.TRUE; }
"false" { return CompactTokenTypes.FALSE; }

/* ========================================================= */
/* Reserved Keywords                                          */
/* ========================================================= */

"await"|"break"|"case"|"catch"|"class"|
"continue"|"debugger"|"delete"|"do"|
"extends"|"finally"|"function"|
"implements"|"in"|"instanceof"|
"interface"|"let"|"null"|
"package"|"private"|"protected"|
"public"|"static"|"super"|
"switch"|"this"|"throw"|
"try"|"typeof"|"var"|
"void"|"while"|"with"|"yield"
{
    return CompactTokenTypes.RESERVED_KEYWORD;
}

/* ========================================================= */
/* Literals                                                   */
/* ========================================================= */

{STRING_DOUBLE} { return CompactTokenTypes.STRING_LITERAL; }
{STRING_SINGLE} { return CompactTokenTypes.STRING_LITERAL; }

{VERSION}  { return CompactTokenTypes.VERSION_LITERAL; }

{HEX}      { return CompactTokenTypes.HEX_LITERAL; }
{BINARY}   { return CompactTokenTypes.BINARY_LITERAL; }
{OCTAL}    { return CompactTokenTypes.OCTAL_LITERAL; }
{DECIMAL}  { return CompactTokenTypes.DECIMAL_LITERAL; }


/* ========================================================= */
/* Identifier                                                 */
/* ========================================================= */

{IDENTIFIER} { return CompactTokenTypes.IDENTIFIER; }

/* ========================================================= */
/* Operators (longest first)                                 */
/* ========================================================= */

"..."
    { return CompactTokenTypes.SPREAD; }

".."
    { return CompactTokenTypes.RANGE; }

"=>"
    { return CompactTokenTypes.ARROW; }

"+="
    { return CompactTokenTypes.PLUS_ASSIGN; }

"-="
    { return CompactTokenTypes.MINUS_ASSIGN; }

"=="
    { return CompactTokenTypes.EQEQ; }

"!="
    { return CompactTokenTypes.NEQ; }

"<="
    { return CompactTokenTypes.LTE; }

">="
    { return CompactTokenTypes.GTE; }

"&&"
    { return CompactTokenTypes.ANDAND; }

"||"
    { return CompactTokenTypes.OROR; }

"="
    { return CompactTokenTypes.ASSIGN; }

"+"
    { return CompactTokenTypes.PLUS; }

"-"
    { return CompactTokenTypes.MINUS; }

"*"
    { return CompactTokenTypes.STAR; }

"/"
    { return CompactTokenTypes.SLASH; }

"%"
    { return CompactTokenTypes.PERCENT; }

"<"
    { return CompactTokenTypes.LT; }

">"
    { return CompactTokenTypes.GT; }

"!"
    { return CompactTokenTypes.NOT; }

"."
    { return CompactTokenTypes.DOT; }

"?"
    { return CompactTokenTypes.QUESTION; }

":"
    { return CompactTokenTypes.COLON; }

/* ========================================================= */
/* Delimiters                                                 */
/* ========================================================= */

"(" { return CompactTokenTypes.LPAREN; }
")" { return CompactTokenTypes.RPAREN; }
"{" { return CompactTokenTypes.LBRACE; }
"}" { return CompactTokenTypes.RBRACE; }
"[" { return CompactTokenTypes.LBRACKET; }
"]" { return CompactTokenTypes.RBRACKET; }

"," { return CompactTokenTypes.COMMA; }
";" { return CompactTokenTypes.SEMICOLON; }
"#" { return CompactTokenTypes.HASH; }

/* ========================================================= */
/* Unknown                                                    */
/* ========================================================= */

\"([^\"\\\r\n]|\\.)* {
    return CompactTokenTypes.UNTERMINATED_STRING;
}

"/*"([^*]|\*+[^*/])* {
    return CompactTokenTypes.UNTERMINATED_BLOCK_COMMENT;
}

[^] { return CompactTokenTypes.BAD_CHARACTER; }