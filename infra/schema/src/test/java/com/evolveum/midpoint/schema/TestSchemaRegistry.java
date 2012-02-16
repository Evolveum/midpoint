/**
 * 
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author Radovan Semancik
 *
 */
public class TestSchemaRegistry {

	private static final String FOO_NAMESPACE = "http://example.com/xml/ns/foo";
	private static final String USER_EXT_NAMESPACE = "http://example.com/xml/ns/user-extension";

	@Test
	public void testBasic() throws SAXException, IOException, SchemaException {
		
		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createAndInitializePrismContext();
		SchemaRegistry reg = context.getSchemaRegistry();
		Schema javaxSchema = reg.getJavaxSchema();
		assertNotNull(javaxSchema);
		
		// Try to use the schema to validate Jack
		Document document = DOMUtil.parseFile("src/test/resources/schema-registry/user-jack.xml");
		Validator validator = javaxSchema.newValidator();
		DOMResult validationResult = new DOMResult();
		validator.validate(new DOMSource(document),validationResult);
//		System.out.println("Validation result:");
//		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}
	
	@Test
	public void testExtraSchema() throws SAXException, IOException, SchemaException {
		Document extraSchemaDoc = DOMUtil.parseFile("src/test/resources/schema-registry/extra-schema.xsd");
		Document dataDoc = DOMUtil.parseFile("src/test/resources/schema-registry/data.xml");

		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createPrismContext();
		SchemaRegistry reg = context.getSchemaRegistry();
		reg.initialize();
		Schema javaxSchema = reg.getJavaxSchema();
		assertNotNull(javaxSchema);
		
		Validator validator = javaxSchema.newValidator();
		DOMResult validationResult = new DOMResult();
		validator.validate(new DOMSource(dataDoc),validationResult);
//		System.out.println("Validation result:");
//		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}

//	@Test
//	public void testCommonSchemaUserType() throws SchemaException, SAXException, IOException {
//
//		SchemaRegistry reg = new SchemaRegistry();
//		reg.initialize();
//		
//		com.evolveum.midpoint.prism.Schema commonSchema = reg.getObjectSchema();
//		assertNotNull("No parsed common schema", commonSchema);
//		System.out.println("Parsed common schema:");
//		System.out.println(commonSchema.dump());
//		
//		PrismContainerDefinition userContainer = commonSchema.findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
//		assertNotNull("No user container", userContainer);
//		
//		System.out.println("testCommonSchemaUserType:");
//		System.out.println(userContainer.dump());
//		
//		assertFalse(userContainer.isWildcard());
//		
//		PrismPropertyDefinition nameDef = userContainer.findPropertyDefinition(SchemaConstants.C_NAME);
//		assertNotNull("No name definition", nameDef);
//
//		PrismContainerDefinition extensionDef = userContainer.findContainerDefinition(SchemaConstants.C_EXTENSION);
//		assertNotNull("No 'extension' definition", extensionDef);
//		assertTrue(extensionDef.isWildcard());
//		
//		PrismPropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"givenName"));
//		assertNotNull("No givenName definition", givenNameDef);
//	}
//	
//	@Test
//	public void testCommonSchemaAccountType() throws SchemaException, SAXException, IOException {
//
//		SchemaRegistry reg = new SchemaRegistry();
//		reg.initialize();
//		
//		com.evolveum.midpoint.prism.Schema commonSchema = reg.getObjectSchema();
//		assertNotNull("No parsed common schema", commonSchema);
//		
//		PrismObjectDefinition<AccountShadowType> accountDef = commonSchema.findObjectDefinition(ObjectTypes.ACCOUNT, AccountShadowType.class);
//		assertNotNull("No account definition", accountDef);
//
//		System.out.println("testCommonSchemaAccountType:");
//		System.out.println(accountDef.dump());
//		
//		PrismPropertyDefinition nameDef = accountDef.findPropertyDefinition(SchemaConstants.C_NAME);
//		assertNotNull("No name definition", nameDef);
//		
//		PrismContainerDefinition extensionDef = accountDef.findContainerDefinition(SchemaConstants.C_EXTENSION);
//		assertNotNull("No 'extension' definition", extensionDef);
//		assertTrue(extensionDef.isWildcard());
//		
//		PrismContainerDefinition attributesDef = accountDef.findContainerDefinition(SchemaConstants.I_ATTRIBUTES);
//		assertNotNull("No 'attributes' definition", attributesDef);
//		assertTrue(attributesDef.isWildcard());
//	}
	
	private MidPointPrismContextFactory getContextFactory() {
		return new MidPointPrismContextFactory();
	}
	
}
