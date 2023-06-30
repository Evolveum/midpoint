/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.objects;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.ApprovalInstruction;
import com.evolveum.midpoint.wf.impl.ExpectedTask;
import com.evolveum.midpoint.wf.impl.ExpectedWorkItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Testing approvals of role lifecycle: create/modify/delete role.
 * <p>
 * Subclasses provide specializations regarding ways how rules and/or approvers are attached to roles.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractTestObjectLifecycleApproval extends AbstractWfTestPolicy {

    static final File TEST_RESOURCE_DIR = new File("src/test/resources/objects");

    private static final File USER_PIRATE_OWNER_FILE = new File(TEST_RESOURCE_DIR, "user-pirate-owner.xml");
    private static final String USER_PIRATE_OWNER_OID = "00000001-d34d-b33f-f00d-504f574e4552";

    private static final File USER_JUDGE_OWNER_FILE = new File(TEST_RESOURCE_DIR, "user-judge-owner.xml");
    static final String USER_JUDGE_OWNER_OID = "00000001-d34d-b33f-f00d-4a4f574e4552";

    private static final File USER_JUPITER_FILE = new File(TEST_RESOURCE_DIR, "user-jupiter.xml");
    static final String USER_JUPITER_OID = "9ab1cabd-2455-490e-9844-79dfa3efa849";

    private static final File USER_PETER_FILE = new File(TEST_RESOURCE_DIR, "user-peter.xml");
    static final String USER_PETER_OID = "f96a5e37-dbe4-4882-b536-7c15c74d1dbc";

    private static final File USER_BOB_FILE = new File(TEST_RESOURCE_DIR, "user-bob.xml");
    static final String USER_BOB_OID = "44d37c0a-8ec6-4540-b8f1-f022fd5a19ef";

    private String rolePirateOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // actually not sure why recomputing these
        addAndRecomputeUser(USER_PIRATE_OWNER_FILE, initTask, initResult);
        addAndRecomputeUser(USER_JUDGE_OWNER_FILE, initTask, initResult);

        repoAddObjectFromFile(USER_JUPITER_FILE, initResult);
        repoAddObjectFromFile(USER_PETER_FILE, initResult);
        repoAddObjectFromFile(USER_BOB_FILE, initResult);
    }

    protected boolean approveObjectAdd() {
        return false;
    }

    @Test
    public void test010CreateRolePirate() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        RoleType pirate = new RoleType(prismContext);
        pirate.setName(PolyStringType.fromOrig("pirate"));

        if (approveObjectAdd()) {
            createObject(pirate, false, true, USER_JUPITER_OID);
            rolePirateOid = searchObjectByName(RoleType.class, "pirate").getOid();
        } else {
            repoAddObject(pirate.asPrismObject(), result);
            rolePirateOid = pirate.getOid();
        }

        PrismReferenceValue pirateOwner = getPrismContext().itemFactory().createReferenceValue(rolePirateOid, RoleType.COMPLEX_TYPE);
        pirateOwner.setRelation(SchemaConstants.ORG_OWNER);
        executeChanges(prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(ObjectTypeUtil.createAssignmentTo(pirateOwner))
                        .asObjectDelta(USER_PIRATE_OWNER_OID),
                null, task, result);
        PrismObject<RoleType> pirateAfter = getRole(rolePirateOid);
        display("Pirate role", pirateAfter);
        display("Pirate owner", getUser(USER_PIRATE_OWNER_OID));

        if (approveObjectAdd()) {
            MetadataType metadata = pirateAfter.asObjectable().getMetadata();
            assertEquals("Wrong create approver ref",
                    singleton(ObjectTypeUtil.createObjectRef(USER_JUPITER_OID, ObjectTypes.USER).relation(SchemaConstants.ORG_DEFAULT)),
                    new HashSet<>(metadata.getCreateApproverRef()));
            assertEquals("Wrong create approval comments",
                    singleton("jupiter :: creation comment"),
                    new HashSet<>(metadata.getCreateApprovalComment()));
        }
    }

    @Test
    public void test100ModifyRolePirateDescription() throws Exception {
        login(userAdministrator);

        ObjectDelta<RoleType> descriptionDelta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_DESCRIPTION).replace("Bloody pirate")
                .asObjectDelta(rolePirateOid);
        prismContext.deltaFactory().object()
                .createModifyDelta(rolePirateOid, Collections.emptyList(), RoleType.class);
        ExpectedTask expectedTask = new ExpectedTask(null, "Modifying role \"pirate\"");
        ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(USER_PIRATE_OWNER_OID, null, expectedTask);
        modifyObject(descriptionDelta, false, true, USER_PIRATE_OWNER_OID,
                Collections.singletonList(expectedTask), Collections.singletonList(expectedWorkItem),
                () -> {},
                () -> assertNull("Description is modified", getRoleSimple(rolePirateOid).getDescription()),
                () -> assertEquals("Description was NOT modified", "Bloody pirate", getRoleSimple(rolePirateOid).getDescription()));

        PrismObject<RoleType> roleAfter = getRole(rolePirateOid);
        display("pirate after", roleAfter);
        MetadataType metadata = roleAfter.asObjectable().getMetadata();
        assertEquals("Wrong modify approver ref",
                singleton(ObjectTypeUtil.createObjectRef(USER_PIRATE_OWNER_OID, ObjectTypes.USER).relation(SchemaConstants.ORG_DEFAULT)),
                new HashSet<>(metadata.getModifyApproverRef()));
        assertEquals("Wrong modify approval comments",
                singleton("pirate-owner :: modification comment"),
                new HashSet<>(metadata.getModifyApprovalComment()));
    }

    @Test
    public void test200DeleteRolePirate() throws Exception {
        login(userAdministrator);

        ExpectedTask expectedTask = new ExpectedTask(null, "Deleting role \"pirate\"");
        ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(USER_PIRATE_OWNER_OID, null, expectedTask);
        deleteObject(RoleType.class, rolePirateOid, false, true,
                Collections.singletonList(expectedTask), Collections.singletonList(expectedWorkItem));
    }

    private void createObject(
            ObjectType object, boolean immediate, boolean approve, String assigneeOid)
            throws Exception {
        //noinspection unchecked
        ObjectDelta<RoleType> addObjectDelta =
                DeltaFactory.Object.createAddDelta((PrismObject<RoleType>) object.asPrismObject());

        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                //noinspection unchecked
                LensContext<RoleType> lensContext = createLensContext((Class) object.getClass());
                addFocusDeltaToContext(lensContext, addObjectDelta);
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) throws Exception {
                if (!immediate) {
                    //                    ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
                    //                    ObjectDelta realDelta0 = taskModelContext.getFocusContext().getPrimaryDelta();
                    //                    assertTrue("Non-empty primary focus delta: " + realDelta0.debugDump(), realDelta0.isEmpty());
                    assertNoObject(object);
                    ExpectedTask expectedTask = new ExpectedTask(null, "Adding role \"" + object.getName().getOrig() + "\"");
                    ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(assigneeOid, null, expectedTask);
                    assertWfContextAfterClockworkRun(rootCase, subcases, workItems,
                            null,
                            Collections.singletonList(expectedTask),
                            Collections.singletonList(expectedWorkItem));
                }
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) throws Exception {
                assertNoObject(object);
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases, Task opTask, OperationResult result) throws Exception {
                if (approve) {
                    assertObject(object);
                } else {
                    assertNoObject(object);
                }
            }

            @Override
            protected boolean executeImmediately() {
                return immediate;
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                return singletonList(new ApprovalInstruction(null, true, USER_JUPITER_OID, "creation comment"));
            }
        }, 1);
    }

    public <T extends ObjectType> void modifyObject(
            ObjectDelta<T> objectDelta, boolean immediate, boolean approve, String assigneeOid,
            List<ExpectedTask> expectedTasks, List<ExpectedWorkItem> expectedWorkItems,
            Runnable assertDelta0Executed, Runnable assertDelta1NotExecuted, Runnable assertDelta1Executed)
            throws Exception {

        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                Class<T> clazz = objectDelta.getObjectTypeClass();
                //PrismObject<T> object = getObject(clazz, objectDelta.getOid());
                LensContext<T> lensContext = createLensContext(clazz);
                addFocusDeltaToContext(lensContext, objectDelta);
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) {
                if (!immediate) {
                    assertDelta1NotExecuted.run();
                    assertWfContextAfterClockworkRun(rootCase, subcases, workItems,
                            objectDelta.getOid(), expectedTasks, expectedWorkItems);
                }
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) {
                assertDelta0Executed.run();
                assertDelta1NotExecuted.run();
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) {
                assertDelta0Executed.run();
                if (approve) {
                    assertDelta1Executed.run();
                } else {
                    assertDelta1NotExecuted.run();
                }
            }

            @Override
            protected boolean executeImmediately() {
                return immediate;
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                return singletonList(new ApprovalInstruction(null, approve, assigneeOid, "modification comment"));
            }
        }, 1);
    }

    public <T extends ObjectType> void deleteObject(
            Class<T> clazz, String objectOid, boolean immediate, boolean approve,
            List<ExpectedTask> expectedTasks, List<ExpectedWorkItem> expectedWorkItems) throws Exception {

        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<T> lensContext = createLensContext(clazz);
                ObjectDelta<T> deleteDelta = prismContext.deltaFactory().object().createDeleteDelta(clazz, objectOid
                );
                addFocusDeltaToContext(lensContext, deleteDelta);
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) {
                if (!immediate) {
                    assertWfContextAfterClockworkRun(rootCase, subcases, workItems,
                            objectOid, expectedTasks, expectedWorkItems);
                }
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) {
                assertObjectExists(clazz, objectOid);
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) {
                if (approve) {
                    assertObjectDoesntExist(clazz, objectOid);
                } else {
                    assertObjectExists(clazz, objectOid);
                }
            }

            @Override
            protected boolean executeImmediately() {
                return immediate;
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
                return approve;
            }
        }, 1);
    }
}
