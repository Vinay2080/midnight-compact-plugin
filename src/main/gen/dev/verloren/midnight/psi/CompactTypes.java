// This is a generated file. Not intended for manual editing.
package dev.verloren.midnight.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import dev.verloren.midnight.lexer.CompactTokenType;
import dev.verloren.midnight.psi.impl.*;

public interface CompactTypes {

  IElementType ARROW_PARAMETER_LIST = new IElementType("ARROW_PARAMETER_LIST", null);
  IElementType BLOCK = new IElementType("BLOCK", null);
  IElementType CBINDING = new IElementType("CBINDING", null);
  IElementType CIRCUIT_DEFINITION = new IElementType("CIRCUIT_DEFINITION", null);
  IElementType CONSTRUCTOR_DEFINITION = new IElementType("CONSTRUCTOR_DEFINITION", null);
  IElementType CONTRACT_DECLARATION = new IElementType("CONTRACT_DECLARATION", null);
  IElementType ENUM_DECLARATION = new IElementType("ENUM_DECLARATION", null);
  IElementType EXPORT_FORM = new IElementType("EXPORT_FORM", null);
  IElementType EXPR = new IElementType("EXPR", null);
  IElementType EXPR_10 = new IElementType("EXPR_10", null);
  IElementType EXPR_3 = new IElementType("EXPR_3", null);
  IElementType EXPR_7 = new IElementType("EXPR_7", null);
  IElementType EXPR_8 = new IElementType("EXPR_8", null);
  IElementType EXPR_9 = new IElementType("EXPR_9", null);
  IElementType EXPR_SEQ = new IElementType("EXPR_SEQ", null);
  IElementType EXTERNAL_CONTRACT_CIRCUIT = new IElementType("EXTERNAL_CONTRACT_CIRCUIT", null);
  IElementType FUN = new IElementType("FUN", null);
  IElementType GARG = new IElementType("GARG", null);
  IElementType GARGS = new IElementType("GARGS", null);
  IElementType GENERIC_PARAM = new IElementType("GENERIC_PARAM", null);
  IElementType GPARAMS = new IElementType("GPARAMS", null);
  IElementType IMPLEMENTS_DECLARATION = new IElementType("IMPLEMENTS_DECLARATION", null);
  IElementType IMPORT_ELEMENT = new IElementType("IMPORT_ELEMENT", null);
  IElementType IMPORT_FORM = new IElementType("IMPORT_FORM", null);
  IElementType IMPORT_NAME = new IElementType("IMPORT_NAME", null);
  IElementType IMPORT_PREFIX = new IElementType("IMPORT_PREFIX", null);
  IElementType IMPORT_SELECTION = new IElementType("IMPORT_SELECTION", null);
  IElementType INCLUDE_FORM = new IElementType("INCLUDE_FORM", null);
  IElementType LEDGER_DECLARATION = new IElementType("LEDGER_DECLARATION", null);
  IElementType MODULE_DEFINITION = new IElementType("MODULE_DEFINITION", null);
  IElementType OPTIONALLY_TYPED_PATTERN = new IElementType("OPTIONALLY_TYPED_PATTERN", null);
  IElementType PATTERN = new IElementType("PATTERN", null);
  IElementType PATTERN_PARAMETER_LIST = new IElementType("PATTERN_PARAMETER_LIST", null);
  IElementType PATTERN_STRUCT_ELT = new IElementType("PATTERN_STRUCT_ELT", null);
  IElementType PRAGMA_FORM = new IElementType("PRAGMA_FORM", null);
  IElementType PROGRAM = new IElementType("PROGRAM", null);
  IElementType PROGRAM_ELEMENT = new IElementType("PROGRAM_ELEMENT", null);
  IElementType RETURN_TYPE = new IElementType("RETURN_TYPE", null);
  IElementType SIMPLE_PARAMETER_LIST = new IElementType("SIMPLE_PARAMETER_LIST", null);
  IElementType STMT = new IElementType("STMT", null);
  IElementType STMT_0 = new IElementType("STMT_0", null);
  IElementType STRUCT_ARG = new IElementType("STRUCT_ARG", null);
  IElementType STRUCT_DECLARATION = new IElementType("STRUCT_DECLARATION", null);
  IElementType TERM = new IElementType("TERM", null);
  IElementType TREF = new IElementType("TREF", null);
  IElementType TSIZE = new IElementType("TSIZE", null);
  IElementType TUPLE_ARG = new IElementType("TUPLE_ARG", null);
  IElementType TYPED_ID = new IElementType("TYPED_ID", null);
  IElementType TYPED_PATTERN = new IElementType("TYPED_PATTERN", null);
  IElementType TYPE_ALIAS_DECLARATION = new IElementType("TYPE_ALIAS_DECLARATION", null);
  IElementType TYPE_EXPRESSION = new IElementType("TYPE_EXPRESSION", null);
  IElementType VERSION_ATOM = new IElementType("VERSION_ATOM", null);
  IElementType VERSION_TERM = new IElementType("VERSION_TERM", null);
  IElementType WITNESS_DECLARATION = new IElementType("WITNESS_DECLARATION", null);

  IElementType ANDAND = new CompactTokenType("&&");
  IElementType ARROW = new CompactTokenType("=>");
  IElementType AS = new CompactTokenType("as");
  IElementType ASSERT = new CompactTokenType("assert");
  IElementType ASSIGN = new CompactTokenType("=");
  IElementType BINARY_LITERAL = new CompactTokenType("BINARY_LITERAL");
  IElementType BOOLEAN_TYPE = new CompactTokenType("BOOLEAN_TYPE");
  IElementType BYTES_TYPE = new CompactTokenType("BYTES_TYPE");
  IElementType CIRCUIT = new CompactTokenType("circuit");
  IElementType COLON = new CompactTokenType(":");
  IElementType COMMA = new CompactTokenType(",");
  IElementType CONST = new CompactTokenType("const");
  IElementType CONSTRUCTOR = new CompactTokenType("constructor");
  IElementType CONTRACT = new CompactTokenType("contract");
  IElementType DECIMAL_LITERAL = new CompactTokenType("DECIMAL_LITERAL");
  IElementType DEFAULT = new CompactTokenType("default");
  IElementType DISCLOSE = new CompactTokenType("disclose");
  IElementType DOT = new CompactTokenType(".");
  IElementType ELSE = new CompactTokenType("else");
  IElementType EMIT = new CompactTokenType("emit");
  IElementType ENUM = new CompactTokenType("enum");
  IElementType EOF = new CompactTokenType("EOF");
  IElementType EQEQ = new CompactTokenType("==");
  IElementType EXPORT = new CompactTokenType("export");
  IElementType FALSE = new CompactTokenType("false");
  IElementType FIELD_TYPE = new CompactTokenType("FIELD_TYPE");
  IElementType FOLD = new CompactTokenType("fold");
  IElementType FOR = new CompactTokenType("for");
  IElementType FROM = new CompactTokenType("from");
  IElementType GT = new CompactTokenType(">");
  IElementType GTE = new CompactTokenType(">=");
  IElementType HASH = new CompactTokenType("#");
  IElementType HEX_LITERAL = new CompactTokenType("HEX_LITERAL");
  IElementType IDENTIFIER = new CompactTokenType("IDENTIFIER");
  IElementType IF = new CompactTokenType("if");
  IElementType IMPORT = new CompactTokenType("import");
  IElementType INCLUDE = new CompactTokenType("include");
  IElementType JUBJUB_SCALAR_TYPE = new CompactTokenType("JUBJUB_SCALAR_TYPE");
  IElementType LBRACE = new CompactTokenType("{");
  IElementType LBRACKET = new CompactTokenType("[");
  IElementType LEDGER = new CompactTokenType("ledger");
  IElementType LPAREN = new CompactTokenType("(");
  IElementType LT = new CompactTokenType("<");
  IElementType LTE = new CompactTokenType("<=");
  IElementType MAP = new CompactTokenType("map");
  IElementType MINUS = new CompactTokenType("-");
  IElementType MINUS_ASSIGN = new CompactTokenType("-=");
  IElementType MODULE = new CompactTokenType("module");
  IElementType NEW = new CompactTokenType("new");
  IElementType NOT = new CompactTokenType("!");
  IElementType NOTEQ = new CompactTokenType("!=");
  IElementType OCTAL_LITERAL = new CompactTokenType("OCTAL_LITERAL");
  IElementType OF = new CompactTokenType("of");
  IElementType OPAQUE_TYPE = new CompactTokenType("OPAQUE_TYPE");
  IElementType OROR = new CompactTokenType("||");
  IElementType PAD = new CompactTokenType("pad");
  IElementType PLUS = new CompactTokenType("+");
  IElementType PLUS_ASSIGN = new CompactTokenType("+=");
  IElementType PRAGMA = new CompactTokenType("pragma");
  IElementType PREFIX = new CompactTokenType("prefix");
  IElementType PURE = new CompactTokenType("pure");
  IElementType QUESTION = new CompactTokenType("?");
  IElementType RANGE = new CompactTokenType("..");
  IElementType RBRACE = new CompactTokenType("}");
  IElementType RBRACKET = new CompactTokenType("]");
  IElementType RESERVED_KEYWORD = new CompactTokenType("RESERVED_KEYWORD");
  IElementType RETURN = new CompactTokenType("return");
  IElementType RPAREN = new CompactTokenType(")");
  IElementType SEALED = new CompactTokenType("sealed");
  IElementType SECP256K1_BASE_TYPE = new CompactTokenType("SECP256K1_BASE_TYPE");
  IElementType SECP256K1_SCALAR_TYPE = new CompactTokenType("SECP256K1_SCALAR_TYPE");
  IElementType SEMICOLON = new CompactTokenType(";");
  IElementType SLICE = new CompactTokenType("slice");
  IElementType SPREAD = new CompactTokenType("...");
  IElementType STAR = new CompactTokenType("*");
  IElementType STRING_LITERAL = new CompactTokenType("STRING_LITERAL");
  IElementType STRUCT = new CompactTokenType("struct");
  IElementType TRUE = new CompactTokenType("true");
  IElementType TYPE = new CompactTokenType("type");
  IElementType UINT_TYPE = new CompactTokenType("UINT_TYPE");
  IElementType VECTOR_TYPE = new CompactTokenType("VECTOR_TYPE");
  IElementType VERSION_LITERAL = new CompactTokenType("VERSION_LITERAL");
  IElementType WITNESS = new CompactTokenType("witness");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ARROW_PARAMETER_LIST) {
        return new CompactArrowParameterListImpl(node);
      }
      else if (type == BLOCK) {
        return new CompactBlockImpl(node);
      }
      else if (type == CBINDING) {
        return new CompactCbindingImpl(node);
      }
      else if (type == CIRCUIT_DEFINITION) {
        return new CompactCircuitDefinitionImpl(node);
      }
      else if (type == CONSTRUCTOR_DEFINITION) {
        return new CompactConstructorDefinitionImpl(node);
      }
      else if (type == CONTRACT_DECLARATION) {
        return new CompactContractDeclarationImpl(node);
      }
      else if (type == ENUM_DECLARATION) {
        return new CompactEnumDeclarationImpl(node);
      }
      else if (type == EXPORT_FORM) {
        return new CompactExportFormImpl(node);
      }
      else if (type == EXPR) {
        return new CompactExprImpl(node);
      }
      else if (type == EXPR_10) {
        return new CompactExpr10Impl(node);
      }
      else if (type == EXPR_3) {
        return new CompactExpr3Impl(node);
      }
      else if (type == EXPR_7) {
        return new CompactExpr7Impl(node);
      }
      else if (type == EXPR_8) {
        return new CompactExpr8Impl(node);
      }
      else if (type == EXPR_9) {
        return new CompactExpr9Impl(node);
      }
      else if (type == EXPR_SEQ) {
        return new CompactExprSeqImpl(node);
      }
      else if (type == EXTERNAL_CONTRACT_CIRCUIT) {
        return new CompactExternalContractCircuitImpl(node);
      }
      else if (type == FUN) {
        return new CompactFunImpl(node);
      }
      else if (type == GARG) {
        return new CompactGargImpl(node);
      }
      else if (type == GARGS) {
        return new CompactGargsImpl(node);
      }
      else if (type == GENERIC_PARAM) {
        return new CompactGenericParamImpl(node);
      }
      else if (type == GPARAMS) {
        return new CompactGparamsImpl(node);
      }
      else if (type == IMPLEMENTS_DECLARATION) {
        return new CompactImplementsDeclarationImpl(node);
      }
      else if (type == IMPORT_ELEMENT) {
        return new CompactImportElementImpl(node);
      }
      else if (type == IMPORT_FORM) {
        return new CompactImportFormImpl(node);
      }
      else if (type == IMPORT_NAME) {
        return new CompactImportNameImpl(node);
      }
      else if (type == IMPORT_PREFIX) {
        return new CompactImportPrefixImpl(node);
      }
      else if (type == IMPORT_SELECTION) {
        return new CompactImportSelectionImpl(node);
      }
      else if (type == INCLUDE_FORM) {
        return new CompactIncludeFormImpl(node);
      }
      else if (type == LEDGER_DECLARATION) {
        return new CompactLedgerDeclarationImpl(node);
      }
      else if (type == MODULE_DEFINITION) {
        return new CompactModuleDefinitionImpl(node);
      }
      else if (type == OPTIONALLY_TYPED_PATTERN) {
        return new CompactOptionallyTypedPatternImpl(node);
      }
      else if (type == PATTERN) {
        return new CompactPatternImpl(node);
      }
      else if (type == PATTERN_PARAMETER_LIST) {
        return new CompactPatternParameterListImpl(node);
      }
      else if (type == PATTERN_STRUCT_ELT) {
        return new CompactPatternStructEltImpl(node);
      }
      else if (type == PRAGMA_FORM) {
        return new CompactPragmaFormImpl(node);
      }
      else if (type == PROGRAM) {
        return new CompactProgramImpl(node);
      }
      else if (type == PROGRAM_ELEMENT) {
        return new CompactProgramElementImpl(node);
      }
      else if (type == RETURN_TYPE) {
        return new CompactReturnTypeImpl(node);
      }
      else if (type == SIMPLE_PARAMETER_LIST) {
        return new CompactSimpleParameterListImpl(node);
      }
      else if (type == STMT) {
        return new CompactStmtImpl(node);
      }
      else if (type == STMT_0) {
        return new CompactStmt0Impl(node);
      }
      else if (type == STRUCT_ARG) {
        return new CompactStructArgImpl(node);
      }
      else if (type == STRUCT_DECLARATION) {
        return new CompactStructDeclarationImpl(node);
      }
      else if (type == TERM) {
        return new CompactTermImpl(node);
      }
      else if (type == TREF) {
        return new CompactTrefImpl(node);
      }
      else if (type == TSIZE) {
        return new CompactTsizeImpl(node);
      }
      else if (type == TUPLE_ARG) {
        return new CompactTupleArgImpl(node);
      }
      else if (type == TYPED_ID) {
        return new CompactTypedIdImpl(node);
      }
      else if (type == TYPED_PATTERN) {
        return new CompactTypedPatternImpl(node);
      }
      else if (type == TYPE_ALIAS_DECLARATION) {
        return new CompactTypeAliasDeclarationImpl(node);
      }
      else if (type == TYPE_EXPRESSION) {
        return new CompactTypeExpressionImpl(node);
      }
      else if (type == VERSION_ATOM) {
        return new CompactVersionAtomImpl(node);
      }
      else if (type == VERSION_TERM) {
        return new CompactVersionTermImpl(node);
      }
      else if (type == WITNESS_DECLARATION) {
        return new CompactWitnessDeclarationImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
