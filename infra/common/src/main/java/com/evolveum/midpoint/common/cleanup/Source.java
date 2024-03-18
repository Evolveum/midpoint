package com.evolveum.midpoint.common.cleanup;

import java.io.File;

import org.jetbrains.annotations.Nullable;

public record Source(@Nullable File file, @Nullable String content) {

    public static final Source EMPTY = new Source(null, null);

    public static Source of(File file, String content) {
        return new Source(file, content);
    }
}
