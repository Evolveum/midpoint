/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.sync;

import static com.evolveum.midpoint.model.api.ModelPublicConstants.FOCUS_VALIDITY_SCAN_ASSIGNMENTS_ID;
import static com.evolveum.midpoint.model.api.ModelPublicConstants.FOCUS_VALIDITY_SCAN_OBJECTS_ID;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.testng.SkipException;

/**
 * TODO
 */
public class TestValidityRecomputeTaskPartitionedSingleTask extends TestValidityRecomputeTask {

    private static final String TASK_PARTITIONED_VALIDITY_SCANNER_FILENAME = COMMON_DIR + "/task-partitioned-validity-scanner-single-task.xml";

    @Override
    protected String getValidityScannerTaskFileName() {
        return TASK_PARTITIONED_VALIDITY_SCANNER_FILENAME;
    }

    @Override
    protected void displayValidityScannerState() throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTask(TASK_VALIDITY_SCANNER_OID, "validity task tree")
                .activityState(ModelPublicConstants.FOCUS_VALIDITY_SCAN_OBJECTS_PATH)
                    .display()
                    .itemProcessingStatistics()
                        .display()
                    .end()
                .end()
                .activityState(ModelPublicConstants.FOCUS_VALIDITY_SCAN_ASSIGNMENTS_PATH)
                    .display()
                    .itemProcessingStatistics()
                        .display()
                    .end()
                .end();
        // @formatter:on
    }

    @Override
    protected void assertLastScanTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end)
            throws ObjectNotFoundException, SchemaException {
        assertLastScanTimestamp(TASK_VALIDITY_SCANNER_OID, ActivityPath.fromId(FOCUS_VALIDITY_SCAN_OBJECTS_ID), start, end);
        assertLastScanTimestamp(TASK_VALIDITY_SCANNER_OID, ActivityPath.fromId(FOCUS_VALIDITY_SCAN_ASSIGNMENTS_ID), start, end);
    }

    @Override
    public void test350CustomValidityScan() {
        throw new SkipException("Not relevant");
    }
}
