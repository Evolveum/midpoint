/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdSchemaParser.ParsedSchemaInfo;
import com.evolveum.midpoint.util.exception.*;

import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.*;
import org.identityconnectors.framework.common.objects.OperationOptionInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Schema;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdSchemaParser.SpecialAttributes;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * Class that can parse ConnId capabilities and schema into midPoint format.
 *
 * May be used either for parsing both capabilities and schema (see
 * {@link #retrieveResourceCapabilitiesAndSchema(Collection, OperationResult)}) or for parsing
 * capabilities only (see {@link #fetchAndParseConnIdCapabilities(OperationResult)}).
 *
 * Note that these structures are intertwined. Capabilities affect the schema, because they determine if the schema can be
 * retrieved at all. The retrieved schema affects the capabilities as well: the presence of {@link SpecialAttributes} influence
 * what capabilities we consider to be present.
 *
 * @author Radovan Semancik
 */
class ConnIdCapabilitiesAndSchemaParser {

    private static final Trace LOGGER = TraceManager.getTrace(ConnIdCapabilitiesAndSchemaParser.class);

    private static final String OP_GET_SUPPORTED_OPERATIONS = ConnectorFacade.class.getName() + ".getSupportedOperations";
    private static final String OP_SCHEMA = ConnectorFacade.class.getName() + ".schema";

    @NotNull private final ConnectorFacade connIdConnectorFacade;

    @NotNull private final ConnectorContext connectorContext;

    @NotNull private final String connectorHumanReadableName;

    /** When schema parsing is requested. */
    ConnIdCapabilitiesAndSchemaParser(
            @NotNull ConnectorFacade connIdConnectorFacade,
            @NotNull ConnectorContext connectorContext) {
        this.connIdConnectorFacade = connIdConnectorFacade;
        this.connectorContext = connectorContext;
        this.connectorHumanReadableName = connectorContext.getHumanReadableName();
    }

    /**
     * Retrieves and parses resource capabilities and schema.
     *
     * Schema is immutable after this method.
     */
    @NotNull NativeCapabilitiesAndSchema retrieveResourceCapabilitiesAndSchema(
            @NotNull Collection<QName> objectClassesToParse, OperationResult result)
            throws CommunicationException, ConfigurationException, GenericFrameworkException, SchemaException {

        LOGGER.debug("Retrieving and parsing schema and capabilities for {}", connectorHumanReadableName);

        var capabilities = fetchAndParseConnIdCapabilities(result);

        ParsedSchemaInfo schemaInfo;
        if (!capabilities.supportsSchema()) {
            schemaInfo = null;
        } else {
            Schema connIdSchema = fetchConnIdSchema(result);
            if (connIdSchema == null) {
                capabilities.updateWithoutSchema();
                schemaInfo = null;
            } else {
                schemaInfo =
                        new ConnIdSchemaParser(connIdSchema, objectClassesToParse, connectorContext.getConfiguredLegacySchema())
                                .parse();
                capabilities.updateFromSchema(connIdSchema, schemaInfo.specialAttributes());
                schemaInfo.parsedSchema().freeze();
            }
        }
        return new NativeCapabilitiesAndSchema(
                capabilities.midPointCapabilities,
                schemaInfo != null ? schemaInfo.parsedSchema() : null,
                schemaInfo != null ? schemaInfo.legacySchema() : null);
    }

    /** Retrieves and parses connector capabilities. */
    @NotNull Capabilities fetchAndParseConnIdCapabilities(OperationResult result)
            throws CommunicationException, ConfigurationException, GenericFrameworkException {
        var connIdCapabilities = fetchConnIdCapabilities(result);
        var midPointCapabilities = parseConnIdCapabilities(connIdCapabilities);
        return new Capabilities(midPointCapabilities, connIdCapabilities);
    }

    /** Fetches capabilities i.e. supported operations the from ConnId connector. */
    private @NotNull Set<Class<? extends APIOperation>> fetchConnIdCapabilities(OperationResult parentResult)
            throws CommunicationException, ConfigurationException, GenericFrameworkException {
        OperationResult result = parentResult.createSubresult(OP_GET_SUPPORTED_OPERATIONS);
        result.addContext("connector", connIdConnectorFacade.getClass());

        try {
            LOGGER.debug("Fetching supported connector operations from {}", connectorHumanReadableName);
            InternalMonitor.recordConnectorOperation("getSupportedOperations");

            var operations = connIdConnectorFacade.getSupportedOperations();

            LOGGER.trace("Connector supported operations: {}", operations);
            return Objects.requireNonNull(operations);

        } catch (UnsupportedOperationException ex) {
            result.recordNotApplicable(ex.getMessage());
            return Set.of();
        } catch (Throwable ex) {
            Throwable midpointEx = ConnIdUtil.processConnIdException(ex, connectorHumanReadableName, result);
            result.recordException(midpointEx);
            castAndThrowException(ex, midpointEx);
            throw new NotHereAssertionError();
        } finally {
            result.close();
        }
    }

    /**
     * Casts arbitrary {@link Throwable} to a supported kind of exception.
     */
    private void castAndThrowException(Throwable originalException, Throwable convertedException)
            throws CommunicationException, ConfigurationException, GenericFrameworkException {

        if (convertedException instanceof CommunicationException communicationException) {
            throw communicationException;
        } else if (convertedException instanceof ConfigurationException configurationException) {
            throw configurationException;
        } else if (convertedException instanceof GenericFrameworkException genericFrameworkException) {
            throw genericFrameworkException;
        } else if (convertedException instanceof RuntimeException runtimeException) {
            throw runtimeException;
        } else if (convertedException instanceof Error error) {
            throw error;
        } else {
            throw new SystemException("Got unexpected exception: " + originalException.getClass().getName()
                    + ": " + originalException.getMessage(), originalException);
        }
    }

    private Schema fetchConnIdSchema(OperationResult parentResult)
            throws CommunicationException, ConfigurationException, GenericFrameworkException {
        // Connector operation cannot create result for itself, so we need to create result for it
        OperationResult result = parentResult.createSubresult(OP_SCHEMA);
        result.addContext("connector", connIdConnectorFacade.getClass());
        try {

            LOGGER.debug("Fetching schema from {}", connectorHumanReadableName);
            // Fetch the schema from the connector (which actually gets that from the resource).
            InternalMonitor.recordConnectorOperation("schema");
            // TODO have context present
            Schema connIdSchema = connIdConnectorFacade.schema();
            if (connIdSchema == null) {
                result.recordNotApplicable("Null schema returned");
            } else {
                result.recordSuccess();
            }
            return connIdSchema;
        } catch (UnsupportedOperationException ex) {
            //recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_SCHEMA, null, ex);
            // The connector does no support schema() operation.
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, ex.getMessage());
            return null;
        } catch (Throwable ex) {
            //recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_SCHEMA, null, ex);
            // conditions.
            // Therefore this kind of heavy artillery is necessary.
            // ICF interface does not specify exceptions or other error
            // TODO maybe we can try to catch at least some specific exceptions
            Throwable midpointEx = ConnIdUtil.processConnIdException(ex, connectorHumanReadableName, result);
            result.recordFatalError(midpointEx.getMessage(), midpointEx);
            castAndThrowException(ex, midpointEx);
            throw new AssertionError("not here");
        } finally {
            result.close();
        }
    }

    /**
     * Create capabilities from supported connector operations.
     * These are not complete! Some information is to be derived from the schema.
     */
    private @NotNull CapabilityCollectionType parseConnIdCapabilities(
            @NotNull Set<Class<? extends APIOperation>> connIdSupportedOperations) {

        CapabilityCollectionType capabilities = new CapabilityCollectionType();

        if (connIdSupportedOperations.contains(SchemaApiOp.class)) {
            capabilities.setSchema(new SchemaCapabilityType());
        }

        if (connIdSupportedOperations.contains(DiscoverConfigurationApiOp.class)) {
            capabilities.setDiscoverConfiguration(new DiscoverConfigurationCapabilityType());
        }

        if (connIdSupportedOperations.contains(SyncApiOp.class)) {
            capabilities.setLiveSync(new LiveSyncCapabilityType());
        }

        if (connIdSupportedOperations.contains(TestApiOp.class)) {
            capabilities.setTestConnection(new TestConnectionCapabilityType());
        }

        if (connIdSupportedOperations.contains(CreateApiOp.class)){
            capabilities.setCreate(new CreateCapabilityType());
        }

        // GetApiOp is processed later. We need supported options (from schema) to fully process it.

        if (connIdSupportedOperations.contains(UpdateDeltaApiOp.class)) {
            UpdateCapabilityType capUpdate = new UpdateCapabilityType();
            capUpdate.setDelta(true);
            // This is the default for all resources.
            // (Currently there is no way how to obtain it from the connector.)
            // It can be disabled manually.
            capUpdate.setAddRemoveAttributeValues(true);
            capabilities.setUpdate(capUpdate);
        } else if (connIdSupportedOperations.contains(UpdateApiOp.class)) {
            UpdateCapabilityType capUpdate = new UpdateCapabilityType();
            // This is the default for all resources.
            // (Currently there is no way how to obtain it from the connector.)
            // It can be disabled manually.
            capUpdate.setAddRemoveAttributeValues(true);
            capabilities.setUpdate(capUpdate);
        }

        if (connIdSupportedOperations.contains(DeleteApiOp.class)) {
            capabilities.setDelete(new DeleteCapabilityType());
        }

        if (connIdSupportedOperations.contains(ScriptOnResourceApiOp.class)
                || connIdSupportedOperations.contains(ScriptOnConnectorApiOp.class)) {
            ScriptCapabilityType capScript = new ScriptCapabilityType();
            if (connIdSupportedOperations.contains(ScriptOnResourceApiOp.class)) {
                ScriptCapabilityHostType host = new ScriptCapabilityHostType();
                host.setType(ProvisioningScriptHostType.RESOURCE);
                capScript.getHost().add(host);
                // language is unknown here
            }
            if (connIdSupportedOperations.contains(ScriptOnConnectorApiOp.class)) {
                ScriptCapabilityHostType host = new ScriptCapabilityHostType();
                host.setType(ProvisioningScriptHostType.CONNECTOR);
                capScript.getHost().add(host);
                // language is unknown here
            }
            capabilities.setScript(capScript);
        }

        return capabilities;
    }

    /** Combined ConnId and midPoint-style capabilities. */
    record Capabilities(
            @NotNull CapabilityCollectionType midPointCapabilities,
            @NotNull Set<Class<? extends APIOperation>> connIdCapabilities) {

        boolean supportsSchema() {
            return midPointCapabilities.getSchema() != null;
        }

        /**
         * Still need to add "read" capability. This capability would be added during schema processing,
         * because it depends on schema options. But if there is no schema we need to add read capability
         * anyway. We do not want to end up with non-readable resource.
         */
        void updateWithoutSchema() {
            if (connIdCapabilities.contains(GetApiOp.class) || connIdCapabilities.contains(SearchApiOp.class)) {
                midPointCapabilities.setRead(new ReadCapabilityType());
            }
        }

        void updateFromSchema(
                @NotNull Schema connIdSchema,
                @NotNull SpecialAttributes specialAttributes) {

            ActivationCapabilityType capAct = getActivationCapabilityType(specialAttributes);

            // TODO: activation and credentials should be per-objectclass capabilities
            if (capAct != null) {
                midPointCapabilities.setActivation(capAct);
            }

            BehaviorCapabilityType capBeh = getBehaviorCapabilityType(specialAttributes);
            if (capBeh != null) {
                midPointCapabilities.setBehavior(capBeh);
            }

            if (specialAttributes.passwordAttributeInfo != null) {
                CredentialsCapabilityType capCred = new CredentialsCapabilityType();
                PasswordCapabilityType capPass = new PasswordCapabilityType();
                if (!specialAttributes.passwordAttributeInfo.isReturnedByDefault()) {
                    capPass.setReturnedByDefault(false);
                }
                if (specialAttributes.passwordAttributeInfo.isReadable()) {
                    capPass.setReadable(true);
                }
                capCred.setPassword(capPass);
                midPointCapabilities.setCredentials(capCred);
            }

            if (specialAttributes.auxiliaryObjectClassAttributeInfo != null) {
                midPointCapabilities.setAuxiliaryObjectClasses(new AuxiliaryObjectClassesCapabilityType());
            }

            boolean canPageSize = false;
            boolean canPageOffset = false;
            boolean canSort = false;
            boolean supportsReturnDefaultAttributes = false;
            for (OperationOptionInfo searchOption: connIdSchema.getSupportedOptionsByOperation(SearchApiOp.class)) {
                switch (searchOption.getName()) {
                    case OperationOptions.OP_PAGE_SIZE:
                        canPageSize = true;
                        break;
                    case OperationOptions.OP_PAGED_RESULTS_OFFSET:
                        canPageOffset = true;
                        break;
                    case OperationOptions.OP_SORT_KEYS:
                        canSort = true;
                        break;
                    case OperationOptions.OP_RETURN_DEFAULT_ATTRIBUTES:
                        supportsReturnDefaultAttributes = true;
                        break;
                }
            }
            if (canPageSize || canPageOffset || canSort) {
                midPointCapabilities.setPagedSearch(new PagedSearchCapabilityType());
            }

            if (connIdCapabilities.contains(GetApiOp.class) || connIdCapabilities.contains(SearchApiOp.class)) {
                ReadCapabilityType capRead = new ReadCapabilityType();
                capRead.setReturnDefaultAttributesOption(supportsReturnDefaultAttributes);
                midPointCapabilities.setRead(capRead);
            }
            if (connIdCapabilities.contains(UpdateDeltaApiOp.class)) {
                processUpdateOperationOptions(connIdSchema.getSupportedOptionsByOperation(UpdateDeltaApiOp.class));
            } else if (connIdCapabilities.contains(UpdateApiOp.class)) {
                processUpdateOperationOptions(connIdSchema.getSupportedOptionsByOperation(UpdateApiOp.class));
            }
        }

        private static @Nullable BehaviorCapabilityType getBehaviorCapabilityType(@NotNull SpecialAttributes specialAttributes) {
            BehaviorCapabilityType capBeh = null;
            if (specialAttributes.lastLoginDateAttributeInfo != null) {
                capBeh = new BehaviorCapabilityType();
                capBeh.setEnabled(true);
                LastLoginTimestampCapabilityType capLastLogin = new LastLoginTimestampCapabilityType();
                capBeh.setLastLoginTimestamp(capLastLogin);
                if (!specialAttributes.lastLoginDateAttributeInfo.isReturnedByDefault()) {
                    capLastLogin.setReturnedByDefault(false);
                }
            }
            return capBeh;
        }

        private static @Nullable ActivationCapabilityType getActivationCapabilityType(@NotNull SpecialAttributes specialAttributes) {
            ActivationCapabilityType capAct = null;

            if (specialAttributes.enableAttributeInfo != null) {
                capAct = new ActivationCapabilityType();
                ActivationStatusCapabilityType capActStatus = new ActivationStatusCapabilityType();
                capAct.setStatus(capActStatus);
                if (!specialAttributes.enableAttributeInfo.isReturnedByDefault()) {
                    capActStatus.setReturnedByDefault(false);
                }
            }

            if (specialAttributes.enableDateAttributeInfo != null) {
                if (capAct == null) {
                    capAct = new ActivationCapabilityType();
                }
                ActivationValidityCapabilityType capValidFrom = new ActivationValidityCapabilityType();
                capAct.setValidFrom(capValidFrom);
                if (!specialAttributes.enableDateAttributeInfo.isReturnedByDefault()) {
                    capValidFrom.setReturnedByDefault(false);
                }
            }

            if (specialAttributes.disableDateAttributeInfo != null) {
                if (capAct == null) {
                    capAct = new ActivationCapabilityType();
                }
                ActivationValidityCapabilityType capValidTo = new ActivationValidityCapabilityType();
                capAct.setValidTo(capValidTo);
                if (!specialAttributes.disableDateAttributeInfo.isReturnedByDefault()) {
                    capValidTo.setReturnedByDefault(false);
                }
            }

            if (specialAttributes.lockoutAttributeInfo != null) {
                if (capAct == null) {
                    capAct = new ActivationCapabilityType();
                }
                ActivationLockoutStatusCapabilityType capActStatus = new ActivationLockoutStatusCapabilityType();
                capAct.setLockoutStatus(capActStatus);
                if (!specialAttributes.lockoutAttributeInfo.isReturnedByDefault()) {
                    capActStatus.setReturnedByDefault(false);
                }
            }
            return capAct;
        }

        private void processUpdateOperationOptions(Set<OperationOptionInfo> supportedOptions) {
            boolean canRunAsUser = false;
            for (OperationOptionInfo searchOption : supportedOptions) {
                //noinspection SwitchStatementWithTooFewBranches,EnhancedSwitchMigration
                switch (searchOption.getName()) {
                    case OperationOptions.OP_RUN_AS_USER:
                        canRunAsUser = true;
                        break;
                    // TODO: run as password
                }
            }
            if (canRunAsUser) {
                midPointCapabilities.setRunAs(new RunAsCapabilityType());
            }
        }
    }
}
