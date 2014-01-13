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

package com.evolveum.midpoint.wf.processors.primary.wrapper;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.processes.itemApproval.Constants;
import com.evolveum.midpoint.wf.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.processors.primary.PcpJob;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author Pavol
 */
public abstract class BaseWrapper implements PrimaryApprovalProcessWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(BaseWrapper.class);

    public static final String GENERAL_APPROVAL_PROCESS = "ItemApproval";
    private static final String DEFAULT_PROCESS_INSTANCE_DETAILS_PANEL_NAME = Constants.DEFAULT_PANEL_NAME;

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected WfTaskUtil wfTaskUtil;

    @Autowired
    protected PrimaryChangeProcessor changeProcessor;

    @Autowired
    protected WrapperHelper wrapperHelper;

    @Override
    public List<ObjectDelta<Objectable>> prepareDeltaOut(ProcessEvent event, PcpJob pcpJob, OperationResult result) throws SchemaException {
        return wrapperHelper.prepareDeltaOut(event, pcpJob, result);
    }

    @Override
    public List<ObjectReferenceType> getApprovedBy(ProcessEvent event) {
        return wrapperHelper.getApprovedBy(event);
    }

    public PrimaryChangeProcessor getChangeProcessor() {
        return changeProcessor;
    }

    @Override
    public String getProcessInstanceDetailsPanelName(WfProcessInstanceType processInstance) {
        return DEFAULT_PROCESS_INSTANCE_DETAILS_PANEL_NAME;
    }
}
