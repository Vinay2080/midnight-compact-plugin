package dev.verloren.midnight.version;

import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;

public class CompactSemVerUtilTest {

  @Test
  public void testParseValidVersions() {
    CompactSemVerUtil.SemVer v1 = CompactSemVerUtil.parse("0.26.0");
    Assert.assertNotNull(v1);
    Assert.assertEquals(0, v1.major());
    Assert.assertEquals(26, v1.minor());
    Assert.assertEquals(0, v1.patch());
    Assert.assertNull(v1.preRelease());

    CompactSemVerUtil.SemVer v2 = CompactSemVerUtil.parse("v1.2.3-beta.1");
    Assert.assertNotNull(v2);
    Assert.assertEquals(1, v2.major());
    Assert.assertEquals(2, v2.minor());
    Assert.assertEquals(3, v2.patch());
    Assert.assertEquals("beta.1", v2.preRelease());

    CompactSemVerUtil.SemVer vTwoDigit = CompactSemVerUtil.parse("0.23");
    Assert.assertNotNull(vTwoDigit);
    Assert.assertEquals(0, vTwoDigit.major());
    Assert.assertEquals(23, vTwoDigit.minor());
    Assert.assertEquals(0, vTwoDigit.patch());
  }

  @Test
  public void testCompareVersions() {
    CompactSemVerUtil.SemVer v023 = CompactSemVerUtil.parse("0.23.0");
    CompactSemVerUtil.SemVer v026 = CompactSemVerUtil.parse("0.26.0");
    CompactSemVerUtil.SemVer v0261 = CompactSemVerUtil.parse("0.26.1");

    Assert.assertNotNull(v023);
    Assert.assertNotNull(v026);
    Assert.assertNotNull(v0261);

    Assert.assertTrue(v026.compareTo(v023) > 0);
    Assert.assertTrue(v023.compareTo(v026) < 0);
    Assert.assertTrue(v0261.compareTo(v026) > 0);
    Assert.assertEquals(0, v026.compareTo(Objects.requireNonNull(CompactSemVerUtil.parse("0.26.0"))));
  }

  @Test
  public void testSatisfiesConstraint() {
    // Greater-or-equal
    Assert.assertTrue(CompactSemVerUtil.satisfiesConstraint("0.26.0", ">= 0.26.0"));
    Assert.assertTrue(CompactSemVerUtil.satisfiesConstraint("0.26.2", ">= 0.26.0"));
    Assert.assertFalse(CompactSemVerUtil.satisfiesConstraint("0.23.0", ">= 0.26.0"));

    // Bare versions (e.g. pragma language_version 0.23; means >= 0.23)
    Assert.assertTrue(CompactSemVerUtil.satisfiesConstraint("0.26.0", "0.23"));
    Assert.assertTrue(CompactSemVerUtil.satisfiesConstraint("0.23.0", "0.23"));
    Assert.assertTrue(CompactSemVerUtil.satisfiesConstraint("0.26.0", "0.26"));
    Assert.assertFalse(CompactSemVerUtil.satisfiesConstraint("0.18.0", "0.23"));

    // Caret (compatible minor for 0.x)
    Assert.assertTrue(CompactSemVerUtil.satisfiesConstraint("0.26.2", "^0.26.0"));
    Assert.assertFalse(CompactSemVerUtil.satisfiesConstraint("0.27.0", "^0.26.0"));
    Assert.assertFalse(CompactSemVerUtil.satisfiesConstraint("0.25.0", "^0.26.0"));

    // Exact match
    Assert.assertTrue(CompactSemVerUtil.satisfiesConstraint("0.26.0", "== 0.26.0"));
    Assert.assertFalse(CompactSemVerUtil.satisfiesConstraint("0.26.1", "== 0.26.0"));

    // Compound / logical OR
    Assert.assertTrue(CompactSemVerUtil.satisfiesConstraint("0.26.0", "0.23 || 0.26"));
    Assert.assertTrue(CompactSemVerUtil.satisfiesConstraint("0.23.0", ">= 0.20 && < 0.25"));
    Assert.assertFalse(CompactSemVerUtil.satisfiesConstraint("0.26.0", ">= 0.20 && < 0.25"));
  }

  @Test
  public void testExtractVersion() {
    Assert.assertEquals("0.26.0", CompactSemVerUtil.extractVersion(">= 0.26.0"));
    Assert.assertEquals("0.26.0", CompactSemVerUtil.extractVersion(">=0.26.0"));
    Assert.assertEquals("0.23.0", CompactSemVerUtil.extractVersion("^0.23.0"));
    Assert.assertEquals("0.26.1", CompactSemVerUtil.extractVersion("== 0.26.1"));
    Assert.assertEquals("0.25.0", CompactSemVerUtil.extractVersion("0.25.0"));
    Assert.assertEquals("0.23", CompactSemVerUtil.extractVersion("0.23"));
  }
}
