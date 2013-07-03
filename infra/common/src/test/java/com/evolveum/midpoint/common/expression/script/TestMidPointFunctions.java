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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collection;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class TestMidPointFunctions {
    
    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @Test
    public void testDetermineLdapSingleAttributeValue01() throws Exception {
    	System.out.println("\n===[ testDetermineLdapSingleAttributeValue01 ]===\n");
    	
        // GIVEN
    	BasicExpressionFunctions f = createMidPointFunctions();
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
    	BasicExpressionFunctions f = createMidPointFunctions();
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
    	BasicExpressionFunctions f = createMidPointFunctions();
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
    	BasicExpressionFunctions f = createMidPointFunctions();
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
    	BasicExpressionFunctions f = createMidPointFunctions();
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

    
	private BasicExpressionFunctions createMidPointFunctions() throws SchemaException, SAXException, IOException {
		PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();
		return new BasicExpressionFunctions(prismContext);
	}

}
