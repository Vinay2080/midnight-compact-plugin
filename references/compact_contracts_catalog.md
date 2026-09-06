# Compact Contracts Reference Catalog

This catalog documents the repository of real Compact smart contracts located in `compact-contracts/contracts/`.
These contracts provide gold-standard, production-grade Compact reference syntax, patterns, and type definitions (from OpenZeppelin Compact Contracts `v0.4.0-alpha.1`) to use as test fixtures and context references instead of synthetic snippets.

---

## Contract Inventory & Taxonomy

### 1. Access Control (`compact-contracts/contracts/src/access/`)
* **`Ownable.compact`**: Standard single-owner pattern. Features `export ledger owner`, constructor initialization, `transferOwnership`, `renounceOwnership`, and owner verification circuits with witnesses.
* **`AccessControl.compact`**: Role-based access control (RBAC). Implements role hashing, admin roles, role grant/revoke/renounce circuits, and role verification witnesses.
* **`ShieldedAccessControl.compact`**: Privacy-preserving access control using shielded commitments.
* **`ZOwnablePK.compact`**: Zero-knowledge public-key-based ownership proofs.
* **Mocks (`test/mocks/`)**: `MockOwnable.compact`, `MockAccessControl.compact`, `MockShieldedAccessControl.compact`, `MockZOwnablePK.compact`.

### 2. Token Standards (`compact-contracts/contracts/src/token/`)
* **`FungibleToken.compact`**: Base fungible token (similar to ERC-20) with transparent ledger balances and mint/burn/transfer circuits.
* **`NonFungibleToken.compact`**: Unique token standard (similar to ERC-721) with token IDs, owner tracking, and transfer approvals.
* **`ConfidentialFungibleToken.compact`**: Private token balances with zero-knowledge transfers, coin balance proofs, and nullifier management.
* **`MultiToken.compact`**: Multi-asset standard (similar to ERC-1155) managing multiple token IDs in a unified ledger.
* **`NativeShieldedToken.compact` & `NativeShieldedTokenCore.compact`**: Core shielded token primitives supporting note commitments and Merkle trees.
* **Extensions (`extensions/`)**: `ConfidentialFungibleTokenPublicSupply.compact`, `NativeShieldedTokenPublicSupply.compact`, `NativeShieldedTokenDerivedNonce.compact`, `NativeShieldedTokenFamily.compact`, `NativeShieldedTokenFamilyPublicSupply.compact`.
* **Mocks (`test/mocks/`)**: `MockFungibleToken.compact`, `MockNonFungibleToken.compact`, `MockConfidentialFungibleToken.compact`, `MockMultiToken.compact`, `MockNativeShieldedToken.compact`.

### 3. Multisig & Governance (`compact-contracts/contracts/src/multisig/`)
* **`ProposalManager.compact`**: Decentralized governance proposals, voting thresholds, multi-party signatures, and action dispatch.
* **`Signer.compact` & `EcdsaSignerManager.compact`**: Individual and aggregate cryptographic signer management.
* **`ShieldedTreasury.compact` & `ShieldedTreasuryStateless.compact`**: Shielded vault/treasury for holding and releasing assets based on multisig approvals.
* **`UnshieldedTreasury.compact`**: Public treasury balance management.
* **Forwarders (`ForwarderPrivate.compact`, `ForwarderShielded.compact`, `ForwarderUnshielded.compact`)**: Execution proxy patterns.
* **Presets (`presets/`)**: `ShieldedMultiSig.compact`, `ShieldedMultiSigV2.compact`, `ShieldedMultiSigV3.compact`.
* **Mocks (`test/mocks/`)**: Mock signers, treasuries, forwarders, and proposal managers.

### 4. Security & Lifecycle (`compact-contracts/contracts/src/security/`)
* **`Initializable.compact`**: Single-execution initialization locks preventing replay or re-entrant constructor behavior.
* **`Pausable.compact`**: Emergency pause switch to halt circuit execution in critical situations.
* **`Allowlist.compact`**: Whitelist membership circuits.
* **`Blocklist.compact`**: Prohibited address/key filtering circuits.
* **Mocks (`test/mocks/`)**: `MockInitializable.compact`, `MockPausable.compact`, `MockAllowlist.compact`, `MockBlocklist.compact`.

### 5. Cryptography (`compact-contracts/contracts/src/crypto/`)
* **`Ecdsa.compact`**: In-circuit ECDSA signature verification and curve point mathematics.
* **`ElGamal.compact`**: Additively homomorphic public-key encryption and rerandomization circuits.
* **`EcdhMask.compact`**: Diffie-Hellman key exchange and blinding masks.
* **Mocks (`test/mocks/`)**: `MockCurveOps.compact`, `MockEcdsa.compact`, `MockElGamal.compact`, `MockEcdhMask.compact`.

### 6. Utilities (`compact-contracts/contracts/src/utils/`)
* **`Utils.compact`**: Common helper routines and math invariants.
* **Mocks (`test/mocks/`)**: `MockUtils.compact`.

### 7. Multi-Module Integration Mocks (`compact-contracts/contracts/test/integration/_mocks/`)
* **`ComposedTokens.compact`**: Complex composition importing and cross-referencing multiple token contracts.
* **`ComposedConfidentialFungibleTokenPublicSupply.compact`**: Token composition with supply ledger state.
* **`SharedInitCollision.compact` & `sharedInit/ModuleA.compact`, `sharedInit/ModuleB.compact`**: Inter-module namespace, symbol collision, and initialization tests.

---

## Usage Guidelines: When to Test Against Which Reference

1. **Parser & Lexer Resilience**:
   * Test across all 79 contracts in `compact-contracts/contracts/` to ensure zero syntax errors or parsing regressions on genuine Midnight code.
2. **ZK Line Markers & Gutter Actions**:
   * Use `Ownable.compact`, `AccessControl.compact`, and `ConfidentialFungibleToken.compact` to verify `disclose`, `witness`, `export circuit`, and `ledger` line markers on realistic contracts.
3. **Hierarchy & Interface Navigation (`implements` / `contract`)**:
   * Use `contract implements <Name>;` against `Mock` contracts and interfaces (e.g. `MockFungibleToken` vs `FungibleToken`, or `MockSigner` vs `Signer`).
4. **Multi-Module & Symbol Resolution**:
   * Use `ComposedTokens.compact` and `SharedInitCollision.compact` to test cross-file imports, prefix handling, and type reference resolution.
