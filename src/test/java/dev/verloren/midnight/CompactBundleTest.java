package dev.verloren.midnight;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CompactBundleTest {

  @Test
  public void testResolvesExistingLocalizationKeys() {
    String displayName = CompactBundle.message("status.bar.compact.display.name");
    assertEquals("Midnight Compact Toolchain", displayName);

    String tooltip = CompactBundle.message("status.bar.compact.tooltip", "0.28.0");
    assertEquals("Current Compact compiler: 0.28.0", tooltip);

    Supplier<String> pointer = CompactBundle.messagePointer("status.bar.compact.display.name");
    assertNotNull(pointer);
    assertEquals("Midnight Compact Toolchain", pointer.get());
  }

  @Test
  public void testDoesNotInvokeDeprecatedDynamicBundleConstructor() throws Exception {
    try (InputStream in = CompactBundle.class.getResourceAsStream("CompactBundle.class")) {
      assertNotNull("CompactBundle.class bytecode must be accessible via ClassLoader", in);
      byte[] bytes = in.readAllBytes();
      String bytecode = new String(bytes, StandardCharsets.ISO_8859_1);

      boolean invokesClassStringConstructor = bytecode.contains("(Ljava/lang/Class;Ljava/lang/String;)V");
      boolean invokesDeprecatedStringConstructor = bytecode.contains("com/intellij/DynamicBundle")
          && bytecode.contains("(Ljava/lang/String;)V");

      assertTrue(
          "CompactBundle must invoke non-deprecated DynamicBundle.<init>(Class, String) constructor",
          invokesClassStringConstructor
      );
      assertFalse(
          "CompactBundle must not reference deprecated DynamicBundle.<init>(String) constructor",
          invokesDeprecatedStringConstructor
      );
    }
  }
}
