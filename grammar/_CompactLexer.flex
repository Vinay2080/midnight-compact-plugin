package dev.verloren.midnight.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static dev.verloren.midnight.psi.CompactTypes.*;

%%

%{
  public _CompactLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class CompactLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+


%%
<YYINITIAL> {
  {WHITE_SPACE}               { return WHITE_SPACE; }

  "pragma"                    { return PRAGMA; }
  "import"                    { return IMPORT; }
  "export"                    { return EXPORT; }
  "from"                      { return FROM; }
  "module"                    { return MODULE; }
  "prefix"                    { return PREFIX; }
  "assert"                    { return ASSERT; }
  "as"                        { return AS; }
  "circuit"                   { return CIRCUIT; }
  "const"                     { return CONST; }
  "constructor"               { return CONSTRUCTOR; }
  "contract"                  { return CONTRACT; }
  "default"                   { return DEFAULT; }
  "disclose"                  { return DISCLOSE; }
  "else"                      { return ELSE; }
  "enum"                      { return ENUM; }
  "external"                  { return EXTERNAL; }
  "fold"                      { return FOLD; }
  "for"                       { return FOR; }
  "if"                        { return IF; }
  "implements"                { return IMPLEMENTS; }
  "include"                   { return INCLUDE; }
  "ledger"                    { return LEDGER; }
  "map"                       { return MAP; }
  "new"                       { return NEW; }
  "of"                        { return OF; }
  "pad"                       { return PAD; }
  "pure"                      { return PURE; }
  "return"                    { return RETURN; }
  "sealed"                    { return SEALED; }
  "slice"                     { return SLICE; }
  "struct"                    { return STRUCT; }
  "type"                      { return TYPE; }
  "witness"                   { return WITNESS; }
  "emit"                      { return EMIT; }
  "true"                      { return TRUE; }
  "false"                     { return FALSE; }
  "="                         { return ASSIGN; }
  "+="                        { return PLUS_ASSIGN; }
  "-="                        { return MINUS_ASSIGN; }
  "+"                         { return PLUS; }
  "-"                         { return MINUS; }
  "*"                         { return STAR; }
  "=="                        { return EQEQ; }
  "!="                        { return NOTEQ; }
  "<"                         { return LT; }
  "<="                        { return LTE; }
  ">"                         { return GT; }
  ">="                        { return GTE; }
  "=>"                        { return ARROW; }
  "!"                         { return NOT; }
  "&&"                        { return ANDAND; }
  "||"                        { return OROR; }
  ".."                        { return RANGE; }
  "."                         { return DOT; }
  "..."                       { return SPREAD; }
  "?"                         { return QUESTION; }
  ":"                         { return COLON; }
  "("                         { return LPAREN; }
  ")"                         { return RPAREN; }
  "{"                         { return LBRACE; }
  "}"                         { return RBRACE; }
  "["                         { return LBRACKET; }
  "]"                         { return RBRACKET; }
  ","                         { return COMMA; }
  ";"                         { return SEMICOLON; }
  "#"                         { return HASH; }
  "await"                     { return AWAIT; }
  "break"                     { return BREAK; }
  "case"                      { return CASE; }
  "catch"                     { return CATCH; }
  "class"                     { return CLASS; }
  "continue"                  { return CONTINUE; }
  "debugger"                  { return DEBUGGER; }
  "delete"                    { return DELETE; }
  "do"                        { return DO; }
  "extends"                   { return EXTENDS; }
  "finally"                   { return FINALLY; }
  "function"                  { return FUNCTION; }
  "interface"                 { return INTERFACE; }
  "let"                       { return LET; }
  "null"                      { return NULL; }
  "package"                   { return PACKAGE; }
  "private"                   { return PRIVATE; }
  "protected"                 { return PROTECTED; }
  "public"                    { return PUBLIC; }
  "static"                    { return STATIC; }
  "super"                     { return SUPER; }
  "switch"                    { return SWITCH; }
  "this"                      { return THIS; }
  "throw"                     { return THROW; }
  "try"                       { return TRY; }
  "typeof"                    { return TYPEOF; }
  "var"                       { return VAR; }
  "void"                      { return VOID; }
  "while"                     { return WHILE; }
  "with"                      { return WITH; }
  "yield"                     { return YIELD; }
  "argument"                  { return ARGUMENT; }
  "eval"                      { return EVAL; }
  "event"                     { return EVENT; }
  "in"                        { return IN; }
  "instanceof"                { return INSTANCEOF; }
  "VERSION_LITERAL"           { return VERSION_LITERAL; }
  "STRING_LITERAL"            { return STRING_LITERAL; }
  "IDENTIFIER"                { return IDENTIFIER; }
  "PREFFIX"                   { return PREFFIX; }
  "BOOLEAN_TYPE"              { return BOOLEAN_TYPE; }
  "FIELD_TYPE"                { return FIELD_TYPE; }
  "JUBJUB_SCALAR_TYPE"        { return JUBJUB_SCALAR_TYPE; }
  "SECP256K1_BASE_TYPE"       { return SECP256K1_BASE_TYPE; }
  "SECP256K1_SCALAR_TYPE"     { return SECP256K1_SCALAR_TYPE; }
  "UINT_TYPE"                 { return UINT_TYPE; }
  "BYTES_TYPE"                { return BYTES_TYPE; }
  "OPAQUE_TYPE"               { return OPAQUE_TYPE; }
  "VECTOR_TYPE"               { return VECTOR_TYPE; }
  "DECIMAL_LITERAL"           { return DECIMAL_LITERAL; }
  "HEX_LITERAL"               { return HEX_LITERAL; }
  "BINARY_LITERAL"            { return BINARY_LITERAL; }
  "OCTAL_LITERAL"             { return OCTAL_LITERAL; }


}

[^] { return BAD_CHARACTER; }
