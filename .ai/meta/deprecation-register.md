# Platform & API Deprecation Register

This register documents deprecated IntelliJ Platform APIs, third-party libraries, and internal methods, tracking their planned replacements and retirement deadlines.

---

## 1. Active Deprecations

### DEP-001: `com.intellij.DynamicBundle` Legacy Constructor
- **Status:** **REPLACED**
- **First Observed:** 2026-09-16 (Plugin verification warning)
- **Problem:** Direct instantiation of `DynamicBundle` using non-localized bundle path was deprecated in IntelliJ Platform 2024.2+.
- **Resolution:** Updated `CompactBundle` to use the modern localization constructor and singleton lookup.
- **Reference:** `.ai/bugs/2026-09-16-bundle-dynamic-bundle-deprecation.md`

### DEP-002: Pre-Java 21 Collection Access Patterns (`get(0)`, `get(size - 1)`)
- **Status:** **PROHIBITED**
- **Enforcement:** Enforced via `.agents/rules/modern-java25.rules.md`.
- **Replacement:** `list.getFirst()`, `list.getLast()`, and sequenced collections.
