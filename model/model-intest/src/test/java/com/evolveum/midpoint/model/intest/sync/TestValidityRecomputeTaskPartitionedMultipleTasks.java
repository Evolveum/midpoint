/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.sync;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.testng.SkipException;

/**
 * TODO
 */
public class TestValidityRecomputeTaskPartitionedMultipleTasks extends TestValidityRecomputeTask {

    private static final String TASK_PARTITIONED_VALIDITY_SCANNER_FILENAME = COMMON_DIR + "/task-partitioned-validity-scanner-multiple-tasks.xml";

    @Override
    protected String getValidityScannerTaskFileName() {
        return TASK_PARTITIONED_VALIDITY_SCANNER_FILENAME;
    }

    @Override
    protected void waitForValidityTaskFinish() throws Exception {
        waitForNextRootActivityCompletion(TASK_VALIDITY_SCANNER_OID, DEFAULT_TASK_WAIT_TIMEOUT);
    }

    @Override
    protected void waitForValidityNextRunAssertSuccess() throws Exception {
        Task completed = waitForNextRootActivityCompletion(TASK_VALIDITY_SCANNER_OID, DEFAULT_TASK_WAIT_TIMEOUT);
        // Because of implementation reasons, we won't check the operation result of the task and all its subtasks.
        // We simply check the number of errors here.
        assertTask(completed, "after")
                .rootActivityStateOverview()
                    .assertSuccess()
                    .assertNoErrors();
        displayValidityScannerState();
    }

    @Override
    protected void displayValidityScannerState() throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(TASK_VALIDITY_SCANNER_OID, "validity task tree")
                .subtaskForPath(ModelPublicConstants.FOCUS_VALIDITY_SCAN_OBJECTS_PATH)
                    .activityState(ModelPublicConstants.FOCUS_VALIDITY_SCAN_OBJECTS_PATH)
                        .display()
                        .itemProcessingStatistics()
                            .display()
                        .end()
                    .end()
                .end()
                .subtaskForPath(ModelPublicConstants.FOCUS_VALIDITY_SCAN_ASSIGNMENTS_PATH)
                    .activityState(ModelPublicConstants.FOCUS_VALIDITY_SCAN_ASSIGNMENTS_PATH)
                        .display()
                        .itemProcessingStatistics()
                            .display()
                        .end()
                    .end();
        // @formatter:on
    }

    @Override
    protected void assertLastScanTimestamp(XMLGregorianCalendar startCal, XMLGregorianCalendar endCal)
            throws ObjectNotFoundException, SchemaException {
        OperationResult result = getTestOperationResult();
        Task master = taskManager.getTaskPlain(TASK_VALIDITY_SCANNER_OID, result);
        for (Task subtask : master.listSubtasks(result)) {
            ActivityPath localRootPath = ActivityStateUtil.getLocalRootPath(subtask.getActivitiesStateOrClone());
            assertTask(subtask.getOid(), "")
                    .activityState(localRootPath)
                        .display()
                        .itemProcessingStatistics()
                            .display()
                        .end()
                    .end()
                    .assertLastScanTimestamp(localRootPath, startCal, endCal);
        }
    }

    @Override
    public void test350CustomValidityScan() {
        throw new SkipException("Not relevant");
    }
}
