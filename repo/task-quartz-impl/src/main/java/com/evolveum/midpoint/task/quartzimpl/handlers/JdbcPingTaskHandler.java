/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.handlers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.schema.result.OperationResultStatus;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@Component
public class JdbcPingTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(JdbcPingTaskHandler.class);
    public static final String HANDLER_URI = TaskConstants.JDBC_PING_HANDLER_URI;

    @Autowired
    private TaskManager taskManager;

    @Autowired(required = false) // during some tests the repo is not available
    private JdbcRepositoryConfiguration jdbcConfig;

    @PostConstruct
    public void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    private static class Statistics {
        Integer min = null;
        Integer max = null;
        int total = 0;
        int okCount = 0;
        int failCount = 0;

        void record(int time) {
            total += time;
            okCount++;
            if (min == null || time < min) {
                min = time;
            }
            if (max == null || time > max) {
                max = time;
            }
        }

        void recordFailure() {
            failCount++;
        }

        @Override
        public String toString() {
            float avg = okCount > 0 ? (float) total / (float) okCount : 0;
            return "OK: " + okCount + ", avg: " + String.format("%.2f", avg) + " ms, min: " + min + " ms, max: " + max + " ms, failures: " + failCount;
        }
    }

    @Override
    public TaskRunResult run(@NotNull RunningTask task) {
        OperationResult opResult = task.getResult().createSubresult(JdbcPingTaskHandler.class.getName() + ".run");
        try {
            return runInternal(task, opResult);
        } catch (Throwable t) {
            opResult.recordFatalError(t);
            throw t;
        } finally {
            opResult.close();
        }
    }

    private @NotNull TaskRunResult runInternal(@NotNull RunningTask task, OperationResult opResult) {
        int tests = get(task, SchemaConstants.JDBC_PING_TESTS_QNAME, 0);
        int interval = get(task, SchemaConstants.JDBC_PING_INTERVAL_QNAME, 10);
        String testQuery = get(task, SchemaConstants.JDBC_PING_TEST_QUERY_QNAME, "select 1");

        String jdbcDriver = get(task, SchemaConstants.JDBC_PING_DRIVER_CLASS_NAME_QNAME,
                jdbcConfig != null ? jdbcConfig.getDriverClassName() : "");
        String jdbcUrl = get(task, SchemaConstants.JDBC_PING_JDBC_URL_QNAME,
                jdbcConfig != null ? jdbcConfig.getJdbcUrl("mp-ping") : "");
        String jdbcUsername = get(task, SchemaConstants.JDBC_PING_JDBC_USERNAME_QNAME,
                jdbcConfig != null ? jdbcConfig.getJdbcUsername() : "");
        String jdbcPassword = get(task, SchemaConstants.JDBC_PING_JDBC_PASSWORD_QNAME,
                jdbcConfig != null ? jdbcConfig.getJdbcPassword() : "");
        boolean logOnInfoLevel = get(task, SchemaConstants.JDBC_PING_LOG_ON_INFO_LEVEL_QNAME, true);

        LOGGER.info("JdbcPingTaskHandler run starting; with progress = {}", task.getLegacyProgress());
        LOGGER.info("Tests to be executed: {}", tests > 0 ? tests : "(unlimited)");
        LOGGER.info("Interval between tests: {} seconds", interval);
        LOGGER.info("SQL query to be used: {}", testQuery);
        LOGGER.info("JDBC:");
        LOGGER.info(" - driver: {}", jdbcDriver);
        LOGGER.info(" - URL: {}", jdbcUrl);
        LOGGER.info(" - username: {}", jdbcUsername);
        LOGGER.info("Log on info level: {}", logOnInfoLevel);

        Statistics connectionStatistics = new Statistics();
        Statistics queryStatistics = new Statistics();

        for (int i = 0; task.canRun() && (tests == 0 || i < tests); i++) {
            Connection connection = null;
            try {
                Class.forName(jdbcDriver);
                long connStart = System.currentTimeMillis();
                connection = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
                long connTime = System.currentTimeMillis() - connStart;
                connectionStatistics.record((int) connTime);
                if (logOnInfoLevel) {
                    LOGGER.info("Successfully connected to database in {} milliseconds", connTime);
                } else {
                    LOGGER.debug("Successfully connected to database in {} milliseconds", connTime);
                }
                try {
                    long queryStart = System.currentTimeMillis();
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(testQuery);
                    int rowCount = 0;
                    while (rs.next()) {
                        rowCount++;
                    }
                    long queryTime = System.currentTimeMillis() - queryStart;
                    queryStatistics.record((int) queryTime);
                    if (logOnInfoLevel) {
                        LOGGER.info("Test query executed successfully in {} milliseconds, returned {} rows", queryTime, rowCount);
                    } else {
                        LOGGER.debug("Test query executed successfully in {} milliseconds, returned {} rows", queryTime, rowCount);
                    }
                } catch (Throwable t) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute test query '" + testQuery + "'", t);
                    queryStatistics.recordFailure();
                }
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't connect to '" + jdbcUrl + "'", t);
                connectionStatistics.recordFailure();
            }
            if (logOnInfoLevel) {
                LOGGER.info("Connection statistics: {}", connectionStatistics);
                LOGGER.info("Query statistics: {}", queryStatistics);
            } else {
                LOGGER.debug("Connection statistics: {}", connectionStatistics);
                LOGGER.debug("Query statistics: {}", queryStatistics);
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Throwable t) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't close DB connection", t);
                }
            }
            try {
                task.incrementLegacyProgressAndStoreStatisticsIfTimePassed(opResult);
            } catch (SchemaException | ObjectNotFoundException e) {
                throw new SystemException(e);
            }
            try {
                //noinspection BusyWait
                Thread.sleep(1000L * interval);
            } catch (InterruptedException e) {
                break;
            }
        }

        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        runResult.setOperationResultStatus(OperationResultStatus.SUCCESS);
        LOGGER.info("JdbcPingTaskHandler run finishing; progress = " + task.getLegacyProgress() + " in task " + task.getName());
        LOGGER.info("Connection statistics: {}", connectionStatistics);
        LOGGER.info("Query statistics: {}", queryStatistics);
        return runResult;
    }

    private <T> T get(Task task, ItemName propertyName, T defaultValue) {
        PrismProperty<T> property = task.getExtensionPropertyOrClone(propertyName);
        if (property == null) {
            return defaultValue;
        } else {
            return property.getRealValue();
        }
    }

    @Override
    public Long heartbeat(Task task) {
        return null; // not to overwrite progress information!
    }

    @Override
    public void refreshStatus(Task task) {
    }

}
