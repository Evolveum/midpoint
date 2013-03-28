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

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.activiti.engine.FormService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.FormType;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * Some notes about the workflow package and its classes/subpackages:
 *
 * External interfaces:
 * - WorkflowManager + WfDataAccessor: externally-visible functionality (initialization, methods needed by GUI).
 * - WfHook: interface to the model ChangeHook mechanism
 * - WfTaskHandler: interface to the midPoint task scheduling mechanism (for active tasks)
 * - ActivitiInterface: communication with Activiti (currently only local instance, in the future probably remote as well)
 *
 * Core:
 * - WfCore: core functionality of the workflow subsystem
 * - processes.ProcessWrapper and its implementations, processes.*:
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
@Component
//@DependsOn("repositoryService")
@DependsOn("sqlRepositoryFactory")
public class WorkflowManager implements BeanFactoryAware {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkflowManager.class);

    @Autowired(required = true)
    private ActivitiEngine activitiEngine;

    @Autowired(required = true)
    private WfHook wfHook;

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private MidpointConfiguration midpointConfiguration;

    @Autowired(required = true)
    private HookRegistry hookRegistry;

    @Autowired(required = true)
    private RepositoryService repositoryService;

    @Autowired(required = true)
    private WfCore wfCore;

    private WfConfiguration wfConfiguration;

    private WfDataAccessor wfDataAccessor;

    private BeanFactory beanFactory;

    @PostConstruct
    public void initialize() {

        LOGGER.info("Initializing WorkflowManager...");
        wfConfiguration = new WfConfiguration();
        wfConfiguration.initialize(midpointConfiguration, beanFactory);

        if (!wfConfiguration.isEnabled()) {
            LOGGER.info("Workflow management is not enabled.");
        } else {
            activitiEngine.initialize(wfConfiguration);
            wfHook.register(hookRegistry);
        }

        wfDataAccessor = new WfDataAccessor(this);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public WfConfiguration getWfConfiguration() {
        return wfConfiguration;
    }

    public boolean isEnabled() {
        return wfConfiguration.isEnabled();
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public ActivitiEngine getActivitiEngine() {
        return activitiEngine;
    }

    public WfDataAccessor getDataAccessor() {
        return wfDataAccessor;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public WfCore getWfCore() {
        return wfCore;
    }
}
