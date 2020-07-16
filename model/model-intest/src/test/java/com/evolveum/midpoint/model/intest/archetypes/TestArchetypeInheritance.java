/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.archetypes;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestArchetypeInheritance extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/archetypes");

    private static final File ARCHETYPE_TASK_BASIC_FILE = new File(TEST_DIR, "archetype-task-basic.xml");

    private static final File ARCHETYPE_RESOURCE_OPERATION_TASK_FILE = new File(TEST_DIR, "archetype-resource-operation-task.xml");

    private static final File ARCHETYPE_RECON_TASK_FILE = new File(TEST_DIR, "archetype-recon-task.xml");
    private static final String ARCHETYPE_RECON_TASK_OID = "00000000-0000-0000-0000-000000000541";

    private static final File ARCHETYPE_LIVE_SYNC_FILE = new File(TEST_DIR, "archetype-liveSync-task.xml");

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

        assertArchetypePolicy(reconTask.asPrismObject())
                .displayType()
                    .assertLabel("Reconciliation task")
                    .assertPluralLabel("Reconciliation tasks")
                    .icon()
                        .assertColor("green")
                        .assertCssClass("fa fa-exchange")
                    .end()
                .end()
                .assertItemConstraints(8)
                .itemConstraints()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY))
                        .hasVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_FINISH_OPERATIONS_ONLY))
                        .hasVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECTCLASS))
                        .hasVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND))
                        .hasVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT))
                        .hasVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_WORKER_THREADS))
                        .hasVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_DRY_RUN))
                        .hasVisibility(UserInterfaceElementVisibilityType.VISIBLE)
                        .end()
                    .itemConstraint(ItemPath.create(TaskType.F_EXTENSION))
                        .hasVisibility(UserInterfaceElementVisibilityType.VACANT)
                        .end()
                    .end()
                .adminGuiConfig()
                    .objectDetails()
                        .type(TaskType.COMPLEX_TYPE)
                        .container()
                            .byIdentifier("resourceOptions")
                                .items()
                                    .item(SchemaConstants.PATH_MODEL_EXTENSION_OBJECT_QUERY)
                                    .item(TaskType.F_OBJECT_REF)
                                    .item(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECTCLASS))
                                    .item(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND))
                                    .item(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));


    }
}
