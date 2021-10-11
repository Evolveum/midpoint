/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
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

    public static final File TEST_DIR = new File("src/test/resources/tolerant");

    private static final String ACCOUNT_JACK_DUMMY_BLACK_FILENAME = "src/test/resources/common/account-jack-dummy-black.xml";

    private static String accountOid;
    private static PrismObjectDefinition<ShadowType> accountDefinition;

    @Test
    public void test100ModifyUserAddAccount() throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_BLACK_FILENAME));

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
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
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
    public void test101ModifyAddAttributesIntolerantPattern() throws Exception {
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
        modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);

        // THEN
        then();
        assertSuccess(result);

        // Check value in "quote attribute"
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        accountOid = getSingleLinkOid(userJack);
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
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
    public void test102modifyAddAttributeTolerantPattern() throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PropertyDelta propertyDelta = prismContext.deltaFactory().property().createModificationAddProperty(UserType.F_DESCRIPTION, getUserDefinition().findPropertyDefinition(UserType.F_DESCRIPTION), "res-thiIsOk");
        userDelta.addModification(propertyDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(userDelta);

        modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);

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
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
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
    public void test103modifyReplaceAttributeIntolerant() throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PropertyDelta propertyDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, getUserDefinition(), "gossip-thiIsNotOk");
        userDelta.addModification(propertyDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(userDelta);

        modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);

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
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
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
    public void test104modifyReplaceAttributeTolerantPattern() throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        ItemPath drinkItemPath = ItemPath.create(new QName(getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME).getNamespace(), "drink"));
        PropertyDelta propertyDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, getUserDefinition(), "thiIsOk");
        userDelta.addModification(propertyDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection) MiscUtil.createCollection(userDelta);

        modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);

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
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
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
    public void test105ModifyAddNonTolerantAttribute() throws Exception {
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
        modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);

        // THEN
        then();
        assertPartialError(result);
    }
}
