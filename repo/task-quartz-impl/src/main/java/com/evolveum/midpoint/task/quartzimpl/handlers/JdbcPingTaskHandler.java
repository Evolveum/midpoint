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

package com.evolveum.midpoint.task.quartzimpl.handlers;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.sql.*;
import java.util.List;

/**
 * @author Pavol Mederly
 */

@Component
public class JdbcPingTaskHandler implements TaskHandler {

	private static final transient Trace LOGGER = TraceManager.getTrace(JdbcPingTaskHandler.class);
	public static final String HANDLER_URI = TaskConstants.JDBC_PING_HANDLER_URI;

	@Autowired
	private TaskManager taskManager;

	@Autowired(required = false)							// during some tests the repo is not available
	private SqlRepositoryFactory sqlRepositoryFactory;

	@PostConstruct
	public void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	private class Statistics {
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
	public TaskRunResult run(Task task) {
		
		long progress = task.getProgress();
		OperationResult opResult = new OperationResult(JdbcPingTaskHandler.class.getName()+".run");

		int tests = get(task, SchemaConstants.JDBC_PING_TESTS_QNAME, 0);
		int interval = get(task, SchemaConstants.JDBC_PING_INTERVAL_QNAME, 10);
		String testQuery = get(task, SchemaConstants.JDBC_PING_TEST_QUERY_QNAME, "select 1");

		SqlRepositoryConfiguration config = sqlRepositoryFactory != null ? sqlRepositoryFactory.getSqlConfiguration() : null;
		String jdbcDriver = get(task, SchemaConstants.JDBC_PING_DRIVER_CLASS_NAME_QNAME, config != null ? config.getDriverClassName() : "");
		String jdbcUrl = get(task, SchemaConstants.JDBC_PING_JDBC_URL_QNAME, config != null ? config.getJdbcUrl() : "");
		String jdbcUsername = get(task, SchemaConstants.JDBC_PING_JDBC_USERNAME_QNAME, config != null ? config.getJdbcUsername() : "");
		String jdbcPassword = get(task, SchemaConstants.JDBC_PING_JDBC_PASSWORD_QNAME, config != null ? config.getJdbcPassword() : "");
		boolean logOnInfoLevel = get(task, SchemaConstants.JDBC_PING_LOG_ON_INFO_LEVEL_QNAME, true);

        LOGGER.info("JdbcPingTaskHandler run starting; with progress = {}", progress);
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
			progress++;
			try {
				Thread.sleep(1000L * interval);
			} catch (InterruptedException e) {
				break;
			}
        }

		opResult.computeStatusIfUnknown();
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);     // would be overwritten when problem is encountered
		runResult.setProgress(progress);
		LOGGER.info("JdbcPingTaskHandler run finishing; progress = " + progress + " in task " + task.getName());
		LOGGER.info("Connection statistics: {}", connectionStatistics);
		LOGGER.info("Query statistics: {}", queryStatistics);
		return runResult;
	}

	private <T> T get(Task task, QName propertyName, T defaultValue) {
		PrismProperty<T> property = task.getExtensionProperty(propertyName);
		if (property == null) {
			return defaultValue;
		} else {
			return property.getRealValue();
		}
	}

	@Override
	public Long heartbeat(Task task) {
		return null;		// not to overwrite progress information!
	}

	@Override
	public void refreshStatus(Task task) {
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }

}
