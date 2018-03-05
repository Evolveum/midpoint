/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.common;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.runtime.ExecutionImpl;
import org.activiti.engine.runtime.Execution;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

/**
 * @author mederly
 */
public class DumpVariables implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(DumpVariables.class);

    @Override
    public void execute(DelegateExecution execution) {

        if (LOGGER.isTraceEnabled()) {

			RuntimeService runtimeService = execution.getEngineServices().getRuntimeService();
        	Set<String> variablesSeen = new HashSet<>();

            LOGGER.trace("--------------------------------- DumpVariables: " + execution.getCurrentActivityId());
//			LOGGER.trace("All variables for execution id={}, parent id={}", execution.getId(), execution.getParentId());
//            execution.getVariableInstances().forEach((n, v) -> LOGGER.trace(" - {} = {} (in {})", n, v.getValue(), v.getExecutionId()));

            LOGGER.trace("Execution hierarchy for id={}", execution.getId());
            LOGGER.trace("------------------------------------");
            dumpExecutionVariables(execution.getId(), execution, null, variablesSeen, runtimeService);

			Collection<String> disjunction = CollectionUtils.disjunction(variablesSeen, execution.getVariableNames());
			if (!disjunction.isEmpty()) {
				LOGGER.trace("*** Variables not found in execution tree or 'execution.getVariableNames()': {}", disjunction);
			}
            LOGGER.trace("--------------------------------- DumpVariables: " + execution.getCurrentActivityId() + " END ------------");
        }

    }

	private void dumpExecutionVariables(String executionId, DelegateExecution delegateExecution, Execution execution, Set<String> variablesSeen, RuntimeService runtimeService) {
		Map<String, Object> variablesLocal = runtimeService.getVariablesLocal(executionId);
		LOGGER.trace("Execution id={} ({} variables); class={}/{}", executionId, variablesLocal.size(),
				delegateExecution != null ? delegateExecution.getClass().getName() : null,
				execution != null ? execution.getClass().getName() : null);
		TreeSet<String> names = new TreeSet<>(variablesLocal.keySet());
		names.forEach(n -> LOGGER.trace(" - {} = {} {}", n, variablesLocal.get(n), variablesSeen.contains(n) ? "(dup)":""));
		variablesSeen.addAll(variablesLocal.keySet());
		if (delegateExecution instanceof ExecutionEntity) {
			ExecutionEntity executionEntity = (ExecutionEntity) delegateExecution;
			if (executionEntity.getParent() != null) {
				dumpExecutionVariables(executionEntity.getParentId(), executionEntity.getParent(), null, variablesSeen,
						runtimeService);
			}
		} else if (delegateExecution instanceof ExecutionImpl) {
			ExecutionImpl executionImpl = (ExecutionImpl) delegateExecution;
			if (executionImpl.getParent() != null) {
				dumpExecutionVariables(executionImpl.getParentId(), executionImpl.getParent(), null, variablesSeen,
						runtimeService);
			}
		} else {
			Execution execution1 = runtimeService.createExecutionQuery().executionId(executionId).singleResult();
			if (execution1 == null) {
				LOGGER.trace("Execution with id {} was not found.", executionId);
			} else if (execution1.getParentId() != null) {
				Execution execution2 = runtimeService.createExecutionQuery().executionId(execution1.getParentId()).singleResult();
				dumpExecutionVariables(execution.getParentId(), null, execution2, variablesSeen, runtimeService);
			}
		}
	}

}
