/*
 * Copyright (c) 2016-2019 mythoss, Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;
import java.util.Collection;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestNullAttribute extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "nullattribute");

    public static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
    public static final String OBJECT_TEMPLATE_USER_OID = "10000000-0000-0000-0000-000000002222";

    protected static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
    //see
    protected static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000001";

    private static final String DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME = "fullname";
    private static final String DUMMY_ACCOUNT_ATTRIBUTE_SHIP = "ship";
    private static final String DUMMY_ACCOUNT_ATTRIBUTE_WEAPON = "weapon";

    public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";

    public static final File ROLE_ACCOUNTONLY_FILE = new File(TEST_DIR, "role-accountonly.xml");
    public static final String ROLE_ACCOUNTONLY_OID = "10000000-0000-0000-0000-000000000601";

    public static final File ROLE_SHIPNWEAPON_FILE = new File(TEST_DIR, "role-shipnweapon.xml");
    public static final String ROLE_SHIPNWEAPON_OID = "10000000-0000-0000-0000-000000000602";

    public static final File USER_SMACK_FILE = new File(TEST_DIR, "user-smack.xml");
    public static final String USER_SMACK_OID = "c0c010c0-d34d-b33f-f00d-111111111112";

    protected static DummyResource dummyResource;
    protected static DummyResourceContoller dummyResourceCtl;
    protected PrismObject<ResourceType> resourceDummy;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Resources
        //default instance, no instance id
        //when id is set it is required to be present in resource.xml (I guess)
        dummyResourceCtl = DummyResourceContoller.create(null, resourceDummy);
        DummyObjectClass dummyAdAccountObjectClass = dummyResourceCtl.getDummyResource().getAccountObjectClass();

        //attributes
        dummyResourceCtl.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME, String.class, false, false);
        dummyResourceCtl.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_SHIP, String.class, false, false);
        dummyResourceCtl.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON, String.class, false, false);

        dummyResource = dummyResourceCtl.getDummyResource();
        resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILE,
                RESOURCE_DUMMY_OID, initTask, initResult);
        dummyResourceCtl.setResource(resourceDummy);
//        dummyResource.setSyncStyle(DummySyncStyle.SMART);

        //
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // Object Templates
        importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
        setDefaultUserTemplate(OBJECT_TEMPLATE_USER_OID);

        // Role
        importObjectFromFile(ROLE_ACCOUNTONLY_FILE, initResult);
        importObjectFromFile(ROLE_SHIPNWEAPON_FILE, initResult);

        PrismObject<RoleType> rolesw = getRole(ROLE_SHIPNWEAPON_OID);
        display("role shipNWeapon initial", rolesw);

        //User
        importObjectFromFile(USER_SMACK_FILE, initResult);

    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_OID, task);
        TestUtil.assertSuccess(testResult);

        PrismObjectDefinition<UserType> userDefinition = getUserDefinition();
        PrismContainerDefinition<?> userExtensionDef = userDefinition.getExtensionDefinition();
        display("User extension definition", userExtensionDef);
        PrismAsserts.assertPropertyDefinition(userExtensionDef,
                new QName(NS_PIRACY, "ship"), DOMUtil.XSD_STRING, 0, 1);

    }

    /**
     * assign role "account only"
     * role should be assigned and fullname should be set in resource account
     */
    @Test
    public void test010UserSmackAssignAccountOnlyRole() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        assignRole(USER_SMACK_OID, ROLE_ACCOUNTONLY_OID, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_SMACK_OID);
        //display("User jack after role account only assignment", user);

        assertAssignedRole(user, ROLE_ACCOUNTONLY_OID);
        assertNotAssignedRole(user, ROLE_SHIPNWEAPON_OID);

        String accountOid = getLinkRefOid(user, RESOURCE_DUMMY_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow smack after role account only assignment", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel jack after role account only assignment", accountModel);

        PrismAsserts.assertPropertyValue(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME), "Smack Sparrow");
        PrismAsserts.assertNoItem(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_SHIP));
        PrismAsserts.assertNoItem(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON));

    }

    /**
     * set extension/ship
     * role ShipNWeapon should be assigned (beacause of objecttemplate)
     * in resource account values for fullname, ship and weapon should exist
     */
    @Test
    public void test020UserSmackSetAttribute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        PrismObject<UserType> smack = getUser(USER_SMACK_OID);
        display("smack initial: " + smack.debugDump());

        // WHEN
        @SuppressWarnings("unchecked, raw")
        Collection<ObjectDelta<? extends ObjectType>> deltas =
                (Collection) prismContext.deltaFor(UserType.class)
                        .item(UserType.F_EXTENSION, new QName(NS_PIRACY, "ship")).add("Black Pearl")
                        .asObjectDeltas(USER_SMACK_OID);
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_SMACK_OID);
        display("User smack after setting attribute piracy:ship", user);

        assertAssignedRole(user, ROLE_ACCOUNTONLY_OID);
        assertAssignedRole(user, ROLE_SHIPNWEAPON_OID);

        String accountOid = getLinkRefOid(user, RESOURCE_DUMMY_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow smack after role shipnweapon assignment", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel jack after role shipnweapon assignment", accountModel);

        PrismAsserts.assertPropertyValue(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME), "Smack Sparrow");
        PrismAsserts.assertPropertyValue(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_SHIP), "Black Pearl");
        // weapon is not in user's extension (MID-3326)
        //PrismAsserts.assertPropertyValue(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON),"pistol");

    }

    /**
     * remove extension/ship
     * role ShipNWeapon should be unassigned (beacause of objecttemplate)
     * in resource account only value for fullname should still exist, ship and weapon should have been removed
     * MID-3325
     */
    @Test // MID-3325
    public void test030UserSmackRemoveAttribute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        //TODO: best way to set extension properties?
        PrismObject<UserType> userBefore = getUser(USER_SMACK_OID);
        display("User before", userBefore);
        PrismObject<UserType> userNewPrism = userBefore.clone();
        prismContext.adopt(userNewPrism);
        if (userNewPrism.getExtension() == null) { userNewPrism.createExtension(); }
        PrismContainer<?> ext = userNewPrism.getExtension();
        ext.setPropertyRealValue(PIRACY_SHIP_QNAME, null);

        ObjectDelta<UserType> delta = userBefore.diff(userNewPrism);
        display("Modifying user with delta", delta);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_SMACK_OID);
        display("User smack after deleting attribute piracy:ship", userAfter);

        assertAssignedRole(userAfter, ROLE_ACCOUNTONLY_OID);
        assertNotAssignedRole(userAfter, ROLE_SHIPNWEAPON_OID);

        String accountOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("accountShadow smack after attribute deletion", accountShadow);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("accountModel jack after attribute deletion", accountModel);

        PrismAsserts.assertPropertyValue(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME), "Smack Sparrow");
        PrismAsserts.assertNoItem(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON));
        PrismAsserts.assertNoItem(accountModel, dummyResourceCtl.getAttributePath(DUMMY_ACCOUNT_ATTRIBUTE_SHIP));

    }

}
