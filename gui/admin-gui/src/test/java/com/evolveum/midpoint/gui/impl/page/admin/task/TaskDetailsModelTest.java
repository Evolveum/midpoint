/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task;

import static org.testng.AssertJUnit.*;

import com.evolveum.midpoint.schema.util.task.ActivityDefinitionBuilder;
import com.evolveum.midpoint.test.AbstractHigherUnitTest;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReconciliationWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.testng.annotations.Test;

/**
 * Tests preparation of tasks before they are added, in particular keeping the task object reference
 * consistent with the resource selected in resource-object work definitions.
 *
 * Covers both newly created and duplicated reconciliation tasks, as well as non-resource tasks
 * that must remain unaffected.
 */
public class TaskDetailsModelTest extends AbstractHigherUnitTest {

    private static final String RESOURCE_A_OID = "00000000-0000-0000-0000-00000000000a";
    private static final String RESOURCE_B_OID = "00000000-0000-0000-0000-00000000000b";
    private static final String OBJECT_X_OID = "00000000-0000-0000-0000-00000000000c";

    @Test
    public void testPrepareObjectForAddUpdatesDuplicatedReconciliationTaskObjectRef() throws Exception {
        TaskType task = resourceTask(resourceRef(RESOURCE_B_OID))
                .objectRef(resourceRef(RESOURCE_A_OID));

        prepareObjectForAdd(task);

        assertResourceRef(task.getObjectRef(), RESOURCE_B_OID);
        assertResourceRef(getReconciliationResourceRef(task), RESOURCE_B_OID);
    }

    @Test
    public void testPrepareObjectForAddSetsFreshResourceTaskObjectRef() throws Exception {
        TaskType task = resourceTask(resourceRef(RESOURCE_B_OID));

        prepareObjectForAdd(task);

        assertResourceRef(task.getObjectRef(), RESOURCE_B_OID);
        assertResourceRef(getReconciliationResourceRef(task), RESOURCE_B_OID);
    }

    @Test
    public void testPrepareObjectForAddDoesNotChangeNonResourceTaskObjectRef() throws Exception {
        TaskType task = cleanupTask().objectRef(resourceRef(OBJECT_X_OID));

        prepareObjectForAdd(task);

        assertResourceRef(task.getObjectRef(), OBJECT_X_OID);
    }

    @Test
    public void testPrepareObjectForAddClearsObjectRefWhenResourceIsCleared() throws Exception {
        TaskType task = resourceTask(null).objectRef(resourceRef(RESOURCE_A_OID));

        prepareObjectForAdd(task);

        assertNull(task.getObjectRef());
        assertNull(getReconciliationResourceRef(task));
    }

    private void prepareObjectForAdd(TaskType task) throws CommonException {
        new TaskDetailsModel(null, null)
                .prepareObjectForAdd(task.asPrismObject());
    }

    private TaskType resourceTask(ObjectReferenceType resourceRef) {
        ResourceObjectSetType resourceObjects = new ResourceObjectSetType();
        if (resourceRef != null) {
            resourceObjects.setResourceRef(resourceRef);
        }

        return new TaskType()
                .activity(ActivityDefinitionBuilder.create(new WorkDefinitionsType()
                                .reconciliation(new ReconciliationWorkDefinitionType()
                                        .resourceObjects(resourceObjects)))
                        .build());
    }

    private TaskType cleanupTask() {
        return new TaskType()
                .activity(ActivityDefinitionBuilder.create(new WorkDefinitionsType()
                                .cleanup(new CleanupWorkDefinitionType()))
                        .build());
    }

    private ObjectReferenceType getReconciliationResourceRef(TaskType task) {
        return task.getActivity()
                .getWork()
                .getReconciliation()
                .getResourceObjects()
                .getResourceRef();
    }

    private ObjectReferenceType resourceRef(String oid) {
        return new ObjectReferenceType()
                .oid(oid)
                .type(ResourceType.COMPLEX_TYPE);
    }

    private void assertResourceRef(ObjectReferenceType ref, String oid) {
        assertNotNull(ref);
        assertEquals(oid, ref.getOid());
        assertEquals(ResourceType.COMPLEX_TYPE, ref.getType());
    }
}
