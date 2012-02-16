package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismTestUtil.*;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

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

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class TestExtraSchema {
	
	/**
	 * Test is extra schema can be loaded to the schema registry and whether the file compliant to that
	 * schema can be validated.
	 */
	@Test
	public void testExtraSchema() throws SAXException, IOException, SchemaException {
		System.out.println("===[ testExtraSchema ]===");
		Document extraSchemaDoc = DOMUtil.parseFile(new File(EXTRA_SCHEMA_DIR, "root.xsd"));
		Document dataDoc = DOMUtil.parseFile(new File(OBJECT_DIR_PATH, "root-foo.xml"));

		PrismContext context = constructPrismContext();
		SchemaRegistry reg = context.getSchemaRegistry();
		reg.registerSchema(extraSchemaDoc, "file root.xsd");
		reg.initialize();
		Schema javaxSchema = reg.getJavaxSchema();
		assertNotNull(javaxSchema);
		
		Validator validator = javaxSchema.newValidator();
		DOMResult validationResult = new DOMResult();
		validator.validate(new DOMSource(dataDoc),validationResult);
//		System.out.println("Validation result:");
//		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}

	/**
	 * Test if a schema directory can be loaded to the schema registry. This contains definition of
	 * user extension, therefore check if it is applied to the user definition. 
	 */
	@Test
	public void testUserExtensionSchema() throws SAXException, IOException, SchemaException {
		System.out.println("===[ testUserExtensionSchema ]===");
		Document dataDoc = DOMUtil.parseFile(USER_JACK_FILE);
		
		PrismContext context = constructPrismContext();
		SchemaRegistry reg = context.getSchemaRegistry();
		reg.registerMidPointSchemasFromDirectory(EXTRA_SCHEMA_DIR);
		context.initialize();
		System.out.println("Initialized registry");
		System.out.println(reg.dump());
		
		// Try midpoint schemas by parsing a XML file
		PrismSchema schema = reg.getSchema(NS_FOO);
		System.out.println("Parsed foo schema:");
		System.out.println(schema.dump());
		PrismObject user = context.parseObject(DOMUtil.getFirstChildElement(dataDoc));
		assertNotNull("No definition for user", user.getDefinition());
		
		System.out.println("Parsed root object:");
		System.out.println(user.dump());

		schema = reg.getSchema(NS_USER_EXT);
		System.out.println("Parsed user ext schema:");
		System.out.println(schema.dump());
		QName userExtTypeQName = new QName(NS_USER_EXT,"UserExtensionType");
		ComplexTypeDefinition userExtComplexType = schema.findComplexTypeDefinition(userExtTypeQName);
		assertEquals("Extension type ref does not match", USER_TYPE_QNAME, userExtComplexType.getExtensionForType());
		
		// Try to fetch object schema, the extension of UserType should be there
		schema = reg.getObjectSchema();
		System.out.println("Object schema:");
		System.out.println(schema.dump());
		PrismObjectDefinition userDef = schema.findObjectDefinitionByType(USER_TYPE_QNAME);
		PrismContainerDefinition extDef = userDef.findContainerDefinition(USER_EXTENSION_QNAME);
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

}
