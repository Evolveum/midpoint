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

package com.evolveum.midpoint.wf.impl.engine;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.request.Request;
import com.evolveum.midpoint.wf.impl.access.AuthorizationHelper;
import com.evolveum.midpoint.wf.impl.engine.actions.Action;
import com.evolveum.midpoint.wf.impl.engine.actions.ActionFactory;
import com.evolveum.midpoint.wf.impl.engine.helpers.AuditHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.NotificationHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.TriggerHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.WorkItemHelper;
import com.evolveum.midpoint.wf.impl.processes.common.ExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.impl.processes.common.StageComputeHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * As a replacement of Activiti, this class manages execution of approval cases:
 * it starts, advances, and stops approval process instances represented by CaseType objects.
 *
 * Handling of concurrency issues: each request is carried out solely "in memory". Only when committing it the
 * case object version is checked in repository. If it was changed in the meanwhile the request is aborted and repeated.
 */
@Component
public class WorkflowEngine {

	private static final Trace LOGGER = TraceManager.getTrace(WorkflowEngine.class);

	@Autowired public Clock clock;
	@Autowired
	@Qualifier("cacheRepositoryService")
	public RepositoryService repositoryService;
	@Autowired public PrismContext prismContext;
	@Autowired public SecurityEnforcer securityEnforcer;
	@Autowired public AuditHelper auditHelper;
	@Autowired public NotificationHelper notificationHelper;
	@Autowired public StageComputeHelper stageComputeHelper;
	@Autowired public PrimaryChangeProcessor primaryChangeProcessor;   // todo
	@Autowired public MiscHelper miscHelper;
	@Autowired public TriggerHelper triggerHelper;
	@Autowired public ExpressionEvaluationHelper expressionEvaluationHelper;
	@Autowired public WorkItemHelper workItemHelper;
	@Autowired public AuthorizationHelper authorizationHelper;
	@Autowired private ActionFactory actionFactory;

	private static final int MAX_ATTEMPTS = 10;

	/**
	 * Executes a request. This is the main entry point.
	 */

	public void executeRequest(Request request, Task opTask, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
			ExpressionEvaluationException, ConfigurationException, CommunicationException {
		int attempt = 1;
		for (;;) {
			EngineInvocationContext ctx = createInvocationContext(request.getCaseOid(), opTask, result);
			Action firstAction = actionFactory.create(request, ctx);
			executeActionChain(firstAction, result);
			try {
				ctx.commit(result);
				return;
			} catch (PreconditionViolationException e) {
				boolean repeat = attempt < MAX_ATTEMPTS;
				String action = repeat ? "retried" : "aborted";
				LOGGER.info("Approval commit conflict detected; operation will be {} (this was attempt {} of {})",
						action, attempt, MAX_ATTEMPTS);
				if (repeat) {
					attempt++;
				} else {
					throw new SystemException("Couldn't execute " + request.getClass() + " in " + MAX_ATTEMPTS + " attempts", e);
				}
			}
		}
	}

	private EngineInvocationContext createInvocationContext(String caseOid, Task opTask, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		PrismObject<CaseType> caseObject = repositoryService.getObject(CaseType.class, caseOid, null, result);
		return new EngineInvocationContext(caseObject.asObjectable(), opTask, this, getMidPointPrincipal());
	}

	private MidPointPrincipal getMidPointPrincipal() {
		MidPointPrincipal user;
		try {
			user = SecurityUtil.getPrincipal();
		} catch (SecurityViolationException e) {
			throw new SystemException("Couldn't get midPoint principal: " + e.getMessage(), e);
		}
		if (user == null) {
			throw new SystemException("No principal");
		}
		return user;
	}

	private void executeActionChain(Action action, OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException {
		while (action != null) {
			action = action.execute(result);
		}
	}
}
