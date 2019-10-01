/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.engine;

import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.casemgmt.api.CaseEventListener;
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
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.request.OpenCaseRequest;
import com.evolveum.midpoint.wf.api.request.Request;
import com.evolveum.midpoint.wf.impl.access.AuthorizationHelper;
import com.evolveum.midpoint.wf.impl.engine.actions.Action;
import com.evolveum.midpoint.wf.impl.engine.actions.ActionFactory;
import com.evolveum.midpoint.wf.impl.engine.helpers.AuditHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.NotificationHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.TriggerHelper;
import com.evolveum.midpoint.wf.impl.engine.helpers.WorkItemHelper;
import com.evolveum.midpoint.wf.impl.execution.ExecutionHelper;
import com.evolveum.midpoint.wf.impl.processes.common.ExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.impl.processes.common.StageComputeHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * As a replacement of Activiti, this class manages execution of approval cases:
 * it starts, advances, and stops approval process instances represented by CaseType objects.
 *
 * Handling of concurrency issues: each request is carried out solely "in memory". Only when committing it the
 * case object version is checked in repository. If it was changed in the meanwhile the request is aborted and repeated.
 */
@Component
public class WorkflowEngine implements CaseEventListener {

	private static final Trace LOGGER = TraceManager.getTrace(WorkflowEngine.class);
	private static final String OP_EXECUTE_REQUEST = WorkflowEngine.class.getName() + ".executeRequest";

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
	@Autowired public ExecutionHelper executionHelper;
	@Autowired private ActionFactory actionFactory;
	@Autowired private CaseEventDispatcher caseEventDispatcher;
	@Autowired private TaskManager taskManager;

	@PostConstruct
	public void init() {
		caseEventDispatcher.registerCaseCreationEventListener(this);
	}

	@PreDestroy
	public void destroy() {
		caseEventDispatcher.unregisterCaseCreationEventListener(this);
	}

	private static final int MAX_ATTEMPTS = 10;

	/**
	 * Executes a request. This is the main entry point.
	 */
	public void executeRequest(Request request, Task opTask, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
			ExpressionEvaluationException, ConfigurationException, CommunicationException {
		int attempt = 1;
		for (;;) {
			OperationResult result = parentResult.subresult(OP_EXECUTE_REQUEST)
					.setMinor()
					.addParam("attempt", attempt)
					.addArbitraryObjectAsParam("request", request)
					.build();
			try {
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
						throw new SystemException("Couldn't execute " + request.getClass() + " in " + MAX_ATTEMPTS + " attempts",
								e);
					}
				}
			} catch (Throwable t) {
				result.recordFatalError(t);
				throw t;
			} finally {
				result.computeStatusIfUnknown();
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

	/**
	 * Here we collect case creation events generated by the built-in manual connector.
	 */
	@Override
	public void onCaseCreation(CaseType aCase, OperationResult result) {
		Task opTask = taskManager.createTaskInstance(WorkflowEngine.class.getName() + ".onCaseCreation");
		try {
			executeRequest(new OpenCaseRequest(aCase.getOid()), opTask, result);
		} catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | SecurityViolationException |
				ExpressionEvaluationException | ConfigurationException | CommunicationException e) {
			throw new SystemException("Couldn't open the case: " + aCase, e);
		}
	}
}
