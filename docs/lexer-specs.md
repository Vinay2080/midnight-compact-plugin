# Compact Lexer Specification

## Keywords

### Module

- export
- from
- import
- module
- prefix

### Declarations & Statements

- assert
- as
- circuit
- const
- constructor
- contract
- default
- disclose
- else
- enum
- fold
- for
- if
- include
- ledger
- map
- new
- of
- pad
- pragma
- pure
- return
- sealed
- slice
- struct
- type
- witness

---

## Built-in Types

### Primitive Types

- Boolean
- Bytes
- Field
- Opaque
- Uint
- Vector

---

## Standard Library ADTs

- Counter
- HistoricMerkleTree
- Kernel
- List
- Map
- MerkleTree
- Set

> Note: These are language-provided ADTs. The grammar does not explicitly state whether they are dedicated lexer tokens
> or ordinary identifiers resolved by the compiler.

## Literals

### Boolean

- true
- false

### Numeric

- Decimal (0, 1, 42, ...)
- Binary (0b1010, 0B1010)
- Octal (0o755, 0O755)
- Hexadecimal (0xFF, 0XFF)

### String

- Single-quoted ('...')
- Double-quoted ("...")
- Supports TypeScript escape sequences.
- Newlines inside string literals are not allowed

---

## Reserved Keywords

- await
- break
- case
- catch
- class
- continue
- debugger
- delete
- do
- extends
- finally
- function
- implements
- in
- instanceof
- interface
- let
- null
- package
- private
- protected
- public
- static
- super
- switch
- this
- throw
- try
- typeof
- var
- void
- while
- with
- yield

---

## Operators

### Assignment

- =
- +=
- -=

### Arithmetic

-

'+'

- '-'
- '*'
- /
- %

### Comparison

- ==
- !=
- <
- <=
- '>'
- '>='

---

### Arrow

- =>

---

### Logical

- !
- &&
- ||

### Range

- ..

### Member Access

- .

### Spread

- ...

### Ternary

- ?
- :

---

## Delimiters

- (
- )
- {
- }
- [
- ]
- ,
- ;
- :
- #

---

## Identifiers

Identifiers use the same syntax as TypeScript identifiers.

Rules:

- First character: letter, '_' or '$'
- Remaining characters: letters, digits, '_' or '$'
- Identifiers beginning with '__compact' are reserved by the compiler.

---

## Comments

- Single-line: // ...
- Block: /* ... */
- Nested block comments are not allowed.

---

## Whitespace

- Space
- Tab
- Newline
- Carriage Return

---

### Version literals

- 1
- 1.2
- 1.2.7

---

## Bad Character

Any unexpected character.