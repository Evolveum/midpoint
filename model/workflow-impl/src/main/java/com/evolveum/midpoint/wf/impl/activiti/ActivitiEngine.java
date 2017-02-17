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

package com.evolveum.midpoint.wf.impl.activiti;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.activiti.users.MidPointUserManagerFactory;
import org.activiti.engine.*;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.history.HistoryLevel;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Manages Activiti instance (starts, stops, maintains reference to it).
 *
 * @author mederly
 */
@Component
public class ActivitiEngine {

    private static final Trace LOGGER = TraceManager.getTrace(ActivitiEngine.class);

    private ProcessEngine processEngine = null;

    @Autowired
    private WfConfiguration wfConfiguration;

    @PostConstruct
    public void init() {

        LOGGER.trace("Attempting to create Activiti engine.");

        if (!wfConfiguration.isEnabled()) {
            LOGGER.trace("Workflows are disabled, exiting.");
            return;
        }

        String schemaUpdate;
        if (wfConfiguration.isDropDatabase()) {
        	schemaUpdate = ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP;
		} else {
        	schemaUpdate = wfConfiguration.isActivitiSchemaUpdate() ?
					ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE :
					ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE;
		}

        ProcessEngineConfiguration pec =
                new MidPointStandaloneProcessEngineConfiguration()         // ugly hack (to provide our own mybatis mapping for Task)
                .setDatabaseSchemaUpdate(schemaUpdate);

        pec = ((ProcessEngineConfigurationImpl) pec)
				// ugly hack - in 5.13 they removed setCustomSessionFactories from ProcessEngineConfiguration abstract class
                .setCustomSessionFactories(Collections.singletonList(new MidPointUserManagerFactory()))
                .setJobExecutorActivate(false)
                .setHistory(HistoryLevel.FULL.getKey());

        if (wfConfiguration.getDataSource() != null) {
            pec = pec.setDataSourceJndiName(wfConfiguration.getDataSource());
        } else {
            pec = pec.setJdbcUrl(wfConfiguration.getJdbcUrl())
                    .setJdbcDriver(wfConfiguration.getJdbcDriver())
                    .setJdbcUsername(wfConfiguration.getJdbcUser())
                    .setJdbcPassword(wfConfiguration.getJdbcPassword());
        }

        pec = pec.setJobExecutorActivate(true);

        processEngine = pec.buildProcessEngine();

        LOGGER.info("Activiti engine successfully created.");

        autoDeploy();
    }

    private void autoDeploy() {

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String[] autoDeploymentFrom = wfConfiguration.getAutoDeploymentFrom();

        for (String adf : autoDeploymentFrom) {
            Resource[] resources;
            try {
                resources = resolver.getResources(adf);
            } catch (IOException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get resources to be automatically deployed from " + adf, e);
                continue;
            }

            LOGGER.info("Auto deployment from " + adf + " yields " + resources.length + " resource(s)");
            for (Resource resource : resources) {
                try {
                    autoDeployResource(resource);
                } catch (IOException | XPathExpressionException | RuntimeException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't deploy the resource " + resource, e);
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
            repositoryService.createDeployment().name(name).addInputStream(name, resource.getInputStream()).deploy();
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
