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
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author Radovan Semancik
 *
 */
public class TestSchemaRegistry {

	private static final String FOO_NAMESPACE = "http://example.com/xml/ns/foo";
	private static final String USER_EXT_NAMESPACE = "http://example.com/xml/ns/user-extension";

	/**
	 * Test whether the midpoint prism context was constructed OK and if it can validate
	 * ordinary user object.
	 */
	@Test
	public void testBasic() throws SAXException, IOException, SchemaException {
		
		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
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
	public void testCommonSchema() throws SchemaException, SAXException, IOException {

		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();
		
		PrismSchema commonSchema = schemaRegistry.getObjectSchema();
		assertNotNull("No parsed common schema", commonSchema);
		System.out.println("Parsed common schema:");
		System.out.println(commonSchema.dump());
		
		// TODO
	}
	
	@Test
	public void testUserType() throws SchemaException, SAXException, IOException {
		
		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();
		
		PrismObjectDefinition<UserType> userContainer = schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class);
		assertNotNull("No user container", userContainer);
		
		System.out.println("testCommonSchemaUserType:");
		System.out.println(userContainer.dump());
		
		assertFalse(userContainer.isWildcard());
		
		PrismPropertyDefinition nameDef = userContainer.findPropertyDefinition(ObjectType.F_NAME);
		assertNotNull("No name definition", nameDef);

		PrismContainerDefinition extensionDef = userContainer.findContainerDefinition(SchemaConstants.C_EXTENSION);
		assertNotNull("No 'extension' definition", extensionDef);
		assertTrue(extensionDef.isWildcard());
		
		PrismPropertyDefinition givenNameDef = userContainer.findPropertyDefinition(UserType.F_GIVEN_NAME);
		assertNotNull("No givenName definition", givenNameDef);
	}
	
	@Test
	public void testCommonSchemaAccountType() throws SchemaException, SAXException, IOException {

		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();
				
		PrismObjectDefinition<AccountShadowType> accountDef = schemaRegistry.findObjectDefinitionByCompileTimeClass(AccountShadowType.class);
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
	
	private MidPointPrismContextFactory getContextFactory() {
		return new MidPointPrismContextFactory();
	}
	
}
