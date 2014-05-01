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

package com.evolveum.midpoint.wf.processors.primary.aspect;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.processors.primary.PcpJob;
import com.evolveum.midpoint.wf.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessSpecificState;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
public abstract class BasePrimaryChangeAspect implements PrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(BasePrimaryChangeAspect.class);

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected WfTaskUtil wfTaskUtil;

    @Autowired
    protected PrimaryChangeProcessor changeProcessor;

    @Autowired
    protected PrimaryChangeAspectHelper primaryChangeAspectHelper;

    @Autowired
    protected ProcessInterfaceFinder processInterfaceFinder;

    @Override
    public List<ObjectDelta<Objectable>> prepareDeltaOut(ProcessEvent event, PcpJob pcpJob, OperationResult result) throws SchemaException {
        return primaryChangeAspectHelper.prepareDeltaOut(event, pcpJob, result);
    }

    @Override
    public List<ObjectReferenceType> prepareApprovedBy(ProcessEvent event, PcpJob job, OperationResult result) {
        return processInterfaceFinder.getProcessInterface(event.getVariables()).prepareApprovedBy(event);
    }

    @Override
    public ProcessSpecificState externalizeProcessInstanceState(Map<String, Object> variables) {
        return processInterfaceFinder.getProcessInterface(variables).externalizeProcessInstanceState(variables);
    }

    public PrimaryChangeProcessor getChangeProcessor() {
        return changeProcessor;
    }
}
