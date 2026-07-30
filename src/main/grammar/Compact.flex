package dev.verloren.midnight.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.psi.CompactTypes;

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

"export"       { return CompactTypes.EXPORT; }
"from"         { return CompactTypes.FROM; }
"import"       { return CompactTypes.IMPORT; }
"module"       { return CompactTypes.MODULE; }
"prefix"       { return CompactTypes.PREFIX; }

"assert"       { return CompactTypes.ASSERT; }
"as"           { return CompactTypes.AS; }
"circuit"      { return CompactTypes.CIRCUIT; }
"const"        { return CompactTypes.CONST; }
"constructor"  { return CompactTypes.CONSTRUCTOR; }
"contract"     { return CompactTypes.CONTRACT; }
"default"      { return CompactTypes.DEFAULT; }
"disclose"     { return CompactTypes.DISCLOSE; }
"else"         { return CompactTypes.ELSE; }
"enum"         { return CompactTypes.ENUM; }
"external"     { return CompactTypes.EXTERNAL; }
"fold"         { return CompactTypes.FOLD; }
"for"          { return CompactTypes.FOR; }
"if"           { return CompactTypes.IF; }
"implements"   { return CompactTypes.IMPLEMENTS; }
"include"      { return CompactTypes.INCLUDE; }
"ledger"       { return CompactTypes.LEDGER; }
"map"          { return CompactTypes.MAP; }
"new"          { return CompactTypes.NEW; }
"of"           { return CompactTypes.OF; }
"pad"          { return CompactTypes.PAD; }
"pragma"       { return CompactTypes.PRAGMA; }
"pure"         { return CompactTypes.PURE; }
"return"       { return CompactTypes.RETURN; }
"sealed"       { return CompactTypes.SEALED; }
"slice"        { return CompactTypes.SLICE; }
"struct"       { return CompactTypes.STRUCT; }
"type"         { return CompactTypes.TYPE; }
"witness"      { return CompactTypes.WITNESS; }
"emit"         { return CompactTypes.EMIT; }

/* ========================================================= */
/* Built-in Types                                             */
/* ========================================================= */

"Boolean"         { return CompactTypes.BOOLEAN_TYPE; }
"Bytes"           { return CompactTypes.BYTES_TYPE; }
"Field"           { return CompactTypes.FIELD_TYPE; }
"Opaque"          { return CompactTypes.OPAQUE_TYPE; }
"Uint"            { return CompactTypes.UINT_TYPE; }
"Vector"          { return CompactTypes.VECTOR_TYPE; }
"JubjubScalar"    { return CompactTypes.JUBJUB_SCALAR_TYPE; }
"Secp256k1Base"   { return CompactTypes.SECP256K1_BASE_TYPE; }
"Secp256k1Scalar" { return CompactTypes.SECP256K1_SCALAR_TYPE; }

/* ========================================================= */
/* Boolean Literals                                           */
/* ========================================================= */

"true"  { return CompactTypes.TRUE; }
"false" { return CompactTypes.FALSE; }

/* ========================================================= */
/* Reserved Keywords                                          */
/* ========================================================= */

"await"        { return CompactTypes.AWAIT; }
"break"        { return CompactTypes.BREAK; }
"case"         { return CompactTypes.CASE; }
"catch"        { return CompactTypes.CATCH; }
"class"        { return CompactTypes.CLASS; }
"continue"     { return CompactTypes.CONTINUE; }
"debugger"     { return CompactTypes.DEBUGGER; }
"delete"       { return CompactTypes.DELETE; }
"do"           { return CompactTypes.DO; }
"extends"      { return CompactTypes.EXTENDS; }
"finally"      { return CompactTypes.FINALLY; }
"function"     { return CompactTypes.FUNCTION; }
"interface"    { return CompactTypes.INTERFACE; }
"let"          { return CompactTypes.LET; }
"null"         { return CompactTypes.NULL; }
"package"      { return CompactTypes.PACKAGE; }
"private"      { return CompactTypes.PRIVATE; }
"protected"    { return CompactTypes.PROTECTED; }
"public"       { return CompactTypes.PUBLIC; }
"static"       { return CompactTypes.STATIC; }
"super"        { return CompactTypes.SUPER; }
"switch"       { return CompactTypes.SWITCH; }
"this"         { return CompactTypes.THIS; }
"throw"        { return CompactTypes.THROW; }
"try"          { return CompactTypes.TRY; }
"typeof"       { return CompactTypes.TYPEOF; }
"var"          { return CompactTypes.VAR; }
"void"         { return CompactTypes.VOID; }
"while"        { return CompactTypes.WHILE; }
"with"         { return CompactTypes.WITH; }
"yield"        { return CompactTypes.YIELD; }
"argument"     { return CompactTypes.ARGUMENT; }
"eval"         { return CompactTypes.EVAL; }
"event"        { return CompactTypes.EVENT; }
"in"           { return CompactTypes.IN; }
"instanceof"   { return CompactTypes.INSTANCEOF; }
/* ========================================================= */
/* Literals                                                   */
/* ========================================================= */

{STRING_DOUBLE} { return CompactTypes.STRING_LITERAL; }
{STRING_SINGLE} { return CompactTypes.STRING_LITERAL; }

{VERSION}  { return CompactTypes.VERSION_LITERAL; }

{HEX}      { return CompactTypes.HEX_LITERAL; }
{BINARY}   { return CompactTypes.BINARY_LITERAL; }
{OCTAL}    { return CompactTypes.OCTAL_LITERAL; }
{DECIMAL}  { return CompactTypes.DECIMAL_LITERAL; }


/* ========================================================= */
/* Identifier                                                 */
/* ========================================================= */

{IDENTIFIER} { return CompactTypes.IDENTIFIER; }

/* ========================================================= */
/* Operators (longest first)                                 */
/* ========================================================= */

"..."
    { return CompactTypes.SPREAD; }

".."
    { return CompactTypes.RANGE; }

"=>"
    { return CompactTypes.ARROW; }

"+="
    { return CompactTypes.PLUS_ASSIGN; }

"-="
    { return CompactTypes.MINUS_ASSIGN; }

"=="
    { return CompactTypes.EQEQ; }

"!="
    { return CompactTypes.NOTEQ; }

"<="
    { return CompactTypes.LTE; }

">="
    { return CompactTypes.GTE; }

"&&"
    { return CompactTypes.ANDAND; }

"||"
    { return CompactTypes.OROR; }

"="
    { return CompactTypes.ASSIGN; }

"+"
    { return CompactTypes.PLUS; }

"-"
    { return CompactTypes.MINUS; }

"*"
    { return CompactTypes.STAR; }

"/"
    { return CompactTokenTypes.SLASH; }

"%"
    { return CompactTokenTypes.PERCENT; }

"<"
    { return CompactTypes.LT; }

">"
    { return CompactTypes.GT; }

"!"
    { return CompactTypes.NOT; }

"."
    { return CompactTypes.DOT; }

"?"
    { return CompactTypes.QUESTION; }

":"
    { return CompactTypes.COLON; }

/* ========================================================= */
/* Delimiters                                                 */
/* ========================================================= */

"(" { return CompactTypes.LPAREN; }
")" { return CompactTypes.RPAREN; }
"{" { return CompactTypes.LBRACE; }
"}" { return CompactTypes.RBRACE; }
"[" { return CompactTypes.LBRACKET; }
"]" { return CompactTypes.RBRACKET; }

"," { return CompactTypes.COMMA; }
";" { return CompactTypes.SEMICOLON; }
"#" { return CompactTypes.HASH; }

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