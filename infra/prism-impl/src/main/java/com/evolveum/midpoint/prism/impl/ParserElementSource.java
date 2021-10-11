/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.ParserSource;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author mederly
 */
public class ParserElementSource implements ParserSource {

    @NotNull private final Element element;

    public ParserElementSource(@NotNull Element element) {
        this.element = element;
    }

    @NotNull
    public Element getElement() {
        return element;
    }

    @NotNull
    @Override
    public InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException();
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
