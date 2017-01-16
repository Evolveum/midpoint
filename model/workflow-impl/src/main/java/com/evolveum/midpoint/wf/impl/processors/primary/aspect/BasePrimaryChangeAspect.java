/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.wf.impl.processors.primary.aspect;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.BaseModelInvocationProcessingHelper;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.wf.impl.processors.BaseConfigurationHelper;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpWfTask;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PrimaryChangeProcessorConfigurationType;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author mederly
 */
public abstract class BasePrimaryChangeAspect implements PrimaryChangeAspect, BeanNameAware {

    private static final Trace LOGGER = TraceManager.getTrace(BasePrimaryChangeAspect.class);

    private String beanName;

    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    @Autowired
    protected WfTaskUtil wfTaskUtil;

    @Autowired
    protected PrimaryChangeProcessor changeProcessor;

    @Autowired
    protected PrimaryChangeAspectHelper primaryChangeAspectHelper;

    @Autowired
    protected ProcessInterfaceFinder processInterfaceFinder;

    @Autowired
    protected BaseConfigurationHelper baseConfigurationHelper;

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ItemApprovalProcessInterface itemApprovalProcessInterface;

    @Autowired
    protected MiscDataUtil miscDataUtil;

	@Autowired
	protected BaseModelInvocationProcessingHelper baseModelInvocationProcessingHelper;

	@PostConstruct
    public void init() {
        changeProcessor.registerChangeAspect(this);
    }

    @Override
    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public ObjectTreeDeltas prepareDeltaOut(ProcessEvent event, PcpWfTask pcpJob, OperationResult result) throws SchemaException {
        return primaryChangeAspectHelper.prepareDeltaOut(event, pcpJob, result);
    }

    @Override
    public List<ObjectReferenceType> prepareApprovedBy(ProcessEvent event, PcpWfTask job, OperationResult result) {
        return processInterfaceFinder.getProcessInterface(event.getVariables()).prepareApprovedBy(event);
    }

    public PrimaryChangeProcessor getChangeProcessor() {
        return changeProcessor;
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;       // overriden in selected aspects
    }

    @Override
    public boolean isEnabled(PrimaryChangeProcessorConfigurationType processorConfigurationType) {
        return primaryChangeAspectHelper.isEnabled(processorConfigurationType, this);
    }
}
