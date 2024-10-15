/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.archetypes;

import java.io.File;

import com.evolveum.midpoint.test.TestObject;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.LIFECYCLE_ARCHIVED;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestArchetypeInheritance extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/archetypes");

    private static final File ARCHETYPE_TASK_BASIC_FILE = new File(TEST_DIR, "archetype-task-basic.xml");

    private static final File ARCHETYPE_RESOURCE_OPERATION_TASK_FILE =
            new File(TEST_DIR, "archetype-resource-operation-task.xml");

    private static final File ARCHETYPE_RECON_TASK_FILE = new File(TEST_DIR, "archetype-recon-task.xml");
    private static final String ARCHETYPE_RECON_TASK_OID = "00000000-0000-0000-0000-000000000541";

    private static final File ARCHETYPE_LIVE_SYNC_FILE = new File(TEST_DIR, "archetype-liveSync-task.xml");
    private static final String ARCHETYPE_LIVE_SYNC_TASK_OID = "00000000-0000-0000-0000-000000000531";

    private static final String TASK_RECON_OID = "7461736b-0000-0000-0000-000000000001";
    private static final String TASK_LIVE_SYNC_OID = "7461736b-0000-0000-0000-000000000002";

    private static final ItemName SYNC_TOKEN = new ItemName(
            "http://midpoint.evolveum.com/xml/ns/public/provisioning/liveSync-3", "token");

    private static final TestObject<ArchetypeType> ARCHETYPE_INHERITED_PARENT =
            TestObject.file(TEST_DIR, "archetype-inherited-parent.xml", "0d69d2c9-d1f4-4cfc-acb3-af8a71db81d9");
    private static final TestObject<ArchetypeType> ARCHETYPE_INHERITED_CHILD =
            TestObject.file(TEST_DIR, "archetype-inherited-child.xml", "0d69d2c9-d1f4-4cfc-acb3-af8a71db89d8");
    private static final TestObject<ArchetypeType> ROLE_INHERITED_ARCHETYPES =
            TestObject.file(TEST_DIR, "role-inherited-archetypes.xml", "748b29c6-199c-11e9-9acc-3f8cc307573b");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ARCHETYPE_TASK_BASIC_FILE, initResult);
        repoAddObjectFromFile(ARCHETYPE_RESOURCE_OPERATION_TASK_FILE, initResult);
        repoAddObjectFromFile(ARCHETYPE_RECON_TASK_FILE, initResult);
        repoAddObjectFromFile(ARCHETYPE_LIVE_SYNC_FILE, initResult);

    }

    @Test
    public void test100reconTaskArchetypePolicy() throws Exception {
        TaskType reconTask = new TaskType(prismContext)
                .name("Recon task")
                .assignment(new AssignmentType(prismContext).targetRef(ARCHETYPE_RECON_TASK_OID, ArchetypeType.COMPLEX_TYPE));

        // @formatter:off
        assertArchetypePolicy(reconTask.asPrismObject())
                .displayType()
                    .assertLabel("Reconciliation task")
                    .assertPluralLabel("Reconciliation tasks")
                    .icon()
                        .assertColor("green")
                        .assertCssClass("fa fa-exchange-alt")
                    .end()
                .end()
                .assertItemConstraints(7)
                .itemConstraints()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECTCLASS))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_WORKER_THREADS))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_DRY_RUN))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION))
                        .assertVisibility(UserInterfaceElementVisibilityType.VACANT)
                        .end()
                    .assertNoItemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_RETRY_LIVE_SYNC_ERRORS))
                    .end()
                .adminGuiConfig()
                    .objectDetails()
                        .assertType(TaskType.COMPLEX_TYPE)
                        .panel()
                            .byIdentifier("basic")
                        .container()
                            .byIdentifier("resourceOperationOptions")
                                .displayType()
                                    .assertLabel("ReconciliationTask.reconciliationOptions")
                                .end()
                                .items()
                                    .assertItem(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_WORKER_THREADS))
                                    .assertItem(SchemaConstants.PATH_MODEL_EXTENSION_DRY_RUN)
                                    .assertNoItem(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_UPDATE_LIVE_SYNC_TOKEN_IN_DRY_RUN))
                                .end()
                            .end()
                            .byIdentifier("resourceOptions")
                                .displayType()
                                    .assertLabel("ReconciliationTask.resourceObjects")
                                .end()
                                .items()
                                    .assertItem(SchemaConstants.PATH_MODEL_EXTENSION_OBJECT_QUERY)
                                    .assertItem(TaskType.F_OBJECT_REF)
                                    .assertItem(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECTCLASS))
                                    .assertItem(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND))
                                    .assertItem(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT))
                                .end()
                            .end()
                            .byDisplayName("Advanced options")
                                .assertItems(4)
                                .assertDisplayOrder(150);
        // @formatter:on
    }

    @Test
    public void test110syncTaskArchetypePolicy() throws Exception {
        TaskType syncTask = new TaskType(prismContext)
                .name("Sync task")
                .assignment(new AssignmentType(prismContext).targetRef(ARCHETYPE_LIVE_SYNC_TASK_OID, ArchetypeType.COMPLEX_TYPE));

        // @formatter:off
        assertArchetypePolicy(syncTask.asPrismObject())
                .displayType()
                    .assertLabel("Live synchronization task")
                    .assertPluralLabel("Live synchronization tasks")
                    .icon()
                        .assertColor("green")
                        .assertCssClass("fa fa-refresh")
                        .end()
                    .end()
                .assertItemConstraints(9)
                .itemConstraints()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_RETRY_LIVE_SYNC_ERRORS))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_UPDATE_LIVE_SYNC_TOKEN_IN_DRY_RUN))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_WORKER_THREADS))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_DRY_RUN))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION))
                        .assertVisibility(UserInterfaceElementVisibilityType.VACANT)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECTCLASS))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SYNC_TOKEN))
                        .assertVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .end()
                .adminGuiConfig()
                    .objectDetails()
                        .assertType(TaskType.COMPLEX_TYPE)
                        .panel().byIdentifier("basic")
                        .container()
                            .byIdentifier("resourceOperationOptions")
                                .displayType()
                                    .assertLabel("LiveSynchronizationTask.synchronizationOptions")
                                .end()
                                .items()
                                    .assertItem(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_RETRY_LIVE_SYNC_ERRORS))
                                    .assertItem(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_WORKER_THREADS))
                                    .assertItem(SchemaConstants.PATH_MODEL_EXTENSION_DRY_RUN)
                                    .assertItem(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_UPDATE_LIVE_SYNC_TOKEN_IN_DRY_RUN))
                                .end()
                            .end()
                            .byIdentifier("resourceOptions")
                                .displayType()
                                    .assertLabel("resourceObjects")
                                .end()
                                .items()
                                    .assertNoItem(SchemaConstants.PATH_MODEL_EXTENSION_OBJECT_QUERY)
                                    .assertItem(TaskType.F_OBJECT_REF)
                                    .assertItem(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECTCLASS))
                                    .assertItem(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND))
                                    .assertItem(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
        // @formatter:on
    }

    @Test
    public void test200assignArchetypeReconTask() throws Exception {
        TaskType reconTask = new TaskType(prismContext)
                .oid(TASK_RECON_OID)
                .name("Reconciliation task")
                .assignment(new AssignmentType(prismContext).targetRef(ARCHETYPE_RECON_TASK_OID, ArchetypeType.COMPLEX_TYPE));

        addObject(reconTask.asPrismObject());

        assertTask(TASK_RECON_OID, "created reconciliation task")
                .assertExecutionState(TaskExecutionStateType.SUSPENDED)
                .assertBinding(TaskBindingType.TIGHT);
    }

    @Test
    public void test210assignArchetypeLiveSyncTask() throws Exception {
        TaskType reconTask = new TaskType(prismContext)
                .oid(TASK_LIVE_SYNC_OID)
                .name("Live synchronization task")
                .assignment(new AssignmentType(prismContext).targetRef(ARCHETYPE_LIVE_SYNC_TASK_OID, ArchetypeType.COMPLEX_TYPE));

        addObject(reconTask.asPrismObject());

        assertTask(TASK_LIVE_SYNC_OID, "created live synchronization task")
                .assertExecutionState(TaskExecutionStateType.SUSPENDED)
                .assertBinding(TaskBindingType.TIGHT);
    }

    /**
     * Role with archived lifecycle state and archetypeRef from archetype and it's super archetype.
     *
     * MID-10101
     */
    @Test
    public void test220InheritedArchetypesForArchivedRole() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ARCHETYPE_INHERITED_PARENT.importObject(task, result);
        ARCHETYPE_INHERITED_CHILD.importObject(task, result);

        given("Role with two archetypeRefs");
        ROLE_INHERITED_ARCHETYPES.importObject(task, result);
        assertRoleAfterByName("Role inherited archetypes test")
                .assignments()
                .assertArchetype(ARCHETYPE_INHERITED_CHILD.oid)
                .end()
                .roleMembershipRefs()
                .assertArchetype(ARCHETYPE_INHERITED_CHILD.oid)
                .assertArchetype(ARCHETYPE_INHERITED_PARENT.oid)
                .end()
                .archetypesRefs()
                .assertArchetype(ARCHETYPE_INHERITED_CHILD.oid)
                .assertArchetype(ARCHETYPE_INHERITED_PARENT.oid);

        when("Change lifecycle state to archived");
        modifyObjectReplaceProperty(RoleType.class, ROLE_INHERITED_ARCHETYPES.oid, RoleType.F_LIFECYCLE_STATE, task, result, LIFECYCLE_ARCHIVED);

        then("Role still has two archetype references");
        assertRoleAfterByName("Role inherited archetypes test")
                .assignments()
                .assertArchetype(ARCHETYPE_INHERITED_CHILD.oid)
                .end()
                .assertRoleMembershipRefs(0)
                .archetypesRefs()
                .assertArchetype(ARCHETYPE_INHERITED_CHILD.oid)
                .assertArchetype(ARCHETYPE_INHERITED_PARENT.oid);
    }
}
