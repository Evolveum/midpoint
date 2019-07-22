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
public class TestThresholdsLiveSyncFull extends TestThresholds {
	
	private static final File TASK_LIVESYNC_OPENDJ_FULL_FILE = new File(TEST_DIR, "task-opendj-livesync-full.xml");
	private static final String TASK_LIVESYNC_OPENDJ_FULL_OID = "10335c7c-838f-11e8-93a6-4b1dd0ab58e4";
	
	
	@Override
	protected File getTaskFile() {
		return TASK_LIVESYNC_OPENDJ_FULL_FILE;
	}

	@Override
	protected String getTaskOid() {
		return TASK_LIVESYNC_OPENDJ_FULL_OID;
	}

	@Override
	protected int getProcessedUsers() {
		return 4;
	}

	@Override
	protected void assertSynchronizationStatisticsAfterImport(Task taskAfter) throws Exception {
		SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
		
		assertSyncToken(taskAfter, 8, taskAfter.getResult());
		
		assertEquals(syncInfo.getCountUnmatched(), 5);
		assertEquals(syncInfo.getCountDeleted(), 0);
		assertEquals(syncInfo.getCountLinked(), 0);
		assertEquals(syncInfo.getCountUnlinked(), 0);
		
		assertEquals(syncInfo.getCountUnmatchedAfter(), 0);
		assertEquals(syncInfo.getCountDeletedAfter(), 0);
		assertEquals(syncInfo.getCountLinkedAfter(), getProcessedUsers());
		assertEquals(syncInfo.getCountUnlinkedAfter(), 0);
		
	}
	
	protected void assertSynchronizationStatisticsActivation(Task taskAfter) {
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnmatched(), 3);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountDeleted(), 0);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountLinked(), 0);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnlinked(), 0);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.testing.story.TestThresholds#assertSynchronizationStatisticsAfterSecondImport(com.evolveum.midpoint.task.api.Task)
	 */
	@Override
	protected void assertSynchronizationStatisticsAfterSecondImport(Task taskAfter) throws Exception {
		SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
		
		assertSyncToken(taskAfter, 12, taskAfter.getResult());
		
		assertEquals(syncInfo.getCountUnmatched(), 5);
		assertEquals(syncInfo.getCountDeleted(), 0);
		assertEquals(syncInfo.getCountLinked(), 0);
		assertEquals(syncInfo.getCountUnlinked(), 0);
		
		assertEquals(syncInfo.getCountUnmatchedAfter(), 0);
		assertEquals(syncInfo.getCountDeletedAfter(), 0);
		assertEquals(syncInfo.getCountLinkedAfter(), getProcessedUsers());
		assertEquals(syncInfo.getCountUnlinkedAfter(), 0);
	}
}
