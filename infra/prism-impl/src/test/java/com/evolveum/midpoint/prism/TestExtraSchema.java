/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

public class TestExtraSchema extends AbstractPrismTest {

    public static final String NS_USER_2_EXT = "http://example.com/xml/ns/user-2-extension";

    private static final ItemName USER_EXTENSION_TYPE_QNAME = new ItemName(NS_USER_EXT, "UserExtensionType");
    private static final ItemName USER_2_EXTENSION_TYPE_QNAME = new ItemName(NS_USER_2_EXT, "User2ExtensionType");

    private static final ItemName USER_EXT_2_ELEMENT = new ItemName(NS_USER_2_EXT, "ext2");

    /**
     * Test if extra schema can be loaded to the schema registry and whether the file compliant to that
     * schema can be validated.
     */
    @Test
    public void testExtraSchema() throws SAXException, IOException, SchemaException {
        Document dataDoc = DOMUtil.parseFile(new File(COMMON_DIR_PATH, "root-foo.xml"));

        PrismContext context = constructPrismContext();
        SchemaRegistryImpl reg = (SchemaRegistryImpl) context.getSchemaRegistry();
        Document extraSchemaDoc = DOMUtil.parseFile(new File(EXTRA_SCHEMA_DIR, "root.xsd"));
        reg.registerSchema(extraSchemaDoc, "file root.xsd");
        reg.initialize();
        Schema javaxSchema = reg.getJavaxSchema();
        assertNotNull(javaxSchema);

        Validator validator = javaxSchema.newValidator();
        DOMResult validationResult = new DOMResult();
        validator.validate(new DOMSource(dataDoc), validationResult);
    }

    /**
     * Test if a schema directory can be loaded to the schema registry. This contains definition of
     * user extension, therefore check if it is applied to the user definition.
     */
    @Test
    public void testUserExtensionSchemaLoad() throws SAXException, IOException, SchemaException {
        PrismContext context = constructPrismContext();
        SchemaRegistryImpl reg = (SchemaRegistryImpl) context.getSchemaRegistry();
        reg.registerPrismSchemasFromDirectory(EXTRA_SCHEMA_DIR);
        context.initialize();
        System.out.println("Initialized registry");
        System.out.println(reg.debugDump());

        // Try midpoint schemas by parsing a XML file
        PrismSchema schema = reg.getPrismSchema(NS_FOO);
        System.out.println("Parsed foo schema:");
        System.out.println(schema.debugDump());

        // TODO: assert user

        schema = reg.getPrismSchema(NS_USER_EXT);
        System.out.println("Parsed user ext schema:");
        System.out.println(schema.debugDump());

        ComplexTypeDefinition userExtComplexType = schema.findComplexTypeDefinitionByType(USER_EXTENSION_TYPE_QNAME);
        assertEquals("Extension type ref does not match", USER_TYPE_QNAME, userExtComplexType.getExtensionForType());

    }

    @Test
    public void testUserExtensionSchemaParseUser() throws SAXException, IOException, SchemaException {
        Document dataDoc = DOMUtil.parseFile(USER_JACK_FILE_XML);

        PrismContext context = constructPrismContext();
        SchemaRegistryImpl reg = (SchemaRegistryImpl) context.getSchemaRegistry();
        reg.registerPrismSchemasFromDirectory(EXTRA_SCHEMA_DIR);
        context.initialize();

        // Parsing user
        PrismObject<UserType> user = context.parserFor(DOMUtil.getFirstChildElement(dataDoc)).compat().parse();        // items from user extension are not correctly defined in the schema
        assertNotNull("No definition for user", user.getDefinition());

        System.out.println("Parsed root object:");
        System.out.println(user.debugDump());

        // TODO: assert user

        // Try javax schemas by validating a XML file
        Schema javaxSchema = reg.getJavaxSchema();
        assertNotNull(javaxSchema);
        Validator validator = javaxSchema.newValidator();
        DOMResult validationResult = new DOMResult();
        validator.validate(new DOMSource(dataDoc), validationResult);
    }

    @Test
    public void testUserExtensionSchemaSchemaRegistry() throws SAXException, IOException, SchemaException {
        PrismContext context = constructPrismContext();
        SchemaRegistryImpl reg = (SchemaRegistryImpl) context.getSchemaRegistry();
        reg.registerPrismSchemasFromDirectory(EXTRA_SCHEMA_DIR);
        context.initialize();

        PrismObjectDefinition<UserType> userDef = reg.findObjectDefinitionByType(USER_TYPE_QNAME);

        System.out.println("User definition:");
        System.out.println(userDef.debugDump());

        assertUserDefinition(userDef);

        PrismObjectDefinition<UserType> usedDefByClass = reg.findObjectDefinitionByCompileTimeClass(UserType.class);
        assertUserDefinition(usedDefByClass);

        PrismObjectDefinition<UserType> userDefByElement = reg.findObjectDefinitionByElementName(USER_QNAME);
        assertUserDefinition(userDefByElement);
    }

    private void assertUserDefinition(PrismObjectDefinition<UserType> userDef) {
        PrismContainerDefinition extDef = userDef.findContainerDefinition(USER_EXTENSION_QNAME);

        System.out.println("User extension");
        System.out.println(extDef.debugDump());

        assertTrue("Extension is not dynamic", extDef.isRuntimeSchema());
        assertTrue("Wrong extension type " + extDef.getTypeName(),
                USER_EXTENSION_TYPE_QNAME.equals(extDef.getTypeName()) || USER_2_EXTENSION_TYPE_QNAME.equals(extDef.getTypeName()));
        assertEquals("Wrong extension displayOrder", (Integer) 1000, extDef.getDisplayOrder());

        PrismPropertyDefinition barPropDef = extDef.findPropertyDefinition(USER_EXT_BAR_ELEMENT);
        assertNotNull("No 'bar' definition in user extension", barPropDef);
        PrismAsserts.assertDefinition(barPropDef, USER_EXT_BAR_ELEMENT, DOMUtil.XSD_STRING, 1, 1);
        assertTrue("'bar' not indexed", barPropDef.isIndexed());

        PrismPropertyDefinition foobarPropDef = extDef.findPropertyDefinition(USER_EXT_FOOBAR_ELEMENT);
        assertNotNull("No 'foobar' definition in user extension", foobarPropDef);
        PrismAsserts.assertDefinition(foobarPropDef, USER_EXT_FOOBAR_ELEMENT, DOMUtil.XSD_STRING, 0, 1);
        assertNull("'foobar' has non-null indexed flag", foobarPropDef.isIndexed());

        PrismPropertyDefinition multiPropDef = extDef.findPropertyDefinition(USER_EXT_MULTI_ELEMENT);
        assertNotNull("No 'multi' definition in user extension", multiPropDef);
        PrismAsserts.assertDefinition(multiPropDef, USER_EXT_MULTI_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
        assertFalse("'multi' not indexed", multiPropDef.isIndexed());

        PrismPropertyDefinition ext2Def = extDef.findPropertyDefinition(USER_EXT_2_ELEMENT);
        assertNotNull("No 'ext2' definition in user extension", ext2Def);
        PrismAsserts.assertDefinition(ext2Def, USER_EXT_2_ELEMENT, DOMUtil.XSD_STRING, 1, 1);

        // Make sure that the ordering is OK. If it is not then a serialization will produce XML that
        // does not comply to schema
        List<? extends ItemDefinition> definitions = userDef.getDefinitions();
        assertDefinitionOrder(definitions, USER_NAME_QNAME, 0);
        assertDefinitionOrder(definitions, USER_DESCRIPTION_QNAME, 1);
        assertDefinitionOrder(definitions, USER_EXTENSION_QNAME, 2);
        assertDefinitionOrder(definitions, USER_PARENT_ORG_REF_QNAME, 3);
        assertDefinitionOrder(definitions, USER_FULLNAME_QNAME, 4);
    }

    private void assertDefinitionOrder(List<? extends ItemDefinition> definitions, QName elementName, int i) {
        assertEquals("Wrong definition, expected that " + PrettyPrinter.prettyPrint(elementName) + " definition will be at index " +
                i + " but there was a " + definitions.get(i).getItemName() + " instead", elementName, definitions.get(i).getItemName());
    }

    /**
     * Test if a schema directory can be loaded to the schema registry. This contains definition of
     * user extension, therefore check if it is applied to the user definition.
     */
    @Test
    public void testTypeOverride() throws SAXException, IOException, SchemaException {
        PrismContext context = constructPrismContext();
        SchemaRegistryImpl reg = (SchemaRegistryImpl) context.getSchemaRegistry();
        reg.registerPrismSchemasFromDirectory(EXTRA_SCHEMA_DIR);
        context.initialize();

        PrismSchema schema = reg.getPrismSchema(NS_ROOT);
        System.out.println("Parsed root schema:");
        System.out.println(schema.debugDump());

        PrismContainerDefinition rootContDef = schema.findContainerDefinitionByElementName(new QName(NS_ROOT, "root"));
        assertNotNull("Not <root> definition", rootContDef);
        PrismContainerDefinition extensionContDef = rootContDef.findContainerDefinition(new ItemName(NS_FOO, "extension"));
        assertNotNull("Not <extension> definition", extensionContDef);
        assertEquals("Wrong <extension> type", new QName(NS_ROOT, "MyExtensionType"), extensionContDef.getTypeName());

    }

}
