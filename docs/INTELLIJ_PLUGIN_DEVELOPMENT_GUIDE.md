# Building an IntelliJ Language Plugin From Scratch: The Complete Developer Guide

This guide is a comprehensive, step-by-step developer handbook that teaches you **how to design and build a full-featured custom language plugin for the IntelliJ Platform from an empty project**.

We will use the **Compact** smart contract language (file extension `.compact`) as our practical running example, but the concepts, architectural patterns, and IntelliJ SDK APIs taught here apply to **any programming language or DSL**.

---

# Table of Contents

1. [The Mental Model of the IntelliJ Platform](#1-the-mental-model-of-the-intellij-platform)
2. [Starting from an Empty Plugin Project](#2-starting-from-an-empty-plugin-project)
3. [Language Registration: Telling IntelliJ the Language Exists](#3-language-registration-telling-intellij-the-language-exists)
4. [File Type Registration: Associating Files with Your Language](#4-file-type-registration-associating-files-with-your-language)
5. [The Lexer: Converting Raw Characters into Tokens](#5-the-lexer-converting-raw-characters-into-tokens)
6. [Lexer Token Separation: A Deep-Dive for Beginners](#6-lexer-token-separation-a-deep-dive-for-beginners)
7. [The Parser: Converting Token Streams into Grammatical Trees](#7-the-parser-converting-token-streams-into-grammatical-trees)
8. [The AST (Abstract Syntax Tree): IntelliJ's Low-Level Tree Structure](#8-the-ast-abstract-syntax-tree-intellijs-low-level-tree-structure)
9. [The PSI (Program Structure Interface): Semantic Tree Architecture](#9-the-psi-program-structure-interface-semantic-tree-architecture)
10. [Syntax and Semantic Highlighting](#10-syntax-and-semantic-highlighting)
11. [Code Completion](#11-code-completion)
12. [Code Templates, Live Templates, and Declaration Expansion](#12-code-templates-live-templates-and-declaration-expansion)
13. [References and Go To Definition](#13-references-and-go-to-definition)
14. [Rename Refactoring](#14-rename-refactoring)
15. [Find Usages](#15-find-usages)
16. [Inspections and Quick-Fixes](#16-inspections-and-quick-fixes)
17. [Annotators: On-the-Fly Analysis and Semantic Highlighting](#17-annotators-on-the-fly-analysis-and-semantic-highlighting)
18. [Code Formatting and Indentation](#18-code-formatting-and-indentation)
19. [Brace Matching and Delimiters](#19-brace-matching-and-delimiters)
20. [Comment Support and Typing Handlers](#20-comment-support-and-typing-handlers)
21. [Structure View and Navigation](#21-structure-view-and-navigation)
22. [Quick Documentation (Ctrl+Q)](#22-quick-documentation-ctrlq)
23. [Advanced Refactoring Support](#23-advanced-refactoring-support)
24. [The Central Nervous System: `plugin.xml`](#24-the-central-nervous-system-pluginxml)
25. [Feature → Files → SDK API Master Table](#25-feature--files--sdk-api-master-table)
26. [Recommended Implementation Order from Zero](#26-recommended-implementation-order-from-zero)
27. [Decision Guide: "If I Want to Add a New Feature, What Do I Look For?"](#27-decision-guide-if-i-want-to-add-a-new-feature-what-do-i-look-for)
28. [Final "Build a Plugin from Zero" Checklist](#28-final-build-a-plugin-from-zero-checklist)

---

# 1. The Mental Model of the IntelliJ Platform

To build a language plugin, you must understand the **core pipeline** IntelliJ uses to transform a flat text file on disk into an intelligent, interactive programming environment.

```text
Source Code (.compact)
        ↓
    Language & FileType
        ↓
    Lexer Engine
        ↓
    Token Stream (IElementType)
        ↓
    Parser Engine (PsiBuilder)
        ↓
    AST Tree (ASTNode)
        ↓
    PSI Tree (PsiElement / PsiFile)
        ↓
    Language Features (Completion, References, Highlighting, Inspections, Refactoring, Formatting)
```

### Why Does IntelliJ Have These Distinct Layers?

| Layer | Responsibility | Why IntelliJ Needs It |
| :--- | :--- | :--- |
| **Language & FileType** | Identifies the dialect and maps file extensions (`.compact`). | Allows IntelliJ to associate files with the correct editor features, icons, and syntax handlers before opening them. |
| **Lexer** | Breaks raw characters into atomic words/symbols (**Tokens**). | Text editors need super-fast, synchronous tokenization for instant syntax coloring as you type, without waiting for the full grammar parser. |
| **Parser** | Groups tokens into grammatical constructs according to grammar rules. | Understands code structure (e.g., this is a `CircuitDeclaration` containing a `ParameterList` and a `Block`). |
| **AST (Abstract Syntax Tree)** | Low-level syntax tree of `ASTNode`s storing exact character offsets and parent-child links. | Fast, memory-efficient tree used for incremental re-parsing, formatting, and folding. |
| **PSI (Program Structure Interface)** | High-level, strongly-typed semantic object tree (`PsiElement`, `CompactCircuit`, etc.). | The primary API used by all IDE features (Completion, Refactoring, Inspections, Type Checking, Navigation). PSI represents what the code *means*, not just what characters are on screen. |
| **Features** | IDE actions (Highlighting, Auto-complete, Go To Definition, Rename). | These consume the PSI and References to give developers the rich IDE experience. |

---

# 2. Starting from an Empty Plugin Project

When starting a new IntelliJ plugin using the **IntelliJ Platform Gradle Plugin** (`org.jetbrains.intellij.platform`), your minimal starting project looks like this:

```text
midnight-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   └── main/
│       ├── java/ (or kotlin/)
│       │   └── dev/verloren/midnight/
│       │       ├── CompactLanguage.java
│       │       ├── CompactFileType.java
│       │       ├── lexer/
│       │       ├── parser/
│       │       └── psi/
│       └── resources/
│           ├── META-INF/
│           │   └── plugin.xml
│           └── icons/
│               └── midnightFile.svg
```

### Progressive Growth as Features Are Added

As you implement features, your project structure grows organically by functional subsystems:

```text
src/main/java/dev/verloren/midnight/
├── CompactLanguage.java              <-- Core Language Definition
├── CompactFileType.java              <-- File Type Mapping (.compact)
│
├── lexer/                            <-- Layer 1: Lexing
│   ├── CompactTokenType.java
│   ├── CompactTokenTypes.java
│   ├── CompactTokenSets.java
│   └── CompactLexer.java (or generated JFlex lexer)
│
├── parser/                           <-- Layer 2: Parsing & AST
│   ├── CompactElementType.java
│   ├── CompactElementTypes.java
│   ├── CompactParserDefinition.java
│   └── CompactParser.java (or generated Grammar-Kit parser)
│
├── psi/                              <-- Layer 3: PSI Semantics
│   ├── CompactFile.java
│   ├── CompactPsiElement.java
│   ├── CompactNamedElement.java
│   ├── CompactCircuitDefinition.java
│   └── CompactElementFactory.java
│
├── highlighter/                      <-- Feature: Highlighting
│   ├── CompactHighlighterColors.java
│   ├── CompactSyntaxHighlighter.java
│   ├── CompactSyntaxHighlighterFactory.java
│   ├── CompactHighlightingAnnotator.java
│   └── CompactColorSettingsPage.java
│
├── reference/ & resolve/             <-- Feature: References & Go To Definition
│   ├── CompactReferenceContributor.java
│   ├── CompactReferenceBase.java
│   ├── CompactValueReference.java
│   ├── CompactTypeReference.java
│   └── CompactResolveUtil.java
│
├── completion/                       <-- Feature: Code Completion
│   ├── CompactCompletionContributor.java
│   ├── CompactCompletionContext.java
│   └── CompactDeclarationInsertHandler.java
│
├── inspection/                       <-- Feature: Linting & Inspections
│   ├── CompactUnresolvedReferenceInspection.java
│   └── CompactDuplicateDeclarationInspection.java
│
├── formatter/                        <-- Feature: Code Formatting
│   ├── CompactFormattingModelBuilder.java
│   ├── CompactBlock.java
│   └── CompactLanguageCodeStyleSettingsProvider.java
│
├── editor/ & navigation/             <-- Feature: Editor Support
│   ├── CompactPairedBraceMatcher.java
│   ├── CompactQuoteHandler.java
│   ├── CompactCommenter.java
│   ├── CompactStructureViewFactory.java
│   ├── CompactBreadcrumbsProvider.java
│   ├── CompactLineMarkerProvider.java
│   ├── CompactInlayHintsProvider.java
│   └── CompactDocumentationProvider.java
│
└── refactoring/                      <-- Feature: Refactoring
    ├── CompactRefactoringSupportProvider.java
    └── CompactNamesValidator.java
```

---

# 3. Language Registration: Telling IntelliJ the Language Exists

### Concept & Purpose
IntelliJ needs a unique identity for your programming language. The `Language` class is a singleton object that serves as the root identifier for all language-specific extension points.

### What IntelliJ API Do We Use?
* **Class**: `com.intellij.lang.Language`
* **Role**: Abstract base class representing a language dialect.

### What Class Do We Create?
Create `CompactLanguage.java`:

```java
package dev.verloren.midnight;

import com.intellij.lang.Language;

/**
 * Singleton identifier for the Compact language.
 */
public final class CompactLanguage extends Language {
  public static final CompactLanguage INSTANCE = new CompactLanguage();

  private CompactLanguage() {
    super("Compact"); // The unique language ID string used across IntelliJ
  }
}
```

### How Does This Connect to `plugin.xml`?
`CompactLanguage.INSTANCE` is referenced by subsequent extension points (file types, highlighters, completion contributors, parsers). Its ID `"Compact"` is used in XML attribute tags like `language="Compact"`.

---

# 4. File Type Registration: Associating Files with Your Language

### Concept & Purpose
When the user opens a file with the `.compact` extension, IntelliJ needs to know:
1. What icon to show in the project tree.
2. What human-readable description to display in File Types settings.
3. Which `Language` singleton owns this file.

### What IntelliJ API Do We Use?
* **Class**: `com.intellij.openapi.fileTypes.LanguageFileType`
* **Extension Point**: `<fileType>` in `plugin.xml`

### What Class Do We Create?
Create `CompactFileType.java`:

```java
package dev.verloren.midnight;

import com.intellij.openapi.fileTypes.LanguageFileType;
import dev.verloren.midnight.icons.MidnightIcons;
import org.jetbrains.annotations.NotNull;
import javax.swing.Icon;

public final class CompactFileType extends LanguageFileType {
  public static final CompactFileType INSTANCE = new CompactFileType();

  private CompactFileType() {
    super(CompactLanguage.INSTANCE); // Links file type to our Language
  }

  @Override
  public @NotNull String getName() {
    return "Compact File"; // Unique internal identifier
  }

  @Override
  public @NotNull String getDescription() {
    return "Midnight Compact smart contract source file";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "compact"; // File extension without dot
  }

  @Override
  public Icon getIcon() {
    return MidnightIcons.FILE; // 16x16 SVG/PNG icon
  }
}
```

### Registration in `plugin.xml`

```xml
<extensions defaultExtensionNs="com.intellij">
  <fileType
      name="Compact File"
      implementationClass="dev.verloren.midnight.CompactFileType"
      fieldName="INSTANCE"
      language="Compact"
      extensions="compact"/>
</extensions>
```

---

# 5. The Lexer: Converting Raw Characters into Tokens

### Concept & Purpose
A **Lexer** (or scanner / tokenizer) takes raw text characters and chops them into atomic tokens:
* Identifiers (`foo`, `totalBalance`)
* Keywords (`circuit`, `witness`, `export`)
* Literals (`123`, `"hello"`, `0x2A`)
* Operators (`+`, `==`, `=>`)
* Delimiters (`{`, `}`, `(`, `)`, `;`)
* Comments and Whitespace

### How Do I Know Which IntelliJ API to Use?

IntelliJ provides three common ways to implement a lexer:

1. **JFlex with `FlexAdapter` (The Standard Approach)**:
   * You write a `.flex` specification file containing regex rules.
   * JFlex compiles `.flex` into a Java scanner class `_CompactLexer.java`.
   * You extend `com.intellij.lexer.FlexAdapter` to adapt JFlex to IntelliJ's `Lexer` interface.
2. **Handwritten `LexerBase` (The Zero-Dependency / High-Control Approach)**:
   * You write a plain Java class extending `com.intellij.lexer.LexerBase`.
   * You scan characters manually using a loop and custom lookup tables. (This is how our Compact plugin is built!).
3. **`IElementType`**:
   * The fundamental unit representing a token type (e.g. `CompactTokenTypes.CIRCUIT`).
4. **`TokenType`**:
   * IntelliJ built-in token types (`TokenType.WHITE_SPACE`, `TokenType.BAD_CHARACTER`).
5. **`TokenSet`**:
   * A high-performance bitset collection of `IElementType`s used to group tokens (e.g., `COMMENTS`, `KEYWORDS`, `LITERALS`).

### The Token Infrastructure Classes

#### 1. The Token Type Base Class (`CompactTokenType.java`)
```java
package dev.verloren.midnight.lexer;

import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.CompactLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CompactTokenType extends IElementType {
  public CompactTokenType(@NotNull @NonNls String debugName) {
    super(debugName, CompactLanguage.INSTANCE);
  }
}
```

#### 2. The Token Dictionary (`CompactTokenTypes.java`)
```java
package dev.verloren.midnight.lexer;

import com.intellij.psi.tree.IElementType;

public final class CompactTokenTypes {
  public static final IElementType IDENTIFIER = new CompactTokenType("IDENTIFIER");
  public static final IElementType CIRCUIT = new CompactTokenType("CIRCUIT");
  public static final IElementType EXPORT = new CompactTokenType("EXPORT");
  public static final IElementType FIELD_TYPE = new CompactTokenType("FIELD_TYPE");
  public static final IElementType VOID_TYPE = new CompactTokenType("VOID_TYPE");
  public static final IElementType LPAREN = new CompactTokenType("LPAREN");
  public static final IElementType RPAREN = new CompactTokenType("RPAREN");
  public static final IElementType LBRACE = new CompactTokenType("LBRACE");
  public static final IElementType RBRACE = new CompactTokenType("RBRACE");
  public static final IElementType COLON = new CompactTokenType("COLON");
  public static final IElementType SEMICOLON = new CompactTokenType("SEMICOLON");
  public static final IElementType LINE_COMMENT = new CompactTokenType("LINE_COMMENT");
  public static final IElementType BLOCK_COMMENT = new CompactTokenType("BLOCK_COMMENT");
  // ...
}
```

#### 3. Token Sets (`CompactTokenSets.java`)
```java
package dev.verloren.midnight.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.TokenSet;

public final class CompactTokenSets {
  public static final TokenSet COMMENTS = TokenSet.create(
      CompactTokenTypes.LINE_COMMENT, 
      CompactTokenTypes.BLOCK_COMMENT
  );
  public static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
  public static final TokenSet KEYWORDS = TokenSet.create(
      CompactTokenTypes.CIRCUIT, 
      CompactTokenTypes.EXPORT,
      CompactTokenTypes.WITNESS,
      CompactTokenTypes.LEDGER
  );
}
```

---

# 6. Lexer Token Separation: A Deep-Dive for Beginners

Let's trace how the lexer breaks apart this line of source code:

```compact
export circuit foo(x: Field): Void {}
```

```text
Input Buffer: "export circuit foo(x: Field): Void {}"
Offset 00..06: "export"  --> CompactTokenTypes.EXPORT
Offset 06..07: " "       --> TokenType.WHITE_SPACE
Offset 07..14: "circuit" --> CompactTokenTypes.CIRCUIT
Offset 14..15: " "       --> TokenType.WHITE_SPACE
Offset 15..18: "foo"     --> CompactTokenTypes.IDENTIFIER
Offset 18..19: "("       --> CompactTokenTypes.LPAREN
Offset 19..20: "x"       --> CompactTokenTypes.IDENTIFIER
Offset 20..21: ":"       --> CompactTokenTypes.COLON
Offset 21..22: " "       --> TokenType.WHITE_SPACE
Offset 22..27: "Field"   --> CompactTokenTypes.FIELD_TYPE
Offset 27..28: ")"       --> CompactTokenTypes.RPAREN
Offset 28..29: ":"       --> CompactTokenTypes.COLON
Offset 29..30: " "       --> TokenType.WHITE_SPACE
Offset 30..34: "Void"    --> CompactTokenTypes.VOID_TYPE
Offset 34..35: " "       --> TokenType.WHITE_SPACE
Offset 35..36: "{"       --> CompactTokenTypes.LBRACE
Offset 36..37: "}"       --> CompactTokenTypes.RBRACE
```

### Lexer Mechanics Explained:

1. **How Token Boundaries Are Determined**:
   * The lexer advances a character pointer `currentOffset`.
   * For identifiers/keywords: It starts on a letter (`isIdentifierStart`) and keeps advancing as long as characters are alphanumeric or `_` (`isIdentifierPart`). When it hits a non-identifier character (like ` ` or `(`), the token boundary is established.
2. **Keywords vs Identifiers**:
   * Both `circuit` and `foo` match the identifier character regex `[a-zA-Z_][a-zA-Z0-9_]*`.
   * **Resolution Rule**: Once the full word string is extracted, check a hash map `KEYWORDS.get(word)`. If found, return the keyword token (e.g. `CIRCUIT`); otherwise, return `IDENTIFIER`.
3. **Whitespace and Comments**:
   * Whitespace and comments are scanned as real tokens (`TokenType.WHITE_SPACE`, `LINE_COMMENT`).
   * They are registered in `CompactParserDefinition.getWhitespaceTokens()` and `getCommentTokens()`. IntelliJ's `PsiBuilder` automatically filters them out so your grammar parser never has to deal with whitespace!
4. **Conflicting Lexer Rules (e.g. `.` vs `..` vs `1.2.3`)**:
   * Multi-character tokens use **lookahead**: When the lexer sees `.`, it checks if the next character is also `.`. If so, it consumes both and emits `RANGE` (`..`); otherwise, it emits a single `DOT` (`.`).

---

# 7. The Parser: Converting Token Streams into Grammatical Trees

### Concept & Purpose
The **Parser** takes the flat token stream produced by the lexer and builds a hierarchical tree according to the language grammar rules.

### Why is the Parser Separate from the Lexer?
* The lexer only knows local word patterns (e.g., "this is a word", "this is a number").
* The parser knows grammatical relationships (e.g., "a circuit declaration starts with `circuit`, followed by an identifier, parameter list, return type, and block body").

### What IntelliJ API Do We Use?
* **Interface**: `com.intellij.lang.PsiParser`
* **Builder**: `com.intellij.lang.PsiBuilder` (provides marker navigation, token consumption, rollback, and error recovery)
* **Parser Definition**: `com.intellij.lang.ParserDefinition`

### How the Parser Operates: The Marker Lifecycle

Inside `CompactParser.java`:

```java
public class CompactParser implements PsiParser {
  @Override
  public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
    PsiBuilder.Marker fileMarker = builder.mark(); // Start root file node

    while (!builder.eof()) {
      parseDeclaration(builder);
    }

    fileMarker.done(root); // Close root file node
    return builder.getTreeBuilt();
  }

  private void parseCircuit(PsiBuilder builder) {
    PsiBuilder.Marker circuitMarker = builder.mark(); // Start CIRCUIT_DEFINITION

    builder.advanceLexer(); // Consumes 'circuit' keyword
    
    if (builder.getTokenType() == CompactTokenTypes.IDENTIFIER) {
      builder.advanceLexer(); // Consumes circuit name 'foo'
    } else {
      builder.error("Expected circuit name");
    }

    parseParameterList(builder);
    
    if (builder.getTokenType() == CompactTokenTypes.COLON) {
      builder.advanceLexer(); // Consumes ':'
      parseType(builder);     // Parses return type 'Void'
    }

    parseBlock(builder);      // Parses '{ ... }'

    circuitMarker.done(CompactElementTypes.CIRCUIT_DEFINITION); // Wrap node
  }
}
```

### The 5 Essential Marker Operations:
1. `builder.mark()`: Places a start marker in the tree.
2. `marker.done(IElementType)`: Completes the node and wraps all tokens consumed since `mark()` under this composite element.
3. `marker.precede()`: Inserts a new parent node above an already existing marker (crucial for binary operator precedence like `1 + 2 * 3`).
4. `marker.rollbackTo()`: Discards the marker and rewinds the token stream (used for speculative lookahead backtracking).
5. `marker.drop()`: Discards the marker without rewinding (used when speculative lookahead succeeds).

---

# 8. The AST (Abstract Syntax Tree): IntelliJ's Low-Level Tree Structure

### Token vs AST Node vs PSI Element

| Concept | Class / Interface | Responsibility | Example |
| :--- | :--- | :--- | :--- |
| **Token (Leaf)** | `LeafElement` / `CompactTokenType` | Raw lexical token with text and offset. | `CIRCUIT`, `IDENTIFIER` (`"foo"`) |
| **AST Node (Composite)** | `CompositeElement` / `ASTNode` | Structural tree node with parent/child links and byte offsets. | `CIRCUIT_DEFINITION`, `BLOCK` |
| **PSI Element (Semantic)** | `PsiElement` / `CompactCircuit` | High-level typed Java class offering semantic methods (`getName()`, `getParameters()`, `getType()`). | `CompactCircuitDefinitionImpl` |

```text
               CompactFile (PSI) / FILE (AST)
                       │
             CompactCircuitDefinition (PSI) / CIRCUIT_DEFINITION (AST)
            ┌──────────┼─────────────────────┬──────────────────┐
        LeafElement  LeafElement        CompositeElement    CompositeElement
         (CIRCUIT)  (IDENTIFIER: "foo")   (PARAMETER_LIST)      (BLOCK)
```

### Do I Manually Create AST Classes?
No! You define the composite `IElementType` constants in `CompactElementTypes.java`. IntelliJ's parser automatically creates `ASTNode` instances when you call `marker.done(CompactElementTypes.CIRCUIT_DEFINITION)`.

```java
package dev.verloren.midnight.parser;

import dev.verloren.midnight.lexer.CompactElementType;

public final class CompactElementTypes {
  public static final CompactElementType CIRCUIT_DEFINITION = new CompactElementType("CIRCUIT_DEFINITION");
  public static final CompactElementType BLOCK = new CompactElementType("BLOCK");
  public static final CompactElementType PARAMETER_LIST = new CompactElementType("PARAMETER_LIST");
  public static final CompactElementType TYPED_ID = new CompactElementType("TYPED_ID");
}
```

---

# 9. The PSI (Program Structure Interface): Semantic Tree Architecture

### Concept & Purpose
While the AST knows about character offsets and raw syntax nodes, the **PSI** is the high-level semantic layer. It represents the object-oriented structure of the code.

For example, instead of querying raw AST children by token index, a developer calls:
```java
CompactCircuit circuit = ...;
String name = circuit.getName();
List<CompactParameter> params = circuit.getParameters();
CompactType returnType = circuit.getReturnType();
```

### The PSI Base Hierarchy

1. **`CompactPsiElement`**:
   * Extends `com.intellij.extapi.psi.ASTWrapperPsiElement`.
   * Base class for all custom PSI elements.
2. **`CompactNamedElement`**:
   * Extends `com.intellij.psi.PsiNamedElement` and `com.intellij.psi.PsiNameIdentifierOwner`.
   * Base interface for any element with a name that can be referenced, searched, or renamed (circuits, variables, structs, types).

### Creating a Typed PSI Class: `CompactCircuitDefinitionImpl.java`

```java
package dev.verloren.midnight.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompactCircuitDefinitionImpl extends CompactNamedElementImpl implements CompactCircuitDefinition {
  public CompactCircuitDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable String getName() {
    PsiElement id = getNameIdentifier();
    return id != null ? id.getText() : null;
  }

  @Override
  public @Nullable PsiElement getNameIdentifier() {
    ASTNode idNode = getNode().findChildByType(CompactTokenTypes.IDENTIFIER);
    return idNode != null ? idNode.getPsi() : null;
  }
}
```

### The PSI Factory: `CompactElementFactory.java`

IntelliJ connects `ASTNode`s to `PsiElement`s using a factory called by `CompactParserDefinition.createElement(ASTNode node)`:

```java
public final class CompactElementFactory {
  public static PsiElement createElement(ASTNode node) {
    IElementType type = node.getElementType();
    if (type == CompactElementTypes.CIRCUIT_DEFINITION) {
      return new CompactCircuitDefinitionImpl(node);
    }
    if (type == CompactElementTypes.BLOCK) {
      return new CompactBlock(node);
    }
    return new CompactPsiElement(node);
  }
}
```

---

# 10. Syntax and Semantic Highlighting

### The Two-Tier Highlighting Architecture

```text
Tier 1: SyntaxHighlighter (Fast, Token-Based)
Token (CIRCUIT) ──> TextAttributesKey (KEYWORD) ──> Color Scheme (Orange / Bold)

Tier 2: HighlightingAnnotator (Semantic, PSI-Based)
PsiElement (CompactCircuitDefinition) ──> Distinguishes Circuit vs Witness Call ──> Color Scheme
```

### 1. The Syntax Highlighter (`CompactSyntaxHighlighter.java`)
* **API**: `com.intellij.openapi.fileTypes.SyntaxHighlighterBase`
* **Factory**: `com.intellij.openapi.fileTypes.SyntaxHighlighterFactory`
* Maps raw lexer tokens to `TextAttributesKey`s:

```java
public class CompactSyntaxHighlighter extends SyntaxHighlighterBase {
  public static final TextAttributesKey KEYWORD = 
      TextAttributesKey.createTextAttributesKey("COMPACT_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey STRING = 
      TextAttributesKey.createTextAttributesKey("COMPACT_STRING", DefaultLanguageHighlighterColors.STRING);

  @Override
  public @NotNull Lexer getHighlightingLexer() {
    return new CompactLexer();
  }

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
    if (CompactTokenSets.KEYWORDS.contains(tokenType)) {
      return pack(KEYWORD);
    }
    if (tokenType == CompactTokenTypes.STRING_LITERAL) {
      return pack(STRING);
    }
    return EMPTY;
  }
}
```

### 2. The Color Settings Page (`CompactColorSettingsPage.java`)
* **API**: `com.intellij.openapi.options.colors.ColorSettingsPage`
* Exposes color customization in **Settings -> Editor -> Color Scheme -> Compact**.

---

# 11. Code Completion

### Concept & Purpose
When the user types `cir`, IntelliJ opens a popup offering `circuit`. When typing inside a function, it offers in-scope variables.

### What IntelliJ API Do We Use?
* **Class**: `com.intellij.codeInsight.completion.CompletionContributor`
* **Provider**: `com.intellij.codeInsight.completion.CompletionProvider`
* **Builder**: `com.intellij.codeInsight.lookup.LookupElementBuilder`
* **Extension Point**: `<completion.contributor>`

### Implementation Example

```java
public class CompactCompletionContributor extends CompletionContributor {
  public CompactCompletionContributor() {
    extend(
        CompletionType.BASIC,
        PlatformPatterns.psiElement().withLanguage(CompactLanguage.INSTANCE),
        new CompletionProvider<>() {
          @Override
          protected void addCompletions(
              @NotNull CompletionParameters parameters,
              @NotNull ProcessingContext context,
              @NotNull CompletionResultSet result
          ) {
            PsiElement position = parameters.getPosition();
            
            // 1. Keyword completions
            result.addElement(LookupElementBuilder.create("circuit")
                .withBoldness(true)
                .withInsertHandler(new CompactDeclarationInsertHandler(CompactDeclarationType.CIRCUIT)));
            
            result.addElement(LookupElementBuilder.create("witness"));
            result.addElement(LookupElementBuilder.create("ledger"));

            // 2. Scope-based completions (variables, circuits)
            for (CompactNamedElement decl : CompactResolveUtil.collectVisibleDeclarations(position)) {
              result.addElement(LookupElementBuilder.create(decl)
                  .withIcon(decl.getIcon(0))
                  .withTypeText(decl.getType().toString()));
            }
          }
        }
    );
  }
}
```

---

# 12. Code Templates, Live Templates, and Declaration Expansion

### Autocomplete vs Live Templates vs Postfix Completion

| Mechanism | Trigger | When to Use | IntelliJ API |
| :--- | :--- | :--- | :--- |
| **Completion Insert Handler** | User picks `circuit` from completion popup. | Smart completion of declaration boilerplate with calculated tab-stops. | `InsertHandler<LookupElement>` |
| **Live Template** | User types `cir` + <kbd>Tab</kbd> or <kbd>Enter</kbd>. | Standard parameterized code snippet templates. | `/liveTemplates/Compact.xml` + `TemplateContextType` |
| **Postfix Completion** | User types `expr.assert` + <kbd>Tab</kbd>. | Surrounding or transforming an existing expression. | `PostfixTemplate` |

### Custom Insert Handler Example (`CompactDeclarationInsertHandler.java`)
When `circuit` is selected from completion, launches an interactive IntelliJ `Template` placing the cursor on the generated name (`circuit1`) with a tab stop to the return type:

```java
public class CompactDeclarationInsertHandler implements InsertHandler<LookupElement> {
  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    Editor editor = context.getEditor();
    TemplateManager templateManager = TemplateManager.getInstance(context.getProject());
    Template template = templateManager.createTemplate("", "");
    
    template.addTextSegment(" ");
    template.addVariable("NAME", new ConstantNode("circuit1"), true);
    template.addTextSegment("(): ");
    template.addVariable("RET", new ConstantNode("Void"), true);
    template.addTextSegment(" {\n  ");
    template.addEndVariable();
    template.addTextSegment("\n}");
    
    templateManager.startTemplate(editor, template);
  }
}
```

---

# 13. References and Go To Definition

### The Reference Resolution Pipeline

```text
User presses Ctrl+Click on identifier "foo" in "foo();"
        ↓
CompactReferenceContributor (Finds PsiReference at caret)
        ↓
CompactValueReference.resolve()
        ↓
CompactResolveUtil.resolveValue("foo", contextElement)
        │
        ├── 1. Check local parameters and local constants
        ├── 2. Check top-level circuits and witnesses in current file
        └── 3. Check included / imported files (include "types.compact";)
        ↓
Returns CompactCircuitDefinitionImpl of "circuit foo() { ... }"
        ↓
IntelliJ navigates editor caret to declaration
```

### What Classes Do We Create?

1. **`CompactReferenceBase.java`**: Extends `PsiPolyVariantReferenceBase<PsiElement>`, caches results in IntelliJ's `ResolveCache`.
2. **`CompactValueReference.java`**: Resolves identifiers in the `VALUE` namespace (variables, circuits, witnesses).
3. **`CompactTypeReference.java`**: Resolves identifiers in the `TYPE` namespace (structs, enums, type aliases).
4. **`CompactReferenceContributor.java`**: Registers reference providers on identifier PSI elements.

```java
public class CompactReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
        PlatformPatterns.psiElement(CompactTokenTypes.IDENTIFIER).withLanguage(CompactLanguage.INSTANCE),
        new PsiReferenceProvider() {
          @Override
          public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
            PsiElement parent = element.getParent();
            if (parent instanceof CompactNamedElement named && element == named.getNameIdentifier()) {
              return PsiReference.EMPTY_ARRAY; // Declarations do not reference themselves
            }
            if (parent instanceof CompactTypeReferenceImpl) {
              return new PsiReference[]{new CompactTypeReference(element, TextRange.from(0, element.getTextLength()))};
            }
            return new PsiReference[]{new CompactValueReference(element, TextRange.from(0, element.getTextLength()))};
          }
        }
    );
  }
}
```

---

# 14. Rename Refactoring

### How Rename Refactoring Works
When the user presses <kbd>Shift+F6</kbd> on a declaration or reference:
1. IntelliJ verifies if the element implements `PsiNamedElement`.
2. It queries `NamesValidator` to check if the new name is a valid identifier (and not a reserved keyword).
3. It searches for all `PsiReference`s across the project that resolve to this element.
4. It calls `element.setName(newName)`.
5. It calls `reference.handleElementRename(newName)` on all matching usages.

### What Classes Do We Create?

#### 1. Refactoring Support Provider (`CompactRefactoringSupportProvider.java`)
```java
public class CompactRefactoringSupportProvider extends RefactoringSupportProvider {
  @Override
  public boolean isMemberInplaceRenameAvailable(@NotNull PsiElement element, @Nullable PsiElement context) {
    return element instanceof CompactNamedElement;
  }
}
```

#### 2. Names Validator (`CompactNamesValidator.java`)
```java
public class CompactNamesValidator implements NamesValidator {
  @Override
  public boolean isKeyword(@NotNull String name, Project project) {
    return CompactTokenSets.KEYWORDS.contains(CompactLexer.findKeywordToken(name));
  }

  @Override
  public boolean isIdentifier(@NotNull String name, Project project) {
    return name.matches("^[a-zA-Z_$][a-zA-Z0-9_$]*$");
  }
}
```

#### 3. Leaf Replacement in `setName()` (`CompactNamedElementImpl.java`)
```java
@Override
public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
  PsiElement identifier = getNameIdentifier();
  if (identifier != null) {
    PsiElement newIdentifier = CompactElementFactory.createIdentifierLeaf(getProject(), name);
    identifier.replace(newIdentifier);
  }
  return this;
}
```

---

# 15. Find Usages

### How Find Usages Works
When the user presses <kbd>Alt+F7</kbd>:
1. IntelliJ resolves the element at caret to a `PsiNamedElement`.
2. It queries `CompactFindUsagesProvider` for descriptive words ("circuit foo", "variable x").
3. IntelliJ uses its internal **Word Index** to find files containing the string `"foo"`.
4. In each candidate file, it checks if the identifier has a `PsiReference` that resolves to the target `PsiNamedElement`.

### Implementation: `CompactFindUsagesProvider.java`
* **API**: `com.intellij.lang.findUsages.FindUsagesProvider`

```java
public class CompactFindUsagesProvider implements FindUsagesProvider {
  @Override
  public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
    return psiElement instanceof CompactNamedElement;
  }

  @Override
  public @NotNull String getNodeText(@NotNull PsiElement element, boolean useFullName) {
    return element instanceof CompactNamedElement named ? named.getName() : "";
  }

  @Override
  public @NotNull String getType(@NotNull PsiElement element) {
    if (element instanceof CompactCircuitDefinition) return "circuit";
    if (element instanceof CompactWitnessDeclaration) return "witness";
    if (element instanceof CompactStructDefinition) return "struct";
    return "declaration";
  }
}
```

---

# 16. Inspections and Quick-Fixes

### Parser Error vs Annotator vs Inspection

| Analysis Tool | Execution Timing | Severity | When to Use |
| :--- | :--- | :--- | :--- |
| **Parser Error** | Synchronously during parsing. | Syntax Error | Missing semicolons, unmatched parentheses, syntax violations. |
| **Annotator** | Real-time during editor typing. | Warning / Error / Info | Dynamic semantic checks that require light resolution or semantic highlighting. |
| **Inspection (`LocalInspectionTool`)** | Background daemon / Batch CI scan. | Configurable (Warning, Error, Weak Warning) | Semantic rules, linting, unused variables, type mismatches, dead code, with registered QuickFixes. |

### Creating an Inspection: `CompactDuplicateDeclarationInspection.java`

```java
public class CompactDuplicateDeclarationInspection extends LocalInspectionTool {
  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new CompactVisitor() {
      @Override
      public void visitCircuitDefinition(@NotNull CompactCircuitDefinition circuit) {
        String name = circuit.getName();
        if (name == null) return;
        
        CompactFile file = (CompactFile) circuit.getContainingFile();
        long duplicates = file.findChildrenByClass(CompactCircuitDefinition.class).stream()
            .filter(c -> name.equals(c.getName()))
            .count();
            
        if (duplicates > 1) {
          holder.registerProblem(
              circuit.getNameIdentifier(),
              "Duplicate circuit declaration '" + name + "'",
              ProblemHighlightType.GENERIC_ERROR
          );
        }
      }
    };
  }
}
```

---

# 17. Annotators: On-the-Fly Analysis and Semantic Highlighting

### Concept & Purpose
An `Annotator` is an on-the-fly visitor that traverses visible PSI nodes in the editor to attach warnings, errors, or custom `TextAttributesKey` highlighting colors.

### Implementation: `CompactHighlightingAnnotator.java`

```java
public class CompactHighlightingAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    // Distinguish circuit call vs witness call
    if (element instanceof CompactCallExpr callExpr) {
      PsiReference ref = callExpr.getReference();
      PsiElement target = ref != null ? ref.resolve() : null;
      
      if (target instanceof CompactWitnessDeclaration) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(CompactHighlighterColors.WITNESS_CALL)
            .create();
      } else {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(CompactHighlighterColors.CIRCUIT_CALL)
            .create();
      }
    }
  }
}
```

---

# 18. Code Formatting and Indentation

### How IntelliJ Formats Code
1. `CompactFormattingModelBuilder` creates a tree of `CompactBlock` objects wrapping the AST.
2. `SpacingBuilder` defines whitespace rules between token types.
3. `Indent` defines indentation rules for child blocks.

```java
public class CompactFormattingModelBuilder implements FormattingModelBuilder {
  @Override
  public @NotNull FormattingModel createModel(@NotNull FormattingContext context) {
    CodeStyleSettings settings = context.getCodeStyleSettings();
    SpacingBuilder spacing = new SpacingBuilder(settings, CompactLanguage.INSTANCE)
        .around(CompactTokenSets.OPERATORS).spaceIf(true)
        .after(CompactTokenTypes.COMMA).spaceIf(true)
        .before(CompactTokenTypes.LBRACE).spaceIf(true)
        .before(CompactTokenTypes.SEMICOLON).spaceIf(false);

    CompactBlock rootBlock = new CompactBlock(context.getNode(), null, null, spacing, Indent.getNoneIndent(), settings);
    return FormattingModelProvider.createFormattingModelForPsiFile(context.getContainingFile(), rootBlock, settings);
  }
}
```

Inside `CompactBlock.java`:
```java
@Override
public Indent getIndent() {
  IElementType type = myNode.getElementType();
  if (myNode.getTreeParent() != null && myNode.getTreeParent().getElementType() == CompactElementTypes.BLOCK) {
    return Indent.getNormalIndent(); // Indent statements inside { }
  }
  return Indent.getNoneIndent();
}
```

---

# 19. Brace Matching and Delimiters

### Implementation: `CompactPairedBraceMatcher.java`
* **API**: `com.intellij.lang.PairedBraceMatcher`
* **Extension Point**: `<lang.braceMatcher>`

```java
public class CompactPairedBraceMatcher implements PairedBraceMatcher {
  private static final BracePair[] PAIRS = new BracePair[]{
      new BracePair(CompactTokenTypes.LBRACE, CompactTokenTypes.RBRACE, true),  // Structural
      new BracePair(CompactTokenTypes.LPAREN, CompactTokenTypes.RPAREN, false),
      new BracePair(CompactTokenTypes.LBRACKET, CompactTokenTypes.RBRACKET, false),
      new BracePair(CompactTokenTypes.LT, CompactTokenTypes.GT, false)
  };

  @Override
  public BracePair @NotNull [] getPairs() {
    return PAIRS;
  }

  @Override
  public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
    return true;
  }

  @Override
  public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
    return openingBraceOffset;
  }
}
```

---

# 20. Comment Support and Typing Handlers

### Implementation: `CompactCommenter.java`
* **API**: `com.intellij.lang.Commenter`
* **Extension Point**: `<lang.commenter>`
* Powers <kbd>Ctrl+/</kbd> (Line Comment) and <kbd>Ctrl+Shift+/</kbd> (Block Comment).

```java
public class CompactCommenter implements Commenter {
  @Override public @Nullable String getLineCommentPrefix() { return "//"; }
  @Override public @Nullable String getBlockCommentPrefix() { return "/*"; }
  @Override public @Nullable String getBlockCommentSuffix() { return "*/"; }
  @Override public @Nullable String getCommentedBlockCommentPrefix() { return null; }
  @Override public @Nullable String getCommentedBlockCommentSuffix() { return null; }
}
```

---

# 21. Structure View and Navigation

### Concept & Purpose
Powers the **Structure Tool Window** (<kbd>Alt+7</kbd>) and the **File Structure Popup** (<kbd>Ctrl+F12</kbd>), allowing developers to navigate between circuits, structs, and fields in a file.

### What Classes Do We Create?
1. **`CompactStructureViewFactory.java`**: Implements `PsiStructureViewFactory`.
2. **`CompactStructureViewModel.java`**: Extends `StructureViewModelBase`.
3. **`CompactStructureViewElement.java`**: Implements `StructureViewTreeElement` and wraps a `CompactNamedElement`.

```java
public class CompactStructureViewElement implements StructureViewTreeElement {
  private final PsiElement element;

  public CompactStructureViewElement(PsiElement element) {
    this.element = element;
  }

  @Override
  public TreeElement @NotNull [] getChildren() {
    if (element instanceof CompactFile file) {
      return file.findChildrenByClass(CompactNamedElement.class).stream()
          .map(CompactStructureViewElement::new)
          .toArray(TreeElement[]::new);
    }
    return EMPTY_ARRAY;
  }

  @Override
  public @NotNull ItemPresentation getPresentation() {
    return ((CompactNamedElement) element).getPresentation();
  }
}
```

---

# 22. Quick Documentation (Ctrl+Q)

### Implementation: `CompactDocumentationProvider.java`
* **API**: `com.intellij.lang.documentation.DocumentationProvider`
* **Extension Point**: `<lang.documentationProvider>`

```java
public class CompactDocumentationProvider implements DocumentationProvider {
  @Override
  public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (element instanceof CompactCircuitDefinition circuit) {
      return "<b>circuit</b> " + circuit.getName() + "()<br><i>Midnight smart contract entry point</i>";
    }
    return null;
  }
}
```

---

# 23. Advanced Refactoring Support

| Refactoring Feature | IntelliJ API | Implementation Requirement |
| :--- | :--- | :--- |
| **Rename** (<kbd>Shift+F6</kbd>) | `RefactoringSupportProvider` + `NamesValidator` | Implement `PsiNamedElement.setName()` and identifier replacement via `CompactElementFactory`. |
| **Safe Delete** (<kbd>Alt+Delete</kbd>) | `SafeDeleteProcessor` | Inspects `PsiReference` usages across the project before deleting the PSI node. |
| **Move File / Declaration** | `MoveFileHandler` / `MoveHandlerDelegate` | Rewrites `include` and `import` path strings across referencing files. |
| **Change Signature** (<kbd>Ctrl+F6</kbd>) | `ChangeSignatureHandler` | Custom dialog modifying `CompactParameterList` and updating call expressions. |

---

# 24. The Central Nervous System: `plugin.xml`

### Why is `plugin.xml` So Important?
In IntelliJ Platform's architecture, **no Java/Kotlin code runs automatically**. Every language capability must be registered as an **Extension** under the `com.intellij` namespace in `plugin.xml`.

```text
My Custom Java Class (CompactCompletionContributor)
         ↓
plugin.xml Extension Point (<completion.contributor language="Compact" .../>)
         ↓
IntelliJ Platform Plugin Manager
         ↓
Feature Activates in Editor for .compact Files
```

### Complete Example `plugin.xml`

```xml
<idea-plugin>
  <id>dev.verloren.midnight</id>
  <name>Midnight Compact Language</name>
  <vendor>Verloren</vendor>
  
  <depends>com.intellij.modules.platform</depends>

  <extensions defaultExtensionNs="com.intellij">
    <!-- 1. Language & FileType -->
    <fileType name="Compact File" implementationClass="dev.verloren.midnight.CompactFileType" fieldName="INSTANCE" language="Compact" extensions="compact"/>
    <lang.parserDefinition language="Compact" implementationClass="dev.verloren.midnight.parser.CompactParserDefinition"/>

    <!-- 2. Highlighting & Colors -->
    <lang.syntaxHighlighterFactory language="Compact" implementationClass="dev.verloren.midnight.highlighter.CompactSyntaxHighlighterFactory"/>
    <annotator language="Compact" implementationClass="dev.verloren.midnight.highlighter.CompactHighlightingAnnotator"/>
    <colorSettingsPage implementationClass="dev.verloren.midnight.highlighter.CompactColorSettingsPage"/>

    <!-- 3. Completion & Templates -->
    <completion.contributor language="Compact" implementationClass="dev.verloren.midnight.completion.CompactCompletionContributor"/>
    <defaultLiveTemplates file="/liveTemplates/Compact.xml"/>
    <liveTemplateContext contextId="COMPACT_CODE" implementation="dev.verloren.midnight.ide.templates.CompactLiveTemplateContextType"/>

    <!-- 4. References & Refactoring -->
    <psi.referenceContributor language="Compact" implementation="dev.verloren.midnight.reference.CompactReferenceContributor"/>
    <lang.refactoringSupport language="Compact" implementationClass="dev.verloren.midnight.refactoring.CompactRefactoringSupportProvider"/>
    <lang.namesValidator language="Compact" implementationClass="dev.verloren.midnight.refactoring.CompactNamesValidator"/>
    <lang.findUsagesProvider language="Compact" implementationClass="dev.verloren.midnight.navigation.CompactFindUsagesProvider"/>

    <!-- 5. Editor Features -->
    <lang.formatter language="Compact" implementationClass="dev.verloren.midnight.formatter.CompactFormattingModelBuilder"/>
    <lang.braceMatcher language="Compact" implementationClass="dev.verloren.midnight.editor.CompactPairedBraceMatcher"/>
    <lang.commenter language="Compact" implementationClass="dev.verloren.midnight.editor.CompactCommenter"/>
    <lang.psiStructureViewFactory language="Compact" implementationClass="dev.verloren.midnight.structure.CompactStructureViewFactory"/>
    <lang.documentationProvider language="Compact" implementationClass="dev.verloren.midnight.editor.CompactDocumentationProvider"/>

    <!-- 6. Inspections -->
    <localInspection language="Compact" displayName="Duplicate declaration" groupName="Compact" enabledByDefault="true" level="ERROR"
                     implementationClass="dev.verloren.midnight.inspection.CompactDuplicateDeclarationInspection"/>
  </extensions>
</idea-plugin>
```

---

# 25. Feature → Files → SDK API Master Table

| Language Feature | Files You Create | IntelliJ SDK Class / Extension Point | Purpose | Depends On |
| :--- | :--- | :--- | :--- | :--- |
| **Language Identity** | `CompactLanguage.java` | `com.intellij.lang.Language` | Defines unique language dialect. | — |
| **File Type** | `CompactFileType.java` | `LanguageFileType` / `<fileType>` | Binds `.compact` extension & icon. | `Language` |
| **Lexer** | `CompactLexer.java` | `LexerBase` or `FlexAdapter` | Scans raw characters into tokens. | `IElementType` |
| **Tokens** | `CompactTokenTypes.java` | `IElementType` / `TokenSet` | Dictionary of token constants. | `Language` |
| **Parser Definition** | `CompactParserDefinition.java`| `ParserDefinition` / `<lang.parserDefinition>` | Glues lexer, parser, and PSI factory. | Lexer, Parser |
| **Parser** | `CompactParser.java` | `PsiParser` + `PsiBuilder` | Parses tokens into AST grammar tree. | Tokens |
| **AST Types** | `CompactElementTypes.java` | `IElementType` (`ASTNode`) | Composite AST element types. | `Language` |
| **PSI Elements** | `CompactCircuitDefinitionImpl` | `ASTWrapperPsiElement` | Strongly-typed semantic tree nodes. | AST Nodes |
| **PSI Factory** | `CompactElementFactory.java` | Static mapping method | Instantiates PSI elements from AST nodes. | PSI classes |
| **Syntax Highlighting**| `CompactSyntaxHighlighter.java`| `SyntaxHighlighterFactory` | Fast token-based editor colors. | Lexer tokens |
| **Semantic Highlighting**| `CompactHighlightingAnnotator`| `Annotator` / `<annotator>` | Context-aware AST coloring. | PSI tree |
| **Color Settings** | `CompactColorSettingsPage.java`| `ColorSettingsPage` | User configurable color settings UI. | `TextAttributesKey` |
| **Code Completion** | `CompactCompletionContributor`| `CompletionContributor` | Suggests keywords, variables, types. | PSI / Scopes |
| **Live Templates** | `Compact.xml` | `<defaultLiveTemplates>` | Code snippet abbreviations (`cir` + Tab).| `TemplateContext` |
| **References** | `CompactReferenceContributor` | `PsiReferenceContributor` | Resolves usages to declarations (Ctrl+Click).| PSI |
| **Rename Refactoring**| `CompactRefactoringSupport` | `RefactoringSupportProvider` | Renames declarations & usages. | `PsiNamedElement` |
| **Find Usages** | `CompactFindUsagesProvider` | `FindUsagesProvider` | Finds symbol occurrences across project. | References |
| **Inspections** | `CompactDuplicateInspection` | `LocalInspectionTool` | Real-time static analysis and linting. | PSI / Scopes |
| **Formatting** | `CompactFormattingBuilder` | `FormattingModelBuilder` | Formats code spaces and indentation. | AST / Tokens |
| **Brace Matching** | `CompactPairedBraceMatcher` | `PairedBraceMatcher` | Highlights matching `{ }`, `( )`, `[ ]`.| Tokens |
| **Comment Support** | `CompactCommenter.java` | `Commenter` | Supports <kbd>Ctrl+/</kbd> comments. | Tokens |
| **Structure View** | `CompactStructureViewFactory` | `PsiStructureViewFactory` | Hierarchy tree in Structure tool window. | PSI |
| **Quick Doc** | `CompactDocumentationProvider`| `DocumentationProvider` | Renders <kbd>Ctrl+Q</kbd> documentation popup. | PSI |

---

# 26. Recommended Implementation Order from Zero

When starting from an empty project, follow this **dependency-driven roadmap**:

```text
Phase 1: Foundation (Tokens & Highlighting)
  1. CompactLanguage & CompactFileType
  2. CompactTokenTypes & CompactLexer
  3. CompactSyntaxHighlighter & CompactColorSettingsPage
  ==> Result: Your .compact files open in IntelliJ with beautiful syntax coloring!

Phase 2: Grammar & PSI Tree
  4. CompactElementTypes & CompactParser
  5. CompactParserDefinition & CompactElementFactory
  6. CompactFile & Base PSI Classes (CompactCircuitDefinition, etc.)
  ==> Result: IntelliJ now understands the full structural tree of your code!

Phase 3: Basic Editor Quality of Life
  7. CompactPairedBraceMatcher (Matching braces)
  8. CompactCommenter (Ctrl+/ commenting)
  9. CompactFormattingModelBuilder (Code reformatting)
  ==> Result: Editing feels clean and native!

Phase 4: Semantics, Navigation & Refactoring
  10. CompactNamedElement & CompactResolveUtil
  11. CompactReferenceContributor (Ctrl+Click Go To Definition!)
  12. CompactRefactoringSupportProvider (Shift+F6 Rename!)
  13. CompactStructureViewFactory (File structure navigation)
  ==> Result: Full IDE navigation works seamlessly!

Phase 5: Productivity & Validation
  14. CompactCompletionContributor & Insert Handlers (Auto-complete)
  15. Live Templates (Compact.xml)
  16. Local Inspections (10 linter checks)
  17. DocumentationProvider (Ctrl+Q tooltips)
  ==> Result: A professional-grade, battle-tested IntelliJ language plugin!
```

---

# 27. Decision Guide: "If I Want to Add a New Feature, What Do I Look For?"

Use this decision table whenever you want to add a capability:

```text
I want to...                                          IntelliJ Platform API / Extension Point
─────────────────────────────────────────────────────────────────────────────────────────────
• Color keywords and tokens fast                     ──> SyntaxHighlighter
• Color symbols based on what they resolve to        ──> Annotator
• Offer suggestions as the user types                ──> CompletionContributor
• Expand a snippet after typing a prefix + Tab       ──> Live Template (<defaultLiveTemplates>)
• Navigate to where a function/variable is defined   ──> PsiReference + PsiReferenceContributor
• Allow the user to rename a symbol project-wide     ──> PsiNamedElement + RefactoringSupportProvider
• Display an error/warning with a quick-fix bulb     ──> LocalInspectionTool + ProblemsHolder
• Adjust spacing and indentation on Ctrl+Alt+L       ──> FormattingModelBuilder + SpacingBuilder
• Highlight matching ( ) and { } brackets            ──> PairedBraceMatcher
• Toggle comments on Ctrl+/                          ──> Commenter
• Show functions and classes in the Structure popup  ──> PsiStructureViewFactory
• Show parameter hints when typing function calls    ──> ParameterInfoHandler
• Show type hints inline in the editor               ──> InlayHintsProvider
• Show documentation on Ctrl+Q                       ──> DocumentationProvider
• Add a gutter icon next to runnable functions       ──> LineMarkerProvider
```

---

# 28. Final "Build a Plugin from Zero" Checklist

Use this interactive checklist as your roadmap when building your language plugin:

- [ ] **Step 1: Language & File Type** ([Section 3](#3-language-registration-telling-intellij-the-language-exists) & [Section 4](#4-file-type-registration-associating-files-with-your-language))
  - [ ] Implement `CompactLanguage`
  - [ ] Implement `CompactFileType`
  - [ ] Register `<fileType>` in `plugin.xml`
- [ ] **Step 2: Lexer & Tokens** ([Section 5](#5-the-lexer-converting-raw-characters-into-tokens) & [Section 6](#6-lexer-token-separation-a-deep-dive-for-beginners))
  - [ ] Define `CompactTokenType` and `CompactTokenTypes`
  - [ ] Group sets in `CompactTokenSets`
  - [ ] Implement `CompactLexer`
- [ ] **Step 3: Syntax Highlighting** ([Section 10](#10-syntax-and-semantic-highlighting))
  - [ ] Create `CompactSyntaxHighlighter` and factory
  - [ ] Create `CompactColorSettingsPage`
  - [ ] Register highlighter in `plugin.xml`
- [ ] **Step 4: Parser & AST** ([Section 7](#7-the-parser-converting-token-streams-into-grammatical-trees) & [Section 8](#8-the-ast-abstract-syntax-tree-intellijs-low-level-tree-structure))
  - [ ] Define `CompactElementTypes`
  - [ ] Implement `CompactParser` with `PsiBuilder`
  - [ ] Create `CompactParserDefinition`
- [ ] **Step 5: PSI Hierarchy** ([Section 9](#9-the-psi-program-structure-interface-semantic-tree-architecture))
  - [ ] Implement `CompactFile`
  - [ ] Implement `CompactNamedElement` & `CompactNamedElementImpl`
  - [ ] Implement typed PSI elements (`CompactCircuitDefinitionImpl`, etc.)
  - [ ] Implement `CompactElementFactory`
- [ ] **Step 6: Editor Polish** ([Sections 18](#18-code-formatting-and-indentation), [19](#19-brace-matching-and-delimiters), [20](#20-comment-support-and-typing-handlers))
  - [ ] Implement `CompactPairedBraceMatcher`
  - [ ] Implement `CompactCommenter`
  - [ ] Implement `CompactFormattingModelBuilder`
- [ ] **Step 7: References & Navigation** ([Sections 13](#13-references-and-go-to-definition) & [21](#21-structure-view-and-navigation))
  - [ ] Implement `CompactResolveUtil` (Scoping & Symbol Tables)
  - [ ] Implement `CompactReferenceBase`, `CompactValueReference`, `CompactTypeReference`
  - [ ] Implement `CompactReferenceContributor`
  - [ ] Implement `CompactStructureViewFactory`
- [ ] **Step 8: Refactoring & Rename** ([Sections 14](#14-rename-refactoring) & [15](#15-find-usages))
  - [ ] Implement `CompactRefactoringSupportProvider`
  - [ ] Implement `CompactNamesValidator`
  - [ ] Implement `CompactFindUsagesProvider`
- [ ] **Step 9: Completion & Live Templates** ([Sections 11](#11-code-completion) & [12](#12-code-templates-live-templates-and-declaration-expansion))
  - [ ] Implement `CompactCompletionContributor`
  - [ ] Implement `CompactDeclarationInsertHandler`
  - [ ] Create `/liveTemplates/Compact.xml`
- [ ] **Step 10: Inspections & Annotators** ([Sections 16](#16-inspections-and-quick-fixes) & [17](#17-annotators-on-the-fly-analysis-and-semantic-highlighting))
  - [ ] Implement `CompactHighlightingAnnotator`
  - [ ] Implement `LocalInspectionTool` classes with QuickFixes
  - [ ] Implement `CompactDocumentationProvider`

---
*Happy IntelliJ Plugin Development!*
