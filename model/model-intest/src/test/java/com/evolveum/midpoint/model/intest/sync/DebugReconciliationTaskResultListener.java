/*
 * Copyright (c) 2013 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskResult;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskResultListener;
import com.evolveum.midpoint.test.IntegrationTestTools;

public class DebugReconciliationTaskResultListener implements
		ReconciliationTaskResultListener {

	private List<ReconciliationTaskResult> results = Collections.synchronizedList(new ArrayList<ReconciliationTaskResult>());
	
	@Override
	public void process(ReconciliationTaskResult reconResult) {
		results.add(reconResult);
		IntegrationTestTools.display("Recon task result", reconResult);
	}
	
	public void clear() {
		results.clear();
	}

	public void assertResult(String resourceOid, long expectedUnOpsCount, long expectedResourceReconCount, long expectedResourceReconErrors, 
			long expectedShadowReconCount) {
		ReconciliationTaskResult result = findResult(resourceOid);
		assertNotNull("No recon result for resource "+resourceOid, result);
		assertEquals("Wrong upOpsCount in recon result for resource "+resourceOid, 
				expectedUnOpsCount, result.getUnOpsCount());
		assertEquals("Wrong resourceReconCount in recon result for resource "+resourceOid, 
				expectedResourceReconCount, result.getResourceReconCount());
		assertEquals("Wrong resourceReconCount in recon result for resource "+resourceOid, 
				expectedResourceReconErrors, result.getResourceReconErrors());
		assertEquals("Wrong shadowReconCount in recon result for resource "+resourceOid, 
				expectedShadowReconCount, result.getShadowReconCount());
	}

	private ReconciliationTaskResult findResult(String resourceOid) {
		for (ReconciliationTaskResult result: results) {
			if (resourceOid.equals(result.getResource().getOid())) {
				return result;
			}
		}
		return null;
	}

}
