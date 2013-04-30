/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.NS_USER_EXT;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ObjectFactory;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class PrismInternalTestUtil implements PrismContextFactory {

	// Files
	public static final String COMMON_DIR_PATH = "src/test/resources/common";
	public static File SCHEMA_DIR = new File("src/test/resources/schema");
	public static File EXTRA_SCHEMA_DIR = new File("src/test/resources/schema-extra");
	
	// User: jack
	public static final File USER_JACK_FILE = new File(COMMON_DIR_PATH, "user-jack.xml");
	public static final File USER_JACK_OBJECT_FILE = new File(COMMON_DIR_PATH, "user-jack-object.xml");
	public static final File USER_JACK_MODIFIED_FILE = new File(COMMON_DIR_PATH, "user-jack-modified.xml");
	public static final File USER_JACK_ADHOC_FILE = new File(COMMON_DIR_PATH, "user-jack-adhoc.xml");
	public static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	public static final XMLGregorianCalendar USER_JACK_VALID_FROM = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0);
	public static final String USER_JACK_DESCRIPTION = "This must be the best pirate the world has ever seen";
	public static final String USER_JACK_POLYNAME_ORIG = "Džek Sperou";
	public static final String USER_JACK_POLYNAME_NORM = "dzek sperou";

	// Namespaces
	public static final String DEFAULT_NAMESPACE_PREFIX = "http://midpoint.evolveum.com/xml/ns";
	public static final String NS_FOO = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd";
	public static final String NS_FOO_TYPES = "http://midpoint.evolveum.com/xml/ns/test/foo-types-1";
	public static final String NS_USER_EXT = "http://example.com/xml/ns/user-extension";
	public static final String NS_ROOT = "http://example.com/xml/ns/test/root.xsd";
	public static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
	public static final String NS_ADHOC = "http://midpoint.evolveum.com/xml/ns/test/adhoc-1.xsd";
	public static final String NS_WEAPONS = "http://midpoint.evolveum.com/xml/ns/test/weapons";
	public static final String NS_WEAPONS_PREFIX = "w";
	
	// FOO schema
	public static final QName USER_QNAME = new QName(NS_FOO,"user");
	public static final QName USER_TYPE_QNAME = new QName(NS_FOO,"UserType");
	
	public static final QName OBJECT_REFERENCE_TYPE_QNAME = new QName(NS_FOO, "ObjectReferenceType");
	
	public static final QName USER_EXTENSION_QNAME = new QName(NS_FOO,"extension");
	
	public static final QName USER_NAME_QNAME = new QName(NS_FOO,"name");
	public static final QName USER_FULLNAME_QNAME = new QName(NS_FOO,"fullName");
	public static final QName USER_GIVENNAME_QNAME = new QName(NS_FOO,"givenName");
	public static final QName USER_FAMILYNAME_QNAME = new QName(NS_FOO,"familyName");
	public static final QName USER_ADDITIONALNAMES_QNAME = new QName(NS_FOO,"additionalNames");
	public static final QName USER_POLYNAME_QNAME = new QName(NS_FOO,"polyName");
	public static final QName USER_LOCALITY_QNAME = new QName(NS_FOO,"locality");

	public static final QName USER_ACTIVATION_QNAME = new QName(NS_FOO,"activation");
	public static final QName USER_ENABLED_QNAME = new QName(NS_FOO,"enabled");
	public static final ItemPath USER_ENABLED_PATH = new ItemPath(USER_ACTIVATION_QNAME, USER_ENABLED_QNAME);
	public static final QName USER_VALID_FROM_QNAME = new QName(NS_FOO,"validFrom");
	public static final ItemPath USER_VALID_FROM_PATH = new ItemPath(USER_ACTIVATION_QNAME, USER_VALID_FROM_QNAME);
	public static final QName USER_VALID_TO_QNAME = new QName(NS_FOO,"validTo");
	public static final QName ACTIVATION_TYPE_QNAME = new QName(NS_FOO,"ActivationType");
	
	public static final QName USER_ASSIGNMENT_QNAME = new QName(NS_FOO,"assignment");
	public static final QName USER_DESCRIPTION_QNAME = new QName(NS_FOO,"description");
	public static final ItemPath USER_ASSIGNMENT_DESCRIPTION_PATH = new ItemPath(USER_ASSIGNMENT_QNAME, USER_DESCRIPTION_QNAME);
	public static final QName ASSIGNMENT_TYPE_QNAME = new QName(NS_FOO,"AssignmentType");
	public static final QName USER_ACCOUNT_CONSTRUCTION_QNAME = new QName(NS_FOO,"accountConstruction");
	public static final Long USER_ASSIGNMENT_1_ID = 1111L;
	public static final Long USER_ASSIGNMENT_2_ID = 1112L;
	public static final Long USER_ASSIGNMENT_3_ID = 1113L;
	
	public static final QName ACCOUNT_CONSTRUCTION_TYPE_QNAME = new QName(NS_FOO,"AccountConstructionType");
	
	public static final QName USER_ACCOUNTREF_QNAME = new QName(NS_FOO,"accountRef");
	public static final QName USER_ACCOUNT_QNAME = new QName(NS_FOO,"account");
	
	public static final QName ACCOUNT_TYPE_QNAME = new QName(NS_FOO,"AccountType");
	
	public static final QName ACCOUNT_NAME_QNAME = new QName(NS_FOO,"name");
	public static final QName ACCOUNT_DESCRIPTION_QNAME = new QName(NS_FOO,"description");
	public static final QName ACCOUNT_ATTRIBUTES_QNAME = new QName(NS_FOO,"attributes");
	
	public static final QName ATTRIBUTES_TYPE_QNAME = new QName(NS_FOO,"AttributesType");
	
	public static final QName DUMMY_PROTECTED_STRING_TYPE = new QName(NS_FOO, "DummyProtectedStringType");
	
	// extension.xsd
	public static final QName EXTENSION_STRING_TYPE_ELEMENT = new QName(NS_EXTENSION, "stringType");
	public static final QName EXTENSION_SINGLE_STRING_TYPE_ELEMENT = new QName(NS_EXTENSION, "singleStringType");
	public static final QName EXTENSION_INT_TYPE_ELEMENT = new QName(NS_EXTENSION, "intType");
	public static final QName EXTENSION_IGNORED_TYPE_ELEMENT = new QName(NS_EXTENSION, "ignoredType");
	public static final QName EXTENSION_INDEXED_STRING_TYPE_ELEMENT = new QName(NS_EXTENSION, "indexedString");
	public static final QName EXTENSION_BLADE_TYPE_QNAME = new QName(NS_EXTENSION, "BladeType");
	public static final QName EXTENSION_MELEE_CONTEXT_ELEMENT = new QName(NS_EXTENSION, "meleeContext");
	public static final QName EXTENSION_MELEE_CONTEXT_TYPE_QNAME = new QName(NS_EXTENSION, "MeleeContextType");
	public static final QName EXTENSION_MELEE_CONTEXT_OPPONENT_REF_ELEMENT = new QName(NS_EXTENSION, "opponentRef");
	
	// These are NOT in the extension.xsd but are used as dynamic elements
	public static final QName EXTENSION_BAR_ELEMENT = new QName(NS_EXTENSION, "bar");
	public static final QName EXTENSION_FOOBAR_ELEMENT = new QName(NS_EXTENSION, "foobar");
	public static final QName EXTENSION_NUM_ELEMENT = new QName(NS_EXTENSION, "num");
	public static final QName EXTENSION_MULTI_ELEMENT = new QName(NS_EXTENSION, "multi");

	// Definitions used in schema-extra/extension/user.xsd
	// this is loaded as an extra schema, it is not part of usual tests. Mind the namespace that is
	// different from the previous elements
	public static final QName USER_EXT_BAR_ELEMENT = new QName(NS_USER_EXT, "bar");
	public static final QName USER_EXT_FOOBAR_ELEMENT = new QName(NS_USER_EXT, "foobar");
	public static final QName USER_EXT_NUM_ELEMENT = new QName(NS_USER_EXT, "num");
	public static final QName USER_EXT_MULTI_ELEMENT = new QName(NS_USER_EXT, "multi");

	public static final QName USER_ADHOC_BOTTLES_ELEMENT = new QName(NS_ADHOC, "bottles");
	
	public static final QName WEAPONS_WEAPON_BRAND_TYPE_QNAME = new QName(NS_WEAPONS, "WeaponBrandType");
	
	public static PrismContext constructInitializedPrismContext() throws SchemaException, SAXException, IOException {
		PrismContext context = constructPrismContext();
		context.initialize();
		return context;
	}
		
	public static PrismContext constructPrismContext() throws SchemaException, FileNotFoundException {
		SchemaRegistry schemaRegistry = new SchemaRegistry();
		DynamicNamespacePrefixMapper prefixMapper = new GlobalDynamicNamespacePrefixMapper();
		// Set default namespace?
		schemaRegistry.setNamespacePrefixMapper(prefixMapper);
		schemaRegistry.registerPrismDefaultSchemaResource("xml/ns/test/foo-1.xsd", "foo", ObjectFactory.class.getPackage());
		schemaRegistry.registerPrismSchemaResource("xml/ns/test/foo-types-1.xsd", "foot", null);
		schemaRegistry.registerSchemaResource("xml/ns/standard/XMLSchema.xsd", "xsd");
		schemaRegistry.registerPrismSchemasFromDirectory(SCHEMA_DIR);
		prefixMapper.registerPrefix(PrismConstants.NS_ANNOTATION, PrismConstants.PREFIX_NS_ANNOTATION, false);
		prefixMapper.registerPrefix(PrismInternalTestUtil.NS_WEAPONS, PrismInternalTestUtil.NS_WEAPONS_PREFIX, false);
		schemaRegistry.setObjectSchemaNamespace(NS_FOO);
		PrismContext context = PrismContext.create(schemaRegistry);
		return context;
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
		final List<Visitable> visits = new ArrayList<Visitable>();
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				visits.add(visitable);
				System.out.println("Visiting: "+visitable);
			}
		};
		visitable.accept(visitor);
		assertEquals("Wrong number of visits", expectedVisits, visits.size());
	}
	
	public static void assertPathVisitor(PathVisitable visitable, final ItemPath path, final boolean recursive, int expectedVisits) {
		final List<Visitable> visits = new ArrayList<Visitable>();
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable) {
				visits.add(visitable);
				System.out.println("Visiting(path="+path+",recursive="+recursive+"): "+visitable);
			}
		};
		visitable.accept(visitor, path, recursive);
		assertEquals("Wrong number of visits for path "+path, expectedVisits, visits.size());
	}

}
