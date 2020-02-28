/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRecomputeTask extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/sync");

    private static final File TASK_USER_RECOMPUTE_FILE = new File(TEST_DIR, "task-user-recompute.xml");
    private static final String TASK_USER_RECOMPUTE_OID = "91919191-76e0-59e2-86d6-3d4f02d3aaaa";

    private static final File TASK_USER_RECOMPUTE_LIGHT_FILE = new File(TEST_DIR, "task-user-recompute-light.xml");
    private static final String TASK_USER_RECOMPUTE_LIGHT_OID = "b7b6af78-fffe-11e6-ac04-2fdd62641ce2";

    private static final File TASK_USER_RECOMPUTE_CAPTAIN_FILE = new File(TEST_DIR, "task-user-recompute-captain.xml");
    private static final String TASK_USER_RECOMPUTE_CAPTAIN_OID = "91919191-76e0-59e2-86d6-3d4f02d3aaac";

    private static final File TASK_USER_RECOMPUTE_HERMAN_BY_EXPRESSION_FILE = new File(TEST_DIR, "task-user-recompute-herman-by-expression.xml");
    private static final String TASK_USER_RECOMPUTE_HERMAN_BY_EXPRESSION_OID = "91919191-76e0-59e2-86d6-3d4f02d3aadd";

    // TODO move to common dir and apply to all tests
    private static final TestResource ARCHETYPE_TASK_RECOMPUTATION = new TestResource(TEST_DIR, "archetype-task-recomputation.xml", "77615e4c-b82e-4b3a-b265-5487a6ac016b");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        addObject(ARCHETYPE_TASK_RECOMPUTATION, initTask, initResult);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
    }

    @Test
    public void test100RecomputeAll() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getResult();

        // Preconditions
        assertUsers(6);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        // Do some ordinary operations

        assignRole(USER_GUYBRUSH_OID, ROLE_PIRATE_OID, task, result);
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        addObject(USER_HERMAN_FILE);
        assignRole(USER_HERMAN_OID, ROLE_JUDGE_OID, task, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Now do something evil

        // change definition of role "pirate". midPoint will not recompute automatically
        // the recompute task should do it

        // One simple change
        modifyRoleAddConstruction(ROLE_JUDGE_OID, 1111L, RESOURCE_DUMMY_RED_OID);

        // More complicated change
        PrismObject<RoleType> rolePirate = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, null, task, result);
        ItemPath attrItemPath = ItemPath.create(RoleType.F_INDUCEMENT, 1111L, AssignmentType.F_CONSTRUCTION, 60004L, ConstructionType.F_ATTRIBUTE);
        PrismContainer<ResourceAttributeDefinitionType> attributeCont = rolePirate.findContainer(attrItemPath);
        assertNotNull("No attribute property in "+rolePirate, attributeCont);
        PrismContainerValue<ResourceAttributeDefinitionType> oldAttrContainer = null;
        for (PrismContainerValue<ResourceAttributeDefinitionType> cval: attributeCont.getValues()) {
            ResourceAttributeDefinitionType attrType = cval.getValue();
            if (ItemPathTypeUtil.asSingleNameOrFail(attrType.getRef()).getLocalPart().equals(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME)) {
                oldAttrContainer = cval;
            }
        }
        assertNotNull("Definition for weapon attribute not found in "+rolePirate, oldAttrContainer);
        PrismContainerValue<ResourceAttributeDefinitionType> newAttrContainer = oldAttrContainer.clone();
        XNode daggerXNode = prismContext.xnodeFactory().primitive("dagger");
        RawType daggerValueEvaluator = new RawType(daggerXNode, prismContext);
        JAXBElement<?> daggerExpressionEvalJaxbElement = new JAXBElement<>(SchemaConstants.C_VALUE, Object.class, daggerValueEvaluator);
        newAttrContainer.getValue().getOutbound().getExpression().getExpressionEvaluator().add(daggerExpressionEvalJaxbElement);
        newAttrContainer.getValue().getOutbound().setStrength(MappingStrengthType.STRONG);

        ObjectDelta<RoleType> rolePirateDelta = prismContext.deltaFactory().object()
                .createModificationDeleteContainer(RoleType.class, ROLE_PIRATE_OID,
                attrItemPath, oldAttrContainer.getValue().clone());
        ResourceAttributeDefinitionType newAttrCVal = newAttrContainer.getValue();
        newAttrCVal.asPrismContainerValue().setId(null);
        rolePirateDelta.addModificationAddContainer(attrItemPath, newAttrCVal);

        display("Role pirate delta", rolePirateDelta);
        modelService.executeChanges(MiscSchemaUtil.createCollection(rolePirateDelta), null, task, result);

        displayRoles(task, result);

        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        assertUser(USER_JACK_OID, "user jack before")
                .display()
                .assignments()
                    .single()
                        .assertRole(ROLE_JUDGE_OID)
                        .end()
                    .end()
                .roleMembershipRefs()
                    .single()
                        .assertOid(ROLE_JUDGE_OID);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        // WHEN
        when();
        addTask(TASK_USER_RECOMPUTE_FILE);

        dummyAuditService.clear();

        waitForTaskStart(TASK_USER_RECOMPUTE_OID, false);

        // WHEN
        when();

        waitForTaskFinish(TASK_USER_RECOMPUTE_OID, true, 40000);

        // THEN
        then();

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after recompute", users);

        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "cutlass", "dagger");
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        assertUser(USER_JACK_OID, "user jack after")
                .display()
                .assertNoArchetypeRef();        // MID-6061

        assertNoDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        assertUsers(7);

        // Check audit
        display("Audit", dummyAuditService);

        List<AuditEventRecord> auditRecords = dummyAuditService.getRecords();

        int i=0;
        int modifications = 0;
        for (; i < (auditRecords.size() - 1); i+=2) {
            AuditEventRecord requestRecord = auditRecords.get(i);
            assertNotNull("No request audit record ("+i+")", requestRecord);
            assertEquals("Got this instead of request audit record ("+i+"): "+requestRecord, AuditEventStage.REQUEST, requestRecord.getEventStage());
            assertTrue("Unexpected delta in request audit record "+requestRecord, requestRecord.getDeltas() == null || requestRecord.getDeltas().isEmpty());

            AuditEventRecord executionRecord = auditRecords.get(i+1);
            assertNotNull("No execution audit record ("+i+")", executionRecord);
            assertEquals("Got this instead of execution audit record ("+i+"): "+executionRecord, AuditEventStage.EXECUTION, executionRecord.getEventStage());

            assertTrue("Empty deltas in execution audit record "+executionRecord, executionRecord.getDeltas() != null && ! executionRecord.getDeltas().isEmpty());
            modifications++;

            // check next records
            while (i < (auditRecords.size() - 2)) {
                AuditEventRecord nextRecord = auditRecords.get(i+2);
                if (nextRecord.getEventStage() == AuditEventStage.EXECUTION) {
                    // more than one execution record is OK
                    i++;
                } else {
                    break;
                }
            }

        }
        assertEquals("Unexpected number of audit modifications", 7, modifications);

        deleteObject(TaskType.class, TASK_USER_RECOMPUTE_OID, task, result);
    }

    private void displayRoles(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<RoleType> rolePirate = modelService.getObject(RoleType.class, ROLE_PIRATE_OID, null, task, result);
        display("Role pirate after modify", rolePirate);
        IntegrationTestTools.displayXml("Role pirate after modify", rolePirate);
        PrismObject<RoleType> roleJudge = modelService.getObject(RoleType.class, ROLE_JUDGE_OID, null, task, result);
        display("Role judge after modify", roleJudge);
        IntegrationTestTools.displayXml("Role judge after modify", roleJudge);

    }

    @Test
    public void test110RecomputeSome() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getResult();

        // Preconditions
        assertUsers(7);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_HERMAN_USERNAME, "Herman Toothrot", true);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Now do something evil, remove "red" construction from judge role
        modifyRoleDeleteInducement(ROLE_JUDGE_OID, 1111L, false, null, getTestTask());

        displayRoles(task, result);

        // WHEN
        when();
        addTask(TASK_USER_RECOMPUTE_CAPTAIN_FILE);

        dummyAuditService.clear();

        waitForTaskStart(TASK_USER_RECOMPUTE_CAPTAIN_OID, false);

        // WHEN
        when();

        waitForTaskFinish(TASK_USER_RECOMPUTE_CAPTAIN_OID, true, 40000);

        // THEN
        then();

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after recompute", users);

        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "cutlass", "dagger");
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        // Red resource does not delete accounts on deprovision, it disables them
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);

        // Only captains are recomputed. Therefore herman stays un-recomputed
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_HERMAN_USERNAME, "Herman Toothrot", true);

        assertUsers(7);

    }

    /**
     * Here we recompute herman as well.
     */
    @Test
    public void test120RecomputeByExpression() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getResult();
        prepareNotifications();

        // Preconditions
        assertUsers(7);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_HERMAN_USERNAME, "Herman Toothrot", true);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        // WHEN
        when();
        addTask(TASK_USER_RECOMPUTE_HERMAN_BY_EXPRESSION_FILE);

        dummyAuditService.clear();

        waitForTaskStart(TASK_USER_RECOMPUTE_HERMAN_BY_EXPRESSION_OID, false);

        // WHEN
        when();

        waitForTaskFinish(TASK_USER_RECOMPUTE_HERMAN_BY_EXPRESSION_OID, true, 40000);

        // THEN
        then();

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after recompute", users);

        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "cutlass", "dagger");
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        // Red resource does not delete accounts on deprovision, it disables them
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);

        // Herman should be recomputed now
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_HERMAN_USERNAME, "Herman Toothrot", false);

        TaskType recomputeTask = getTask(TASK_USER_RECOMPUTE_HERMAN_BY_EXPRESSION_OID).asObjectable();
        assertEquals("Wrong success count", 1, recomputeTask.getOperationStats().getIterativeTaskInformation().getTotalSuccessCount());
        assertEquals("Wrong failure count", 0, recomputeTask.getOperationStats().getIterativeTaskInformation().getTotalFailureCount());

        assertUsers(7);

        displayAllNotifications();
        assertSingleDummyTransportMessageContaining("simpleAccountNotifier-SUCCESS", "Channel: " + SchemaConstants.CHANGE_CHANNEL_RECOMPUTE_URI);
    }

    /**
     * Light recompute. Very efficient, no resource operations, just fix the focus.
     * MID-3384
     */
    @Test
    public void test130RecomputeLight() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getResult();

        // Preconditions
        assertUsers(7);

        assertUser(USER_JACK_OID, "user jack before")
                .display()
                .assertNoArchetypeRef()
                .assignments()
                    .single()
                        .assertRole(ROLE_JUDGE_OID)
                        .end()
                    .end()
                .roleMembershipRefs()
                    .single()
                        .assertOid(ROLE_JUDGE_OID);

        assignOrg(USER_GUYBRUSH_OID, ORG_MINISTRY_OF_OFFENSE_OID, null);
        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("User guybrush before", userGuybrushBefore);
        assertAssignedRole(userGuybrushBefore, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userGuybrushBefore, ROLE_PIRATE_OID, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignedOrgs(userGuybrushBefore, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userGuybrushBefore, ORG_MINISTRY_OF_OFFENSE_OID);

        clearUserOrgAndRoleRefs(USER_JACK_OID);
        clearUserOrgAndRoleRefs(USER_GUYBRUSH_OID);

        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);

        // WHEN
        when();
        addTask(TASK_USER_RECOMPUTE_LIGHT_FILE);

        dummyAuditService.clear();

        waitForTaskStart(TASK_USER_RECOMPUTE_LIGHT_OID, false);

        // WHEN
        when();

        waitForTaskFinish(TASK_USER_RECOMPUTE_LIGHT_OID, true, 40000);

        // THEN
        then();

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after recompute", users);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 0);

        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "cutlass", "dagger");
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        // Red resource does not delete accounts on deprovision, it disables them
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);

        // Herman should be recomputed now
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_HERMAN_USERNAME, "Herman Toothrot", false);

        TaskType recomputeTask = getTask(TASK_USER_RECOMPUTE_LIGHT_OID).asObjectable();
        assertEquals("Wrong success count", 7, recomputeTask.getOperationStats().getIterativeTaskInformation().getTotalSuccessCount());
        assertEquals("Wrong failure count", 0, recomputeTask.getOperationStats().getIterativeTaskInformation().getTotalFailureCount());

        assertUser(USER_JACK_OID, "user jack after")
                .display()
                .assertNoArchetypeRef()     // MID-6061
                .assignments()
                    .single()
                        .assertRole(ROLE_JUDGE_OID)
                        .end()
                    .end()
                .roleMembershipRefs()
                    .single()
                        .assertOid(ROLE_JUDGE_OID);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User guybrush after", userGuybrushAfter);
        assertAssignedRole(userGuybrushAfter, ROLE_PIRATE_OID);
        assertRoleMembershipRef(userGuybrushAfter, ROLE_PIRATE_OID, ORG_MINISTRY_OF_OFFENSE_OID);
        assertAssignedOrgs(userGuybrushAfter, ORG_MINISTRY_OF_OFFENSE_OID);
        assertHasOrgs(userGuybrushAfter, ORG_MINISTRY_OF_OFFENSE_OID);

        assertUsers(7);
    }
}
