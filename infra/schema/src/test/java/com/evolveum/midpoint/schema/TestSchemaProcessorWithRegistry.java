/**
 * 
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author Radovan Semancik
 *
 */
public class TestSchemaProcessorWithRegistry {

	private static final String TEST_DIR = "src/test/resources/schema-registry/";
	private static final String NS_FOO = "http://www.example.com/foo";
	
	@Test
	public void testParseUserFromJaxb() throws SchemaException, SAXException, IOException, JAXBException {
		
		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		Schema commonSchema = reg.getCommonSchema();
		assertNotNull(commonSchema);
		
		// Try to use the schema to validate Jack
		JAXBElement<UserType> jaxbElement = JAXBUtil.unmarshal(new File(TEST_DIR, "user-jack.xml"), UserType.class);
		UserType userType = jaxbElement.getValue();
		
		ObjectDefinition<UserType> userDefinition = commonSchema.findObjectDefinitionByType(SchemaConstants.I_USER_TYPE);
		assertNotNull("UserType definition not found in parsed schema",userDefinition);
		
		// WHEN
		
		MidPointObject<UserType> user = userDefinition.parseObjectType(userType);
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(user.dump());
		
		assertProperty(user, SchemaConstants.C_NAME, "jack");
		assertProperty(user, new QName(SchemaConstants.NS_C,"fullName"), "Cpt. Jack Sparrow");
		assertProperty(user, new QName(SchemaConstants.NS_C,"givenName"), "Jack");
		assertProperty(user, new QName(SchemaConstants.NS_C,"familyName"), "Sparrow");
		assertProperty(user, new QName(SchemaConstants.NS_C,"honorificPrefix"), "Cpt.");
		assertProperty(user.findOrCreatePropertyContainer(SchemaConstants.C_EXTENSION),
				new QName(NS_FOO, "bar"), "BAR");
		Property password = user.findOrCreatePropertyContainer(SchemaConstants.C_EXTENSION).findProperty(new QName(NS_FOO, "password"));
		assertNotNull(password);
		// TODO: check inside
		assertProperty(user.findOrCreatePropertyContainer(SchemaConstants.C_EXTENSION),
				new QName(NS_FOO, "num"), 42);
		Property multi = user.findOrCreatePropertyContainer(SchemaConstants.C_EXTENSION).findProperty(new QName(NS_FOO, "multi"));
		assertEquals(3,multi.getValues().size());
		
		// WHEN
		
		Node domNode = user.serializeToDom();
		
		//THEN
		System.out.println("\nSerialized user:");
		System.out.println(DOMUtil.serializeDOMToString(domNode));
		
		Element userEl = DOMUtil.getFirstChildElement(domNode);
		assertEquals(SchemaConstants.I_USER,DOMUtil.getQName(userEl));
		
		// TODO: more asserts
	}
	
	// This fails and should fail. Accounts cannot be parsed like this ... at least not now
	@Test(enabled=false)
	public void testParseAccountFromJaxb() throws SchemaException, SAXException, IOException, JAXBException {
		
		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		Schema commonSchema = reg.getCommonSchema();
		assertNotNull(commonSchema);
		
		// Try to use the schema to validate Jack
		JAXBElement<AccountShadowType> jaxbElement = JAXBUtil.unmarshal(new File(TEST_DIR, "account-jack.xml"), AccountShadowType.class);
		AccountShadowType accType = jaxbElement.getValue();
		
		ObjectDefinition<AccountShadowType> accDefinition = commonSchema.findObjectDefinition(ObjectTypes.ACCOUNT, AccountShadowType.class);
		assertNotNull("account definition not found in parsed schema",accDefinition);
		
		MidPointObject<AccountShadowType> account = accDefinition.parseObjectType(accType);
		
		System.out.println("Parsed account:");
		System.out.println(account.dump());
	}
	
	private void assertProperty(PropertyContainer cont, QName propName, Object value) {
		Property prop = cont.findProperty(propName);
		assertNotNull(propName+" in null",prop);
		assertEquals(propName+" has wrong name", propName, prop.getName());
		assertEquals(propName+" has wrong value", value, prop.getValue());
	}

}
