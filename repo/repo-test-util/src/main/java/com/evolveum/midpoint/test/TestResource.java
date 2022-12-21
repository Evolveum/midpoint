/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * A file-based {@link AbstractTestResource}.
 *
 * TODO better name? There is a confusion with midPoint {@link ResourceType} objects here.
 *  `TestObject`, `TestFile`, or something else ?
 */
@Experimental
public class TestResource<T extends ObjectType> extends AbstractTestResource<T> {

    @NotNull private final File file;

    public TestResource(@NotNull File dir, @NotNull String fileName) {
        this(dir, fileName, null);
    }

    public TestResource(@NotNull File dir, @NotNull String fileName, String oid) {
        super(oid);
        this.file = new File(dir, fileName);
    }

    public @NotNull File getFile() {
        return file;
    }

    @Override
    public @NotNull InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public @NotNull String getDescription() {
        return file + " (" + oid + ")";
    }
}
