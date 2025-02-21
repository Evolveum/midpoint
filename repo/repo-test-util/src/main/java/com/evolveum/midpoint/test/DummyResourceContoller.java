/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_GROUP_OBJECT_CLASS;

import static java.util.Objects.requireNonNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.time.ZonedDateTime;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.test.asserter.DummyOrgAsserter;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang3.StringUtils;
import org.testng.AssertJUnit;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyOrg;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.ObjectDoesNotExistException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.test.asserter.DummyAccountAsserter;
import com.evolveum.midpoint.test.asserter.DummyGroupAsserter;
import com.evolveum.midpoint.test.ldap.AbstractResourceController;
import com.evolveum.midpoint.util.DOMUtil;
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
    public static final String DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_NAME = "enlistTimestamp";

    public static final ItemName DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME = new ItemName(NS_RI, DummyAccount.ATTR_FULLNAME_NAME);
    public static final ItemName DUMMY_ACCOUNT_ATTRIBUTE_TITLE_QNAME = new ItemName(NS_RI, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
    public static final ItemName DUMMY_ACCOUNT_ATTRIBUTE_DESCRIPTION_QNAME = new ItemName(NS_RI, DummyAccount.ATTR_DESCRIPTION_NAME);
    public static final ItemName DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME = new ItemName(NS_RI, DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME);
    public static final ItemName DUMMY_ACCOUNT_ATTRIBUTE_DRINK_QNAME = new ItemName(NS_RI, DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);
    public static final ItemName DUMMY_ACCOUNT_ATTRIBUTE_WATER_QNAME = new ItemName(NS_RI, DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME);
    public static final ItemName DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_QNAME = new ItemName(NS_RI, DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME);
    public static final ItemName DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_QNAME = new ItemName(NS_RI, DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME);
    public static final ItemName DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME = new ItemName(NS_RI, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
    public static final ItemName DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME = new ItemName(NS_RI, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
    public static final ItemName DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_QNAME = new ItemName(NS_RI, DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_NAME);

    public static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME);
    public static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_TITLE_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_QNAME);
    public static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME);
    public static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_DRINK_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_DRINK_QNAME);
    public static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_WATER_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_WATER_QNAME);
    public static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_QNAME);
    public static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME);
    public static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_QNAME);
    public static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME);
    public static final ItemPath DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_QNAME);

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

    public static final ItemName DUMMY_GROUP_ATTRIBUTE_DESCRIPTION_QNAME = new ItemName(NS_RI, DUMMY_GROUP_ATTRIBUTE_DESCRIPTION);
    public static final ItemName DUMMY_GROUP_ATTRIBUTE_CC_QNAME = new ItemName(NS_RI, DUMMY_GROUP_ATTRIBUTE_CC);
    public static final ItemPath DUMMY_GROUP_ATTRIBUTE_DESCRIPTION_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_GROUP_ATTRIBUTE_DESCRIPTION_QNAME);
    public static final ItemPath DUMMY_GROUP_ATTRIBUTE_CC_PATH = ItemPath.create(ShadowType.F_ATTRIBUTES, DUMMY_GROUP_ATTRIBUTE_CC_QNAME);

    public static final String DUMMY_PRIVILEGE_ATTRIBUTE_POWER = "power";

    public static final String DUMMY_ENTITLEMENT_GROUP_NAME = "group";
    public static final QName DUMMY_ENTITLEMENT_GROUP_QNAME = new ItemName(MidPointConstants.NS_RI, DUMMY_ENTITLEMENT_GROUP_NAME);
    public static final String DUMMY_ENTITLEMENT_PRIVILEGE_NAME = "privileges";

    public static final String CONNECTOR_DUMMY_NS = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector";
    public static final String CONNECTOR_DUMMY_USELESS_STRING_NAME = "uselessString";
    public static final QName CONNECTOR_DUMMY_USELESS_STRING_QNAME = new QName(CONNECTOR_DUMMY_NS, CONNECTOR_DUMMY_USELESS_STRING_NAME);

    public static final String ORG_TOP_NAME = "top";

    public static final String OBJECTCLASS_ORG_LOCAL_PART = "CustomorgObjectClass";

    public static final String DUMMY_POSIX_ACCOUNT_OBJECT_CLASS_NAME = "posixAccount";

    public static final int PIRATE_SCHEMA_NUMBER_OF_DEFINITIONS = 19;

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
    public void extendSchemaPirate() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
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
        addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_NAME, ZonedDateTime.class, false, false);

        DummyObjectClass groupObjectClass = dummyResource.getGroupObjectClass();
        addAttrDef(groupObjectClass, DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, String.class, false, false);
        addAttrDef(groupObjectClass, DUMMY_GROUP_ATTRIBUTE_CC, String.class, false, false);

        DummyObjectClass privilegeObjectClass = dummyResource.getPrivilegeObjectClass();
        addAttrDef(privilegeObjectClass, DUMMY_PRIVILEGE_ATTRIBUTE_POWER, Integer.class, false, false);
    }

    /**
     * Extend dummy schema to look like AD
     */
    public void extendSchemaAd() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
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
    }

    /**
     * Extend dummy schema to have an auxiliary OC.
     */
    public void extendSchemaPosix() {
        DummyObjectClass posixAccount = new DummyObjectClass();
        addAttrDef(posixAccount, DUMMY_ACCOUNT_ATTRIBUTE_POSIX_UID_NUMBER, Integer.class, false, false);            // uid and gid are temporarily not required
        addAttrDef(posixAccount, DUMMY_ACCOUNT_ATTRIBUTE_POSIX_GID_NUMBER, Integer.class, false, false);
        dummyResource.addAuxiliaryObjectClass(DUMMY_POSIX_ACCOUNT_OBJECT_CLASS_NAME, posixAccount);
    }

    public DummyAttributeDefinition addAttrDef(DummyObjectClass objectClass, String attrName, Class<?> type, boolean isRequired, boolean isMulti) {
        isExtendedSchema = true;
        DummyAttributeDefinition attrDef = new DummyAttributeDefinition(attrName, type, isRequired, isMulti);
        objectClass.add(attrDef);
        return attrDef;
    }

    public void setExtendedSchema() {
        isExtendedSchema = true;
    }

    public ItemPath getAttributePath(QName attrQName) {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName);
    }

    public ItemName getAttributeFullnameQName() {
        return  getAttributeQName(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);
    }

    public ItemPath getAttributeFullnamePath() {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeFullnameQName());
    }

    public QName getAttributeWeaponQName() {
        assertExtendedSchema();
        return  getAttributeQName(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
    }

    public ItemPath getAttributeWeaponPath() {
        assertExtendedSchema();
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeWeaponQName());
    }

    public QName getAttributeLootQName() {
        assertExtendedSchema();
        return  getAttributeQName(DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME);
    }

    public ItemPath getAttributeLootPath() {
        assertExtendedSchema();
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeLootQName());
    }

    private void assertExtendedSchema() {
        assert isExtendedSchema : "Resource "+resource+" does not have extended schema yet an extended attribute was requested";
    }

    public void assertDummyResourceSchemaSanity(ResourceSchema resourceSchema) throws SchemaException {
        assertDummyResourceSchemaSanity(resourceSchema, resource.asObjectable(), true);
    }

    public void assertDummyResourceSchemaSanity(
            ResourceSchema resourceSchema, ResourceType resourceType, boolean checkDisplayOrder)
            throws SchemaException {
        IntegrationTestTools.assertIcfResourceSchemaSanity(resourceSchema, resourceType);

        // ACCOUNT
        ResourceObjectDefinition accountDef =
                resourceSchema.findDefinitionForObjectClass(RI_ACCOUNT_OBJECT_CLASS);
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
                    fullnameDef.getDisplayOrder() == 200
                            || fullnameDef.getDisplayOrder() == 250
                            || fullnameDef.getDisplayOrder() == 270);
        }

        // GROUP
        ResourceObjectDefinition groupObjectClass =
                resourceSchema.findDefinitionForObjectClass(RI_GROUP_OBJECT_CLASS);
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
            if (def instanceof ResourceObjectTypeDefinition) {
                AssertJUnit.fail("Refined definition sneaked into resource schema of "+getName()+" dummy resource: "+def);
            }
        }
    }

    /**
     * @return default account definition
     */
    public ResourceObjectDefinition assertDummyResourceSchemaSanityExtended(ResourceSchema resourceSchema)
            throws SchemaException {
        return assertDummyResourceSchemaSanityExtended(resourceSchema, resource.asObjectable(), true);
    }

    /**
     * @return default account definition
     */
    public ResourceObjectDefinition assertDummyResourceSchemaSanityExtended(
            ResourceSchema resourceSchema, ResourceType resourceType, boolean checkDisplayOrder) throws SchemaException {
        return assertDummyResourceSchemaSanityExtended(resourceSchema, resourceType, checkDisplayOrder, PIRATE_SCHEMA_NUMBER_OF_DEFINITIONS);
    }

    /**
     * @return default account definition
     */
    public ResourceObjectDefinition assertDummyResourceSchemaSanityExtended(ResourceSchema resourceSchema, ResourceType resourceType, boolean checkDisplayOrder, int numberOfAccountDefinitions) throws SchemaException {
        assertDummyResourceSchemaSanity(resourceSchema, resourceType, checkDisplayOrder);

        ResourceObjectDefinition accountDef =
                resourceSchema.findDefinitionForObjectClass(RI_ACCOUNT_OBJECT_CLASS);
        assertNotNull("No default account definition", accountDef);
        ResourceObjectClassDefinition accountObjectClassDef =
                resourceSchema.findObjectClassDefinition(RI_ACCOUNT_OBJECT_CLASS);
        assertNotNull("No AccountObjectClass definition", accountObjectClassDef);
        assertTrue("Default account definition is not same as AccountObjectClass", accountDef == accountObjectClassDef);
        assertEquals("Unexpected number of definitions", numberOfAccountDefinitions, accountDef.getDefinitions().size());

        ResourceAttributeDefinition<?> treasureDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_TREASURE_NAME);
        assertFalse("Treasure IS returned by default and should not be", treasureDef.isReturnedByDefault());

        // MID-4751
        ResourceAttributeDefinition<?> enlistTimestampDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_NAME);
        PrismAsserts.assertDefinition(enlistTimestampDef,
                new QName(NS_RI, DUMMY_ACCOUNT_ATTRIBUTE_ENLIST_TIMESTAMP_NAME),
                DOMUtil.XSD_DATETIME, 0, 1);

        assertTrue("Account definition in not default", accountObjectClassDef.isDefaultAccountDefinition());
        assertFalse("Account definition is deprecated", accountObjectClassDef.isDeprecated());
        assertFalse("Account definition is auxiliary", accountObjectClassDef.isAuxiliary());

        return accountDef;
    }

    public void assertRefinedSchemaSanity(ResourceSchema refinedSchema) {

        ResourceObjectDefinition accountDef =
                refinedSchema.findDefaultDefinitionForKindRequired(ShadowKindType.ACCOUNT);
        assertNotNull("Account definition is missing", accountDef);
        assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
        assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
        assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
        assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
        assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
        assertFalse("No nativeObjectClass in account",
                StringUtils.isEmpty(accountDef.getObjectClassDefinition().getNativeObjectClass()));

        ResourceAttributeDefinition<?> uidDef = accountDef.findAttributeDefinition(SchemaConstants.ICFS_UID);
        assertEquals(1, uidDef.getMaxOccurs());
        assertEquals(0, uidDef.getMinOccurs());
        assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
        assertFalse("UID has create", uidDef.canAdd());
        assertFalse("UID has update",uidDef.canModify());
        assertTrue("No UID read",uidDef.canRead());
        assertTrue("UID definition not in identifiers", accountDef.getPrimaryIdentifiers().contains(uidDef));

        ResourceAttributeDefinition<?> nameDef = accountDef.findAttributeDefinition(SchemaConstants.ICFS_NAME);
        assertEquals(1, nameDef.getMaxOccurs());
        assertEquals(1, nameDef.getMinOccurs());
        assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
        assertTrue("No NAME create", nameDef.canAdd());
        assertTrue("No NAME update",nameDef.canModify());
        assertTrue("No NAME read",nameDef.canRead());
        assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));

        ResourceAttributeDefinition<?> fullnameDef = accountDef.findAttributeDefinition(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME);
        assertNotNull("No definition for fullname", fullnameDef);
        assertEquals(1, fullnameDef.getMaxOccurs());
        assertEquals(1, fullnameDef.getMinOccurs());
        assertTrue("No fullname create", fullnameDef.canAdd());
        assertTrue("No fullname update", fullnameDef.canModify());
        assertTrue("No fullname read", fullnameDef.canRead());

        assertNull("The _PASSWORD_ attribute sneaked into schema", accountDef.findAttributeDefinition(new QName(SchemaTestConstants.NS_ICFS,"password")));

    }

    public DummyOrg addOrgTop()
            throws ConnectException, FileNotFoundException, ObjectAlreadyExistsException, SchemaViolationException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        return addOrg(ORG_TOP_NAME);
    }

    public DummyOrg addOrg(String name)
            throws ConflictException, FileNotFoundException, ObjectDoesNotExistException, SchemaViolationException,
            ObjectAlreadyExistsException, InterruptedException, ConnectException {
        DummyOrg org = new DummyOrg(name);
        dummyResource.addOrg(org);
        return org;
    }

    public DummyAccount addAccount(String userId)
            throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException, FileNotFoundException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        DummyAccount account = new DummyAccount(userId);
        dummyResource.addAccount(account);
        return account;
    }

    public DummyAccount addAccount(String userId, String fullName)
            throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException, FileNotFoundException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        DummyAccount account = new DummyAccount(userId);
        account.setEnabled(true);
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, fullName);
        dummyResource.addAccount(account);
        return account;
    }

    public void addAccount(String userId, String fullName, String location)
            throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException, FileNotFoundException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        assertExtendedSchema();
        DummyAccount account = new DummyAccount(userId);
        account.setEnabled(true);
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, fullName);
        account.addAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, location);
        dummyResource.addAccount(account);
    }

    public DummyGroup addGroup(String name)
            throws ObjectAlreadyExistsException, SchemaViolationException, ConnectException, FileNotFoundException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        assertExtendedSchema();
        DummyGroup group = new DummyGroup(name);
        group.setEnabled(true);
        dummyResource.addGroup(group);
        return group;
    }

    public void deleteAccount(String name) throws ConnectException, FileNotFoundException, ObjectDoesNotExistException, SchemaViolationException, ConflictException, InterruptedException {
        dummyResource.deleteAccountByName(name);
    }

    public QName getOrgObjectClassQName() {
        return new QName(NS_RI, OBJECTCLASS_ORG_LOCAL_PART);
    }

    public QName getAccountObjectClassQName() {
        return new QName(NS_RI, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
    }

    public DummyObjectClass getAccountObjectClass()
            throws ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        return dummyResource.getAccountObjectClass();
    }

    public DummyObjectClass getGroupObjectClass()
            throws ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        return dummyResource.getGroupObjectClass();
    }

    /**
     * Resets the blocking state, error simulation, etc.
     */
    public void reset() {
        dummyResource.setBreakMode(BreakMode.NONE);
        dummyResource.setBlockOperations(false);
        dummyResource.setSyncSearchHandlerStart(false);
        dummyResource.unblockAll();
    }

    public DummyAccountAsserter<Void> assertAccountByUsername(String username) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = dummyResource.getAccountByUsername(username);
        assertNotNull("Account "+username+" does not exist on dummy resource "+getName(), account);
        return assertAccount(account);
    }

    public DummyAccountAsserter<Void> assertAccountById(String id) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = dummyResource.getAccountById(id);
        assertNotNull("Account id="+id+" does not exist on dummy resource "+getName(), account);
        return assertAccount(account);
    }

    private DummyAccountAsserter<Void> assertAccount(DummyAccount account) {
        return new DummyAccountAsserter<>(account, getName());
    }

    public void assertNoAccountByUsername(String username) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = dummyResource.getAccountByUsername(username);
        assertNull("Unexpected account "+username+" on dummy resource "+getName(), account);
    }

    public DummyGroupAsserter<Void> assertGroupByName(String name) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        DummyGroup group = dummyResource.getGroupByName(name);
        assertNotNull("Group "+name+" does not exist on dummy resource "+getName(), group);
        return assertGroup(group);
    }

    private DummyGroupAsserter<Void> assertGroup(DummyGroup group) {
        return new DummyGroupAsserter<>(group, getName());
    }

    public DummyOrgAsserter<Void> assertOrgByName(String name)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        DummyOrg org = dummyResource.getOrgByName(name);
        assertNotNull("Org " + name + " does not exist on dummy resource " + getName(), org);
        return assertOrg(org);
    }

    private DummyOrgAsserter<Void> assertOrg(DummyOrg org) {
        return new DummyOrgAsserter<>(org, getName());
    }

    public void setSyncStyle(DummySyncStyle syncStyle) {
        dummyResource.setSyncStyle(syncStyle);
    }

    public <T> ResourceAttribute<T> createAccountAttribute(ItemName attrName) throws SchemaException, ConfigurationException {
        ResourceObjectDefinition accountDef = getRefinedAccountDefinition();
        //noinspection unchecked
        return (ResourceAttribute<T>) requireNonNull(accountDef.findAttributeDefinition(attrName),
                () -> "No attribute " + attrName + " found in " + accountDef)
                .instantiate();
    }

    public ResourceObjectDefinition getRefinedAccountDefinition() throws SchemaException, ConfigurationException {
        return getRefinedSchema().findDefaultDefinitionForKind(ShadowKindType.ACCOUNT);
    }

    public ResourceSchema getRefinedSchema() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory.getCompleteSchema(getResourceType());
    }
}
