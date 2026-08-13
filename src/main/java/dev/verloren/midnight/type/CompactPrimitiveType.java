package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CompactPrimitiveType implements CompactType {
  public static final CompactPrimitiveType BOOLEAN = new CompactPrimitiveType("Boolean");
  public static final CompactPrimitiveType FIELD = new CompactPrimitiveType("Field");
  public static final CompactPrimitiveType UNKNOWN = new CompactPrimitiveType("Unknown");

  private final String name;

  public CompactPrimitiveType(String name) {
    this.name = name;
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompactPrimitiveType that = (CompactPrimitiveType) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public String toString() {
    return name;
  }
}
