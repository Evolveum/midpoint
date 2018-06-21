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

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.quartzimpl.execution.JobExecutor;
import com.evolveum.midpoint.task.quartzimpl.execution.JobStarter;
import com.evolveum.midpoint.task.quartzimpl.handlers.NoOpTaskHandler;
import com.evolveum.midpoint.task.quartzimpl.handlers.WaitForSubtasksByPollingTaskHandler;
import com.evolveum.midpoint.task.quartzimpl.handlers.WaitForTasksTaskHandler;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeErrorStatusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * @author Pavol Mederly
 */

public class Initializer {

    private static final transient Trace LOGGER = TraceManager.getTrace(Initializer.class);

    private TaskManagerQuartzImpl taskManager;

    Initializer(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

    public void init(OperationResult result) throws TaskManagerInitializationException {

        MidpointConfiguration midpointConfiguration = taskManager.getMidpointConfiguration();

        LOGGER.info("Task Manager initialization.");

        // get the configuration (general section + JDBC section as well)
        TaskManagerConfiguration configuration = taskManager.getConfiguration();
        configuration.checkAllowedKeys(midpointConfiguration);
        configuration.setBasicInformation(midpointConfiguration);
        configuration.validateBasicInformation();

        LOGGER.info("Task Manager: Quartz Job Store: "
                + (configuration.isJdbcJobStore() ? "JDBC":"in-memory") + ", "
                + (configuration.isClustered() ? "":"NOT ") + "clustered. Threads: " + configuration.getThreads());

        if (configuration.isJdbcJobStore()) {

            // quartz properties related to database connection will be taken from SQL repository
            String defaultJdbcUrlPrefix = null;
            SqlRepositoryConfiguration sqlConfig = null;
            try {
                SqlRepositoryFactory sqlRepositoryFactory = (SqlRepositoryFactory) taskManager.getBeanFactory().getBean("sqlRepositoryFactory");
                sqlConfig = sqlRepositoryFactory.getSqlConfiguration();
                if (sqlConfig.isEmbedded()) {
                    defaultJdbcUrlPrefix = sqlConfig.getDefaultEmbeddedJdbcUrlPrefix();
                }
            } catch (NoSuchBeanDefinitionException e) {
                LOGGER.info("SqlRepositoryFactory is not available, JDBC Job Store configuration will be taken from taskManager section only.");
                LOGGER.trace("Reason is", e);
            }

            configuration.setJdbcJobStoreInformation(midpointConfiguration, sqlConfig, defaultJdbcUrlPrefix);
            configuration.validateJdbcJobStoreInformation();
        }

        // register node
        NodeType node = taskManager.getClusterManager().createOrUpdateNodeInRepo(result);     // may throw initialization exception
        if (!taskManager.getConfiguration().isTestMode()) {  // in test mode do not start cluster manager thread nor verify cluster config
            taskManager.getClusterManager().checkClusterConfiguration(result);      // Does not throw exceptions. Sets the ERROR state if necessary, however.
        }

        NoOpTaskHandler.instantiateAndRegister(taskManager);
        WaitForSubtasksByPollingTaskHandler.instantiateAndRegister(taskManager);
        WaitForTasksTaskHandler.instantiateAndRegister(taskManager);
        JobExecutor.setTaskManagerQuartzImpl(taskManager);       // unfortunately, there seems to be no clean way of letting jobs know the taskManager
        JobStarter.setTaskManagerQuartzImpl(taskManager);        // the same here

        taskManager.getExecutionManager().initializeLocalScheduler();
        if (taskManager.getLocalNodeErrorStatus() == NodeErrorStatusType.OK) {
            taskManager.getExecutionManager().setLocalExecutionLimitations(node);
        } else {
            taskManager.getExecutionManager().shutdownLocalSchedulerChecked();
        }

        // populate the scheduler with jobs (if RAM-based), or synchronize with midPoint repo
        if (!taskManager.getExecutionManager().synchronizeJobStores(result)) {
            if (!configuration.isJdbcJobStore()) {
                LOGGER.error("Some or all tasks could not be imported from midPoint repository to Quartz job store. They will therefore not be executed.");
            } else {
                LOGGER.warn("Some or all tasks could not be synchronized between midPoint repository and Quartz job store. They may not function correctly.");
            }
        }

        LOGGER.trace("Quartz scheduler initialized (not yet started, however)");
        LOGGER.info("Task Manager initialized");
    }
}
