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
    Assert.assertEquals("0.23.0", CompactVersionManager.cleanVersion("0.23"));
    Assert.assertEquals("0.26.0", CompactVersionManager.cleanVersion("0.26"));
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
    Assert.assertEquals("0.34.0", CompactVersionManager.resolveToolchainVersionForLanguage("0.26"));
    Assert.assertEquals("0.31.1", CompactVersionManager.resolveToolchainVersionForLanguage("0.23.0"));
    Assert.assertEquals("0.31.1", CompactVersionManager.resolveToolchainVersionForLanguage("0.23"));
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

  @Test
  public void testZipSlipPrevention() throws IOException {
    File tempDir = FileUtil.createTempDirectory("compact-zip-slip-test", null, true);
    File zipFile = new File(tempDir, "malicious.zip");
    File destDir = new File(tempDir, "dest");
    Assert.assertTrue(destDir.mkdirs());

    try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFile))) {
      java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("../escaped.txt");
      zos.putNextEntry(entry);
      zos.write("malicious payload".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      zos.closeEntry();
    }

    try {
      CompactVersionInstaller.extractZip(zipFile, destDir);
      Assert.fail("Expected IOException due to Zip Slip vulnerability prevention");
    } catch (IOException e) {
      Assert.assertTrue(e.getMessage().contains("Zip entry is outside target directory"));
    } finally {
      FileUtil.delete(tempDir);
    }
  }

  @Test
  public void testFindExecutableInSubdirectoriesArchitecture() throws IOException {
    File tempDir = FileUtil.createTempDirectory("compact-arch-test", null, true);
    try {
      File archDir = new File(tempDir, "x86_64-unknown-linux-musl");
      Assert.assertTrue(archDir.mkdirs());
      File exe = new File(archDir, "compactc");
      Assert.assertTrue(exe.createNewFile());

      File found = CompactVersionManager.findExecutableInVersionDir(tempDir);
      Assert.assertNotNull(found);
      Assert.assertTrue(found.getAbsolutePath().contains("x86_64-unknown-linux-musl"));
    } finally {
      FileUtil.delete(tempDir);
    }
  }

  @Test
  public void testDetectVersionFromExecutableFastPath() {
    String path = "/home/user/.compact/versions/0.34.0/bin/compactc";
    String version = CompactVersionManager.detectVersionFromExecutable(path);
    Assert.assertEquals("0.34.0", version);
  }

  @Test
  public void testCleanVersionEdgeCases() {
    Assert.assertEquals("", CompactVersionManager.cleanVersion(""));
    Assert.assertEquals("", CompactVersionManager.cleanVersion("   "));
    Assert.assertEquals("0.22.0", CompactVersionManager.cleanVersion("0.22"));
    Assert.assertEquals("0.34.0", CompactVersionManager.cleanVersion("v0.34.0"));
  }

  @Test
  public void testIsVersionInstalledGraceful() {
    Assert.assertFalse(CompactVersionManager.isVersionInstalled("999.999.999"));
  }
}

