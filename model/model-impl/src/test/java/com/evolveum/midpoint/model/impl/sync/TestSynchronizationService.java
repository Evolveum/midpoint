/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.api.util.DiagnosticContextManager;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ClockworkMedic;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.util.mock.MockLensDebugListener;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.DiagnosticContext;
import com.evolveum.midpoint.schema.util.DiagnosticContextHolder;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSynchronizationService extends AbstractInternalModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/sync");

    private static final DummyTestResource RESOURCE_DUMMY_LIMITED =
            new DummyTestResource(
                    TEST_DIR,
                    "resource-dummy-limited.xml",
                    "cbe8baa0-64dd-11e8-9760-076bd690e1c4",
                    "limited",
                    controller -> controller.extendSchemaPirate());

    private static final File SHADOW_PIRATES_DUMMY_FILE = new File(TEST_DIR, "shadow-pirates-dummy.xml");
    private static final String GROUP_PIRATES_DUMMY_NAME = "pirates";

    private static final String INTENT_GROUP = "group";

    private static final DummyTestResource RESOURCE_DUMMY_BROKEN =
            new DummyTestResource(TEST_DIR, "resource-dummy-broken.xml", "ba2099e5-7c6b-4742-a13a-0cbc476aeb01", "broken");

    private static final TestTask TASK_IMPORT_DUMMY_BROKEN =
            new TestTask(TEST_DIR, "task-import-dummy-broken.xml", "826071d1-5f8b-4d44-af0a-f0e7970f01a4");

    @Autowired SynchronizationService synchronizationService;
    @Autowired Clockwork clockwork;
    @Autowired ClockworkMedic clockworkMedic;

    private String accountShadowJackDummyOid = null;
    private String accountShadowJackDummyLimitedOid;
    private String accountShadowCalypsoDummyOid = null;

    private MockLensDebugListener mockListener;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        logger.trace("initSystem");
        super.initSystem(initTask, initResult);

        initAndTestDummyResource(RESOURCE_DUMMY_LIMITED, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_BROKEN, initTask, initResult);
    }

    @Test
    public void test010AddedAccountJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        setDebugListener();

        PrismObject<ShadowType> accountShadowJack = repoAddObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILE, result);
        accountShadowJackDummyOid = accountShadowJack.getOid();
        prepareShadow(accountShadowJack, task, result);
        assertNotNull("No oid in shadow", accountShadowJack.getOid());
        DummyAccount dummyAccount = new DummyAccount();
        dummyAccount.setName(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.setPassword("deadMenTellNoTales");
        dummyAccount.setEnabled(true);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
        getDummyResource().addAccount(dummyAccount);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setShadowedResourceObject(accountShadowJack);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN
        synchronizationService.notifyChange(change, task, result);

        // THEN
        assertSuccess(result);

        LensContext<UserType> context = cleanDebugListener();

        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
                ActivationStatusType.ENABLED);

        ProjectionContextKey rsd = getDefaultAccountKey();
        LensProjectionContext accCtx = context.findProjectionContextByKeyExact(rsd);
        assertNotNull("No projection context for " + rsd, accCtx);
        assertEquals("Wrong detected situation in context", SynchronizationSituationType.UNLINKED, accCtx.getSynchronizationSituationDetected());
        assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationResolved());

        PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());
        //it this really expected?? delta was already executed, should we expect it in the secondary delta?
//        assertNotNull("Missing account secondary delta", accCtx.getSecondaryDelta());
//        assertIterationDelta(accCtx.getSecondaryDelta(), 0, "");

        assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());

        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);
    }

    @Test
    public void test020ModifyLootAbsolute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        setDebugListener();

        DummyAccount dummyAccount = getDummyResource().getAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 999L);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJack = provisioningService.getObject(ShadowType.class, accountShadowJackDummyOid, null, task, result);
        change.setShadowedResourceObject(accountShadowJack);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN
        when();
        synchronizationService.notifyChange(change, task, result);

        // THEN
        then();
        LensContext<UserType> context = cleanDebugListener();

        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertEquals("Unexpected number of executed deltas", 1, context.getFocusContext().getExecutedDeltas().size());
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getExecutedDeltas().iterator().next().getObjectDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        assertEquals("Unexpected number of modifications in user secondary delta", 7, userSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(userSecondaryDelta, UserType.F_COST_CENTER, "999");

        ProjectionContextKey rat = getDefaultAccountKey();
        LensProjectionContext accCtx = context.findProjectionContextByKeyExact(rat);
        assertNotNull("No account sync context for " + rat, accCtx);

        PrismAsserts.assertNoDelta("account primary delta", accCtx.getPrimaryDelta());
        PrismAsserts.assertNoDelta("account secondary delta", accCtx.getSecondaryDelta());

        assertEquals("Wrong detected situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationDetected());

        assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertEquals("Unexpected used constCenter", "999", user.asObjectable().getCostCenter());

        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);

        assertSuccess(result);
    }

    @Test
    public void test021ModifyLootAbsoluteEmpty() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        setDebugListener();

        DummyAccount dummyAccount = getDummyResource().getAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJack = provisioningService.getObject(ShadowType.class, accountShadowJackDummyOid, null, task, result);
        change.setShadowedResourceObject(accountShadowJack);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        displayDumpable("SENDING CHANGE NOTIFICATION", change);

        // WHEN
        synchronizationService.notifyChange(change, task, result);

        // THEN
        LensContext<UserType> context = cleanDebugListener();

        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertEquals("Unexpected number of executed deltas", 1, context.getFocusContext().getExecutedDeltas().size());
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getExecutedDeltas().iterator().next().getObjectDelta();
//        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        assertEquals("Unexpected number of modifications in user secondary delta", 7, userSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyDelete(userSecondaryDelta, UserType.F_COST_CENTER, "999");

        ProjectionContextKey rat = getDefaultAccountKey();
        LensProjectionContext accCtx = context.findProjectionContextByKeyExact(rat);
        assertNotNull("No account sync context for " + rat, accCtx);

        PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());
        PrismAsserts.assertNoDelta("Unexpected account secondary delta", accCtx.getSecondaryDelta());

        assertEquals("Wrong detected situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationDetected());

        assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertNull("Unexpected used constCenter", user.asObjectable().getCostCenter());

        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);

        assertSuccess(result);
    }

    /**
     * Sending empty delta, this is what reconciliation does.
     */
    @Test
    public void test030Reconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        setDebugListener();

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJack = provisioningService.getObject(ShadowType.class, accountShadowJackDummyOid, null, task, result);
        change.setShadowedResourceObject(accountShadowJack);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANNEL_DISCOVERY_URI);

        // WHEN
        synchronizationService.notifyChange(change, task, result);

        // THEN
        LensContext<UserType> context = cleanDebugListener();

        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly("user secondary delta", context.getFocusContext().getSecondaryDelta());

        ProjectionContextKey rat = getDefaultAccountKey();
        LensProjectionContext accCtx = context.findProjectionContextByKeyExact(rat);
        assertNotNull("No account sync context for " + rat, accCtx);

        PrismAsserts.assertNoDelta("account primary delta", accCtx.getPrimaryDelta());
        PrismAsserts.assertNoDelta("account secondary delta", accCtx.getSecondaryDelta());

        assertEquals("Wrong detected situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationDetected());

        assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());

        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);

        assertSuccess(result);
    }

    @Test
    public void test039DeletedAccountJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountShadowJackDummyOid, null, result);
        ShadowAsserter.forShadow(shadowRepo, "repo shadow before")
                .assertLive()
                .assertIteration(0)
                .assertIterationToken("")
                .assertSynchronizationSituation(SynchronizationSituationType.LINKED);

        setDebugListener();

        getDummyResource().deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);

        PrismObject<ShadowType> shadow; // TODO = getShadowModelNoFetch(accountShadowJackDummyOid);

        shadowRepo = repositoryService.getObject(ShadowType.class, accountShadowJackDummyOid, null, result);
        ShadowAsserter.forShadow(shadowRepo, "repo shadow after noFetch")
                // This is noFetch. Provisioning won't figure out that the shadow is dead (yet).
                .assertLive()
                .assertIteration(0)
                .assertIterationToken("")
                .assertSynchronizationSituation(SynchronizationSituationType.LINKED);

        // In fact, it is responsibility of provisioning to mark shadow dead before invoking sync
        // service. This is unit test, therefore we have to simulate behavior of provisioning here.
        markShadowTombstone(accountShadowJackDummyOid);

        shadowRepo = repositoryService.getObject(ShadowType.class, accountShadowJackDummyOid, null, result);
        ShadowAsserter.forShadow(shadowRepo, "repo shadow before synchronization")
                .assertTombstone()
                .assertIteration(0)
                .assertIterationToken("")
                .assertSynchronizationSituation(SynchronizationSituationType.LINKED);

        // Once again, to have fresh data
        shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        ShadowAsserter.forShadow(shadowRepo, "repo shadow before synchronization (noFetch)")
                .assertTombstone()
                .assertIteration(0)
                .assertIterationToken("")
                .assertSynchronizationSituation(SynchronizationSituationType.LINKED);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setShadowedResourceObject(shadow);
        change.setResource(getDummyResourceObject());
        ObjectDelta<ShadowType> syncDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountShadowJackDummyOid);
        change.setObjectDelta(syncDelta);
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN
        when();
        synchronizationService.notifyChange(change, task, result);

        // THEN
        then();
        assertSuccess(result);
        LensContext<UserType> context = cleanDebugListener();

        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly("user secondary delta", context.getFocusContext().getSecondaryDelta());

        ProjectionContextKey key = getAccountGoneKey();
        LensProjectionContext accCtx = context.findProjectionContextByKeyExact(key);
        assertNotNull("No account sync context for " + key, accCtx);
        assertEquals("Wrong detected situation in context", SynchronizationSituationType.DELETED, accCtx.getSynchronizationSituationDetected());

        PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());

        UserAsserter.forUser(context.getFocusContext().getObjectOld(), "old focus in lens context)")
                .assertLinked(accountShadowJackDummyOid);

        assertUserAfter(USER_JACK_OID)
                .links()
                .singleAny()
                .assertOid(accountShadowJackDummyOid);

        assertRepoShadow(accountShadowJackDummyOid)
                .assertTombstone()
                .assertIteration(0)
                .assertIterationToken("")
                .assertSynchronizationSituation(SynchronizationSituationType.DELETED);

        // Cleanup
        unlinkUser(USER_JACK_OID, accountShadowJackDummyOid);
        repositoryService.deleteObject(ShadowType.class, accountShadowJackDummyOid, result);
    }

    @NotNull
    private ProjectionContextKey getAccountGoneKey() {
        return ProjectionContextKey.forKnownResource(
                getDummyResourceObject().getOid(),
                ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT),
                null,
                0,
                true);
    }

    /**
     * Calypso is protected, no reaction should be applied.
     */
    @Test
    public void test050AddedAccountCalypso() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        setDebugListener();

        PrismObject<ShadowType> accountShadowCalypso = repoAddObjectFromFile(ACCOUNT_SHADOW_CALYPSO_DUMMY_FILE, result);
        accountShadowCalypsoDummyOid = accountShadowCalypso.getOid();
        prepareShadow(accountShadowCalypso, task, result);
        assertNotNull("No oid in shadow", accountShadowCalypso.getOid());
        // Make sure that it is properly marked as protected. This is what provisioning would normally do
        accountShadowCalypso.asObjectable().setProtectedObject(true);

        DummyAccount dummyAccount = new DummyAccount();
        dummyAccount.setName(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        dummyAccount.setPassword("h1ghS3AS");
        dummyAccount.setEnabled(true);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Calypso");
        getDummyResource().addAccount(dummyAccount);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setShadowedResourceObject(accountShadowCalypso);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN
        when();
        synchronizationService.notifyChange(change, task, result);

        // THEN
        then();
        LensContext<UserType> context = cleanDebugListener();

        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNull("Unexpected lens context", context);

        PrismObject<UserType> userCalypso = findUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        assertNull("Unexpected user " + userCalypso, userCalypso);

        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowCalypsoDummyOid);
        assertSituation(shadow, null);

        assertSuccess(result);
    }

    /**
     * Calypso is protected, no reaction should be applied.
     */
    @Test
    public void test051CalypsoRecon() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        setDebugListener();

        // Let's make this a bit more interesting by setting up a fake situation in the shadow
        ObjectDelta<ShadowType> objectDelta = createModifyAccountShadowReplaceDelta(accountShadowCalypsoDummyOid,
                getDummyResourceObject(), ShadowType.F_SYNCHRONIZATION_SITUATION, SynchronizationSituationType.DISPUTED);
        repositoryService.modifyObject(ShadowType.class, accountShadowCalypsoDummyOid, objectDelta.getModifications(), result);

        PrismObject<ShadowType> accountShadowCalypso = getShadowModelNoFetch(accountShadowCalypsoDummyOid);
        // Make sure that it is properly marked as protected. This is what provisioning would normally do
        accountShadowCalypso.asObjectable().setProtectedObject(true);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setShadowedResourceObject(accountShadowCalypso);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        displayDumpable("Change notification", change);

        // WHEN
        when();
        synchronizationService.notifyChange(change, task, result);

        // THEN
        then();
        LensContext<UserType> context = cleanDebugListener();

        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNull("Unexpected lens context", context);

        PrismObject<UserType> userCalypso = findUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        assertNull("Unexpected user " + userCalypso, userCalypso);

        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowCalypsoDummyOid);
        assertSituation(shadow, SynchronizationSituationType.DISPUTED);

        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test100AddedAccountJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertUserBefore(USER_JACK_OID)
                .assertLiveLinks(0)
                .assertRelatedLinks(0);
        setDebugListener();

        PrismObject<ShadowType> accountShadowJack = repoAddObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILE, result);
        accountShadowJackDummyOid = accountShadowJack.getOid();
        prepareShadow(accountShadowJack, task, result);
        assertNotNull("No oid in shadow", accountShadowJack.getOid());
        DummyAccount dummyAccount = new DummyAccount();
        dummyAccount.setName(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.setPassword("deadMenTellNoTales");
        dummyAccount.setEnabled(true);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
        getDummyResource().addAccount(dummyAccount);

        displayDumpable("Dummy resource before", getDummyResource());

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setShadowedResourceObject(accountShadowJack);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN
        when();
        synchronizationService.notifyChange(change, task, result);

        // THEN
        then();
        assertSuccess(result);
        LensContext<UserType> context = cleanDebugListener();

        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
                ActivationStatusType.ENABLED);

        ProjectionContextKey key = getDefaultAccountKey();
        LensProjectionContext accCtx = context.findProjectionContextByKeyExact(key);
        assertNotNull("No account sync context for " + key, accCtx);
        assertEquals("Wrong detected situation in context", SynchronizationSituationType.UNLINKED, accCtx.getSynchronizationSituationDetected());
        assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationResolved());

        PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());
        //it this really expected?? delta was already executed, should we expect it in the secondary delta?

        assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());

        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        assertLiveLinks(userAfter, 1);
        assertLinked(userAfter, shadow);
    }

    private @NotNull ProjectionContextKey getDefaultAccountKey() {
        return ProjectionContextKey.forKnownResource(
                getDummyResourceObject().getOid(),
                ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT),
                null);
    }

    /**
     * Delete the account but also the shadow in the repo. The system should work well.
     */
    @Test
    public void test199DeletedAccountJackTotal() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        setDebugListener();

        getDummyResource().deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setShadowedResourceObject(shadow);
        change.setResource(getDummyResourceObject());
        ObjectDelta<ShadowType> syncDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountShadowJackDummyOid);
        change.setObjectDelta(syncDelta);
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        repositoryService.deleteObject(ShadowType.class, accountShadowJackDummyOid, result);

        // WHEN
        when();
        synchronizationService.notifyChange(change, task, result);

        // THEN
        then();
        assertSuccess(result, 1);
        LensContext<UserType> context = cleanDebugListener();

        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNotNull("No focus context", context.getFocusContext());
        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly("user secondary delta", context.getFocusContext().getSecondaryDelta());

        ProjectionContextKey key = getAccountGoneKey();
        LensProjectionContext accCtx = context.findProjectionContextByKeyExact(key);
        assertNotNull("No account sync context for " + key, accCtx);
        assertEquals("Wrong detected situation in context", SynchronizationSituationType.DELETED, accCtx.getSynchronizationSituationDetected());

        PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());

        assertUserAfter(USER_JACK_OID)
                .assertLiveLinks(0);

        assertNoObject(ShadowType.class, accountShadowJackDummyOid, task, result);
    }

    /**
     * Schema violation error on the connector while doing synchronization.
     * While we cannot really execute any connector operation (e.g. we cannot
     * reconcile), we still want the shadow linked to the user.
     * MID-3787
     */
    @Test
    public void test200AddedAccountJackSchemaViolation() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        assertLiveLinks(userBefore, 0);
        setDebugListener();

        PrismObject<ShadowType> accountShadowJack = repoAddObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILE, result);
        accountShadowJackDummyOid = accountShadowJack.getOid();
        prepareShadow(accountShadowJack, task, result);
        assertNotNull("No oid in shadow", accountShadowJack.getOid());
        DummyAccount dummyAccount = new DummyAccount();
        dummyAccount.setName(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.setPassword("deadMenTellNoTales");
        dummyAccount.setEnabled(true);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
        getDummyResource().addAccount(dummyAccount);

        displayDumpable("Dummy resource before", getDummyResource());

        getDummyResource().setModifyBreakMode(BreakMode.SCHEMA);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setShadowedResourceObject(accountShadowJack);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN
        when();
        synchronizationService.notifyChange(change, task, result);

        // THEN
        then();
        getDummyResource().resetBreakMode();
        assertPartialError(result);

        LensContext<UserType> context = cleanDebugListener();
        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
                ActivationStatusType.ENABLED);

        ProjectionContextKey key = getDefaultAccountKey();
        LensProjectionContext accCtx = context.findProjectionContextByKeyExact(key);
        assertNotNull("No account sync context for " + key, accCtx);
        assertEquals("Wrong detected situation in context", SynchronizationSituationType.UNLINKED, accCtx.getSynchronizationSituationDetected());
        assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationResolved());

        PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());

        assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        assertLiveLinks(userAfter, 1);

        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertSituation(shadow, SynchronizationSituationType.LINKED);
        assertLinked(userAfter, shadow);
    }

    /**
     * Pretend that the account is updated. It is already linked.
     * There is still schema violation error on the connector while doing
     * synchronization.
     * The shadow should still be linked.
     * MID-3787
     */
    @Test
    public void test202UpdatedAccountJackSchemaViolation() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        assertLiveLinks(userBefore, 1);
        setDebugListener();

        displayDumpable("Dummy resource before", getDummyResource());

        getDummyResource().setModifyBreakMode(BreakMode.SCHEMA);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJackBefore = getShadowModelNoFetch(accountShadowJackDummyOid);
        change.setShadowedResourceObject(accountShadowJackBefore);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN
        when();
        synchronizationService.notifyChange(change, task, result);

        // THEN
        then();
        getDummyResource().resetBreakMode();
        assertPartialError(result);

        LensContext<UserType> context = cleanDebugListener();
        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
                ActivationStatusType.ENABLED);

        ProjectionContextKey key = getDefaultAccountKey();
        LensProjectionContext accCtx = context.findProjectionContextByKeyExact(key);
        assertNotNull("No account sync context for " + key, accCtx);
        assertEquals("Wrong detected situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationDetected());
        assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationResolved());

        PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());

        assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJackDummyOid);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        assertLiveLinks(userAfter, 1);

        PrismObject<ShadowType> shadowAfter = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertSituation(shadowAfter, SynchronizationSituationType.LINKED);
        assertLinked(userAfter, shadowAfter);

    }

    /**
     * Assign dummy account. The account already exists, the shadow exists, it is even linked.
     * But up until now it was not really reconciled because there was an error. But now everything
     * is fixed.
     */
    @Test
    public void test210AssignJackDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setDebugListener();
        getDummyResource().resetBreakMode();

        // WHEN
        when();
        assignAccount(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        LensContext<UserType> context = cleanDebugListener();
        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNotNull("Missing user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
                ActivationStatusType.ENABLED);

        ProjectionContextKey key = getDefaultAccountKey();
        LensProjectionContext accCtx = context.findProjectionContextByKeyExact(key);
        assertNotNull("No account sync context for " + key, accCtx);
        assertNull("Wrong detected situation in context", accCtx.getSynchronizationSituationDetected());
        assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationResolved());

        PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());

        assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJackDummyOid);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        assertLiveLinks(userAfter, 1);

        PrismObject<ShadowType> shadowAfter = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertSituation(shadowAfter, SynchronizationSituationType.LINKED);
        assertLinked(userAfter, shadowAfter);
    }

    /**
     * Add another account .. to prepare for next tests.
     */
    @Test
    public void test212AssignJackDummyLimited() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setDebugListener();
        getDummyResource().resetBreakMode();

        // WHEN
        when();
        assignAccount(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_LIMITED.oid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        LensContext<UserType> context = cleanDebugListener();
        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNotNull("Missing user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
                ActivationStatusType.ENABLED);

        ProjectionContextKey key = getDefaultAccountKey();
        LensProjectionContext accCtx = context.findProjectionContextByKeyExact(key);
        assertNotNull("No account sync context for " + key, accCtx);
        assertNull("Wrong detected situation in context", accCtx.getSynchronizationSituationDetected());
        assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationResolved());

        PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());

        assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJackDummyOid);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        assertLiveLinks(userAfter, 2);
        accountShadowJackDummyLimitedOid = assertAccount(userAfter, RESOURCE_DUMMY_LIMITED.oid);

        PrismObject<ShadowType> shadowDummyAfter = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertSituation(shadowDummyAfter, SynchronizationSituationType.LINKED);
        assertLinked(userAfter, shadowDummyAfter);

        PrismObject<ShadowType> shadowDummyLimitedAfter = getShadowModelNoFetch(accountShadowJackDummyLimitedOid);
        assertSituation(shadowDummyLimitedAfter, SynchronizationSituationType.LINKED);
    }

    /**
     * Limited dummy resource has limited propagation (limitPropagation=true).
     * Therefore it should only read/write to its own resource.
     * Ruin both jack's accounts, so reconciliation would normally try to
     * fix both accounts. The initiate sync from limited dummy. As limited
     * dummy should only care about itself, it should not fix the other
     * dummy account. Also, is should not even read full dummy account.
     * MID-3805
     */
    @Test
    public void test214UpdatedAccountJackLimited() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        assertLiveLinks(userBefore, 2);
        getDummyResource().resetBreakMode();
        setDebugListener();

        getDummyResource().getAccountByName(USER_JACK_USERNAME)
                .replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Dummyland");
        RESOURCE_DUMMY_LIMITED.controller.getDummyResource().getAccountByName(USER_JACK_USERNAME)
                .replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Limitistan");

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_OID);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_LIMITED.oid);

        // Sleeping here so that "get" operation below will cache data with timestamp that is different from the one
        // in the invalidation record.
        MiscUtil.sleepCatchingInterruptedException(50L);

        displayDumpable("Dummy resource before", getDummyResource());

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowLimitedJackBefore = getShadowModel(accountShadowJackDummyLimitedOid);
        // This is needed to avoid fetching the full shadow right after starting the synchronization
        accountShadowLimitedJackBefore.asObjectable().setContentDescription(ShadowContentDescriptionType.FROM_RESOURCE_COMPLETE);
        change.setShadowedResourceObject(accountShadowLimitedJackBefore);
        change.setResource(getDummyResourceObject(RESOURCE_DUMMY_LIMITED.name));
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);
        // Make sure that default dummy resource is not touched
        getDummyResource().setBreakMode(BreakMode.ASSERTION_ERROR);

        // WHEN
        when();
        synchronizationService.notifyChange(change, task, result);

        // THEN
        then();
        assertSuccess(result);

        getDummyResource().resetBreakMode();

        LensContext<UserType> context = cleanDebugListener();
        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
                ActivationStatusType.ENABLED);

        ProjectionContextKey keyDummy = getDefaultAccountKey();
        LensProjectionContext accCtxDummy = context.findProjectionContextByKeyExact(keyDummy);
        assertNotNull("No account sync context for " + keyDummy, accCtxDummy);
        PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtxDummy.getPrimaryDelta());
        assertFalse("Wrong fullShadow for " + keyDummy, accCtxDummy.isFullShadow());
        assertFalse("Wrong canProject for " + keyDummy, accCtxDummy.isCanProject());

        ProjectionContextKey keyDummyLimited =
                ProjectionContextKey.classified(
                        RESOURCE_DUMMY_LIMITED.oid, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT, null);
        LensProjectionContext accCtxDummyLimited = context.findProjectionContextByKeyExact(keyDummyLimited);
        assertNotNull("No account sync context for " + keyDummyLimited, accCtxDummyLimited);

        // This is tricky. In both cases (caching, not caching) the shadow is initially marked as "full".
        // In both cases, it is downgraded to "shadow only" after the execution of computed deltas.
        // But in the "not caching" case, it is loaded again, because of the reconciliation (although
        // this could be perhaps optimized some day).
        assertThat(accCtxDummyLimited.isFullShadow())
                .as("fullShadow flag in " + keyDummyLimited)
                .isEqualTo(!InternalsConfig.isShadowCachingFullByDefault());

        assertTrue("Wrong canProject for " + keyDummyLimited, accCtxDummyLimited.isCanProject());

        assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJackDummyOid);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 5);
        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 1);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        assertLiveLinks(userAfter, 2);

        displayDumpable("Dummy resource after", getDummyResource());

        PrismObject<ShadowType> shadowDummyAfter = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertSituation(shadowDummyAfter, SynchronizationSituationType.LINKED);
        assertLinked(userAfter, shadowDummyAfter);

        PrismObject<ShadowType> shadowDummyLimitedAfter = getShadowModelNoFetch(accountShadowJackDummyLimitedOid);
        assertSituation(shadowDummyLimitedAfter, SynchronizationSituationType.LINKED);
        assertLinked(userAfter, shadowDummyLimitedAfter);

        assertDummyAccountAttribute(null, USER_JACK_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Dummyland");
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIMITED.name, USER_JACK_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Caribbean");
    }

    @Test
    public void test300AddedGroupPirates() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        setDebugListener();
        getDummyResource().resetBreakMode();

        PrismObject<ShadowType> shadowPirates = repoAddObjectFromFile(SHADOW_PIRATES_DUMMY_FILE, result);
        prepareShadow(shadowPirates, task, result);
        assertNotNull("No oid in shadow", shadowPirates.getOid());
        DummyGroup dummyGroup = new DummyGroup();
        dummyGroup.setName(GROUP_PIRATES_DUMMY_NAME);
        dummyGroup.setEnabled(true);
        dummyGroup.addAttributeValues(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, "Scurvy Pirates");
        getDummyResource().addGroup(dummyGroup);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setShadowedResourceObject(shadowPirates);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANNEL_LIVE_SYNC_URI);

        // WHEN
        when();
        synchronizationService.notifyChange(change, task, result);

        // THEN
        when();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        LensContext<UserType> context = cleanDebugListener();
        displayDumpable("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNotNull("No focus primary delta", context.getFocusContext().getPrimaryDelta());
        assertFalse("No executed focus deltas", context.getFocusContext().getExecutedDeltas().isEmpty());

        ProjectionContextKey key =
                ProjectionContextKey.classified(
                        getDummyResourceObject().getOid(), ShadowKindType.ENTITLEMENT, INTENT_GROUP, null);
        LensProjectionContext projCtx = context.findProjectionContextByKeyExact(key);
        assertNotNull("No projection sync context for " + key, projCtx);
        assertEquals("Wrong detected situation in context", SynchronizationSituationType.UNMATCHED, projCtx.getSynchronizationSituationDetected());
        assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, projCtx.getSynchronizationSituationResolved());

        PrismAsserts.assertNoDelta("Unexpected projection primary delta", projCtx.getPrimaryDelta());

        assertLinked(RoleType.class, context.getFocusContext().getOid(), shadowPirates.getOid());

        PrismObject<ShadowType> shadow = getShadowModelNoFetch(shadowPirates.getOid());
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);

    }

    private void prepareShadow(PrismObject<ShadowType> shadow, Task task, OperationResult result) throws CommonException {
        provisioningService.applyDefinition(shadow, task, result);
        provisioningService.determineShadowState(shadow, task, result);
        provisioningService.updateShadowMarksAndPolicies(shadow, false, task, result);
    }

    @Test
    public void test400BrokenCorrelator() throws Exception {
        given("dummy account exists on a resource with broken correlator");
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountName = "test400";
        RESOURCE_DUMMY_BROKEN.controller.addAccount(accountName);

        when("import task is run");
        TASK_IMPORT_DUMMY_BROKEN.init(this, task, result);
        waitForTaskCloseOrSuspend(TASK_IMPORT_DUMMY_BROKEN.oid, 20000);

        then("error is correctly recorded both in task and in shadow");

        // Adapt this if the details of error handling in synchronization service are changed.
        String expectedMessage = "Error occurred during resource object shadow owner lookup, reason: Circuit is broken";

        TASK_IMPORT_DUMMY_BROKEN.assertAfter()
                .rootActivityState()
                .progress()
                .assertCommitted(0, 1, 0)
                .end()
                .itemProcessingStatistics()
                .assertTotalCounts(0, 1, 0)
                .assertLastFailureObjectName(accountName)
                .assertLastFailureMessage(expectedMessage);

        PrismObject<ShadowType> shadow = findShadowByPrismName(accountName, RESOURCE_DUMMY_BROKEN.get(), result);
        assertShadowAfter(shadow)
                .assertCorrelationSituation(CorrelationSituationType.ERROR)
                .assertHasComplexOperationExecutionFailureWithMessage(TASK_IMPORT_DUMMY_BROKEN.oid, expectedMessage);
    }

    /**
     * Checks that the correlation is repeatable, i.e. that the (previous) correlation does not prevent the current one
     * from proceeding correctly - typically, when the situation or configuration changes.
     *
     * MID-8223
     */
    @Test
    public void test410RepeatedCorrelation() throws Exception {
        given("dummy account exists");
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountName = "test410";
        RESOURCE_DUMMY_LIMITED.controller.addAccount(accountName);

        when("account is imported (the first time)");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_LIMITED.oid)
                .withNameValue(accountName)
                .execute(result);

        then("user and the shadow is there, shadow has 'no owner' correlation state");
        // @formatter:on
        var userOid = assertUserAfterByUsername(accountName)
                .assertLiveLinks(1)
                .singleLink()
                .resolveTarget()
                .display()
                .assertValues(
                        SchemaConstants.CORRELATION_SITUATION_PATH,
                        CorrelationSituationType.NO_OWNER)
                .end()
                .end()
                .getObjectable().getOid();
        // @formatter:off

        // This simulates the situation that the correlation rule is fixed, so instead of "no owner" it should produce "unlinked"
        when("user->shadow link disappears (this simulates fixing the correlation rule)");
        repositoryService.modifyObject(
                UserType.class,
                userOid,
                deltaFor(UserType.class)
                        .item(UserType.F_LINK_REF)
                        .replace()
                        .asItemDeltas(),
                result);

        and("account is re-imported");
        var taskOid = importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_LIMITED.oid)
                .withNameValue(accountName)
                .execute(result);

        then("task is OK");
        assertTask(taskOid, "after reimport")
                .display()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(1, 0, 0);

        // @formatter:on
        assertUser(userOid, "after reimport")
                .assertLiveLinks(1)
                .singleLink()
                .resolveTarget()
                .display()
                .assertValues(
                        SchemaConstants.CORRELATION_SITUATION_PATH,
                        CorrelationSituationType.EXISTING_OWNER)
                .end()
                .end();
        // @formatter:off
    }

    private void setDebugListener() {
        mockListener = new MockLensDebugListener();
        DiagnosticContextManager manager = new DiagnosticContextManager() {

            @Override
            public DiagnosticContext createNewContext() {
                return mockListener;
            }

            @Override
            public void processFinishedContext(DiagnosticContext ctx) {
            }

        };
        clockworkMedic.setDiagnosticContextManager(manager);
        DiagnosticContextHolder.push(mockListener);
    }

    private LensContext<UserType> cleanDebugListener() {
        DiagnosticContextHolder.pop();
        return mockListener.getLastSyncContext();
    }
}
