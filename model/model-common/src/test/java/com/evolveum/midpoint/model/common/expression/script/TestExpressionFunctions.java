/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.ProtectorImpl;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class TestExpressionFunctions {
	
	public static final File TEST_DIR = new File("src/test/resources/expression/functions");
	public static final File USER_JACK_FILE = new File(TEST_DIR, "user-jack.xml");
	public static final File ACCOUNT_JACK_FILE = new File(TEST_DIR, "account-jack.xml");
	public static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	private static final String ATTR_FULLNAME_LOCAL_PART = "fullname";
	private static final String ATTR_WEAPON_LOCAL_PART = "weapon";
    
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
    public void testGetAttributeValueParts() throws Exception {
    	final String TEST_NAME = "testGetAttributeValueParts";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	PrismObject<ShadowType> accountJack = PrismTestUtil.parseObject(ACCOUNT_JACK_FILE);

        // WHEN
        String attrVal = f.getAttributeValue(accountJack.asObjectable(),
        		MidPointConstants.NS_RI,
        		ATTR_FULLNAME_LOCAL_PART);

        // THEN
        assertEquals("Wrong value for attribute "+ATTR_FULLNAME_LOCAL_PART, "Jack Sparrow", attrVal);
    }

    @Test
    public void testGetAttributeValueDefaultRi() throws Exception {
    	final String TEST_NAME = "testGetAttributeValueDefaultRi";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	PrismObject<ShadowType> accountJack = PrismTestUtil.parseObject(ACCOUNT_JACK_FILE);

        // WHEN
        String attrVal = f.getAttributeValue(accountJack.asObjectable(),
        		ATTR_FULLNAME_LOCAL_PART);

        // THEN
        assertEquals("Wrong value for attribute "+ATTR_FULLNAME_LOCAL_PART, "Jack Sparrow", attrVal);
    }

    @Test
    public void testGetAttributeValuesParts() throws Exception {
    	final String TEST_NAME = "testGetAttributeValuesParts";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	PrismObject<ShadowType> accountJack = PrismTestUtil.parseObject(ACCOUNT_JACK_FILE);

        // WHEN
        Collection<String> attrVals = f.getAttributeValues(accountJack.asObjectable(),
        		MidPointConstants.NS_RI,
        		ATTR_WEAPON_LOCAL_PART);

        // THEN
        TestUtil.assertSetEquals("Wrong value for attribute "+ATTR_WEAPON_LOCAL_PART, attrVals, "rum", "smell");
    }

    @Test
    public void testGetAttributeValuesDefaultRi() throws Exception {
    	final String TEST_NAME = "testGetAttributeValuesDefaultRi";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	PrismObject<ShadowType> accountJack = PrismTestUtil.parseObject(ACCOUNT_JACK_FILE);

        // WHEN
        Collection<String> attrVals = f.getAttributeValues(accountJack.asObjectable(),
        		ATTR_WEAPON_LOCAL_PART);

        // THEN
        TestUtil.assertSetEquals("Wrong value for attribute "+ATTR_WEAPON_LOCAL_PART, attrVals, "rum", "smell");
    }
    
    @Test
    public void testgetResourceIcfConfigurationPropertyValueStringHost() throws Exception {
    	final String TEST_NAME = "testgetResourceIcfConfigurationPropertyValueStringHost";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_OPENDJ_FILE);

        // WHEN
        String val = f.getResourceIcfConfigurationPropertyValue(resource.asObjectable(), "host");

        // THEN
        assertEquals("Wrong value of ICF configuration property", "localhost", val);
    }
    
    @Test
    public void testgetResourceIcfConfigurationPropertyValueStringPort() throws Exception {
    	final String TEST_NAME = "testgetResourceIcfConfigurationPropertyValueStringPort";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_OPENDJ_FILE);

        // WHEN
        int val = f.getResourceIcfConfigurationPropertyValue(resource.asObjectable(), "port");

        // THEN
        assertEquals("Wrong value of ICF configuration property", 10389, val);
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

    @Test
    public void testFormatDateTime() throws Exception {
    	System.out.println("\n===[ testFormatDateTime ]===\n");
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	XMLGregorianCalendar xmlCal = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 22, 30, 0, 0, DatatypeConstants.FIELD_UNDEFINED); // don't use GMT (offset 0) because serialized value is then in local time
    	
        // WHEN
        String resultValue = f.formatDateTime("yyyy MM dd HH:mm:ss.SSS", xmlCal);		// don't include timezone in the format string, it is hard to check then

        // THEN
        assertNotNull("Result value is null", resultValue);
        System.out.println("Resulting value: "+resultValue);

        assertEquals("Wrong result value", "1975 05 30 22:30:00.000", resultValue);
    }

    @Test
    public void testParseDateTime() throws Exception {
    	final String TEST_NAME = "testParseDateTime";
    	TestUtil.displayTestTile(TEST_NAME);
    	
        // GIVEN
    	BasicExpressionFunctions f = createBasicFunctions();
    	
        // WHEN
        XMLGregorianCalendar resultValue = f.parseDateTime("yyyy MM dd HH:mm:ss.SSS zzzz",
        		"1975 05 30 22:30:00.000 Central European Time");

        // THEN
        assertNotNull("Result value is null", resultValue);
        System.out.println("Resulting value: "+resultValue);

        XMLGregorianCalendar xmlCal = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0);
        assertEquals("Wrong result value", xmlCal, resultValue);
    }

    
	private BasicExpressionFunctions createBasicFunctions() throws SchemaException, SAXException, IOException {
		PrismContext prismContext = PrismTestUtil.createInitializedPrismContext();
		Protector protector = new ProtectorImpl();
		return new BasicExpressionFunctions(prismContext, protector);
	}
	
	@Test
	public void testStringify() throws Exception {
		final String TEST_NAME = "testStringifyString";
		TestUtil.displayTestTile(TEST_NAME);
		BasicExpressionFunctions basic = createBasicFunctions();
		assertEquals("foo", basic.stringify("foo"));
		assertEquals("foo", basic.stringify(poly("foo")));
		assertEquals("foo", basic.stringify(PrismTestUtil.createPolyStringType("foo")));
		assertEquals("42", basic.stringify(42));
		assertEquals("", basic.stringify(null));
		assertEquals("", basic.stringify(""));
	}
	
	@Test
	public void testConcatName() throws Exception {
		final String TEST_NAME = "testConcatName";
		TestUtil.displayTestTile(TEST_NAME);
		BasicExpressionFunctions basic = createBasicFunctions();
		assertEquals("foo bar", basic.concatName("foo","bar"));
		assertEquals("foo bar", basic.concatName(poly("foo"),"bar"));
		assertEquals("foo bar", basic.concatName("foo",poly("bar")));
		assertEquals("foo", basic.concatName("foo",""));
		assertEquals("foo", basic.concatName("foo",null));
		assertEquals("foo bar", basic.concatName("foo",null,"bar"));
		assertEquals("foo bar", basic.concatName("foo","","bar"));
		assertEquals("foo bar", basic.concatName("foo ","bar"));
		assertEquals("foo bar", basic.concatName("foo"," bar"));
		assertEquals("foo bar", basic.concatName("   foo   ","  bar        "));
		assertEquals("foo bar", basic.concatName("   foo   ",null,"  bar        "));
		assertEquals("foo bar", basic.concatName("   foo   ","    ","  bar        "));
	}
	
	private PolyString poly(String s) {
		return PrismTestUtil.createPolyString(s);
	}

	@Test
	public void testToAscii() throws Exception {
		final String TEST_NAME = "testToAscii";
		TestUtil.displayTestTile(TEST_NAME);
		BasicExpressionFunctions basic = createBasicFunctions();
		assertEquals("foo", basic.toAscii("foo"));
		assertEquals("foo", basic.toAscii(poly("foo")));
		assertEquals("foo", basic.toAscii(PrismTestUtil.createPolyStringType("foo")));
		assertEquals("Cortuv hrad, tam Strasa!", basic.toAscii("Čórtův hrád, tam Strašá!"));
		assertEquals("hrabe Teleke z Toloko", basic.toAscii(poly("hrabě Teleke z Tölökö")));
		assertEquals("Vedeckotechnicka revoluce neni zadna idyla!", basic.toAscii(PrismTestUtil.createPolyStringType("Vědeckotechnická revoluce není žádná idyla!")));
		assertEquals(null, basic.toAscii(null));
		assertEquals("", basic.toAscii(""));
	}
	
	@Test
	public void testComposeDn() throws Exception {
		final String TEST_NAME = "testComposeDn";
		TestUtil.displayTestTile(TEST_NAME);
		BasicExpressionFunctions basic = createBasicFunctions();
		
		assertEquals("cn=foo,o=bar", basic.composeDn("cn","foo","o","bar"));
		assertEquals("cn=foo,o=bar", basic.composeDn("cn",PrismTestUtil.createPolyString("foo"),"o","bar"));
		assertEquals("cn=foo,o=bar", basic.composeDn("cn",PrismTestUtil.createPolyStringType("foo"),"o","bar"));
		assertEquals("cn=foo,o=bar", basic.composeDn("cn","foo",new Rdn("o","bar")));
		assertEquals("cn=foo,ou=baz,o=bar", basic.composeDn(new Rdn("cn","foo"),"ou","baz",new Rdn("o","bar")));
		assertEquals("cn=foo,ou=baz,o=bar", basic.composeDn(new Rdn("cn","foo"),"ou","baz","o","bar"));
		assertEquals("cn=foo,ou=baz,o=bar", basic.composeDn(new Rdn("cn","foo"),new LdapName("ou=baz,o=bar")));
		assertEquals("cn=foo,ou=baz,o=bar", basic.composeDn("cn","foo",new LdapName("ou=baz,o=bar")));
		assertEquals("cn=foo\\,foo,ou=baz,o=bar", basic.composeDn("cn","foo,foo",new LdapName("ou=baz,o=bar")));
		assertEquals("cn=foo\\=foo,ou=baz,o=bar", basic.composeDn("cn","foo=foo",new LdapName("ou=baz,o=bar")));
		assertEquals(null, basic.composeDn(null));
		assertEquals(null, basic.composeDn());
		assertEquals(null, basic.composeDn(""));
		assertEquals(null, basic.composeDn("   "));
	}
	
	@Test
	public void testComposeDnWithSuffix() throws Exception {
		final String TEST_NAME = "testComposeDnWithSuffix";
		TestUtil.displayTestTile(TEST_NAME);
		BasicExpressionFunctions basic = createBasicFunctions();
		
		assertEquals("cn=foo,ou=baz,o=bar", basic.composeDnWithSuffix(new Rdn("cn","foo"),"ou=baz,o=bar"));
		assertEquals("cn=foo,ou=baz,o=bar", basic.composeDnWithSuffix(new Rdn("cn","foo"),new LdapName("ou=baz,o=bar")));
		assertEquals("cn=foo,ou=baz,o=bar", basic.composeDnWithSuffix("cn","foo","ou=baz,o=bar"));
		assertEquals("cn=foo,ou=baz,o=bar", basic.composeDnWithSuffix("cn",PrismTestUtil.createPolyString("foo"),"ou=baz,o=bar"));
		assertEquals("cn=foo,ou=baz,o=bar", basic.composeDnWithSuffix("cn",PrismTestUtil.createPolyStringType("foo"),"ou=baz,o=bar"));
		assertEquals("cn=foo,ou=baz,o=bar", basic.composeDnWithSuffix("cn","foo",new LdapName("ou=baz,o=bar")));
		assertEquals("cn=foo,ou=baz\\,baz,o=bar", basic.composeDnWithSuffix("cn","foo","ou=baz\\,baz,o=bar"));
		assertEquals("cn=foo,ou=baz\\,baz,o=bar", basic.composeDnWithSuffix("cn","foo",new LdapName("ou=baz\\,baz,o=bar")));
		assertEquals("cn=foo\\,foo,ou=baz,o=bar", basic.composeDnWithSuffix("cn","foo,foo","ou=baz,o=bar"));
		assertEquals("cn=foo\\,foo,ou=baz,o=bar", basic.composeDnWithSuffix("cn","foo,foo",new LdapName("ou=baz,o=bar")));
		assertEquals("cn=foo\\=foo,ou=baz,o=bar", basic.composeDnWithSuffix("cn","foo=foo","ou=baz,o=bar"));
		assertEquals("cn=foo\\=foo,ou=baz,o=bar", basic.composeDnWithSuffix("cn","foo=foo",new LdapName("ou=baz,o=bar")));
		assertEquals("ou=baz,o=bar", basic.composeDnWithSuffix("ou=baz,o=bar"));
		assertEquals("ou=baz, o=bar", basic.composeDnWithSuffix("ou=baz, o=bar"));
		assertEquals("OU=baz, o=bar", basic.composeDnWithSuffix("OU=baz, o=bar"));
		assertEquals("ou=baz,o=bar", basic.composeDnWithSuffix(new LdapName("ou=baz,o=bar")));
		assertEquals(null, basic.composeDnWithSuffix(null));
		assertEquals(null, basic.composeDnWithSuffix());
		assertEquals(null, basic.composeDnWithSuffix(""));
		assertEquals(null, basic.composeDnWithSuffix("   "));
	}
	
	@Test
	public void testParseFullName() throws Exception {
		final String TEST_NAME = "testParseFullName";
		TestUtil.displayTestTile(TEST_NAME);
		BasicExpressionFunctions basic = createBasicFunctions();
		
		assertEquals(null, basic.parseGivenName(null));
		assertEquals(null, basic.parseGivenName("   "));
		
		assertEquals("Jack", basic.parseGivenName("Jack Sparrow"));
		assertEquals("Jack", basic.parseGivenName(" Jack     Sparrow  "));
		assertEquals("Jack", basic.parseGivenName(new PolyString("Jack Sparrow")));
		assertEquals("Sparrow", basic.parseFamilyName("Jack Sparrow"));
		assertEquals("Sparrow", basic.parseFamilyName("   Jack    Sparrow   "));
		
		assertEquals("Tim", basic.parseGivenName("Tim Berners-Lee"));
		assertEquals("Berners-Lee", basic.parseFamilyName("Tim Berners-Lee"));
		
		assertEquals("Cassius", basic.parseGivenName("Cassius Marcellus \"Muhammad Ali\" Clay, Jr."));
		assertEquals("Marcellus", basic.parseAdditionalName("Cassius Marcellus \"Muhammad Ali\" Clay, Jr."));
		assertEquals("Clay", basic.parseFamilyName("Cassius Marcellus \"Muhammad Ali\" Clay, Jr."));
		assertEquals("Muhammad Ali", basic.parseNickName("Cassius Marcellus \"Muhammad Ali\" Clay, Jr."));
		assertEquals("Jr.", basic.parseHonorificSuffix("Cassius Marcellus \"Muhammad Ali\" Clay, Jr."));
		
		assertEquals("Radovan", basic.parseGivenName("Ing. Radovan Semančík, PhD."));
		assertEquals("Semančík", basic.parseFamilyName("Ing. Radovan Semančík, PhD."));
		assertEquals("Ing.", basic.parseHonorificPrefix("Ing. Radovan Semančík, PhD."));
		assertEquals("PhD.", basic.parseHonorificSuffix("Ing. Radovan Semančík, PhD."));
	}
	
}
