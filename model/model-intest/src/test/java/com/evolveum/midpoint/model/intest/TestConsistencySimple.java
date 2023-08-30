/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests various aspects of consistency mechanism. Unlike the complex story test,
 * those tests here are much simpler (e.g. use dummy resource instead of OpenDJ)
 * and relatively isolated.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConsistencySimple extends AbstractInitializedModelIntegrationTest {

    private static final boolean ASSERT_SUCCESS = true;

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "consistency-simple");

    private static final TestObject<UserType> USER_JIM = TestObject.file(
            TEST_DIR, "user-jim.xml", "99576c2e-4edf-40d1-a7ea-47add9362c3a");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        login(USER_ADMINISTRATOR_USERNAME);
    }

    private enum FocusOperation {RECONCILE, RECOMPUTE}

    private enum ShadowOperation {KEEP, DELETE, UNLINK, UNLINK_AND_TOMBSTONE}

    private enum ResourceObjectOperation {KEEP, DELETE}

    private ResourceObjectDefinition getAccountObjectClassDefinition() throws SchemaException {
        ResourceSchema schema = ResourceSchemaFactory.getRawSchema(getDummyResourceObject());
        assertNotNull(schema);
        return schema.findDefinitionForObjectClass(dummyResourceCtl.getAccountObjectClassQName());
    }

    @Test
    public void test100Reconcile_Keep_Keep() throws Exception {
        executeTest(FocusOperation.RECONCILE, ShadowOperation.KEEP, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test110Recompute_Keep_Keep() throws Exception {
        executeTest(FocusOperation.RECOMPUTE, ShadowOperation.KEEP, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test120Reconcile_Unlink_Keep() throws Exception {
        executeTest(FocusOperation.RECONCILE, ShadowOperation.UNLINK, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test130Recompute_Unlink_Keep() throws Exception {
        executeTest(FocusOperation.RECOMPUTE, ShadowOperation.UNLINK, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test140Reconcile_UnlinkTombstone_Keep() throws Exception {
        executeTest(FocusOperation.RECONCILE, ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test150Recompute_UnlinkTombstone_Keep() throws Exception {
        executeTest(FocusOperation.RECOMPUTE, ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test160Reconcile_Delete_Keep() throws Exception {
        executeTest(FocusOperation.RECONCILE, ShadowOperation.DELETE, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test170Recompute_Delete_Keep() throws Exception {
        executeTest(FocusOperation.RECOMPUTE, ShadowOperation.DELETE, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test200Reconcile_Keep_Delete() throws Exception {
        executeTest(FocusOperation.RECONCILE, ShadowOperation.KEEP, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test210Recompute_Keep_Delete() throws Exception {
        executeTest(FocusOperation.RECOMPUTE, ShadowOperation.KEEP, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test220Reconcile_Unlink_Delete() throws Exception {
        executeTest(FocusOperation.RECONCILE, ShadowOperation.UNLINK, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test230Recompute_Unlink_Delete() throws Exception {
        executeTest(FocusOperation.RECOMPUTE, ShadowOperation.UNLINK, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test240Reconcile_UnlinkTombstone_Delete() throws Exception {
        executeTest(FocusOperation.RECONCILE, ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test250Recompute_UnlinkTombstone_Delete() throws Exception {
        executeTest(FocusOperation.RECOMPUTE, ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test260Reconcile_Delete_Delete() throws Exception {
        executeTest(FocusOperation.RECONCILE, ShadowOperation.DELETE, ResourceObjectOperation.KEEP);
    }

    @Test
    public void test270Recompute_Delete_Delete() throws Exception {
        executeTest(FocusOperation.RECOMPUTE, ShadowOperation.DELETE, ResourceObjectOperation.KEEP);
    }

    @SuppressWarnings("SameParameterValue")
    private void executeTest(FocusOperation focusOperation, ShadowOperation shadowOperation,
            ResourceObjectOperation resourceObjectOperation) throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        cleanUpBeforeTest(task, result);

        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
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

    private List<PrismObject<ShadowType>> assertLiveShadows(int expected, OperationResult result) throws SchemaException {
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
            unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
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
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        display("Jack after cleanup", jack);
        assertEquals("Unexpected # of accounts for jack after cleanup", 0, jack.asObjectable().getLinkRef().size());

        List<PrismObject<ShadowType>> deadShadows = assertLiveShadows(0, result);
        for (PrismObject<ShadowType> deadShadow : deadShadows) {
            repositoryService.deleteObject(ShadowType.class, deadShadow.getOid(), result);
        }

        assertNoDummyAccount(null, "jack");
    }

    private List<PrismObject<ShadowType>> getJacksShadows(OperationResult result) throws SchemaException {
        ObjectQuery shadowQuery = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
                .and().item(SchemaConstants.ICFS_NAME_PATH,
                        getAccountObjectClassDefinition().findAttributeDefinition(SchemaConstants.ICFS_NAME)).eq("jack")
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
                    .assertUnfinishedOperation()
                        .deleteOperation()
                            .display();
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
        assertInProgress(reconciliationResult);

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
}
