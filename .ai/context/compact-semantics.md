# Compact Language Semantics

This document records the verified semantic behavior of the Midnight Compact programming language based on the official compiler implementation (`compact/compiler/*`) and language specifications.

---

## 1. Environment & Scopes

### Lexical Scoping & Shadowing
- **VERIFIED**: Compact uses static lexical scoping. Inner block bindings (`const`, parameters, pattern bindings) shadow outer definitions within the same namespace.
- **Source**: `compact/compiler/infer-types.ss`, `references/lsrc.md` (Scope rules).
- **VERIFIED**: Namespace separation: Compact maintains distinct namespaces for `VALUE` (variables, constants, circuits, witnesses, constructor) and `TYPE` (structs, enums, type aliases, generic type parameters). A struct/type and a variable can share the same identifier name without collision.
- **Source**: `references/infer-types.ss`, `dev.verloren.midnight.resolve.CompactResolveUtil`.

---

## 2. Declarations & Top-Level Forms

### Pragmas
- **VERIFIED**: Pragmas define compiler flags and language versions at the top of the file: `pragma language_version >= 0.20.0;`.
- **Source**: `compact/compiler/parser.ss` (`pdecl`), `references/compact-grammar.mdx`.

### Contracts & Modules
- **VERIFIED**:
  - `contract C { ... }` or `contract C implements Interface { ... }` defines the top-level smart contract.
  - `module M { ... }` defines a scoped module that can contain nested declarations and can be exported.
- **Source**: `compact/compiler/parser.ss` (`mdefn`, `cdefn`), `references/compact-grammar.mdx`.

### Ledger State & Constructor
- **VERIFIED**:
  - `ledger { ... }` or `ledger x: Type;` declares persistent on-chain state. Fields can be marked `sealed`.
  - `constructor(...) { ... }` executes initialization logic when deploying the contract.
- **Source**: `compact/compiler/parser.ss` (`ldecl`, `lconstructor`), `references/check-sealed-fields.ss`.

### Circuits & Witnesses
- **VERIFIED**:
  - `circuit name(params): ReturnType { ... }` defines public ZK computation executing on-chain or verifiable via zk-SNARKs.
  - `witness name(params): ReturnType { ... }` defines private/client-side computations querying user-held private data.
  - Circuits can be marked `pure` or `export`.
- **Source**: `compact/compiler/parser.ss` (`cdefn`, `wdecl`), `compact/compiler/infer-types.ss`.

---

## 3. Types & Generics

### Primitive Types
- **VERIFIED**:
  - `Boolean`: `true` / `false`.
  - `Field`: Prime field elements used in arithmetic circuits.
  - `Uint`: Unsigned integers, optionally parameterized with bit width (e.g. `Uint<8>`, `Uint<32>`, `Uint<64>`).
  - `Bytes`: Fixed or variable byte sequences (e.g. `Bytes<32>`).
  - `Opaque<"name">`: Opaque values representing external or cryptographic handle types.
  - `Vector<T, N>`: Fixed-size vector of `N` elements of type `T`.
- **Source**: `compact/compiler/infer-types.ss`, `compact/compiler/langs.ss`.

### User-Defined Types
- **VERIFIED**:
  - Structs: `struct Point { x: Field, y: Field }` (nominal type, support field access `p.x` and struct instantiation `Point { x: 1, y: 2 }` or positional syntax).
  - Enums: `enum Color { Red, Green, Blue }` (nominal type, member access via `Color.Red`).
  - Type Aliases: `type Hash = Bytes<32>;`.
- **Source**: `compact/compiler/parser.ss` (`sdecl`, `edecl`, `tdecl`).

### Generics
- **VERIFIED**: Circuits, functions, and structs may declare type parameters: `<T, N>`.
- **Source**: `compact/compiler/parser.ss` (`type-params`).

---

## 4. Modules, Imports & Exports

### Imports
- **VERIFIED**:
  - Module aliasing with prefix: `import M prefix $;` makes exported members accessible via `$memberName`.
  - Specific member import: `import { a, b as c } from M;` brings symbols into the current scope with optional renaming.
  - File inclusion: `include "path.compact";` embeds another file's declarations.
- **Source**: `compact/compiler/parser.ss` (`idecl`, `incld`), `references/expand-modules-and-types.ss`.

### Exports
- **VERIFIED**:
  - `export { symbol1, symbol2 };` or `export circuit foo() { ... }` exports symbols from a module or contract.
- **Source**: `compact/compiler/parser.ss` (`xdecl`).

---

## 5. Expressions & Operators

### Precedence & Associativity
- **VERIFIED**: Compact uses a 12-level expression precedence hierarchy:
  1. Primary / Postfix (`.`, `()`, `[]`)
  2. Unary (`!`, `-`, `+`, `~`)
  3. Multiplicative (`*`, `/`, `%`)
  4. Additive (`+`, `-`)
  5. Shift / Bitwise (`<<`, `>>`, `&`, `^`, `|`)
  6. Relational (`<`, `<=`, `>`, `>=`)
  7. Equality (`==`, `!=`)
  8. Logical AND (`&&`)
  9. Logical OR (`||`)
  10. Ternary (`? :`)
  11. Cast (`as Type`)
  12. Assignment (`=`, `+=`, `-=`, etc.)
- **Source**: `compact/compiler/parser.ss` (`expr0` through `expr9`), `references/compact-grammar.mdx`.

### Privacy & Disclosure
- **VERIFIED**:
  - `disclose(expr)` explicitly reveals a private witness calculation to the public circuit context.
- **Source**: `compact/compiler/parser.ss`, `compact/compiler/infer-types.ss`.

---

## 6. Verification Status Summary

| Construct | Status | Source |
| :--- | :--- | :--- |
| Lexical scoping & local shadowing | **VERIFIED** | `compact/compiler/infer-types.ss` |
| Split `VALUE` / `TYPE` namespaces | **VERIFIED** | `compact/compiler/infer-types.ss` |
| Struct field & Enum member dot-access | **VERIFIED** | `compact/compiler/parser.ss`, `infer-types.ss` |
| `disclose(...)` expression | **VERIFIED** | `compact/compiler/parser.ss` |
| `as Type` casting semantics | **VERIFIED** | `compact/compiler/parser.ss` |
| Cross-file module import resolution mechanics | **INFERENCE** | `compact/compiler/expand-modules-and-types.ss` (single compilation unit expansion) |
| Standard library ZKIR intrinsics resolution | **INFERENCE** | `compact/compiler/standard-library.compact`, `zkir-v3-library.compact` |
| Dynamic dispatch / Virtual method tables | **UNKNOWN / NOT PRESENT** | Compact contracts are static; no dynamic dispatch found in AST. |
