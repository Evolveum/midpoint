/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

/**
 *
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import java.io.IOException;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Radovan Semancik
 *
 */
public class TestSchemaRegistry {

	private static final String FOO_NAMESPACE = "http://example.com/xml/ns/foo";
	private static final String USER_EXT_NAMESPACE = "http://example.com/xml/ns/user-extension";
	private static final String EXTENSION_SCHEMA_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/test/extension";

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
//		PrismObject<UserType> user = context.parseObject(new File("src/test/resources/common/user-jack.xml"));
//		Element document = context.serializeToDom(user);
		Document document = DOMUtil.parseFile("src/test/resources/common/user-jack.xml");
		Validator validator = javaxSchema.newValidator();
		DOMResult validationResult = new DOMResult();
		validator.validate(new DOMSource(document), validationResult);
//		System.out.println("Validation result:");
//		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}


    @Test
	public void testCommonSchema() throws SchemaException, SAXException, IOException {

		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();

		PrismSchema commonSchema = schemaRegistry.findSchemaByNamespace(SchemaConstants.NS_C);
		assertNotNull("No parsed common schema", commonSchema);
		System.out.println("Parsed common schema:");
		System.out.println(commonSchema.debugDump());

		// TODO
	}

    @Test
    public void testReferenceInExtension() throws SchemaException, SAXException, IOException {

        MidPointPrismContextFactory factory = getContextFactory();
        PrismContext context = factory.createInitializedPrismContext();
        SchemaRegistry schemaRegistry = context.getSchemaRegistry();

        // Common schema should be parsed during creation of the context
		((SchemaRegistryImpl) schemaRegistry).loadPrismSchemaResource("schema/extension.xsd");

        // Check that the extension schema was loaded
        PrismSchema extensionSchema = schemaRegistry.findSchemaByNamespace(EXTENSION_SCHEMA_NAMESPACE);
        assertNotNull("Extension schema not parsed", extensionSchema);

        ItemDefinition itemDefinition = schemaRegistry.findItemDefinitionByElementName(TestConstants.EXTENSION_USER_REF_ELEMENT);
        assertNotNull("userRef element definition was not found", itemDefinition);
        System.out.println("UserRef definition:");
        System.out.println(itemDefinition.debugDump());

        assertTrue("Wrong userRef definition class: " + itemDefinition.getClass(), itemDefinition instanceof PrismReferenceDefinition);
    }

    @Test
	public void testUserType() throws Exception {
    	System.out.println("===[ testUserType ]===");

		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();

		PrismObjectDefinition<UserType> userDefinition = schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class);
		assertNotNull("No user definition", userDefinition);

		System.out.println("testUserType:");
		System.out.println(userDefinition.debugDump());

		assertFalse("User definition is marked as runtime", userDefinition.isRuntimeSchema());

		PrismPropertyDefinition<PolyString> nameDef = userDefinition.findPropertyDefinition(ObjectType.F_NAME);
		assertNotNull("No name definition", nameDef);

		PrismContainerDefinition extensionDef = userDefinition.findContainerDefinition(UserType.F_EXTENSION);
		assertNotNull("No 'extension' definition", extensionDef);
		assertTrue("Extension definition is NOT marked as runtime", extensionDef.isRuntimeSchema());

		PrismPropertyDefinition<PolyString> givenNameDef = userDefinition.findPropertyDefinition(UserType.F_GIVEN_NAME);
		assertNotNull("No givenName definition", givenNameDef);

		PrismPropertyDefinition<String> preferredLanguageDef = userDefinition.findPropertyDefinition(UserType.F_PREFERRED_LANGUAGE);
		assertNotNull("No preferredLanguage definition", preferredLanguageDef);
		PrismReferenceValue preferredLanguageValueEnumerationRef = preferredLanguageDef.getValueEnumerationRef();
		assertNotNull("No valueEnumerationRef in preferredLanguage definition", preferredLanguageValueEnumerationRef);
		assertEquals("Wrong OID in valueEnumerationRef in preferredLanguage definition",
				SystemObjectsType.LOOKUP_LANGUAGES.value(), preferredLanguageValueEnumerationRef.getOid());
		assertEquals("Wrong type in valueEnumerationRef in preferredLanguage definition",
				LookupTableType.COMPLEX_TYPE, preferredLanguageValueEnumerationRef.getTargetType());

		// Just make sure this does not end with NPE or stack overflow
		PrismObjectDefinition<UserType> shallowClone = userDefinition.clone();
		PrismObjectDefinition<UserType> deepClone = userDefinition.deepClone(false, null);
		PrismObjectDefinition<UserType> ultraDeepClone = userDefinition.deepClone(true, null);
	}

    @Test
	public void testRoleType() throws Exception {
    	System.out.println("\n\n===[ testRoleType ]===");

		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();

		PrismObjectDefinition<RoleType> roleDefinition = schemaRegistry.findObjectDefinitionByCompileTimeClass(RoleType.class);
		assertNotNull("No role definition", roleDefinition);
		System.out.println("\nRole definition");
		System.out.println(roleDefinition.debugDump(1));
		System.out.println("\nRole definition CTD");
		System.out.println(roleDefinition.getComplexTypeDefinition().debugDump(1));

		assertFalse("Role definition is marked as runtime", roleDefinition.isRuntimeSchema());

		PrismPropertyDefinition nameDef = roleDefinition.findPropertyDefinition(ObjectType.F_NAME);
		assertNotNull("No name definition", nameDef);

		PrismContainerDefinition extensionDef = roleDefinition.findContainerDefinition(ObjectType.F_EXTENSION);
		assertNotNull("No 'extension' definition", extensionDef);
		assertTrue("Extension definition is NOT marked as runtime", extensionDef.isRuntimeSchema());

		PrismPropertyDefinition identifierDef = roleDefinition.findPropertyDefinition(RoleType.F_IDENTIFIER);
		assertNotNull("No identifier definition", identifierDef);

		List<SchemaMigration> schemaMigrations = roleDefinition.getSchemaMigrations();
		assertNull("Unexpected schema migrations in role definition", schemaMigrations);

		// Just make sure this does not end with NPE or stack overflow
		PrismObjectDefinition<RoleType> shallowClone = roleDefinition.clone();
		PrismObjectDefinition<RoleType> deepClone = roleDefinition.deepClone(false, null);
		PrismObjectDefinition<RoleType> ultraDeepClone = roleDefinition.deepClone(true, null);
	}
    
    @Test
	public void testAbstractRoleType() throws Exception {
    	System.out.println("\n\n===[ testAbstractRoleType ]===");

		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();

		PrismObjectDefinition<AbstractRoleType> abstractRoleDefinition = schemaRegistry.findObjectDefinitionByCompileTimeClass(AbstractRoleType.class);
		assertNotNull("No role definition", abstractRoleDefinition);
		System.out.println("\nAbstractRole definition");
		System.out.println(abstractRoleDefinition.debugDump(1));
		System.out.println("\nAbstractRole definition CTD");
		System.out.println(abstractRoleDefinition.getComplexTypeDefinition().debugDump(1));

		assertFalse("Role definition is marked as runtime", abstractRoleDefinition.isRuntimeSchema());

		PrismPropertyDefinition nameDef = abstractRoleDefinition.findPropertyDefinition(ObjectType.F_NAME);
		assertNotNull("No name definition", nameDef);

		PrismContainerDefinition extensionDef = abstractRoleDefinition.findContainerDefinition(ObjectType.F_EXTENSION);
		assertNotNull("Unexpected 'extension' definition", extensionDef);
		assertTrue("Extension definition is NOT marked as runtime", extensionDef.isRuntimeSchema());
		assertTrue("Extension definition is NOT empty", extensionDef.isEmpty());

		PrismPropertyDefinition identifierDef = abstractRoleDefinition.findPropertyDefinition(AbstractRoleType.F_IDENTIFIER);
		assertNotNull("No identifier definition", identifierDef);

		List<SchemaMigration> schemaMigrations = abstractRoleDefinition.getSchemaMigrations();
		assertEquals("Wrong number of schema migrations in role definition", 8, schemaMigrations.size());

		// Just make sure this does not end with NPE or stack overflow
		PrismObjectDefinition<AbstractRoleType> shallowClone = abstractRoleDefinition.clone();
		PrismObjectDefinition<AbstractRoleType> deepClone = abstractRoleDefinition.deepClone(false, null);
		PrismObjectDefinition<AbstractRoleType> ultraDeepClone = abstractRoleDefinition.deepClone(true, null);
	}

    @Test
	public void testCommonSchemaAccountType() throws SchemaException, SAXException, IOException {
    	System.out.println("===[ testCommonSchemaAccountType ]===");

		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();

		PrismObjectDefinition<ShadowType> accountDef = schemaRegistry.findObjectDefinitionByCompileTimeClass(ShadowType.class);
		assertNotNull("No account definition", accountDef);

		System.out.println("testCommonSchemaAccountType:");
		System.out.println(accountDef.debugDump());

		PrismPropertyDefinition nameDef = accountDef.findPropertyDefinition(ShadowType.F_NAME);
		assertNotNull("No name definition", nameDef);

		PrismContainerDefinition extensionDef = accountDef.findContainerDefinition(ShadowType.F_EXTENSION);
		assertNotNull("No 'extension' definition", extensionDef);
		assertTrue("'extension' definition is not marked as runtime", extensionDef.isRuntimeSchema());

		PrismContainerDefinition attributesDef = accountDef.findContainerDefinition(ShadowType.F_ATTRIBUTES);
		assertNotNull("No 'attributes' definition", attributesDef);
		assertTrue("'attributes' definition is not marked as runtime", attributesDef.isRuntimeSchema());
	}

	private MidPointPrismContextFactory getContextFactory() {
		return new MidPointPrismContextFactory();
	}

}
