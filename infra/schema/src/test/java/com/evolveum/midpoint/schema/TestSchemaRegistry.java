/**
 * 
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;

/**
 * @author Radovan Semancik
 *
 */
public class TestSchemaRegistry {

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
		System.out.println("Validation result:");
		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}
	
	@Test
	public void testExtraSchema() throws SAXException, IOException, SchemaException {
		Document extraSchemaDoc = DOMUtil.parseFile("src/test/resources/schema-registry/extra-schema.xsd");
		Document dataDoc = DOMUtil.parseFile("src/test/resources/schema-registry/data.xml");

		SchemaRegistry reg = new SchemaRegistry();
		reg.registerSchema(extraSchemaDoc);
		reg.initialize();
		Schema midPointSchema = reg.getJavaxSchema();
		assertNotNull(midPointSchema);
		
		Validator validator = midPointSchema.newValidator();
		DOMResult validationResult = new DOMResult();
		validator.validate(new DOMSource(dataDoc),validationResult);
		System.out.println("Validation result:");
		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}
	
	@Test
	public void testCommonSchemaUserType() throws SchemaException, SAXException, IOException {

		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		
		com.evolveum.midpoint.schema.processor.Schema commonSchema = reg.getCommonSchema();
		assertNotNull("No parsed common schema", commonSchema);
		System.out.println("Parsed common schema:");
		System.out.println(commonSchema.dump());
		
		PropertyContainerDefinition userContainer = commonSchema.findContainerDefinitionByType(SchemaConstants.I_USER_TYPE);
		assertNotNull("No user container", userContainer);
		
		System.out.println("testCommonSchemaUserType:");
		System.out.println(userContainer.dump());
		
		assertFalse(userContainer.isWildcard());
		
		PropertyDefinition nameDef = userContainer.findPropertyDefinition(SchemaConstants.C_NAME);
		assertNotNull("No name definition", nameDef);

		PropertyContainerDefinition extensionDef = userContainer.findPropertyContainerDefinition(SchemaConstants.C_EXTENSION);
		assertNotNull("No 'extension' definition", extensionDef);
		assertTrue(extensionDef.isWildcard());
		
		PropertyDefinition givenNameDef = userContainer.findPropertyDefinition(new QName(SchemaConstants.NS_C,"givenName"));
		assertNotNull("No givenName definition", givenNameDef);
	}
	
	@Test
	public void testCommonSchemaAccountType() throws SchemaException, SAXException, IOException {

		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		
		com.evolveum.midpoint.schema.processor.Schema commonSchema = reg.getCommonSchema();
		assertNotNull("No parsed common schema", commonSchema);
		
		ObjectDefinition<AccountShadowType> accountDef = commonSchema.findObjectDefinition(ObjectTypes.ACCOUNT, AccountShadowType.class);
		assertNotNull("No account definition", accountDef);

		System.out.println("testCommonSchemaAccountType:");
		System.out.println(accountDef.dump());
		
		PropertyDefinition nameDef = accountDef.findPropertyDefinition(SchemaConstants.C_NAME);
		assertNotNull("No name definition", nameDef);
		
		PropertyContainerDefinition extensionDef = accountDef.findPropertyContainerDefinition(SchemaConstants.C_EXTENSION);
		assertNotNull("No 'extension' definition", extensionDef);
		assertTrue(extensionDef.isWildcard());
		
		PropertyContainerDefinition attributesDef = accountDef.findPropertyContainerDefinition(SchemaConstants.I_ATTRIBUTES);
		assertNotNull("No 'attributes' definition", attributesDef);
		assertTrue(attributesDef.isWildcard());
	}
}
