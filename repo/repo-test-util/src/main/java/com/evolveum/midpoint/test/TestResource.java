/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import java.io.File;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * A file-based {@link TestObject}.
 *
 * DEPRECATED. Use {@link TestObject#file(File, String, String)} instead.
 */
@Deprecated
public class TestResource<T extends ObjectType> extends TestObject<T> {

    public TestResource(@NotNull File dir, @NotNull String fileName) {
        this(dir, fileName, null);
    }

    public TestResource(@NotNull File dir, @NotNull String fileName, String oid) {
        super(new FileBasedTestObjectSource(dir, fileName), oid);
    }

    public @NotNull File getFile() {
        return ((FileBasedTestObjectSource) source).getFile();
    }
}
