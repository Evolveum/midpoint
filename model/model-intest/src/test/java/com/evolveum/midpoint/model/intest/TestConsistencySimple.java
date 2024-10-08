/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.INTENT_DEFAULT;
import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.util.List;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.test.DummyTestResource;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Tests various aspects of consistency mechanism. Unlike the complex story test,
 * those tests here are much simpler (e.g. use dummy resource instead of OpenDJ)
 * and relatively isolated.
 *
 * Tests 1xx and 2xx are based on the scenario described in
 * {@link #executeBasicTest(ShadowOperation, ResourceObjectOperation, FocusOperation)}.
 *
 * There are also other, unrelated tests.
 *
 * Tests 4xx are devoted to the discovery, i.e. running embedded clockwork triggered by some inconsistency being detected.
 * They could be factored out into a separate test class later.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConsistencySimple extends AbstractInitializedModelIntegrationTest {

    private static final boolean ASSERT_SUCCESS = true;

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "consistency-simple");

    private static final TestObject<UserType> USER_JIM = TestObject.file(
            TEST_DIR, "user-jim.xml", "99576c2e-4edf-40d1-a7ea-47add9362c3a");

    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON = TestObject.file(
            TEST_DIR, "archetype-person.xml", "3cdd3cac-affc-466b-bc4b-78c64952901e");
    private static final DummyTestResource RESOURCE_SOURCE = new DummyTestResource(
            TEST_DIR, "resource-source.xml", "9794d702-5aed-4b96-99fc-14012c7595a0", "source");
    private static final DummyTestResource RESOURCE_TARGET_1 = new DummyTestResource(
            TEST_DIR, "resource-target-1.xml", "86100e19-1052-4913-8551-b2cef402c85d", "target-1");
    private static final DummyTestResource RESOURCE_TARGET_2 = new DummyTestResource(
            TEST_DIR, "resource-target-2.xml", "1f594726-59ce-4ed6-a6bb-e39a3d0770c7", "target-2");
    private static final DummyTestResource RESOURCE_MAPPING_STRENGTHS = new DummyTestResource(
            TEST_DIR, "resource-mapping-strengths.xml", "3bbe4c63-ff4f-489e-9a94-977adec4b24d",
            "mapping-strengths",
            c -> c.extendSchemaPirate());

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        login(USER_ADMINISTRATOR_USERNAME);

        initTestObjects(initTask, initResult,
                ARCHETYPE_PERSON,
                RESOURCE_SOURCE,
                RESOURCE_TARGET_1,
                RESOURCE_TARGET_2,
                RESOURCE_MAPPING_STRENGTHS);
    }

    private enum FocusOperation {RECONCILE, RECOMPUTE}

    private enum ShadowOperation {KEEP, DELETE, UNLINK, UNLINK_AND_TOMBSTONE}

    private enum ResourceObjectOperation {KEEP, DELETE}

    private ResourceObjectDefinition getAccountObjectClassDefinition() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory
                .getCompleteSchemaRequired(getDummyResourceObject())
                .findDefinitionForObjectClass(dummyResourceCtl.getAccountObjectClassQName());
    }

    @Test
    public void test100KeepKeepReconcile() throws Exception {
        executeBasicTest(ShadowOperation.KEEP, ResourceObjectOperation.KEEP, FocusOperation.RECONCILE);
    }

    @Test
    public void test110KeepKeepRecompute() throws Exception {
        executeBasicTest(ShadowOperation.KEEP, ResourceObjectOperation.KEEP, FocusOperation.RECOMPUTE);
    }

    @Test
    public void test120UnlinkKeepReconcile() throws Exception {
        executeBasicTest(ShadowOperation.UNLINK, ResourceObjectOperation.KEEP, FocusOperation.RECONCILE);
    }

    @Test
    public void test130UnlinkKeepRecompute() throws Exception {
        executeBasicTest(ShadowOperation.UNLINK, ResourceObjectOperation.KEEP, FocusOperation.RECOMPUTE);
    }

    @Test
    public void test140UnlinkTombstoneKeepReconcile() throws Exception {
        executeBasicTest(ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP, FocusOperation.RECONCILE);
    }

    @Test
    public void test150UnlinkTombstoneKeepRecompute() throws Exception {
        executeBasicTest(ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP, FocusOperation.RECOMPUTE);
    }

    @Test
    public void test160DeleteKeepReconcile() throws Exception {
        executeBasicTest(ShadowOperation.DELETE, ResourceObjectOperation.KEEP, FocusOperation.RECONCILE);
    }

    @Test
    public void test170DeleteKeepRecompute() throws Exception {
        executeBasicTest(ShadowOperation.DELETE, ResourceObjectOperation.KEEP, FocusOperation.RECOMPUTE);
    }

    @Test
    public void test200KeepDeleteReconcile() throws Exception {
        executeBasicTest(ShadowOperation.KEEP, ResourceObjectOperation.KEEP, FocusOperation.RECONCILE);
    }

    @Test
    public void test210KeepDeleteRecompute() throws Exception {
        executeBasicTest(ShadowOperation.KEEP, ResourceObjectOperation.KEEP, FocusOperation.RECOMPUTE);
    }

    @Test
    public void test220UnlinkDeleteReconcile() throws Exception {
        executeBasicTest(ShadowOperation.UNLINK, ResourceObjectOperation.KEEP, FocusOperation.RECONCILE);
    }

    @Test
    public void test230UnlinkDeleteRecompute() throws Exception {
        executeBasicTest(ShadowOperation.UNLINK, ResourceObjectOperation.KEEP, FocusOperation.RECOMPUTE);
    }

    @Test
    public void test240UnlinkTombstoneDeleteReconcile() throws Exception {
        executeBasicTest(ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP, FocusOperation.RECONCILE);
    }

    @Test
    public void test250UnlinkTombstoneDeleteRecompute() throws Exception {
        executeBasicTest(ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP, FocusOperation.RECOMPUTE);
    }

    @Test
    public void test260DeleteDeleteReconcile() throws Exception {
        executeBasicTest(ShadowOperation.DELETE, ResourceObjectOperation.KEEP, FocusOperation.RECONCILE);
    }

    @Test
    public void test270DeleteDeleteRecompute() throws Exception {
        executeBasicTest(ShadowOperation.DELETE, ResourceObjectOperation.KEEP, FocusOperation.RECOMPUTE);
    }

    /**
     * Tests a basic consistency-related scenario:
     *
     * . there is a user with an account
     * . account is manipulated (see {@link ShadowOperation} and {@link ResourceObjectOperation}),
     * e.g. shadow is deleted in repo but account is preserved on the resource
     * . user is recomputed or reconciled (see {@link FocusOperation})
     * . the account should be OK again
     */
    private void executeBasicTest(
            ShadowOperation shadowOperation,
            ResourceObjectOperation resourceObjectOperation,
            FocusOperation focusOperation) throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        cleanUpBeforeTest(task, result);

        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, INTENT_DEFAULT, task, result);
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        display("Jack with account", jack);
        assertEquals("Unexpected # of accounts for jack", 1, jack.asObjectable().getLinkRef().size());

        ShadowType shadowBefore = getObject(ShadowType.class, jack.asObjectable().getLinkRef().get(0).getOid()).asObjectable();
        display("Shadow", shadowBefore);

        if (shadowOperation != ShadowOperation.KEEP) {
            ObjectDelta<UserType> removeLinkRefDelta = deltaFor(UserType.class)
                    .item(UserType.F_LINK_REF).replace()
                    .asObjectDelta(USER_JACK_OID);
            executeChanges(removeLinkRefDelta, executeOptions().raw(), task, result);
            jack = getUser(USER_JACK_OID);
            assertEquals("Unexpected # of accounts for jack after linkRef removal", 0, jack.asObjectable().getLinkRef().size());

            if (shadowOperation == ShadowOperation.DELETE) {
                deleteObjectRaw(ShadowType.class, shadowBefore.getOid(), task, result);
                assertNoObject(ShadowType.class, shadowBefore.getOid());
            } else {
                if (shadowOperation == ShadowOperation.UNLINK_AND_TOMBSTONE) {
                    ObjectDelta<ShadowType> markAsDead = deltaFor(ShadowType.class)
                            .item(ShadowType.F_DEAD).replace(Boolean.TRUE)
                            .item(ShadowType.F_EXISTS).replace(Boolean.FALSE)
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                            .asObjectDelta(shadowBefore.getOid());
                    executeChanges(markAsDead, executeOptions().raw(), task, result);
                }
                assertNotNull("jack's shadow does not exist", getObject(ShadowType.class, shadowBefore.getOid()));
            }
        }

        if (resourceObjectOperation == ResourceObjectOperation.DELETE) {
            getDummyResource().deleteAccountByName("jack");
            assertNoDummyAccount(null, "jack");
        } else {
            assertDummyAccount(null, "jack");
        }

        task = getTestTask();
        result = task.getResult();

        // WHEN
        when();
        switch (focusOperation) {
            case RECOMPUTE:
                recomputeUser(USER_JACK_OID, task, result);
                break;
            case RECONCILE:
                ObjectDelta<UserType> emptyDelta = prismContext.deltaFactory().object()
                        .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
                modelService.executeChanges(MiscSchemaUtil.createCollection(emptyDelta), executeOptions().reconcile(), task, result);
                break;
            default:
                throw new IllegalArgumentException("focusOperation: " + focusOperation);
        }

        // THEN
        then();
        result.computeStatus();
        display("Result", result);
        if (ASSERT_SUCCESS) {
            // Do not look too deep into the result. There may be failures deep inside.
            TestUtil.assertSuccess(result, 2);
        }

        jack = getUser(USER_JACK_OID);
        display("Jack after " + focusOperation, jack);
        assertEquals("Unexpected # of accounts for jack", 1, jack.asObjectable().getLinkRef().size());
        String shadowOidAfter = jack.asObjectable().getLinkRef().get(0).getOid();

        // check the shadow really exists
        assertNotNull("jack's shadow does not exist after " + focusOperation, getObject(ShadowType.class, shadowOidAfter));

        assertLiveShadows(1, result);

        // other checks
        assertDummyAccount(null, "jack");

        cleanUpAfterTest(task, result);
    }

    private List<PrismObject<ShadowType>> assertLiveShadows(int expected, OperationResult result)
            throws SchemaException, ConfigurationException {
        List<PrismObject<ShadowType>> shadowsAfter = getJacksShadows(result);
        display("Shadows for 'jack' on dummy resource", shadowsAfter);
        PrismObject<ShadowType> liveShadowAfter = null;
        for (PrismObject<ShadowType> shadowAfter : shadowsAfter) {
            if (!ShadowUtil.isDead(shadowAfter.asObjectable())) {
                if (liveShadowAfter == null) {
                    liveShadowAfter = shadowAfter;
                } else {
                    fail("More than one live shadow " + liveShadowAfter + ", " + shadowAfter);
                }
            }
        }
        if (expected == 0 && liveShadowAfter != null) {
            fail("Unexpected live shadow: " + liveShadowAfter);
        }
        if (expected == 1 && liveShadowAfter == null) {
            fail("No live shadow");
        }
        return shadowsAfter;
    }

    private void cleanUpBeforeTest(Task task, OperationResult result) throws Exception {
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        display("Jack on start", jack);
        if (!jack.asObjectable().getAssignment().isEmpty()) {
            unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, INTENT_DEFAULT, task, result);
            jack = getUser(USER_JACK_OID);
            display("Jack after initial unassign", jack);
        }
        if (!jack.asObjectable().getLinkRef().isEmpty()) {
            for (ObjectReferenceType ref : jack.asObjectable().getLinkRef()) {
                deleteObject(ShadowType.class, ref.getOid());
            }
            ObjectDelta<UserType> killLinkRefDelta = deltaFor(UserType.class)
                    .item(UserType.F_LINK_REF).replace().asObjectDelta(USER_JACK_OID);
            executeChanges(killLinkRefDelta, executeOptions().raw(), task, result);
        }
        List<PrismObject<ShadowType>> jacksShadows = getJacksShadows(result);
        for (PrismObject<ShadowType> shadow : jacksShadows) {
            deleteObject(ShadowType.class, shadow.getOid());
        }
    }

    private void cleanUpAfterTest(Task task, OperationResult result) throws Exception {
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, INTENT_DEFAULT, task, result);
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        display("Jack after cleanup", jack);
        assertEquals("Unexpected # of accounts for jack after cleanup", 0, jack.asObjectable().getLinkRef().size());

        List<PrismObject<ShadowType>> deadShadows = assertLiveShadows(0, result);
        for (PrismObject<ShadowType> deadShadow : deadShadows) {
            repositoryService.deleteObject(ShadowType.class, deadShadow.getOid(), result);
        }

        assertNoDummyAccount(null, "jack");
    }

    private List<PrismObject<ShadowType>> getJacksShadows(OperationResult result) throws SchemaException, ConfigurationException {
        ObjectQuery shadowQuery = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
                .and().item(SchemaConstants.ICFS_NAME_PATH,
                        getAccountObjectClassDefinition().findSimpleAttributeDefinition(SchemaConstants.ICFS_NAME)).eq("jack")
                .build();
        return repositoryService.searchObjects(ShadowType.class, shadowQuery, null, result);
    }

    /**
     * A sequence of events leading to manifestation of MID-7292:
     *
     * Let's have an unreachable resource.
     *
     * 1. User `jim` is created, with an account on the resource assigned to him. Operation is "in progress".
     * 2. User `jim` is deleted. But the shadow remains.
     * 3. User `jim` is re-created with the same configuration.
     * 4. User `jim` is reconciled.
     */
    @Test
    public void test300CreateDeleteCreateReconcileJim() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("resource is unreachable");
        getDummyResource().setBreakMode(BreakMode.NETWORK);

        when("jim is created with account on unreachable resource assigned");
        addObject(USER_JIM, task, result);

        then("jim should exist, with a single account");
        assertUser(USER_JIM.oid, "after first creation")
                .display()
                .assertLinks(1, 0);

        when("jim is deleted");
        deleteObject(UserType.class, USER_JIM.oid, task, result);

        then("no user should be there, but an account should");
        assertNoObject(UserType.class, USER_JIM.oid);
        // @formatter:off
        assertShadow(findShadowByPrismName("jim", getDummyResourceObject(), result), "after deletion")
                .display()
                .pendingOperations()
                    .assertNoUnfinishedOperations(); // the deletion is finished (it is not applicable)
        // @formatter:on

        when("jim is re-created");
        OperationResult recreationResult = new OperationResult("recreation");
        addObject(USER_JIM, task, recreationResult);

        then("operation should be in progress (no error there)");
        assertInProgress(recreationResult);

        // @formatter:off
        assertUser(USER_JIM.oid, "after re-creation")
                .display()
                .singleLink()
                    .resolveTarget()
                        .display();
        // @formatter:on

        // TODO Maybe there should be a new account with pending ADD operation. But it's currently not so.

        when("jim is reconciled"); // Just another account to break it
        OperationResult reconciliationResult = new OperationResult("reconciliation");
        reconcileUser(USER_JIM.oid, task, reconciliationResult);

        then("operation should be in progress (no error there)");
        if (InternalsConfig.isShadowCachingFullByDefault()) {
            assertSuccess(reconciliationResult);
        } else {
            assertInProgress(reconciliationResult);
        }

        // @formatter:off
        assertUser(USER_JIM.oid, "after reconciliation")
                .display()
                    .singleLink()
                        .resolveTarget()
                            .display();
        // @formatter:on

        // TODO It is questionable if we should check "bring resource up and reconcile the user" scenario here,
        //  or if it's in the scope of more advanced consistency tests (like TestConsistencyMechanism in story tests).
    }

    /**
     * There are mappings of various strengths.
     *
     * - fullname mapping is weak
     * - description mapping is normal
     * - location mapping is strong
     *
     * The resource is not reachable, and these mappings are executed in midPoint.
     * Only the results of normal and strong mappings should be propagated to the resource.
     *
     * MID-9861
     */
    @Test
    public void test310WeakMappingWithUnreachableResource() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();
        var dummyResource = RESOURCE_MAPPING_STRENGTHS.getDummyResource();

        given("a user with an account on the resource");
        var user = new UserType()
                .name(userName)
                .assignment(RESOURCE_MAPPING_STRENGTHS.assignmentWithConstructionOf(ACCOUNT, INTENT_DEFAULT));
        addObject(user, task, result);

        and("attributes are set externally for the account and resource is made unreachable");
        dummyResource.getAccountByName(userName)
                .addAttributeValue(DummyAccount.ATTR_FULLNAME_NAME, "Mr. Dummy")
                .addAttributeValue(DummyAccount.ATTR_DESCRIPTION_NAME, "from dummy")
                .addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "dummy");
        dummyResource.setBreakMode(BreakMode.NETWORK);

        invalidateShadowCacheIfNeeded(RESOURCE_MAPPING_STRENGTHS.oid);

        when("respective properties are set in midPoint (to values different from the above ones)");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_FULL_NAME)
                        .replace(PolyString.fromOrig("Mr. MidPoint"))
                        .item(UserType.F_DESCRIPTION)
                        .replace("from midpoint")
                        .item(UserType.F_LOCALITY)
                        .replace(PolyString.fromOrig("midpoint"))
                        .asObjectDelta(user.getOid()),
                null, task, result);

        var shadow = findShadowByPrismName(userName, RESOURCE_MAPPING_STRENGTHS.controller.getResource(), result);
        displayDumpable("shadow after user is changed but resource is unreachable", shadow);

        and("resource is made reachable and pending changes are executed (via shadow refresh)");
        dummyResource.setBreakMode(BreakMode.NONE);
        provisioningService.refreshShadow(shadow, null, task, result);

        then("full name should be kept intact, others should be changed");
        assertDummyAccountByUsername(RESOURCE_MAPPING_STRENGTHS.name, userName)
                .assertFullName("Mr. Dummy")
                .assertDescription("from midpoint")
                .assertLocation("midpoint");

        when("resource is made unreachable again and the user is reconciled");
        dummyResource.setBreakMode(BreakMode.NETWORK);
        reconcileUser(user.getOid(), task, result);

        shadow = findShadowByPrismName(userName, RESOURCE_MAPPING_STRENGTHS.controller.getResource(), result);
        displayDumpable("shadow after user is reconciled but resource is unreachable", shadow);

        and("resource is made reachable and pending changes are executed (via shadow refresh)");
        dummyResource.setBreakMode(BreakMode.NONE);
        provisioningService.refreshShadow(shadow, null, task, result);

        then("full name should be kept intact, description should be changed");
        assertDummyAccountByUsername(RESOURCE_MAPPING_STRENGTHS.name, userName)
                .assertFullName("Mr. Dummy")
                .assertDescription("from midpoint")
                .assertLocation("midpoint");
    }

    /**
     * Tests administrative status computation in the case of deleted shadow discovery.
     * Inspired by MID-9103.
     *
     * The scenario:
     *
     * . one source system, two target systems (with different administrativeStatus mappings, see below)
     * . an account on the source system disappears
     * . the user is reconciled
     * . there are two clockwork runs:
     * .. the inner - "discovery" - one, triggered by the fact that the account is gone;
     * .. the outer - "main" - one, executing after the discovery run finishes
     *
     * `target-1` has a default administrativeStatus mapping (although strong);
     * `target-2` has a mapping that uses `administrativeStatus` instead of `effectiveStatus`.
     *
     * MID-9103 is about the fact that the administrativeStatus for projections is computed wrongly
     * in the outer clockwork execution.
     */
    @Test
    public void test400ActivationAfterDiscovery() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("account on source system exists");
        RESOURCE_SOURCE.controller.addAccount(userName);

        and("it's synchronized into repo");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SOURCE.oid)
                .withDefaultAccountType()
                .withNameValue(userName)
                .executeOnForeground(result);

        var userOid = assertUserByUsername(userName, "initial")
                .assertLinks(3, 0)
                .getOid();

        and("the accounts on target systems are enabled");
        RESOURCE_TARGET_1.controller.assertAccountByUsername(userName)
                .display()
                .assertAttribute(DummyAccount.ATTR_DESCRIPTION_NAME, "enabled");
        RESOURCE_TARGET_2.controller.assertAccountByUsername(userName)
                .display()
                .assertAttribute(DummyAccount.ATTR_DESCRIPTION_NAME, "enabled");

        when("account on source system is deleted and the user is reconciled");
        dummyAuditService.clear();
        RESOURCE_SOURCE.controller.deleteAccount(userName);
        invalidateShadowCacheIfNeeded(RESOURCE_SOURCE.oid);
        reconcileUser(userOid, task, result);

        then("the user exists and is disabled");
        // @formatter:off
        assertUserByUsername(userName, "after reconciliation")
                .display()
                .assertLiveLinks(2)
                .activation()
                    .assertEffectiveStatus(ActivationStatusType.DISABLED)
                .end();
        // @formatter:on

        and("the target accounts are disabled as well");
        RESOURCE_TARGET_1.controller.assertAccountByUsername(userName)
                .display()
                .assertAttribute(DummyAccount.ATTR_DESCRIPTION_NAME, "disabled");
        RESOURCE_TARGET_2.controller.assertAccountByUsername(userName)
                .display()
                .assertAttribute(DummyAccount.ATTR_DESCRIPTION_NAME, "disabled");
    }
}
