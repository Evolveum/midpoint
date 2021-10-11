/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.util.DOMUtil;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class TestPrismParsingXml extends TestPrismParsing {

    public static final String USER_JACK_XXE_BASENAME = "user-jack-xxe";

    @Override
    protected String getSubdirName() {
        return "xml";
    }

    @Override
    protected String getFilenameSuffix() {
        return "xml";
    }

    @Override
    protected String getOutputFormat() {
        return PrismContext.LANG_XML;
    }

    @Test
    public void testPrismParseDom() throws Exception {
        // GIVEN
        Document document = DOMUtil.parseFile(getFile(USER_JACK_FILE_BASENAME));
        Element userElement = DOMUtil.getFirstChildElement(document);

        PrismContext prismContext = constructInitializedPrismContext();

        // WHEN
        PrismObject<UserType> user = prismContext.parserFor(userElement).parse();

        // THEN
        System.out.println("User:");
        System.out.println(user.debugDump());
        assertNotNull(user);

        assertUserJack(user, true, true);
    }

    @Test
    public void testPrismParseDomAdhoc() throws Exception {
        // GIVEN
        Document document = DOMUtil.parseFile(getFile(USER_JACK_ADHOC_BASENAME));
        Element userElement = DOMUtil.getFirstChildElement(document);

        PrismContext prismContext = constructInitializedPrismContext();

        // WHEN
        PrismObject<UserType> user = prismContext.parserFor(userElement).parse();

        // THEN
        System.out.println("User:");
        System.out.println(user.debugDump());
        assertNotNull(user);

        assertUserAdhoc(user, true, false);
    }

    @Test
    public void testPrismParseXxe() throws Exception {
        PrismContext prismContext = constructInitializedPrismContext();

        try {
            // WHEN
            prismContext.parseObject(getFile(USER_JACK_XXE_BASENAME));

            AssertJUnit.fail("Unexpected success");
        } catch (IllegalStateException e) {
            displayExpectedException(e);
            assertTrue("Unexpected exception message: "+e.getMessage(), e.getMessage().contains("DOCTYPE"));
        }
    }

    @Test
    public void testPrismParseDomXxe() {
        try {
            // WHEN
            DOMUtil.parseFile(getFile(USER_JACK_XXE_BASENAME));

            AssertJUnit.fail("Unexpected success");
        } catch (IllegalStateException e) {
            displayExpectedException(e);
            assertTrue("Unexpected exception message: "+e.getMessage(), e.getMessage().contains("DOCTYPE"));
        }
    }
}
