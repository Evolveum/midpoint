/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link AbstractTestResource} that resides among classpath-accessible resources.
 */
public class ClassPathTestResource<O extends ObjectType> extends AbstractTestResource<O> {

    @NotNull private final String name;

    private ClassPathTestResource(@NotNull String name, String oid) {
        super(oid);
        this.name = name;
    }

    public ClassPathTestResource(@NotNull String dir, @NotNull String name, String oid) {
        this(dir + "/" + name, oid);
    }

    @Override
    public @NotNull InputStream getInputStream() throws IOException {
        return MiscUtil.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(name),
                () -> new IllegalStateException("No resource '" + name + "' was found"));
    }

    @Override
    public @NotNull String getDescription() {
        return name + " (" + oid + ")";
    }
}
