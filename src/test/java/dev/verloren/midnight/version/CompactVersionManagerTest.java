package dev.verloren.midnight.version;

import com.intellij.openapi.util.io.FileUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CompactVersionManagerTest {

  @Test
  public void testCleanVersion() {
    Assert.assertEquals("0.26.0", CompactVersionManager.cleanVersion("0.26.0"));
    Assert.assertEquals("0.26.0", CompactVersionManager.cleanVersion("v0.26.0"));
    Assert.assertEquals("0.23.1", CompactVersionManager.cleanVersion("  v0.23.1-rc1  "));
    Assert.assertEquals("1.0.0", CompactVersionManager.cleanVersion("1.0.0"));
  }

  @Test
  public void testFindExecutableInVersionDir() throws IOException {
    File tempDir = FileUtil.createTempDirectory("compact-version-test", null, true);
    try {
      // 1. Create node_modules/.bin/compactc
      File binDir = new File(tempDir, "node_modules/.bin");
      Assert.assertTrue(binDir.mkdirs());
      File exe = new File(binDir, "compactc");
      Assert.assertTrue(exe.createNewFile());

      File found = CompactVersionManager.findExecutableInVersionDir(tempDir);
      Assert.assertNotNull(found);
      Assert.assertTrue(found.getAbsolutePath().contains("compactc"));
    } finally {
      FileUtil.delete(tempDir);
    }
  }

  @Test
  public void testKnownVersions() {
    Assert.assertFalse(CompactVersionManager.KNOWN_VERSIONS.isEmpty());
    Assert.assertTrue(CompactVersionManager.KNOWN_VERSIONS.contains("0.34.0"));
    Assert.assertTrue(CompactVersionManager.KNOWN_VERSIONS.contains("0.31.1"));
    Assert.assertTrue(CompactVersionManager.KNOWN_VERSIONS.contains("0.26.0"));
    Assert.assertTrue(CompactVersionManager.KNOWN_VERSIONS.contains("0.23.0"));
  }

  @Test
  public void testResolveToolchainVersionForLanguage() {
    Assert.assertEquals("0.34.0", CompactVersionManager.resolveToolchainVersionForLanguage("0.26.0"));
    Assert.assertEquals("0.31.1", CompactVersionManager.resolveToolchainVersionForLanguage("0.23.0"));
    Assert.assertEquals("0.30.0", CompactVersionManager.resolveToolchainVersionForLanguage("0.22.0"));
    Assert.assertEquals("0.29.0", CompactVersionManager.resolveToolchainVersionForLanguage("0.21.0"));
    Assert.assertEquals("0.26.0", CompactVersionManager.resolveToolchainVersionForLanguage("0.18.0"));
    Assert.assertEquals("1.0.0", CompactVersionManager.resolveToolchainVersionForLanguage("1.0.0"));
  }

  @Test
  public void testGetLanguageVersionForToolchain() {
    Assert.assertEquals("0.26.0", CompactVersionManager.getLanguageVersionForToolchain("0.34.0"));
    Assert.assertEquals("0.23.0", CompactVersionManager.getLanguageVersionForToolchain("0.31.1"));
    Assert.assertEquals("0.23.0", CompactVersionManager.getLanguageVersionForToolchain("0.31.0"));
    Assert.assertEquals("0.22.0", CompactVersionManager.getLanguageVersionForToolchain("0.30.0"));
    Assert.assertEquals("0.21.0", CompactVersionManager.getLanguageVersionForToolchain("0.29.0"));
    Assert.assertEquals("0.18.0", CompactVersionManager.getLanguageVersionForToolchain("0.26.0"));
    Assert.assertEquals("0.18.0", CompactVersionManager.getLanguageVersionForToolchain("0.26.2"));
  }

  @Test
  public void testGetVersionDescription() {
    Assert.assertTrue(CompactVersionManager.getVersionDescription("0.34.0").contains("0.26.0"));
    Assert.assertTrue(CompactVersionManager.getVersionDescription("0.34.0").contains("Midnight Ledger 9"));
    Assert.assertTrue(CompactVersionManager.getVersionDescription("0.31.1").contains("0.23.0"));
    Assert.assertTrue(CompactVersionManager.getVersionDescription("0.26.0").contains("0.18.0"));
  }

  @Test
  public void testGetReleaseAssetName() {
    String assetName = CompactVersionManager.getReleaseAssetName("0.34.0");
    Assert.assertNotNull(assetName);
    Assert.assertTrue(assetName.startsWith("compactc_v0.34.0_"));
    Assert.assertTrue(assetName.endsWith(".zip"));
  }
}
