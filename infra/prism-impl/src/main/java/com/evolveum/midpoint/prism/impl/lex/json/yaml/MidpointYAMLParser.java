/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.lex.json.yaml;

import java.io.Reader;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

public class MidpointYAMLParser extends YAMLParser {

    public MidpointYAMLParser(IOContext ctxt, BufferRecycler br, int parserFeatures, int csvFeatures,
            ObjectCodec codec, Reader reader) {
        super(ctxt, br, parserFeatures, csvFeatures, codec, reader);
        // TODO Auto-generated constructor stub
    }
}
