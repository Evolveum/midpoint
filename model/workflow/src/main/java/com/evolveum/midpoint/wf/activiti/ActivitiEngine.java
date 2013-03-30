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

package com.evolveum.midpoint.wf.activiti;

import com.evolveum.midpoint.common.expression.script.xpath.MidPointNamespaceContext;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.WorkflowManager;
import com.evolveum.midpoint.wf.activiti.users.MidPointUserManagerFactory;
import org.activiti.engine.*;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.repository.Deployment;
import org.apache.commons.lang.Validate;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 11.5.2012
 * Time: 22:30
 * To change this template use File | Settings | File Templates.
 */
public class ActivitiEngine {

    private static final Trace LOGGER = TraceManager.getTrace(ActivitiEngine.class);

    private static final String ADMINISTRATOR = "administrator";

    private ProcessEngine processEngine = null;
    private WfConfiguration wfConfiguration;
    private static final String BPMN_URI = "http://www.omg.org/spec/BPMN/20100524/MODEL";

    public ActivitiEngine(WorkflowManager workflowManager, WfConfiguration wfConfiguration) {

        this.wfConfiguration = wfConfiguration;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Attempting to create Activiti engine.");
        }

        List<SessionFactory> sessionFactories = new ArrayList<SessionFactory>();
        sessionFactories.add(new MidPointUserManagerFactory());

        ProcessEngineConfiguration pec =
                ((StandaloneProcessEngineConfiguration) ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration())
                .setDatabaseSchemaUpdate(wfConfiguration.isActivitiSchemaUpdate() ?
                        ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE :
                        ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setCustomSessionFactories(sessionFactories)
                .setJdbcUrl(wfConfiguration.getJdbcUrl())
                .setJdbcDriver(wfConfiguration.getJdbcDriver())
                .setJdbcUsername(wfConfiguration.getJdbcUser())
                .setJdbcPassword(wfConfiguration.getJdbcPassword())
                .setJobExecutorActivate(false)
                .setHistory(ProcessEngineConfiguration.HISTORY_FULL);

        pec = pec.setJobExecutorActivate(true);

        processEngine = pec.buildProcessEngine();

        LOGGER.info("Activiti engine successfully created.");

        autoDeploy();

//        IdentityService identityService = getIdentityService();
//
//        UserQuery uq = identityService.createUserQuery().userId(ADMINISTRATOR);
//        if (uq.count() == 0) {
//            User admin = identityService.newUser(ADMINISTRATOR);
//            identityService.saveUser(admin);
//            LOGGER.info("Created workflow user '" + ADMINISTRATOR + "'");
//
//            GroupQuery gq = identityService.createGroupQuery();
//            for (Group group : gq.list()) {
//                identityService.createMembership(ADMINISTRATOR, group.getId());
//                LOGGER.info("Created membership of user '" + ADMINISTRATOR + "' in group '" + group.getId() + "'");
//            }
//
//            LOGGER.info("Finished creating workflow user '" + ADMINISTRATOR + "' and its group membership.");
//        } else {
//            User admin = uq.singleResult();
//            LOGGER.info("User " + ADMINISTRATOR + " with ID " + admin.getId() + " exists.");
//            List<Group> groups = identityService.createGroupQuery().groupMember(admin.getId()).list();
//            for (Group g : groups) {
//                LOGGER.info(" - member of " + g.getId());
//            }
//
//        }
    }

    private void autoDeploy() {

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String adf = wfConfiguration.getAutoDeploymentFrom();

        Resource[] resources;
        try {
            resources = resolver.getResources(adf);
        } catch (IOException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get resources to be automatically deployed from " + adf, e);
            return;
        }

        LOGGER.info("Auto deployment from " + adf + " yields " + resources.length + " resource(s)");
        for (Resource resource : resources) {
            try {
                autoDeployResource(resource);
            } catch (IOException e) {
                LoggingUtils.logException(LOGGER, "Couldn't deploy the resource " + resource, e);
            } catch (XPathExpressionException e) {
                LoggingUtils.logException(LOGGER, "Couldn't deploy the resource " + resource, e);
            } catch (RuntimeException e) {
                LoggingUtils.logException(LOGGER, "Couldn't deploy the resource " + resource, e);
            }
        }

    }

    private void autoDeployResource(Resource resource) throws IOException, XPathExpressionException {

        RepositoryService repositoryService = processEngine.getRepositoryService();

        URL url = resource.getURL();
        String name = url.toString();
        long resourceLastModified = resource.lastModified();
        LOGGER.debug("Checking resource " + name + " (last modified = " + new Date(resourceLastModified) + ")");

        boolean tooOld = false;

        List<Deployment> existingList = repositoryService.createDeploymentQuery().deploymentName(name).orderByDeploymenTime().desc().listPage(1, 1);
        Deployment existing = existingList != null && !existingList.isEmpty() ? existingList.get(0) : null;
        if (existing != null) {
            if (resourceLastModified >= existing.getDeploymentTime().getTime()) {
                tooOld = true;
            }
            LOGGER.debug("Found deployment " + existing.getName() + ", last modified " + existing.getDeploymentTime() +
                    (tooOld ? " (too old)" : " (current)"));
        } else {
            LOGGER.debug("Deployment with name " + name + " was not found.");
        }

        if (existing == null || tooOld) {
            repositoryService.createDeployment().name(name).addInputStream(name, resource.getInputStream()).deploy();
            LOGGER.info("Successfully deployed Activiti resource " + name);
        }
    }

    @PreDestroy
    public void shutdown() {

        if (processEngine != null) {
            LOGGER.info("Shutting Activiti ProcessEngine down.");
            processEngine.close();
        }
    }

    public ProcessEngine getProcessEngine() {
        return processEngine;
    }

    public HistoryService getHistoryService() {
        return processEngine.getHistoryService();
    }

    public TaskService getTaskService() {
        return processEngine.getTaskService();
    }

    public RuntimeService getRuntimeService() {
        return processEngine.getRuntimeService();
    }

    public IdentityService getIdentityService() {
        return processEngine.getIdentityService();
    }

    public FormService getFormService() {
        return processEngine.getFormService();
    }
}
