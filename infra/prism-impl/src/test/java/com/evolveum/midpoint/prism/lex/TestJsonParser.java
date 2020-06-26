/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.lex;

import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.lex.json.DelegatingLexicalProcessor;
import com.evolveum.midpoint.prism.impl.lex.json.reader.JsonReader;
import com.evolveum.midpoint.prism.impl.lex.json.writer.JsonWriter;
import com.evolveum.midpoint.prism.util.PrismTestUtil;

public class TestJsonParser extends DelegatingLexicalProcessorTest {

    @Override
    protected String getSubdirName() {
        return "json";
    }

    @Override
    protected String getFilenameSuffix() {
        return "json";
    }

    @Override
    protected LexicalProcessor<String> createLexicalProcessor() {
        return new DelegatingLexicalProcessor(
                new JsonReader(PrismTestUtil.getSchemaRegistry()),
                new JsonWriter());
    }

    @Override
    protected String getWhenItemSerialized() {
        return "\"when\" : \"2012-02-24T10:48:52.000Z\"";
    }
}
