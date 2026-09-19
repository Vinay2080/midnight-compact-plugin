# Test Flakiness Register & Quarantine Log

This register records intermittent, non-deterministic, or race-prone test failures across CI and local runs.

---

## 1. Quarantine & Handling Protocol

1. **Detection:** When a test fails in CI or locally without any relevant code changes, it is flagged here.
2. **Investigation:** Diagnose whether the cause is:
   - UI thread scheduling (`ApplicationManager.getApplication().invokeLater()`).
   - File system / VFS timestamp lag (`VfsUtil.markDirtyAndRefresh()`).
   - Port or socket conflict in compiler daemons / toolchain tests.
3. **Quarantine Rule:** If a test fails intermittently $>2$ times across 10 builds, the test is marked `@Disabled("Quarantined: Flaky race condition under investigation")` or moved to a quarantined suite with an issue ticket link.

---

## 2. Active Flakiness Records

| Test Name | Package | Failure Mode | Trigger Condition | Status | Action Taken |
| :--- | :--- | :--- | :--- | :--- | :--- |
| *None currently quarantined* | — | — | — | **Healthy** | All current test suites are deterministic and passing. |
