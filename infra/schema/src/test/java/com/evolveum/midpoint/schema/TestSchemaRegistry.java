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
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.SchemaRegistry;
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
		
		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		Schema midPointSchema = reg.getJavaxSchema();
		assertNotNull(midPointSchema);
		
		// Try to use the schema to validate Jack
		Document document = DOMUtil.parseFile("src/test/resources/schema-registry/user-jack.xml");
		Validator validator = midPointSchema.newValidator();
		DOMResult validationResult = new DOMResult();
		validator.validate(new DOMSource(document),validationResult);
//		System.out.println("Validation result:");
//		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}
	
	@Test
	public void testExtraSchema() throws SAXException, IOException, SchemaException {
		Document extraSchemaDoc = DOMUtil.parseFile("src/test/resources/schema-registry/extra-schema.xsd");
		Document dataDoc = DOMUtil.parseFile("src/test/resources/schema-registry/data.xml");

		SchemaRegistry reg = new SchemaRegistry();
		reg.registerSchema(extraSchemaDoc, "extra schema extra-schema.xsd");
		reg.initialize();
		Schema midPointSchema = reg.getJavaxSchema();
		assertNotNull(midPointSchema);
		
		Validator validator = midPointSchema.newValidator();
		DOMResult validationResult = new DOMResult();
		validator.validate(new DOMSource(dataDoc),validationResult);
//		System.out.println("Validation result:");
//		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}

	@Test
	public void testExtraSchemaDir() throws SAXException, IOException, SchemaException {
		System.out.println("===[ testExtraSchemaDir ]===");
		File extraSchemaDir = new File("src/test/resources/schema-registry/schema-dir");
		Document dataDoc = DOMUtil.parseFile("src/test/resources/schema-registry/data-schemadir.xml");

		SchemaRegistry reg = new SchemaRegistry();
		reg.registerMidPointSchemasFromDirectory(extraSchemaDir);
		reg.initialize();
		System.out.println("Initialized registry");
		System.out.println(reg.dump());
		
		// Try midpoint schemas by parsing a XML file
		com.evolveum.midpoint.prism.Schema schema = reg.getSchema(FOO_NAMESPACE);
		System.out.println("Parsed foo schema:");
		System.out.println(schema.dump());
		QName rootType = new QName(FOO_NAMESPACE, "RootType");
		PrismContainerDefinition rootDef = schema.findContainerDefinitionByType(rootType);
		assertNotNull("No parsed definition for type "+rootType,rootDef);
		PrismContainer parsedRoot = rootDef.parseItem(DOMUtil.getFirstChildElement(dataDoc));
		System.out.println("Parsed root container:");
		System.out.println(parsedRoot.dump());

		schema = reg.getSchema(USER_EXT_NAMESPACE);
		System.out.println("Parsed user ext schema:");
		System.out.println(schema.dump());
		QName userExtTypeQName = new QName(USER_EXT_NAMESPACE,"UserExtensionType");
		ComplexTypeDefinition userExtComplexType = schema.findComplexTypeDefinition(userExtTypeQName);
		assertEquals("Extension type ref does not match", ObjectTypes.USER.getTypeQName(),userExtComplexType.getExtensionForType());
		
		// Try to fetch object schema, the extension of UserType should be there
		schema = reg.getObjectSchema();
		System.out.println("Object schema:");
		System.out.println(schema.dump());
		PrismObjectDefinition<UserType> userDef = schema.findObjectDefinition(UserType.class);
		PrismContainerDefinition extDef = userDef.findContainerDefinition(SchemaConstants.C_EXTENSION);
		assertTrue("Extension is not dynamic", extDef.isRuntimeSchema());
		assertEquals("Wrong extension type", userExtTypeQName, extDef.getTypeName());
		
		// Try javax schemas by validating a XML file
		Schema javaxSchema = reg.getJavaxSchema();
		assertNotNull(javaxSchema);
		Validator validator = javaxSchema.newValidator();
		DOMResult validationResult = new DOMResult();
		validator.validate(new DOMSource(dataDoc),validationResult);
//		System.out.println("Validation result:");
//		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}

	@Test
	public void testCommonSchemaUserType() throws SchemaException, SAXException, IOException {

		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		
		com.evolveum.midpoint.prism.Schema commonSchema = reg.getObjectSchema();
		assertNotNull("No parsed common schema", commonSchema);
		System.out.println("Parsed common schema:");
		System.out.println(commonSchema.dump());
		
		PrismContainerDefinition userContainer = commonSchema.findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		assertNotNull("No user container", userContainer);
		
		System.out.println("testCommonSchemaUserType:");
		System.out.println(userContainer.dump());
		
		assertFalse(userContainer.isWildcard());
		
		PrismPropertyDefinition nameDef = userContainer.findPropertyDefinition(SchemaConstants.C_NAME);
		assertNotNull("No name definition", nameDef);

		PrismContainerDefinition extensionDef = userContainer.findContainerDefinition(SchemaConstants.C_EXTENSION);
		assertNotNull("No 'extension' definition", extensionDef);
		assertTrue(extensionDef.isWildcard());
		
		PrismPropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"givenName"));
		assertNotNull("No givenName definition", givenNameDef);
	}
	
	@Test
	public void testCommonSchemaAccountType() throws SchemaException, SAXException, IOException {

		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		
		com.evolveum.midpoint.prism.Schema commonSchema = reg.getObjectSchema();
		assertNotNull("No parsed common schema", commonSchema);
		
		PrismObjectDefinition<AccountShadowType> accountDef = commonSchema.findObjectDefinition(ObjectTypes.ACCOUNT, AccountShadowType.class);
		assertNotNull("No account definition", accountDef);

		System.out.println("testCommonSchemaAccountType:");
		System.out.println(accountDef.dump());
		
		PrismPropertyDefinition nameDef = accountDef.findPropertyDefinition(SchemaConstants.C_NAME);
		assertNotNull("No name definition", nameDef);
		
		PrismContainerDefinition extensionDef = accountDef.findContainerDefinition(SchemaConstants.C_EXTENSION);
		assertNotNull("No 'extension' definition", extensionDef);
		assertTrue(extensionDef.isWildcard());
		
		PrismContainerDefinition attributesDef = accountDef.findContainerDefinition(SchemaConstants.I_ATTRIBUTES);
		assertNotNull("No 'attributes' definition", attributesDef);
		assertTrue(attributesDef.isWildcard());
	}
}
