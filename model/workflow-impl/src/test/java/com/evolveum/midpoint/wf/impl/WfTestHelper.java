/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

/**
 *
 */
@Component
public class WfTestHelper {

	protected static final Trace LOGGER = TraceManager.getTrace(WfTestHelper.class);

	private boolean verbose = false;

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	public static CaseType findAndRemoveCase0(List<CaseType> subcases) {
	    CaseType case0 = null;
	    for (CaseType subcase : subcases) {
		    if (subcase.getApprovalContext() == null || subcase.getApprovalContext().getApprovalSchema() == null) {
			    assertNull("More than one non-wf-monitoring subtask", case0);
			    case0 = subcase;
		    }
	    }
		if (case0 != null) {
		    subcases.remove(case0);
	    }
	    return case0;
	}

	@NotNull
	public CaseType getRootCase(OperationResult result) throws ObjectNotFoundException, SchemaException {
	    String caseOid = OperationResult.referenceToCaseOid(result.findAsynchronousOperationReference());
		assertNotNull("Case OID is not set in operation result", caseOid);
		return repositoryService.getObject(CaseType.class, caseOid, null, result).asObjectable();
	}

	public CaseType waitForCaseClose(CaseType aCase, final int timeout) throws Exception {
	    final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForCaseClose");
		Holder<CaseType> currentCaseHolder = new Holder<>();
	    Checker checker = new Checker() {
	        @Override
	        public boolean check() throws CommonException {
	            CaseType currentCase = repositoryService.getObject(CaseType.class, aCase.getOid(), null, waitResult).asObjectable();
	            currentCaseHolder.setValue(currentCase);
	            if (verbose) AbstractIntegrationTest.display("Case", currentCase);
	            return SchemaConstants.CASE_STATE_CLOSED.equals(currentCase.getState());
	        }
	        @Override
	        public void timeout() {
	            PrismObject<CaseType> currentCase;
	            try {
	                currentCase = repositoryService.getObject(CaseType.class, aCase.getOid(), null, waitResult);
		            currentCaseHolder.setValue(currentCase.asObjectable());
	            } catch (ObjectNotFoundException | SchemaException e) {
	                throw new AssertionError("Couldn't retrieve case " + aCase, e);
	            }
	            LOGGER.debug("Timed-out case:\n{}", currentCase.debugDump());
	            assert false : "Timeout ("+timeout+") while waiting for "+currentCase+" to finish";
	        }
	    };
	    IntegrationTestTools.waitFor("Waiting for "+aCase+" finish", checker, timeout, 1000);
	    return currentCaseHolder.getValue();
	}
}
