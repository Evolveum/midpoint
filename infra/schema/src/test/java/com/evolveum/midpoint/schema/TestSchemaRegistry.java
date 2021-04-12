/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.*;

import java.io.IOException;
import java.util.List;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import com.evolveum.midpoint.prism.annotation.DiagramElementFormType;
import com.evolveum.midpoint.prism.annotation.DiagramElementInclusionType;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TestSchemaRegistry extends AbstractUnitTest {

    @SuppressWarnings("unused") // for the future
    private static final String FOO_NAMESPACE = "http://example.com/xml/ns/foo";
    @SuppressWarnings("unused") // for the future
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
//        PrismObject<UserType> user = context.parseObject(new File("src/test/resources/common/user-jack.xml"));
//        Element document = context.serializeToDom(user);
        Document document = DOMUtil.parseFile("src/test/resources/common/user-jack.xml");
        Validator validator = javaxSchema.newValidator();
        DOMResult validationResult = new DOMResult();
        validator.validate(new DOMSource(document), validationResult);
//        System.out.println("Validation result:");
//        System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
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
        List<ItemDiagramSpecification> diagrams = givenNameDef.getDiagrams();
        assertNotNull("No diagrams in user definition", diagrams);
        assertEquals("Unexpected number of diagrams in user definition", 1, diagrams.size());
        assertEquals("Unexpected name of diagram in user definition", "user-overview", diagrams.get(0).getName());
        assertEquals("Unexpected name of diagram in user definition", DiagramElementInclusionType.INCLUDE, diagrams.get(0).getInclusion());

        PrismPropertyDefinition<String> preferredLanguageDef = userDefinition.findPropertyDefinition(UserType.F_PREFERRED_LANGUAGE);
        assertNotNull("No preferredLanguage definition", preferredLanguageDef);
        PrismReferenceValue preferredLanguageValueEnumerationRef = preferredLanguageDef.getValueEnumerationRef();
        assertNotNull("No valueEnumerationRef in preferredLanguage definition", preferredLanguageValueEnumerationRef);
        assertEquals("Wrong OID in valueEnumerationRef in preferredLanguage definition",
                SystemObjectsType.LOOKUP_LANGUAGES.value(), preferredLanguageValueEnumerationRef.getOid());
        assertEquals("Wrong type in valueEnumerationRef in preferredLanguage definition",
                LookupTableType.COMPLEX_TYPE, preferredLanguageValueEnumerationRef.getTargetType());

        diagrams = userDefinition.getDiagrams();
        assertNotNull("No diagrams in user definition", diagrams);
        assertEquals("Unexpected number of diagrams in user definition", 3, diagrams.size());
        assertEquals("Unexpected name of diagram in user definition", "user-shadow-resource", diagrams.get(0).getName());
        assertEquals("Unexpected form of diagram in user definition", DiagramElementFormType.COLLAPSED, diagrams.get(0).getForm());
        assertEquals("Unexpected form of diagram in user definition", DiagramElementInclusionType.INCLUDE, diagrams.get(0).getInclusion());
        assertEquals("Unexpected form of diagram in user definition", DiagramElementInclusionType.AUTO, diagrams.get(0).getSubitemInclusion());

        // Just make sure this does not end with NPE or stack overflow
        userDefinition.clone();
        userDefinition.deepClone(false, null);
        userDefinition.deepClone(true, null);
    }

    @Test
    public void testRoleType() throws Exception {
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
        roleDefinition.clone();
        roleDefinition.deepClone(false, null);
        roleDefinition.deepClone(true, null);
    }

    @Test
    public void testAbstractRoleType() throws Exception {
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

        assertNull("Unexpected schema migrations in abstract role definition (OTD)", abstractRoleDefinition.getSchemaMigrations());
        List<SchemaMigration> schemaMigrations = abstractRoleDefinition.getComplexTypeDefinition().getSchemaMigrations();
        assertEquals("Wrong number of schema migrations in AbstractRoleType definition (CTD)", 8, schemaMigrations.size());

        // Just make sure this does not end with NPE or stack overflow
        abstractRoleDefinition.clone();
        abstractRoleDefinition.deepClone(false, null);
        abstractRoleDefinition.deepClone(true, null);
    }

    @Test
    public void testCommonSchemaShadowType() throws SchemaException, SAXException, IOException {
        MidPointPrismContextFactory factory = getContextFactory();
        PrismContext context = factory.createInitializedPrismContext();
        SchemaRegistry schemaRegistry = context.getSchemaRegistry();

        PrismObjectDefinition<ShadowType> shadowDef = schemaRegistry.findObjectDefinitionByCompileTimeClass(ShadowType.class);
        assertNotNull("No shadow definition", shadowDef);

        System.out.println("\nshadow definition:");
        System.out.println(shadowDef.debugDump(1));

        PrismPropertyDefinition nameDef = shadowDef.findPropertyDefinition(ShadowType.F_NAME);
        assertNotNull("No name definition", nameDef);

        PrismContainerDefinition extensionDef = shadowDef.findContainerDefinition(ShadowType.F_EXTENSION);
        assertNotNull("No 'extension' definition", extensionDef);
        assertTrue("'extension' definition is not marked as runtime", extensionDef.isRuntimeSchema());

        PrismContainerDefinition attributesDef = shadowDef.findContainerDefinition(ShadowType.F_ATTRIBUTES);
        assertNotNull("No 'attributes' definition", attributesDef);
        assertTrue("'attributes' definition is not marked as runtime", attributesDef.isRuntimeSchema());

        List<SchemaMigration> schemaMigrations = shadowDef.getSchemaMigrations();
        System.out.println("\nshadow schema migrations (" + schemaMigrations.size() + "):");
        System.out.println(DebugUtil.debugDump(schemaMigrations));
        assertEquals("Wrong number of schema migrations in shadow definition", 1, schemaMigrations.size());

        ComplexTypeDefinition shadowTypeDef = shadowDef.getComplexTypeDefinition();
        List<SchemaMigration> shadowTypeSchemaMigrations = shadowTypeDef.getSchemaMigrations();
        System.out.println("\nShadowType schema migrations (" + shadowTypeSchemaMigrations.size() + "):");
        System.out.println(DebugUtil.debugDump(shadowTypeSchemaMigrations));
        assertEquals("Wrong number of schema migrations in ShadowType definition", 4, shadowTypeSchemaMigrations.size());
    }

    @Test
    public void testCommonSchemaLegacyAccount() throws SchemaException, SAXException, IOException {
        MidPointPrismContextFactory factory = getContextFactory();
        PrismContext context = factory.createInitializedPrismContext();
        SchemaRegistry schemaRegistry = context.getSchemaRegistry();

        ItemDefinition accountDef = schemaRegistry.findItemDefinitionByElementName(SchemaConstants.C_ACCOUNT);
        assertNotNull("No definition for legacy account", accountDef);
        System.out.println("\naccount definition:");
        System.out.println(accountDef.debugDump(1));
        assertTrue("Expected object definition for legacy account, but got " + accountDef, accountDef instanceof PrismObjectDefinition);

        assertEquals("Unexpected element name in legacy account definition", SchemaConstants.C_SHADOW, accountDef.getItemName());
    }

    @Test
    public void testAuditTimestampDocumentation() throws SchemaException, SAXException, IOException {
        MidPointPrismContextFactory factory = getContextFactory();
        PrismContext context = factory.createInitializedPrismContext();
        SchemaRegistry schemaRegistry = context.getSchemaRegistry();

        PrismSchema auditSchema = schemaRegistry.findSchemaByCompileTimeClass(AuditEventRecordType.class);
        assertNotNull("No audit schema", auditSchema);
        ItemDefinition<?> timestampDef = auditSchema.findComplexTypeDefinitionByType(AuditEventRecordType.COMPLEX_TYPE)
                .findItemDefinition(AuditEventRecordType.F_TIMESTAMP);
        assertNotNull("No timestamp definition", timestampDef);

        System.out.println("timestamp: " + timestampDef.debugDump());
        assertNotNull("No timestamp documentation", timestampDef.getDocumentation());
    }

    private MidPointPrismContextFactory getContextFactory() {
        return new MidPointPrismContextFactory();
    }

}
