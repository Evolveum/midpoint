/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Some notes about the workflow package and its classes/subpackages:
 *
 * External interfaces:
 * - WorkflowManager + WfDataAccessor: externally-visible functionality (initialization, methods needed by GUI).
 * - WfHook: interface to the model ChangeHook mechanism
 * - WfProcessShadowTaskHandler: interface to the midPoint task scheduling mechanism (for active tasks)
 * - ActivitiInterface: communication with Activiti (currently only local instance, in the future probably remote as well)
 *
 * Core:
 * - WfCore: core functionality of the workflow subsystem
 * - processes.PrimaryApprovalProcessWrapper and its implementations, processes.*:
 *     functionality specific to particular workflow processes
 *
 * Data containers:
 * - messages.*: data that is transferred from/to Activiti
 * - WorkItem: objects for communication with the GUI
 *
 * Helper classes:
 * - WfTaskUtil: utility methods to work with tasks as wf process instance "mirrors"
 * - WfConfiguration: container for the configuration of the workflow module
 * - WfConstants: some constants (e.g. names of commonly used process variables)
 * - ActivitiEngine: management of the locally-run engine
 * - activiti.* (other): helpher classes for BPMN processes
 *
 *
 *
 * @author mederly
 */
@Component("workflowManager")
@DependsOn({ "sqlRepositoryFactory",
             "repositoryService",
             "taskManager",
             "wfConfiguration"
              })        // todo hookRegistry

public class WorkflowManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkflowManager.class);

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private HookRegistry hookRegistry;

    @Autowired(required = true)
    private TaskManager taskManager;

    @Autowired
    private WorkflowServiceImpl workflowServiceImpl;

    @Autowired
    private WfConfiguration wfConfiguration;

    public WfConfiguration getWfConfiguration() {
        return wfConfiguration;
    }

    public boolean isEnabled() {
        return wfConfiguration.isEnabled();
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public WorkflowServiceImpl getDataAccessor() {
        return workflowServiceImpl;
    }
}
