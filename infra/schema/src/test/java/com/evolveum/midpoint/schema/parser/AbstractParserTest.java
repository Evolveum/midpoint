/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.parser;

import static com.evolveum.midpoint.schema.TestConstants.COMMON_DIR_PATH;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author mederly
 */
public abstract class AbstractParserTest extends AbstractUnitTest {

    protected String language;
    protected boolean namespaces;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

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
}
