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

package com.evolveum.midpoint.repo.sql.schemacheck;

import com.evolveum.midpoint.repo.sql.MetadataExtractorIntegrator;
import com.evolveum.midpoint.repo.sql.data.common.RGlobalMetadata;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.sql.ScriptRunner;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class SchemaChecker {

	private static final Trace LOGGER = TraceManager.getTrace(SchemaChecker.class);

	@Autowired private BaseHelper baseHelper;
	@Autowired private SchemaActionComputer actionComputer;

	@PostConstruct
	public void execute() {

		/*
		 * At this point the checks imposed by hbm2ddl are already done, i.e.
		 * 1) if "validate" we know that the schema has been successfully validated
		 * 2) if "update"/"create"/"create-drop" we know the schema has been created or updated
		 *
		 * Usually the skipExplicitSchemaValidation is true for such cases (it's the default value for them),
		 * so we skip further processing.
		 */
		if (baseHelper.getConfiguration().isSkipExplicitSchemaValidation()) {
			LOGGER.debug("Skipping explicit schema validation because of repo configuration setting");
			return;
		}

		SchemaState schemaState = new SchemaState(determineDataStructureCompliance(), determineDeclaredVersion());
		SchemaAction action = actionComputer.determineSchemaAction(schemaState);
		LOGGER.info("Determined schema action: " + action);     // todo debug

		if (action instanceof SchemaAction.None) {
			// nothing to do
		} else if (action instanceof SchemaAction.Warn) {
			LOGGER.warn(((SchemaAction.Warn) action).message);
		} else if (action instanceof SchemaAction.Stop) {
			SchemaAction.Stop stop = (SchemaAction.Stop) action;
			logAndShowStopBanner(stop.message);
			throw new SystemException("Database schema problem: " + stop.message.replace('\n', ';'), stop.cause);
		} else if (action instanceof SchemaAction.CreateSchema) {
			applyCreateScript(((SchemaAction.CreateSchema) action).script);
		} else {
			throw new AssertionError(action);
		}
	}

	private DataStructureCompliance determineDataStructureCompliance() {
		Metadata metadata = MetadataExtractorIntegrator.getMetadata();
		try {
			new SchemaValidator().validate(metadata);
			LOGGER.debug("DB schema is OK.");
			return new DataStructureCompliance(DataStructureCompliance.State.COMPLIANT, null);
		} catch (org.hibernate.tool.schema.spi.SchemaManagementException e) {
			LOGGER.warn("Found a problem with DB schema: {}", e.getMessage());
			LOGGER.debug("Exception", e);
			return new DataStructureCompliance(
					areSomeTablesPresent(metadata) ? DataStructureCompliance.State.NOT_COMPLIANT : DataStructureCompliance.State.NO_TABLES,
					e);
		}
	}

	private boolean areSomeTablesPresent(Metadata metadata) {
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
		return !presentTables.isEmpty();
	}

	private DeclaredVersion determineDeclaredVersion() {
		String entityName = RGlobalMetadata.class.getSimpleName();
		try (Session session = baseHelper.beginReadOnlyTransaction()) {
			//noinspection JpaQlInspection
			List<?> result = session.createQuery("select value from " + entityName + " where name = '" +
					RGlobalMetadata.DATABASE_SCHEMA_VERSION + "'").list();
			if (result.isEmpty()) {
				return new DeclaredVersion(DeclaredVersion.State.VERSION_VALUE_MISSING, null);
			} else if (result.size() == 1) {
				return new DeclaredVersion(DeclaredVersion.State.VERSION_VALUE_PRESENT, (String) result.get(0));
			} else {
				throw new IllegalStateException("More than one value of " + RGlobalMetadata.DATABASE_SCHEMA_VERSION + " present: " + result);
			}
		} catch (Throwable t) {
			LOGGER.warn("Database schema version could not be determined: {}", t.getMessage());
			LOGGER.debug("Database schema version could not be determined: {}", t.getMessage(), t);
			return new DeclaredVersion(DeclaredVersion.State.METADATA_TABLE_MISSING, null);
		}
	}

	private void applyCreateScript(String createScript) {
		createSchema(createScript);
		LOGGER.info("Validating database tables after creation.");
		try {
			new SchemaValidator().validate(MetadataExtractorIntegrator.getMetadata());
			LOGGER.info("Schema creation was successful.");
		} catch (org.hibernate.tool.schema.spi.SchemaManagementException e) {
			logAndShowStopBanner("The following problem is present even after running the create script: " + e.getMessage());
			LOGGER.error("Exception details", e);
			throw new SystemException("DB schema is not OK even after running the create script: " + e.getMessage(), e);
		}
	}

	private void createSchema(String fileName) {
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

	private void logAndShowStopBanner(String additionalMessage) {
		String message = "\n\n*******************************************************************************" +
				"\n***                                                                         ***" +
				"\n***       Couldn't start midPoint because of a database schema issue.       ***" +
				"\n***                                                                         ***" +
				"\n*******************************************************************************\n" +
				(additionalMessage != null ? "\n" + additionalMessage + "\n\n" : "");
		LOGGER.error("{}", message);
		System.err.println(message);
	}
}
