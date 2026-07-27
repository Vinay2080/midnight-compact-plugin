// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import dev.verloren.midnight.lexer.CompactTokenType;
import dev.verloren.midnight.psi.impl.*;

public interface CompactTypes {

  IElementType BLOCK = new IElementType("BLOCK", null);
  IElementType CIRCUIT_DECLARATION = new IElementType("CIRCUIT_DECLARATION", null);
  IElementType IMPORT_STATEMENT = new IElementType("IMPORT_STATEMENT", null);
  IElementType PARAMETER = new IElementType("PARAMETER", null);
  IElementType PARAMETER_LIST = new IElementType("PARAMETER_LIST", null);
  IElementType PRAGMA_STATEMENT = new IElementType("PRAGMA_STATEMENT", null);
  IElementType RETURN_STATEMENT = new IElementType("RETURN_STATEMENT", null);
  IElementType RETURN_TYPE = new IElementType("RETURN_TYPE", null);
  IElementType TYPE = new IElementType("TYPE", null);

  IElementType CIRCUIT = new CompactTokenType("CIRCUIT");
  IElementType COLON = new CompactTokenType("COLON");
  IElementType COMMA = new CompactTokenType("COMMA");
  IElementType IDENTIFIER = new CompactTokenType("IDENTIFIER");
  IElementType IMPORT = new CompactTokenType("IMPORT");
  IElementType LBRACE = new CompactTokenType("LBRACE");
  IElementType LPAREN = new CompactTokenType("LPAREN");
  IElementType NUMBER = new CompactTokenType("NUMBER");
  IElementType PRAGMA = new CompactTokenType("PRAGMA");
  IElementType RBRACE = new CompactTokenType("RBRACE");
  IElementType RETURN = new CompactTokenType("RETURN");
  IElementType RPAREN = new CompactTokenType("RPAREN");
  IElementType SEMICOLON = new CompactTokenType("SEMICOLON");
  IElementType STRING_LITERAL = new CompactTokenType("STRING_LITERAL");
  IElementType VERSION = new CompactTokenType("VERSION");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == BLOCK) {
        return new CompactBlockImpl(node);
      }
      else if (type == CIRCUIT_DECLARATION) {
        return new CompactCircuitDeclarationImpl(node);
      }
      else if (type == IMPORT_STATEMENT) {
        return new CompactImportStatementImpl(node);
      }
      else if (type == PARAMETER) {
        return new CompactParameterImpl(node);
      }
      else if (type == PARAMETER_LIST) {
        return new CompactParameterListImpl(node);
      }
      else if (type == PRAGMA_STATEMENT) {
        return new CompactPragmaStatementImpl(node);
      }
      else if (type == RETURN_STATEMENT) {
        return new CompactReturnStatementImpl(node);
      }
      else if (type == RETURN_TYPE) {
        return new CompactReturnTypeImpl(node);
      }
      else if (type == TYPE) {
        return new CompactTypeImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
