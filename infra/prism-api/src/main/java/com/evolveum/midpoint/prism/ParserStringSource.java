/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author mederly
 */
public class ParserStringSource implements ParserSource {

    @NotNull private final String data;

    public ParserStringSource(@NotNull String data) {
        this.data = data;
    }

    @NotNull
    public String getData() {
        return data;
    }

    @NotNull
    @Override
    public InputStream getInputStream() {
        return IOUtils.toInputStream(data, StandardCharsets.UTF_8);
    }

    @Override
    public boolean closeStreamAfterParsing() {
        return true;
    }

    @Override
    public boolean throwsIOException() {
        return false;
    }
}
