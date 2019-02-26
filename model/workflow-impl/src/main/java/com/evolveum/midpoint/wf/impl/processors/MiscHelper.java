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

package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
@Component
public class MiscHelper {

    private static final Trace LOGGER = TraceManager.getTrace(MiscHelper.class);

    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private PrismContext prismContext;
	@Autowired private ModelInteractionService modelInteractionService;
	@Autowired private MiscDataUtil miscDataUtil;

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	public PrismObject<UserType> getRequesterIfExists(CaseType aCase, OperationResult result) {
		if (aCase == null || aCase.getRequestorRef() == null) {
			return null;
		}
		ObjectReferenceType requesterRef = aCase.getRequestorRef();
		//noinspection unchecked
		return (PrismObject<UserType>) miscDataUtil.resolveAndStoreObjectReference(requesterRef, result);
	}

	public String getCompleteStageInfo(CaseType aCase) {
		return WfContextUtil.getCompleteStageInfo(aCase);
	}

	public String getAnswerNice(CaseType aCase) {
		return ApprovalUtils.makeNiceFromUri(getOutcome(aCase));
	}

	private String getOutcome(CaseType aCase) {
		return aCase.getWorkflowContext() != null ? aCase.getOutcome() : null;
	}

	public List<ObjectReferenceType> getAssigneesAndDeputies(CaseWorkItemType workItem, Task task, OperationResult result)
			throws SchemaException {
		List<ObjectReferenceType> rv = new ArrayList<>();
		rv.addAll(workItem.getAssigneeRef());
		rv.addAll(modelInteractionService.getDeputyAssignees(workItem, task, result));
		return rv;
	}

	public List<CaseType> getSubcases(CaseType rootCase, OperationResult result) throws SchemaException {
		return getSubcases(rootCase.getOid(), result);
	}

	public List<CaseType> getSubcases(String oid, OperationResult result) throws SchemaException {
		return repositoryService.searchObjects(CaseType.class,
				prismContext.queryFor(CaseType.class)
					.item(CaseType.F_PARENT_REF).ref(oid)
					.build(),
				null,
				result)
				.stream()
					.map(o -> o.asObjectable())
					.collect(Collectors.toList());
	}

}
