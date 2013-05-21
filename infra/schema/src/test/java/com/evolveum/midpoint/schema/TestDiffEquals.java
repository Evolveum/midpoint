/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author lazyman
 */
public class TestDiffEquals {

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testUserSimplePropertyDiff() {
        UserType userType1 = new UserType();
        userType1.setName(PrismTestUtil.createPolyStringType("test name"));
        UserType userType2 = new UserType();
        userType2.setName(PrismTestUtil.createPolyStringType("test name"));

        ObjectDelta delta = userType1.asPrismObject().diff(userType2.asPrismObject());
        assertNotNull(delta);
        assertEquals(0, delta.getModifications().size());

        userType2.setDescription(null);

        delta = userType1.asPrismObject().diff(userType2.asPrismObject());
        assertNotNull(delta);
        assertEquals("Delta should be empty, nothing changed.", 0, delta.getModifications().size());
    }

    @Test
    public void testUserListSimpleDiff() {
        UserType u1 = new UserType();
        u1.setName(PrismTestUtil.createPolyStringType("test name"));
        UserType u2 = new UserType();
        u2.setName(PrismTestUtil.createPolyStringType("test name"));

        ObjectDelta delta = u1.asPrismObject().diff(u2.asPrismObject());
        assertNotNull(delta);
        assertEquals(0, delta.getModifications().size());

        u2.getAdditionalName();

        delta = u1.asPrismObject().diff(u2.asPrismObject());
        assertNotNull(delta);
        assertEquals("Delta should be empty, nothing changed.", 0, delta.getModifications().size());
    }
}
