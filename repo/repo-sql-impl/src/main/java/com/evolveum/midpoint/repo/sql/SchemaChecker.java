/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.repo.sql.data.common.RGlobalMetadata;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.sql.ScriptRunner;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * @author mederly
 */
@Component
public class SchemaChecker {

	private static final Trace LOGGER = TraceManager.getTrace(SchemaChecker.class);
	private static final String RELEASE_NOTES_URL_PREFIX = "https://wiki.evolveum.com/display/midPoint/Release+";

	@Autowired private BaseHelper baseHelper;
	@Autowired private LocalizationService localizationService;

	@PostConstruct
	public void execute() {

		SqlRepositoryConfiguration.MissingSchemaAction missingSchemaAction = baseHelper.getConfiguration().getMissingSchemaAction();
		LOGGER.debug("missingSchemaAction = {}", missingSchemaAction);
		if (missingSchemaAction == SqlRepositoryConfiguration.MissingSchemaAction.NONE) {
			checkDeclaredSchemaVersion();       // schema might or might not be missing; but let us check declared version before continuing
			return;
		}

		Metadata metadata = MetadataExtractorIntegrator.getMetadata();

		Exception exception;
		try {
			new SchemaValidator().validate(metadata);
			LOGGER.debug("DB schema is OK.");
			// schema seems to be OK; however, let us check the declared version to be sure
			checkDeclaredSchemaVersion();
			return;
		} catch (org.hibernate.tool.schema.spi.SchemaManagementException e) {
			exception = e;
			LOGGER.warn("Found a problem with DB schema: {}", e.getMessage());
			LOGGER.debug("Exception", e);
		}

		checkSchemaPartiallyPresent(metadata, exception);

		// now we know that the schema is missing; so OutdatedSchemaAction is not relevant anymore

		if (missingSchemaAction == SqlRepositoryConfiguration.MissingSchemaAction.STOP) {
			String message = "Stopping because midPoint database tables are not present [" + exception.getMessage() +
					"] and missingSchemaAction = STOP.";
			bigWindow(message);
			throw new SystemException(message, exception);
		}

		if (missingSchemaAction == SqlRepositoryConfiguration.MissingSchemaAction.CREATE) {

			createSchema();

			LOGGER.info("Validating database tables after creation.");
			try {
				new SchemaValidator().validate(metadata);
			} catch (org.hibernate.tool.schema.spi.SchemaManagementException e) {
				bigWindow(null);
				LOGGER.error("The following problem is present even after running the create script: {}", e.getMessage(),
						exception);
				throw new SystemException("DB schema is not OK even after running the create script: " + e.getMessage(), e);
			}
			LOGGER.info("Schema creation was successful.");

		} else {

			String message = "Stopping because midPoint database tables are not present [" + exception.getMessage() +
					"] and missingSchemaAction has an unsupported value of '" + missingSchemaAction + "'";
			bigWindow(message);
			throw new SystemException(message, exception);
		}
	}

	private void checkDeclaredSchemaVersion() {
		SqlRepositoryConfiguration.OutdatedSchemaAction action = baseHelper.getConfiguration().getOutdatedSchemaAction();

		String versionPresent = getDeclaredSchemaVersion();
		String versionRequired = getMidPointMajorVersion();
		String upgradeUrl = RELEASE_NOTES_URL_PREFIX + versionRequired;
		LOGGER.debug("Database schema as present = {}, database schema required = {}", versionPresent, versionRequired);
		if (versionPresent == null) {
			// todo differentiate between "RGlobalMetadata missing" and "no schema information in RGlobalMetadata"
			schemaVersionMessage("Existing database schema version could not be determined, meaning it is probably 3.8 or older. The system requires version "
					+ versionRequired + ".", upgradeUrl, action);
		} else if (!versionPresent.equals(versionRequired)) {
			schemaVersionMessage("Existing database schema version (" + versionPresent + ") is different from the "
					+ "version required by the system (" + versionRequired + ").", upgradeUrl, action);
		}
	}

	private void schemaVersionMessage(String message, String upgradeUrl, @NotNull SqlRepositoryConfiguration.OutdatedSchemaAction action) {
		switch (action) {
			case UPGRADE:       // not supported now
			case STOP:
				String actionToDo = "Please carry out appropriate upgrade procedure as described in " + upgradeUrl + ", if applicable.";
				bigWindow(message + "\n" + actionToDo);
				throw new SystemException(message + " " + actionToDo);
			case WARN:
				LOGGER.warn("{}", message);
				LOGGER.warn("Continuing, because " + SqlRepositoryConfiguration.PROPERTY_OUTDATED_SCHEMA_ACTION + " is set to '"
						+ action.getValue() + "'");
				LOGGER.warn("This could cause unpredictable behavior. Please fix this as soon as possible.");
				return;
			case NONE:
				LOGGER.debug("{}", message);
				return;
			default:
				throw new AssertionError(action);
		}
	}

	private String getDeclaredSchemaVersion() {
		String entityName = RGlobalMetadata.class.getSimpleName();
		try (Session session = baseHelper.beginReadOnlyTransaction()) {
			List<?> result = session.createQuery("select value from " + entityName + " where name = '" +
					RGlobalMetadata.DATABASE_SCHEMA_VERSION + "'").list();
			String version;
			if (result.isEmpty()) {
				version = null;
			} else if (result.size() == 1) {
				version = (String) result.get(0);
			} else {
				throw new IllegalStateException("More than one value of " + RGlobalMetadata.DATABASE_SCHEMA_VERSION + " present: " + result);
			}
			LOGGER.debug("Database schema version is determined to be {}", version);
			return version;
		} catch (Throwable t) {
			LOGGER.debug("Database schema version could not be determined: {}", t.getMessage(), t);
			return null;
		}
	}

	private void createSchema() {
		SqlRepositoryConfiguration.Database database = baseHelper.getConfiguration().getDatabase();
		if (database == null) {
			throw new SystemException("Couldn't create DB schema because database kind is not known");
		}
		if (database == SqlRepositoryConfiguration.Database.MARIADB) {
			database = SqlRepositoryConfiguration.Database.MYSQL;
		}
		String version = getMidPointMajorVersion();
		String fileName = database.name().toLowerCase() + "-" + version + "-all.sql";
		LOGGER.info("Attempting to create database tables from file '{}'.", fileName);

		String filePath = "/sql/" + fileName;
		InputStream stream = getClass().getResourceAsStream(filePath);
		if (stream == null) {
			throw new SystemException("DB schema (" + filePath + ") couldn't be found");
		}
		Reader reader = new BufferedReader(new InputStreamReader(stream));

		try (Session session = baseHelper.getSessionFactory().openSession()) {
			session.doWork(connection -> {
				int transactionIsolation = connection.getTransactionIsolation();
				if (baseHelper.getConfiguration().isUsingSQLServer()) {
					int newTxIsolation = Connection.TRANSACTION_READ_COMMITTED;
					LOGGER.info("Setting transaction isolation to {}", newTxIsolation);
					connection.setTransactionIsolation(newTxIsolation);
				}
				try {
					ScriptRunner scriptRunner = new ScriptRunner(connection, false, false);
					try {
						scriptRunner.runScript(reader);
					} catch (IOException e) {
						throw new SystemException("Couldn't execute DB creation script " + filePath + ": " + e.getMessage(), e);
					}
				} finally {
					if (connection.getTransactionIsolation() != transactionIsolation) {
						LOGGER.info("Resetting transaction isolation back to {}", transactionIsolation);
						connection.setTransactionIsolation(transactionIsolation);
					}
				}
			});
		}
	}

	// TODO move to better place
	@NotNull
	private String getMidPointMajorVersion() {
		String version = localizationService
				.translate(LocalizableMessageBuilder.buildKey("midPointVersion"), Locale.getDefault());
		String noSnapshot = StringUtils.removeEnd(version, "-SNAPSHOT");
		int firstDot = noSnapshot.indexOf('.');
		if (firstDot < 0) {
			throw new SystemException("Couldn't determine midPoint version from '" + version + "'");
		}
		int secondDot = noSnapshot.indexOf('.', firstDot+1);
		if (secondDot < 0) {
			return noSnapshot;
		} else {
			return noSnapshot.substring(0, secondDot);
		}
	}

	private void checkSchemaPartiallyPresent(Metadata metadata, Exception exception) {
		Collection<String> presentTables = new ArrayList<>();
		Collection<String> missingTables = new ArrayList<>();
		for (Table table : metadata.collectTableMappings()) {
			String tableName = table.getName();
			try (Session session = baseHelper.beginReadOnlyTransaction()) {
				List<?> result = session.createNativeQuery("select count(*) from " + tableName).list();
				LOGGER.debug("Table {} seems to be present; number of records is {}", tableName, result);
				presentTables.add(tableName);
			} catch (Throwable t) {
				LOGGER.debug("Table {} seems to be missing: {}", tableName, t.getMessage(), t);
				missingTables.add(tableName);
			}
		}
		LOGGER.info("The following midPoint tables are present (not necessarily well-defined): {}", presentTables);
		LOGGER.info("Couldn't find the following midPoint tables: {}", missingTables);
		if (!presentTables.isEmpty()) {
			checkDeclaredSchemaVersion();       // maybe the problem is that the schema is obsolete
			bigWindow("The schema is partially present but not complete.");
			LOGGER.error("Exception reported: {}", exception.getMessage(), exception);
			throw new SystemException("Database schema is partially present but not complete: " + exception.getMessage(), exception);
		}
	}

	private void bigWindow(String additionalMessage) {
		LOGGER.error(
				"\n*******************************************************************************" +
				"\n***                                                                         ***" +
				"\n***       Couldn't start midPoint because of a database schema issue.       ***" +
				"\n***                                                                         ***" +
				"\n*******************************************************************************\n" +
						(additionalMessage != null ? "\n" + additionalMessage + "\n\n" : ""));
	}

}
