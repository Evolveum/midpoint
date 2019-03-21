/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;

/**
 * @author katka
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholdsReconFull extends TestThresholds {

	private static final File TASK_RECONCILE_OPENDJ_FULL_FILE = new File(TEST_DIR, "task-opendj-reconcile-full.xml");
	private static final String TASK_RECONCILE_OPENDJ_FULL_OID = "20335c7c-838f-11e8-93a6-4b1dd0ab58e4";
	

	@Override
	protected File getTaskFile() {
		return TASK_RECONCILE_OPENDJ_FULL_FILE;
	}
	
	@Override
	protected String getTaskOid() {
		return TASK_RECONCILE_OPENDJ_FULL_OID;
	}

	@Override
	protected int getProcessedUsers() {
		return 4;
	}

	@Override
	protected void assertSynchronizationStatisticsAfterImport(Task taskAfter) throws Exception {
		IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
		assertEquals(infoType.getTotalFailureCount(), 1);
		
		SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
		
		assertEquals(syncInfo.getCountUnmatched(), 5);
		assertEquals(syncInfo.getCountDeleted(), 0);
		assertEquals(syncInfo.getCountLinked(), getDefaultUsers());
		assertEquals(syncInfo.getCountUnlinked(), 0);
		
		assertEquals(syncInfo.getCountUnmatchedAfter(), 0);
		assertEquals(syncInfo.getCountDeleted(), 0);
		assertEquals(syncInfo.getCountLinkedAfter(), getDefaultUsers() + getProcessedUsers());
		assertEquals(syncInfo.getCountUnlinked(), 0);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.testing.story.TestThresholds#assertSynchronizationStatisticsAfterSecondImport(com.evolveum.midpoint.task.api.Task)
	 */
	@Override
	protected void assertSynchronizationStatisticsAfterSecondImport(Task taskAfter) throws Exception {
		IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
		assertEquals(infoType.getTotalFailureCount(), 1);
		
		SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
		
		assertEquals(syncInfo.getCountUnmatched(), 5);
		assertEquals(syncInfo.getCountDeleted(), 0);
		assertEquals(syncInfo.getCountLinked(), getDefaultUsers()+getProcessedUsers());
		assertEquals(syncInfo.getCountUnlinked(), 0);
		
		assertEquals(syncInfo.getCountUnmatchedAfter(), 0);
		assertEquals(syncInfo.getCountDeleted(), 0);
		assertEquals(syncInfo.getCountLinkedAfter(), getDefaultUsers() + getProcessedUsers()*2);
		assertEquals(syncInfo.getCountUnlinked(), 0);
	}
	
	protected void assertSynchronizationStatisticsActivation(Task taskAfter) {
		IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
		assertEquals(infoType.getTotalFailureCount(), 1);
		
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnmatched(), 5);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountDeleted(), 0);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountLinked(), getDefaultUsers() + getProcessedUsers());
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnlinked(), 0);
		
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnmatchedAfter(), 0);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountDeleted(), 0);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountLinked(), getDefaultUsers() + getProcessedUsers());
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnlinked(), 0);
	}
	
}
