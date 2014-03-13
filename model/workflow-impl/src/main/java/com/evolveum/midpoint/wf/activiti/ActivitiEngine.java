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

package com.evolveum.midpoint.wf.activiti;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.activiti.users.MidPointUserManagerFactory;
import org.activiti.engine.*;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Manages Activiti instance (starts, stops, maintains reference to it).
 *
 * @author mederly
 */
@Component
public class ActivitiEngine {

    private static final Trace LOGGER = TraceManager.getTrace(ActivitiEngine.class);

    private static final String ADMINISTRATOR = "administrator";

    private ProcessEngine processEngine = null;
    private static final String BPMN_URI = "http://www.omg.org/spec/BPMN/20100524/MODEL";

    @Autowired
    private WfConfiguration wfConfiguration;

    @PostConstruct
    public void init() {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Attempting to create Activiti engine.");
        }

        if (wfConfiguration == null || wfConfiguration.isEnabled() == null) {
            throw new IllegalStateException("WfConfiguration is not initialized, despite of ActivitiEngine depending on it - check Spring dependencies.");
        }

        if (!wfConfiguration.isEnabled()) {
            LOGGER.trace("Workflows are disabled, exiting.");
            return;
        }


        List<SessionFactory> sessionFactories = new ArrayList<SessionFactory>();
        sessionFactories.add(new MidPointUserManagerFactory());

        ProcessEngineConfiguration pec =
                ((StandaloneProcessEngineConfiguration) ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration())
                .setDatabaseSchemaUpdate(wfConfiguration.isActivitiSchemaUpdate() ?
                        ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE :
                        ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);

        pec = ((ProcessEngineConfigurationImpl) pec)
                .setCustomSessionFactories(sessionFactories)            // ugly hack - in 5.13 they removed setCustomSessionFactories from ProcessEngineConfiguration abstract class
                .setJdbcUrl(wfConfiguration.getJdbcUrl())
                .setJdbcDriver(wfConfiguration.getJdbcDriver())
                .setJdbcUsername(wfConfiguration.getJdbcUser())
                .setJdbcPassword(wfConfiguration.getJdbcPassword())
                .setJobExecutorActivate(false)
                .setHistory(HistoryLevel.FULL.getKey());

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
        String[] autoDeploymentFrom = wfConfiguration.getAutoDeploymentFrom();

        for (String adf : autoDeploymentFrom) {
            Resource[] resources;
            try {
                resources = resolver.getResources(adf);
            } catch (IOException e) {
                LoggingUtils.logException(LOGGER, "Couldn't get resources to be automatically deployed from " + adf, e);
                continue;
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
            Deployment deployment = repositoryService.createDeployment().name(name).addInputStream(name, resource.getInputStream()).deploy();
            LOGGER.info("Successfully deployed Activiti resource " + name); // + " as deployment with id = " + deployment.getId() + ", name = " + deployment.getName());
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
