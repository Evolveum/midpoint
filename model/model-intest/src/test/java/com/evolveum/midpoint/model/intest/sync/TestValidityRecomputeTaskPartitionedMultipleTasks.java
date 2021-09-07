/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.sync;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;

import javax.xml.datatype.XMLGregorianCalendar;

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
        waitForTaskTreeNextFinishedRun(TASK_VALIDITY_SCANNER_OID, DEFAULT_TASK_WAIT_TIMEOUT);
    }

    @Override
    protected void waitForValidityNextRunAssertSuccess() throws Exception {
        OperationResult result = waitForTaskTreeNextFinishedRun(TASK_VALIDITY_SCANNER_OID, DEFAULT_TASK_WAIT_TIMEOUT);
        TestUtil.assertSuccess(result);
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
}
