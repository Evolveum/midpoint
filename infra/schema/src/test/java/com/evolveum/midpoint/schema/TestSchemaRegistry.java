/*
 * Copyright (c) 2010-2017 Evolveum
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

/**
 * 
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
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

        assertEquals("Wrong userRef definition class", PrismReferenceDefinitionImpl.class, itemDefinition.getClass());
    }

    @Test
	public void testUserType() throws Exception {
		
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
		PrismObjectDefinition<UserType> deepClone = userDefinition.deepClone(false);
		PrismObjectDefinition<UserType> ultraDeepClone = userDefinition.deepClone(true);
	}
    
    @Test
	public void testRoleType() throws Exception {
		
		MidPointPrismContextFactory factory = getContextFactory();
		PrismContext context = factory.createInitializedPrismContext();
		SchemaRegistry schemaRegistry = context.getSchemaRegistry();

		PrismObjectDefinition<RoleType> roleDefinition = schemaRegistry.findObjectDefinitionByCompileTimeClass(RoleType.class);
		assertNotNull("No role definition", roleDefinition);
		
		assertFalse("Role definition is marked as runtime", roleDefinition.isRuntimeSchema());
		
		PrismPropertyDefinition nameDef = roleDefinition.findPropertyDefinition(ObjectType.F_NAME);
		assertNotNull("No name definition", nameDef);

		PrismContainerDefinition extensionDef = roleDefinition.findContainerDefinition(ObjectType.F_EXTENSION);
		assertNotNull("No 'extension' definition", extensionDef);
		assertTrue("Extension definition is NOT marked as runtime", extensionDef.isRuntimeSchema());
		
		PrismPropertyDefinition identifierDef = roleDefinition.findPropertyDefinition(RoleType.F_IDENTIFIER);
		assertNotNull("No identifier definition", identifierDef);
		
		// Just make sure this does not end with NPE or stack overflow
		PrismObjectDefinition<RoleType> shallowClone = roleDefinition.clone();
		PrismObjectDefinition<RoleType> deepClone = roleDefinition.deepClone(false);
		PrismObjectDefinition<RoleType> ultraDeepClone = roleDefinition.deepClone(true);
	}

    @Test
	public void testCommonSchemaAccountType() throws SchemaException, SAXException, IOException {

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
