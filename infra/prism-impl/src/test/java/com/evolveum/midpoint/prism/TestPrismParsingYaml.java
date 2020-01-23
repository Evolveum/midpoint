/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

public class TestPrismParsingYaml extends TestPrismParsing {

    @Override
    protected String getSubdirName() {
        return "yaml";
    }

    @Override
    protected String getFilenameSuffix() {
        return "yaml";
    }

    @Override
    protected String getOutputFormat() {
        return PrismContext.LANG_YAML;
    }

}
