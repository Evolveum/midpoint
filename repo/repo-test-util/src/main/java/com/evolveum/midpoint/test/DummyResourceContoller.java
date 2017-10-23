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
package com.evolveum.midpoint.test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.FileNotFoundException;
import java.net.ConnectException;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.MidPointConstants;

import org.apache.commons.lang.StringUtils;
import org.testng.AssertJUnit;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyOrg;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.test.ldap.AbstractResourceController;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class DummyResourceContoller extends AbstractResourceController {

	public static final String DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME = "fullname";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME = "title";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME = "location";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME = "loot";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_TREASURE_NAME = "treasure";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_WEALTH_NAME = "wealth";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME = "ship";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME = "weapon";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME = "drink";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME = "quote";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME = "gossip";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME = "water";

	public static final QName DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME = new QName(MidPointConstants.NS_RI, DummyAccount.ATTR_FULLNAME_NAME);
	public static final QName DUMMY_ACCOUNT_ATTRIBUTE_DESCRIPTION_QNAME = new QName(MidPointConstants.NS_RI, DummyAccount.ATTR_DESCRIPTION_NAME);

	public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME = "givenName";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME = "sn";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME = "sAMAccountName";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME = "userPrincipalName";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME = "mail";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_SHARED_FOLDER_OTHER_NAME = "userSharedFolderOther";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_DEPARTMENT_NAME = "department";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_FACSIMILE_TELEPHONE_NUMBER_NAME = "facsimileTelephoneNumber";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME = "telephoneNumber";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_MOBILE_NAME = "mobile";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_IP_PHONE_NAME = "ipPhone";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_PHYSICAL_DELIVERY_OFFICE_NAME_NAME = "physicalDeliveryOfficeName";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_DESCRIPTION_NAME = "description";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_ACCOUNT_EXPIRES_NAME = "accountExpires";
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_AD_GROUPS_NAME = "groups";

	public static final String DUMMY_ACCOUNT_ATTRIBUTE_POSIX_UID_NUMBER = "uidNumber";
	public static final String DUMMY_ACCOUNT_ATTRIBUTE_POSIX_GID_NUMBER = "gidNumber";

    public static final String DUMMY_GROUP_MEMBERS_ATTRIBUTE_NAME = "members";
	public static final String DUMMY_GROUP_ATTRIBUTE_DESCRIPTION = "description";
    public static final String DUMMY_GROUP_ATTRIBUTE_CC = "cc";

    public static final String DUMMY_PRIVILEGE_ATTRIBUTE_POWER = "power";

	public static final String DUMMY_ENTITLEMENT_GROUP_NAME = "group";
	public static final String DUMMY_ENTITLEMENT_PRIVILEGE_NAME = "privileges";

	public static final String CONNECTOR_DUMMY_NS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector";
	public static final String CONNECTOR_DUMMY_USELESS_STRING_NAME = "uselessString";
	public static final QName CONNECTOR_DUMMY_USELESS_STRING_QNAME = new QName(CONNECTOR_DUMMY_NS, CONNECTOR_DUMMY_USELESS_STRING_NAME);

	public static final String ORG_TOP_NAME = "top";

	public static final String OBJECTCLASS_ORG_LOCAL_PART = "CustomorgObjectClass";

	public static final String DUMMY_POSIX_ACCOUNT_OBJECT_CLASS_NAME = "posixAccount";

	private DummyResource dummyResource;
	private boolean isExtendedSchema = false;
	private String instanceName;


	public static DummyResourceContoller create(String instanceName) {
		return create(instanceName, null);
	}

	public static DummyResourceContoller create(String instanceName, PrismObject<ResourceType> resource) {
		DummyResourceContoller ctl = new DummyResourceContoller();

		ctl.instanceName = instanceName;
		ctl.dummyResource = DummyResource.getInstance(instanceName);
		ctl.dummyResource.reset();

		ctl.resource = resource;

		return ctl;
	}

	public DummyResource getDummyResource() {
		return dummyResource;
	}

	public String getName() {
		return instanceName;
	}

	public void populateWithDefaultSchema() {
		dummyResource.populateWithDefaultSchema();
	}

	/**
	 * Extend schema in piratey fashion. Arr! This is used in many tests. Lots of attributes, various combination of types, etc.
	 */
	public void extendSchemaPirate() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		populateWithDefaultSchema();
		DummyObjectClass accountObjectClass = dummyResource.getAccountObjectClass();
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, String.class, false, false);
		DummyAttributeDefinition lootAttrDef =  addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, Integer.class, false, false);
		lootAttrDef.setReturnedByDefault(false);
		DummyAttributeDefinition treasureAttrDef = addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_TREASURE_NAME, String.class, false, false);
		treasureAttrDef.setReturnedByDefault(false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_WEALTH_NAME, Integer.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, String.class, false, false);

		DummyObjectClass groupObjectClass = dummyResource.getGroupObjectClass();
		addAttrDef(groupObjectClass, DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, String.class, false, false);
        addAttrDef(groupObjectClass, DUMMY_GROUP_ATTRIBUTE_CC, String.class, false, false);

        DummyObjectClass privilegeObjectClass = dummyResource.getPrivilegeObjectClass();
		addAttrDef(privilegeObjectClass, DUMMY_PRIVILEGE_ATTRIBUTE_POWER, Integer.class, false, false);

		isExtendedSchema = true;
	}

	/**
	 * Extend dummy schema to look like AD
	 */
	public void extendSchemaAd() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		DummyObjectClass accountObjectClass = dummyResource.getAccountObjectClass();
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_GIVEN_NAME_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_SN_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_PRINCIPAL_NAME_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_USER_SHARED_FOLDER_OTHER_NAME, String.class, false, true);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_MAIL_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_DEPARTMENT_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_FACSIMILE_TELEPHONE_NUMBER_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_TELEPHONE_NUMBER_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_MOBILE_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_IP_PHONE_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_PHYSICAL_DELIVERY_OFFICE_NAME_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_DESCRIPTION_NAME, String.class, false, false);
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_ACCOUNT_EXPIRES_NAME, Long.class, false, false);
		// This should in fact be icfs:groups but this is OK for now
		addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_AD_GROUPS_NAME, String.class, false, true);

		isExtendedSchema = true;
	}

	/**
	 * Extend dummy schema to have an auxiliary OC.
	 */
	public void extendSchemaPosix() throws ConnectException, FileNotFoundException, SchemaViolationException {
		DummyObjectClass posixAccount = new DummyObjectClass();
		addAttrDef(posixAccount, DUMMY_ACCOUNT_ATTRIBUTE_POSIX_UID_NUMBER, Integer.class, false, false);			// uid and gid are temporarily not required
		addAttrDef(posixAccount, DUMMY_ACCOUNT_ATTRIBUTE_POSIX_GID_NUMBER, Integer.class, false, false);
		dummyResource.addAuxiliaryObjectClass(DUMMY_POSIX_ACCOUNT_OBJECT_CLASS_NAME, posixAccount);
		isExtendedSchema = true;
	}

	public DummyAttributeDefinition addAttrDef(DummyObjectClass accountObjectClass, String attrName, Class<?> type, boolean isRequired, boolean isMulti) {
		DummyAttributeDefinition attrDef = new DummyAttributeDefinition(attrName, type, isRequired, isMulti);
		accountObjectClass.add(attrDef);
		return attrDef;
	}

	public QName getAttributeQName(String attrName) {
		return new QName(getNamespace(), attrName);
	}

	public ItemPath getAttributePath(QName attrQName) {
		return new ItemPath(ShadowType.F_ATTRIBUTES, attrQName);
	}

	public ItemPath getAttributePath(String attrName) {
		return new ItemPath(ShadowType.F_ATTRIBUTES, getAttributeQName(attrName));
	}

	public QName getAttributeFullnameQName() {
		return  getAttributeQName(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);
	}

	public ItemPath getAttributeFullnamePath() {
		return new ItemPath(ShadowType.F_ATTRIBUTES, getAttributeFullnameQName());
	}

	public QName getAttributeWeaponQName() {
		assertExtendedSchema();
		return  getAttributeQName(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
	}

	public ItemPath getAttributeWeaponPath() {
		assertExtendedSchema();
		return new ItemPath(ShadowType.F_ATTRIBUTES, getAttributeWeaponQName());
	}

	public QName getAttributeLootQName() {
		assertExtendedSchema();
		return  getAttributeQName(DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME);
	}

	public ItemPath getAttributeLootPath() {
		assertExtendedSchema();
		return new ItemPath(ShadowType.F_ATTRIBUTES, getAttributeLootQName());
	}

	private void assertExtendedSchema() {
		assert isExtendedSchema : "Resource "+resource+" does not have extended schema yet an extended attribute was requested";
	}

	public void assertDummyResourceSchemaSanity(ResourceSchema resourceSchema) {
		assertDummyResourceSchemaSanity(resourceSchema, resource.asObjectable(), true);
	}

	public void assertDummyResourceSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType, boolean checkDisplayOrder) {
		IntegrationTestTools.assertIcfResourceSchemaSanity(resourceSchema, resourceType);

		// ACCOUNT
		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		assertNotNull("No ACCOUNT kind definition", accountDef);

		ResourceAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canAdd());
		assertTrue("No fullname update", fullnameDef.canModify());
		assertTrue("No fullname read", fullnameDef.canRead());
		if (checkDisplayOrder) {
			// TODO: fix, see MID-2642
			assertTrue("Wrong displayOrder for attribute fullName: "+fullnameDef.getDisplayOrder(),
					fullnameDef.getDisplayOrder() == 200 || fullnameDef.getDisplayOrder() == 250 || fullnameDef.getDisplayOrder() == 260);
		}

		// GROUP
		ObjectClassComplexTypeDefinition groupObjectClass = resourceSchema.findObjectClassDefinition(SchemaTestConstants.GROUP_OBJECT_CLASS_LOCAL_NAME);
		assertNotNull("No group objectClass", groupObjectClass);

		ResourceAttributeDefinition membersDef = groupObjectClass.findAttributeDefinition(DUMMY_GROUP_MEMBERS_ATTRIBUTE_NAME);
		assertNotNull("No definition for members", membersDef);
		assertEquals("Wrong maxOccurs", -1, membersDef.getMaxOccurs());
		assertEquals("Wrong minOccurs", 0, membersDef.getMinOccurs());
		assertTrue("No members create", membersDef.canAdd());
		assertTrue("No members update", membersDef.canModify());
		assertTrue("No members read", membersDef.canRead());

		assertEquals("Unexpected number of schema definitions in "+getName()+" dummy resource", dummyResource.getNumberOfObjectclasses(), resourceSchema.getDefinitions().size());

		for (Definition def: resourceSchema.getDefinitions()) {
			if (def instanceof RefinedObjectClassDefinition) {
				AssertJUnit.fail("Refined definition sneaked into resource schema of "+getName()+" dummy resource: "+def);
			}
		}
	}

	public void assertDummyResourceSchemaSanityExtended(ResourceSchema resourceSchema) {
		assertDummyResourceSchemaSanityExtended(resourceSchema, resource.asObjectable(), true);
	}

	public void assertDummyResourceSchemaSanityExtended(ResourceSchema resourceSchema, ResourceType resourceType, boolean checkDisplayOrder) {
		assertDummyResourceSchemaSanityExtended(resourceSchema, resourceType, checkDisplayOrder, 18);
	}

	public void assertDummyResourceSchemaSanityExtended(ResourceSchema resourceSchema, ResourceType resourceType, boolean checkDisplayOrder, int numberOfAccountDefinitions) {
		assertDummyResourceSchemaSanity(resourceSchema, resourceType, checkDisplayOrder);

		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		assertNotNull("No default account definition", accountDef);
		ObjectClassComplexTypeDefinition accountObjectClassDef = resourceSchema.findObjectClassDefinition(SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
		assertNotNull("No AccountObjectClass definition", accountObjectClassDef);
		assertTrue("Default account definition is not same as AccountObjectClass", accountDef == accountObjectClassDef);
		assertEquals("Unexpected number of definitions", numberOfAccountDefinitions, accountDef.getDefinitions().size());
		ResourceAttributeDefinition treasureDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_TREASURE_NAME);
		assertFalse("Treasure IS returned by default and should not be", treasureDef.isReturnedByDefault());
		assertEquals("Unexpected kind in account definition", ShadowKindType.ACCOUNT, accountDef.getKind());
		assertTrue("Account definition in not default", accountDef.isDefaultInAKind());
		assertNull("Non-null intent in account definition", accountDef.getIntent());
		assertFalse("Account definition is deprecated", accountDef.isDeprecated());
		assertFalse("Account definition is auxiliary", accountDef.isAuxiliary());
	}

	public void assertRefinedSchemaSanity(RefinedResourceSchema refinedSchema) {

		RefinedObjectClassDefinition accountDef = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		RefinedAttributeDefinition uidDef = accountDef.findAttributeDefinition(SchemaTestConstants.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(0, uidDef.getMinOccurs());
		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
		assertFalse("UID has create", uidDef.canAdd());
		assertFalse("UID has update",uidDef.canModify());
		assertTrue("No UID read",uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getPrimaryIdentifiers().contains(uidDef));

		RefinedAttributeDefinition nameDef = accountDef.findAttributeDefinition(SchemaTestConstants.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canAdd());
		assertTrue("No NAME update",nameDef.canModify());
		assertTrue("No NAME read",nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));

		RefinedAttributeDefinition fullnameDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canAdd());
		assertTrue("No fullname update", fullnameDef.canModify());
		assertTrue("No fullname read", fullnameDef.canRead());

		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDef.findAttributeDefinition(new QName(SchemaTestConstants.NS_ICFS,"password")));

	}

	public QName getAccountObjectClass() {
        return new QName(ResourceTypeUtil.getResourceNamespace(getResourceType()), "AccountObjectClass");
    }

	public QName getGroupObjectClass() {
        return new QName(ResourceTypeUtil.getResourceNamespace(getResourceType()), "GroupObjectClass");
    }

	public DummyOrg addOrgTop() throws ConnectException, FileNotFoundException, ObjectAlreadyExistsException, SchemaViolationException, ConflictException {
		DummyOrg org = new DummyOrg(ORG_TOP_NAME);
		dummyResource.addOrg(org);
		return org;
	}

	public DummyAccount addAccount(String userId, String fullName) throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException, FileNotFoundException, ConflictException {
		DummyAccount account = new DummyAccount(userId);
		account.setEnabled(true);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, fullName);
		dummyResource.addAccount(account);
		return account;
	}

	public void addAccount(String userId, String fullName, String location) throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException, FileNotFoundException, ConflictException {
		assertExtendedSchema();
		DummyAccount account = new DummyAccount(userId);
		account.setEnabled(true);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, fullName);
		account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, location);
		dummyResource.addAccount(account);
	}

	public void addGroup(String name) throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException, FileNotFoundException, ConflictException {
		assertExtendedSchema();
		DummyGroup group = new DummyGroup(name);
		group.setEnabled(true);
		dummyResource.addGroup(group);
	}

	public QName getOrgObjectClassQName() {
		return new QName(getNamespace(), OBJECTCLASS_ORG_LOCAL_PART);
	}

	public QName getAccountObjectClassQName() {
		return new QName(getNamespace(), SchemaTestConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
	}

	/**
	 * Resets the blocking state, error simulation, etc.
	 */
	public void reset() {
		dummyResource.setBreakMode(BreakMode.NONE);
		dummyResource.setBlockOperations(false);
		dummyResource.unblockAll();
	}

}
