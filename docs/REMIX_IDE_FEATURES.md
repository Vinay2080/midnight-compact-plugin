# Blockchain IDE (Remix-Style) Feature Specification

This document details the architectural design and functional specifications for integrating **Remix-style Blockchain IDE capabilities** into the **Midnight Compact Language Plugin** for JetBrains IntelliJ IDEA.

---

## 1. Vision & Background

Browser-based IDEs like **Ethereum Remix** revolutionized smart contract development by providing instant compilation, one-click contract deployment, form-driven transaction execution, live storage inspection, and account management without requiring complex CLI terminal scripts.

However, browser IDEs lack the industrial-grade AST refactoring, multi-file navigation, type checking, and indexing capabilities of JetBrains IntelliJ IDEA.

By infusing **Midnight Compact** with Remix-style blockchain workflows, developers gain the ultimate smart-contract environment:
- **IntelliJ's World-Class Code Intelligence**: Handwritten lexer/parser, type inference, static inspections, and cross-file resolution.
- **Remix's Frictionless Runtime Usability**: Visual contract deployment, dynamic circuit calling forms, private witness input builders, live ledger inspectors, and local devnet management.

```text
\u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510
\u2502                    IntelliJ IDEA Workspace UI                               \u2502
\u2502 \u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510 \u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510 \u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510 \u2502
\u2502 \u2502 Project Tree View     \u2502 \u2502 Code Editor (Contract.compact)    \u2502 \u2502 REMIX SIDEBAR PANEL       \u2502 \u2502
\u2502 \u2502 \u251c\u2500\u2500 contracts/         \u2502 \u2502 - Gutter Play Run Circuit          \u2502 \u2502 \u251c\u2500\u2500 Environment Selector    \u2502 \u2502
\u2502 \u2502 \u2502   \u2514\u2500\u2500 Counter.compact  \u2502 \u2502 - Type Inlay & Inspections         \u2502 \u2502 \u251c\u2500\u2500 Account & Balance       \u2502 \u2502
\u2502 \u2502 \u251c\u2500\u2500 tests/             \u2502 \u2502 - Live Pragma Version Switcher    \u2502 \u2502 \u251c\u2500\u2500 Deploy Form (Constructor)\u2502 \u2502
\u2502 \u2502 \u2514\u2500\u2500 zk-config/         \u2502 \u2502                                    \u2502 \u2502 \u251c\u2500\u2500 Deployed Contracts UI  \u2502 \u2502
\u2502 \u2502                        \u2502 \u2502                                    \u2502 \u2502 \u2502   \u2514\u2500\u2500 Call Circuits / Inputs\u2502 \u2502
\u2502 \u2502                        \u2502 \u2502                                    \u2502 \u2502 \u2514\u2500\u2500 Ledger State Inspector   \u2502 \u2502
\u2502 \u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518 \u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518 \u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518 \u2502
\u2502 \u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510 \u2502
\u2502 \u2502 Bottom Panel: Node RPC Console / Proof Server Logs / ZK Profiler         \u2502 \u2502
\u2502 \u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518 \u2502
\u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518
```

---

## 2. Core Feature Pillars

### Pillar 1: Multi-Version Compiler & Build Configuration
*(Building upon the existing `CompactCompilerToolWindowFactory`)*
- **Compiler Version Pinned per Project**: Automatically maintained in `.idea/midnight.xml`.
- **Auto-Download & Isolation**: Versions installed under `~/.compact/versions/<version>/` (in WSL or native OS).
- **Target Artifact Selection**:
  - Checkbox: Generate ZKIR bytecode (`.zkir`).
  - Checkbox: Generate TypeScript Bindings (`--typescript`).
  - Checkbox: Generate Verification Keys (`.vkey`).
  - Optimization Level dropdown: `Debug (O0)`, `Balanced (O1)`, `Max Constraint Optimization (O2)`.

---

### Pillar 2: Deploy & Run Transactions Panel

The **Midnight Explorer** tool window will feature a dedicated **Deploy & Run** tab matching Remix IDE semantics:

#### 1. Environment Provider Dropdown
- **In-Memory Mock Ledger (Fast)**: Instant state simulation running entirely in the JVM without network latency.
- **Local Devnet (Docker)**: Automatically connects to `http://localhost:9944` (Substrate RPC) and `http://localhost:6300` (Midnight Proof Server) provided by `midnight-local-dev`.
- **Midnight Testnet (Remote)**: Connects to public Midnight testnet with TLS and API key support.
- **Custom RPC**: Configurable JSON-RPC URL endpoint.

#### 2. Account & Wallet Manager
- Discovers pre-funded developer accounts from `midnight-local-dev/accounts.json`.
- Displays active account address, public key, and balance (tDUST / Night tokens).
- Dropdown allows 1-click switching between test accounts (e.g. `Alice`, `Bob`, `Charlie`).
- Seed generator and seed import wizard for custom private keys.

#### 3. Contract Deployment Form
- Select target contract from active editor or project index.
- If the contract defines a `constructor(...)`, automatically renders typed input text fields:
  - `initialSupply: Uint<64>` \u2192 Numeric field with boundary checking.
  - `owner: Bytes<32>` \u2192 Hexadecimal address field with checksum validation.
- **Deploy Button**: Initiates transaction packaging, computes initial state commitment, sends transaction to RPC, and lists the deployed contract under **Deployed Contracts**.

#### 4. Deployed Contracts & Circuit Execution
- Lists deployed contract instances with their unique contract address, block height, and timestamp.
- Expanding a contract displays all public `@export circuit` members:
  - Example: `circuit transfer(recipient: Bytes<32>, amount: Uint<64>): Void`
  - Renders input parameters with types.
- **Private Witness Input Modal**:
  - In Midnight Compact, circuits frequently call witness functions to obtain private data:
    ```compact
    witness secretKey(): Bytes<32>;
    ```
  - Before executing the circuit, the UI provides a **Witness Input Drawer** allowing the developer to supply private values for the transaction.
  - Generates the zero-knowledge proof locally via the proof server before submitting the transaction.
- **Transaction Receipt Card**:
  - Displays transaction hash, block number, execution status (`SUCCESS` / `FAILED`), gas/DUST consumed, and proof generation latency.

---

### Pillar 3: Ledger State & Privacy Inspector

Unlike traditional blockchains where all storage is public, Midnight contracts distinguish between:
1. **Disclosed State**: Plaintext data visible to all nodes on the ledger.
2. **Sealed / Private State**: Cryptographic commitments, Merkle roots, and private UTXO cells whose contents are shielded by zero-knowledge proofs.

#### Inspector Capabilities:
- **Visual Ledger Tree**:
  - Displays ledger variables declared via `ledger { ... }`.
  - Color-coded badges:
    - `[PUBLIC]` for standard disclosed variables.
    - `[SEALED]` for private state cells.
- **State History & Diffs**:
  - Tracks ledger state changes across consecutive circuit calls.
  - Side-by-side diff highlighting modified fields in green and red.
- **UTXO / Cell Visualizer**:
  - Displays active unspent notes owned by the selected devnet account.

---

### Pillar 4: ZK-SNARK Circuit Profiler & Constraint Matrix

Smart contract development on Midnight requires understanding the zero-knowledge cost of circuits.

- **Constraint Count Card**:
  - Analyzes the compiled ZKIR to report total R1CS/Plonk constraints per circuit.
  - Warns developers when circuits exceed recommended proof sizes:
    - \u2264 50,000 constraints: Green (Sub-second proof on mobile/browser).
    - 50,000\u2013250,000: Yellow (Fast proof on desktop).
    - \u003e 250,000: Red (High prover latency warning).
- **Prover Timing Benchmark**:
  - Measures execution time for:
    1. Witness evaluation.
    2. Witness to ZKIR synthesis.
    3. Proof generation by `proof-server`.
    4. Verification key verification.
- **ZKIR Bytecode Viewer**:
  - Integrated editor view displaying human-readable ZKIR assembly with syntax highlighting.

---

### Pillar 5: Local Devnet & Proof Server Manager

Seamless control of the local Docker stack (`midnight-local-dev`):

- **Status Bar & Tool Window Indicators**:
  - Node RPC: `Online (9944)` / `Connecting...` / `Stopped`.
  - Proof Server: `Online (6300)` / `Stopped`.
  - Indexer: `Online (8088)` / `Stopped`.
- **1-Click Stack Controls**:
  - \"Start Localnet\": Runs `docker compose -f standalone.yml up -d` in background.
  - \"Stop Localnet\": Runs `docker compose -f standalone.yml down`.
  - \"Reset Blockchain State\": Flushes chain state back to genesis.
- **Embedded RPC Log Terminal**:
  - Streams real-time Substrate node and proof server logs inside IntelliJ's Console window.

---

## 3. Implementation Blueprint & IntelliJ Platform Classes

| Feature Component | Class Name | Package | Extension Point in `plugin.xml` |
| :--- | :--- | :--- | :--- |
| **Explorer Tool Window** | `MidnightExplorerToolWindowFactory` | `dev.verloren.midnight.toolwindow.explorer` | `<toolWindow id=\"Midnight_Explorer\" .../>` |
| **Deploy & Run Panel** | `CompactDeployAndRunPanel` | `dev.verloren.midnight.toolwindow.explorer` | Sub-panel inside Tool Window |
| **Circuit Interaction Form** | `CompactCircuitInvocationForm` | `dev.verloren.midnight.toolwindow.explorer` | Dynamic Swing UI built from PSI |
| **Witness Input Dialog** | `CompactWitnessInputDialog` | `dev.verloren.midnight.toolwindow.dialog` | IntelliJ `DialogWrapper` |
| **Ledger State Viewer** | `CompactLedgerInspectorPanel` | `dev.verloren.midnight.toolwindow.explorer` | TreeTable UI |
| **Devnet Manager Service** | `MidnightDevnetService` | `dev.verloren.midnight.devnet` | `<projectService serviceImplementation=\"...\"/>` |
| **Node RPC Client** | `MidnightRpcClient` | `dev.verloren.midnight.rpc` | JSON-RPC 2.0 Client (Java HTTP) |
| **ZK Profiler Panel** | `CompactCircuitProfilerPanel` | `dev.verloren.midnight.profiler` | Metric visualizer |
