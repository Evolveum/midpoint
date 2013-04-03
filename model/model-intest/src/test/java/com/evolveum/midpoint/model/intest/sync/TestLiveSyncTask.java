/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.intest.sync;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLiveSyncTask extends AbstractSynchronizationStoryTest {
		
	public TestLiveSyncTask() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResourceGreen.setSyncStyle(DummySyncStyle.SMART);
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		dummyResourceBlue.setSyncStyle(DummySyncStyle.SMART);
		
	}
	
	@Override
	protected void importSyncTask(PrismObject<ResourceType> resource) throws FileNotFoundException {
		if (resource == resourceDummyGreen) {
			importObjectFromFile(TASK_LIVE_SYNC_DUMMY_GREEN_FILENAME);
		} else if (resource == resourceDummyBlue) {
			importObjectFromFile(TASK_LIVE_SYNC_DUMMY_BLUE_FILENAME);
		} else if (resource == resourceDummy) {
			importObjectFromFile(TASK_LIVE_SYNC_DUMMY_FILENAME);
		} else {
			throw new IllegalArgumentException("Unknown resource "+resource);
		}
	}

	@Override
	protected String getSyncTaskOid(PrismObject<ResourceType> resource) {
		if (resource == resourceDummyGreen) {
			return TASK_LIVE_SYNC_DUMMY_GREEN_OID;
		} else if (resource == resourceDummyBlue) {
			return TASK_LIVE_SYNC_DUMMY_BLUE_OID;
		} else if (resource == resourceDummy) {
			return TASK_LIVE_SYNC_DUMMY_OID;
		} else {
			throw new IllegalArgumentException("Unknown resource "+resource);
		}
	}

}
