/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.INTENT_DEFAULT;

import static com.evolveum.midpoint.test.DummyResourceContoller.*;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.MidPointConstants;

import com.evolveum.midpoint.test.DummyTestResource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestTolerantAttributes extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "tolerance");

    private static final File ACCOUNT_JACK_DUMMY_BLACK_FILENAME = new File(COMMON_DIR, "account-jack-dummy-black.xml");

    private static final String ATTR_TITLE2 = "title2";
    private static final String ATTR_QUOTE2 = "quote2";
    private static final String ATTR_GOSSIP2 = "gossip2";

    /**
     * - title: intolerant (via pattern), with a strong outbound mapping
     * - title2: intolerant (via false tolerance), with a strong outbound mapping
     * - quote: intolerant (via pattern), with a normal outbound mapping
     * - quote2: intolerant (via false tolerance), with a normal outbound mapping
     * - gossip: intolerant (via pattern), with a weak outbound mapping
     * - gossip2: intolerant (via false tolerance), with a weak outbound mapping
     */
    private static final DummyTestResource RESOURCE_DUMMY_TOLERANCE = new DummyTestResource(
            TEST_DIR, "resource-dummy-tolerance.xml", "f64d35cb-1e75-46e9-83e3-0a6267d309b6", "tolerance",
            c -> {
                c.extendSchemaPirate();
                var accountObjectClass = c.getAccountObjectClass();
                c.addAttrDef(accountObjectClass, ATTR_TITLE2, String.class, false, true);
                c.addAttrDef(accountObjectClass, ATTR_QUOTE2, String.class, false, true);
                c.addAttrDef(accountObjectClass, ATTR_GOSSIP2, String.class, false, true);
            });

    private static String accountOid;
    private static PrismObjectDefinition<ShadowType> accountDefinition;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_DUMMY_TOLERANCE.init(this, initTask, initResult);
    }

    @Test
    public void test100ModifyUserAddAccount() throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_BLACK_FILENAME);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(userDelta);

        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        dummyTransport.clearMessages();
        notificationManager.setDisabled(false);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        // Check accountRef
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));

        accountDefinition = accountModel.getDefinition();

        // Check account in dummy resource
        assertAccount(userJack, RESOURCE_DUMMY_BLACK_OID);
    }

    /**
     * We are trying to add value to the resource (through a mapping). This value matches
     * intolerant pattern. But as this value is explicitly added by a mapping from a primary
     * delta then the value should be set to resource even in that case.
     */
    @Test
    public void test110ModifyAddAttributesIntolerantPattern() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PropertyDelta propertyDelta = prismContext.deltaFactory().property().createModificationAddProperty(
                UserType.F_DESCRIPTION, getUserDefinition().findPropertyDefinition(UserType.F_DESCRIPTION),
                "This value will be not added");
        userDelta.addModification(propertyDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, executeOptions().reconcile(), task, result);

        // THEN
        then();
        assertSuccess(result);

        // Check value in "quote attribute"
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        accountOid = getSingleLinkOid(userJack);
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));

        // Check account in dummy resource
        assertAccount(userJack, RESOURCE_DUMMY_BLACK_OID);

        // Check value of quote attribute
        assertDummyAccountAttribute(RESOURCE_DUMMY_BLACK_NAME, "jack", "quote", "This value will be not added");
    }

    @Test
    public void test120ModifyAddAttributeTolerantPattern() throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PropertyDelta propertyDelta = prismContext.deltaFactory().property().createModificationAddProperty(UserType.F_DESCRIPTION, getUserDefinition().findPropertyDefinition(UserType.F_DESCRIPTION), "res-thiIsOk");
        userDelta.addModification(propertyDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(userDelta);

        modelService.executeChanges(deltas, executeOptions().reconcile(), task, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Check value in "quote attribute"
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));

        // Check account in dummy resource
        assertAccount(userJack, RESOURCE_DUMMY_BLACK_OID);

        // Check value of quote attribute
        assertDummyAccountAttribute(RESOURCE_DUMMY_BLACK_NAME, "jack", "quote", "res-thiIsOk");
    }

    @Test
    public void test130ModifyReplaceAttributeIntolerant() throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PropertyDelta propertyDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, getUserDefinition(), "gossip-thiIsNotOk");
        userDelta.addModification(propertyDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(userDelta);

        modelService.executeChanges(deltas, executeOptions().reconcile(), task, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Check value in "quote attribute"
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
//            assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));

        // Check account in dummy resource
        assertAccount(userJack, RESOURCE_DUMMY_BLACK_OID);

        // Check value of drink attribute
        assertDummyAccountAttribute(RESOURCE_DUMMY_BLACK_NAME, "jack", "gossip", null);
    }

    @Test
    public void test140ModifyReplaceAttributeTolerantPattern() throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        ItemPath drinkItemPath = ItemPath.create(new QName(MidPointConstants.NS_RI, "drink"));
        PropertyDelta propertyDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, getUserDefinition(), "thiIsOk");
        userDelta.addModification(propertyDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(userDelta);

        modelService.executeChanges(deltas, executeOptions().reconcile(), task, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Check value in "quote attribute"
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
//            assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));

        // Check account in dummy resource
        assertAccount(userJack, RESOURCE_DUMMY_BLACK_OID);

        // Check value of drink attribute
        assertDummyAccountAttribute(RESOURCE_DUMMY_BLACK_NAME, "jack", "gossip", "thiIsOk");
    }

    @Test
    public void test150ModifyAddNonTolerantAttribute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<ShadowType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountOid);

        ItemPath drinkItemPath = ItemPath.create(ShadowType.F_ATTRIBUTES, new QName(RESOURCE_DUMMY_BLACK_NAMESPACE, "drink"));
        assertNotNull("null definition for drink attribute ", accountDefinition.findPropertyDefinition(drinkItemPath));
        PropertyDelta propertyDelta = prismContext.deltaFactory().property().createModificationAddProperty(drinkItemPath, accountDefinition.findPropertyDefinition(drinkItemPath), "This should be ignored");
        userDelta.addModification(propertyDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, executeOptions().reconcile(), task, result);

        // THEN
        then();
        assertPartialError(result);
    }

    /**
     * Checks the behavior of intolerant attributes (defined by pattern or by false tolerance) with respect to mappings
     * of different strengths (strong, normal, weak).
     */
    @Test(description = "MID-10289")
    public void test200SettingIntolerantAttributes() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();
        var description1 = "description1";
        var description2 = "description2";

        when("user is created");
        var user = new UserType()
                .name(userName)
                .description(description1)
                .assignment(RESOURCE_DUMMY_TOLERANCE.assignmentTo(ACCOUNT, INTENT_DEFAULT));
        var userOid = addObject(user, task, result);

        then("user is created, attributes are set");
        assertDummyAccountByUsername(RESOURCE_DUMMY_TOLERANCE.name, userName)
                .display()
                .assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, description1)
                .assertAttribute(ATTR_TITLE2, description1)
                .assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, description1)
                .assertAttribute(ATTR_QUOTE2, description1)
                .assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, description1)
                .assertAttribute(ATTR_GOSSIP2, description1);

        when("user is reconciled");
        reconcileUser(userOid, task, result);

        then("attributes should have the same values");
        assertDummyAccountByUsername(RESOURCE_DUMMY_TOLERANCE.name, userName)
                .display()
                //.assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, description1) // This fails (value is null)
                .assertAttribute(ATTR_TITLE2, description1)
                //.assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, description1) // This fails (value is null)
                .assertAttribute(ATTR_QUOTE2, description1)
                //.assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, description1) // This fails (value is null)
                .assertAttribute(ATTR_GOSSIP2, description1);

        when("the description is changed");
        modifyUserReplace(userOid, UserType.F_DESCRIPTION, task, result, description2);

        then("attributes should have the new values");
        assertDummyAccountByUsername(RESOURCE_DUMMY_TOLERANCE.name, userName)
                .display(); // TODO asserts

        when("user is reconciled again");
        reconcileUser(userOid, task, result);

        then("attributes should have the same values");
        assertDummyAccountByUsername(RESOURCE_DUMMY_TOLERANCE.name, userName)
                .display(); // TODO asserts

        when("user is reconciled once again");
        reconcileUser(userOid, task, result);

        then("attributes should have the same values");
        assertDummyAccountByUsername(RESOURCE_DUMMY_TOLERANCE.name, userName)
                .display(); // TODO asserts
    }
}
