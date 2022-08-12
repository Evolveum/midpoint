/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.schemacheck;

import java.util.Arrays;
import java.util.HashSet;
import java.util.PrimitiveIterator;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.MissingSchemaAction;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.UpgradeableSchemaAction;
import com.evolveum.midpoint.repo.sql.data.common.RGlobalMetadata;
import com.evolveum.midpoint.repo.sqlbase.SupportedDatabase;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Determines the action that should be done against the database (none, stop, warn, create, upgrade)
 *
 * Takes the following input:
 * - information about the database schema state (tables existence/non-existence, declared schema version)
 * - repository configuration (namely actions for missing/upgradeable/incompatible schemas)
 */
@Component
class SchemaActionComputer {

    /**
     * For database schema versioning please see
     * <a href="https://docs.evolveum.com/midpoint/reference/repository/database-schema-versioning/">wiki page about DB versioning</a>.
     */
    public static final String REQUIRED_DATABASE_SCHEMA_VERSION = "4.6";

    private static final Trace LOGGER = TraceManager.getTrace(SchemaActionComputer.class);

    private static final String RELEASE_NOTES_URL_PREFIX = "https://docs.evolveum.com/midpoint/release/";
    private static final String SQL_SCHEMA_SCRIPTS_URL = "https://docs.evolveum.com/midpoint/reference/repository/sql-schema-scripts/";

    @Autowired private SqlRepositoryConfiguration repositoryConfiguration;

    // TODO we don't have scripts for the entries here anyway, we just need PREVIOUS_DATABASE_SCHEMA_VERSION
    //  or UPGRADABLE_FROM_DATABASE_VERSION or something like that + REQUIRED (CURRENT?) version above.
    private static final Set<Pair<String, String>> AUTOMATICALLY_UPGRADEABLE = new HashSet<>(
            Arrays.asList(
                    new ImmutablePair<>("3.8", "3.9"),
                    new ImmutablePair<>("3.9", "4.0"),
                    new ImmutablePair<>("4.0", "4.2"),
                    new ImmutablePair<>("4.2", "4.3"),
                    new ImmutablePair<>("4.3", "4.4"),
                    new ImmutablePair<>("4.4", "4.5"),
                    new ImmutablePair<>("4.5", "4.6")));

    enum State {
        COMPATIBLE, NO_TABLES, AUTOMATICALLY_UPGRADEABLE, MANUALLY_UPGRADEABLE, INCOMPATIBLE
    }

    @NotNull
    SchemaAction determineSchemaAction(SchemaState schemaState) {
        LOGGER.debug("Determining action for state: {}", schemaState);
        State state = determineState(schemaState);
        switch (state) {
            case COMPATIBLE:
                return new SchemaAction.None();
            case NO_TABLES:
                return determineActionForNoTables(schemaState);
            case AUTOMATICALLY_UPGRADEABLE:
                return determineActionForAutomaticallyUpgradeableSchema(schemaState);
            case MANUALLY_UPGRADEABLE:
                return determineActionForManuallyUpgradeableSchema(schemaState);
            case INCOMPATIBLE:
                return determineActionForIncompatibleSchema(schemaState);
            default:
                throw new AssertionError(state);
        }
    }

    @NotNull
    private SchemaAction determineActionForNoTables(SchemaState schemaState) {
        MissingSchemaAction action = getMissingSchemaAction();
        switch (action) {
            case CREATE:
                return new SchemaAction.CreateSchema(determineCreateScriptFileName());
            case WARN:
                return new SchemaAction.Warn("Database schema is missing or inaccessible. Please resolve the situation immediately.");
            case STOP:
                return new SchemaAction.Stop("Database schema is missing or inaccessible. Please resolve the situation immediately.\n"
                        + "You can either run the SQL scripts manually (see " + SQL_SCHEMA_SCRIPTS_URL + ")\nor you can set the '"
                        + SqlRepositoryConfiguration.PROPERTY_MISSING_SCHEMA_ACTION + "' configuration property to '"
                        + MissingSchemaAction.CREATE.getValue() + "' and midPoint will do that for you.",
                        schemaState.dataStructureCompliance.validationException);
            default:
                throw new AssertionError(action);
        }
    }

    @NotNull
    private SchemaAction determineActionForManuallyUpgradeableSchema(SchemaState state) {
        String message = "Database schema is not compatible with the executing code; however, an upgrade path is available.\n"
                + getCurrentAndRequiredVersionInformation(state)
                + "For more information about the upgrade process please see "
                + RELEASE_NOTES_URL_PREFIX + REQUIRED_DATABASE_SCHEMA_VERSION;
        UpgradeableSchemaAction action = getUpgradeableSchemaAction();
        switch (action) {
            case WARN:
                return new SchemaAction.Warn(message);
            case STOP:
            case UPGRADE:
                return new SchemaAction.Stop(message, state.dataStructureCompliance.validationException);
            default:
                throw new AssertionError(action);
        }
    }

    @NotNull
    private SchemaAction determineActionForAutomaticallyUpgradeableSchema(SchemaState state) {
        UpgradeableSchemaAction action = getUpgradeableSchemaAction();
        if (action == UpgradeableSchemaAction.UPGRADE) {
            String from = state.declaredVersion.version;
            String to = REQUIRED_DATABASE_SCHEMA_VERSION;
            return new SchemaAction.UpgradeSchema(determineUpgradeScriptFileName(from, to), from, to);
        }
        String message = "Database schema is not compatible with the executing code; however, an upgrade path is available.\n"
                + getCurrentAndRequiredVersionInformation(state)
                + "For more information about the upgrade process please see "
                + RELEASE_NOTES_URL_PREFIX + REQUIRED_DATABASE_SCHEMA_VERSION + ".\n\n"
                + "You can even request automatic upgrade by setting '" + SqlRepositoryConfiguration.PROPERTY_UPGRADEABLE_SCHEMA_ACTION + "' "
                + "property to '" + UpgradeableSchemaAction.UPGRADE.getValue() + "'.";
        //noinspection Duplicates
        switch (action) {
            case WARN:
                return new SchemaAction.Warn(message);
            case STOP:
                return new SchemaAction.Stop(message, state.dataStructureCompliance.validationException);
            default:
                throw new AssertionError(action);
        }
    }

    @NotNull
    private SchemaAction determineActionForIncompatibleSchema(SchemaState state) {
        String message =
                "Database schema is not compatible with the executing code.\n"
                        + getCurrentAndRequiredVersionInformation(state)
                        + "Please resolve this situation immediately.";
        if (REQUIRED_DATABASE_SCHEMA_VERSION.equals(state.declaredVersion.version) &&
                state.dataStructureCompliance.state == DataStructureCompliance.State.NOT_COMPLIANT) {
            message += "\n\nAlthough the declared schema version matches the required version, the validation of the schema "
                    + "did not pass. This may indicate corrupted or inaccessible (parts of) the database schema.";
            if (state.dataStructureCompliance.validationException != null) {
                message += "\nValidation result: " + state.dataStructureCompliance.validationException.getMessage();
            }
        }
        SqlRepositoryConfiguration.IncompatibleSchemaAction action = getIncompatibleSchemaAction();
        //noinspection Duplicates
        switch (action) {
            case WARN:
                return new SchemaAction.Warn(message);
            case STOP:
                return new SchemaAction.Stop(message, state.dataStructureCompliance.validationException);
            default:
                throw new AssertionError(action);
        }
    }

    @NotNull
    private String getCurrentAndRequiredVersionInformation(SchemaState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        if (state.declaredVersion.version != null) {
            sb.append("Current version is: ").append(state.declaredVersion.version);
        } else {
            sb.append("Current version cannot be determined.");
            switch (state.declaredVersion.state) {
                case METADATA_TABLE_MISSING:
                    sb.append(" The metadata table (" + RGlobalMetadata.TABLE_NAME + ") is missing or inaccessible. This indicates the version is below 3.9.");
                    break;
                case VERSION_VALUE_MISSING:
                    sb.append(" The metadata table (" + RGlobalMetadata.TABLE_NAME + ") exists but it does not contain version information.");
                    break;
            }
        }
        sb.append("\nRequired version is: ").append(REQUIRED_DATABASE_SCHEMA_VERSION).append("\n\n");
        return sb.toString();
    }

    private State determineState(SchemaState schemaState) {
        @NotNull String requiredVersion = REQUIRED_DATABASE_SCHEMA_VERSION;
        DataStructureCompliance.State dataComplianceState = schemaState.dataStructureCompliance.state;
        Exception dataComplianceException = schemaState.dataStructureCompliance.validationException;
        DeclaredVersion.State declaredVersionState = schemaState.declaredVersion.state;
        String declaredVersionNumber = schemaState.declaredVersion.version;
        LOGGER.info("Required database schema version: {}, declared version: {} ({}), data structure compliance: {}{}",
                requiredVersion, declaredVersionNumber, declaredVersionState, dataComplianceState,
                dataComplianceException != null ? " (" + dataComplianceException.getMessage() + ")" : "");
        if (requiredVersion.equals(schemaState.declaredVersion.version)) {
            switch (dataComplianceState) {
                case COMPLIANT:
                    return State.COMPATIBLE;
                case NO_TABLES:
                    throw new AssertionError("No tables but schema version declared?");
                case NOT_COMPLIANT:
                    LOGGER.warn("Strange: Declared version matches but the table structure is not compliant. Please investigate this.");
                    return State.INCOMPATIBLE;
                default:
                    throw new AssertionError(dataComplianceState);
            }
        } else {
            switch (dataComplianceState) {
                case NO_TABLES:
                    return State.NO_TABLES;
                case COMPLIANT:
                    if (declaredVersionState == DeclaredVersion.State.METADATA_TABLE_MISSING) {
                        LOGGER.warn("Strange: Data structure is compliant but metadata table is missing or inaccessible. Please investigate this.");
                        return State.INCOMPATIBLE;
                    } else if (declaredVersionState == DeclaredVersion.State.VERSION_VALUE_MISSING) {
                        LOGGER.warn("Data structure is compliant but version information is missing from the global metadata table. Please investigate and fix this.");
                        return State.COMPATIBLE;    // let's continue
                    }
                    return determineUpgradeability(schemaState.declaredVersion, requiredVersion);
                case NOT_COMPLIANT:
                    if (declaredVersionState == DeclaredVersion.State.METADATA_TABLE_MISSING) {
                        return State.MANUALLY_UPGRADEABLE;           // this is currently true (any version can be upgraded to 3.9)
                    } else if (declaredVersionState == DeclaredVersion.State.VERSION_VALUE_MISSING) {
                        return State.INCOMPATIBLE;              // something strange happened; this does not seem to be an upgrade situation
                    }
                    return determineUpgradeability(schemaState.declaredVersion, requiredVersion);
                default:
                    throw new AssertionError(dataComplianceState);
            }
        }
    }

    private State determineUpgradeability(DeclaredVersion declaredVersionInfo, String requiredVersion) {
        String declaredVersion = declaredVersionInfo.version;
        assert declaredVersion != null;
        assert requiredVersion != null;
        assert !requiredVersion.equals(declaredVersion);

        int comparison = compareVersions(declaredVersion, requiredVersion);
        if (comparison == 0) {
            throw new AssertionError("Versions are different but comparison yields 0: " + declaredVersion + " vs " + requiredVersion);
        } else if (comparison > 0) {
            // declared > expected
            return State.INCOMPATIBLE;
        } else {
            // declared < expected
            if (AUTOMATICALLY_UPGRADEABLE.contains(new ImmutablePair<>(declaredVersion, requiredVersion))) {
                return State.AUTOMATICALLY_UPGRADEABLE;
            } else {
                return State.MANUALLY_UPGRADEABLE;
            }
        }
    }

    @NotNull
    private UpgradeableSchemaAction getUpgradeableSchemaAction() {
        return repositoryConfiguration.getUpgradeableSchemaAction();
    }

    @NotNull
    private SqlRepositoryConfiguration.IncompatibleSchemaAction getIncompatibleSchemaAction() {
        return repositoryConfiguration.getIncompatibleSchemaAction();
    }

    @NotNull
    private MissingSchemaAction getMissingSchemaAction() {
        return repositoryConfiguration.getMissingSchemaAction();
    }

    private String determineUpgradeScriptFileName(@NotNull String from, @NotNull String to) {
        return getDatabaseType().name().toLowerCase()
                + "-upgrade-" + from + "-" + to + ".sql";
    }

    private String determineCreateScriptFileName() {
        return getDatabaseType().name().toLowerCase()
                + "-" + REQUIRED_DATABASE_SCHEMA_VERSION + "-all" + ".sql";
    }

    @NotNull
    private SupportedDatabase getDatabaseType() {
        SupportedDatabase database = repositoryConfiguration.getDatabaseType();
        if (database == null) {
            throw new SystemException("Couldn't create/upgrade DB schema because database kind is not known");
        }
        return database;
    }

    private int compareVersions(String version1, String version2) {
        PrimitiveIterator.OfInt parts1 = toParts(version1);
        PrimitiveIterator.OfInt parts2 = toParts(version2);

        for (; ; ) {
            if (!parts1.hasNext()) {
                if (parts2.hasNext()) {
                    return -1;
                } else {
                    return 0;
                }
            } else if (!parts2.hasNext()) {
                return 1;
            }
            int next1 = parts1.next();
            int next2 = parts2.next();
            if (next1 != next2) {
                return Integer.compare(next1, next2);
            }
        }
    }

    private PrimitiveIterator.OfInt toParts(String s) {
        return Arrays.stream(s.split("\\.")).mapToInt(Integer::parseInt).iterator();
    }
}
