/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.test.util;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensibleObjectType;

/**
 *
 * @author laszlohordos
 */
public class TestUtilTest {


    /**
     * Test of getSampleObject method, of class TestUtil.
     */
    @Test
    public void testGetSampleObject() {
        ExtensibleObjectType result = TestUtil.getSampleObject(SampleObjects.RESOURCETYPE_LOCALHOST_OPENDJ);
        AssertJUnit.assertNotNull(result);
        result = null;
        result = TestUtil.getSampleObject(SampleObjects.RESOURCEACCESSCONFIGURATIONTYPE_IDENTITY_CONNECTOR_INTEGRATION);
        AssertJUnit.assertNotNull(result);
        result = null;
        result = TestUtil.getSampleObject(SampleObjects.ACCOUNTSHADOWTYPE_OPENDJ_JBOND);
        AssertJUnit.assertNotNull(result);
        result = null;
        result = TestUtil.getSampleObject(SampleObjects.RESOURCETYPE_FLATFILE);
        AssertJUnit.assertNotNull(result);
        result = null;
        result = TestUtil.getSampleObject(SampleObjects.RESOURCETYPE_LOCALHOST_DATABASETABLE);
        AssertJUnit.assertNotNull(result);
    }

}