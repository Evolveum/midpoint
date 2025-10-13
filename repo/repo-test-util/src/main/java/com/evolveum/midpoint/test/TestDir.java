/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import java.io.File;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/** Directory containing test objects as files. */
@Experimental
public class TestDir {

    /** The directory in file system. */
    @NotNull private final File file;

    private TestDir(@NotNull File file) {
        this.file = file;
    }

    public static TestDir of(@NotNull File file) {
        return new TestDir(file);
    }

    public static TestDir of(@NotNull String fileName) {
        return new TestDir(new File(fileName));
    }

    public <O extends ObjectType> TestObject<O> object(@NotNull String fileName, @NotNull String oid) {
        return TestObject.file(this.file, fileName, oid);
    }
}
