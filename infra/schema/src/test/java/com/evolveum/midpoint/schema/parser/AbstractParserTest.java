/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.parser;

import static com.evolveum.midpoint.schema.TestConstants.COMMON_DIR_PATH;

import java.io.File;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.AbstractSchemaTest;

public abstract class AbstractParserTest extends AbstractSchemaTest {

    protected String language;
    protected boolean namespaces;

    @BeforeClass
    @Parameters({ "language", "namespaces" })
    public void temp(@Optional String language, @Optional Boolean namespaces) {
        this.language = language != null ? language : "xml";
        this.namespaces = namespaces != null ? namespaces : Boolean.TRUE;
        System.out.println("Testing with language = " + this.language + ", namespaces = " + this.namespaces);
    }

    protected File getFile(String baseName) {
        return new File(COMMON_DIR_PATH + "/" + language + "/" + (namespaces ? "ns" : "no-ns"),
                baseName + "." + language);
    }

    protected abstract File getFile();

    protected PrismContext getPrismContext() {
        return PrismTestUtil.getPrismContext();
    }

    boolean isJson() {
        return PrismContext.LANG_JSON.equals(language);
    }
}
