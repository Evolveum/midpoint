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
package com.evolveum.midpoint.provisioning;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.test.impl.TestDummy;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowEntitlementsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;

/**
 * @author semancik
 *
 */
public class ProvisioningTestUtil {
	
	public static final String COMMON_TEST_DIR_FILENAME = "src/test/resources/object/";
	
	
	public static final String RESOURCE_DUMMY_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-9999dddddddd";
	
	public static final String RESOURCE_DUMMY_ATTR_FULLNAME_LOCALNAME = "fullname";
	public static final QName RESOURCE_DUMMY_ATTR_FULLNAME_QNAME = new QName(RESOURCE_DUMMY_NS, RESOURCE_DUMMY_ATTR_FULLNAME_LOCALNAME);
	public static final ItemPath RESOURCE_DUMMY_ATTR_FULLNAME_PATH = new ItemPath(ShadowType.F_ATTRIBUTES, RESOURCE_DUMMY_ATTR_FULLNAME_QNAME);	
	
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME = "title";
	public static final QName RESOURCE_DUMMY_ATTR_TITLE_QNAME = new QName(RESOURCE_DUMMY_NS, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
	public static final ItemPath RESOURCE_DUMMY_ATTR_TITLE_PATH = new ItemPath(ShadowType.F_ATTRIBUTES, RESOURCE_DUMMY_ATTR_TITLE_QNAME);
	
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME = "ship";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME = "weapon";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME = "loot";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_TREASURE_NAME = "treasure";
	
	public static final String DUMMY_GROUP_MEMBERS_ATTRIBUTE_NAME = "members";
	
	public static final String DUMMY_GROUP_ATTRIBUTE_DESCRIPTION = "description";
	public static final QName DUMMY_GROUP_ATTRIBUTE_DESCRIPTION_QNAME = new QName(RESOURCE_DUMMY_NS, DUMMY_GROUP_ATTRIBUTE_DESCRIPTION);
	public static final ItemPath DUMMY_GROUP_ATTRIBUTE_DESCRIPTION_PATH = new ItemPath(ShadowType.F_ATTRIBUTES, DUMMY_GROUP_ATTRIBUTE_DESCRIPTION_QNAME);
	
	public static final String DUMMY_ENTITLEMENT_GROUP_NAME = "group";
	public static final QName DUMMY_ENTITLEMENT_GROUP_QNAME = new QName(RESOURCE_DUMMY_NS, DUMMY_ENTITLEMENT_GROUP_NAME);

	public static final String DUMMY_ENTITLEMENT_PRIVILEGE_NAME = "priv";
	public static final QName DUMMY_ENTITLEMENT_PRIVILEGE_QNAME = new QName(RESOURCE_DUMMY_NS, DUMMY_ENTITLEMENT_PRIVILEGE_NAME);
	

	public static void assertConnectorSchemaSanity(ConnectorType conn, PrismContext prismContext) throws SchemaException {
		XmlSchemaType xmlSchemaType = conn.getSchema();
		assertNotNull("xmlSchemaType is null",xmlSchemaType);
		Element connectorXsdSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(conn);
		assertNotNull("No schema", connectorXsdSchemaElement);
		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaType);
		assertNotNull("No xsd:schema element in xmlSchemaType",xsdElement);
		display("XSD schema of "+conn, DOMUtil.serializeDOMToString(xsdElement));
		// Try to parse the schema
		PrismSchema schema = null;
		try {
			schema = PrismSchema.parse(xsdElement, "schema of "+conn, prismContext);
		} catch (SchemaException e) {
			throw new SchemaException("Error parsing schema of "+conn+": "+e.getMessage(),e);
		}
		assertConnectorSchemaSanity(schema, conn.toString());
	}
	
	public static void assertConnectorSchemaSanity(PrismSchema schema, String connectorDescription) {
		assertNotNull("Cannot parse connector schema of "+connectorDescription,schema);
		assertFalse("Empty connector schema in "+connectorDescription,schema.isEmpty());
		display("Parsed connector schema of "+connectorDescription,schema);
		
		// Local schema namespace is used here.
		PrismContainerDefinition configurationDefinition = 
			schema.findItemDefinition(ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart(), PrismContainerDefinition.class);
		assertNotNull("Definition of <configuration> property container not found in connector schema of "+connectorDescription,
				configurationDefinition);
		assertFalse("Empty definition of <configuration> property container in connector schema of "+connectorDescription,
				configurationDefinition.isEmpty());
		
		// ICFC schema is used on other elements
		PrismContainerDefinition configurationPropertiesDefinition = 
			configurationDefinition.findContainerDefinition(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("Definition of <configurationProperties> property container not found in connector schema of "+connectorDescription,
				configurationPropertiesDefinition);
		assertFalse("Empty definition of <configurationProperties> property container in connector schema of "+connectorDescription,
				configurationPropertiesDefinition.isEmpty());
		assertFalse("No definitions in <configurationProperties> in "+connectorDescription, configurationPropertiesDefinition.getDefinitions().isEmpty());

		// TODO: other elements
	}
	
	public static void assertIcfResourceSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType) {
		QName objectClassQname = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass");
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(objectClassQname);
		assertNotNull("No object class definition for "+objectClassQname+" in resource schema", accountDefinition);
		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		assertTrue("Mismatched account definition: "+accountDefinition+" <-> "+accountDef, accountDefinition == accountDef);
		
		assertNotNull("No object class definition " + objectClassQname, accountDefinition);
		assertEquals("Object class " + objectClassQname + " is not account", ShadowKindType.ACCOUNT, accountDefinition.getKind());
		assertTrue("Object class " + objectClassQname + " is not default account", accountDefinition.isDefaultInAKind());
		assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isEmpty());
		assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isIgnored());
		
		Collection<? extends ResourceAttributeDefinition> identifiers = accountDefinition.getIdentifiers();
		assertNotNull("Null identifiers for " + objectClassQname, identifiers);
		assertFalse("Empty identifiers for " + objectClassQname, identifiers.isEmpty());

		ResourceAttributeDefinition icfAttributeDefinition = accountDefinition.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		assertNotNull("No definition for attribute "+ConnectorFactoryIcfImpl.ICFS_UID, icfAttributeDefinition);
		assertTrue("Attribute "+ConnectorFactoryIcfImpl.ICFS_UID+" in not an identifier",icfAttributeDefinition.isIdentifier(accountDefinition));
		assertTrue("Attribute "+ConnectorFactoryIcfImpl.ICFS_UID+" in not in identifiers list",identifiers.contains(icfAttributeDefinition));
		
		Collection<? extends ResourceAttributeDefinition> secondaryIdentifiers = accountDefinition.getSecondaryIdentifiers();
		assertNotNull("Null secondary identifiers for " + objectClassQname, secondaryIdentifiers);
		assertFalse("Empty secondary identifiers for " + objectClassQname, secondaryIdentifiers.isEmpty());
		
		ResourceAttributeDefinition nameAttributeDefinition = accountDefinition.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertNotNull("No definition for attribute "+ConnectorFactoryIcfImpl.ICFS_NAME, nameAttributeDefinition);
		assertTrue("Attribute "+ConnectorFactoryIcfImpl.ICFS_NAME+" in not an identifier",nameAttributeDefinition.isSecondaryIdentifier(accountDefinition));
		assertTrue("Attribute "+ConnectorFactoryIcfImpl.ICFS_NAME+" in not in identifiers list",secondaryIdentifiers.contains(nameAttributeDefinition));

		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		ResourceAttributeDefinition uidDef = accountDef
				.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(0, uidDef.getMinOccurs());
		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
		assertFalse("UID has create", uidDef.canCreate());
		assertFalse("UID has update",uidDef.canUpdate());
		assertTrue("No UID read",uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(uidDef));

		ResourceAttributeDefinition nameDef = accountDef
				.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canCreate());
		assertTrue("No NAME update",nameDef.canUpdate());
		assertTrue("No NAME read",nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));
		
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));
	}

	public static void checkRepoAccountShadow(PrismObject<ShadowType> repoShadow) {
		checkRepoShadow(repoShadow, ShadowKindType.ACCOUNT);
	}
	
	public static void checkRepoEntitlementShadow(PrismObject<ShadowType> repoShadow) {
		checkRepoShadow(repoShadow, ShadowKindType.ENTITLEMENT);
	}
	
	public static void checkRepoShadow(PrismObject<ShadowType> repoShadow, ShadowKindType kind) {
		ShadowType repoShadowType = repoShadow.asObjectable();
		assertNotNull("No OID in repo shadow "+repoShadow, repoShadowType.getOid());
		assertNotNull("No name in repo shadow "+repoShadow, repoShadowType.getName());
		assertNotNull("No objectClass in repo shadow "+repoShadow, repoShadowType.getObjectClass());
		assertEquals("Wrong kind in repo shadow "+repoShadow, kind, repoShadowType.getKind());
		PrismContainer<Containerable> attributesContainer = repoShadow.findContainer(ShadowType.F_ATTRIBUTES);
		assertNotNull("No attributes in repo shadow "+repoShadow, attributesContainer);
		List<Item<?>> attributes = attributesContainer.getValue().getItems();
		assertFalse("Empty attributes in repo shadow "+repoShadow, attributes.isEmpty());
		assertEquals("Unexpected number of attributes in repo shadow "+repoShadow, 2, attributes.size());
	}

	public static void assertDummyResourceSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType) {
		ProvisioningTestUtil.assertIcfResourceSchemaSanity(resourceSchema, resourceType);
		
		// ACCOUNT
		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		assertNotNull("No ACCOUNT kind definition", accountDef);
		
		ResourceAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canCreate());
		assertTrue("No fullname update", fullnameDef.canUpdate());
		assertTrue("No fullname read", fullnameDef.canRead());
		
		// GROUP
		ObjectClassComplexTypeDefinition groupObjectClass = resourceSchema.findObjectClassDefinition(ConnectorFactoryIcfImpl.GROUP_OBJECT_CLASS_LOCAL_NAME);
		assertNotNull("No group objectClass", groupObjectClass);
		
		ResourceAttributeDefinition membersDef = groupObjectClass.findAttributeDefinition(DUMMY_GROUP_MEMBERS_ATTRIBUTE_NAME);
		assertNotNull("No definition for members", membersDef);
		assertEquals("Wrong maxOccurs", -1, membersDef.getMaxOccurs());
		assertEquals("Wrong minOccurs", 0, membersDef.getMinOccurs());
		assertTrue("No members create", membersDef.canCreate());
		assertTrue("No members update", membersDef.canUpdate());
		assertTrue("No members read", membersDef.canRead());
	}
	
	public static void extendSchema(DummyResource dummyResource) throws ConnectException, FileNotFoundException {
		DummyObjectClass accountObjectClass = dummyResource.getAccountObjectClass();		
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, String.class, false, true);
		DummyAttributeDefinition lootAttrDef = addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, Integer.class, false, false);
		lootAttrDef.setReturnedByDefault(false);
		DummyAttributeDefinition treasureAttrDef = addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_TREASURE_NAME, String.class, false, false);
		treasureAttrDef.setReturnedByDefault(false);
		
		DummyObjectClass groupObjectClass = dummyResource.getGroupObjectClass();		
		addAttrDef(groupObjectClass, DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, String.class, false, false);
	}
	
	private static DummyAttributeDefinition addAttrDef(DummyObjectClass accountObjectClass, String attrName, Class<?> type, boolean isRequired, boolean isMulti) {
		DummyAttributeDefinition attrDef = new DummyAttributeDefinition(attrName, type, isRequired, isMulti);
		accountObjectClass.add(attrDef);
		return attrDef;
	}
	
	public static void assertDummyResourceSchemaSanityExtended(ResourceSchema resourceSchema, ResourceType resourceType) {
		assertDummyResourceSchemaSanity(resourceSchema, resourceType);
		
		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);		
		assertEquals("Unexpected number of defnitions", 11, accountDef.getDefinitions().size());
		ResourceAttributeDefinition treasureDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_TREASURE_NAME);
		assertFalse("Treasure IS returned by default and should not be", treasureDef.isReturnedByDefault());
	}
	
	public static QName getDefaultAccountObjectClass(ResourceType resourceType) {
		String namespace = ResourceTypeUtil.getResourceNamespace(resourceType);
		return new QName(namespace, ConnectorFactoryIcfImpl.ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
	}
	
	public static <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, String attrName, 
			T... expectedValues) {
		QName attrQname = new QName(ResourceTypeUtil.getResourceNamespace(resource), attrName);
		assertAttribute(resource, shadow, attrQname, expectedValues);
	}
	
	public static <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, QName attrQname, 
			T... expectedValues) {
		List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
		PrismAsserts.assertSets("attribute "+attrQname+" in " + shadow, actualValues, expectedValues);
	}
	
	public static ObjectDelta<ShadowType> createEntitleDelta(String accountOid, QName associationName, String groupOid, PrismContext prismContext) throws SchemaException {
		ShadowAssociationType association = new ShadowAssociationType();
		association.setName(associationName);
		association.setOid(groupOid);
		ItemPath entitlementAssociationPath = new ItemPath(ShadowType.F_ENTITLEMENTS, ShadowEntitlementsType.F_ASSOCIATION);
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationAddContainer(ShadowType.class, 
				accountOid, entitlementAssociationPath, prismContext, association);
		return delta;
	}
	
	public static ObjectDelta<ShadowType> createDetitleDelta(String accountOid, QName associationName, String groupOid, PrismContext prismContext) throws SchemaException {
		ShadowAssociationType association = new ShadowAssociationType();
		association.setName(associationName);
		association.setOid(groupOid);
		ItemPath entitlementAssociationPath = new ItemPath(ShadowType.F_ENTITLEMENTS, ShadowEntitlementsType.F_ASSOCIATION);
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationDeleteContainer(ShadowType.class, 
				accountOid, entitlementAssociationPath, prismContext, association);
		return delta;
	}

}
