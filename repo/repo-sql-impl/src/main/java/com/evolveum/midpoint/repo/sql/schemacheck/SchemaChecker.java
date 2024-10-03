/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.schemacheck;

import java.io.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.sql.MetadataExtractorIntegrator;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.data.common.RGlobalMetadata;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.sql.ScriptRunner;

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
        LOGGER.debug("Determined schema action: {}", action);

        if (action instanceof SchemaAction.None) {
            // nothing to do
        } else if (action instanceof SchemaAction.Warn) {
            LOGGER.warn(((SchemaAction.Warn) action).message);
        } else if (action instanceof SchemaAction.Stop stop) {
            logAndShowStopBanner(stop.message);
            throw new SystemException("Database schema problem: " + stop.message.replace('\n', ';'), stop.cause);
        } else if (action instanceof SchemaAction.CreateSchema) {
            executeCreateAction((SchemaAction.CreateSchema) action);
        } else if (action instanceof SchemaAction.UpgradeSchema) {
            executeUpgradeAction((SchemaAction.UpgradeSchema) action);
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
            try (EntityManager em = baseHelper.beginReadOnlyTransaction()) {
                List<?> result = em.createNativeQuery("select count(*) from " + tableName).getResultList();
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
        SqlRepositoryConfiguration cfg = baseHelper.getConfiguration();
        if (cfg.getSchemaVersionOverride() != null) {
            return new DeclaredVersion(DeclaredVersion.State.VERSION_VALUE_EXTERNALLY_SUPPLIED, cfg.getSchemaVersionOverride());
        }

        String entityName = RGlobalMetadata.class.getSimpleName();
        try (EntityManager em = baseHelper.beginReadOnlyTransaction()) {
            //noinspection JpaQlInspection
            List<?> result = em.createQuery("select value from " + entityName + " where name = '" +
                    RGlobalMetadata.DATABASE_SCHEMA_VERSION + "'").getResultList();
            if (result.isEmpty()) {
                if (cfg.getSchemaVersionIfMissing() != null) {
                    return new DeclaredVersion(DeclaredVersion.State.VERSION_VALUE_EXTERNALLY_SUPPLIED, cfg.getSchemaVersionIfMissing());
                } else {
                    return new DeclaredVersion(DeclaredVersion.State.VERSION_VALUE_MISSING, null);
                }
            } else if (result.size() == 1) {
                return new DeclaredVersion(DeclaredVersion.State.VERSION_VALUE_PRESENT, (String) result.get(0));
            } else {
                throw new IllegalStateException("More than one value of " + RGlobalMetadata.DATABASE_SCHEMA_VERSION + " present: " + result);
            }
        } catch (Throwable t) {
            LOGGER.warn("Database schema version could not be determined: {}", t.getMessage());
            LOGGER.debug("Database schema version could not be determined: {}", t.getMessage(), t);
            if (cfg.getSchemaVersionIfMissing() != null) {
                return new DeclaredVersion(DeclaredVersion.State.VERSION_VALUE_EXTERNALLY_SUPPLIED, cfg.getSchemaVersionIfMissing());
            } else {
                return new DeclaredVersion(DeclaredVersion.State.METADATA_TABLE_MISSING, null);
            }
        }
    }

    private void executeCreateAction(SchemaAction.CreateSchema action) {
        LOGGER.info("Attempting to create database tables from file '{}'.", action.script);
        applyScript(action.script);
        LOGGER.info("Validating database tables after creation.");
        validateAfterScript("create");
        LOGGER.info("Schema creation was successful.");
    }

    private void executeUpgradeAction(SchemaAction.UpgradeSchema action) {
        LOGGER.info("Attempting to upgrade database tables using file '{}'.", action.script);
        applyScript(action.script);
        LOGGER.info("Validating database tables after upgrading.");
        validateAfterScript("upgrade");
        LOGGER.info("\n\n"
                + "***********************************************************************\n"
                + "***                                                                 ***\n"
                + "***            Database schema upgrade was successful               ***\n"
                + "***                                                                 ***\n"
                + "***********************************************************************\n\n"
                + "Schema was successfully upgraded from {} to {} using script '{}'.\n"
                + "Please verify everything works as expected.\n\n", action.from, action.to, action.script);
    }

    private void validateAfterScript(String type) {
        try {
            new SchemaValidator().validate(MetadataExtractorIntegrator.getMetadata());
        } catch (org.hibernate.tool.schema.spi.SchemaManagementException e) {
            logAndShowStopBanner("The following problem is present even after running the " + type + " script: " + e.getMessage());
            LOGGER.error("Exception details", e);
            throw new SystemException("DB schema is not OK even after running the " + type + " script: " + e.getMessage(), e);
        }
    }

    private void applyScript(String fileName) {
        String filePath = "/sql/" + fileName;
        InputStream stream = getClass().getResourceAsStream(filePath);
        if (stream == null) {
            throw new SystemException("DB script (" + filePath + ") couldn't be found");
        }
        Reader reader = new BufferedReader(new InputStreamReader(stream));

        try (EntityManager em = baseHelper.getEntityManagerFactory().createEntityManager()) {
            Session session = em.unwrap(Session.class);
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
                        throw new SystemException("Couldn't execute DB script " + filePath + ": " + e.getMessage(), e);
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
