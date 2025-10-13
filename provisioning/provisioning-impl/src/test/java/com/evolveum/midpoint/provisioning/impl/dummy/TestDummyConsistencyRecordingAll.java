/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import com.evolveum.midpoint.test.asserter.PendingOperationsAsserter;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;

import java.io.File;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType.FATAL_ERROR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;

/**
 * Variation of {@link TestDummyConsistency} but we want to record all pending operations - and avoid duplicates.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyConsistencyRecordingAll extends TestDummyConsistency {

    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy-retry-recording-all.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    void assertPendingOperationsAfter812(PendingOperationsAsserter<?> asserter) {
        // @formatter:off
        asserter.singleOperation()
                    .delta()
                        .assertModify()
                    .end()
                    .assertHasCompletionTimestamp()
                    .assertExecutionStatus(COMPLETED)
                    .assertResultStatus(FATAL_ERROR);
        // @formatter:on
    }

    @Override
    void assertPendingOperationsAfter814(PendingOperationsAsserter<?> asserter) {
        asserter.assertOperations(3);
    }
}
