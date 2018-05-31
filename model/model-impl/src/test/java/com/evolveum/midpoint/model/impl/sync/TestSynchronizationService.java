/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.model.impl.sync;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertFalse;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.model.api.util.DiagnosticContextManager;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ClockworkMedic;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.util.mock.MockLensDebugListener;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.DiagnosticContext;
import com.evolveum.midpoint.schema.util.DiagnosticContextHolder;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSynchronizationService extends AbstractInternalModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/sync");

	public static final File SHADOW_PIRATES_DUMMY_FILE = new File(TEST_DIR, "shadow-pirates-dummy.xml");
	public static final String GROUP_PIRATES_DUMMY_NAME = "pirates";

	private static final String INTENT_GROUP = "group";

	@Autowired SynchronizationService synchronizationService;
	@Autowired Clockwork clockwork;
	@Autowired ClockworkMedic clockworkMedic;

	private String accountShadowJackDummyOid = null;
	private String accountShadowCalypsoDummyOid = null;
	
	private MockLensDebugListener mockListener;

	@Test
    public void test010AddedAccountJack() throws Exception {
		final String TEST_NAME = "test010AddedAccountJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        setDebugListener();

        PrismObject<ShadowType> accountShadowJack = repoAddObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILE, result);
        accountShadowJackDummyOid = accountShadowJack.getOid();
        provisioningService.applyDefinition(accountShadowJack, task, result);
        assertNotNull("No oid in shadow", accountShadowJack.getOid());
        DummyAccount dummyAccount = new DummyAccount();
        dummyAccount.setName(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.setPassword("deadMenTellNoTales");
        dummyAccount.setEnabled(true);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
		getDummyResource().addAccount(dummyAccount);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(accountShadowJack);
        change.setResource(getDummyResourceObject());

		// WHEN
        synchronizationService.notifyChange(change, task, result);

        // THEN
        LensContext<UserType> context = cleanDebugListener();
        
        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
        		ActivationStatusType.ENABLED);

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(getDummyResourceObject().getOid(), ShadowKindType.ACCOUNT, null);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.UNLINKED, accCtx.getSynchronizationSituationDetected());
		assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationResolved());

		PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());
		//it this really expected?? delta was already executed, should we expect it in the secondary delta?
//		assertNotNull("Missing account secondary delta", accCtx.getSecondaryDelta());
//		assertIterationDelta(accCtx.getSecondaryDelta(), 0, "");

		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());

		PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);

        assertSuccess(result);        
	}
	
	@Test
    public void test020ModifyLootAbsolute() throws Exception {
		final String TEST_NAME = "test020ModifyLootAbsolute";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        setDebugListener();

        DummyAccount dummyAccount = getDummyResource().getAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, "999");

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJack = provisioningService.getObject(ShadowType.class, accountShadowJackDummyOid, null, task, result);
        change.setCurrentShadow(accountShadowJack);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        synchronizationService.notifyChange(change, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        LensContext<UserType> context = cleanDebugListener();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertEquals("Unexpected number of executed deltas", 1, context.getFocusContext().getExecutedDeltas().size());
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getExecutedDeltas().iterator().next().getObjectDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        assertEquals("Unexpected number of modifications in user secondary delta", 7, userSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_COST_CENTER, "999");

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(getDummyResourceObject().getOid(), ShadowKindType.ACCOUNT, null);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);

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
		final String TEST_NAME = "test021ModifyLootAbsoluteEmpty";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        setDebugListener();

        DummyAccount dummyAccount = getDummyResource().getAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJack = provisioningService.getObject(ShadowType.class, accountShadowJackDummyOid, null, task, result);
        change.setCurrentShadow(accountShadowJack);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI);

        display("SENDING CHANGE NOTIFICATION", change);

		// WHEN
        synchronizationService.notifyChange(change, task, result);

        // THEN
        LensContext<UserType> context = cleanDebugListener();;

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertEquals("Unexpected number of executed deltas", 1, context.getFocusContext().getExecutedDeltas().size());
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getExecutedDeltas().iterator().next().getObjectDelta();
//        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        assertEquals("Unexpected number of modifications in user secondary delta", 7, userSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(userSecondaryDelta, UserType.F_COST_CENTER);

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(getDummyResourceObject().getOid(), ShadowKindType.ACCOUNT, null);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);

		PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());
		PrismAsserts.assertNoDelta("Unexpected account secondary delta", accCtx.getSecondaryDelta());

		assertEquals("Wrong detected situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationDetected());

		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());

		PrismObject<UserType> user = getUser(USER_JACK_OID);
		assertEquals("Unexpected used constCenter", null, user.asObjectable().getCostCenter());

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
		final String TEST_NAME = "test030Reconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        setDebugListener();

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJack = provisioningService.getObject(ShadowType.class, accountShadowJackDummyOid, null, task, result);
        change.setCurrentShadow(accountShadowJack);
        change.setResource(getDummyResourceObject());
        change.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_DISCOVERY_URI);

		// WHEN
        synchronizationService.notifyChange(change, task, result);

        // THEN
        LensContext<UserType> context = cleanDebugListener();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly("user secondary delta", context.getFocusContext().getSecondaryDelta());

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(getDummyResourceObject().getOid(),
        		ShadowKindType.ACCOUNT, null);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);

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
		final String TEST_NAME = "test039DeletedAccountJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountShadowJackDummyOid, null, result);
        assertIteration(shadowRepo, 0, "");
        assertSituation(shadowRepo, SynchronizationSituationType.LINKED);
        setDebugListener();

        getDummyResource().deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);

        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);

        shadowRepo = repositoryService.getObject(ShadowType.class, accountShadowJackDummyOid, null, result);
        assertIteration(shadowRepo, 0, "");
        assertSituation(shadowRepo, SynchronizationSituationType.LINKED);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(shadow);
        change.setResource(getDummyResourceObject());
        ObjectDelta<ShadowType> syncDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountShadowJackDummyOid, prismContext);
		change.setObjectDelta(syncDelta);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
        synchronizationService.notifyChange(change, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        LensContext<UserType> context = cleanDebugListener();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly("user secondary delta", context.getFocusContext().getSecondaryDelta());

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(getDummyResourceObject().getOid(),
        		ShadowKindType.ACCOUNT, null, true);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.DELETED, accCtx.getSynchronizationSituationDetected());

		PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());

		assertNotLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJackDummyOid);

		shadowRepo = repositoryService.getObject(ShadowType.class, accountShadowJackDummyOid, null, result);
        assertIteration(shadowRepo, 0, "");
        assertSituation(shadowRepo, SynchronizationSituationType.DELETED);

        result.computeStatus();
        TestUtil.assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		assertLinks(userAfter, 0);

        repositoryService.deleteObject(ShadowType.class, accountShadowJackDummyOid, result);
	}

	/**
	 * Calypso is protected, no reaction should be applied.
	 */
	@Test
    public void test050AddedAccountCalypso() throws Exception {
		final String TEST_NAME = "test050AddedAccountCalypso";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        setDebugListener();

        PrismObject<ShadowType> accountShadowCalypso = repoAddObjectFromFile(ACCOUNT_SHADOW_CALYPSO_DUMMY_FILE, result);
        accountShadowCalypsoDummyOid = accountShadowCalypso.getOid();
        provisioningService.applyDefinition(accountShadowCalypso, task, result);
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
        change.setCurrentShadow(accountShadowCalypso);
        change.setResource(getDummyResourceObject());

		// WHEN
        synchronizationService.notifyChange(change, task, result);

        // THEN
        LensContext<UserType> context = cleanDebugListener();

        display("Resulting context (as seen by debug listener)", context);
        assertNull("Unexpected lens context", context);

		PrismObject<UserType> userCalypso = findUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
		assertNull("Unexpected user "+userCalypso, userCalypso);

		PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowCalypsoDummyOid);
        assertSituation(shadow, null);

        result.computeStatus();
        TestUtil.assertSuccess(result);
	}

	/**
	 * Calypso is protected, no reaction should be applied.
	 */
	@Test
    public void test051CalypsoRecon() throws Exception {
		final String TEST_NAME = "test051CalypsoRecon";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        setDebugListener();

        // Lets make this a bit more interesting by setting up a fake situation in the shadow
        ObjectDelta<ShadowType> objectDelta = createModifyAccountShadowReplaceDelta(accountShadowCalypsoDummyOid,
        		getDummyResourceObject(), new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION), SynchronizationSituationType.DISPUTED);
        repositoryService.modifyObject(ShadowType.class, accountShadowCalypsoDummyOid, objectDelta.getModifications(), result);

        PrismObject<ShadowType> accountShadowCalypso = getShadowModelNoFetch(accountShadowCalypsoDummyOid);
        // Make sure that it is properly marked as protected. This is what provisioning would normally do
        accountShadowCalypso.asObjectable().setProtectedObject(true);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(accountShadowCalypso);
        change.setResource(getDummyResourceObject());

        display("Change notification", change);

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        synchronizationService.notifyChange(change, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        LensContext<UserType> context = cleanDebugListener();

        display("Resulting context (as seen by debug listener)", context);
        assertNull("Unexpected lens context", context);

		PrismObject<UserType> userCalypso = findUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
		assertNull("Unexpected user "+userCalypso, userCalypso);

		PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowCalypsoDummyOid);
        assertSituation(shadow, SynchronizationSituationType.DISPUTED);

        result.computeStatus();
        TestUtil.assertSuccess(result);
	}

	@Test
    public void test100AddedAccountJack() throws Exception {
		final String TEST_NAME = "test100AddedAccountJack";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		assertLinks(userBefore, 0);
		setDebugListener();

        PrismObject<ShadowType> accountShadowJack = repoAddObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILE, result);
        accountShadowJackDummyOid = accountShadowJack.getOid();
        provisioningService.applyDefinition(accountShadowJack, task, result);
        assertNotNull("No oid in shadow", accountShadowJack.getOid());
        DummyAccount dummyAccount = new DummyAccount();
        dummyAccount.setName(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.setPassword("deadMenTellNoTales");
        dummyAccount.setEnabled(true);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
        getDummyResource().addAccount(dummyAccount);

		display("Dummy resource before", getDummyResource());

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(accountShadowJack);
        change.setResource(getDummyResourceObject());

		// WHEN
        displayWhen(TEST_NAME);
        synchronizationService.notifyChange(change, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        LensContext<UserType> context = cleanDebugListener();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
        		ActivationStatusType.ENABLED);

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(getDummyResourceObject().getOid(), ShadowKindType.ACCOUNT, null);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.UNLINKED, accCtx.getSynchronizationSituationDetected());
		assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationResolved());

		PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());
		//it this really expected?? delta was already executed, should we expect it in the secondary delta?
//		assertNotNull("Missing account secondary delta", accCtx.getSecondaryDelta());
//		assertIterationDelta(accCtx.getSecondaryDelta(), 0, "");

		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());

		PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		assertLinks(userAfter, 1);
		assertLinked(userAfter, shadow);
	}

	/**
	 * Delete the account but also the shadow in the repo. The system should work well.
	 */
	@Test
    public void test199DeletedAccountJackTotal() throws Exception {
		final String TEST_NAME = "test199DeletedAccountJackTotal";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        setDebugListener();

        getDummyResource().deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        PrismObject<ShadowType> shadow = getShadowModelNoFetch(accountShadowJackDummyOid);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(shadow);
        change.setResource(getDummyResourceObject());
        ObjectDelta<ShadowType> syncDelta = ObjectDelta.createDeleteDelta(ShadowType.class, accountShadowJackDummyOid, prismContext);
		change.setObjectDelta(syncDelta);

		repositoryService.deleteObject(ShadowType.class, accountShadowJackDummyOid, result);

		// WHEN
        synchronizationService.notifyChange(change, task, result);

        // THEN
        LensContext<UserType> context = cleanDebugListener();

        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNotNull("No focus context", context.getFocusContext());
        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly("user secondary delta", context.getFocusContext().getSecondaryDelta());

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(getDummyResourceObject().getOid(),
        		ShadowKindType.ACCOUNT, null, true);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.DELETED, accCtx.getSynchronizationSituationDetected());

		PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());

		assertNotLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJackDummyOid);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		assertLinks(userAfter, 0);

        result.computeStatus();
        display("Final result", result);
        TestUtil.assertSuccess(result,1);

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
		final String TEST_NAME = "test200AddedAccountJackSchemaViolation";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		assertLinks(userBefore, 0);
		setDebugListener();

        PrismObject<ShadowType> accountShadowJack = repoAddObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILE, result);
        accountShadowJackDummyOid = accountShadowJack.getOid();
        provisioningService.applyDefinition(accountShadowJack, task, result);
        assertNotNull("No oid in shadow", accountShadowJack.getOid());
        DummyAccount dummyAccount = new DummyAccount();
        dummyAccount.setName(ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.setPassword("deadMenTellNoTales");
        dummyAccount.setEnabled(true);
        dummyAccount.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
        getDummyResource().addAccount(dummyAccount);

		display("Dummy resource before", getDummyResource());
		
		getDummyResource().setModifyBreakMode(BreakMode.SCHEMA);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(accountShadowJack);
        change.setResource(getDummyResourceObject());

		// WHEN
        displayWhen(TEST_NAME);
        synchronizationService.notifyChange(change, task, result);

        // THEN
        displayThen(TEST_NAME);
        getDummyResource().resetBreakMode();
        assertPartialError(result);

        LensContext<UserType> context = cleanDebugListener();
        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
        		ActivationStatusType.ENABLED);

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(getDummyResourceObject().getOid(), ShadowKindType.ACCOUNT, null);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.UNLINKED, accCtx.getSynchronizationSituationDetected());
		assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationResolved());

		PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());

		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJack.getOid());

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		assertLinks(userAfter, 1);
        
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
		final String TEST_NAME = "test202UpdatedAccountJackSchemaViolation";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
		assertLinks(userBefore, 1);
		setDebugListener();

		display("Dummy resource before", getDummyResource());
		
		getDummyResource().setModifyBreakMode(BreakMode.SCHEMA);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        PrismObject<ShadowType> accountShadowJackBefore = getShadowModelNoFetch(accountShadowJackDummyOid);
        change.setCurrentShadow(accountShadowJackBefore);
        change.setResource(getDummyResourceObject());

		// WHEN
        displayWhen(TEST_NAME);
        synchronizationService.notifyChange(change, task, result);

        // THEN
        displayThen(TEST_NAME);
        getDummyResource().resetBreakMode();
        assertPartialError(result);

        LensContext<UserType> context = cleanDebugListener();
        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNull("Unexpected user primary delta", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
        		ActivationStatusType.ENABLED);

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(getDummyResourceObject().getOid(), ShadowKindType.ACCOUNT, null);
		LensProjectionContext accCtx = context.findProjectionContext(rat);
		assertNotNull("No account sync context for "+rat, accCtx);
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationDetected());
		assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, accCtx.getSynchronizationSituationResolved());

		PrismAsserts.assertNoDelta("Unexpected account primary delta", accCtx.getPrimaryDelta());

		assertLinked(context.getFocusContext().getObjectOld().getOid(), accountShadowJackDummyOid);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		assertLinks(userAfter, 1);
        
		PrismObject<ShadowType> shadowAfter = getShadowModelNoFetch(accountShadowJackDummyOid);
        assertSituation(shadowAfter, SynchronizationSituationType.LINKED);
		assertLinked(userAfter, shadowAfter);

	}
	
	@Test
    public void test300AddedGroupPirates() throws Exception {
		final String TEST_NAME = "test300AddedGroupPirates";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        setDebugListener();

        PrismObject<ShadowType> shadowPirates = repoAddObjectFromFile(SHADOW_PIRATES_DUMMY_FILE, result);
        provisioningService.applyDefinition(shadowPirates, task, result);
        assertNotNull("No oid in shadow", shadowPirates.getOid());
        DummyGroup dummyGroup = new DummyGroup();
        dummyGroup.setName(GROUP_PIRATES_DUMMY_NAME);
        dummyGroup.setEnabled(true);
        dummyGroup.addAttributeValues(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, "Scurvy Pirates");
        getDummyResource().addGroup(dummyGroup);

        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(shadowPirates);
        change.setResource(getDummyResourceObject());

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        synchronizationService.notifyChange(change, task, result);

        // THEN
        TestUtil.displayWhen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        LensContext<UserType> context = cleanDebugListener();
        display("Resulting context (as seen by debug listener)", context);
        assertNotNull("No resulting context (as seen by debug listener)", context);

        assertNotNull("No focus primary delta", context.getFocusContext().getPrimaryDelta());
//        assertNotNull("No focus secondary delta", context.getFocusContext().getSecondaryDelta());
        assertFalse("No executed focus deltas", context.getFocusContext().getExecutedDeltas().isEmpty());
        ObjectDelta<UserType> userSecondaryDelta = (ObjectDelta<UserType>) context.getFocusContext().getExecutedDeltas().iterator().next().getObjectDelta();

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(getDummyResourceObject().getOid(),
        		ShadowKindType.ENTITLEMENT, INTENT_GROUP);
		LensProjectionContext projCtx = context.findProjectionContext(rat);
		assertNotNull("No projection sync context for "+rat, projCtx);
		assertEquals("Wrong detected situation in context", SynchronizationSituationType.UNMATCHED, projCtx.getSynchronizationSituationDetected());
		assertEquals("Wrong resolved situation in context", SynchronizationSituationType.LINKED, projCtx.getSynchronizationSituationResolved());

		PrismAsserts.assertNoDelta("Unexpected projection primary delta", projCtx.getPrimaryDelta());
		//it this really expected?? delta was already executed, should we expect it in the secondary delta?
//		assertNotNull("Missing account secondary delta", accCtx.getSecondaryDelta());
//		assertIterationDelta(accCtx.getSecondaryDelta(), 0, "");

		assertLinked(RoleType.class, context.getFocusContext().getOid(), shadowPirates.getOid());

		PrismObject<ShadowType> shadow = getShadowModelNoFetch(shadowPirates.getOid());
        assertIteration(shadow, 0, "");
        assertSituation(shadow, SynchronizationSituationType.LINKED);

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
