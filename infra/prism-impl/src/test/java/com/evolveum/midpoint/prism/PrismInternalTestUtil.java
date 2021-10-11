/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.crypto.KeyStoreBasedProtectorBuilder;
import com.evolveum.midpoint.prism.foo.*;

import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.impl.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationArgumentType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 *
 */
public class PrismInternalTestUtil implements PrismContextFactory {

    // Files
    public static final String TEST_CATALOG_RESOURCE_NAME = "META-INF/catalog-test.xml";

    public static final String COMMON_DIR_PATH = "src/test/resources/common";
    public static final File COMMON_DIR = new File(COMMON_DIR_PATH);
    public static final File SCHEMA_DIR = new File("src/test/resources/schema");
    public static final File EXTRA_SCHEMA_DIR = new File("src/test/resources/schema-extra");

    public static final File COMMON_DIR_XML = new File(COMMON_DIR, "xml");

    // User: jack
    public static final String USER_JACK_FILE_BASENAME = "user-jack";
    public static final File USER_JACK_FILE_XML = new File(COMMON_DIR_XML, USER_JACK_FILE_BASENAME+".xml");

    public static final String USER_JACK_NO_NS_BASENAME = "user-jack-no-ns";

    public static final String USER_JACK_OBJECT_BASENAME = "user-jack-object";
    public static final File USER_JACK_OBJECT_FILE = new File(COMMON_DIR_XML, "user-jack-object.xml");

    public static final String USER_JACK_MODIFIED_FILE_BASENAME = "user-jack-modified";
    public static final File USER_JACK_MODIFIED_FILE = new File(COMMON_DIR_PATH, "user-jack-modified.xml");

    public static final String USER_JACK_ADHOC_BASENAME = "user-jack-adhoc";
    public static final File USER_JACK_ADHOC_FILE = new File(COMMON_DIR_XML, "user-jack-adhoc.xml");

    public static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
    public static final XMLGregorianCalendar USER_JACK_VALID_FROM = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0);
    public static final String USER_JACK_DESCRIPTION = "This must be the best pirate the world has ever seen";
    public static final String USER_JACK_POLYNAME_ORIG = "Džek Sperou";
    public static final String USER_JACK_POLYNAME_NORM = "dzek sperou";

    // User: barbossa (very simple user)
    public static final String USER_BARBOSSA_FILE_BASENAME = "user-barbossa";
    public static final File USER_BARBOSSA_FILE = new File(COMMON_DIR_XML, "user-barbossa.xml");

    // User: will (has all the extensions)
    public static final String USER_WILL_FILE_BASENAME = "user-will";
    public static final File USER_WILL_FILE = new File(COMMON_DIR_XML, "user-will.xml");

    public static final String ACCOUNT_BARBOSSA_FILE_BASENAME = "account-barbossa";

    public static final String USER_ELISABETH_FILE_BASENAME = "user-elisabeth";

    public static final String RESOURCE_RUM_FILE_BASENAME = "resource-rum";
    public static final String RESOURCE_RUM_OID = "c0c010c0-d34d-b33f-f00d-222222220001";

    public static final String REF_WITH_FILTER_BASENAME = "ref-with-filter";
    public static final String REF_WITH_FILTER_DIFFERENT_PATH_BASENAME = "ref-with-filter-different-path";
    public static final String REF_WITH_FILTER_NO_OID_BASENAME = "ref-with-filter-no-oid";
    public static final String REF_WITHOUT_FILTER_BASENAME = "ref-without-filter";

    // Namespaces
    public static final String DEFAULT_NAMESPACE_PREFIX = "http://midpoint.evolveum.com/xml/ns";
    public static final String NS_FOO = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd";
    public static final String NS_FOO_TYPES = "http://midpoint.evolveum.com/xml/ns/test/foo-types-1";
    public static final String NS_USER_EXT = "http://example.com/xml/ns/user-extension";
    public static final String NS_ROOT = "http://example.com/xml/ns/test/root.xsd";
    public static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
    public static final String NS_EXTENSION_SECONDARY = "http://midpoint.evolveum.com/xml/ns/test/extension/secondary";
    public static final String NS_ADHOC = "http://midpoint.evolveum.com/xml/ns/test/adhoc-1.xsd";
    public static final String NS_WEAPONS = "http://midpoint.evolveum.com/xml/ns/test/weapons";
    public static final String NS_WEAPONS_PREFIX = "w";

    // FOO schema
    public static final ItemName USER_QNAME = new ItemName(NS_FOO,"user");
    public static final ItemName USER_TYPE_QNAME = new ItemName(NS_FOO,"UserType");

    public static final ItemName OBJECT_REFERENCE_TYPE_QNAME = new ItemName(NS_FOO, "ObjectReferenceType");

    public static final ItemName USER_EXTENSION_QNAME = new ItemName(NS_FOO,"extension");
    public static final ItemName USER_PARENT_ORG_QNAME = new ItemName(NS_FOO,"parentOrg");
    public static final ItemName USER_PARENT_ORG_REF_QNAME = new ItemName(NS_FOO,"parentOrgRef");

    public static final ItemName USER_NAME_QNAME = new ItemName(NS_FOO,"name");
    public static final ItemName USER_FULLNAME_QNAME = new ItemName(NS_FOO,"fullName");
    public static final ItemName USER_GIVENNAME_QNAME = new ItemName(NS_FOO,"givenName");
    public static final ItemName USER_FAMILYNAME_QNAME = new ItemName(NS_FOO,"familyName");
    public static final ItemName USER_ADDITIONALNAMES_QNAME = new ItemName(NS_FOO,"additionalNames");
    public static final ItemName USER_POLYNAME_QNAME = new ItemName(NS_FOO,"polyName");
    public static final ItemName USER_LOCALITY_QNAME = new ItemName(NS_FOO,"locality");

    public static final ItemName USER_ACTIVATION_QNAME = new ItemName(NS_FOO,"activation");
    public static final ItemName USER_ENABLED_QNAME = new ItemName(NS_FOO,"enabled");
    public static final ItemPath USER_ENABLED_PATH = ItemPath.create(USER_ACTIVATION_QNAME, USER_ENABLED_QNAME);
    public static final ItemName USER_VALID_FROM_QNAME = new ItemName(NS_FOO,"validFrom");
    public static final ItemPath USER_VALID_FROM_PATH = ItemPath.create(USER_ACTIVATION_QNAME, USER_VALID_FROM_QNAME);
    public static final ItemName USER_VALID_TO_QNAME = new ItemName(NS_FOO,"validTo");
    public static final ItemName ACTIVATION_TYPE_QNAME = new ItemName(NS_FOO,"ActivationType");

    public static final ItemName USER_ASSIGNMENT_QNAME = new ItemName(NS_FOO,"assignment");
    public static final ItemName USER_DESCRIPTION_QNAME = new ItemName(NS_FOO,"description");
    public static final ItemPath USER_ASSIGNMENT_DESCRIPTION_PATH = ItemPath.create(USER_ASSIGNMENT_QNAME, USER_DESCRIPTION_QNAME);
    public static final ItemName ASSIGNMENT_TYPE_QNAME = new ItemName(NS_FOO,"AssignmentType");
    public static final ItemName USER_ACCOUNT_CONSTRUCTION_QNAME = new ItemName(NS_FOO,"accountConstruction");
    public static final Long USER_ASSIGNMENT_1_ID = 1111L;
    public static final Long USER_ASSIGNMENT_2_ID = 1112L;
    public static final Long USER_ASSIGNMENT_3_ID = 1113L;

    public static final ItemName ACCOUNT_CONSTRUCTION_TYPE_QNAME = new ItemName(NS_FOO,"AccountConstructionType");

    public static final ItemName USER_ACCOUNTREF_QNAME = new ItemName(NS_FOO,"accountRef");
    public static final ItemName USER_ACCOUNT_QNAME = new ItemName(NS_FOO,"account");

    public static final ItemName ACCOUNT_TYPE_QNAME = new ItemName(NS_FOO,"AccountType");

    public static final ItemName ACCOUNT_NAME_QNAME = new ItemName(NS_FOO,"name");
    public static final ItemName ACCOUNT_DESCRIPTION_QNAME = new ItemName(NS_FOO,"description");
    public static final ItemName ACCOUNT_ATTRIBUTES_QNAME = new ItemName(NS_FOO,"attributes");

    public static final ItemName ATTRIBUTES_TYPE_QNAME = new ItemName(NS_FOO,"AttributesType");

    public static final ItemName DUMMY_PROTECTED_STRING_TYPE = new ItemName(NS_FOO, "DummyProtectedStringType");

    public static final ItemName RESOURCE_QNAME = new ItemName(NS_FOO,"resource");
    public static final ItemName RESOURCE_TYPE_QNAME = new ItemName(NS_FOO,"ResourceType");

    // extension.xsd
    public static final ItemName EXTENSION_STRING_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "stringType");
    public static final ItemName EXTENSION_SINGLE_STRING_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "singleStringType");
    public static final ItemName EXTENSION_DOUBLE_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "doubleType");
    public static final ItemName EXTENSION_INT_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "intType");
    public static final ItemName EXTENSION_INTEGER_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "integerType");
    public static final ItemName EXTENSION_LONG_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "longType");
    public static final ItemName EXTENSION_DATE_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "dateType");
    public static final ItemName EXTENSION_DURATION_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "durationType");
    public static final ItemName EXTENSION_LOCATIONS_ELEMENT = new ItemName(NS_EXTENSION, "locations");
    public static final ItemName EXTENSION_LOCATIONS_TYPE_QNAME = new ItemName(NS_EXTENSION, "LocationsType");
    public static final ItemName EXTENSION_IGNORED_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "ignoredType");
    public static final ItemName EXTENSION_INDEXED_STRING_TYPE_ELEMENT = new ItemName(NS_EXTENSION, "indexedString");
    public static final ItemName EXTENSION_BLADE_TYPE_QNAME = new ItemName(NS_EXTENSION, "BladeType");
    public static final ItemName EXTENSION_MELEE_CONTEXT_ELEMENT = new ItemName(NS_EXTENSION, "meleeContext");
    public static final ItemName EXTENSION_MELEE_CONTEXT_TYPE_QNAME = new ItemName(NS_EXTENSION, "MeleeContextType");
    public static final ItemName EXTENSION_MELEE_CONTEXT_OPPONENT_REF_ELEMENT = new ItemName(NS_EXTENSION, "opponentRef");
    public static final ItemName EXTENSION_MELEE_CONTEXT_OPPONENT_ELEMENT = new ItemName(NS_EXTENSION, "opponent");

    // extension-secondary.xsd
    public static final ItemName EXTENSION_SECONDARY_STRING_TYPE_ELEMENT = new ItemName(NS_EXTENSION_SECONDARY, "stringType");
    public static final ItemName EXTENSION_SECONDARY_SECONDARY_STRING_TYPE_ELEMENT = new ItemName(NS_EXTENSION_SECONDARY, "secondaryStringType");

    // These are NOT in the extension.xsd but are used as dynamic elements
    public static final ItemName EXTENSION_BAR_ELEMENT = new ItemName(NS_EXTENSION, "bar");
    public static final ItemName EXTENSION_FOOBAR_ELEMENT = new ItemName(NS_EXTENSION, "foobar");
    public static final ItemName EXTENSION_NUM_ELEMENT = new ItemName(NS_EXTENSION, "num");
    public static final ItemName EXTENSION_MULTI_ELEMENT = new ItemName(NS_EXTENSION, "multi");

    // Definitions used in schema-extra/extension/user.xsd
    // this is loaded as an extra schema, it is not part of usual tests. Mind the namespace that is
    // different from the previous elements
    public static final ItemName USER_EXT_BAR_ELEMENT = new ItemName(NS_USER_EXT, "bar");
    public static final ItemName USER_EXT_FOOBAR_ELEMENT = new ItemName(NS_USER_EXT, "foobar");
    public static final ItemName USER_EXT_NUM_ELEMENT = new ItemName(NS_USER_EXT, "num");
    public static final ItemName USER_EXT_MULTI_ELEMENT = new ItemName(NS_USER_EXT, "multi");

    public static final ItemName USER_ADHOC_BOTTLES_ELEMENT = new ItemName(NS_ADHOC, "bottles");

    public static final ItemName WEAPONS_WEAPON_BRAND_TYPE_QNAME = new ItemName(NS_WEAPONS, "WeaponBrandType");

    public static final String KEYSTORE_PATH = "src/test/resources/keystore.jceks";
    public static final String KEYSTORE_PASSWORD = "changeit";

    public static PrismContextImpl constructInitializedPrismContext() throws SchemaException, SAXException, IOException {
        PrismContextImpl context = constructPrismContext();
        context.initialize();
        return context;
    }

    public static PrismContext constructInitializedPrismContext(File extraSchema) throws SchemaException, SAXException, IOException {
        PrismContext context = constructPrismContext(extraSchema);
        context.initialize();
        return context;
    }

    public static PrismContextImpl constructPrismContext() throws SchemaException, FileNotFoundException {
        return constructPrismContext(null);
    }

    public static PrismContextImpl constructPrismContext(File extraSchema) throws SchemaException, FileNotFoundException {
        SchemaRegistryImpl schemaRegistry = new SchemaRegistryImpl();
        schemaRegistry.setCatalogResourceName(TEST_CATALOG_RESOURCE_NAME);
        DynamicNamespacePrefixMapper prefixMapper = new GlobalDynamicNamespacePrefixMapper();
        // Set default namespace?
        schemaRegistry.setNamespacePrefixMapper(prefixMapper);
        schemaRegistry.registerPrismDefaultSchemaResource("xml/ns/test/foo-1.xsd", "foo", ObjectFactory.class.getPackage());
        schemaRegistry.registerPrismSchemaResource("xml/ns/test/foo-types-1.xsd", "foot", null);
        schemaRegistry.registerPrismSchemaResource("xml/ns/public/types-3.xsd", "t", com.evolveum.prism.xml.ns._public.types_3.ObjectFactory.class.getPackage());
        schemaRegistry.registerPrismSchemaResource("xml/ns/public/query-3.xsd", "q", com.evolveum.prism.xml.ns._public.query_3.ObjectFactory.class.getPackage());
        schemaRegistry.registerPrismSchemasFromDirectory(SCHEMA_DIR);
        if (extraSchema != null){
            schemaRegistry.registerPrismSchemaFile(extraSchema);
        }
        prefixMapper.registerPrefix(XMLConstants.W3C_XML_SCHEMA_NS_URI, DOMUtil.NS_W3C_XML_SCHEMA_PREFIX, false);
        prefixMapper.registerPrefix(PrismConstants.NS_ANNOTATION, PrismConstants.PREFIX_NS_ANNOTATION, false);
        prefixMapper.registerPrefix(PrismInternalTestUtil.NS_WEAPONS, PrismInternalTestUtil.NS_WEAPONS_PREFIX, false);
        PrismContextImpl prismContext = PrismContextImpl.create(schemaRegistry);
        prismContext.setObjectsElementName(new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "objects"));
        return prismContext;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.prism.PrismContextFactory#createPrismContext()
     */
    @Override
    public PrismContext createPrismContext() throws SchemaException, FileNotFoundException {
        return constructPrismContext();
    }

    public static PrismObjectDefinition<UserType> getUserTypeDefinition() {
        return PrismTestUtil.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
    }

    public static void assertVisitor(Visitable visitable, int expectedVisits) {
        final List<Visitable> visits = new ArrayList<>();
        Visitor visitor = new Visitor() {
            @Override
            public void visit(Visitable visitable) {
                visits.add(visitable);
                System.out.println("#" + visits.size() + ": Visiting: "+visitable);
            }
        };
        visitable.accept(visitor);
        assertEquals("Wrong number of visits", expectedVisits, visits.size());
    }

    public static void assertPathVisitor(PathVisitable visitable, final ItemPath path, final boolean recursive, int expectedVisits) {
        final List<Visitable> visits = new ArrayList<>();
        Visitor visitor = visitable1 -> {
            visits.add(visitable1);
            System.out.println("#" + visits.size() + ": Visiting(path="+path+",recursive="+recursive+"): "+ visitable1);
        };
        visitable.accept(visitor, path, recursive);
        assertEquals("Wrong number of visits for path "+path, expectedVisits, visits.size());
    }

    public static void assertUserJack(PrismObject<UserType> user, boolean expectRawInConstructions, boolean withIncomplete) throws SchemaException {
        assertUserJack(user, expectRawInConstructions, true, withIncomplete);
    }

    public static void assertUserJack(PrismObject<UserType> user, boolean expectRawInConstructions, boolean expectFullPolyName, boolean withIncomplete) throws SchemaException {
        user.checkConsistence();
        user.assertDefinitions("test");
        assertUserJackContent(user, expectRawInConstructions, expectFullPolyName, withIncomplete);
        assertUserJackExtension(user, withIncomplete);
        assertVisitor(user, 71);

        assertPathVisitor(user, UserType.F_ASSIGNMENT, true, 9);
        assertPathVisitor(user, ItemPath.create(UserType.F_ASSIGNMENT, USER_ASSIGNMENT_1_ID), true, 3);
        assertPathVisitor(user, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ENABLED), true, 2);
        assertPathVisitor(user, UserType.F_EXTENSION, true, 15);

        assertPathVisitor(user, UserType.F_ASSIGNMENT, false, 1);
        assertPathVisitor(user, ItemPath.create(UserType.F_ASSIGNMENT, USER_ASSIGNMENT_1_ID), false, 1);
        assertPathVisitor(user, ItemPath.create(UserType.F_ASSIGNMENT, null), false, 2);
        assertPathVisitor(user, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ENABLED), false, 1);
        assertPathVisitor(user, UserType.F_EXTENSION, false, 1);
    }

    public static void assertUserJackContent(PrismObject<UserType> user, boolean expectRawInConstructions,
            boolean expectFullPolyName, boolean withIncomplete) throws SchemaException {
        PrismContext prismContext = user.getPrismContext();
        assertEquals("Wrong oid", USER_JACK_OID, user.getOid());
        assertEquals("Wrong version", "42", user.getVersion());
        PrismAsserts.assertObjectDefinition(user.getDefinition(), USER_QNAME, USER_TYPE_QNAME, UserType.class);
        PrismAsserts.assertParentConsistency(user);

        assertPropertyValue(user, "fullName", "cpt. Jack Sparrow");
        assertPropertyDefinition(user, "fullName", DOMUtil.XSD_STRING, 1, 1);
        assertPropertyValue(user, "givenName", "Jack");
        assertPropertyDefinition(user, "givenName", DOMUtil.XSD_STRING, 0, 1);
        assertPropertyValue(user, "familyName", "Sparrow");
        assertPropertyDefinition(user, "familyName", DOMUtil.XSD_STRING, 0, 1);
        assertPropertyValue(user, "name", new PolyString("jack", "jack"));
        assertPropertyDefinition(user, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

        assertPropertyValue(user, "special", "got it!");
        assertPropertyDefinition(user, "special", DOMUtil.XSD_STRING, 0, 1);

        asssertJackPolyName(user, expectFullPolyName);
        assertPropertyDefinition(user, "polyName", PolyStringType.COMPLEX_TYPE, 0, 1);

        ItemPath enabledPath = USER_ENABLED_PATH;
        PrismProperty<Boolean> enabledProperty1 = user.findProperty(enabledPath);
        assertNotNull("No enabled property", enabledProperty1);
        PrismAsserts.assertDefinition(enabledProperty1.getDefinition(), USER_ENABLED_QNAME, DOMUtil.XSD_BOOLEAN, 0, 1);
        assertNotNull("Property "+enabledPath+" not found", enabledProperty1);
        PrismAsserts.assertPropertyValue(enabledProperty1, true);

        PrismProperty<XMLGregorianCalendar> validFromProperty = user.findProperty(USER_VALID_FROM_PATH);
        assertNotNull("Property "+USER_VALID_FROM_PATH+" not found", validFromProperty);
        PrismAsserts.assertPropertyValue(validFromProperty, USER_JACK_VALID_FROM);

        ItemName actName = new ItemName(NS_FOO,"activation");
        // Use path
        PrismContainer<ActivationType> actContainer1 = user.findContainer(actName);
        assertContainerDefinition(actContainer1, "activation", ACTIVATION_TYPE_QNAME, 0, 1);
        assertNotNull("Property "+ actName +" not found", actContainer1);
        assertEquals("Wrong activation name",actName,actContainer1.getElementName());
        // Use name
        PrismContainer<ActivationType> actContainer2 = user.findContainer(actName);
        assertNotNull("Property "+actName+" not found", actContainer2);
        assertEquals("Wrong activation name",actName,actContainer2.getElementName());
        // Compare
        assertEquals("Eh?",actContainer1,actContainer2);

        PrismProperty<Boolean> enabledProperty2 = actContainer1.findProperty(new ItemName(NS_FOO,"enabled"));
        assertNotNull("Property enabled not found", enabledProperty2);
        PrismAsserts.assertPropertyValue(enabledProperty2, true);
        assertEquals("Eh?",enabledProperty1,enabledProperty2);

        ItemName assName = new ItemName(NS_FOO,"assignment");
        ItemName descriptionName = new ItemName(NS_FOO,"description");
        ItemName accountConstructionName = new ItemName(NS_FOO,"accountConstruction");
        PrismContainer<AssignmentType> assContainer = user.findContainer(assName);
        if (withIncomplete) {
            assertTrue("Assignment is not incomplete", assContainer.isIncomplete());
        }
        assertEquals("Wrong assignment values", 2, assContainer.getValues().size());
        PrismProperty<String> a2DescProperty = assContainer.getValue(USER_ASSIGNMENT_2_ID).findProperty(descriptionName);
        assertEquals("Wrong assigment 2 description", "Assignment 2", a2DescProperty.getValue().getValue());

        ItemPath a1Path = ItemPath.create(assName, USER_ASSIGNMENT_1_ID, descriptionName);
        PrismProperty a1Property = user.findProperty(a1Path);
        assertNotNull("Property "+a1Path+" not found", a1Property);
        PrismAsserts.assertPropertyValue(a1Property, "Assignment 1");

        ItemPath a2Path = ItemPath.create(assName, USER_ASSIGNMENT_2_ID, accountConstructionName);
        PrismProperty a2Property = user.findProperty(a2Path);
        assertNotNull("Property "+a2Path+" not found", a2Property);
        AccountConstructionType accountConstructionType = (AccountConstructionType) a2Property.getRealValue();
        assertEquals("Wrong number of values in accountConstruction", 2, accountConstructionType.getValue().size());
        RawType value1 = accountConstructionType.getValue().get(0).clone();
        if (expectRawInConstructions) {
            assertNotNull("Value #1 has no XNode present", value1.getXnode());
            PrismPropertyDefinition value1def = user.getPrismContext().definitionFactory().createPropertyDefinition(
                    new QName(NS_FOO, "dummy"),           // element name
                    DOMUtil.XSD_STRING                 // type name
                    );
            PrismPropertyValue<String> prismValue1 = value1.getParsedValue(value1def, value1def.getItemName());
            assertEquals("Wrong value #1", "ABC", prismValue1.getValue());
        } else {
            assertNull("Value #1 has XNode present", value1.getXnode());
            assertEquals("Wrong value #1", "ABC", value1.getParsedRealValue(String.class));
        }
        RawType value2 = accountConstructionType.getValue().get(1).clone();
        assertNotNull("Value #2 has no XNode present", value2.getXnode());
        PrismValue prismValue2 = value2.getParsedValue(user.getDefinition(), user.getDefinition().getItemName());
        PrismContainerValue<UserType> prismUserValue2 = (PrismContainerValue<UserType>) prismValue2;
        assertEquals("Wrong value #2", "Nobody", prismUserValue2.findProperty(new ItemName(NS_FOO, "fullName")).getRealValue());

        PrismReference accountRef = user.findReference(USER_ACCOUNTREF_QNAME);
        if (withIncomplete) {
            assertTrue("accountRef is not incomplete", accountRef.isIncomplete());
        }
        assertNotNull("Reference "+USER_ACCOUNTREF_QNAME+" not found", accountRef);
        assertEquals("Wrong number of accountRef values", 3, accountRef.getValues().size());
        PrismAsserts.assertReferenceValue(accountRef, "c0c010c0-d34d-b33f-f00d-aaaaaaaa1111");
        PrismAsserts.assertReferenceValue(accountRef, "c0c010c0-d34d-b33f-f00d-aaaaaaaa1112");
        PrismAsserts.assertReferenceValue(accountRef, "c0c010c0-d34d-b33f-f00d-aaaaaaaa1113");
        PrismReferenceValue accountRefVal2 = accountRef.findValueByOid("c0c010c0-d34d-b33f-f00d-aaaaaaaa1112");
        assertEquals("Wrong oid for accountRef", "c0c010c0-d34d-b33f-f00d-aaaaaaaa1112", accountRefVal2.getOid());
        assertEquals("Wrong accountRef description", "This is a reference with a filter", accountRefVal2.getDescription());
        assertNotNull("No filter in accountRef", accountRefVal2.getFilter());

    }

    private static void asssertJackPolyName(PrismObject<UserType> user, boolean expectFullPolyName) {
        ItemName propQName = new ItemName(NS_FOO, "polyName");
        PrismProperty<PolyString> polyNameProp = user.findProperty(propQName);
        asssertJackPolyName(polyNameProp, user, expectFullPolyName);
    }

    public static void asssertJackPolyName(PrismProperty<PolyString> polyNameProp, PrismObject<UserType> user, boolean expectFullPolyName) {
        assertNotNull("No polyName in "+user, polyNameProp);
        PolyString polyName = polyNameProp.getAnyRealValue();
        assertEquals("Wrong polyName.orig in "+user, "Džek Sperou", polyName.getOrig());
        assertEquals("Wrong polyName.norm in "+user, "dzek sperou", polyName.getNorm());

        if (expectFullPolyName) {

            PolyStringTranslationType translation = polyName.getTranslation();
            assertNotNull("No polyName.translation in "+user, translation);
            assertEquals("Wrong polyName.translation.key in "+user, "JACK", translation.getKey());
            assertEquals("Wrong polyName.translation.fallback in "+user, "Jack", translation.getFallback());
            List<PolyStringTranslationArgumentType> arguments = translation.getArgument();
            assertNotNull("No polyName.translation.argument list in "+user, arguments);
            assertEquals("Wrong number of polyName.translation.argument in "+user, 1, arguments.size());
            PolyStringTranslationArgumentType argument = arguments.get(0);
            assertNotNull("No polyName.translation.argument in "+user, argument);
            assertEquals("Wrong polyName.translation.argument.value in "+user, "Sparrow", argument.getValue());

            Map<String, String> lang = polyName.getLang();
            assertNotNull("No polyName.lang in "+user, lang);
            assertLang(lang, "en", "Jack Sparrow");
            assertLang(lang, "sk", "Džek Sperou");
            assertLang(lang, "hr", "Ðek Sperou");
        }
    }

    private static void assertLang(Map<String, String> lang, String key, String expectedValue) {
        assertEquals("Wrong lang in polystring for key "+key, expectedValue, lang.get(key));
    }

    private static void assertUserJackExtension(PrismObject<UserType> user, boolean withIncomplete) {
        PrismContext prismContext = user.getPrismContext();
        PrismContainer<?> extension = user.getExtension();
        assertContainerDefinition(extension, "extension", DOMUtil.XSD_ANY, 0, 1);
        PrismContainerValue<?> extensionValue = extension.getValue();
        assertTrue("Extension parent", extensionValue.getParent() == extension);
        assertNull("Extension ID", extensionValue.getId());
        PrismAsserts.assertPropertyValue(extension, EXTENSION_BAR_ELEMENT, "BAR");
        PrismAsserts.assertPropertyValue(extension, EXTENSION_NUM_ELEMENT, 42);
        Collection<PrismPropertyValue<Object>> multiPVals = extension.findProperty(EXTENSION_MULTI_ELEMENT).getValues();
        assertEquals("Multi",3,multiPVals.size());

        PrismProperty<?> singleStringType = extension.findProperty(EXTENSION_SINGLE_STRING_TYPE_ELEMENT);
        PrismPropertyDefinition singleStringTypePropertyDef = singleStringType.getDefinition();
        PrismAsserts.assertDefinition(singleStringTypePropertyDef, EXTENSION_SINGLE_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, 1);
        assertNull("'Indexed' attribute on 'singleStringType' property is not null", singleStringTypePropertyDef.isIndexed());

        PrismProperty<?> indexedString = extension.findProperty(EXTENSION_INDEXED_STRING_TYPE_ELEMENT);
        PrismPropertyDefinition indexedStringPropertyDef = indexedString.getDefinition();
        PrismAsserts.assertDefinition(indexedStringPropertyDef, EXTENSION_SINGLE_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
        assertEquals("'Indexed' attribute on 'singleStringType' property is wrong", Boolean.FALSE, indexedStringPropertyDef.isIndexed());

        ItemPath barPath = ItemPath.create(new QName(NS_FOO,"extension"), EXTENSION_BAR_ELEMENT);
        PrismProperty<String> barProperty = user.findProperty(barPath);
        assertNotNull("Property "+barPath+" not found", barProperty);
        PrismAsserts.assertPropertyValue(barProperty, "BAR");
        PrismPropertyDefinition barPropertyDef = barProperty.getDefinition();
        assertNotNull("No definition for bar", barPropertyDef);
        PrismAsserts.assertDefinitionTypeLoose(barPropertyDef, EXTENSION_BAR_ELEMENT, DOMUtil.XSD_STRING, 1, -1);
        assertNull("'Indexed' attribute on 'bar' property is not null", barPropertyDef.isIndexed());

        PrismProperty<?> multi = extension.findProperty(EXTENSION_MULTI_ELEMENT);
        PrismPropertyDefinition multiPropertyDef = multi.getDefinition();
        PrismAsserts.assertDefinitionTypeLoose(multiPropertyDef, EXTENSION_MULTI_ELEMENT, DOMUtil.XSD_STRING, 1, -1);
        assertNull("'Indexed' attribute on 'multi' property is not null", multiPropertyDef.isIndexed());
        if (withIncomplete) {
            assertTrue("Extension multi is not incomplete", multi.isIncomplete());
        }
    }

    public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
        ItemName propQName = new ItemName(NS_FOO, propName);
        PrismAsserts.assertPropertyValue(container, propQName, propValue);
    }

    public static void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
            int maxOccurs) {
        QName propQName = new QName(NS_FOO, propName);
        PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
    }

    public static void assertContainerDefinition(PrismContainer container, String contName, QName xsdType, int minOccurs,
            int maxOccurs) {
        QName qName = new QName(NS_FOO, contName);
        PrismAsserts.assertDefinition(container.getDefinition(), qName, xsdType, minOccurs, maxOccurs);
    }

    public static PrismSchema getFooSchema(PrismContext prismContext) {
        return prismContext.getSchemaRegistry().findSchemaByNamespace(NS_FOO);
    }

    public static Protector createProtector(String xmlCipher){
        return KeyStoreBasedProtectorBuilder.create(getPrismContext())
                .keyStorePassword(KEYSTORE_PASSWORD)
                .keyStorePath(KEYSTORE_PATH)
                .encryptionAlgorithm(xmlCipher)
                .initialize();
    }
}
