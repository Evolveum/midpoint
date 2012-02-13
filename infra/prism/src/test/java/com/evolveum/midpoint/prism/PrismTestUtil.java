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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.util.Set;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ObjectFactory;
import com.evolveum.midpoint.schema.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class PrismTestUtil {

	// TODO: Globalize
	private static final String NS_TYPE = "http://midpoint.evolveum.com/xml/ns/public/common/types-1.xsd";
	public static final QName OBJECT_REFERENCE_TYPE_QNAME = new QName(NS_TYPE, "ObjectReferenceType");
	
	public static final String NS_FOO = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd";
	public static final String NS_BAR = "http://www.example.com/bar";
	
	public static final QName USER_QNAME = new QName(NS_FOO,"user");
	public static final QName USER_EXTENSION_QNAME = new QName(NS_FOO,"extension");
	
	public static final QName USER_NAME_QNAME = new QName(NS_FOO,"name");
	public static final QName USER_FULLNAME_QNAME = new QName(NS_FOO,"fullName");
	public static final QName USER_GIVENNAME_QNAME = new QName(NS_FOO,"givenName");
	public static final QName USER_FAMILYNAME_QNAME = new QName(NS_FOO,"familyName");
	public static final QName USER_ADDITIONALNAMES_QNAME = new QName(NS_FOO,"additionalNames");

	public static final QName USER_ACTIVATION_QNAME = new QName(NS_FOO,"activation");
	public static final QName USER_ENABLED_QNAME = new QName(NS_FOO,"enabled");
	public static final PropertyPath USER_ENABLED_PATH = new PropertyPath(USER_ACTIVATION_QNAME, USER_ENABLED_QNAME);
	public static final QName ACTIVATION_TYPE_QNAME = new QName(NS_FOO,"ActivationType");
	
	public static final QName USER_ASSIGNMENT_QNAME = new QName(NS_FOO,"assignment");
	public static final QName USER_DESCRIPTION_QNAME = new QName(NS_FOO,"description");
	public static final PropertyPath USER_ASSIGNMENT_DESCRIPTION_PATH = new PropertyPath(USER_ASSIGNMENT_QNAME, USER_DESCRIPTION_QNAME);
	public static final QName ASSIGNMENT_TYPE_QNAME = new QName(NS_FOO,"AssignmentType");
	
	public static final QName USER_ACCOUNTREF_QNAME = new QName(NS_FOO,"accountRef");
	public static final QName USER_ACCOUNT_QNAME = new QName(NS_FOO,"account");
	
	public static final QName ACCOUNT_TYPE_QNAME = new QName(NS_FOO,"AccountType");
	
	
	public static void assertDefinition(Item item, QName type, int minOccurs, int maxOccurs) {
		ItemDefinition definition = item.getDefinition();
		assertDefinition(definition, item.getName(), type, minOccurs, maxOccurs);
	}
		
	public static void assertPropertyDefinition(PrismContainerDefinition containerDef, QName propertyName, QName type, int minOccurs, int maxOccurs) {
		PrismPropertyDefinition definition = containerDef.findPropertyDefinition(propertyName);
		assertDefinition(definition, propertyName, type, minOccurs, maxOccurs);
	}
	
	public static void assertDefinition(ItemDefinition definition, QName itemName, QName type, int minOccurs, int maxOccurs) {
		assertNotNull("No definition for "+itemName, definition);
		assertEquals("Wrong definition type for "+itemName, type, definition.getTypeName());
		assertEquals("Wrong definition minOccurs for "+itemName, minOccurs, definition.getMinOccurs());
		assertEquals("Wrong definition maxOccurs for "+itemName, maxOccurs, definition.getMaxOccurs());
	}
	
	public static void assertReferenceValue(PrismReference ref, String oid) {
		for (PrismReferenceValue val: ref.getValues()) {
			if (oid.equals(val.getOid())) {
				return;
			}
		}
		AssertJUnit.fail("Oid "+oid+" not found in reference "+ref);
	}

	
	public static PrismContext constructPrismContext() throws SchemaException, SAXException, IOException {
		
		SchemaRegistry schemaRegistry = new SchemaRegistry();
		DynamicNamespacePrefixMapper prefixMapper = new GlobalDynamicNamespacePrefixMapper();
		// Set default namespace?
		schemaRegistry.setNamespacePrefixMapper(prefixMapper);
		schemaRegistry.registerPrismSchemaResource("xml/ns/test/foo-1.xsd", "foo", ObjectFactory.class.getPackage());
		schemaRegistry.setObjectSchemaNamespace(NS_FOO);
		schemaRegistry.initialize();
		
		PrismContext context = PrismContext.create(schemaRegistry);
		return context;
	}

}
