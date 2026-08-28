# Midnight Compact Language Plugin — Manual Testing & QA Guide

This document is the **Comprehensive Manual Testing and Quality Assurance (QA) Guide** for the **Midnight Compact Language Plugin for IntelliJ IDEA** (`dev.verloren.midnight`).

It provides **exact inputs**, **actions to perform**, **expected visual outputs**, and **verification checklists** for every single IDE feature. Use this guide to manually test every subsystem in a live sandbox IDE, discover edge cases, verify fixes, and validate 100% feature coverage.

---

## Table of Contents
1. [How to Launch the Test Sandbox IDE](#1-how-to-launch-the-test-sandbox-ide)
2. [Subsystem 1: Syntax Highlighting & Token Coloring](#2-subsystem-1-syntax-highlighting--token-coloring)
3. [Subsystem 2: Navigation & References (Go-to-Declaration: `Ctrl + Click`)](#3-subsystem-2-navigation--references-go-to-declaration-ctrl--click)
4. [Subsystem 3: Safe Rename Refactoring (`Shift + F6`)](#4-subsystem-3-safe-rename-refactoring-shift--f6)
5. [Subsystem 4: Find Usages (`Alt + F7`)](#5-subsystem-4-find-usages-alt--f7)
6. [Subsystem 5: Contextual Code Completion (`Ctrl + Space`)](#6-subsystem-5-contextual-code-completion-ctrl--space)
7. [Subsystem 6: Real-Time Semantic & Compiler Inspections (`Alt + Enter`)](#7-subsystem-6-real-time-semantic--compiler-inspections-alt--enter)
8. [Subsystem 7: Code Formatting & Smart Indentation (`Ctrl + Alt + L`)](#8-subsystem-7-code-formatting--smart-indentation-ctrl--alt--l)
9. [Subsystem 8: Code Folding, Breadcrumbs, Structure View & Quick Docs](#9-subsystem-8-code-folding-breadcrumbs-structure-view--quick-docs)
10. [Subsystem 9: Live Templates & New File Actions](#10-subsystem-9-live-templates--new-file-actions)
11. [Subsystem 10: Search Everywhere (`Ctrl + N` / `Double Shift`)](#11-subsystem-10-search-everywhere-ctrl--n--double-shift)
12. [Subsystem 11: Bundled Standard Library & ZKIR Indexing](#12-subsystem-11-bundled-standard-library--zkir-indexing)
13. [Subsystem 12: Inlay Parameter Name Hints](#13-subsystem-12-inlay-parameter-name-hints)
14. [Subsystem 13: Compiler Run Configurations & Gutter Play Buttons](#14-subsystem-13-compiler-run-configurations--gutter-play-buttons)
15. [Subsystem 14: Midnight Plugin Settings Page](#15-subsystem-14-midnight-plugin-settings-page)
16. [Subsystem 15: Background Compiler Diagnostics (`compactc`)](#16-subsystem-15-background-compiler-diagnostics-compactc)
17. [Subsystem 16: Semantic Gutter Line Markers (Privacy & Circuit Visualizer)](#17-subsystem-16-semantic-gutter-line-markers-privacy--circuit-visualizer)
18. [Subsystem 17: Master End-to-End Test Contract](#18-subsystem-17-master-end-to-end-test-contract)
19. [QA Manual Test Execution Checklist Table](#19-qa-manual-test-execution-checklist-table)



---

## 1. How to Launch the Test Sandbox IDE

### 1.1 Starting the Sandbox
1. In the terminal / PowerShell at project root (`C:\Users\shaki\IdeaProjects\midnight-plugin`), execute:
   ```bash
   ./gradlew runIde
   ```
   *(Or click the Gradle tool window on the right $\to$ `Tasks` $\to$ `intellij` $\to$ `runIde`)*
2. A new standalone instance of IntelliJ IDEA (Sandbox instance) will launch with the Midnight plugin pre-installed.
3. In the Sandbox IDE:
   - Click **New Project** $\to$ choose any location (e.g. `C:\Users\shaki\IdeaProjects\CompactTestProject`).
   - Create a source folder or root directory.
   - Right-click the folder $\to$ **New** $\to$ **Compact File** $\to$ name it `test.compact`.

### 1.2 Inspecting / Adjusting Color Scheme
- Open **Settings** (`Ctrl + Alt + S`) $\to$ **Editor** $\to$ **Color Scheme** $\to$ **Compact**.
- Verify that 42+ distinct token/semantic categories appear in the settings page (Keywords, Pragma, Types, Structs, Enums, Circuits, Witnesses, Numbers, Comments, Escape Sequences, etc.).

---

## 2. Subsystem 1: Syntax Highlighting & Token Coloring

### 2.1 Pragma Directives & Versions
#### **Input Code Snippet:**
```compact
pragma language_version >= 0.20.0;
pragma compiler_version ^1.2.3;
```
#### **Expected Output & Visual Behavior:**
- `pragma` is highlighted in **Keyword / Metadata** color (e.g., Bold Orange/Purple depending on theme).
- `language_version` and `compiler_version` are highlighted as language keywords/identifiers.
- `>=` and `^` are highlighted as **Operators**.
- `0.20.0` and `1.2.3` are highlighted in **Number / Version** color (e.g., Cyan/Blue).
- Semicolons `;` are highlighted as punctuation.

---

### 2.2 Keywords, Modifiers & Declarations
#### **Input Code Snippet:**
```compact
export contract TokenContract {
    sealed ledger balance: Uint<64>;
    
    constructor(initialSupply: Uint<64>) {
        balance = initialSupply;
    }
    
    export pure circuit getBalance(): Uint<64> {
        return balance;
    }
    
    witness privateKey(): Bytes<32>;
}
```
#### **Expected Output & Visual Behavior:**
- **Keywords / Modifiers**: `export`, `contract`, `sealed`, `ledger`, `constructor`, `pure`, `circuit`, `witness`, `return` are highlighted as **Bold Keywords**.
- **Contract Name**: `TokenContract` is highlighted in **Class Name** style.
- **Circuit / Witness Names**: `getBalance` and `privateKey` are highlighted in **Function Declaration** style (e.g., Yellow/Green).
- **Ledger Field**: `balance` is highlighted in **Global / Ledger Variable** style.
- **Parameters**: `initialSupply` is highlighted in **Parameter** style.

---

### 2.3 Built-in Primitive Types vs Nominal User Types
#### **Input Code Snippet:**
```compact
struct Coordinate {
    x: Field,
    y: Uint<32>,
    flag: Boolean,
    data: Bytes<16>,
    secret: Opaque<"salt">
}

enum Status {
    Pending,
    Approved,
    Rejected
}

type BalanceMap = Vector<Uint<64>, 10>;
```
#### **Expected Output & Visual Behavior:**
- **Built-in Types**: `Field`, `Uint`, `Boolean`, `Bytes`, `Opaque`, `Vector` are highlighted in **Builtin Type / Keyword** color.
- **Struct Name**: `Coordinate` is highlighted in **Struct / Class** color.
- **Enum Name**: `Status` is highlighted in **Enum / Class** color.
- **Enum Members**: `Pending`, `Approved`, `Rejected` are highlighted in **Static Field / Constant** color.
- **Type Alias**: `BalanceMap` is highlighted in **Interface / Type Alias** color.

---

### 2.4 Multi-Base Numeric Literals & Strings
#### **Input Code Snippet:**
```compact
circuit literalsTest(): Void {
    const decimalNum = 12345;
    const hexNum = 0x1A2F;
    const binaryNum = 0b101001;
    const octalNum = 0o755;
    const str = "Valid escape: \n \t \" \\ \0";
    const flagTrue = true;
    const flagFalse = false;
}
```
#### **Expected Output & Visual Behavior:**
- `12345`, `0x1A2F`, `0b101001`, `0o755` are all parsed as **Numbers** (cyan/blue).
- String `"Valid escape: ..."` is rendered as **String** (green), with `\n`, `\t`, `\"`, `\\` specifically highlighted with **Valid Escape Sequence** styling.
- `true` and `false` are highlighted as **Boolean Keywords**.

---

### 2.5 Comments & Documentation Blocks
#### **Input Code Snippet:**
```compact
// Single-line comment: explaining state

/* Multi-line block comment
   spanning multiple lines */

/**
 * Doc comment for circuit
 * @param amount transfer amount
 * @return success status
 */
circuit transfer(amount: Uint<32>): Boolean {
    return true;
}
```
#### **Expected Output & Visual Behavior:**
- `// ...` renders in **Line Comment** color (grey/italic).
- `/* ... */` renders in **Block Comment** color.
- `/** ... */` renders in **Doc Comment** color; `@param` and `@return` render as **Doc Comment Tags**.

---

## 3. Subsystem 2: Navigation & References (Go-to-Declaration: `Ctrl + Click`)

### 3.1 Local Variables & Constants
#### **Input Code Snippet:**
```compact
circuit calc(a: Field): Field {
    const factor = 10;
    const result = a * factor;
    return result;
}
```
#### **Action to Perform:**
1. Hold `Ctrl` (or `Cmd` on macOS) and hover over `factor` on line 3 (`a * factor`).
2. Notice `factor` turns into a clickable blue hyperlink with a declaration preview tooltip.
3. Click `factor`.
#### **Expected Output:**
- Editor caret immediately jumps to `const factor = 10;` on line 2.
- Testing `result` in `return result;` jumps to `const result = ...;` on line 3.

---

### 3.2 Function Parameters
#### **Input Code Snippet:**
```compact
circuit verifyUser(userId: Uint<32>, userKey: Bytes<32>): Boolean {
    const id = userId;
    return id > 0;
}
```
#### **Action to Perform:**
- `Ctrl + Click` on `userId` on line 2 (`const id = userId;`).
#### **Expected Output:**
- Caret jumps directly to `userId: Uint<32>` in the parameter list of `verifyUser`.

---

### 3.3 Function / Circuit / Witness Invocations
#### **Input Code Snippet:**
```compact
witness generateNonce(): Field;

circuit computeHash(x: Field): Field {
    return x + 1;
}

circuit main(): Field {
    const n = generateNonce();
    const h = computeHash(n);
    return h;
}
```
#### **Action to Perform:**
1. `Ctrl + Click` on `generateNonce()` inside `main()`.
   - **Expected Result**: Caret jumps to `witness generateNonce(): Field;`.
2. `Ctrl + Click` on `computeHash(n)` inside `main()`.
   - **Expected Result**: Caret jumps to `circuit computeHash(x: Field): Field` declaration.

---

### 3.4 Struct Fields & Enum Members
#### **Input Code Snippet:**
```compact
struct UserProfile {
    id: Uint<64>,
    isActive: Boolean
}

enum Role {
    Admin,
    Member
}

circuit check(u: UserProfile, r: Role): Boolean {
    const active = u.isActive;
    const isAdm = r == Role.Admin;
    return active && isAdm;
}
```
#### **Action to Perform:**
1. `Ctrl + Click` on `.isActive` in `u.isActive`.
   - **Expected Result**: Caret jumps to `isActive: Boolean` inside `struct UserProfile`.
2. `Ctrl + Click` on `Admin` in `Role.Admin`.
   - **Expected Result**: Caret jumps to `Admin` inside `enum Role`.

---

### 3.5 Cross-File Resolution (`include` Directives)
#### **Setup:**
Create two files in the same directory:

**File 1: `types.compact`**
```compact
export struct Account {
    owner: Field,
    balance: Uint<64>
}
```

**File 2: `main.compact`**
```compact
include "./types.compact";

circuit process(acc: Account): Uint<64> {
    return acc.balance;
}
```
#### **Action to Perform:**
1. `Ctrl + Click` on `"./types.compact"` inside the `include` statement.
   - **Expected Result**: IDE opens `types.compact`.
2. In `main.compact`, `Ctrl + Click` on `Account` type in `acc: Account`.
   - **Expected Result**: IDE navigates to `struct Account` inside `types.compact`.
3. `Ctrl + Click` on `.balance` in `acc.balance`.
   - **Expected Result**: IDE navigates to `balance: Uint<64>` in `types.compact`.

---

## 4. Subsystem 3: Safe Rename Refactoring (`Shift + F6`)

### 4.1 Renaming a Circuit across Declarations and Usages
#### **Initial Code:**
```compact
circuit mintToken(amount: Uint<32>): Void {
    // minting logic
}

circuit execute(): Void {
    mintToken(100);
}
```
#### **Action to Perform:**
1. Place caret on `mintToken` in the declaration.
2. Press `Shift + F6` (or Right-Click $\to$ **Refactor** $\to$ **Rename**).
3. Type `issueToken` and press **Enter**.
#### **Expected Output:**
```compact
circuit issueToken(amount: Uint<32>): Void {
    // minting logic
}

circuit execute(): Void {
    issueToken(100);
}
```
*Both the declaration and the call site in `execute()` are updated simultaneously.*

---

### 4.2 Renaming a Function Parameter
#### **Initial Code:**
```compact
circuit transfer(recipient: Field, amount: Uint<64>): Void {
    const a = amount;
    const b = amount + 10;
}
```
#### **Action to Perform:**
1. Place caret on `amount` in `circuit transfer(...)`.
2. Press `Shift + F6`, type `tokensToTransfer`, and press **Enter**.
#### **Expected Output:**
```compact
circuit transfer(recipient: Field, tokensToTransfer: Uint<64>): Void {
    const a = tokensToTransfer;
    const b = tokensToTransfer + 10;
}
```

---

### 4.3 Renaming a Struct and Struct Usages
#### **Initial Code:**
```compact
struct Point {
    x: Field,
    y: Field
}

circuit draw(p: Point): Field {
    const p2: Point = p;
    return p2.x;
}
```
#### **Action to Perform:**
1. Place caret on `Point` in `struct Point`.
2. Press `Shift + F6`, type `Vector2D`, and press **Enter**.
#### **Expected Output:**
```compact
struct Vector2D {
    x: Field,
    y: Field
}

circuit draw(p: Vector2D): Field {
    const p2: Vector2D = p;
    return p2.x;
}
```

---

### 4.4 Name Validation (Keyword & Invalid Name Rejection)
#### **Action to Perform:**
1. Place caret on any variable (e.g. `p2`).
2. Press `Shift + F6`, type `circuit` (a reserved keyword) or `123bad` (invalid identifier), and press Enter.
#### **Expected Output:**
- IntelliJ displays an error dialog/tooltip: **"Identifier is invalid or is a reserved Compact keyword"** and prevents the renaming.

---

## 5. Subsystem 4: Find Usages (`Alt + F7`)

### 5.1 Finding Usages of a Circuit / Witness / Variable
#### **Input Code Snippet:**
```compact
circuit calculateInterest(principal: Uint<64>): Uint<64> {
    return principal * 5 / 100;
}

circuit firstCaller(): Uint<64> {
    return calculateInterest(1000);
}

circuit secondCaller(): Uint<64> {
    const p = 2000;
    return calculateInterest(p);
}
```
#### **Action to Perform:**
1. Place caret on `calculateInterest` declaration.
2. Press `Alt + F7` (or Right-Click $\to$ **Find Usages**).
#### **Expected Output:**
- A **Find Usages** tool window appears at the bottom of IntelliJ IDEA.
- The results list:
  - `firstCaller()` $\to$ `calculateInterest(1000)` (Call / Usage)
  - `secondCaller()` $\to$ `calculateInterest(p)` (Call / Usage)
- Double-clicking any usage navigates instantly to that location in the code editor.

---

## 6. Subsystem 5: Contextual Code Completion (`Ctrl + Space`)

### 6.1 Top-Level Keyword Completion
#### **Input Code Snippet:**
```compact
con<caret>
```
#### **Action to Perform:**
- Press `Ctrl + Space`.
#### **Expected Output:**
- Autocomplete popup appears proposing `contract`.
- Typing `c` proposes `circuit`, `const`, `contract`, `constructor`.
- Typing `w` proposes `witness`.
- Typing `s` proposes `struct`, `sealed`.
- Typing `p` proposes `pragma`, `pure`, `pad`.

---

### 6.2 Type Annotation Autocomplete
#### **Input Code Snippet:**
```compact
struct MyData {
    id: <caret>
}
```
#### **Action to Perform:**
- Press `Ctrl + Space` at `<caret>`.
#### **Expected Output:**
- Suggestions include built-in primitive types: `Boolean`, `Field`, `Uint`, `Bytes`, `Vector`, `Opaque`, `JubjubScalar`, `Secp256k1Base`, `Secp256k1Scalar`.
- If user structs or enums exist in the file, they are also suggested at the top with class icons.

---

### 6.3 Expression Scope Autocomplete (Variables & Parameters)
#### **Input Code Snippet:**
```compact
circuit test(secretKey: Field, publicAddress: Uint<32>): Field {
    const multiplier = 5;
    return <caret>
}
```
#### **Action to Perform:**
- Press `Ctrl + Space` after `return `.
#### **Expected Output:**
- Lookup list contains in-scope elements:
  - `secretKey` (Parameter)
  - `publicAddress` (Parameter)
  - `multiplier` (Local variable)
  - `test` (Enclosing circuit)

---

### 6.4 Member Dot-Completion (Struct Fields & Enum Variants)
#### **Input Code Snippet:**
```compact
struct Config {
    timeout: Uint<32>,
    retries: Uint<8>
}

enum State {
    Idle,
    Running,
    Finished
}

circuit run(cfg: Config): Void {
    const t = cfg.<caret>
    const s = State.<caret>
}
```
#### **Action to Perform:**
1. Place caret after `cfg.` and trigger completion (`Ctrl + Space`).
   - **Expected Result**: Suggests `timeout` and `retries`.
2. Place caret after `State.` and trigger completion (`Ctrl + Space`).
   - **Expected Result**: Suggests `Idle`, `Running`, `Finished`.

---

## 7. Subsystem 6: Real-Time Semantic Inspections & Quick-Fixes (`Alt + Enter`)

### 7.1 Unresolved Reference Inspection
#### **Input Code Snippet:**
```compact
circuit run(): Void {
    const x = nonexistentVariable;
}
```
#### **Expected Output & Visual Behavior:**
- `nonexistentVariable` is highlighted with a **Warning squiggle** (yellow/red).
- Hovering over it displays tooltip: `"Unresolved reference 'nonexistentVariable'"`.

---

### 7.2 Duplicate Declaration Inspection
#### **Input Code Snippet:**
```compact
circuit duplicateTest(): Void {
    const duplicateName = 1;
    const duplicateName = 2;
}
```
#### **Expected Output & Visual Behavior:**
- The second `duplicateName` is highlighted with a warning: `"Duplicate declaration 'duplicateName'"`.
- Declaring two circuits with the same name in the same file produces a duplicate declaration warning on both.

---

### 7.3 Unused Local Variable Inspection & Quick-Fix
#### **Input Code Snippet:**
```compact
circuit unusedTest(): Void {
    const unusedBinding = 42;
    const usedBinding = 10;
    const total = usedBinding + 5;
}
```
#### **Action to Perform:**
1. Notice `unusedBinding` is highlighted as unused / greyed out with warning: `"Unused local variable 'unusedBinding'"`.
2. Place caret on `unusedBinding` and press `Alt + Enter`.
3. Select **"Remove unused variable 'unusedBinding'"** and press **Enter**.
#### **Expected Output:**
- The line `const unusedBinding = 42;` is safely deleted automatically:
```compact
circuit unusedTest(): Void {
    const usedBinding = 10;
    const total = usedBinding + 5;
}
```

---

### 7.4 Type Mismatch Inspection
#### **Input Code Snippet:**
```compact
circuit typeTest(): Void {
    if (12345) {
        // invalid non-boolean condition
    }
}
```
#### **Expected Output & Visual Behavior:**
- `12345` inside the `if` condition is underlined with a warning: `"Type mismatch: condition requires Boolean, found CompactNumericLiteralType"`.

---

### 7.5 Pure Circuit Invariants & Quick-Fix (`Alt + Enter`)
#### **Input Code Snippet:**
```compact
ledger state: Field;
witness secret(): Field;

pure circuit compute(): Field {
    return secret();
}
```
#### **Action to Perform:**
1. Notice `secret()` is underlined with warning: `"Pure circuit 'compute' cannot call private witness 'secret'"`.
2. Place caret on `secret()` and press `Alt + Enter`.
3. Select **"Remove 'pure' modifier"** and press **Enter**.
#### **Expected Output:**
- The modifier `pure` is removed from `circuit compute(): Field`, turning it into a valid regular circuit:
```compact
circuit compute(): Field {
    return secret();
}
```

---

### 7.6 Sealed Ledger Field Mutation Inspection
#### **Input Code Snippet:**
```compact
sealed ledger root: Field;

constructor(initialRoot: Field) {
    root = initialRoot; // OK inside constructor
}

circuit updateRoot(newRoot: Field): [] {
    root = newRoot; // Illegal outside constructor
}
```
#### **Expected Output & Visual Behavior:**
- `root = newRoot;` inside `updateRoot` is underlined with warning: `"Cannot mutate sealed ledger field 'root' outside constructor"`.

---

### 7.7 Recursive Circuit Inspection
#### **Input Code Snippet:**
```compact
circuit fibonacci(n: Field): Field {
    return fibonacci(n);
}
```
#### **Expected Output & Visual Behavior:**
- `fibonacci(n)` is underlined with warning: `"Circuit 'fibonacci' cannot be recursive; recursion is forbidden in ZK circuits"`.
- Mutual recursion cycles (e.g. `circuitA` $\to$ `circuitB` $\to$ `circuitA`) are similarly detected and flagged.

---

### 7.8 Constructor Restrictions Inspection
#### **Input Code Snippet:**
```compact
constructor() {
    emit myEvent(123);
}
```
#### **Expected Output & Visual Behavior:**
- `emit myEvent(123)` is underlined with warning: `"Constructors cannot emit events; events can only be emitted during circuit transactions"`.

---

### 7.9 Undisclosed Witness Protection (WPP) & Quick-Fix (`Alt + Enter`)
#### **Input Code Snippet:**
```compact
ledger authority: Field;
witness getSecretKey(): Field;

circuit setAuthority(): [] {
    authority = getSecretKey();
}
```
#### **Action to Perform:**
1. Notice `getSecretKey()` is underlined with warning: `"Private witness data cannot be assigned to ledger state without 'disclose(...)'"`.
2. Place caret on `getSecretKey()` and press `Alt + Enter`.
3. Select **"Wrap with disclose(...)"** and press **Enter**.
#### **Expected Output:**
- The assignment is automatically wrapped:
```compact
circuit setAuthority(): [] {
    authority = disclose(getSecretKey());
}
```


---

## 8. Subsystem 7: Code Formatting & Smart Indentation (`Ctrl + Alt + L`)

### 8.1 AST Block Tree Formatting
#### **Input (Unaligned / Messy Code):**
```compact
contract  MessyContract{
ledger  counter:Uint<64>;
constructor( initVal:Uint<64> ){
counter=initVal;
}
circuit add( a:Field,b:Field ):Field{
const result=a+b*2;
return result;
}
}
```
#### **Action to Perform:**
- Press `Ctrl + Alt + L` (Code $\to$ **Reformat Code**).
#### **Expected Output (Properly Formatted):**
```compact
contract MessyContract {
    ledger counter: Uint<64>;

    constructor(initVal: Uint<64>) {
        counter = initVal;
    }

    circuit add(a: Field, b: Field): Field {
        const result = a + b * 2;
        return result;
    }
}
```
*Notice spaces after commas, around binary operators (`=`, `+`, `*`), before opening braces `{`, and 4-space block indentation.*

---

### 8.2 Smart Indentation on `Enter`
#### **Action to Perform:**
1. Type:
   ```compact
   circuit test(): Void {<caret>
   ```
2. Press **`Enter`**.
#### **Expected Output:**
- The editor automatically indents by 4 spaces on the new line:
  ```compact
  circuit test(): Void {
      <caret>
  }
  ```

---

## 9. Subsystem 8: Code Folding, Breadcrumbs, Structure View & Quick Docs

### 9.1 Code Folding (`Ctrl + NumPad -` / `Ctrl + NumPad +`)
#### **Input Code Snippet:**
```compact
contract FoldTest {
    circuit longCircuit() {
        const a = 1;
        const b = 2;
        const c = 3;
    }
}
```
#### **Action to Perform:**
- Click the `-` collapse icon in the editor gutter next to `contract FoldTest` or `circuit longCircuit()`.
#### **Expected Output:**
- Block collapses neatly into `contract FoldTest { ... }` or `circuit longCircuit() { ... }`.
- Gutter icon changes to `+`. Clicking `+` unfolds the code block.

---

### 9.2 Breadcrumbs Navigation
#### **Action to Perform:**
- Place caret inside a nested statement block inside a circuit inside a contract.
- Look at the breadcrumb bar at the top or bottom of the editor.
#### **Expected Output:**
- Displays hierarchy: `ContractName > circuitName > if (...)`. Clicking any breadcrumb element selects the corresponding PSI node.

---

### 9.3 Structure View Tool Window (`Alt + 7` / `Ctrl + F12`)
#### **Input Code Snippet:**
```compact
contract Bank {
    ledger totalDeposits: Uint<64>;
    
    struct Account {
        id: Field,
        bal: Uint<64>
    }
    
    enum Status { Open, Closed }
    
    constructor() {}
    
    export circuit deposit(amount: Uint<64>): Void {}
    
    witness fetchSecret(): Bytes<32>;
}
```
#### **Action to Perform:**
- Open **Structure Tool Window** (`Alt + 7` or View $\to$ Tool Windows $\to$ Structure).
#### **Expected Output:**
- A tree hierarchy appears showing:
  - `Bank` (Contract Icon)
    - `totalDeposits` (Ledger Variable Icon)
    - `Account` (Struct Icon)
      - `id` (Field Icon)
      - `bal` (Field Icon)
    - `Status` (Enum Icon)
      - `Open` (Member Icon)
      - `Closed` (Member Icon)
    - `constructor` (Method Icon)
    - `deposit` (Circuit Icon)
    - `fetchSecret` (Witness Icon)
- Clicking any item navigates immediately to its line in the editor.

---

### 9.4 Quick Documentation (`Ctrl + Q` / Hover)
#### **Action to Perform:**
1. Hover over `circuit` keyword $\to$ Tooltip renders Compact language documentation for circuits.
2. Hover over `Field` $\to$ Tooltip renders documentation explaining finite field elements in Compact.
3. Hover over `disclose` $\to$ Tooltip explains the ZK disclosure boundary from private witness to circuit.
4. Hover over `deposit` $\to$ Tooltip displays signature `circuit deposit(amount: Uint<64>): Void`.

---

## 10. Subsystem 9: Live Templates & New File Actions

### 10.1 Live Templates (Abbreviation + `Tab`)

| Template Abbreviation | Action | Expanded Code Result |
| :--- | :--- | :--- |
| `cir` + `Tab` | Inserts circuit skeleton | `export circuit circuitName(): Void { ... }` |
| `wit` + `Tab` | Inserts witness skeleton | `witness witnessName(): Field;` |
| `cct` + `Tab` | Inserts full contract | `export contract Contract { circuit main(): Void; } ledger { ... } constructor() { ... } export circuit main(): Void { ... }` |
| `ccti` + `Tab` | Contract implements | `export contract implements Interface; export circuit main(): Void { ... }` |
| `mod` + `Tab` | Module skeleton | `export module MyModule { ... }` |
| `led` + `Tab` | Ledger block | `ledger { state: Field; }` |
| `ledg` + `Tab`| Ledger field | `export ledger balance: Cell<Field>;` |
| `str` + `Tab` | Struct skeleton | `struct MyStruct { field1: Field; }` |
| `en` + `Tab`  | Enum skeleton | `enum MyEnum { A, B }` |
| `ass` + `Tab` | Assert statement | `assert true, "Assertion failed";` |
| `disc` + `Tab`| Disclose statement | `disclose(value);` |
| `inc` + `Tab` | Include statement | `include "./types.compact";` |
| `type` + `Tab`| Type alias | `type MyType = Field;` |

---

### 10.2 New File Templates
#### **Action to Perform:**
1. Right-click any folder in the Project view $\to$ **New** $\to$ **Compact File**.
2. A popup opens with template options:
   - `Compact Contract`
   - `Compact Interface`
   - `Compact Module`
   - `Compact File` (Empty)
3. Enter name `MyContract` $\to$ press Enter.
#### **Expected Output:**
- A new file `MyContract.compact` is created containing the standard Compact contract skeleton with proper syntax highlighting.

---

---

## 11. Subsystem 10: Search Everywhere (`Ctrl + N` / `Double Shift`)

### 11.1 Navigate to Class / Type (`Ctrl + N`)
#### **Input Code Snippet:**
```compact
contract EscrowContract {}
module EscrowLib {}
struct EscrowAccount { id: Field }
enum EscrowState { Created, Completed }
type EscrowId = Bytes<32>;
```
#### **Action to Perform:**
1. Press `Ctrl + N` (or `Double Shift` and switch to the **Classes** tab).
2. Type `Escrow`.
#### **Expected Output:**
- The Search Everywhere popup displays all matching Compact types with distinct icons:
  - `EscrowContract` (Class Icon)
  - `EscrowLib` (Module Icon)
  - `EscrowAccount` (Struct / Record Icon)
  - `EscrowState` (Enum Icon)
  - `EscrowId` (Type Icon)
- Selecting any item jumps directly to its declaration.

---

### 11.2 Navigate to Symbol (`Ctrl + Alt + Shift + N`)
#### **Input Code Snippet:**
```compact
ledger escrowBalance: Uint<64>;
witness fetchSigner(): Bytes<32>;
circuit releaseFunds(): [] {}
```
#### **Action to Perform:**
1. Press `Ctrl + Alt + Shift + N` (or `Double Shift` $\to$ **Symbols** tab).
2. Type `release` or `fetchSigner` or `escrowBalance`.
#### **Expected Output:**
- Displays matching declarations with icons and parent file indicators `(filename.compact)`:
  - `releaseFunds` (Method / Circuit Icon)
  - `fetchSigner` (Function / Witness Icon)
  - `escrowBalance` (Field / Ledger Icon)
- Pressing `Enter` navigates straight to the line.

---

## 12. Subsystem 11: Bundled Standard Library & ZKIR Indexing

### 12.1 Zero-Config Standard Library Auto-Complete & Go-To-Declaration
#### **Input Code Snippet:**
```compact
circuit testStdlib(): Maybe<Field> {
    return some(42);
}

circuit testZkir(): Boolean {
    const fn = secp256k1EcdsaVerify;
    return true;
}
```
#### **Action to Perform:**
1. Place caret on `Maybe` and press `Ctrl + B` (`Ctrl + Click`).
2. Place caret on `some` and press `Ctrl + B`.
3. Place caret on `secp256k1EcdsaVerify` and press `Ctrl + B`.
4. Press `Ctrl + Q` on `Maybe` or `some`.
#### **Expected Output:**
- `Maybe` resolves directly into the bundled standard library definition (`Maybe<T> { is_some: Boolean; value: T; }`).
- `some` resolves to `export circuit some<T>(value: T): Maybe<T>`.
- `secp256k1EcdsaVerify` resolves into `zkir-v3-library.compact`.
- `Ctrl + Q` renders quick documentation for standard library structs and circuits.

---

## 13. Subsystem 12: Inlay Parameter Name Hints

### 13.1 Parameter Names Inlay Display
#### **Input Code Snippet:**
```compact
circuit createOrder(buyer: Field, amount: Uint<64>, nonce: Bytes<32>): [] {
    return [];
}

circuit test(): [] {
    createOrder(101, 5000, [0; 32]);
}
```
#### **Expected Output & Visual Behavior:**
- In the editor at `createOrder(101, 5000, [0; 32])`, inline light-gray parameter hints appear:
  `createOrder(`**`buyer:`**` 101, `**`amount:`**` 5000, `**`nonce:`**` [0; 32]);`
- If an argument variable name matches the parameter name (e.g. `const buyer = 101; createOrder(buyer, 5000, ...)`), the `buyer:` hint is intelligently omitted to reduce visual noise.

---

## 14. Subsystem 13: Compiler Run Configurations & Gutter Play Buttons

### 14.1 Gutter "Play" Button Execution
#### **Input Code Snippet:**
```compact
export contract TokenContract {
    export circuit mint(): Void {}
}
```
#### **Action to Perform:**
1. Notice the green **"Play" Triangle** icon rendered in the editor gutter next to `export contract TokenContract` and `export circuit mint`.
2. Click the Play icon $\to$ select **"Run 'TokenContract'"** or **"Compile Compact Contract"**.
#### **Expected Output:**
- A **Run Tool Window** opens at the bottom running `compactc --vscode --skip-zk TokenContract.compact gen`.
- Build logs appear in real-time.
- If errors occur, the compiler output format (`file:line:col`) turns into a clickable hyperlink that jumps directly to the source error.

---

### 14.2 Creating / Editing Run Configurations (`Run | Edit Configurations...`)
#### **Action to Perform:**
1. Navigate to **Run** $\to$ **Edit Configurations...** $\to$ Click `+` $\to$ select **Compact Smart Contract**.
2. Set:
   - Name: `Compile Token`
   - Compact File: `src/token.compact`
   - Output Directory: `dist/artifacts`
   - Skip ZK: Checked (`true`)
   - Custom Flags: `--target typescript`
3. Click **OK** $\to$ Press `Shift + F10` (Run).
#### **Expected Output:**
- The command line is constructed with the customized arguments and executes cleanly in the run console.

---

## 15. Subsystem 14: Midnight Plugin Settings Page

### 15.1 Accessing & Customizing Settings
#### **Action to Perform:**
1. Open **Settings** (`Ctrl + Alt + S`) $\to$ Navigate to **Languages & Frameworks** $\to$ **Midnight Compact**.
2. Inspect the settings form:
   - **Compiler executable path (compactc)**: File chooser to specify path to binary or local node wrapper.
   - **Default output directory**: Text field (default: `gen`).
   - **Skip ZK proving key generation by default**: Checkbox (default: `true`).
   - **Devnet / Node RPC URL**: Text field (default: `http://localhost:9944`).
3. Change default output directory to `build/gen` and click **Apply** $\to$ **OK**.
4. Re-open settings to verify the values persisted across IDE sessions.

---

## 16. Subsystem 15: Background Compiler Diagnostics (`compactc`)

### 16.1 Live External Linter Annotations
#### **Action to Perform:**
1. Open any `.compact` contract in the editor.
2. Type an invalid expression (e.g. referencing an undeclared symbol or invalid modifier combination).
3. Wait 1-2 seconds for background compilation.
#### **Expected Output:**
- The background annotator (`CompactExternalAnnotator`) invokes `compactc --vscode --skip-zk` in the background.
- Compiler errors are highlighted directly in the editor on the exact line and column with full compiler error messages in hover tooltips.

---

## 17. Subsystem 16: Semantic Gutter Line Markers (Privacy & Circuit Visualizer)

### 17.1 Zero-Knowledge Boundaries & Privacy Gutter Icons
#### **Input Code Snippet:**
```compact
ledger authority: Field;
witness fetchSecret(): Bytes<32>;

export circuit mint(): Void {
    const s = disclose(fetchSecret());
}
```
#### **Expected Visual Output in Editor Gutter:**
- 🛡️ / ⚡ **AbstractMethod Icon** on `fetchSecret`: Tooltip displays *"Private off-chain witness query 'fetchSecret'"*.
- 🔑 **Keymap / Boundary Icon** on `disclose`: Tooltip displays *"Zero-Knowledge boundary: disclosing private witness data into circuit"*.
- ⚡ **Lightning Icon** on `export circuit mint`: Tooltip displays *"Exported on-chain ZK circuit 'mint'"*.
- 💾 **DataTables Icon** on `ledger authority`: Tooltip displays *"On-chain ledger state 'authority'"*.

---

## 18. Subsystem 17: Master End-to-End Test Contract

Copy and paste this full, valid contract into a file named `MasterTest.compact` to verify highlighting, resolution, structure view, navigation, and formatting all at once:

```compact
pragma language_version >= 0.20.0;

/**
 * Master Verification Contract for Midnight Compact IntelliJ Plugin.
 * Tests all AST constructs, keywords, types, and references.
 */
export contract MasterToken {
    // 1. Ledger State
    sealed ledger totalSupply: Uint<64>;
    ledger owner: Field;

    // 2. Data Types
    struct TokenAccount {
        ownerPk: Field,
        balance: Uint<64>,
        isActive: Boolean
    }

    enum TransferStatus {
        Success,
        InsufficientBalance,
        Unauthorized
    }

    type AccountList = Vector<TokenAccount, 10>;

    // 3. Constructor
    constructor(initialOwner: Field, initialSupply: Uint<64>) {
        owner = initialOwner;
        totalSupply = initialSupply;
    }

    // 4. Private Witness Functions
    witness getSenderPrivateKey(): Bytes<32>;
    witness computeSecretNonce(salt: Opaque<"nonce_salt">): Field;

    // 5. Public Circuits
    export pure circuit getContractOwner(): Field {
        return owner;
    }

    export circuit checkAccountStatus(account: TokenAccount): TransferStatus {
        if (account.isActive) {
            const hasFunds = account.balance > 0;
            if (hasFunds) {
                return TransferStatus.Success;
            } else {
                return TransferStatus.InsufficientBalance;
            }
        }
        return TransferStatus.Unauthorized;
    }

    export circuit transfer(recipient: Field, amount: Uint<64>): Boolean {
        // Disclose witness data into circuit
        const privKey = disclose(getSenderPrivateKey());
        assert amount > 0, "Transfer amount must be positive";

        const currentTotal = totalSupply;
        const fee = 10;
        const netAmount = amount - fee;

        if (netAmount > 0) {
            return true;
        } else {
            return false;
        }
    }
}
```

---

## 19. QA Manual Test Execution Checklist Table

Use this table during manual QA passes. Mark each feature as **PASS / FAIL** and record observations:

| #      | Subsystem                   | Test Action / Scenario              | Shortcut / Trigger                                                            | Expected Output                                          | Status (Pass/Fail) | Notes / Bugs |
|:-------|:----------------------------|:------------------------------------|:------------------------------------------------------------------------------|:---------------------------------------------------------|:------------------:|:-------------|
| **1**  | **Lexer / Highlighting**    | Pragma directives tokenization      | Type `pragma language_version >= 0.20.0;`                                     | Bold keywords, operator, cyan version                    |        [ ]         |              |
| **2**  | **Lexer / Highlighting**    | Keywords & Modifiers coloring       | `export contract`, `sealed ledger`, `pure circuit`                            | Distinct keyword attributes                              |        [ ]         |              |
| **3**  | **Lexer / Highlighting**    | Multi-base numbers                  | `1234`, `0x1A2F`, `0b1010`, `0o755`                                           | Number highlight for all bases                           |        [ ]         |              |
| **4**  | **Lexer / Highlighting**    | String escape sequences             | `"Hello \n \t \" \\"`                                                         | String color with distinct escape highlights             |        [ ]         |              |
| **5**  | **Lexer / Highlighting**    | Comments & Doc comments             | `//`, `/* */`, `/** @param */`                                                | Comments italicized, doc tags highlighted                |        [ ]         |              |
| **6**  | **Navigation (RES)**        | Local variable Go-to-Decl           | `Ctrl + Click` on local `const` usage                                         | Jumps to `const var = ...` declaration                   |        [ ]         |              |
| **7**  | **Navigation (RES)**        | Parameter Go-to-Decl                | `Ctrl + Click` on parameter usage                                             | Jumps to function signature parameter                    |        [ ]         |              |
| **8**  | **Navigation (RES)**        | Circuit / Witness call navigation   | `Ctrl + Click` on `circuitName()` call                                        | Jumps to circuit/witness definition                      |        [ ]         |              |
| **9**  | **Navigation (RES)**        | Struct field navigation             | `Ctrl + Click` on `account.balance`                                           | Jumps to `balance` in `struct TokenAccount`              |        [ ]         |              |
| **10** | **Navigation (RES)**        | Enum variant navigation             | `Ctrl + Click` on `Status.Success`                                            | Jumps to `Success` in `enum Status`                      |        [ ]         |              |
| **11** | **Navigation (RES)**        | Cross-file `include` navigation     | `Ctrl + Click` on `"./types.compact"`                                         | Opens included file; resolves types                      |        [ ]         |              |
| **12** | **Refactoring (REF)**       | In-place Circuit Rename             | `Shift + F6` on `circuitName`                                                 | Renames declaration and all call sites                   |        [ ]         |              |
| **13** | **Refactoring (REF)**       | Parameter Rename                    | `Shift + F6` on parameter                                                     | Renames parameter in body & signature                    |        [ ]         |              |
| **14** | **Refactoring (REF)**       | Struct & Field Rename               | `Shift + F6` on Struct or field                                               | Renames all occurrences across file                      |        [ ]         |              |
| **15** | **Refactoring (REF)**       | Identifier Validation               | Rename to `circuit` (keyword)                                                 | Rejects invalid name with error dialog                   |        [ ]         |              |
| **16** | **Find Usages (FND)**       | Find Usages of Circuit              | `Alt + F7` on circuit declaration                                             | Tool window lists all caller references                  |        [ ]         |              |
| **17** | **Completion (CMP)**        | Top-level keyword completion        | `Ctrl + Space` at start of file                                               | Suggests `contract`, `circuit`, `struct`                 |        [ ]         |              |
| **18** | **Completion (CMP)**        | Type annotation completion          | `Ctrl + Space` after `x: `                                                    | Suggests `Boolean`, `Field`, `Uint`, Structs             |        [ ]         |              |
| **19** | **Completion (CMP)**        | Expression scope completion         | `Ctrl + Space` inside function body                                           | Suggests local vars, params, circuits                    |        [ ]         |              |
| **20** | **Completion (CMP)**        | Struct dot-completion               | `Ctrl + Space` after `account.`                                               | Suggests struct fields (`ownerPk`, etc.)                 |        [ ]         |              |
| **21** | **Completion (CMP)**        | Enum dot-completion                 | `Ctrl + Space` after `Status.`                                                | Suggests enum variants (`Success`, etc.)                 |        [ ]         |              |
| **22** | **Inspections (INS)**       | Unresolved reference warning        | Type `const x = missingVar;`                                                  | Yellow/red squiggle on `missingVar`                      |        [ ]         |              |
| **23** | **Inspections (INS)**       | Duplicate declaration warning       | Declare `const a = 1; const a = 2;`                                           | Squiggle on duplicate identifier                         |        [ ]         |              |
| **24** | **Inspections (INS)**       | Unused local variable + Quick-Fix   | `Alt + Enter` on unused variable                                              | Suggests & executes "Remove unused variable"             |        [ ]         |              |
| **25** | **Inspections (INS)**       | Type mismatch warning               | Type `if (123) { ... }`                                                       | Squiggle: Condition requires Boolean                     |        [ ]         |              |
| **26** | **Inspections (INS)**       | Pure Circuit Guard + Quick-Fix      | Call witness in `pure circuit`                                                | Warning + `Alt + Enter` "Remove 'pure' modifier"         |        [ ]         |              |
| **27** | **Inspections (INS)**       | Sealed Ledger Mutation Guard        | Mutate `sealed ledger` outside constructor                                    | Warning: Cannot mutate sealed ledger outside constructor |        [ ]         |              |
| **28** | **Inspections (INS)**       | Recursion Prevention Guard          | Circuit calling itself `fib(n)`                                               | Warning: Circuit cannot be recursive                     |        [ ]         |              |
| **29** | **Inspections (INS)**       | Constructor Restrictions Guard      | `emit` inside constructor                                                     | Warning: Constructors cannot emit events                 |        [ ]         |              |
| **30** | **Inspections (INS)**       | Witness Privacy Guard (WPP) + Fix   | Assign witness to ledger                                                      | Warning + `Alt + Enter` "Wrap with disclose(...)"        |        [ ]         |              |
| **31** | **Formatter (FMT)**         | Reformat code                       | `Ctrl + Alt + L`                                                              | Formats indentation, braces, spaces                      |        [ ]         |              |
| **32** | **Formatter (FMT)**         | Smart enter / indent                | Press `Enter` after `{`                                                       | Auto-indents 4 spaces inside block                       |        [ ]         |              |
| **33** | **Editor (STR/DOC)**        | Structure View tree                 | `Alt + 7` or `Ctrl + F12`                                                     | Shows contracts, circuits, structs tree                  |        [ ]         |              |
| **34** | **Editor (STR/DOC)**        | Quick Documentation hover           | Hover or `Ctrl + Q` on keyword/type                                           | Renders HTML documentation popup                         |        [ ]         |              |
| **35** | **Editor (FLD/BRD)**        | Code folding & Breadcrumbs          | Gutter `-` / `+` click & breadcrumbs                                          | Folds blocks cleanly; shows path hierarchy               |        [ ]         |              |
| **36** | **Templates (TMP)**         | Live template expansion             | Type `cir` + `Tab` or `cct` + `Tab`                                           | Expands to corresponding code skeleton                   |        [ ]         |              |
| **37** | **Templates (TMP)**         | New File Action                     | Right-Click $\to$ New $\to$ Compact File                                      | Creates selected template file                           |        [ ]         |              |
| **38** | **Search Everywhere (NAV)** | Search Class / Struct / Enum / Type | `Ctrl + N` / `Double Shift`                                                   | Finds contracts, modules, structs, enums, types          |        [ ]         |              |
| **39** | **Search Everywhere (NAV)** | Search Symbol / Method / Field      | `Ctrl + Alt + Shift + N`                                                      | Finds circuits, witnesses, ledger fields, struct fields  |        [ ]         |              |
| **40** | **Standard Library (STD)**  | Stdlib Struct / Circuit Resolution  | `Ctrl + Click` on `Maybe` or `some`                                           | Jumps to bundled `standard-library.compact`              |        [ ]         |              |
| **41** | **Standard Library (STD)**  | ZKIR Primitive Resolution           | `Ctrl + Click` on `secp256k1EcdsaVerify`                                      | Jumps to bundled `zkir-v3-library.compact`               |        [ ]         |              |
| **42** | **Inlay Hints (HNT)**       | Call Site Parameter Hints           | Call `compute(1, 2)`                                                          | Renders inline `round: 1, sk: 2` labels                  |        [ ]         |              |
| **43** | **Run Config (RUN)**        | Gutter Run / Play Button            | Click Green Play Icon in Gutter                                               | Runs `compactc` execution in Run Console                 |        [ ]         |              |
| **44** | **Run Config (RUN)**        | Edit Configurations Dialog          | `Run                                               \| Edit Configurations...` | Customizes input file, output dir, flags                 |        [ ]         |              |
| **45** | **Run Config (RUN)**        | Clickable Console Link Filter       | Click `file.compact:14:5` in console                                          | Jumps directly to line 14, column 5 in editor            |        [ ]         |              |
| **46** | **Settings (SET)**          | Midnight Settings Page              | `Settings -> Languages & Frameworks`                                          | Configures compiler path, RPC, defaults                  |        [ ]         |              |
| **47** | **External Linter (LINT)**  | Background Compiler Annotator       | Type invalid code and wait                                                    | Highlights real-time `compactc` errors                   |        [ ]         |              |
| **48** | **Line Markers (LMK)**      | Witness Private Query Icon          | View gutter at `witness` declaration                                          | Shows AbstractMethod / Private query icon                |        [ ]         |              |
| **49** | **Line Markers (LMK)**      | ZK Disclosure Boundary Icon         | View gutter at `disclose(...)`                                                | Shows Keymap / Privacy boundary icon                     |        [ ]         |              |
| **50** | **Line Markers (LMK)**      | Exported Circuit Lightning Icon     | View gutter at `export circuit`                                               | Shows Lightning / Public entry icon                      |        [ ]         |              |


