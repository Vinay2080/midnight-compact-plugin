package dev.verloren.midnight.toolwindow;

import org.junit.Assert;
import org.junit.Test;

public class CompactVersionCardTest {

  @Test
  public void testActiveInstalledCardState() {
    CompactVersionCard card = new CompactVersionCard(
        "0.34.0",
        "Compact v0.34.0",
        true,
        true,
        true,
        false,
        "/path/to/compactc",
        () -> {},
        () -> {}
    );

    Assert.assertEquals("0.34.0", card.getVersion());
    Assert.assertTrue(card.isInstalled());
    Assert.assertTrue(card.isActive());
    Assert.assertEquals("/path/to/compactc", card.getInstalledPath());
  }

  @Test
  public void testUninstalledCardState() {
    CompactVersionCard card = new CompactVersionCard(
        "0.28.0",
        "Compact v0.28.0",
        false,
        false,
        false,
        false,
        null,
        () -> {},
        null
    );

    Assert.assertEquals("0.28.0", card.getVersion());
    Assert.assertFalse(card.isInstalled());
    Assert.assertFalse(card.isActive());
    Assert.assertNull(card.getInstalledPath());
  }

  @Test
  public void testUniformCardDimensionsAcrossStates() {
    CompactVersionCard installedCard = new CompactVersionCard(
        "0.34.0",
        "Compact v0.34.0",
        true,
        true,
        true,
        false,
        "/path/to/compactc",
        () -> {},
        () -> {}
    );

    CompactVersionCard uninstalledCard = new CompactVersionCard(
        "0.22.0",
        "Compact v0.22.0",
        false,
        false,
        false,
        false,
        null,
        () -> {},
        null
    );

    CompactVersionCard downloadingCard = new CompactVersionCard(
        "0.25.0",
        "Compact v0.25.0",
        false,
        false,
        false,
        true,
        null,
        () -> {},
        null
    );

    // Verify all cards have identical fixed height
    Assert.assertEquals(installedCard.getPreferredSize().height, uninstalledCard.getPreferredSize().height);
    Assert.assertEquals(installedCard.getPreferredSize().height, downloadingCard.getPreferredSize().height);
    Assert.assertEquals(installedCard.getMinimumSize().height, uninstalledCard.getMinimumSize().height);
    Assert.assertEquals(installedCard.getMaximumSize().height, uninstalledCard.getMaximumSize().height);
  }
}
