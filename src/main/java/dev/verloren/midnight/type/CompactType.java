package dev.verloren.midnight.type;

import org.jetbrains.annotations.NotNull;

public interface CompactType {
    @NotNull String getName();
    
    default boolean isAssignableTo(@NotNull CompactType other) {
        return this.equals(other);
    }
}
