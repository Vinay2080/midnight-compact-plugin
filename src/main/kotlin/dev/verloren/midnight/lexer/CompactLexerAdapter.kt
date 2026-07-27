package dev.verloren.midnight.lexer

import com.intellij.lexer.FlexAdapter

class CompactLexerAdapter : FlexAdapter(CompactLexer(null))