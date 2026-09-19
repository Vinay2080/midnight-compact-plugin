# Path Rules: Modern Java 25 Idioms & Language Standards

**Scope:** `src/main/java/**`, `src/test/java/**`

## Invariants & Guardrails

The project toolchain is Java 25 (`JavaLanguageVersion.of(25)`). All code must use modern Java features and reject legacy pre-Java 17/21 idioms.

1. **Java Records for Data Carriers**:
   - Use `record` for immutable DTOs, type descriptors, AST pairs, and cache keys.
   - Prohibited: Boilerplate POJO classes with private final fields, manual getters, and manual `equals()`/`hashCode()`.

2. **Sequenced Collections (Java 21+)**:
   - Prohibited: `list.get(0)` or `list.get(list.size() - 1)`.
   - Mandated: `list.getFirst()`, `list.getLast()`, `deque.getFirst()`, `deque.getLast()`, `collection.reversed()`.

3. **Pattern Matching & Type Dispatch (Java 16/21+)**:
   - Prohibited: Manual downcasting `if (x instanceof Foo) { Foo f = (Foo) x; }`.
   - Mandated: Pattern matching `if (x instanceof Foo f) { ... }`.
   - Mandated: Pattern matching switch expressions with arrow syntax for AST/token dispatch.

4. **Unnamed Variables (`_`) (Java 22+)**:
   - For unused exception variables or lambda arguments, use `_` (e.g. `catch (NumberFormatException _) { return null; }`).
   - Prohibited: Declaring named variables that remain unused.

5. **Multi-line Text Blocks (`"""..."""`)**:
   - Prohibited: Manual string concatenation with `\n` and `+` for multi-line strings, test fixtures, or HTML/XML inspection descriptions.
   - Mandated: Standard multi-line text blocks.

6. **Immutable Collection Factories**:
   - Prohibited: Instantiating mutable collections and chaining `.add()` immediately, or wrapping with `Collections.unmodifiableList(...)`.
   - Mandated: `List.of(...)`, `Set.of(...)`, `Map.of(...)`, `Map.ofEntries(...)`.
