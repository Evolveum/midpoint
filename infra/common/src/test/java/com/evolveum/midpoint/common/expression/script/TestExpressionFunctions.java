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
package com.evolveum.midpoint.common.expression.script;

import com.evolveum.midpoint.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class TestExpressionFunctions {
	
	public static final File TEST_DIR = new File("src/test/resources/expression/functions");
	public static final File USER_JACK_FILE = new File(TEST_DIR, "user-jack.xml");
    
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @Test
    public void testGetExtensionPropertyValue() throws Exception {
    	final String TEST_NAME = "testGetExtensionPropertyValue";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	PrismObject<UserType> userJack = PrismTestUtil.parseObject(USER_JACK_FILE);

        // WHEN
        String shipExtension = f.getExtensionPropertyValue(userJack.asObjectable(), 
        		SchemaTestConstants.EXTENSION_SHIP_ELEMENT);

        // THEN
        assertEquals("Wrong value for extension "+SchemaTestConstants.EXTENSION_SHIP_ELEMENT, "Black Pearl", shipExtension);
    }
    
    @Test
    public void testGetExtensionPropertyValueParts() throws Exception {
    	final String TEST_NAME = "testGetExtensionPropertyValueParts";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	PrismObject<UserType> userJack = PrismTestUtil.parseObject(USER_JACK_FILE);

        // WHEN
        String shipExtension = f.getExtensionPropertyValue(userJack.asObjectable(), 
        		SchemaTestConstants.EXTENSION_SHIP_ELEMENT.getNamespaceURI(),
        		SchemaTestConstants.EXTENSION_SHIP_ELEMENT.getLocalPart());

        // THEN
        assertEquals("Wrong value for extension "+SchemaTestConstants.EXTENSION_SHIP_ELEMENT, "Black Pearl", shipExtension);
    }

    @Test
    public void testGetExtensionPropertyValueNotPresent() throws Exception {
    	final String TEST_NAME = "testGetExtensionPropertyValueNotPresent";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	PrismObject<UserType> userJack = PrismTestUtil.parseObject(USER_JACK_FILE);

        // WHEN
        String extensionVal = f.getExtensionPropertyValue(userJack.asObjectable(), 
        		new QName(SchemaTestConstants.NS_EXTENSION, "kajdsfhklfdsjh"));

        // THEN
        assertNull("Unexpected value for extension "+SchemaTestConstants.EXTENSION_SHIP_ELEMENT+": "+extensionVal, extensionVal);
    }
    
    @Test
    public void testGetExtensionPropertyValueNullObject() throws Exception {
    	final String TEST_NAME = "testGetExtensionPropertyValueNullObject";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();

        // WHEN
        String shipExtension = f.getExtensionPropertyValue(null, 
        		SchemaTestConstants.EXTENSION_SHIP_ELEMENT);

        // THEN
        assertNull("Unexpected value for extension "+SchemaTestConstants.EXTENSION_SHIP_ELEMENT+": "+shipExtension, shipExtension);
    }
    
    @Test
    public void testDetermineLdapSingleAttributeValue01() throws Exception {
    	final String TEST_NAME = "testDetermineLdapSingleAttributeValue01";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	String dn = "uid=foo, ou=People, dc=example,dc=com";
    	String attributeName = "uid";
    	Collection<String> values = MiscUtil.createCollection("bar", "foo", "FooBAR");

        // WHEN
        String resultValue = f.determineLdapSingleAttributeValue(dn, attributeName, values);

        // THEN
        assertNotNull("Result value is null", resultValue);
        System.out.println("Resulting value: "+resultValue);

        assertEquals("Wrong result value", "foo", resultValue);
    }

    @Test
    public void testDetermineLdapSingleAttributeValue02() throws Exception {
    	System.out.println("\n===[ testDetermineLdapSingleAttributeValue02 ]===\n");
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	String dn = "cn=jack+uid=FooBAR, ou=People, dc=example,dc=com";
    	String attributeName = "uid";
    	Collection<String> values = MiscUtil.createCollection("bar", "foo", "FooBAR");

        // WHEN
        String resultValue = f.determineLdapSingleAttributeValue(dn, attributeName, values);

        // THEN
        assertNotNull("Result value is null", resultValue);
        System.out.println("Resulting value: "+resultValue);

        assertEquals("Wrong result value", "FooBAR", resultValue);
    }

    /**
     * Single value is always returned regardless of DN
     */
    @Test
    public void testDetermineLdapSingleAttributeValueSingle() throws Exception {
    	System.out.println("\n===[ testDetermineLdapSingleAttributeValueSingle ]===\n");
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	String dn = "cn=jack+uid=FooBar, ou=People, dc=example,dc=com";
    	String attributeName = "uid";
    	Collection<String> values = MiscUtil.createCollection("heh");

        // WHEN
        String resultValue = f.determineLdapSingleAttributeValue(dn, attributeName, values);

        // THEN
        assertNotNull("Result value is null", resultValue);
        System.out.println("Resulting value: "+resultValue);

        assertEquals("Wrong result value", "heh", resultValue);
    }

    @Test
    public void testDetermineLdapSingleAttributeValueNull() throws Exception {
    	System.out.println("\n===[ testDetermineLdapSingleAttributeValueNull ]===\n");
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	String dn = "cn=jack+uid=FooBar, ou=People, dc=example,dc=com";
    	String attributeName = "uid";
    	Collection<String> values = MiscUtil.createCollection("heh");

        // WHEN
        String resultValue = f.determineLdapSingleAttributeValue(dn, attributeName, null);

        // THEN
        System.out.println("Resulting value: "+resultValue);
        assertNull("Result value is not null", resultValue);
    }
    
    @Test
    public void testDetermineLdapSingleAttributeValueFallback() throws Exception {
    	System.out.println("\n===[ testDetermineLdapSingleAttributeValueFallback ]===\n");
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	String dn = "cn=jack, ou=People, dc=example,dc=com";
    	String attributeName = "uid";
    	Collection<String> values = MiscUtil.createCollection("foo", "bar", "foobar");

        // WHEN
        String resultValue = f.determineLdapSingleAttributeValue(dn, attributeName, values);

        // THEN
        assertNotNull("Result value is null", resultValue);
        System.out.println("Resulting value: "+resultValue);

        assertEquals("Wrong result value", "bar", resultValue);
    }

    
	private BasicExpressionFunctions createBasicFunctions() throws SchemaException, SAXException, IOException {
		PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();
		return new BasicExpressionFunctions(prismContext);
	}

}
