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

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.NodeErrorStatus;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.quartzimpl.execution.JobExecutor;
import com.evolveum.midpoint.task.quartzimpl.execution.TaskSynchronizer;
import com.evolveum.midpoint.task.quartzimpl.handlers.NoOpTaskHandler;
import com.evolveum.midpoint.task.quartzimpl.handlers.WaitForSubtasksTaskHandler;
import com.evolveum.midpoint.task.quartzimpl.handlers.WaitForTasksTaskHandler;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
                    defaultJdbcUrlPrefix = sqlRepositoryFactory.prepareJdbcUrlPrefix(sqlConfig);
                }
            } catch(NoSuchBeanDefinitionException e) {
                LOGGER.info("SqlRepositoryFactory is not available, JDBC Job Store configuration will be taken from taskManager section only.");
                LOGGER.trace("Reason is", e);
            } catch (RepositoryServiceFactoryException e) {
                LoggingUtils.logException(LOGGER, "Cannot determine default JDBC URL for embedded database", e);
            }

            configuration.setJdbcJobStoreInformation(midpointConfiguration, sqlConfig, defaultJdbcUrlPrefix);
            configuration.validateJdbcJobStoreInformation();
        }

        // register node
        taskManager.getClusterManager().createNodeObject(result);     // may throw initialization exception
        if (!taskManager.getConfiguration().isTestMode()) {  // in test mode do not start cluster manager thread nor verify cluster config
            taskManager.getClusterManager().checkClusterConfiguration(result);      // does not throw exceptions, sets the ERROR state if necessary, however
        }

        NoOpTaskHandler.instantiateAndRegister(taskManager);
        WaitForSubtasksTaskHandler.instantiateAndRegister(taskManager);
        WaitForTasksTaskHandler.instantiateAndRegister(taskManager);
        JobExecutor.setTaskManagerQuartzImpl(taskManager);       // unfortunately, there seems to be no clean way of letting jobs know the taskManager

        taskManager.getExecutionManager().initializeLocalScheduler();
        if (taskManager.getLocalNodeErrorStatus() != NodeErrorStatus.OK) {
            taskManager.getExecutionManager().shutdownLocalSchedulerChecked();
        }

        // populate the scheduler with jobs (if RAM-based), or synchronize with midPoint repo
        if (taskManager.getExecutionManager().synchronizeJobStores(result) == false) {
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
