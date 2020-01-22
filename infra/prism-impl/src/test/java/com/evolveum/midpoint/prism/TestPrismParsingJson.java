/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import org.testng.annotations.Test;

public class TestPrismParsingJson extends TestPrismParsing {

    @Override
    protected String getSubdirName() {
        return "json";
    }

    @Override
    protected String getFilenameSuffix() {
        return "json";
    }

    @Test
    public void f() {
    }

    @Override
    protected String getOutputFormat() {
        return PrismContext.LANG_JSON;
    }

}
