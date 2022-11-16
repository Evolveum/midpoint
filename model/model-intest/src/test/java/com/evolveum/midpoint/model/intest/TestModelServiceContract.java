/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static com.evolveum.midpoint.schema.processor.ResourceSchemaTestUtil.findObjectTypeDefinitionRequired;
import static com.evolveum.midpoint.test.DummyResourceContoller.*;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.Format.TEXT;
import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.SortBy.TIME;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;

import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.CompiledTracingProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.OperationsPerformanceInformationUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestModelServiceContract extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/contract");

    private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
    private static final String USER_BLACKBEARD_OID = "c0c010c0-d34d-b33f-f00d-161161116666";

    private static String accountJackOid;
    private static String accountJackBlueOid;
    private static String userCharlesOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);
        InternalMonitor.reset();
        InternalMonitor.setTrace(InternalCounters.PRISM_OBJECT_CLONE_COUNT, true);

//        setGlobalTracingOverride(addNotificationsLogging(createModelLoggingTracingProfile()));
    }

    @Test
    public void test050GetUserJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        when();
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);

        then();
        display("User jack", userJack);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertUserJack(userJack);

        assertSuccess(result);

        assertSteadyResources();
    }

    @Test
    public void test051GetUserBarbossa() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        when();
        PrismObject<UserType> userBarbossa = modelService.getObject(UserType.class, USER_BARBOSSA_OID, null, task, result);

        then();
        display("User barbossa", userBarbossa);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertUser(userBarbossa, USER_BARBOSSA_OID, "barbossa", "Hector Barbossa", "Hector", "Barbossa");

        assertSuccess(result);

        userBarbossa.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);

        assertSteadyResources();
    }

    @Test
    public void test099ModifyUserAddAccountFailing() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);
        // the following modifies nothing but it is used to produce a user-level notification (LINK_REF by itself causes no such notification)
        PropertyDelta<String> attributeDelta = prismContext.deltaFactory().property().createReplaceDeltaOrEmptyDelta(getUserDefinition(), UserType.F_TELEPHONE_NUMBER, "555-1234");
        userDelta.addModification(attributeDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        getDummyResource().setAddBreakMode(BreakMode.UNSUPPORTED);       // hopefully this does not kick consistency mechanism

        try {
            when();
            modelService.executeChanges(deltas, null, task, result);

            assertNotReached();

        } catch (UnsupportedOperationException e) {
            then();
            displayExpectedException(e);
        }

        assertFailure(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        // Check accountRef
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());

        notificationManager.setDisabled(true);
        getDummyResource().resetBreakMode();

        // Check notifications
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);        // actually I don't know why provisioning does not report unsupported operation as a failure...
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
        checkDummyTransportMessages("simpleUserNotifier-FAILURE", 0); // This should be called, but it is not implemented now

        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0); // MID-4779
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test100ModifyUserAddAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        getDummyResource().resetBreakMode();

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_FILE, task, result);

        then();
        assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        // Check accountRef
        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User jack after", userAfter);
        assertUserJack(userAfter);
        UserType userJackType = userAfter.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountJackOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountJackOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountJackOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        /*
        TODO for new repo - why does it complain about raw when there is raw above?
        java.lang.IllegalStateException: Raw value PPV([raw], raw element: XNode(primitive:parser ValueParser(DOM-less, Caribbean, namespace declarations)))
         in item PP({.../resource/instance/10000000-0000-0000-0000-000000000004}location):[PPV([raw], raw element: XNode(primitive:parser ValueParser(DOM-less, Caribbean, namespace declarations)))]
          (attributes/location in shadow:3a727816-70f7-4301-98ab-c1a0020d1ea2(jack))
         */
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDefaultDummyAccount("jack", "Jack Sparrow", true);
        displayDumpable("dummyAccount after", dummyAccount);

        assertDummyScriptsAdd(userAfter, accountModel, getDummyResourceType());

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, add account, link
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        notificationManager.setDisabled(true);

        // Check notifications
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        List<Message> messages = dummyTransport.getMessages("dummy:accountPasswordNotifier");
        Message message = messages.get(0); // number of messages was already checked
        assertEquals("Invalid list of recipients", Collections.singletonList("recipient@evolveum.com"), message.getTo());
        assertTrue("No account name in account password notification", message.getBody().contains("Password for account jack on Dummy Resource is:"));

        assertSteadyResources();
    }

    @Test
    public void test101GetAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // Let's do some evil things. Like changing some of the attribute on a resource and see if they will be
        // fetched after get.
        // Also set a value for ignored "water" attribute. The system should cope with that.
        DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        jackDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "The best pirate captain ever");
        jackDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, "cold");
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        when();
        PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);

        then();
        display("Account", account);
        displayDumpable("Account def", account.getDefinition());
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
        displayDumpable("Account attributes def", accountContainer.getDefinition());
        displayDumpable("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
        assertDummyAccountShadowModel(account, accountJackOid, "jack", "Jack Sparrow");

        assertSuccess("getObject result", result);

        account.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);

        IntegrationTestTools.assertAttribute(account,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
                "The best pirate captain ever");
        // This one should still be here, even if ignored
        IntegrationTestTools.assertAttribute(account,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME),
                "cold");

        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(account);
        assertNotNull("No attribute container from " + account, attributesContainer);
        Collection<ResourceAttribute<?>> identifiers = attributesContainer.getPrimaryIdentifiers();
        assertNotNull("No identifiers (null) in attributes container in " + accountJackOid, identifiers);
        assertFalse("No identifiers (empty) in attributes container in " + accountJackOid, identifiers.isEmpty());

        ResourceAttribute<String> fullNameAttr = attributesContainer.findAttribute(dummyResourceCtl.getAttributeFullnameQName());
        PrismAsserts.assertPropertyValue(fullNameAttr, ACCOUNT_JACK_DUMMY_FULLNAME);
        ResourceAttributeDefinition<String> fullNameAttrDef = fullNameAttr.getDefinition();
        displayDumpable("attribute fullname definition", fullNameAttrDef);
        PrismAsserts.assertDefinition(fullNameAttrDef, dummyResourceCtl.getAttributeFullnameQName(),
                DOMUtil.XSD_STRING, 1, 1);
        // MID-3144
        if (fullNameAttrDef.getDisplayOrder() == null || fullNameAttrDef.getDisplayOrder() < 100 || fullNameAttrDef.getDisplayOrder() > 400) {
            AssertJUnit.fail("Wrong fullname displayOrder: " + fullNameAttrDef.getDisplayOrder());
        }
        assertEquals("Wrong fullname displayName", "Full Name", fullNameAttrDef.getDisplayName());

        assertSteadyResources();
    }

    @Test
    public void test102GetAccountNoFetch() throws Exception {

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());

        when();
        PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountJackOid, options, task, result);

        display("Account", account);
        displayDumpable("Account def", account.getDefinition());
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
        displayDumpable("Account attributes def", accountContainer.getDefinition());
        displayDumpable("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
        assertDummyAccountShadowRepo(account, accountJackOid, "jack");

        assertSuccess("getObject result", result);

        assertSteadyResources();
    }

    /**
     * MID-6716
     */
    @Test
    public void test103GetAccountRaw() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult parentResult = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder().raw().build();

        when();

        // Just to avoid result pruning.
        CompiledTracingProfile tracingProfile = tracer.compileProfile(
                new TracingProfileType(prismContext)
                        .createTraceFile(false), parentResult);
        OperationResult result = parentResult.subresult("get")
                .tracingProfile(tracingProfile)
                .build();

        PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountJackOid, options, task, result);

        then();

        display("Account", account);
        displayDumpable("Account def", account.getDefinition());
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
        displayDumpable("Account attributes def", accountContainer.getDefinition());
        displayDumpable("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());
        assertDummyAccountShadowRepo(account, accountJackOid, "jack");

        assertSuccess("getObject result", result);

        assertSteadyResources();

        displayDumpable("result", result);

        List<String> provisioningOperations = result.getResultStream()
                .map(OperationResult::getOperation)
                .filter(operation -> operation.startsWith(ProvisioningService.class.getName()))
                .collect(Collectors.toList());
        assertThat(provisioningOperations).as("provisioning operations").isEmpty();
    }

    @Test
    public void test105SearchAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // get weapon attribute definition
        PrismObject<ResourceType> dummyResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
        ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchema(dummyResource);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);

        QName accountObjectClassQName = dummyResourceCtl.getAccountObjectClassQName();
        ResourceObjectClassDefinition accountObjectClassDefinition =
                resourceSchema.findObjectClassDefinition(accountObjectClassQName);
        QName weaponQName = dummyResourceCtl.getAttributeWeaponQName();
        ResourceAttributeDefinition<?> weaponDefinition = accountObjectClassDefinition.findAttributeDefinition(weaponQName);

        ObjectQuery q = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(accountObjectClassQName)
                .and().item(ItemPath.create(ShadowType.F_ATTRIBUTES, weaponQName), weaponDefinition).eq("rum")
                .build();

        when();
        List<PrismObject<ShadowType>> list = modelService.searchObjects(ShadowType.class, q, null, task, result);

        then();
        display("Accounts", list);
        assertEquals("Wrong # of objects returned", 1, list.size());
    }

    @Test
    public void test106SearchAccountWithoutResourceSchema() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // create weapon attribute definition - NOT SUPPORTED, use only when you know what you're doing!
        QName accountObjectClassQName = dummyResourceCtl.getAccountObjectClassQName();
        QName weaponQName = dummyResourceCtl.getAttributeWeaponQName();
        PrismPropertyDefinition<String> weaponFakeDef = prismContext.definitionFactory().createPropertyDefinition(weaponQName, DOMUtil.XSD_STRING);

        ObjectQuery q = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(accountObjectClassQName)
                .and().item(ItemPath.create(ShadowType.F_ATTRIBUTES, weaponQName), weaponFakeDef).eq("rum")
                .build();

        when();
        List<PrismObject<ShadowType>> list = modelService.searchObjects(ShadowType.class, q, null, task, result);

        then();
        display("Accounts", list);
        assertEquals("Wrong # of objects returned", 1, list.size());
    }

    @Test
    public void test108ModifyUserAddAccountAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        try {

            when();
            modelService.executeChanges(deltas, null, task, result);

            then();
            assert false : "Expected executeChanges operation to fail but it has obviously succeeded";
        } catch (SchemaException e) {
            // This is expected
            e.printStackTrace();
            then();
            String message = e.getMessage();
            assertMessageContains(message, "already contains account");
            assertMessageContains(message, "default");
        }

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);

        assertSteadyResources();
    }

    @Test
    public void test109ModifyUserAddAccountAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(null);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        try {

            when();
            modelService.executeChanges(deltas, null, task, result);

            then();
            assert false : "Expected executeChanges operation to fail but it has obviously succeeded";
        } catch (SchemaException e) {
            // This is expected
            then();
            String message = e.getMessage();
            assertMessageContains(message, "already contains account");
            assertMessageContains(message, "default");
        }

        assertNoProvisioningScripts();

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);

        assertSteadyResources();
    }

    @Test
    public void test110GetUserResolveAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .item(UserType.F_LINK_REF).resolve()
                .build();

        when();
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options, task, result);

        then();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNotNull("Missing account object in accountRefValue", accountRefValue.getObject());

        ShadowType shadow = (ShadowType) accountRefValue.getObject().asObjectable();
        assertDummyAccountShadowModel(shadow.asPrismObject(), accountOid, "jack", "Jack Sparrow");

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);

        assertSteadyResources();
    }

    @Test
    public void test111GetUserResolveAccountResource() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .item(UserType.F_LINK_REF).resolve()
                .item(UserType.F_LINK_REF, ShadowType.F_RESOURCE_REF).resolve()
                .build();

        when();
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options, task, result);

        then();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNotNull("Missing account object in accountRefValue", accountRefValue.getObject());

        ShadowType shadow = (ShadowType) accountRefValue.getObject().asObjectable();
        assertDummyAccountShadowModel(shadow.asPrismObject(), accountOid, "jack", "Jack Sparrow");

        assertNotNull("Resource in account was not resolved", shadow.getResourceRef().asReferenceValue().getObject());

        assertSuccess("getObject result", result);

        userJack.checkConsistence(true, true);

        assertSteadyResources();
    }

    @Test
    public void test112GetUserResolveAccountNoFetch() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        GetOperationOptions getOpts = new GetOperationOptions();
        getOpts.setResolve(true);
        getOpts.setNoFetch(true);
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(prismContext.toUniformPath(UserType.F_LINK_REF), getOpts);

        when();
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, options, task, result);

        then();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNotNull("Missing account object in accountRefValue", accountRefValue.getObject());

        ShadowType shadow = (ShadowType) accountRefValue.getObject().asObjectable();
        assertDummyAccountShadowRepo(shadow.asPrismObject(), accountOid, "jack");

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);

        userJack.checkConsistence(true, true);

        assertSteadyResources();
    }

    @Test
    public void test119ModifyUserDeleteAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(accountJackOid);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result, 2);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        // Check accountRef
        PrismObject<UserType> userJack =
                modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of linkRefs", 0, userJackType.getLinkRef().size());

        // Check is shadow is gone
        try {
            repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
            AssertJUnit.fail("Shadow " + accountJackOid + " still exists");
        } catch (ObjectNotFoundException e) {
            // This is OK
        }

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, delete account, unlink
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    @Test
    public void test120AddAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        ObjectDelta<ShadowType> accountDelta = DeltaFactory.Object.createAddDelta(account);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges =
                executeChanges(accountDelta, null, task, result);

        then();
        assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        accountJackOid = ObjectDeltaOperation.findProjectionDeltaOidInCollection(executeChanges);
        assertNotNull("No account OID in executed deltas", accountJackOid);
        // Check accountRef (should be none)
        PrismObject<UserType> userJack =
                modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(
                ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(
                ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        // The user is not associated with the account
        assertDummyScriptsAdd(null, accountModel, getDummyResourceType());

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        // We cannot have OID in the request. The OID is not assigned yet at that stage.
        dummyAuditService.assertTarget(accountShadow.getOid(), AuditEventStage.EXECUTION);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);          // there's no password for that account
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);               // account has no owner
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    /**
     * Linking existing account.
     */
    @Test
    public void test121ModifyUserAddAccountRef() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        ReferenceDelta accountDelta =
                prismContext.deltaFactory().reference().createModificationAdd(
                        UserType.F_LINK_REF, getUserDefinition(), accountJackOid);
        userDelta.addModification(accountDelta);

        when();
        executeChanges(userDelta, null, task, result);

        then();
        assertSuccess(result);

        // There is strong mapping. Complete account is fetched.
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(
                ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, modify account, link
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    @Test
    public void test128ModifyUserDeleteAccountRef() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountJackOid);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
        // Check accountRef
        assertUser(userJack, "after")
                .display()
                .assertLiveLinks(0)
                .assertRelatedLinks(0); // The link was deleted.

        // Check shadow (if it is unchanged)
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account (if it is unchanged)
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource (if it is unchanged)
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    @Test
    public void test129DeleteAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountJackOid);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1); // delete shadow
        String shadowOid = dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class).getOid();
        assertEquals("Wrong shadow delete OID", accountJackOid, shadowOid);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);           // there's no link user->account (removed in test128)
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    @Test
    public void test130PreviewModifyUserJackAssignAccount() {
        given();
        try {
            Task task = getTestTask();
            OperationResult result = task.getResult();
            preTestCleanup(AssignmentPolicyEnforcementType.FULL);

            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
            ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
            deltas.add(accountAssignmentUserDelta);

            when();
            modelInteractionService.previewChanges(deltas, executeOptions(), task, result);

            then();
            result.computeStatus();
            TestUtil.assertSuccess("previewChanges result", result);
            assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

            PrismObject<UserType> userJack = getUser(USER_JACK_OID);
            display("User after change execution", userJack);
            assertUserJack(userJack);
            // Check accountRef
            assertUserNoAccountRefs(userJack);

            // TODO: assert context
            // TODO: assert context
            // TODO: assert context

            // We (temporarily?) turned off resource ref resolution during preview operation.
            // It complicated things regarding objects immutability a bit. Let the clients resolve these references themselves.

            //assertResolvedResourceRefs(modelContext);

            // Check account in dummy resource
            assertNoDummyAccount("jack");

            dummyAuditService.assertNoRecord();
        } catch (Exception ex) {
            logger.info("Exception {}", ex.getMessage(), ex);
        }

        assertSteadyResources();
    }

    @Test
    public void test131ModifyUserJackAssignAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        then();
        assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 0, 50);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        assertUserJack(userAfter);
        AssignmentType assignmentType = assertAssignedAccount(userAfter, RESOURCE_DUMMY_OID);
        assertAssignments(userAfter, 1);
        assertModifyMetadata(userAfter, startTime, endTime);
        assertCreateMetadata(assignmentType, startTime, endTime);
        assertLastProvisioningTimestamp(userAfter, startTime, endTime);

        accountJackOid = getSingleLinkOid(userAfter);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyScriptsAdd(userAfter, accountModel, getDummyResourceType());

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertCustomColumn("foo", "test");
        dummyAuditService.assertCustomColumn("ship", "Black Pearl");
//        dummyAuditService.assertResourceOid(RESOURCE_DUMMY_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    /**
     * Modify the account. Some of the changes should be reflected back to the user by inbound mapping.
     */
    @Test
    public void test132ModifyAccountJackDummy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                accountJackOid, dummyResourceCtl.getAttributeFullnamePath(), "Cpt. Jack Sparrow");
        accountDelta.addModificationReplaceProperty(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH, "Queen Anne's Revenge");

        when();
        executeChanges(accountDelta, null, task, result);

        then();
        assertSuccess(result);
        // There is strong mapping. Complete account is fetched.
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        // Fullname inbound mapping is not used because it is weak
        assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
        // ship inbound mapping is used, it is strong
        assertEquals("Wrong user locality (orig)", "The crew of Queen Anne's Revenge",
                userJack.asObjectable().getOrganizationalUnit().iterator().next().getOrig());
        assertEquals("Wrong user locality (norm)", "the crew of queen annes revenge",
                userJack.asObjectable().getOrganizationalUnit().iterator().next().getNorm());
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        // All the changes should be reflected to the account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Cpt. Jack Sparrow");
        PrismAsserts.assertPropertyValue(accountModel, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH, "Queen Anne's Revenge");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
                "Queen Anne's Revenge");

        assertDummyScriptsModify(userJack);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertAnyRequestDeltas();

        dummyAuditService.assertExecutionDeltas(0, 2); // lastProvisioningTimestamp, modify account
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
                dummyResourceCtl.getAttributeFullnamePath(), "Jack Sparrow");
//        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
//                dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));

        dummyAuditService.assertExecutionDeltas(1, 1);
        dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    /**
     * MID-3080
     */
    @Test
    public void test135ModifyUserJackAssignAccountAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        then();
        assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertAssignedAccount(userJack, RESOURCE_DUMMY_OID);
        assertAssignments(userJack, 1);

        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Cpt. Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Cpt. Jack Sparrow", true);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
                "Queen Anne's Revenge");

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1); // operation changes the metadata (but idempotent from the business point of view) -- MID-6370
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);

        // MID-6370
//        checkDummyTransportMessages("simpleUserNotifier", 0);           // op is idempotent
//        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    @Test
    public void test136JackRecomputeNoChange() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        assertUserJack(userAfter);
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_OID);
        assertAssignments(userAfter, 1);
        assertLastProvisioningTimestamp(userAfter, null, startTime);

        String accountJackOidAfter = getSingleLinkOid(userAfter);
        assertEquals("Account OID changed", accountJackOid, accountJackOidAfter);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Cpt. Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Cpt. Jack Sparrow", true);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
                "Queen Anne's Revenge");

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(0);
        dummyAuditService.assertSimpleRecordSanity();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    @Test
    public void test139ModifyUserJackUnassignAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    /**
     * Assignment enforcement is set to POSITIVE for this test. The account should be added.
     */
    @Test
    public void test141ModifyUserJackAssignAccountPositiveEnforcement() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // Let's break the delta a bit. Projector should handle this anyway
        breakAssignmentDelta(deltas);

        // This is a second time we assigned this account. Therefore all the scripts in mapping should already
        // be compiled ... check this.
        rememberCounter(InternalCounters.SCRIPT_COMPILE_COUNT);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        // This is a second time we assigned this account. Therefore all the scripts in mapping should already
        // be compiled ... check this.
        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);

        assertSteadyResources();
    }

    /**
     * Assignment enforcement is set to POSITIVE for this test. The account should remain as it is.
     */
    @Test
    public void test148ModifyUserJackUnassignAccountPositiveEnforcement() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

        // Let's break the delta a bit. Projector should handle this anyway
        breakAssignmentDelta(deltas);

        //change resource assigment policy to be positive..if they are not applied by projector, the test will fail
        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, false);
        // the previous command changes resource, therefore let's explicitly re-read it before test
        // to refresh the cache and not affect the performance results (monitor).
        modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);

        assertSteadyResources();

        when();
        executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);
        // There is strong mapping. Complete account is fetched.
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertLinked(userJack, accountJackOid);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertNoProvisioningScripts();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0); // MID-4779
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertSteadyResources();

        // return resource to the previous state..delete assignment enforcement to prevent next test to fail..
        deleteResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, false);
        // the previous command changes resource, therefore let's explicitly re-read it before test
        // to refresh the cache and not affect the performance results (monitor).
        modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
        assertSteadyResources();
    }

    /**
     * Assignment enforcement is set to POSITIVE for this test as it was for the previous test.
     * Now we will explicitly delete the account.
     */
    @Test
    public void test149ModifyUserJackDeleteAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setOid(accountJackOid);
        ReferenceDelta accountRefDelta = prismContext.deltaFactory().reference().createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountJackOid);
        userDelta.addModification(accountRefDelta);

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountJackOid);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, delete account, unlink
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0); // MID-4779
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertSteadyResources();
    }

    /**
     * Assignment enforcement is set to RELATTIVE for this test. The account should be added.
     */
    @Test
    public void test151ModifyUserJackAssignAccountRelativeEnforcement() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // Let's break the delta a bit. Projector should handle this anyway
        breakAssignmentDelta(deltas);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);
        assertResultSerialization(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    /**
     * Assignment enforcement is set to RELATIVE for this test. The account should be gone.
     */
    @Test
    public void test158ModifyUserJackUnassignAccountRelativeEnforcement() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    /**
     * Assignment enforcement is set to NONE for this test.
     */
    @Test
    public void test161ModifyUserJackAssignAccountNoneEnforcement() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.NONE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // Let's break the delta a bit. Projector should handle this anyway
        breakAssignmentDelta(deltas);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyProvisioningScriptsNone();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test163ModifyUserJackAddAccountNoneEnforcement() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(userDelta);

        dummyAuditService.clear();
        dummyTransport.clearMessages();
        notificationManager.setDisabled(false);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, add account, link
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        notificationManager.setDisabled(true);

        // Check notifications
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test164ModifyUserJackUnassignAccountNoneEnforcement() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.NONE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyProvisioningScriptsNone();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        notificationManager.setDisabled(true);

        // Check notifications
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test169ModifyUserJackDeleteAccountNoneEnforcement() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.NONE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setOid(accountJackOid);
        ReferenceDelta accountRefDelta = prismContext.deltaFactory().reference().createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountJackOid);
        userDelta.addModification(accountRefDelta);

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountJackOid);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3); // lastProvisioningTimestamp, delete account, unlink
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test180ModifyUserAddAccountFullEnforcement() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(userDelta);

        try {

            when();
            modelService.executeChanges(deltas, null, task, result);

            AssertJUnit.fail("Unexpected executeChanges success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        then();
        result.computeStatus();
        TestUtil.assertFailure("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check that shadow was not created
        assertNoShadow(accountJackOid);

        // Check that dummy resource account was not created
        assertNoDummyAccount("jack");

        assertNoProvisioningScripts();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0, 0);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
        dummyAuditService.assertTarget(USER_JACK_OID);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test182ModifyUserAddAndAssignAccountPositiveEnforcement() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(userDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        // Check accountRef
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountJackOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountJackOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountJackOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    /**
     * Assignment enforcement is set to POSITIVE for this test as it was for the previous test.
     * Now we will explicitly delete the account.
     */
    @Test
    public void test189ModifyUserJackUnassignAndDeleteAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        // Explicit unlink is not needed here, it should work without it

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountJackOid);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    /**
     * We try to both assign an account and modify that account in one operation.
     * Some changes should be reflected to account (e.g.  weapon) as the mapping is weak,
     * other should be overridden (e.g. fullname) as the mapping is strong.
     */
    @Test
    public void test190ModifyUserJackAssignAccountAndModify() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        ShadowCoordinatesQualifiedObjectDelta<ShadowType> accountDelta =
                RefineryObjectFactory.createShadowDiscriminatorModificationReplaceProperty(
                        ShadowType.class,
                        RESOURCE_DUMMY_OID,
                        ShadowKindType.ACCOUNT,
                        SchemaConstants.INTENT_DEFAULT,
                        dummyResourceCtl.getAttributeFullnamePath(),
                        prismContext,
                        "Cpt. Jack Sparrow");
        accountDelta.addModificationAddProperty(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_PATH, "smell");
        deltas.add(accountDelta);
        deltas.add(accountAssignmentUserDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, USER_JACK_USERNAME);
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, USER_JACK_USERNAME, "Cpt. Jack Sparrow");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
        DummyAccount dummyAccount = getDummyAccount(null, USER_JACK_USERNAME);
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "smell");
        assertNull("Unexpected loot", dummyAccount.getAttributeValue("loot", Integer.class));

        assertDummyScriptsAdd(userJack, accountModel, getDummyResourceType());

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    /**
     * We try to modify an assignment of the account and see whether changes will be recorded in the account itself.
     *
     * We also check the metadata.channel migration for both the object and the assignment (MID-6547).
     */
    @Test
    public void test191ModifyUserJackModifyAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

        PrismObject<ResourceType> dummyResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);

        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(dummyResource);
        // This explicitly parses the schema, therefore ...
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);

        ResourceObjectTypeDefinition accountDefinition =
                findObjectTypeDefinitionRequired(refinedSchema, ShadowKindType.ACCOUNT, null);
        PrismPropertyDefinition gossipDefinition = accountDefinition.findPropertyDefinition(new ItemName(
                "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3",
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME));
        assertNotNull("gossip attribute definition not found", gossipDefinition);

        ConstructionType accountConstruction = createAccountConstruction(RESOURCE_DUMMY_OID, null);
        ResourceAttributeDefinitionType radt = new ResourceAttributeDefinitionType();
        radt.setRef(new ItemPathType(gossipDefinition.getItemName()));
        MappingType outbound = new MappingType();
        radt.setOutbound(outbound);

        ExpressionType expression = new ExpressionType();
        outbound.setExpression(expression);

        MappingType value = new MappingType();

        //noinspection unchecked
        PrismProperty<String> property = gossipDefinition.instantiate();
        property.addRealValue("q");

        List evaluators = expression.getExpressionEvaluator();
        Collection<JAXBElement<RawType>> collection = StaticExpressionUtil.serializeValueElements(property);
        ObjectFactory of = new ObjectFactory();
        for (JAXBElement<RawType> obj : collection) {
            //noinspection unchecked
            evaluators.add(of.createValue(obj.getValue()));
        }

        value.setExpression(expression);
        radt.setOutbound(value);
        accountConstruction.getAttribute().add(radt);

        PrismObject<UserType> jackBefore = getUserFromRepo(USER_JACK_OID);
        assertEquals("Wrong # of assignments", 1, jackBefore.asObjectable().getAssignment().size());
        Long assignmentId = jackBefore.asObjectable().getAssignment().get(0).getId();
        ObjectDelta<UserType> accountAssignmentUserDelta =
                createReplaceAccountConstructionUserDelta(USER_JACK_OID, assignmentId, accountConstruction);
        deltas.add(accountAssignmentUserDelta);

        // Set user and assignment create channel to legacy value.
        repositoryService.modifyObject(
                UserType.class, jackBefore.getOid(),
                deltaFor(UserType.class)
                        .item(UserType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
                        .replace(Channel.USER.getLegacyUri())
                        .item(UserType.F_ASSIGNMENT, assignmentId, AssignmentType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
                        .replace(Channel.USER.getLegacyUri())
                        .asItemDeltas(),
                result);

        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<UserType> userJackOld = getUser(USER_JACK_OID);
        display("User before change execution", userJackOld);
        display("Deltas to execute execution", deltas);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        // First fetch: initial account read
        // Second fetch: fetchback after modification to correctly process inbound
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 2);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");
        accountJackOid = getSingleLinkOid(userJack);

        // MID-6547 (channel URI migration)
        assertThat(userJack.asObjectable().getMetadata().getCreateChannel()).isEqualTo(Channel.USER.getUri());
        assertThat(userJack.asObjectable().getAssignment().get(0).getMetadata().getCreateChannel()).isEqualTo(Channel.USER.getUri());

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, USER_JACK_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, USER_JACK_USERNAME, "Cpt. Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
        DummyAccount dummyAccount = getDummyAccount(null, USER_JACK_USERNAME);
        display(dummyAccount.debugDump());
        assertDummyAccountAttribute(null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "q");

        assertDummyScriptsModify(userJack, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        Collection<ObjectDeltaOperation<? extends ObjectType>> auditExecutionDeltas = dummyAuditService.getExecutionDeltas();
        assertEquals("Wrong number of execution deltas", 2, auditExecutionDeltas.size());
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test192ModifyUserJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
                PrismTestUtil.createPolyString("Magnificent Captain Jack Sparrow"));

        then();
        assertSuccess(result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, "Magnificent Captain Jack Sparrow");
        accountJackOid = getSingleLinkOid(userAfter);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Magnificent Captain Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);

        assertDummyScriptsModify(userAfter);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);

        dummyAuditService.assertOldValue(ChangeType.MODIFY, UserType.class,
                UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Jack Sparrow"));
        // We have full account here. It is loaded because of strong mapping.
        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
                dummyResourceCtl.getAttributeFullnamePath(), "Cpt. Jack Sparrow");

        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test193ModifyUserJackLocationEmpty() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result /* no value */);

        then();
        assertSuccess(result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, "Magnificent Captain Jack Sparrow", "Jack", "Sparrow", null);
        accountJackOid = getSingleLinkOid(userAfter);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Magnificent Captain Jack Sparrow");
        IntegrationTestTools.assertNoAttribute(accountModel, dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);

        assertDummyScriptsModify(userAfter);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertPropertyReplace(ChangeType.MODIFY, UserType.class, UserType.F_LOCALITY /* no values */);
        // MID-4020
        dummyAuditService.assertOldValue(ChangeType.MODIFY, UserType.class, UserType.F_LOCALITY, createPolyString(USER_JACK_LOCALITY));
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test194ModifyUserJackLocationNull() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        try {
            when();
            modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, (PolyString) null);

            AssertJUnit.fail("Unexpected success");
        } catch (IllegalStateException e) {
            // This is expected
        }
        then();
        result.computeStatus();
        TestUtil.assertFailure(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        assertNoProvisioningScripts();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        // This should fail even before the request record is created
        dummyAuditService.assertRecords(0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test195ModifyUserJackLocationSea() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, createPolyString("sea"));

        then();
        assertSuccess(result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, "Magnificent Captain Jack Sparrow", USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME, "sea");
        accountJackOid = getSingleLinkOid(userAfter);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Magnificent Captain Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);

        assertDummyScriptsModify(userAfter);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertPropertyReplace(ChangeType.MODIFY, UserType.class, UserType.F_LOCALITY, createPolyString("sea"));
        // MID-4020
        dummyAuditService.assertOldValue(ChangeType.MODIFY, UserType.class, UserType.F_LOCALITY /* no values */);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test198ModifyUserJackRaw() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_FULL_NAME,
                PrismTestUtil.createPolyString("Marvelous Captain Jack Sparrow"));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(objectDelta);

        when();
        modelService.executeChanges(deltas, executeOptions().raw(), task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Marvelous Captain Jack Sparrow", "Jack", "Sparrow", "sea");
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account - the original fullName should not be changed
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Magnificent Captain Jack Sparrow");

        // Check account in dummy resource - the original fullName should not be changed
        assertDefaultDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);

        assertNoProvisioningScripts();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID); // MID-2451
        dummyAuditService.assertExecutionSuccess();

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test199DeleteUserJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createDeleteDelta(UserType.class, USER_JACK_OID
        );
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        try {
            getUser(USER_JACK_OID);
            AssertJUnit.fail("Jack is still alive!");
        } catch (ObjectNotFoundException ex) {
            // This is OK
        }

        // Check is shadow is gone
        assertNoShadow(accountJackOid);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        assertDummyScriptsDelete();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
        checkDummyTransportMessages("simpleUserNotifier-DELETE", 1);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test200AddUserBlackbeardWithAccount() throws Exception {
        given();
        Task task = getTestTask();
        // Use custom channel to trigger a special outbound mapping
        task.setChannel("http://pirates.net/avast");
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-blackbeard-account-dummy.xml"));
        addAccountLinkRef(user, new File(TEST_DIR, "account-blackbeard-dummy.xml"));
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userBlackbeard = modelService.getObject(UserType.class, USER_BLACKBEARD_OID, null, task, result);
        UserType userBlackbeardType = userBlackbeard.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userBlackbeardType.getLinkRef().size());
        ObjectReferenceType accountRefType = userBlackbeardType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        assertEncryptedUserPassword(userBlackbeard, "QueenAnne");
        assertPasswordMetadata(userBlackbeard, CredentialsType.F_PASSWORD, true, startTime, endTime, USER_ADMINISTRATOR_OID, "http://pirates.net/avast");

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "blackbeard");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "blackbeard", "Edward Teach");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("blackbeard", "Edward Teach", true);
        DummyAccount dummyAccount = getDummyAccount(null, "blackbeard");
        assertEquals("Wrong loot", (Integer) 10000, dummyAccount.getAttributeValue("loot", Integer.class));

        assertDummyScriptsAdd(userBlackbeard, accountModel, getDummyResourceType());

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0, 3);
        dummyAuditService.assertHasDelta(0, ChangeType.ADD, UserType.class);
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0, ChangeType.ADD, ShadowType.class);
        // this one was redundant
//        dummyAuditService.assertExecutionDeltas(1, 1);
//        dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, UserType.class);
        // raw operation, no target
//        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 1);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);
        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);

        assertSteadyResources();
    }

    @Test
    public void test210AddUserMorganWithAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-morgan-assignment-dummy.xml"));
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        executeChanges(userDelta, null, task, result);

        then();
        assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        display("User morgan after", userMorgan);
        UserType userMorganType = userMorgan.asObjectable();
        AssignmentType assignmentType = assertAssignedAccount(userMorgan, RESOURCE_DUMMY_OID);
        assertLiveLinks(userMorgan, 1);
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        assertCreateMetadata(userMorgan, startTime, endTime);
        assertCreateMetadata(assignmentType, startTime, endTime);

        assertEncryptedUserPassword(userMorgan, "rum");
        assertPasswordMetadata(userMorgan, true, startTime, endTime);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "morgan");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "morgan", "Sir Henry Morgan");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("morgan", "Sir Henry Morgan", true);

        assertDummyScriptsAdd(userMorgan, accountModel, getDummyResourceType());

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_MORGAN_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 1);
        checkDummyTransportMessages("userPasswordNotifier", 1);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 1);
        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);

        assertSteadyResources();
    }

    @Test
    public void test212RenameUserMorgan() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        modifyUserReplace(USER_MORGAN_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString("sirhenry"));

        then();
        assertSuccess(result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        // Check shadow
        PrismObject<ShadowType> accountShadowRepo = repositoryService.getObject(ShadowType.class, accountOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        display("Shadow repo", accountShadowRepo);
        assertDummyAccountShadowRepo(accountShadowRepo, accountOid, "sirhenry");

        // Check account
        PrismObject<ShadowType> accountShadowModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Shadow model", accountShadowModel);
        assertDummyAccountShadowModel(accountShadowModel, accountOid, "sirhenry", "Sir Henry Morgan");

        // Check account in dummy resource
        assertDefaultDummyAccount("sirhenry", "Sir Henry Morgan", true);

        assertDummyScriptsModify(userMorgan);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        ObjectDeltaOperation<ShadowType> auditShadowDelta = dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);

        assertEquals(
                "Unexpected number of modifications in shadow audit delta: " + auditShadowDelta.debugDump(),
                8,
                auditShadowDelta.getObjectDelta().getModifications().size());

        // The modifications are following:
        //  metadata/modifyChannel, modifyTimestamp, modifierRef, modifyTaskRef, modifyApproverRef, modifyApprovalComment
        //  attributes/icfs:name: morgan -> sirhenry
        //  attributes/icfs:uid: morgan -> sirhenry - this is a change induced by the connector/resource

        dummyAuditService.assertOldValue(ChangeType.MODIFY, UserType.class,
                UserType.F_NAME, PrismTestUtil.createPolyString("morgan"));
        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
                SchemaConstants.ICFS_NAME_PATH, "morgan");
        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
                SchemaConstants.ICFS_UID_PATH, "morgan");
        // This is a side-effect change. It is silently done by provisioning. It is not supposed to
        // appear in audit log.
//        dummyAuditService.assertOldValue(ChangeType.MODIFY, ShadowType.class,
//                ItemPath.create(ShadowType.F_NAME), PrismTestUtil.createPolyString("morgan"));

        dummyAuditService.assertTarget(USER_MORGAN_OID);
        dummyAuditService.assertExecutionSuccess();

        // Check notifications
        notificationManager.setDisabled(true);
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleAccountNotifier-DELETE-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 1);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
        checkDummyTransportMessages("simpleUserNotifier-DELETE", 0);

        assertSteadyResources();
    }

    /**
     * This basically tests for correct auditing.
     */
    @Test
    public void test240AddUserCharlesRaw() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        PrismObject<UserType> user = createUser("charles", "Charles L. Charles");
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        when();
        modelService.executeChanges(deltas, executeOptions().raw(), task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userAfter = findUserByUsername("charles");
        assertNotNull("No charles", userAfter);
        userCharlesOid = userAfter.getOid();

        assertNoProvisioningScripts();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
        // raw operation, no target
//        dummyAuditService.assertTarget(userAfter.getOid());
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
    }

    /**
     * This basically tests for correct auditing.
     */
    @Test
    public void test241DeleteUserCharlesRaw() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createDeleteDelta(UserType.class, userCharlesOid
        );
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        when();
        modelService.executeChanges(deltas, executeOptions().raw(), task, result);

        then();
        assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userAfter = findUserByUsername("charles");
        assertNull("Charles is not gone", userAfter);

        assertNoProvisioningScripts();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, UserType.class);
        // raw operation, no target
//        dummyAuditService.assertTarget(userCharlesOid);
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
    }

    @Test
    public void test300AddUserJackWithAssignmentBlue() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        PrismObject<UserType> userJack = PrismTestUtil.parseObject(USER_JACK_FILE);
        AssignmentType assignmentBlue = createConstructionAssignment(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null);
        userJack.asObjectable().getAssignment().add(assignmentBlue);

        ObjectDelta<UserType> delta = DeltaFactory.Object.createAddDelta(userJack);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        // Weak activation mapping means account load. But if the account does not previously exist,
        // no loading is required. And now we skip activation processing for completed projections.
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User after change execution", userJackAfter);
        assertUserJack(userJackAfter);
        accountJackBlueOid = getSingleLinkOid(userJackAfter);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackBlueOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertAccountShadowRepo(accountShadow, accountJackBlueOid, USER_JACK_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackBlueOid, null, task, result);
        getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME);
        assertShadowModel(accountModel, accountJackBlueOid, USER_JACK_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME),
                RI_ACCOUNT_OBJECT_CLASS);
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, USER_JACK_FULL_NAME, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.ADD, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0); // MID-4779
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertSteadyResources();
    }

    /**
     * modify account blue directly + request reconcile. check old value in delta.
     */
    @Test
    public void test302ModifyAccountJackDummyBlue() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                accountJackBlueOid, getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME).getAttributeFullnamePath(),
                "Cpt. Jack Sparrow");
        accountDelta.addModificationReplaceProperty(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH, "Queen Anne's Revenge");
        deltas.add(accountDelta);

        ModelExecuteOptions options = executeOptions().reconcile();

        OperationsPerformanceMonitor.INSTANCE.clearGlobalPerformanceInformation();

        when();
        modelService.executeChanges(deltas, options, task, result);

        then();
        OperationsPerformanceInformationType performanceInformation =
                OperationsPerformanceInformationUtil.toOperationsPerformanceInformationType(
                        OperationsPerformanceMonitor.INSTANCE.getGlobalPerformanceInformation());
        displayValue("Operation performance (by name)",
                OperationsPerformanceInformationUtil.format(performanceInformation));
        displayValue("Operation performance (by time)",
                OperationsPerformanceInformationUtil.format(performanceInformation,
                        new AbstractStatisticsPrinter.Options(TEXT, TIME), null, null));

        assertSuccess(result);
        // Not sure why 2 ... but this is not a big problem now
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 2);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        // Fullname inbound mapping is not used because it is weak
        assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
        String accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), result);
        assertAccountShadowRepo(accountShadow, accountJackBlueOid, USER_JACK_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));

        // Check account
        // All the changes should be reflected to the account
        modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertAccountShadowRepo(accountShadow, accountJackBlueOid, USER_JACK_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();

        dummyAuditService.assertExecutionDeltas(0, 2); // lastProvisioningTimestamp, modify account
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
                getDummyResourceController(RESOURCE_DUMMY_BLUE_NAME).getAttributeFullnamePath(), "Jack Sparrow");
//        dummyAuditService.assertOldValue(0, ChangeType.MODIFY, ShadowType.class,
//                dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));

        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();

        assertSteadyResources();
    }

    @Test // MID-5516
    public void test400RemoveExtensionProtectedStringValue() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ProtectedStringType protectedValue = protector.encryptString("hi");
        UserType joe = new UserType(prismContext)
                .name("joe");
        PrismPropertyDefinition<ProtectedStringType> definition = joe.asPrismObject().getDefinition()
                .findPropertyDefinition(ItemPath.create(UserType.F_EXTENSION, "locker"));
        PrismProperty<ProtectedStringType> protectedProperty = definition.instantiate();
        protectedProperty.setRealValue(protectedValue.clone());
        joe.asPrismObject().addExtensionItem(protectedProperty);

        addObject(joe.asPrismObject());

        display("joe before", joe.asPrismObject());

        when();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, "locker")
                .delete(protectedValue.clone())
                .asObjectDelta(joe.getOid());

        executeChanges(delta, null, task, result);

        then();
        PrismObject<UserType> joeAfter = getObject(UserType.class, joe.getOid());

        display("joe after", joeAfter);

        joeAfter.checkConsistence();
    }

    @Test // MID-6592
    public void test410RecomputeRole() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String roleOid = addTestRole(task, result);

        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<RoleType> emptyDelta = deltaFor(RoleType.class)
                .asObjectDelta(roleOid);

        when();
        modelService.executeChanges(singleton(emptyDelta), ModelExecuteOptions.create().reconcile(), task, result);

        then();
        assertSuccess(result);

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertCustomColumn("foo", "test");
    }

    /**
     * Tests the sanity of transformed schema. Currently there is a specific problem
     * with looking up container definitions pretending they are properties. See
     * also `TestSchemaRegistry.testMismatchedDefinitionLookup`.
     *
     * See MID-7690.
     *
     * This test is in this class because I've found no suitable test class in model-impl module.
     */
    @Test()
    public void test500MismatchedDefinitionLookupInTransformedSchema() throws CommonException {
        given("obtaining ResourceType definition via model-api");
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resource =
                modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        PrismObjectDefinition<ResourceType> objectDefinition = resource.getDefinition();

        when("looking up a definition for 'synchronization' (now container) assuming it's a property");
        PrismPropertyDefinition<?> propDef = objectDefinition.findPropertyDefinition(ResourceType.F_SYNCHRONIZATION);

        then("asserting it's null");
        assertThat(propDef).as("definition of property 'synchronization'").isNull();
    }

    private String addTestRole(Task task, OperationResult result) throws CommonException {
        RoleType role = new RoleType(prismContext)
                .name("test410");
        PrismPropertyDefinition<String> costCenterDef = role.asPrismObject().getDefinition()
                .findPropertyDefinition(ItemPath.create(RoleType.F_EXTENSION, "costCenter"));
        PrismProperty<String> costCenterProp = costCenterDef.instantiate();
        costCenterProp.setRealValue("CC000");
        role.asPrismObject().getOrCreateExtension().getValue().add(costCenterProp);
        return addObject(role, task, result);
    }

    private void assertDummyScriptsAdd(PrismObject<UserType> user, PrismObject<? extends ShadowType> account, ResourceType resource) {
        ProvisioningScriptSpec script = new ProvisioningScriptSpec("\nto spiral :size\n" +
                "   if  :size > 30 [stop]\n   fd :size rt 15\n   spiral :size *1.02\nend\n            ");

        String userName = null;
        if (user != null) {
            userName = user.asObjectable().getName().getOrig();
        }
        script.addArgSingle("usr", "user: " + userName);

        // Note: We cannot test for account name as name is only assigned in provisioning
        String accountEnabled = null;
        if (account != null && account.asObjectable().getActivation() != null
                && account.asObjectable().getActivation().getAdministrativeStatus() != null) {
            accountEnabled = account.asObjectable().getActivation().getAdministrativeStatus().toString();
        }
        script.addArgSingle("acc", "account: " + accountEnabled);

        String resourceName = null;
        if (resource != null) {
            resourceName = resource.getName().getOrig();
        }
        script.addArgSingle("res", "resource: " + resourceName);

        script.addArgSingle("size", "3");
        script.setLanguage("Logo");
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), script);
    }

    private void assertDummyScriptsModify(PrismObject<UserType> user) {
        assertDummyScriptsModify(user, false);
    }

    private void assertDummyScriptsModify(PrismObject<UserType> user, boolean recon) {
        ProvisioningScriptSpec modScript = new ProvisioningScriptSpec("Beware the Jabberwock, my son!");
        String name = null;
        String fullName = null;
        String costCenter = null;
        if (user != null) {
            name = user.asObjectable().getName().getOrig();
            fullName = user.asObjectable().getFullName().getOrig();
            costCenter = user.asObjectable().getCostCenter();
        }
        modScript.addArgSingle("howMuch", costCenter);
        modScript.addArgSingle("howLong", "from here to there");
        modScript.addArgSingle("who", name);
        modScript.addArgSingle("whatchacallit", fullName);
        if (recon) {
            ProvisioningScriptSpec reconBeforeScript = new ProvisioningScriptSpec("The vorpal blade went snicker-snack!");
            reconBeforeScript.addArgSingle("who", name);
            ProvisioningScriptSpec reconAfterScript = new ProvisioningScriptSpec("He left it dead, and with its head");
            reconAfterScript.addArgSingle("how", "enabled");
            IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), reconBeforeScript, modScript, reconAfterScript);
        } else {
            IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), modScript);
        }
    }

    private void assertDummyScriptsDelete() {
        ProvisioningScriptSpec script = new ProvisioningScriptSpec("The Jabberwock, with eyes of flame");
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), script);
    }

    private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        purgeProvisioningScriptHistory();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
    }

    private void assertResultSerialization(OperationResult result) throws SchemaException {
        OperationResultType resultType = result.createOperationResultType();
        String serialized = prismContext.xmlSerializer().serializeAnyData(resultType, SchemaConstants.C_RESULT);
        displayValue("OperationResultType serialized", serialized);
    }

}
