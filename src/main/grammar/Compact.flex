package dev.verloren.midnight.lexer;

%%

%public
%class CompactLexer
%unicode
%final
%function advance
%type com.intellij.psi.tree.IElementType

%%

WHITE_SPACE = [ \t\r\n]+

DIGIT = [0-9]
LETTER = [A-Za-z]
IDENTIFIER = ({LETTER}|[_$])({LETTER}|{DIGIT}|[_$])*

DECIMAL = 0|[1-9]{DIGIT}*
BINARY = 0[bB][01]+
OCTAL = 0[oO][0-7]+
HEX = 0[xX][0-9a-fA-F]+

%%

/* Lexer rules will go here */