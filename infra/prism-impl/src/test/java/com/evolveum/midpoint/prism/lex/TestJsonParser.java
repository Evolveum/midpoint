/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.lex;

import com.evolveum.midpoint.prism.impl.lex.json.JsonLexicalProcessor;
import com.evolveum.midpoint.prism.util.PrismTestUtil;

public class TestJsonParser extends AbstractJsonLexicalProcessorTest {

    @Override
    protected String getSubdirName() {
        return "json";
    }

    @Override
    protected String getFilenameSuffix() {
        return "json";
    }

    @Override
    protected JsonLexicalProcessor createParser() {
        return new JsonLexicalProcessor(PrismTestUtil.getSchemaRegistry());
    }

    @Override
    protected String getWhenItemSerialized() {
        return "\"when\" : \"2012-02-24T10:48:52.000Z\"";
    }
}
