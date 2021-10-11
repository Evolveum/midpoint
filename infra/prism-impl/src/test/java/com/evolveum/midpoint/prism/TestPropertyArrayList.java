/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import java.io.File;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.impl.xjc.PropertyArrayList;
import com.evolveum.midpoint.util.DOMUtil;

public class TestPropertyArrayList extends AbstractPrismTest {

    private String getSubdirName() {
        return "xml";
    }

    private String getFilenameSuffix() {
        return "xml";
    }

    private File getCommonSubdir() {
        return new File(COMMON_DIR_PATH, getSubdirName());
    }

    private File getFile(String baseName) {
        return new File(getCommonSubdir(), baseName + "." + getFilenameSuffix());
    }

    @Test
    public void testPrismParseDom() throws Exception {
        // GIVEN
        Document document = DOMUtil.parseFile(getFile(USER_JACK_FILE_BASENAME));
        Element userElement = DOMUtil.getFirstChildElement(document);

        PrismContext prismContext = constructInitializedPrismContext();

        PrismObject<UserType> user = prismContext.parserFor(userElement).parse();
        PrismProperty<String> property = user.findProperty(UserType.F_ADDITIONAL_NAMES);
        PropertyArrayList<String> propertyArrayList = new PropertyArrayList<>(property, user.getValue());

        // WHEN
        System.out.println("Additional names before test: ");
        System.out.println(property.debugDump());
        String testName = "test-name";
        propertyArrayList.set(1, "test-name");

        // THEN
        System.out.println("Additional names after test: ");
        System.out.println(property.debugDump());
        assertEquals(testName, propertyArrayList.get(1));
    }
}
