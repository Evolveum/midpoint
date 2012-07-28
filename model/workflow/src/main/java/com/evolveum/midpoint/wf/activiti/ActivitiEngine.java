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

import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConfiguration;
import org.activiti.engine.*;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.apache.commons.lang.Validate;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 11.5.2012
 * Time: 22:30
 * To change this template use File | Settings | File Templates.
 */
@Component
public class ActivitiEngine {

    private static final Trace LOGGER = TraceManager.getTrace(TaskManager.class);

    private static final String ADMINISTRATOR = "administrator";

    private ProcessEngine processEngine = null;
    private WfConfiguration wfConfiguration;

    public void initialize(WfConfiguration configuration) throws Exception {

        Validate.notNull(configuration);

        wfConfiguration = configuration;

        LOGGER.trace("Attempting to create Activiti engine.");

        processEngine = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration()
                .setDatabaseSchemaUpdate(configuration.isActivitiSchemaUpdate() ?
                        ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE :
                        ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setJdbcUrl(configuration.getJdbcUrl())
                .setJdbcDriver(configuration.getJdbcDriver())
                .setJdbcUsername(configuration.getJdbcUser())
                .setJdbcPassword(configuration.getJdbcPassword())
                .setJobExecutorActivate(false)
                .setHistory(ProcessEngineConfiguration.HISTORY_FULL)
                .buildProcessEngine();

        LOGGER.info("Activiti engine successfully created.");

        IdentityService identityService = getIdentityService();

        UserQuery uq = identityService.createUserQuery().userId(ADMINISTRATOR);
        if (uq.count() == 0) {
            User admin = identityService.newUser(ADMINISTRATOR);
            identityService.saveUser(admin);
            LOGGER.info("Created workflow user '" + ADMINISTRATOR + "'");

            GroupQuery gq = identityService.createGroupQuery();
            for (Group group : gq.list()) {
                identityService.createMembership(ADMINISTRATOR, group.getId());
                LOGGER.info("Created membership of user '" + ADMINISTRATOR + "' in group '" + group.getId() + "'");
            }

            LOGGER.info("Finished creating workflow user '" + ADMINISTRATOR + "' and its group membership.");
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

    public IdentityService getIdentityService() {
        return processEngine.getIdentityService();
    }

    public FormService getFormService() {
        return processEngine.getFormService();
    }
}
