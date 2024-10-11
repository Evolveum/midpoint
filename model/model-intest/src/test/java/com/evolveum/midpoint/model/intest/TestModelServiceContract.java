/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.schema.processor.ResourceSchemaTestUtil.findObjectTypeDefinitionRequired;
import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.Format.TEXT;
import static com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.SortBy.TIME;
import static com.evolveum.midpoint.test.DummyResourceContoller.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.util.*;

import jakarta.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.test.TestSimulationResult;

import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
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
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.CompiledTracingProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter;
import com.evolveum.midpoint.schema.statistics.OperationsPerformanceInformationUtil;
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

/**
 * Note regarding simulation tests: The goal is not to check the "processed objects" in detail. Just to see if there's no
 * failure, no visible side effects, and that delta types and marks are roughly OK.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestModelServiceContract extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/contract");

    private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
    private static final String USER_BLACKBEARD_OID = "c0c010c0-d34d-b33f-f00d-161161116666";

    private static String accountJackOid;
    private static String accountJackBlueOid;
    private static String userCharlesOid;

    private final ObjectsCounter objectsCounter = new ObjectsCounter(FocusType.class, ShadowType.class);

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        addMarks(this, initTask, initResult);

        ARCHETYPE_REPORT.init(this, getTestTask(), getTestOperationResult());
        ARCHETYPE_COLLECTION_REPORT.init(this, getTestTask(), getTestOperationResult());
        REPORT_SIMULATION_OBJECTS.init(this, initTask, initResult);
        REPORT_SIMULATION_ITEMS_CHANGED.init(this, initTask, initResult);
        REPORT_SIMULATION_VALUES_CHANGED.init(this, initTask, initResult);

        InternalMonitor.reset();
        InternalMonitor.setTrace(InternalCounters.PRISM_OBJECT_CLONE_COUNT, true);
    }

    @Test
    public void test050GetUserJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        when();
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);

        then();
        assertSuccess(result);

        display("User jack", userJack);
        assertUserJack(userJack);

        assertNoShadowFetchOperations();
        assertSteadyResources();
    }

    @Test
    public void test051GetUserBarbossa() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        when();
        PrismObject<UserType> userBarbossa =
                modelService.getObject(UserType.class, USER_BARBOSSA_OID, null, task, result);

        then();
        assertSuccess(result);

        display("User barbossa", userBarbossa);
        assertUser(
                userBarbossa, USER_BARBOSSA_OID, "barbossa",
                "Hector Barbossa", "Hector", "Barbossa");
        userBarbossa.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);

        assertNoShadowFetchOperations();
        assertSteadyResources();
    }

    /** Creates an account, but the resource is set to be failing. */
    @Test
    public void test100ModifyUserAddAccountFailing() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);

        ObjectDelta<UserType> userDelta = deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .add(ObjectTypeUtil.createObjectRefWithFullObject(account))
                // the following modifies nothing but it is used to produce a user-level notification
                // (LINK_REF by itself causes no such notification)
                .item(UserType.F_TELEPHONE_NUMBER)
                .replace("555-1234")
                .asObjectDelta(USER_JACK_OID);

        getDummyResource().setAddBreakMode(BreakMode.UNSUPPORTED); // hopefully this does not kick consistency mechanism

        try {
            when();
            modelService.executeChanges(List.of(userDelta), null, task, result);

            assertNotReached();

        } catch (UnsupportedOperationException e) {
            then();
            displayExpectedException(e);
        }

        assertFailure(result);
        assertNoShadowFetchOperations();

        and("there is no linkRef");
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        assertEquals("Unexpected number of linkRefs", 0, userJack.asObjectable().getLinkRef().size());

        notificationManager.setDisabled(true);
        getDummyResource().resetBreakMode();

        and("notifications are OK");
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 0);
        // actually I don't know why provisioning does not report unsupported operation as a failure.
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 0);
        checkDummyTransportMessages("simpleUserNotifier", 0);
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);
        // This should be called, but it is not implemented now
        checkDummyTransportMessages("simpleUserNotifier-FAILURE", 0);

        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0); // MID-4779
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test110ModifyUserAddAccountSimulated() throws Exception {

        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        getDummyResource().resetBreakMode();
        objectsCounter.remember(result);

        when("account is added in the simulation mode");
        var simulationResult = executeWithSimulationResult(
                task, result,
                () -> modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_FILE, task, result));

        then("operation is successful");
        assertSuccess(result);

        and("no resource access, steady resources");
        assertNoShadowFetchOperations();
        assertSteadyResources();

        and("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find(
                        a -> a.assertEventMarks())
                .by().objectType(ShadowType.class).changeType(ChangeType.ADD).find(
                        a -> a.assertEventMarks(MARK_PROJECTION_ACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED))
                .assertSize(2);

        and("no side effects: no new objects, no provisioning scripts, no audit deltas, no notifications");
        objectsCounter.assertNoNewObjects(result);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
        dummyAuditService.assertNoRecord();
        dummyTransport.assertNoMessages();

        assertStandardSimulationReports(simulationResult, 2, 2, 2, result);
    }

    private void assertStandardSimulationReports(
            TestSimulationResult simulationResult, Integer objects, Integer items, Integer values, OperationResult result)
            throws CommonException, IOException {
        when("object-level simulation report is produced");
        List<String> lines1 = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        if (objects != null) {
            then("report is OK");
            assertCsv(lines1, "after")
                    .assertRecords(objects);
        }

        when("item-level simulation report is produced");
        List<String> lines2 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        if (items != null) {
            then("report is OK");
            assertCsv(lines2, "after")
                    .assertRecords(items);
        }

        when("value-level simulation report is produced");
        List<String> lines3 = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        if (values != null) {
            then("report is OK");
            assertCsv(lines3, "after")
                    .assertRecords(values);
        }
    }

    @Test
    public void test120ModifyUserAddAccount() throws Exception {
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
        assertNoShadowFetchOperations();

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
        var accountShadow = getShadowRepo(accountJackOid);
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
    public void test130GetAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        // Let's do some evil things. Like changing some of the attribute on a resource and see if they will be
        // fetched after get.
        // Also set a value for ignored "water" attribute. The system should cope with that.
        DummyAccount jackDummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        jackDummyAccount.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "The best pirate captain ever");
        jackDummyAccount.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, "cold");
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
                getDummyResourceController().getAttributeQName(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
                "The best pirate captain ever");
        // This one should still be here, even if ignored
        IntegrationTestTools.assertAttribute(account,
                getDummyResourceController().getAttributeQName(DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME),
                "cold");

        ShadowAttributesContainer attributesContainer = ShadowUtil.getAttributesContainer(account);
        assertNotNull("No attribute container from " + account, attributesContainer);
        Collection<ShadowSimpleAttribute<?>> identifiers = attributesContainer.getPrimaryIdentifiers();
        assertNotNull("No identifiers (null) in attributes container in " + accountJackOid, identifiers);
        assertFalse("No identifiers (empty) in attributes container in " + accountJackOid, identifiers.isEmpty());

        ShadowSimpleAttribute<String> fullNameAttr = attributesContainer.findSimpleAttribute(dummyResourceCtl.getAttributeFullnameQName());
        PrismAsserts.assertPropertyValue(fullNameAttr, ACCOUNT_JACK_DUMMY_FULLNAME);
        ShadowSimpleAttributeDefinition<String> fullNameAttrDef = fullNameAttr.getDefinition();
        displayDumpable("attribute fullname definition", fullNameAttrDef);
        PrismAsserts.assertDefinition(fullNameAttrDef, dummyResourceCtl.getAttributeFullnameQName(),
                DOMUtil.XSD_STRING, 1, 1);
        // MID-3144
        if (fullNameAttrDef.getDisplayOrder() == null
                || fullNameAttrDef.getDisplayOrder() < 100
                || fullNameAttrDef.getDisplayOrder() > 400) {
            AssertJUnit.fail("Wrong fullname displayOrder: " + fullNameAttrDef.getDisplayOrder());
        }
        assertEquals("Wrong fullname displayName", "Full Name", fullNameAttrDef.getDisplayName());

        assertSteadyResources();
    }

    @Test
    public void test140GetAccountNoFetch() throws Exception {

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options = createNoFetchCollection();

        when();
        PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountJackOid, options, task, result);

        display("Account", account);
        displayDumpable("Account def", account.getDefinition());
        assertNoShadowFetchOperations();
        PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
        displayDumpable("Account attributes def", accountContainer.getDefinition());
        displayDumpable("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());

        var repoAccount = getShadowRepo(accountJackOid);
        assertDummyAccountShadowRepo(repoAccount, accountJackOid, "jack");

        assertSuccess("getObject result", result);

        assertSteadyResources();
    }

    /**
     * MID-6716
     */
    @Test
    public void test150GetAccountRaw() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult parentResult = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder().raw().build();

        when();

        // Just to avoid result pruning.
        CompiledTracingProfile tracingProfile = tracer.compileProfile(
                new TracingProfileType()
                        .createTraceFile(false), parentResult);
        OperationResult result = parentResult.subresult("get")
                .tracingProfile(tracingProfile)
                .build();

        PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountJackOid, options, task, result);

        then();

        display("Account", account);
        displayDumpable("Account def", account.getDefinition());
        assertNoShadowFetchOperations();
        PrismContainer<Containerable> accountContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
        displayDumpable("Account attributes def", accountContainer.getDefinition());
        displayDumpable("Account attributes def complex type def", accountContainer.getDefinition().getComplexTypeDefinition());

        assertDummyAccountShadowRepo(getShadowRepo(accountJackOid), accountJackOid, "jack");

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
    public void test160SearchAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // get weapon attribute definition
        PrismObject<ResourceType> dummyResource =
                repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
        ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchemaRequired(dummyResource.asObjectable());
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);

        QName accountObjectClassQName = dummyResourceCtl.getAccountObjectClassQName();
        ResourceObjectClassDefinition accountObjectClassDefinition =
                resourceSchema.findObjectClassDefinitionRequired(accountObjectClassQName);
        QName weaponQName = dummyResourceCtl.getAttributeWeaponQName();
        ShadowSimpleAttributeDefinition<?> weaponDefinition = accountObjectClassDefinition.findSimpleAttributeDefinition(weaponQName);

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
    public void test170SearchAccountWithoutResourceSchema() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // create weapon attribute definition - NOT SUPPORTED, use only when you know what you're doing!
        QName accountObjectClassQName = dummyResourceCtl.getAccountObjectClassQName();
        QName weaponQName = dummyResourceCtl.getAttributeWeaponQName();
        PrismPropertyDefinition<String> weaponFakeDef =
                prismContext.definitionFactory().newPropertyDefinition(weaponQName, DOMUtil.XSD_STRING);

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
    public void test180ModifyUserAddAccountAgainSimulated() throws Exception {
        skipIfNotNativeRepository();

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        deleteAllSimulationResults(result);
        try {
            when("account is tried to be added (simulation mode)");
            executeWithSimulationResult(
                    task, result,
                    () -> modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_FILE, task, result));

            then();
            assert false : "Expected executeChanges operation to fail but it has obviously succeeded";
        } catch (SchemaException e) {
            then("error is reported");
            assertExpectedException(e)
                    .hasMessageContaining("already contains account")
                    .hasMessageContaining("default");
        }

        and("there is a simulation result with two unchanged objects (user, account)");
        TestSimulationResult simResult = findTestSimulationResultRequired(result);
        assertProcessedObjects(simResult, "after")
                .display()
                .by().objectType(UserType.class).state(ObjectProcessingStateType.UNMODIFIED).find(
                        a -> a.assertEventMarks())
                .by().objectType(ShadowType.class).state(ObjectProcessingStateType.UNMODIFIED).find(
                        a -> a.assertEventMarks())
                .assertSize(2)
                .end();
        // NOTE: currently no error is indicated there (unlike in audit)

        assertStandardSimulationReports(simResult, 2, 2, 2, result);
    }

    @Test
    public void test190ModifyUserAddAccountAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        try {
            when("account is tried to be added");
            modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_FILE, task, result);

            then();
            assert false : "Expected executeChanges operation to fail but it has obviously succeeded";
        } catch (SchemaException e) {
            then("error is reported");
            assertExpectedException(e)
                    .hasMessageContaining("already contains account")
                    .hasMessageContaining("default");
        }

        assertNoShadowFetchOperations();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);

        assertSteadyResources();
    }

    /** Adding account again; this time without OID. */
    @Test
    public void test200ModifyUserAddAccountAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(null);
        ObjectDelta<UserType> userDelta = createAddAccountDelta(USER_JACK_OID, account);

        try {

            when();
            modelService.executeChanges(List.of(userDelta), null, task, result);

            then();
            assert false : "Expected executeChanges operation to fail but it has obviously succeeded";
        } catch (SchemaException e) {
            then();
            assertExpectedException(e)
                    .hasMessageContaining("already contains account")
                    .hasMessageContaining("default");
        }

        assertNoProvisioningScripts();

        assertNoShadowFetchOperations();

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
    public void test210GetUserResolveAccount() throws Exception {
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
    public void test220GetUserResolveAccountResource() throws Exception {
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
    public void test230GetUserResolveAccountNoFetch() throws Exception {
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
        assertNoShadowFetchOperations();
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
        assertDummyAccountShadowModel(shadow.asPrismObject(), accountOid, "jack");

        result.computeStatus();
        TestUtil.assertSuccess("getObject result", result);

        userJack.checkConsistence(true, true);

        assertSteadyResources();
    }

    @Test
    public void test240ModifyUserDeleteAccountSimulated() throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);
        objectsCounter.remember(result);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(accountJackOid);
        ObjectDelta<UserType> userDelta = createDeleteAccountDelta(USER_JACK_OID, account);

        when("account is deleted in the simulation mode");
        var simulationResult = executeWithSimulationResult(List.of(userDelta), task, result);

        then("operation is successful");
        assertSuccess(result);

        and("1 or 0 resource access, steady resources");
        assertShadowFetchOperations(isCached() ? 0 : 1);
        assertSteadyResources();

        and("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find(
                        a -> a.assertEventMarks())
                .by().objectType(ShadowType.class).changeType(ChangeType.DELETE).find(
                        a -> a.assertEventMarks(
                                MARK_PROJECTION_DEACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED))
                .assertSize(2);

        and("no side effects: no new objects, no provisioning scripts, no audit deltas, no notifications");
        objectsCounter.assertNoNewObjects(result);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
        dummyAuditService.assertNoRecord();
        dummyTransport.assertNoMessages();

        assertStandardSimulationReports(simulationResult, 2, 2, 2, result);
    }

    @Test
    public void test250ModifyUserDeleteAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        account.setOid(accountJackOid);
        ObjectDelta<UserType> userDelta = createDeleteAccountDelta(USER_JACK_OID, account);

        when();
        modelService.executeChanges(List.of(userDelta), null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result, 2);
        assertNoShadowFetchOperations();

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
    public void test260AddAccount() throws Exception {
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
        assertNoShadowFetchOperations();

        accountJackOid = ObjectDeltaOperation.findProjectionDeltaOidInCollection(executeChanges);
        assertNotNull("No account OID in executed deltas", accountJackOid);
        // Check accountRef (should be none)
        PrismObject<UserType> userJack =
                modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
        checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0); // there's no password for that account
        checkDummyTransportMessages("userPasswordNotifier", 0);
        checkDummyTransportMessages("simpleAccountNotifier-SUCCESS", 1);
        checkDummyTransportMessages("simpleAccountNotifier-FAILURE", 0);
        checkDummyTransportMessages("simpleAccountNotifier-ADD-SUCCESS", 1);
        checkDummyTransportMessages("simpleUserNotifier", 0); // account has no owner
        checkDummyTransportMessages("simpleUserNotifier-ADD", 0);

        assertSteadyResources();
    }

    /**
     * Linking existing account.
     */
    @Test
    public void test270ModifyUserAddAccountRef() throws Exception {
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
        assertShadowFetchOperations(isCached() ? 0 : 1);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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

    /** Simulated deletion of the account while it is still linked. */
    @Test
    public void test280DeleteLinkedAccountSimulated() throws Exception {
        skipIfNotNativeRepository();

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        objectsCounter.remember(result);

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountJackOid);

        when();
        when("account is deleted in the simulation mode");
        var simulationResult = executeWithSimulationResult(List.of(accountDelta), task, result);

        then("operation is successful");
        assertSuccess(result);

        and("no resource access, steady resources");
        assertShadowFetchOperations(isCached() ? 0 : 1); // Because of the event mark policy rules
        assertSteadyResources();

        and("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find(
                        a -> a.assertEventMarks())
                .by().objectType(ShadowType.class).changeType(ChangeType.DELETE).find(
                        a -> a.assertEventMarks(MARK_PROJECTION_DEACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED))
                .assertSize(2);

        and("no side effects: no new objects, no provisioning scripts, no audit deltas, no notifications");
        objectsCounter.assertNoNewObjects(result);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
        dummyAuditService.assertNoRecord();
        dummyTransport.assertNoMessages();

        assertStandardSimulationReports(simulationResult, 2, 2, 2, result);
    }

    @Test
    public void test290ModifyUserDeleteAccountRef() throws Exception {
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
        assertNoShadowFetchOperations();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
        // Check accountRef
        assertUser(userJack, "after")
                .display()
                .assertLiveLinks(0)
                .assertRelatedLinks(0); // The link was deleted.

        // Check shadow (if it is unchanged)
        var accountShadow = getShadowRepo(accountJackOid);
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

    /** Simulated deletion of an unlinked account. */
    @Test
    public void test300DeleteUnlinkedAccountSimulated() throws Exception {
        skipIfNotNativeRepository();

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        objectsCounter.remember(result);

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountJackOid);

        when();
        when("account is deleted in the simulation mode");
        var simulationResult = executeWithSimulationResult(List.of(accountDelta), task, result);

        then("operation is successful");
        assertSuccess(result);

        and("single resource access (because of sims), steady resources");
        assertShadowFetchOperations(isCached() ? 0 : 1);
        assertSteadyResources();

        and("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display() // No user, because the account was not linked
                .by().objectType(ShadowType.class).changeType(ChangeType.DELETE).find(
                        a -> a.assertEventMarks(MARK_PROJECTION_DEACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED))
                .assertSize(1);

        and("no side effects: no new objects, no provisioning scripts, no audit deltas, no notifications");
        objectsCounter.assertNoNewObjects(result);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
        dummyAuditService.assertNoRecord();
        dummyTransport.assertNoMessages();

        assertStandardSimulationReports(simulationResult, 1, 1, 1, result);
    }

    @Test
    public void test310DeleteUnlinkedAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.POSITIVE);

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountJackOid);

        when();
        modelService.executeChanges(List.of(accountDelta), null, task, result);

        then();
        assertSuccess("executeChanges result", result);
        assertNoShadowFetchOperations();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
        assertUserNoAccountRefs(userJack); // Check that linkRef is gone
        assertNoShadow(accountJackOid); // Check is shadow is gone
        assertNoDummyAccount("jack"); // Check if dummy resource account is gone
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
    public void test320ModifyUserJackAssignAccountSimulated() throws Exception {
        skipIfNotNativeRepository();

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        objectsCounter.remember(result);

        ObjectDelta<UserType> delta =
                createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);

        when("account is assigned in the simulation mode");
        var simulationResult = executeWithSimulationResult(List.of(delta), task, result);

        then("operation is successful");
        assertSuccess(result);

        and("no resource access, steady resources");
        assertNoShadowFetchOperations();
        assertSteadyResources();

        and("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find(
                        a -> a.assertEventMarks(MARK_FOCUS_ASSIGNMENT_CHANGED))
                .by().objectType(ShadowType.class).changeType(ChangeType.ADD).find(
                        a -> a.assertEventMarks(MARK_PROJECTION_ACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED))
                .assertSize(2);

        and("no side effects: no new objects, no provisioning scripts, no audit deltas, no notifications");
        objectsCounter.assertNoNewObjects(result);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
        dummyAuditService.assertNoRecord();
        dummyTransport.assertNoMessages();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertUserNoAccountRefs(userJack); // No accountRef
        assertNoDummyAccount("jack"); // No account in dummy resource

        assertStandardSimulationReports(simulationResult, 2, 3, 3, result);
    }

    @Test
    public void test330ModifyUserJackAssignAccountPreview() {
        given();
        try {
            Task task = getTestTask();
            OperationResult result = task.getResult();
            preTestCleanup(AssignmentPolicyEnforcementType.FULL);

            ObjectDelta<UserType> delta =
                    createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);

            when();
            modelInteractionService.previewChanges(List.of(delta), executeOptions(), task, result);

            then();
            result.computeStatus();
            TestUtil.assertSuccess("previewChanges result", result);
            assertNoShadowFetchOperations();

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
    public void test340ModifyUserJackAssignAccount() throws Exception {
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
        assertNoShadowFetchOperations();
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 0, 50);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        assertUserAfter(userAfter)
                .assertModifyMetadataComplex(startTime, endTime)
                .assertLastProvisioningTimestamp(startTime, endTime)
                .assignments()
                .assertAssignments(1)
                .by().accountOn(RESOURCE_DUMMY_OID).find()
                .valueMetadataSingle()
                .assertCreateMetadataComplex(startTime, endTime);

        assertUserJack(userAfter);

        accountJackOid = getSingleLinkOid(userAfter);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel =
                modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
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
     *
     * Simulation mode.
     */
    @Test
    public void test350ModifyAccountJackDummySimulated() throws Exception {
        skipIfNotNativeRepository();

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
        objectsCounter.remember(result);

        when("account is modified in the simulation mode");
        var simulationResult = executeWithSimulationResult(List.of(createJacksAccountModifyDelta()), task, result);

        then("operation is successful");
        assertSuccess(result);

        and("single shadow fetch, steady resources");
        assertShadowFetchOperations(isCached() ? 0 : 1); // strong mapping, simulation mode
        assertSteadyResources();

        and("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY)
                .find(a -> a.assertEventMarks()
                        .delta(d -> d.assertModifiedExclusive(
                                        InfraItemName.METADATA,
                                        UserType.F_ORGANIZATIONAL_UNIT)
                                .assertPolyStringModification(
                                        UserType.F_ORGANIZATIONAL_UNIT,
                                        null,
                                        "The crew of Queen Anne's Revenge")))
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY)
                .find(a -> a.assertEventMarks(MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                        .delta(d -> d.assertModifiedExclusive(
                                        InfraItemName.METADATA,
                                        DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH,
                                        DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH)
                                .assertModification(
                                        DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH,
                                        "Jack Sparrow",
                                        "Cpt. Jack Sparrow")
                                .assertModification(
                                        DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH,
                                        null,
                                        "Queen Anne's Revenge")))
                .assertSize(2);

        and("no side effects: no new objects, no provisioning scripts, no audit deltas, no notifications");
        objectsCounter.assertNoNewObjects(result);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
        dummyAuditService.assertNoRecord();
        dummyTransport.assertNoMessages();

        and("No changes in the account");
        PrismObject<ShadowType> accountModel =
                modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");
        PrismAsserts.assertNoItem(accountModel, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH);

        assertStandardSimulationReports(simulationResult, 2, 3, 4, result);
        // 1x organizationalUnit, 2x fullname, 1x ship
    }

    private ObjectDelta<ShadowType> createJacksAccountModifyDelta() throws SchemaException, ConfigurationException {
        return Resource.of(dummyResourceCtl.getResource())
                .deltaFor(RI_ACCOUNT_OBJECT_CLASS)
                .item(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH)
                .replace("Cpt. Jack Sparrow")
                .item(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH)
                .replace("Queen Anne's Revenge")
                .asObjectDelta(accountJackOid);
    }

    /**
     * Modify the account. Some of the changes should be reflected back to the user by inbound mapping.
     */
    @Test
    public void test360ModifyAccountJackDummy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        // Creating empty partial processing option is the same as having none. This code is here just to test MID-9477.
        var options = ModelExecuteOptions.create()
                .partialProcessing(new PartialProcessingOptionsType());
        executeChanges(createJacksAccountModifyDelta(), options, task, result);

        then();
        assertSuccess(result);

        assertShadowFetchOperations(isCached() ? 0 : 1); // There is strong mapping. Complete account is fetched.

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
        var accountShadow = getShadowRepo(accountJackOid);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        // All the changes should be reflected to the account
        PrismObject<ShadowType> accountModel =
                modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Cpt. Jack Sparrow");
        PrismAsserts.assertPropertyValue(accountModel, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH, "Queen Anne's Revenge");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_JACK_USERNAME, "Cpt. Jack Sparrow", true);
        assertDummyAccountAttribute(
                null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
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
    public void test370ModifyUserJackAssignAccountAgain() throws Exception {
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
        var accountShadow = getShadowRepo(accountJackOid);
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
    public void test380JackRecomputeNoChange() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        var userAfter = assertUserAfter(USER_JACK_OID)
                .assignments()
                .assertAccount(RESOURCE_DUMMY_OID)
                .assertAssignments(1)
                .end()
                .assertLastProvisioningTimestamp(null, startTime)
                .getObject();

        assertUserJack(userAfter);

        String accountJackOidAfter = getSingleLinkOid(userAfter);
        assertEquals("Account OID changed", accountJackOid, accountJackOidAfter);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel =
                modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Cpt. Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Cpt. Jack Sparrow", true);
        assertDummyAccountAttribute(
                null, USER_JACK_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME,
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
    public void test390ModifyUserJackUnassignAccountSimulated() throws Exception {
        skipIfNotNativeRepository();

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
        objectsCounter.remember(result);

        when();
        var simulationResult = executeWithSimulationResult(
                List.of(createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false)),
                task, result);

        then("operation is successful");
        assertSuccess(result);

        and("single shadow read, steady resources");
        assertShadowFetchOperations(isCached() ? 0 : 1);
        assertSteadyResources();

        and("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find(
                        a -> a.assertEventMarks(MARK_FOCUS_ASSIGNMENT_CHANGED))
                .by().objectType(ShadowType.class).changeType(ChangeType.DELETE).find(
                        a -> a.assertEventMarks(MARK_PROJECTION_DEACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED))
                .assertSize(2);

        and("no side effects: no new objects, no provisioning scripts, no audit deltas, no notifications");
        objectsCounter.assertNoNewObjects(result);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
        dummyAuditService.assertNoRecord();
        dummyTransport.assertNoMessages();

        assertStandardSimulationReports(simulationResult, 2, 3, 3, result);
        // assignment, linkRef, shadow
    }

    @Test
    public void test400ModifyUserJackUnassignAccount() throws Exception {
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
        assertNoShadowFetchOperations();

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
    public void test410ModifyUserJackAssignAccountPositiveEnforcement() throws Exception {
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
        assertNoShadowFetchOperations();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
    public void test420ModifyUserJackUnassignAccountPositiveEnforcement() throws Exception {
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
        assertShadowFetchOperations(isCached() ? 0 : 1);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertLinked(userJack, accountJackOid);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
    public void test430ModifyUserJackDeleteAccount() throws Exception {
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
        assertNoShadowFetchOperations();

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
    public void test440ModifyUserJackAssignAccountRelativeEnforcement() throws Exception {
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
        assertNoShadowFetchOperations();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
    public void test450ModifyUserJackUnassignAccountRelativeEnforcement() throws Exception {
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
        assertNoShadowFetchOperations();

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
    public void test460ModifyUserJackAssignAccountNoneEnforcement() throws Exception {
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
        assertNoShadowFetchOperations();

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
    public void test470ModifyUserJackAddAccountNoneEnforcement() throws Exception {
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
        assertNoShadowFetchOperations();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
    public void test480ModifyUserJackUnassignAccountNoneEnforcement() throws Exception {
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
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, isCached() ? 0 : 1);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
    public void test490ModifyUserJackDeleteAccountNoneEnforcement() throws Exception {
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
        assertNoShadowFetchOperations();

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
    public void test500ModifyUserAddAccountFullEnforcement() throws Exception {
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
        assertNoShadowFetchOperations();

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
    public void test510ModifyUserAddAndAssignAccountPositiveEnforcement() throws Exception {
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
        assertNoShadowFetchOperations();
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
        var accountShadow = getShadowRepo(accountJackOid);
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
    public void test520ModifyUserJackUnassignAndDeleteAccount() throws Exception {
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
        assertNoShadowFetchOperations();

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
    public void test530ModifyUserJackAssignAccountAndModify() throws Exception {
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
        assertNoShadowFetchOperations();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
    public void test540ModifyUserJackModifyAssignment() throws Exception {
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
        PrismPropertyDefinition<String> gossipDefinition =
                accountDefinition.findPropertyDefinition(
                        new ItemName(NS_RI, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME));
        assertNotNull("gossip attribute definition not found", gossipDefinition);

        ConstructionType accountConstruction = createAccountConstruction(RESOURCE_DUMMY_OID, null);
        ResourceAttributeDefinitionType radt = new ResourceAttributeDefinitionType();
        radt.setRef(new ItemPathType(gossipDefinition.getItemName()));
        MappingType outbound = new MappingType();
        radt.setOutbound(outbound);

        ExpressionType expression = new ExpressionType();
        outbound.setExpression(expression);

        MappingType value = new MappingType();

        PrismProperty<String> property = gossipDefinition.instantiate();
        property.addRealValue("q");

        List<JAXBElement<?>> evaluators = expression.getExpressionEvaluator();
        Collection<JAXBElement<RawType>> collection = StaticExpressionUtil.serializeValueElements(property);
        ObjectFactory of = new ObjectFactory();
        for (JAXBElement<RawType> obj : collection) {
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

//        // Set user and assignment create channel to legacy value.
//        repositoryService.modifyObject(
//                UserType.class, jackBefore.getOid(),
//                deltaFor(UserType.class)
//                        .item(UserType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
//                        .replace(Channel.USER.getLegacyUri())
//                        .item(UserType.F_ASSIGNMENT, assignmentId, AssignmentType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
//                        .replace(Channel.USER.getLegacyUri())
//                        .asItemDeltas(),
//                result);

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
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, isCached() ? 0 : 2);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");
        accountJackOid = getSingleLinkOid(userJack);

//        // MID-6547 (channel URI migration)
//        assertThat(ValueMetadataTypeUtil.getStorageMetadata(userJack.asObjectable()))
//                .as("storage metadata in jack")
//                .extracting(m -> m.getCreateChannel())
//                .isEqualTo(Channel.USER.getUri());
//        assertThat(ValueMetadataTypeUtil.getStorageMetadata(userJack.asObjectable().getAssignment().get(0)))
//                .as("storage metadata in jack's first assignment")
//                .extracting(m -> m.getCreateChannel())
//                .isEqualTo(Channel.USER.getUri());

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
    public void test550ModifyUserJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
                PolyString.fromOrig("Magnificent Captain Jack Sparrow"));

        then();
        assertSuccess(result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, isCached() ? 0 : 1);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, "Magnificent Captain Jack Sparrow");
        accountJackOid = getSingleLinkOid(userAfter);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
                UserType.F_FULL_NAME, PolyString.fromOrig("Jack Sparrow"));
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
    public void test560ModifyUserJackLocationEmpty() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result /* no value */);

        then();
        assertSuccess(result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, isCached() ? 0 : 1);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, "Magnificent Captain Jack Sparrow", "Jack", "Sparrow", null);
        accountJackOid = getSingleLinkOid(userAfter);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
        dummyAuditService.assertOldValue(
                ChangeType.MODIFY, UserType.class, UserType.F_LOCALITY, PolyString.fromOrig(USER_JACK_LOCALITY));
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
    public void test570ModifyUserJackLocationNull() throws Exception {
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
        assertNoShadowFetchOperations();

        assertNoProvisioningScripts();

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        // This should fail even before the request record is created
        dummyAuditService.assertRecords(0);

        assertCounterIncrement(InternalCounters.SCRIPT_COMPILE_COUNT, 0);
        assertSteadyResources();
    }

    @Test
    public void test580ModifyUserJackLocationSea() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, PolyString.fromOrig("sea"));

        then();
        assertSuccess(result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, isCached() ? 0 : 1);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, "Magnificent Captain Jack Sparrow", USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME, "sea");
        accountJackOid = getSingleLinkOid(userAfter);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
        dummyAuditService.assertPropertyReplace(
                ChangeType.MODIFY, UserType.class, UserType.F_LOCALITY, PolyString.fromOrig("sea"));
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

    /** The simulated raw execution is forbidden. */
    @Test
    public void test590ModifyUserJackRawSimulated() throws Exception {
        skipIfNotNativeRepository();

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME)
                .replace(PolyString.fromOrig("Marvelous Captain Jack Sparrow"))
                .asObjectDelta(USER_JACK_OID);

        when();
        try {
            executeWithSimulationResult(
                    List.of(delta),
                    executeOptions().raw(),
                    TaskExecutionMode.SIMULATED_PRODUCTION,
                    defaultSimulationDefinition(),
                    task, result);
            fail("unexpected success");
        } catch (UnsupportedOperationException e) {
            assertExpectedException(e)
                    .hasMessageContaining("Raw operation execution is not supported");
        }
    }

    @Test
    public void test600ModifyUserJackRaw() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME)
                .replace(PolyString.fromOrig("Marvelous Captain Jack Sparrow"))
                .asObjectDelta(USER_JACK_OID);

        when();
        modelService.executeChanges(List.of(delta), executeOptions().raw(), task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertNoShadowFetchOperations();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Marvelous Captain Jack Sparrow", "Jack", "Sparrow", "sea");
        accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
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
    public void test610DeleteUserJackSimulated() throws Exception {
        skipIfNotNativeRepository();

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
        objectsCounter.remember(result);

        ObjectDelta<UserType> delta =
                prismContext.deltaFactory().object().createDeleteDelta(UserType.class, USER_JACK_OID);

        when();
        var simulationResult = executeWithSimulationResult(List.of(delta), task, result);

        then("operation is successful");
        assertSuccess(result);

        and("single shadow fetch, steady resources");
        assertShadowFetchOperations(isCached() ? 0 : 1);
        assertSteadyResources();

        and("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.DELETE).find(
                        a -> a.assertEventMarks(MARK_FOCUS_DEACTIVATED))
                .by().objectType(ShadowType.class).changeType(ChangeType.DELETE).find(
                        a -> a.assertEventMarks(MARK_PROJECTION_DEACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED))
                .assertSize(2);

        and("no side effects: no new objects, no provisioning scripts, no audit deltas, no notifications");
        objectsCounter.assertNoNewObjects(result);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
        dummyAuditService.assertNoRecord();
        dummyTransport.assertNoMessages();

        // user, shadow
        assertStandardSimulationReports(simulationResult, 2, 2, 2, result);
    }

    @Test
    public void test620DeleteUserJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> delta =
                prismContext.deltaFactory().object().createDeleteDelta(UserType.class, USER_JACK_OID);

        when();
        modelService.executeChanges(List.of(delta), null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertNoShadowFetchOperations();

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
    public void test630AddUserBlackbeardWithAccount() throws Exception {
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
        assertNoShadowFetchOperations();

        PrismObject<UserType> userBlackbeard = modelService.getObject(UserType.class, USER_BLACKBEARD_OID, null, task, result);
        UserType userBlackbeardType = userBlackbeard.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userBlackbeardType.getLinkRef().size());
        ObjectReferenceType accountRefType = userBlackbeardType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        assertEncryptedUserPassword(userBlackbeard, "QueenAnne");
        assertPasswordMetadata(userBlackbeard, CredentialsType.F_PASSWORD, true, startTime, endTime, USER_ADMINISTRATOR_OID, "http://pirates.net/avast");

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "blackbeard");
        assertEnableTimestampShadow(accountShadow, startTime, endTime);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, "blackbeard", "Edward Teach");
        assertEnableTimestampShadow(accountModel, startTime, endTime);

        // Check account in dummy resource
        assertDefaultDummyAccount("blackbeard", "Edward Teach", true);
        DummyAccount dummyAccount = getDummyAccount(null, "blackbeard");
        assertEquals("Wrong loot", (Long) 10000L, dummyAccount.getAttributeValue("loot", Long.class));

        assertDummyScriptsAdd(userBlackbeard, accountModel, getDummyResourceType());

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        // If activation is cached, the weak inbound mapping is applied.
        // This depends on the default cache use, which is currently USE_CACHED_OR_FRESH.
        // It this changes, we will need to adapt this test.
        dummyAuditService.assertRecords(isCached() ? 3 : 2);
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
    public void test640AddUserMorganWithAssignmentSimulated() throws Exception {
        skipIfNotNativeRepository();

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
        objectsCounter.remember(result);

        ObjectDelta<UserType> delta =
                DeltaFactory.Object.createAddDelta(
                        PrismTestUtil.parseObject(
                                new File(TEST_DIR, "user-morgan-assignment-dummy.xml")));

        when();
        var simulationResult = executeWithSimulationResult(List.of(delta), task, result);

        then("operation is successful");
        assertSuccess(result);

        and("no shadow fetch, steady resources");
        assertNoShadowFetchOperations();
        assertSteadyResources();

        and("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.ADD).find(
                        a -> a.assertEventMarks(MARK_FOCUS_ACTIVATED))
                .by().objectType(ShadowType.class).changeType(ChangeType.ADD).find(
                        a -> a.assertEventMarks(MARK_PROJECTION_ACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED))
                .assertSize(2);

        and("no side effects: no new objects, no provisioning scripts, no audit deltas, no notifications");
        objectsCounter.assertNoNewObjects(result);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
        dummyAuditService.assertNoRecord();
        dummyTransport.assertNoMessages();

        assertStandardSimulationReports(simulationResult, 2, 2, 2, result);
    }

    @Test
    public void test645AddUserMorganWithAssignment() throws Exception {
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
        assertNoShadowFetchOperations();

        PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        assertUserAfter(userMorgan)
                .assertCreateMetadataComplex(startTime, endTime)
                .assignments()
                .by().accountOn(RESOURCE_DUMMY_OID).find()
                .valueMetadataSingle()
                .assertCreateMetadataComplex(startTime, endTime);

        assertLiveLinks(userMorgan, 1);
        ObjectReferenceType accountRefType = userMorgan.asObjectable().getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        assertEncryptedUserPassword(userMorgan, "rum");
        assertPasswordMetadata(userMorgan, true, startTime, endTime);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
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
    public void test650RenameUserMorganSimulated() throws Exception {
        skipIfNotNativeRepository();

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
        objectsCounter.remember(result);

        when();
        var simulationResult = executeWithSimulationResult(
                task, result,
                () -> modifyUserReplace(
                        USER_MORGAN_OID, UserType.F_NAME, task, result, PolyString.fromOrig("sirhenry")));

        then("operation is successful");
        assertSuccess(result);

        and("one shadow fetch, steady resources");
        assertShadowFetchOperations(isCached() ? 0 : 1);
        assertSteadyResources();

        and("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find(
                        a -> a.assertEventMarks(MARK_FOCUS_RENAMED)
                                .assertName("morgan")) // original name (MID-8610)
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find(
                        a -> a.assertEventMarks(
                                MARK_PROJECTION_RENAMED,
                                MARK_PROJECTION_IDENTIFIER_CHANGED,
                                MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                                .assertName("morgan")) // original name (MID-8610)
                .assertSize(2);

        and("no side effects: no new objects, no provisioning scripts, no audit deltas, no notifications");
        objectsCounter.assertNoNewObjects(result);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
        dummyAuditService.assertNoRecord();
        dummyTransport.assertNoMessages();

        assertStandardSimulationReports(simulationResult, 2, 2, 4, result);
        // name ADD+DELETE, shadow name ADD+DELETE
    }

    @Test
    public void test655RenameUserMorgan() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        when();
        modifyUserReplace(USER_MORGAN_OID, UserType.F_NAME, task, result, PolyString.fromOrig("sirhenry"));

        then();
        assertSuccess(result);
        // Strong mappings
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, isCached() ? 0 : 1);

        PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        // Check shadow
        var accountShadowRepo = getShadowRepo(accountOid);
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
                UserType.F_NAME, PolyString.fromOrig("morgan"));
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
    public void test660AddUserCharlesRaw() throws Exception {
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
        assertNoShadowFetchOperations();

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
    public void test670DeleteUserCharlesRaw() throws Exception {
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
        assertNoShadowFetchOperations();

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
    public void test680AddUserJackWithAssignmentBlue() throws Exception {
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
        assertNoShadowFetchOperations();

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User after change execution", userJackAfter);
        assertUserJack(userJackAfter);
        accountJackBlueOid = getSingleLinkOid(userJackAfter);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackBlueOid);
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
    public void test690ModifyAccountJackDummyBlue() throws Exception {
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
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, isCached() ? 0 : 2);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        // Fullname inbound mapping is not used because it is weak
        assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
        String accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid); // TODO jackOid vs blueOid??
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
    public void test700RemoveExtensionProtectedStringValue() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);

        ProtectedStringType protectedValue = protector.encryptString("hi");
        UserType joe = new UserType()
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
    public void test710RecomputeRole() throws Exception {
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

    private String addTestRole(Task task, OperationResult result) throws CommonException {
        RoleType role = new RoleType()
                .name(getTestNameShort());
        PrismPropertyDefinition<String> costCenterDef = role.asPrismObject().getDefinition()
                .findPropertyDefinition(ItemPath.create(RoleType.F_EXTENSION, "costCenter"));
        PrismProperty<String> costCenterProp = costCenterDef.instantiate();
        costCenterProp.setRealValue("CC000");
        role.asPrismObject().getOrCreateExtension().getValue().add(costCenterProp);
        return addObject(role, task, result);
    }

    /**
     * Checks whether broken live `linkRef` value is correctly removed.
     *
     * See MID-8361.
     */
    @Test
    public void test720DanglingLiveLinkRefCleanup() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        String randomShadowOid = "79898dfa-0d84-43be-b15d-2bb2c8333428";

        given("a user with a dangling live `linkRef` exists");
        UserType user = new UserType()
                .name("test420")
                .linkRef(randomShadowOid, ShadowType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT);
        repositoryService.addObject(user.asPrismObject(), null, result);

        when("the user is recomputed");
        recomputeUser(user.getOid(), task, result);

        then("there should be no link now");
        assertUserAfter(user.getOid())
                .assertLinks(0, 0);
    }

    /**
     * Checks whether broken dead `linkRef` value is correctly removed.
     *
     * See MID-8361.
     */
    @Test
    public void test730DanglingDeadLinkRefCleanup() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        preTestCleanup(AssignmentPolicyEnforcementType.RELATIVE);

        String randomShadowOid = "8b61179c-6c7c-4933-ba6a-12a943eb1491";

        given("a user with a dangling dead `linkRef` exists");
        UserType user = new UserType()
                .name("test430")
                .linkRef(randomShadowOid, ShadowType.COMPLEX_TYPE, SchemaConstants.ORG_RELATED);
        repositoryService.addObject(user.asPrismObject(), null, result);

        when("the user is recomputed");
        recomputeUser(user.getOid(), task, result);

        then("there should be no link now");
        assertUserAfter(user.getOid())
                .assertLinks(0, 0);
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
    @Test
    public void test740MismatchedDefinitionLookupInTransformedSchema() throws CommonException {
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

    /**
     * Checking the number of audit records when a new role (without assignments) is created. MID-8659.
     */
    @Test
    public void test750CreateRoleSimple() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        dummyAuditService.clear();

        when("a role is created");
        RoleType role = new RoleType()
                .name(getTestNameShort());
        addObject(role.asPrismObject(), task, result);

        then("there should be 2 audit records");
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
    }

    /**
     * Checking the number of audit records when a new role (with an archetype) is created. MID-8659.
     */
    @Test
    public void test760CreateRoleWithArchetype() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        given("an archetype");
        ArchetypeType archetype = new ArchetypeType()
                .name(getTestNameShort());
        var archetypeOid = addObject(archetype.asPrismObject(), task, result);

        dummyAuditService.clear();

        when("a role with an archetype is created");
        RoleType role = new RoleType()
                .name(getTestNameShort())
                .assignment(new AssignmentType()
                        .targetRef(archetypeOid, ArchetypeType.COMPLEX_TYPE));
        addObject(role.asPrismObject(), task, result);

        then("audit records are OK");
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2 + accessesMetadataAuditOverhead(1));
        dummyAuditService.assertSimpleRecordSanity();
    }

    /**
     * Checking the number of audit records when a new user (with a role) is created. MID-8659.
     */
    @Test
    public void test770CreateUserWithRole() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        given("a role");
        RoleType role = new RoleType()
                .name(getTestNameShort());
        var roleOid = addObject(role.asPrismObject(), task, result);

        dummyAuditService.clear();

        when("a user with a role is created");
        UserType user = new UserType()
                .name(getTestNameShort())
                .assignment(new AssignmentType()
                        .targetRef(roleOid, RoleType.COMPLEX_TYPE));
        addObject(user.asPrismObject(), task, result);

        then("audit records are OK");
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2 + accessesMetadataAuditOverhead(1));
        dummyAuditService.assertSimpleRecordSanity();
    }

    /** The clockwork should be able to unlink also dead shadows. MID-9668. */
    @Test
    public void test780UnlinkDeadShadow() throws Exception {
        testUnlinkOrDeleteDeadShadow(false);
    }

    /** The clockwork should be able to delete also dead shadows. MID-9668. */
    @Test
    public void test785DeleteDeadShadow() throws Exception {
        testUnlinkOrDeleteDeadShadow(true);
    }

    private void testUnlinkOrDeleteDeadShadow(boolean delete) throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        if (isCached()) {
            throw new SkipException("Temporarily disabled");
        }

        given("a user with a dead shadow");
        var user = new UserType()
                .name(userName)
                .assignment(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)));
        var userOid = addObject(user, task, result);

        dummyResourceCtl.deleteAccount(userName);
        reconcileUser(userOid, task, result);

        var deadLinkRefVal = assertUserBefore(userOid)
                .assertLinks(1, 1)
                .links()
                .singleDead()
                .getRefVal();

        PrismObject<ShadowType> shadowToDelete;
        if (delete) {
            shadowToDelete = provisioningService.getObject(
                    ShadowType.class,
                    deadLinkRefVal.getOid(),
                    createNoFetchCollection(),
                    task, result);
        } else {
            shadowToDelete = null;
        }
        deadLinkRefVal.setObject(shadowToDelete);

        when("the dead shadow is " + (delete ? "deleted" : "unlinked"));
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_LINK_REF)
                        .delete(deadLinkRefVal.clone())
                        .asObjectDelta(userOid),
                null, task, result);

        then("the dead linkRef is not there anymore");
        assertUserAfter(userOid)
                .assertLinks(1, 0);

        and("the shadow still exists (even when unlinked - because of the dead shadow retention by provisioning");
        assertRepoShadow(deadLinkRefVal.getOid())
                .display()
                .assertDead();
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

    private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
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

    boolean isCached() {
        return InternalsConfig.isShadowCachingFullByDefault();
    }
}
