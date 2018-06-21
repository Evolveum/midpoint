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

package com.evolveum.midpoint.model.intest.sync;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author mederly
 */
public class TestValidityRecomputeTaskPartitioned extends TestValidityRecomputeTask {

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
	}

	@Override
	protected void assertLastScanTimestamp(String taskOid, XMLGregorianCalendar startCal, XMLGregorianCalendar endCal)
			throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException {
		OperationResult result = new OperationResult(TestValidityRecomputeTaskPartitioned.class.getName() + ".assertLastScanTimestamp");
		Task master = taskManager.getTask(taskOid, result);
		for (Task subtask : master.listSubtasks(result)) {
			super.assertLastScanTimestamp(subtask.getOid(), startCal, endCal);
		}
	}
}
