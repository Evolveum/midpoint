/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import static java.util.Collections.emptySet;
import static org.apache.commons.collections4.SetUtils.emptyIfNull;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.ucfAttributeNameToConnId;
import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdUtil.processConnIdException;
import static com.evolveum.midpoint.schema.reporting.ConnIdOperation.getIdentifier;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.MiscUtil;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.identityconnectors.common.pooling.ObjectPoolConfiguration;
import org.identityconnectors.framework.api.*;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.impl.api.APIConfigurationImpl;
import org.identityconnectors.framework.impl.api.local.LocalConnectorInfoImpl;
import org.identityconnectors.framework.impl.api.local.ObjectPool;
import org.identityconnectors.framework.impl.api.local.ObjectPool.Statistics;
import org.identityconnectors.framework.impl.api.local.operations.ConnectorOperationalContext;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.query.FilterInterpreter;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.reporting.ConnIdOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CriticalityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Implementation of {@link ConnectorInstance} for ConnId connectors.
 *
 * This class implements the {@link ConnectorInstance} interface. The methods are converting the data from
 * the "midPoint semantics" as seen by the {@link ConnectorInstance} interface to the "ConnId semantics"
 * as seen by the ConnId framework.
 *
 * @author Radovan Semancik
 */
class ConnectorInstanceConnIdImpl implements ConnectorInstance, ConnectorContext {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorInstanceConnIdImpl.class);

    private static final String FACADE_OP_GET_OBJECT = ConnectorFacade.class.getName() + ".getObject";

    /** MidPoint connector definition bean. */
    @NotNull private final ConnectorType connectorBean;

    /**
     * MidPoint connector instance configuration (if this instance is configured).
     * Here we store the state _before_ connector schema is applied to the configuration.
     * (But maybe we are too cautious here.)
     */
    @Nullable private ConnectorConfiguration configuration;

    /** MidPoint connector schema; may be null if there are no configuration properties defined. */
    @Nullable private final ConnectorSchema connectorSchema;

    /** ConnId connector metadata. */
    @NotNull private final ConnectorInfo connIdConnectorInfo;

    /** ConnId connector configuration. */
    private APIConfiguration connIdConnectorConfig = null;

    /** ConnId connector operations entry point. */
    private ConnectorFacade connIdConnectorFacade;

    /** Useful Spring beans. */
    @NotNull private final ConnIdBeans b = ConnIdBeans.get();

    /** Helper that converts between midPoint and ConnId objects. */
    @NotNull final ConnIdObjectConvertor connIdObjectConvertor;

    /**
     * Holds provided or parsed ConnId schema and capabilities.
     *
     * By using this field the we always have a consistent schema, even during reconfigure and fetch operations.
     * There is either old schema or new schema, but there is no partially-parsed schema.
     */
    @NotNull private NativeCapabilitiesAndSchema nativeCapabilitiesAndSchema =
            new NativeCapabilitiesAndSchema(null, null, null);

    /**
     * Indicates that the real attempt to fetch capabilities and schema was made.
     *
     * Actually, this may not mean much. The schema may be updated by {@link #updateSchema(NativeResourceSchema)} later,
     * and we do not reset this flag in such cases.
     *
     * But currently we don't care. We just want to know that an attempt to fetch the schema was made, in order to provide
     * the same behavior as in 4.8 and before.
     */
    private boolean capabilitiesAndSchemaFetchedFromResource;

    /**
     * Does the resource use "legacy schema" i.e. `pass:[__ACCOUNT__]` and `pass:[__GROUP__]` object class names?
     * See e.g. https://docs.evolveum.com/connectors/connid/1.x/connector-development-guide/#schema-best-practices
     *
     * It can be configured or detected from the schema.
     */
    private Boolean configuredLegacySchema;

    /** Usually the resource name. */
    @NotNull private final String instanceName;

    /** Complex description for development diagnostics, e.g. `ConnectorSpec(resource OID...)` */
    @NotNull private final String description;

    ConnectorInstanceConnIdImpl(
            @NotNull ConnectorInfo connIdConnectorInfo,
            @NotNull ConnectorType connectorBean,
            @Nullable ConnectorSchema connectorSchema,
            @NotNull String instanceName,
            @NotNull String description) {
        this.connIdConnectorInfo = connIdConnectorInfo;
        this.connectorBean = connectorBean;
        this.connectorSchema = connectorSchema;
        this.connIdObjectConvertor = new ConnIdObjectConvertor(this);
        this.instanceName = instanceName;
        this.description = description;
    }

    public @NotNull String getDescription() {
        return description;
    }

    /**
     * Simple instance name for system administrator (name of the resource)
     */
    private @NotNull String getInstanceName() {
        return instanceName;
    }

    @Override
    public synchronized ConnectorInstance configure(
            @NotNull ConnectorConfiguration configuration,
            @NotNull ConnectorConfigurationOptions options,
            @NotNull OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_CONFIGURE);

        LOGGER.trace("Configuring connector {}, provided configuration:\n{}",
                connectorBean, configuration.debugDumpLazily(1));

        try {
            this.configuration = configuration.clone();

            // Make sure that the proper configuration schema is applied. This will cause that all the "raw" elements are parsed.
            // We need to use a clone, as the getCurrentConfiguration() method must return the same object as was passed
            // to this method.
            PrismContainerValue<?> configurationPcvRaw = configuration.configuration();
            PrismContainerValue<?> configurationPcv =
                    configurationPcvRaw != null && !configurationPcvRaw.hasNoItems() ?
                            configurationPcvRaw.clone().applyDefinition(getConfigurationContainerDefinition()) : null;

            // Transform XML configuration from the resource to the ConnId connector configuration
            try {
                connIdConnectorConfig =
                        new ConnIdConfigurationTransformer(connectorBean, connIdConnectorInfo, b.protector, options)
                                .transformConnectorConfiguration(configurationPcv);
            } catch (SchemaException e) {
                result.recordException(e);
                throw e;
            }

            logTransformedConfiguration();

            connIdConnectorConfig.setInstanceName(getInstanceName());

            ConnectorFacade oldConnIdConnectorFacade = connIdConnectorFacade;

            // Create new connector instance using the transformed configuration
            connIdConnectorFacade = ConnectorFacadeFactory.getInstance().newInstance(connIdConnectorConfig);

            if (oldConnIdConnectorFacade != null) {
                // Make sure old connector instance is disposed. We do not want to waste resources.
                // In case that old and new facade are the same, this will cause all existing
                // ConnId connector instances to dispose (i.e. connector pool is emptied).
                // But this is exactly what we want on reconfigure. We want the connections to
                // be closed and re-opened.
                LOGGER.debug("Disposing old ConnId ConnectorFacade for instance: {} (connector reconfiguration)", instanceName);
                oldConnIdConnectorFacade.dispose();
            }

            PrismProperty<Boolean> legacySchemaConfigProperty =
                    configurationPcv != null ?
                            configurationPcv.findProperty(ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_LEGACY_SCHEMA_ELEMENT) :
                            null;
            configuredLegacySchema = legacySchemaConfigProperty != null ? legacySchemaConfigProperty.getRealValue() : null;
            LOGGER.trace("Legacy schema (config): {}", configuredLegacySchema);

        } catch (Throwable ex) {
            Throwable midpointEx = processConnIdException(ex, this, result);
            result.computeStatus("Configuration operation failed");
            // Do some kind of acrobatics to do proper throwing of checked exception
            if (midpointEx instanceof CommunicationException communicationException) {
                throw communicationException;
            } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                throw genericFrameworkException;
            } else if (midpointEx instanceof SchemaException schemaException) {
                throw schemaException;
            } else if (midpointEx instanceof ConfigurationException configurationException) {
                throw configurationException;
            } else if (midpointEx instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (midpointEx instanceof Error error) {
                throw error;
            } else {
                throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        } finally {
            result.close();
        }
        return this;
    }

    @Override
    public @Nullable ConnectorConfiguration getCurrentConfiguration() {
        return configuration;
    }

    private void logTransformedConfiguration() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Configuring connector {}, transformed configuration:", connectorBean);
            var properties = connIdConnectorConfig.getConfigurationProperties();
            for (String propName : properties.getPropertyNames()) {
                LOGGER.trace("P: {} = {}", propName, properties.getProperty(propName).getValue());
            }
        }
    }

    private PrismContainerDefinition<?> getConfigurationContainerDefinition() throws SchemaException {
        var configContainerQName = new QName(connectorBean.getNamespace(), ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
        stateCheck(connectorSchema != null, "No schema for connector %s", instanceName);
        var configContainerDef = connectorSchema.findContainerDefinitionByElementName(configContainerQName);
        if (configContainerDef == null) {
            throw new SchemaException(
                    "No definition of container " + configContainerQName + " in configuration schema for connector " + this);
        }
        return configContainerDef;
    }

    @Override
    public ConnectorOperationalStatus getOperationalStatus() {

        if (!(connIdConnectorInfo instanceof LocalConnectorInfoImpl localConnectorInfo)) {
            LOGGER.trace("Cannot get operational status of a remote connector {}", connectorBean);
            return null;
        }

        if (connIdConnectorConfig == null) {
            LOGGER.trace("Cannot get operational status of a connector {}: connector not yet configured", connectorBean);
            throw new IllegalStateException("Connector "+ connectorBean +" not yet configured");
        }

        ConnectorOperationalStatus status = new ConnectorOperationalStatus();

        var connectorOperationalContext =
                new ConnectorOperationalContext(localConnectorInfo, (APIConfigurationImpl) connIdConnectorConfig);

        Class<? extends Connector> connectorClass = connectorOperationalContext.getConnectorClass();
        if (connectorClass != null) {
            status.setConnectorClassName(connectorClass.getName());
        }

        ObjectPoolConfiguration poolConfiguration = connIdConnectorConfig.getConnectorPoolConfiguration();
        if (poolConfiguration != null) {
            status.setPoolConfigMaxSize(poolConfiguration.getMaxObjects());
            status.setPoolConfigMinIdle(poolConfiguration.getMinIdle());
            status.setPoolConfigMaxIdle(poolConfiguration.getMaxIdle());
            status.setPoolConfigWaitTimeout(poolConfiguration.getMaxWait());
            status.setPoolConfigMinEvictableIdleTime(poolConfiguration.getMinEvictableIdleTimeMillis());
            status.setPoolConfigMaxIdleTime(poolConfiguration.getMaxIdleTimeMillis());
        }

        ObjectPool<PoolableConnector> pool = connectorOperationalContext.getPool();
        if (pool != null) {
            Statistics poolStats = pool.getStatistics();
            if (poolStats != null) {
                status.setPoolStatusNumActive(poolStats.getNumActive());
                status.setPoolStatusNumIdle(poolStats.getNumIdle());
            }
        }

        return status;
    }

    @NotNull ConnectorFacade getConnIdConnectorFacadeRequired() {
        return stateNonNull(connIdConnectorFacade,
                "Attempt to use unconfigured connector: %s %s", instanceName, description);
    }

    @NotNull private ConnectorConfiguration getConfigurationRequired() {
        return stateNonNull(configuration,
                "Attempt to use unconfigured connector: %s %s", instanceName, description);
    }

    private void checkConfigured() {
        getConnIdConnectorFacadeRequired();
        getConfigurationRequired();
    }

    @Override
    public @NotNull ConnectorInstance initialize(
            @Nullable NativeResourceSchema lastKnownResourceSchema,
            @Nullable CapabilityCollectionType lastKnownNativeCapabilities,
            @NotNull OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException {

        OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_INITIALIZE);
        result.addContext("connector", connectorBean);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ConnectorFactoryConnIdImpl.class);
        try {
            checkConfigured();
            if (lastKnownResourceSchema != null && lastKnownNativeCapabilities != null) {
                nativeCapabilitiesAndSchema =
                        NativeCapabilitiesAndSchema.of(lastKnownNativeCapabilities, lastKnownResourceSchema);
            } else {
                nativeCapabilitiesAndSchema = retrieveAndParseResourceCapabilitiesAndSchema(result);
            }
        } catch (Throwable t) {
            // Note that even communication exception while retrieving the schema is in fact fatal, because
            // there is no schema. Not even the pre-cached one. The connector will not be able to work.
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
        return this;
    }

    public void updateSchema(NativeResourceSchema resourceSchema) {
        if (resourceSchema != null) {
            nativeCapabilitiesAndSchema = nativeCapabilitiesAndSchema.withUpdatedSchema(resourceSchema);
        }
    }

    @Override
    public synchronized NativeResourceSchema fetchResourceSchema(@NotNull OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException {

        OperationResult result = parentResult.createSubresult(OP_FETCH_RESOURCE_SCHEMA);
        result.addContext("connector", connectorBean);

        try {

            if (capabilitiesAndSchemaFetchedFromResource && nativeCapabilitiesAndSchema.nativeSchema() != null) {
                return nativeCapabilitiesAndSchema.nativeSchema();
            }

            nativeCapabilitiesAndSchema = retrieveAndParseResourceCapabilitiesAndSchema(result);

            var fetchedSchema = nativeCapabilitiesAndSchema.nativeSchema();
            if (fetchedSchema == null) {
                result.recordNotApplicable("Connector does not support schema");
            }
            return fetchedSchema;

        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
    }

    @Override
    public synchronized CapabilityCollectionType fetchCapabilities(OperationResult parentResult)
        throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException {

        OperationResult result = parentResult.createMinorSubresult(OP_FETCH_CAPABILITIES);
        result.addContext("connector", connectorBean);

        try {
            // Always refresh capabilities and schema here, even if we have already parsed that before.
            // This method is used in "test connection" (among other places). We want to force fresh fetch there.
            // TODO: later: clean up this mess. Make fresh fetch somehow explicit?
            nativeCapabilitiesAndSchema = retrieveAndParseResourceCapabilitiesAndSchema(result);
            return nativeCapabilitiesAndSchema.capabilities();
        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
    }

    @Override
    public @NotNull CapabilityCollectionType getNativeCapabilities(OperationResult result)
            throws CommunicationException, ConfigurationException, GenericFrameworkException {

        APIConfiguration apiConfig = connIdConnectorInfo.createDefaultAPIConfiguration();

        ConnectorFacade facade = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

        return new ConnIdCapabilitiesAndSchemaParser(facade, this)
                .fetchAndParseConnIdCapabilities(result)
                .midPointCapabilities();
    }

    private @NotNull NativeCapabilitiesAndSchema retrieveAndParseResourceCapabilitiesAndSchema(OperationResult result)
            throws CommunicationException, ConfigurationException, GenericFrameworkException, SchemaException {

        var old = nativeCapabilitiesAndSchema;

        var current =
                new ConnIdCapabilitiesAndSchemaParser(getConnIdConnectorFacadeRequired(), this)
                        .retrieveResourceCapabilitiesAndSchema(getConfigurationRequired().objectClassesToGenerate(), result);

        capabilitiesAndSchemaFetchedFromResource = true;

        if (current.nativeSchema() == null) {
            // If the schema is not fetched, we want to keep the old one.
            return old.withUpdatedCapabilities(current.capabilities());
        } else {
            return current;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private synchronized <C extends CapabilityType> C getCapability(Class<C> capClass) {
        return CapabilityUtil.getCapability(getCapabilities(), capClass);
    }

    @Override
    public UcfResourceObject fetchObject(
            @NotNull ResourceObjectIdentification.WithPrimary resourceObjectIdentification,
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, SecurityViolationException, ConfigurationException {

        Validate.notNull(resourceObjectIdentification, "Null primary identifiers");
        ResourceObjectDefinition objectDefinition = resourceObjectIdentification.getResourceObjectDefinition();

        OperationResult result = parentResult.createMinorSubresult(OP_FETCH_OBJECT);
        result.addArbitraryObjectAsParam("resourceObjectDefinition", objectDefinition);
        result.addArbitraryObjectAsParam("identification", resourceObjectIdentification);
        result.addContext("connector", connectorBean);
        try {
            Uid uid = getUid(resourceObjectIdentification);
            ObjectClass icfObjectClass = objectClassToConnId(objectDefinition);

            OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
            convertToIcfAttrsToGet(objectDefinition, shadowItemsToReturn, optionsBuilder);
            optionsBuilder.setAllowPartialResults(true);
            OperationOptions options = optionsBuilder.build();

            ConnectorObject co;
            //noinspection CaughtExceptionImmediatelyRethrown
            try {
                co = fetchConnectorObject(ctx, objectDefinition, icfObjectClass, uid, options, result);
            } catch (CommunicationException | RuntimeException | SchemaException | GenericFrameworkException |
                    ConfigurationException | SecurityViolationException ex) {
                // This is fatal. No point in continuing. Just re-throw the exception.
                throw ex;
            } catch (ObjectNotFoundException ex) {
                throw ex.wrap(String.format(
                        "Object identified by %s (ConnId UID %s), objectClass %s was not found in %s",
                        resourceObjectIdentification, uid, objectDefinition.getTypeName(), description));
            }

            if (co == null) {
                throw new ObjectNotFoundException(
                        String.format(
                                "Object identified by %s (ConnId UID %s), objectClass %s was not in %s",
                                resourceObjectIdentification, uid, objectDefinition.getTypeName(), description),
                        ShadowType.class, uid.getUidValue());
            }

            // TODO configure error reporting method
            return connIdObjectConvertor.convertToUcfObject(
                    co, objectDefinition, UcfFetchErrorReportingMethod.EXCEPTION, createOperationContext(ctx), result);

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Returns null if nothing is found.
     */
    private ConnectorObject fetchConnectorObject(
            UcfExecutionContext reporter,
            ResourceObjectDefinition objectDefinition,
            ObjectClass icfObjectClass,
            Uid uid,
            OperationOptions options,
            OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SecurityViolationException,
            SchemaException, ConfigurationException {

        // Connector operation cannot create result for itself, so we need to create result for it
        OperationResult icfResult = parentResult.createMinorSubresult(FACADE_OP_GET_OBJECT);
        icfResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);
        icfResult.addParam("uid", uid.getUidValue());
        icfResult.addArbitraryObjectAsParam("options", options);
        icfResult.addContext("connector", getConnIdConnectorFacadeRequired().getClass());

        InternalMonitor.recordConnectorOperation("getObject");
        ConnIdOperation operation = recordIcfOperationStart(reporter, ProvisioningOperation.ICF_GET, objectDefinition, uid);

        LOGGER.trace("Fetching connector object ObjectClass={}, UID={}, operation id={}, options={}",
                icfObjectClass, uid, getIdentifier(operation), ConnIdUtil.dumpOptionsLazily(options));

        ConnectorObject co;
        try {
            // Invoke the ConnId connector
            co = getConnIdConnectorFacadeRequired().getObject(icfObjectClass, uid, options);
            recordIcfOperationEnd(reporter, operation, null);
            icfResult.recordSuccess();
        } catch (Throwable ex) {
            recordIcfOperationEnd(reporter, operation, ex);
            String desc = this.getHumanReadableName() + " while getting object identified by ConnId UID '"+uid.getUidValue()+"'";
            Throwable midpointEx = processConnIdException(ex, desc, icfResult);
            icfResult.computeStatus("Add object failed");

            // Do some kind of acrobatics to do proper throwing of checked
            // exception
            if (midpointEx instanceof CommunicationException communicationException) {
                icfResult.muteError();
                throw communicationException;
            } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                throw genericFrameworkException;
            } else if (midpointEx instanceof ConfigurationException configurationException) {
                throw configurationException;
            } else if (midpointEx instanceof SecurityViolationException securityViolationException) {
                throw securityViolationException;
            } else if (midpointEx instanceof ObjectNotFoundException) {
                LOGGER.trace("Got ObjectNotFoundException while looking for resource object ConnId UID: {}", uid);
                return null;
            } else if (midpointEx instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (midpointEx instanceof Error) {
                // This should not happen. But some connectors are very strange.
                throw new SystemException("ERROR: "+midpointEx.getClass().getName()+": "+midpointEx.getMessage(), midpointEx);
            } else {
                throw new SystemException(midpointEx.getClass().getName()+": "+midpointEx.getMessage(), midpointEx);
            }

        }

        return co;
    }

    void convertToIcfAttrsToGet(
            ResourceObjectDefinition resourceObjectDefinition,
            ShadowItemsToReturn shadowItemsToReturn,
            OperationOptionsBuilder optionsBuilder) throws SchemaException {
        if (shadowItemsToReturn == null) {
            return;
        }
        if (shadowItemsToReturn.isAllDefault()) {
            return;
        }
        Set<String> icfAttrsToGet = new HashSet<>();
        if (shadowItemsToReturn.isReturnDefaultAttributes()) {
            if (supportsReturnDefaultAttributes()) {
                optionsBuilder.setReturnDefaultAttributes(true);
            } else {
                // Add all the attributes that are defined as "returned by default" by the schema
                for (var itemDef : resourceObjectDefinition.getAttributeDefinitions()) {
                    if (itemDef.isReturnedByDefault()) {
                        icfAttrsToGet.add(
                                ucfAttributeNameToConnId(itemDef));
                    }
                }
            }
        }
        if (shadowItemsToReturn.isReturnPasswordExplicit()
                || (shadowItemsToReturn.isReturnDefaultAttributes() && passwordReturnedByDefault())) {
            icfAttrsToGet.add(OperationalAttributes.PASSWORD_NAME);
        }
        if (shadowItemsToReturn.isReturnAdministrativeStatusExplicit()
                || (shadowItemsToReturn.isReturnDefaultAttributes() && enabledReturnedByDefault())) {
            icfAttrsToGet.add(OperationalAttributes.ENABLE_NAME);
        }
        if (shadowItemsToReturn.isReturnLockoutStatusExplicit()
                || (shadowItemsToReturn.isReturnDefaultAttributes() && lockoutReturnedByDefault())) {
            icfAttrsToGet.add(OperationalAttributes.LOCK_OUT_NAME);
        }
        if (shadowItemsToReturn.isReturnValidFromExplicit()
                || (shadowItemsToReturn.isReturnDefaultAttributes() && validFromReturnedByDefault())) {
            icfAttrsToGet.add(OperationalAttributes.ENABLE_DATE_NAME);
        }
        if (shadowItemsToReturn.isReturnValidToExplicit()
                || (shadowItemsToReturn.isReturnDefaultAttributes() && validToReturnedByDefault())) {
            icfAttrsToGet.add(OperationalAttributes.DISABLE_DATE_NAME);
        }
        if (shadowItemsToReturn.isReturnLastLoginTimestampExplicit()
            || (shadowItemsToReturn.isReturnDefaultAttributes() && lastLoginTimestampReturnedByDefault())) {
            icfAttrsToGet.add(PredefinedAttributes.LAST_LOGIN_DATE_NAME);
        }
        for (var itemDef : MiscUtil.emptyIfNull(shadowItemsToReturn.getItemsToReturn())) {
            icfAttrsToGet.add(
                    ucfAttributeNameToConnId(itemDef));
        }
        // Log full list here. ConnId is shortening it and it cannot be seen in logs.
        LOGGER.trace("Converted attributes to return: {}\n to ConnId attributesToGet: {}", shadowItemsToReturn, icfAttrsToGet);
        optionsBuilder.setAttributesToGet(icfAttrsToGet);
    }

    private synchronized boolean supportsReturnDefaultAttributes() {
        ReadCapabilityType capability = CapabilityUtil.getCapability(getCapabilities(), ReadCapabilityType.class);
        return capability != null
                && Boolean.TRUE.equals(capability.isReturnDefaultAttributesOption());
    }

    private synchronized boolean passwordReturnedByDefault() {
        return CapabilityUtil.isPasswordReturnedByDefault(
                CapabilityUtil.getCapability(getCapabilities(), CredentialsCapabilityType.class));
    }

    private synchronized boolean enabledReturnedByDefault() {
        return CapabilityUtil.isActivationStatusReturnedByDefault(
                CapabilityUtil.getCapability(getCapabilities(), ActivationCapabilityType.class));
    }

    private synchronized boolean lockoutReturnedByDefault() {
        return CapabilityUtil.isActivationLockoutStatusReturnedByDefault(
                CapabilityUtil.getCapability(getCapabilities(), ActivationCapabilityType.class));
    }

    private synchronized boolean validFromReturnedByDefault() {
        return CapabilityUtil.isActivationValidFromReturnedByDefault(
                CapabilityUtil.getCapability(getCapabilities(), ActivationCapabilityType.class));
    }

    private synchronized boolean validToReturnedByDefault() {
        return CapabilityUtil.isActivationValidToReturnedByDefault(
                CapabilityUtil.getCapability(getCapabilities(), ActivationCapabilityType.class));
    }

    private synchronized boolean lastLoginTimestampReturnedByDefault() {
        return CapabilityUtil.isLastLoginTimestampReturnedByDefault(
                CapabilityUtil.getCapability(getCapabilities(), BehaviorCapabilityType.class));
    }

    private synchronized boolean supportsDeltaUpdateOp() {
        UpdateCapabilityType capability = CapabilityUtil.getCapability(getCapabilities(), UpdateCapabilityType.class);
        if (capability == null) {
            return false;
        }
        Boolean delta = capability.isDelta();
        if (delta == null) {
            return false;
        }
        return delta;
    }

    @Override
    public @NotNull UcfAddReturnValue addObject(
            @NotNull PrismObject<? extends ShadowType> shadow,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException {

        UcfExecutionContext.checkExecutionFullyPersistent(ctx);

        validateShadowOnAdd(shadow);

        Collection<ShadowSimpleAttribute<?>> identifiers;

        OperationResult result = parentResult.createSubresult(OP_ADD_OBJECT);
        result.addParam("resourceObject", shadow);
        try {

            var objDef = ShadowUtil.getResourceObjectDefinition(shadow.asObjectable());

            var connIdInfo = connIdObjectConvertor.convertToConnIdObjectInfo(shadow.asObjectable(), false);

            OperationOptionsBuilder operationOptionsBuilder = new OperationOptionsBuilder();
            OperationOptions options = operationOptionsBuilder.build();

            OperationResult connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".create");
            connIdResult.addArbitraryObjectAsParam("objectClass", connIdInfo.objectClass());
            connIdResult.addArbitraryObjectCollectionAsParam("auxiliaryObjectClasses", connIdInfo.auxiliaryObjectClasses());
            connIdResult.addArbitraryObjectCollectionAsParam("attributes", connIdInfo.attributes());
            connIdResult.addArbitraryObjectAsParam("options", options);
            connIdResult.addContext("connector", getConnIdConnectorFacadeRequired().getClass());

            // CALL THE ConnId FRAMEWORK
            InternalMonitor.recordConnectorOperation("create");
            InternalMonitor.recordConnectorModification("create");
            ConnIdOperation operation = recordIcfOperationStart(ctx, ProvisioningOperation.ICF_CREATE, objDef, null);

            Uid uid;
            try {

                LOGGER.trace("Calling ConnId create for {}", operation);
                uid = getConnIdConnectorFacadeRequired().create(connIdInfo.objectClass(), connIdInfo.attributes(), options);
                if (uid != null) {
                    operation.setUid(uid.getUidValue());
                }
                recordIcfOperationEnd(ctx, operation, null);

            } catch (Throwable ex) {
                recordIcfOperationEnd(ctx, operation, ex);
                Throwable midpointEx = processConnIdException(ex, this, connIdResult);
                result.computeStatus("Add object failed");

                // Do some kind of acrobatics to do proper throwing of checked exception
                if (midpointEx instanceof ObjectAlreadyExistsException objectAlreadyExistsException) {
                    throw objectAlreadyExistsException;
                } else if (midpointEx instanceof CommunicationException communicationException) {
                    throw communicationException;
                } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                    throw genericFrameworkException;
                } else if (midpointEx instanceof SchemaException schemaException) {
                    throw schemaException;
                } else if (midpointEx instanceof ConfigurationException configurationException) {
                    throw configurationException;
                } else if (midpointEx instanceof SecurityViolationException securityViolationException) {
                    throw securityViolationException;
                } else if (midpointEx instanceof PolicyViolationException policyViolationException) {
                    throw policyViolationException;
                } else if (midpointEx instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else if (midpointEx instanceof Error error) {
                    throw error;
                } else {
                    throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }

            if (uid == null || uid.getUidValue() == null || uid.getUidValue().isEmpty()) {
                connIdResult.recordFatalError("ConnId did not returned UID after create");
                result.computeStatus("Add object failed");
                throw new GenericFrameworkException("ConnId did not returned UID after create");
            }

            identifiers = ConnIdUtil.convertToIdentifiers(uid, objDef, ctx.getResourceSchema());

            connIdResult.recordSuccess();
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
        return UcfAddReturnValue.fromResult(
                List.copyOf(identifiers),
                result);
    }

    private void validateShadowOnAdd(PrismObject<? extends ShadowType> shadow) throws SchemaException {
        if (shadow == null) {
            throw new IllegalArgumentException("Cannot add null shadow");
        }
        var attributesContainer = ShadowUtil.getAttributesContainer(shadow);
        if (attributesContainer == null) {
            throw new IllegalArgumentException("Cannot add a shadow without attributes container");
        }
        // This is a legacy check; to be reviewed (why don't we check UID in non-ConnId form like ri:entryUUID?)
        if (attributesContainer.findSimpleAttribute(SchemaConstants.ICFS_UID) != null) {
            throw new SchemaException("ICF UID explicitly specified in attributes");
        }
    }

    // TODO [med] beware, this method does not obey its contract specified in the interface
    // (1) currently it does not return all the changes, only the 'side effect' changes
    // (2) it throws exceptions even if some of the changes were made
    // (3) among identifiers, only the UID value is updated on object rename
    //     (other identifiers are ignored on input and output of this method)

    @Override
    public @NotNull UcfModifyReturnValue modifyObject(
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            PrismObject<ShadowType> shadowIgnored,
            @NotNull Collection<Operation> changes,
            ConnectorOperationOptions options,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(OP_MODIFY_OBJECT);
        result.addArbitraryObjectAsParam("identification", identification);
        result.addArbitraryObjectCollectionAsParam("changes", changes);
        result.addArbitraryObjectAsParam("options", options);

        try {

            if (changes.isEmpty()) {
                LOGGER.debug("No modifications for connector object specified. Skipping processing.");
                result.recordNotApplicable();
                return UcfModifyReturnValue.fromResult(List.of(), result);
            }

            UcfExecutionContext.checkExecutionFullyPersistent(ctx);

            Uid uid = getUid(identification);
            ObjectClass objClass = objectClassToConnId(identification.getResourceObjectDefinition());

            if (supportsDeltaUpdateOp()) {
                return modifyObjectDelta(identification, objClass, uid, changes, options, ctx, result);
            } else {
                return modifyObjectUpdate(identification, objClass, uid, changes, options, ctx, result);
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Modifies object by using new delta update operations.
     */
    private @NotNull UcfModifyReturnValue modifyObjectDelta(
            ResourceObjectIdentification.WithPrimary identification,
            ObjectClass objClass,
            Uid uid,
            Collection<Operation> changes,
            ConnectorOperationOptions options,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            OperationResult result)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException {

        ResourceObjectDefinition objectClassDef = identification.getResourceObjectDefinition();

        var resourceSchema = createOperationContext(ctx).getResourceSchemaRequired();
        var converter = new DeltaModificationConverter(
                changes, resourceSchema, objectClassDef, description, options, connIdObjectConvertor);
        converter.convert();

        LOGGER.trace("converted attributesDelta:\n {}", converter.debugDumpLazily(1));

        OperationResult connIdResult;

        @NotNull Set<AttributeDelta> knownExecutedChanges; // May or may not cover all executed changes
        Set<AttributeDelta> attributesDelta = converter.getAttributesDeltas();
        if (!attributesDelta.isEmpty()) {
            OperationOptions connIdOptions = createConnIdOptions(options, changes);
            connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".updateDelta");
            connIdResult.addParam("objectClass", objectClassDef.toString());
            connIdResult.addParam("uid", uid.getUidValue());
            connIdResult.addParam("attributesDelta", attributesDelta.toString());
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", getConnIdConnectorFacadeRequired().getClass());

            InternalMonitor.recordConnectorOperation("update");
            InternalMonitor.recordConnectorModification("update");
            ConnIdOperation operation = recordIcfOperationStart(ctx, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);

            LOGGER.trace("Invoking ICF update(), objectclass={}, uid={}, operation id={}, attributes delta: {}",
                    objClass, uid, getIdentifier(operation), lazy(() -> dumpAttributesDelta(attributesDelta)));
            try {
                knownExecutedChanges =
                        emptyIfNull(getConnIdConnectorFacadeRequired().updateDelta(objClass, uid, attributesDelta, connIdOptions));

                recordIcfOperationEnd(ctx, operation, null);
                connIdResult.recordSuccess();
            } catch (Throwable ex) {
                recordIcfOperationEnd(ctx, operation, ex);
                String desc = this.getHumanReadableName() + " while updating object identified by ConnId UID '"+uid.getUidValue()+"'";
                Throwable midpointEx = processConnIdException(ex, desc, connIdResult);
                result.computeStatus("Update failed");
                // Do some kind of acrobatics to do proper throwing of checked
                // exception
                if (midpointEx instanceof ObjectNotFoundException objectNotFoundException) {
                    throw objectNotFoundException;
                } else if (midpointEx instanceof CommunicationException communicationException) {
                    //in this situation this is not a critical error, because we know to handle it..so mute the error and sign it as expected
                    result.muteError();
                    connIdResult.muteError();
                    throw communicationException;
                } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                    throw genericFrameworkException;
                } else if (midpointEx instanceof SchemaException schemaException) {
                    throw schemaException;
                } else if (midpointEx instanceof ObjectAlreadyExistsException objectAlreadyExistsException) {
                    throw objectAlreadyExistsException;
                } else if (midpointEx instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else if (midpointEx instanceof SecurityViolationException securityViolationException) {
                    throw securityViolationException;
                } else if (midpointEx instanceof PolicyViolationException policyViolationException) {
                    throw policyViolationException;
                } else if (midpointEx instanceof Error error) {
                    throw error;
                } else {
                    throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        } else {
            knownExecutedChanges = emptySet();
        }
        result.computeStatus();

        var knownExecutedOperations = convertToExecutedOperations(knownExecutedChanges, identification, objectClassDef);
        return UcfModifyReturnValue.fromResult(knownExecutedOperations, result);
    }

    private Collection<PropertyModificationOperation<?>> convertToExecutedOperations(
            Set<AttributeDelta> knownExecutedIcfDeltas,
            ResourceObjectIdentification.WithPrimary identification,
            ResourceObjectDefinition objectClassDef) throws SchemaException {
        Collection<PropertyModificationOperation<?>> knownExecutedOperations = new ArrayList<>();
        for (AttributeDelta executedIcfDelta : knownExecutedIcfDeltas) {
            String name = executedIcfDelta.getName();
            if (name.equals(Uid.NAME)) {
                Uid newUid = new Uid((String)executedIcfDelta.getValuesToReplace().get(0));
                PropertyDelta<String> uidDelta = createUidDelta(newUid, getUidDefinition(identification));
                PropertyModificationOperation<?> uidMod = new PropertyModificationOperation<>(uidDelta);
                knownExecutedOperations.add(uidMod);
            } else if (name.equals(Name.NAME)) {
                Name newName = new Name((String)executedIcfDelta.getValuesToReplace().get(0));
                PropertyDelta<?> nameDelta = createNameDelta(newName, getNameDefinition(identification));
                knownExecutedOperations.add(
                        new PropertyModificationOperation<>(nameDelta));
            } else {
                ShadowSimpleAttributeDefinition<Object> definition = objectClassDef.findSimpleAttributeDefinition(name);

                if (definition == null) {
                    throw new SchemaException("Returned delta references attribute '" + name + "' that has no definition.");
                }
                PropertyDelta<Object> delta = PrismContext.get().deltaFactory().property()
                        .create(ItemPath.create(ShadowType.F_ATTRIBUTES, definition.getItemName()), definition);
                if (executedIcfDelta.getValuesToReplace() != null) {
                    delta.setRealValuesToReplace(executedIcfDelta.getValuesToReplace().get(0));
                } else {
                    if (executedIcfDelta.getValuesToAdd() != null) {
                        for (Object value : executedIcfDelta.getValuesToAdd()) {
                            delta.addRealValuesToAdd(value);
                        }
                    }
                    if (executedIcfDelta.getValuesToRemove() != null) {
                        for (Object value : executedIcfDelta.getValuesToRemove()) {
                            delta.addRealValuesToDelete(value);
                        }
                    }
                }
                knownExecutedOperations.add(new PropertyModificationOperation<>(delta));
            }
        }
        return knownExecutedOperations;
    }

    /**
     * Modifies object by using old add/delete/replace attribute operations.
     */
    private @NotNull UcfModifyReturnValue modifyObjectUpdate(
            ResourceObjectIdentification.WithPrimary identification,
            ObjectClass objClass,
            Uid uid,
            Collection<Operation> changes,
            ConnectorOperationOptions options,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            OperationResult result)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
            SecurityViolationException, ObjectAlreadyExistsException, PolicyViolationException {


        ResourceObjectDefinition objectClassDef = identification.getResourceObjectDefinition();
        String originalUid = uid.getUidValue();

        var resourceSchema = createOperationContext(ctx).getResourceSchemaRequired();
        var converter = new UpdateModificationConverter(
                changes, resourceSchema, objectClassDef, description, options, connIdObjectConvertor);

        try {

            converter.convert();

        } catch (SchemaException | RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        }

        LOGGER.trace("converted attributes:\n{}", converter.debugDumpLazily(1));

        // Needs three complete try-catch blocks because we need to create
        // icfResult for each operation
        // and handle the faults individually

        OperationResult connIdResult;
        Set<Attribute> attributesToAdd = converter.getAttributesToAdd();
        if (!attributesToAdd.isEmpty()) {

            OperationOptions connIdOptions = createConnIdOptions(options, changes);
            connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".addAttributeValues");
            connIdResult.addArbitraryObjectAsParam("objectClass", objectClassDef);
            connIdResult.addParam("uid", uid.getUidValue());
            connIdResult.addArbitraryObjectAsParam("attributes", attributesToAdd);
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", getConnIdConnectorFacadeRequired().getClass());

            InternalMonitor.recordConnectorOperation("addAttributeValues");
            InternalMonitor.recordConnectorModification("addAttributeValues");
            @Nullable ConnIdOperation operation =
                    recordIcfOperationStart(ctx, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);

            LOGGER.trace(
                    "Invoking ConnId addAttributeValues(), objectclass={}, uid={}, operation id={}, attributes: {}",
                    objClass, uid, getIdentifier(operation), lazy(() -> dumpAttributes(attributesToAdd)));

            try {
                uid = getConnIdConnectorFacadeRequired().addAttributeValues(objClass, uid, attributesToAdd, connIdOptions);
                recordIcfOperationEnd(ctx, operation, null);

                connIdResult.recordSuccess();

            } catch (Throwable ex) {
                recordIcfOperationEnd(ctx, operation, ex);
                String desc = this.getHumanReadableName() + " while adding attribute values to object identified by ConnId UID '"+uid.getUidValue()+"'";
                Throwable midpointEx = processConnIdException(ex, desc, connIdResult);
                result.computeStatus("Adding attribute values failed");
                // Do some kind of acrobatics to do proper throwing of checked
                // exception
                if (midpointEx instanceof ObjectNotFoundException objectNotFoundException) {
                    throw objectNotFoundException;
                } else if (midpointEx instanceof CommunicationException communicationException) {
                    //in this situation this is not a critical error, because we know to handle it..so mute the error and sign it as expected
                    result.muteError();
                    connIdResult.muteError();
                    throw communicationException;
                } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                    throw genericFrameworkException;
                } else if (midpointEx instanceof SchemaException schemaException) {
                    throw schemaException;
                } else if (midpointEx instanceof AlreadyExistsException alreadyExistsException) {
                    throw alreadyExistsException;
                } else if (midpointEx instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else if (midpointEx instanceof SecurityViolationException securityViolationException){
                    throw securityViolationException;
                } else if (midpointEx instanceof PolicyViolationException policyViolationException) {
                    throw policyViolationException;
                } else if (midpointEx instanceof Error error){
                    throw error;
                } else {
                    throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        }

        Set<Attribute> attributesToUpdate = converter.getAttributesToUpdate();
        if (!attributesToUpdate.isEmpty()) {
            OperationOptions connIdOptions = createConnIdOptions(options, changes);
            connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".update");
            connIdResult.addArbitraryObjectAsParam("objectClass", objectClassDef);
            connIdResult.addParam("uid", uid==null?"null":uid.getUidValue());
            connIdResult.addArbitraryObjectAsParam("attributes", attributesToUpdate);
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", getConnIdConnectorFacadeRequired().getClass());

            InternalMonitor.recordConnectorOperation("update");
            InternalMonitor.recordConnectorModification("update");
            @Nullable ConnIdOperation operation =
                    recordIcfOperationStart(ctx, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);

            LOGGER.trace("Invoking ConnId update(), objectclass={}, uid={}, operation id={}, attributes: {}",
                    objClass, uid, getIdentifier(operation), lazy(() -> dumpAttributes(attributesToUpdate)));

            try {
                uid = getConnIdConnectorFacadeRequired().update(objClass, uid, attributesToUpdate, connIdOptions);
                recordIcfOperationEnd(ctx, operation, null);

                connIdResult.recordSuccess();
            } catch (Throwable ex) {
                recordIcfOperationEnd(ctx, operation, ex);
                String uidValue = uid != null ? uid.getUidValue() : null;
                String desc = this.getHumanReadableName() + " while updating object identified by ConnId UID '" + uidValue + "'";
                Throwable midpointEx = processConnIdException(ex, desc, connIdResult);
                result.computeStatus("Update failed");
                // Do some kind of acrobatics to do proper throwing of checked
                // exception
                if (midpointEx instanceof ObjectNotFoundException objectNotFoundException) {
                    throw objectNotFoundException;
                } else if (midpointEx instanceof CommunicationException communicationException) {
                    //in this situation this is not a critical error, because we know to handle it..so mute the error and sign it as expected
                    result.muteError();
                    connIdResult.muteError();
                    throw communicationException;
                } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                    throw genericFrameworkException;
                } else if (midpointEx instanceof SchemaException schemaException) {
                    throw schemaException;
                } else if (midpointEx instanceof ObjectAlreadyExistsException objectAlreadyExistsException) {
                    throw objectAlreadyExistsException;
                } else if (midpointEx instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else if (midpointEx instanceof SecurityViolationException securityViolationException) {
                    throw securityViolationException;
                } else if (midpointEx instanceof PolicyViolationException policyViolationException) {
                    throw policyViolationException;
                } else if (midpointEx instanceof Error error) {
                    throw error;
                } else {
                    throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        }

        Set<Attribute> attributesToRemove = converter.getAttributesToRemove();
        if (!attributesToRemove.isEmpty()) {

            OperationOptions connIdOptions = createConnIdOptions(options, changes);
            connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".removeAttributeValues");
            connIdResult.addArbitraryObjectAsParam("objectClass", objectClassDef);
            connIdResult.addParam("uid", uid.getUidValue());
            connIdResult.addArbitraryObjectAsParam("attributes", attributesToRemove);
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", getConnIdConnectorFacadeRequired().getClass());

            InternalMonitor.recordConnectorOperation("removeAttributeValues");
            InternalMonitor.recordConnectorModification("removeAttributeValues");
            @Nullable ConnIdOperation operation =
                    recordIcfOperationStart(ctx, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);

            LOGGER.trace(
                    "Invoking ConnId removeAttributeValues(), objectclass={}, uid={}, operation id={}, attributes: {}",
                    objClass, uid, getIdentifier(operation), lazy(() -> dumpAttributes(attributesToRemove)));

            try {
                uid = getConnIdConnectorFacadeRequired().removeAttributeValues(objClass, uid, attributesToRemove, connIdOptions);
                recordIcfOperationEnd(ctx, operation, null);
                connIdResult.recordSuccess();
            } catch (Throwable ex) {
                recordIcfOperationEnd(ctx, operation, ex);
                String desc = this.getHumanReadableName() + " while removing attribute values from object identified by ConnId UID '"+uid.getUidValue()+"'";
                Throwable midpointEx = processConnIdException(ex, desc, connIdResult);
                result.computeStatus("Removing attribute values failed");
                // Do some kind of acrobatics to do proper throwing of checked
                // exception
                if (midpointEx instanceof ObjectNotFoundException objectNotFoundException) {
                    throw objectNotFoundException;
                } else if (midpointEx instanceof CommunicationException communicationException) {
                    //in this situation this is not a critical error, because we know to handle it..so mute the error and sign it as expected
                    result.muteError();
                    connIdResult.muteError();
                    throw communicationException;
                } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                    throw genericFrameworkException;
                } else if (midpointEx instanceof SchemaException schemaException) {
                    throw schemaException;
                } else if (midpointEx instanceof ObjectAlreadyExistsException objectAlreadyExistsException) {
                    throw objectAlreadyExistsException;
                } else if (midpointEx instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else if (midpointEx instanceof SecurityViolationException securityViolationException) {
                    throw securityViolationException;
                } else if (midpointEx instanceof PolicyViolationException policyViolationException) {
                    throw policyViolationException;
                } else if (midpointEx instanceof Error error) {
                    throw error;
                } else {
                    throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        }
        result.computeStatus();

        Collection<PropertyModificationOperation<?>> sideEffectChanges = new ArrayList<>();
        if (!originalUid.equals(uid.getUidValue())) {
            // UID was changed during the operation, this is most likely a rename
            PropertyDelta<String> uidDelta = createUidDelta(uid, getUidDefinition(identification));
            // TODO what about matchingRuleQName ?
            sideEffectChanges.add(
                    new PropertyModificationOperation<>(uidDelta));
        }
        return UcfModifyReturnValue.fromResult(sideEffectChanges, result);
    }

    private PropertyDelta<?> createNameDelta(
            @NotNull Name name, @NotNull ShadowSimpleAttributeDefinition<?> nameDefinition) {
        //noinspection unchecked
        PropertyDelta<String> nameDelta =
                (PropertyDelta<String>) PrismContext.get().deltaFactory().property().create(
                        ItemPath.create(ShadowType.F_ATTRIBUTES, nameDefinition.getItemName()),
                        nameDefinition);
        nameDelta.setRealValuesToReplace(name.getNameValue());
        return nameDelta;
    }

    private PropertyDelta<String> createUidDelta(Uid uid, ShadowSimpleAttributeDefinition<String> uidDefinition) {
        PropertyDelta<String> uidDelta =
                PrismContext.get().deltaFactory().property()
                        .create(ItemPath.create(ShadowType.F_ATTRIBUTES, uidDefinition.getItemName()),
                uidDefinition);
        uidDelta.setRealValuesToReplace(uid.getUidValue());
        return uidDelta;
    }

    private String dumpAttributesDelta(Set<AttributeDelta> attributesDelta) {
        if (attributesDelta == null) {
            return "(null)";
        }
        if(attributesDelta.isEmpty()){
            return "(empty)";
        }
        StringBuilder sb = new StringBuilder();
        for (AttributeDelta attrDelta : attributesDelta) {
            sb.append("\n\n");
            sb.append(attrDelta.getName());
            sb.append("\n");
            sb.append(dumpValue("Values to Replace", attrDelta.getValuesToReplace()));
            sb.append("\n");
            sb.append(dumpValue("Values to Add", attrDelta.getValuesToAdd()));
            sb.append("\n");
            sb.append(dumpValue("Values to Remove", attrDelta.getValuesToRemove()));
        }
        return sb.toString();
    }

    private String dumpAttributes(Set<Attribute> attributes) {
        if (attributes == null) {
            return "(null)";
        }
        if (attributes.isEmpty()) {
            return "(empty)";
        }
        StringBuilder sb = new StringBuilder();
        for (Attribute attr : attributes) {
            sb.append("\n");
            if (attr.getValue() == null || attr.getValue().isEmpty()) {
                sb.append(attr.getName());
                sb.append(" (empty)");
            } else {
                for (Object value : attr.getValue()) {
                    sb.append(attr.getName());
                    sb.append(" = ");
                    sb.append(value);
                }
            }
        }
        return sb.toString();
    }

    private String dumpValue(String attrDeltaList, List<Object> values){
        StringBuilder sb = new StringBuilder();
        if (values == null || values.isEmpty()) {
            sb.append(attrDeltaList);
            sb.append(" (empty)");
        } else {
            sb.append(attrDeltaList);
            sb.append(" = ");
            for (Object value : values) {
                sb.append(value);
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Override
    public @NotNull UcfDeleteResult deleteObject(
            @NotNull ResourceObjectIdentification<?> identification,
            @Nullable PrismObject<ShadowType> shadow,
            @NotNull UcfExecutionContext ctx,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException {

        UcfExecutionContext.checkExecutionFullyPersistent(ctx);

        OperationResult result = parentResult.createSubresult(OP_DELETE_OBJECT);
        result.addArbitraryObjectAsParam("identification", identification);
        try {

            if (!(identification instanceof ResourceObjectIdentification.WithPrimary primaryIdentification)) {
                throw new IllegalArgumentException("Expected primary identification, got " + identification);
            }

            ResourceObjectDefinition objectDefinition = primaryIdentification.getResourceObjectDefinition();

            ObjectClass objClass = objectClassToConnId(objectDefinition);
            Uid uid = getUid(primaryIdentification);

            InternalMonitor.recordConnectorOperation("delete");
            InternalMonitor.recordConnectorModification("delete");
            ConnIdOperation operation = recordIcfOperationStart(ctx, ProvisioningOperation.ICF_DELETE, objectDefinition, uid);

            OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".delete");
            icfResult.addArbitraryObjectAsParam("uid", uid);
            icfResult.addArbitraryObjectAsParam("objectClass", objClass);
            icfResult.addContext("connector", getConnIdConnectorFacadeRequired().getClass());

            try {
                LOGGER.trace("Invoking ConnId delete operation: {}", operation);

                getConnIdConnectorFacadeRequired().delete(objClass, uid, new OperationOptionsBuilder().build());
                recordIcfOperationEnd(ctx, operation, null);

            } catch (Throwable ex) {
                recordIcfOperationEnd(ctx, operation, ex);
                String desc = "%s while deleting object identified by ConnId UID '%s'".formatted(
                        getHumanReadableName(), uid.getUidValue());
                Throwable midpointEx = processConnIdException(ex, desc, icfResult);
                // Do some kind of acrobatics to do proper throwing of checked exception
                if (midpointEx instanceof ObjectNotFoundException objectNotFoundException) {
                    throw objectNotFoundException;
                } else if (midpointEx instanceof CommunicationException communicationException) {
                    throw communicationException;
                } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                    throw genericFrameworkException;
                } else if (midpointEx instanceof SchemaException) {
                    // Schema exception during delete? It must be a missing UID
                    throw new IllegalArgumentException(midpointEx.getMessage(), midpointEx);
                } else if (midpointEx instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else if (midpointEx instanceof Error error) {
                    throw error;
                } else {
                    throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            } finally {
                icfResult.close();
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
        return UcfDeleteResult.fromResult(result);
    }

    @Override
    public UcfSyncToken fetchCurrentToken(
            ResourceObjectDefinition objectDefinition,
            @NotNull UcfExecutionContext ctx,
            @NotNull OperationResult parentResult) throws CommunicationException, GenericFrameworkException {

        OperationResult result = parentResult.createSubresult(OP_FETCH_CURRENT_TOKEN);
        result.addArbitraryObjectAsParam("objectClass", objectDefinition);

        ObjectClass icfObjectClass;
        if (objectDefinition == null) {
            icfObjectClass = ObjectClass.ALL;
        } else {
            icfObjectClass = objectClassToConnId(objectDefinition);
        }

        OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".sync");
        icfResult.addContext("connector", getConnIdConnectorFacadeRequired().getClass());
        icfResult.addArbitraryObjectAsParam("icfObjectClass", icfObjectClass);

        SyncToken syncToken;
        InternalMonitor.recordConnectorOperation("getLatestSyncToken");
        ConnIdOperation operation = recordIcfOperationStart(ctx, ProvisioningOperation.ICF_GET_LATEST_SYNC_TOKEN, objectDefinition);
        LOGGER.trace("Invoking ConnId getLatestSyncToken operation: {}", operation);
        try {
            syncToken = getConnIdConnectorFacadeRequired().getLatestSyncToken(icfObjectClass);
            recordIcfOperationEnd(ctx, operation, null);
            icfResult.recordSuccess();
            icfResult.addReturn("syncToken", syncToken==null?null:String.valueOf(syncToken.getValue()));
        } catch (Throwable ex) {
            recordIcfOperationEnd(ctx, operation, ex);
            Throwable midpointEx = processConnIdException(ex, this, icfResult);
            result.computeStatus();
            // Do some kind of acrobatics to do proper throwing of checked
            // exception
            if (midpointEx instanceof CommunicationException communicationException) {
                throw communicationException;
            } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                throw genericFrameworkException;
            } else if (midpointEx instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (midpointEx instanceof Error error) {
                throw error;
            } else {
                throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        if (syncToken != null) {
            result.recordSuccess();
            return TokenUtil.toUcf(syncToken);
        } else {
            result.recordWarning("Resource has not provided a current sync token");
            return null;
        }
    }

    @Override
    public UcfFetchChangesResult fetchChanges(
            @Nullable ResourceObjectDefinition objectDefinition,
            @Nullable UcfSyncToken initialTokenValue,
            @Nullable ShadowItemsToReturn attrsToReturn,
            @Nullable Integer maxChanges,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull UcfLiveSyncChangeListener changeListener,
            @NotNull OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException {

        OperationResult result = parentResult.subresult(OP_FETCH_CHANGES)
                .addArbitraryObjectAsContext("objectClass", objectDefinition)
                .addArbitraryObjectAsParam("initialToken", initialTokenValue)
                .build();
        try {
            SyncToken initialToken = TokenUtil.toConnId(initialTokenValue);
            LOGGER.trace("Initial token: {}", initialToken == null ? null : initialToken.getValue());

            ResourceObjectClassDefinition objectClassDefinition =
                    objectDefinition != null ? objectDefinition.getObjectClassDefinition() : null;

            // get icf object class
            ObjectClass requestConnIdObjectClass;
            if (objectClassDefinition == null) {
                requestConnIdObjectClass = ObjectClass.ALL;
            } else {
                requestConnIdObjectClass = objectClassToConnId(objectClassDefinition);
            }

            OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
            if (objectDefinition != null) {
                convertToIcfAttrsToGet(objectDefinition, attrsToReturn, optionsBuilder);
            }
            OperationOptions options = optionsBuilder.build();

            AtomicInteger deltasProcessed = new AtomicInteger(0);

            Thread callerThread = Thread.currentThread();

            SyncDeltaConverter changeConverter = new SyncDeltaConverter(
                    this, objectDefinition, createOperationContext(ctx));

            AtomicBoolean allChangesFetched = new AtomicBoolean(true);
            UcfFetchChangesResult fetchChangesResult;

            OperationResult connIdResult = result.subresult(ConnectorFacade.class.getName() + ".sync")
                    .addContext("connector", getConnIdConnectorFacadeRequired().getClass())
                    .addArbitraryObjectAsParam("objectClass", requestConnIdObjectClass)
                    .addArbitraryObjectAsParam("initialToken", initialToken)
                    .build();
            try {

                InternalMonitor.recordConnectorOperation("sync");
                ConnIdOperation operation = recordIcfOperationStart(ctx, ProvisioningOperation.ICF_SYNC, objectDefinition);

                /*
                 * We assume that the only way how changes are _not_ fetched is that we explicitly tell ConnId to stop
                 * fetching them by returning 'false' from the handler.handle() method. (Or an exception occurs in the sync()
                 * method.)
                 *
                 * In other words, we assume that if we tell ConnId to continue feeding changes to us, we are sure that on
                 * successful exit from sync() method all changes were processed.
                 */
                SyncResultsHandler syncHandler = syncDelta -> {

                    Thread handlingThread = Thread.currentThread();
                    if (!handlingThread.equals(callerThread)) {
                        LOGGER.warn("Live Sync changes are being processed in a thread {} that is different from the invoking one ({}). "
                                + "This can cause issues e.g. with operational statistics reporting.", handlingThread, callerThread);
                    }

                    recordIcfOperationSuspend(ctx, operation);
                    LOGGER.trace("Received sync delta: {}", syncDelta);
                    OperationResult handleResult;
                    // We can reasonably assume that this handler is NOT called in concurrent threads.
                    // But - just for sure - let us create subresults in a safe way.
                    synchronized (connIdResult) {
                        handleResult = connIdResult.subresult(OP_FETCH_CHANGES + ".handle")
                                .addArbitraryObjectAsParam("uid", syncDelta.getUid())
                                .setMinor()
                                .build();
                    }
                    UcfLiveSyncChange change = null;
                    try {
                        // Here we again assume we are called in a single thread, and that changes received here are in
                        // the correct order - i.e. in the order in which they are to be processed.
                        int sequentialNumber = deltasProcessed.incrementAndGet();

                        change = changeConverter.createChange(sequentialNumber, syncDelta, handleResult);

                        // The following should not throw any exceptions
                        boolean canContinue = changeListener.onChange(change, handleResult);

                        boolean doContinue = canContinue && canRun(ctx) && (maxChanges == null || maxChanges == 0 || sequentialNumber < maxChanges);
                        if (!doContinue) {
                            allChangesFetched.set(false);
                        }
                        return doContinue;

                    } catch (RuntimeException e) {
                        handleResult.recordFatalError(e);
                        // any exception here is not expected
                        LoggingUtils.logUnexpectedException(LOGGER, "Got unexpected exception while handling live sync "
                                + "change, stopping the processing. Sync delta: {}, UCF change: {}", e, syncDelta, change);
                        return false;
                    } finally {
                        // Asynchronously processed changes (if used) have their own, separate, operation results
                        // that are tied to the lightweight asynchronous task handlers in ChangeProcessingCoordinator.
                        //
                        // So we can safely compute/cleanup/summarize results here.
                        handleResult.computeStatusIfUnknown();
                        handleResult.cleanup();
                        connIdResult.summarize(true);
                        recordIcfOperationResume(ctx, operation);
                    }
                };

                LOGGER.trace("Invoking ConnId sync operation: {}", operation);
                SyncToken finalToken;
                try {
                    finalToken = getConnIdConnectorFacadeRequired()
                            .sync(requestConnIdObjectClass, initialToken, syncHandler, options);
                    // Note that finalToken value is not quite reliable. The SyncApiOp documentation is not clear on its semantics;
                    // it is only from SyncTokenResultsHandler (SPI) documentation and SyncImpl class that we know this value is
                    // non-null when all changes were fetched. And some of the connectors return null even then.
                    LOGGER.trace("connector sync method returned: {}", finalToken);
                    connIdResult.computeStatus();
                    connIdResult.cleanup();
                    connIdResult.addReturn(OperationResult.RETURN_COUNT, deltasProcessed.get());
                    recordIcfOperationEnd(ctx, operation, null);
                } catch (Throwable ex) {
                    recordIcfOperationEnd(ctx, operation, ex);
                    Throwable midpointEx = processConnIdException(ex, this, connIdResult);
                    connIdResult.computeStatusIfUnknown();
                    connIdResult.cleanup();
                    result.computeStatus();
                    // Do some kind of acrobatics to do proper throwing of checked exception
                    if (midpointEx instanceof CommunicationException communicationException) {
                        throw communicationException;
                    } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                        throw genericFrameworkException;
                    } else if (midpointEx instanceof SchemaException schemaException) {
                        throw schemaException;
                    } else if (midpointEx instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    } else if (midpointEx instanceof Error error) {
                        throw error;
                    } else {
                        throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                    }
                }
                if (!canRun(ctx)) {
                    result.recordStatus(OperationResultStatus.SUCCESS, "Interrupted by task suspension");
                }

                if (allChangesFetched.get()) {
                    // We might consider finalToken value here. I.e. it it's non null, we could declare all changes to be fetched.
                    // But as mentioned above, this is not supported explicitly in SyncApiOp. So let's be a bit conservative.
                    LOGGER.trace("All changes were fetched; with finalToken = {}", finalToken);
                    fetchChangesResult = new UcfFetchChangesResult(true, TokenUtil.toUcf(finalToken));
                } else {
                    fetchChangesResult = new UcfFetchChangesResult(false, null);
                }

            } catch (Throwable t) {
                connIdResult.recordFatalError(t);
                throw t;
            } finally {
                connIdResult.computeStatusIfUnknown();
            }

            result.recordSuccess();
            result.addReturn(OperationResult.RETURN_COUNT, deltasProcessed.get());

            return fetchChangesResult;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private boolean canRun(UcfExecutionContext reporter) {
        return reporter == null || reporter.canRun();
    }

    @Override
    public void test(OperationResult parentResult) {
        testConnection(false, parentResult);
    }

    @Override
    public void testPartialConfiguration(OperationResult parentResult) {
        testConnection(true, parentResult);
    }

    private void testConnection(boolean isPartialTest, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_TEST);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ConnectorInstanceConnIdImpl.class);
        result.addContext("connector", connectorBean);

        try {
            if (isPartialTest) {
                InternalMonitor.recordConnectorOperation("testPartialConfiguration");
                getConnIdConnectorFacadeRequired().testPartialConfiguration();
            } else {
                InternalMonitor.recordConnectorOperation("test");
                getConnIdConnectorFacadeRequired().test();
            }
        } catch (UnsupportedOperationException ex) {
            // Connector does not support test connection.
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE,
                    "Operation not supported by the connector", ex);
            // Do not rethrow. Recording the status is just OK.
        } catch (Throwable icfEx) {
            Throwable midPointEx = processConnIdException(icfEx, this, result);
            result.recordFatalError(midPointEx);
        } finally {
            result.close();
        }
    }

    @Override
    public @NotNull Collection<PrismProperty<?>> discoverConfiguration(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_DISCOVER_CONFIGURATION);
        result.addContext("connector", connectorBean);

        InternalMonitor.recordConnectorOperation("discoverConfiguration");

        try {
            Map<String, SuggestedValues> suggestions = getConnIdConnectorFacadeRequired().discoverConfiguration();

            ConnIdConfigurationTransformer configTransformer =
                    new ConnIdConfigurationTransformer(connectorBean, connIdConnectorInfo, b.protector, null);

            // Transform suggested configuration from the ConnId connector configuration to prism properties
            return configTransformer.transformSuggestedConfiguration(suggestions);
        } catch (UnsupportedOperationException ex) {
            // Connector does not support discover configuration.
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE,
                    "Operation not supported by the connector", ex);
            // Do not rethrow. Recording the status is just OK.
            return Collections.emptySet();
        } catch (Throwable icfEx) {
            Throwable midPointEx = processConnIdException(icfEx, this, result);
            result.recordFatalError(midPointEx);
            return Collections.emptySet();
        } finally {
            result.close();
        }
    }

    @Override
    public SearchResultMetadata search(
            @NotNull ResourceObjectDefinition objectDefinition,
            ObjectQuery query,
            @NotNull UcfObjectHandler handler,
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            @Nullable PagedSearchCapabilityType pagedSearchConfiguration,
            @Nullable SearchHierarchyConstraints searchHierarchyConstraints,
            @Nullable UcfFetchErrorReportingMethod ucfErrorReportingMethod,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SecurityViolationException, SchemaException,
            ObjectNotFoundException {

        // Result type for this operation
        final OperationResult result = parentResult.createSubresult(OP_SEARCH);
        result.addArbitraryObjectAsParam("objectClass", objectDefinition);
        result.addContext("connector", connectorBean);
        try {
            checkConfigured();

            if (pagedSearchConfiguration == null) {
                pagedSearchConfiguration = getCapability(PagedSearchCapabilityType.class);
            }

            var opCtx = createOperationContext(ctx);
            return new SearchExecutor(
                    objectDefinition, query, handler, shadowItemsToReturn,
                    pagedSearchConfiguration, searchHierarchyConstraints,
                    ucfErrorReportingMethod, opCtx, this)
                    .execute(result);

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown(); // or should we simply record success if unknown?
        }
    }

    private ConnectorOperationContext createOperationContext(@NotNull SchemaAwareUcfExecutionContext ctx) {
        return new ConnectorOperationContext(this, ctx);
    }

    @Override
    public int count(
            ResourceObjectDefinition objectDefinition,
            final ObjectQuery query,
            PagedSearchCapabilityType pagedSearchCapabilityType,
            UcfExecutionContext ctx,
            OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, UnsupportedOperationException {

        // Result type for this operation
        final OperationResult result = parentResult.createSubresult(OP_COUNT);
        result.addArbitraryObjectAsParam("objectClass", objectDefinition);
        result.addContext("connector", connectorBean);

        if (objectDefinition == null) {
            result.recordFatalError("Object class not defined");
            throw new IllegalArgumentException("objectClass not defined");
        }

        ObjectClass icfObjectClass = objectClassToConnId(objectDefinition);
        final boolean useConnectorPaging = pagedSearchCapabilityType != null;
        if (!useConnectorPaging) {
            throw new UnsupportedOperationException("ConnectorInstanceIcfImpl.count operation is supported only in combination with connector-implemented paging");
        }

        OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
        optionsBuilder.setAttributesToGet(Name.NAME);
        optionsBuilder.setPagedResultsOffset(1);
        optionsBuilder.setPageSize(1);
        if (pagedSearchCapabilityType.getDefaultSortField() != null) {
            String orderByIcfName = ConnIdNameMapper.ucfAttributeNameToConnId(pagedSearchCapabilityType.getDefaultSortField(), objectDefinition, "(default sorting field)");
            boolean isAscending = pagedSearchCapabilityType.getDefaultSortDirection() != OrderDirectionType.DESCENDING;
            optionsBuilder.setSortKeys(new SortKey(orderByIcfName, isAscending));
        }
        OperationOptions options = optionsBuilder.build();

        // Connector operation cannot create result for itself, so we need to
        // create result for it
        OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".search");
        icfResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);
        icfResult.addContext("connector", getConnIdConnectorFacadeRequired().getClass());

        int retval;

        InternalMonitor.recordConnectorOperation("search");
        ConnIdOperation operation = recordIcfOperationStart(ctx, ProvisioningOperation.ICF_SEARCH, objectDefinition);

        try {

            Filter filter = convertFilterToIcf(query, objectDefinition);
            final Holder<Integer> fetched = new Holder<>(0);

            ResultsHandler connIdHandler = new ResultsHandler() {
                @Override
                public boolean handle(ConnectorObject connectorObject) {
                    fetched.setValue(fetched.getValue()+1);         // actually, this should execute at most once
                    return false;
                }

                @Override
                public String toString() {
                    return "(midPoint counting result handler)";
                }
            };
            LOGGER.trace("Invoking ConnId search operation (to count objects): {}", operation);
            SearchResult searchResult = getConnIdConnectorFacadeRequired().search(icfObjectClass, filter, connIdHandler, options);
            recordIcfOperationEnd(ctx, operation, null);

            if (searchResult == null || searchResult.getRemainingPagedResults() == -1) {
                throw new UnsupportedOperationException("Connector does not seem to support paged searches or does not provide object count information");
            } else {
                retval = fetched.getValue() + searchResult.getRemainingPagedResults();
            }

            icfResult.recordSuccess();
        } catch (IntermediateSchemaException inEx) {
            recordIcfOperationEnd(ctx, operation, inEx);
            var ex = inEx.getSchemaException();
            icfResult.recordFatalError(ex);
            result.recordFatalError(ex);
            throw ex;
        } catch (UnsupportedOperationException uoe) {
            recordIcfOperationEnd(ctx, operation, uoe);
            icfResult.recordFatalError(uoe);
            result.recordFatalError(uoe);
            throw uoe;
        } catch (Throwable ex) {
            recordIcfOperationEnd(ctx, operation, ex);
            Throwable midpointEx = processConnIdException(ex, this, icfResult);
            result.computeStatus();
            // Do some kind of acrobatics to do proper throwing of checked
            // exception
            if (midpointEx instanceof CommunicationException communicationException) {
                throw communicationException;
            } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                throw genericFrameworkException;
            } else if (midpointEx instanceof SchemaException schemaException) {
                throw schemaException;
            } else if (midpointEx instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (midpointEx instanceof Error error) {
                throw error;
            } else {
                throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        if (result.isUnknown()) {
            result.recordSuccess();
        }

        return retval;
    }

    @NotNull ObjectClass objectClassToConnId(ResourceObjectDefinition objectDefinition) {
        return ConnIdNameMapper.ucfObjectClassNameToConnId(objectDefinition, isLegacySchema());
    }

    Filter convertFilterToIcf(ObjectQuery query, ResourceObjectDefinition objectDefinition) throws SchemaException {
        ObjectFilter prismFilter = query != null ? query.getFilter() : null;
        if (prismFilter != null) {
            LOGGER.trace("Start to convert filter: {}", prismFilter.debugDumpLazily());
            FilterInterpreter interpreter = new FilterInterpreter(objectDefinition);
            Filter connIdFilter = interpreter.interpret(prismFilter);
            LOGGER.trace("ConnId filter: {}", lazy(() -> ConnIdUtil.dump(connIdFilter)));
            return connIdFilter;
        } else {
            return null;
        }
    }

    // UTILITY METHODS

    private @NotNull Uid getUid(ResourceObjectIdentification.WithPrimary identification) throws SchemaException {
        // We hope that the value is String. But it perhaps should be OK to use toString() method if it's not.
        String uidValue = identification.getPrimaryIdentifier().getStringOrigValue();
        String nameValue = getNameValue(identification);
        if (nameValue == null) {
            return new Uid(uidValue);
        } else {
            return new Uid(uidValue, new Name(nameValue));
        }
    }

    private String getNameValue(ResourceObjectIdentification<?> identification) throws SchemaException {
        var secondaryIdentifiers = identification.getSecondaryIdentifiers();
        if (secondaryIdentifiers.size() == 1) {
            return secondaryIdentifiers.iterator().next().getStringOrigValue();
        } else if (secondaryIdentifiers.size() > 1) {
            for (var secondaryIdentifier : secondaryIdentifiers) {
                if (Name.NAME.equals(secondaryIdentifier.getDefinition().getFrameworkAttributeName())) {
                    return secondaryIdentifier.getStringOrigValue();
                }
            }
            throw new SchemaException(
                    "More than one secondary identifier in " + identification + ", cannot determine ConnId __NAME__");
        } else {
            assert secondaryIdentifiers.isEmpty();
            assert identification.hasPrimaryIdentifier();
            return null;
        }
    }

    private ShadowSimpleAttributeDefinition<?> getNameDefinition(ResourceObjectIdentification.WithPrimary identification)
            throws SchemaException {
        ResourceObjectDefinition objDef = identification.getResourceObjectDefinition();
        var namingAttributeDef = objDef.getNamingAttribute();
        if (namingAttributeDef != null) {
            return namingAttributeDef;
        }
        var icfsNameDef = objDef.findSimpleAttributeDefinition(SchemaConstants.ICFS_NAME);
        if (icfsNameDef != null) {
            return icfsNameDef;
        }
        throw new SchemaException("No naming attribute definition for " + identification);
    }

    private <T> ShadowSimpleAttributeDefinition<T> getUidDefinition(ResourceObjectIdentification.WithPrimary identification) {
        //noinspection unchecked
        return (ShadowSimpleAttributeDefinition<T>) identification.getPrimaryIdentifierAttribute().getDefinition();
    }

    @Override
    public Object executeScript(ExecuteProvisioningScriptOperation scriptOperation, UcfExecutionContext ctx, OperationResult parentResult) throws CommunicationException, GenericFrameworkException {

        UcfExecutionContext.checkExecutionFullyPersistent(ctx);

        OperationResult result = parentResult.createSubresult(OP_EXECUTE_SCRIPT);
        try {
            return executeScriptIcf(ctx, scriptOperation, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private Object executeScriptIcf(UcfExecutionContext reporter, ExecuteProvisioningScriptOperation scriptOperation, OperationResult parentResult) throws CommunicationException, GenericFrameworkException {

        String icfOpName;
        if (scriptOperation.isConnectorHost()) {
            icfOpName = "runScriptOnConnector";
        } else if (scriptOperation.isResourceHost()) {
            icfOpName = "runScriptOnResource";
        } else {
            throw new IllegalArgumentException("Where to execute the script?");
        }

        // convert execute script operation to the script context required from
        // the connector
        ScriptContext scriptContext = convertToScriptContext(scriptOperation);

        OperationResult icfResult = parentResult.createSubresult(ConnectorFacade.class.getName() + "." + icfOpName);
        icfResult.addContext("connector", getConnIdConnectorFacadeRequired().getClass());

        ConnIdOperation operation = recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SCRIPT, null);

        Object output = null;

        try {

            LOGGER.trace("Running script ({}): {}", icfOpName, operation);

            if (scriptOperation.isConnectorHost()) {
                InternalMonitor.recordConnectorOperation("runScriptOnConnector");
                output = getConnIdConnectorFacadeRequired()
                        .runScriptOnConnector(scriptContext, new OperationOptionsBuilder().build());
            } else if (scriptOperation.isResourceHost()) {
                InternalMonitor.recordConnectorOperation("runScriptOnResource");
                output = getConnIdConnectorFacadeRequired()
                        .runScriptOnResource(scriptContext, new OperationOptionsBuilder().build());
            }
            recordIcfOperationEnd(reporter, operation, null);

            icfResult.recordSuccess();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Finished running script ({}), script result: {}", icfOpName, PrettyPrinter.prettyPrint(output));
            }

        } catch (Throwable ex) {

            recordIcfOperationEnd(reporter, operation, ex);

            LOGGER.debug("Finished running script ({}), ERROR: {}", icfOpName, ex.getMessage());

            Throwable midpointEx = processConnIdException(ex, this, icfResult);

            CriticalityType criticality = scriptOperation.getCriticality();
            if (criticality == null || criticality == CriticalityType.FATAL) {
                parentResult.computeStatus();
                // Do some kind of acrobatics to do proper throwing of checked
                // exception
                if (midpointEx instanceof CommunicationException communicationException) {
                    throw communicationException;
                } else if (midpointEx instanceof GenericFrameworkException genericFrameworkException) {
                    throw genericFrameworkException;
                } else if (midpointEx instanceof SchemaException) {
                    // Schema exception during delete? It must be a missing UID
                    throw new IllegalArgumentException(midpointEx.getMessage(), midpointEx);
                } else if (midpointEx instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else if (midpointEx instanceof Error error) {
                    throw error;
                } else {
                    throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }

            } else if (criticality == CriticalityType.PARTIAL) {
                icfResult.setStatus(OperationResultStatus.PARTIAL_ERROR);
                parentResult.computeStatus();
            }
        }

        return output;
    }

    private ScriptContext convertToScriptContext(ExecuteProvisioningScriptOperation executeOp) {
        // creating script arguments map form the execute script operation
        // arguments
        Map<String, Object> scriptArguments = new HashMap<>();
        for (ExecuteScriptArgument argument : executeOp.getArgument()) {
            scriptArguments.put(argument.getArgumentName(), argument.getArgumentValue());
        }
        return new ScriptContext(executeOp.getLanguage(), executeOp.getTextCode(), scriptArguments);
    }

    @Override
    public String toString() {
        return "ConnectorInstanceIcfImpl(" + connectorBean + ")";
    }

    @Override
    public String getHumanReadableDescription() {
        return getOrig(connectorBean.getName()); // e.g. "ConnId com.evolveum.icf.dummy.connector.DummyConnector v2.0"
    }

    public String getHumanReadableName() {
        return instanceName + ": " + description;
    }

    @Override
    public void dispose() {
        if (connIdConnectorFacade != null) {
            LOGGER.debug("Disposing ConnId ConnectorFacade for instance: {} (dispose explicitly invoked on ConnectorInstance)", instanceName);
            connIdConnectorFacade.dispose();
            connIdConnectorFacade = null;
        }
    }

    @Contract("!null, _, _, _ -> !null; null, _, _, _ -> null")
    private @Nullable ConnIdOperation recordIcfOperationStart(
            UcfExecutionContext reporter, ProvisioningOperation operation, ResourceObjectDefinition objectDefinition, Uid uid) {
        if (reporter != null) {
            return reporter.recordIcfOperationStart(operation, objectDefinition, uid != null ? uid.getUidValue() : null);
        } else {
            LOGGER.warn("Couldn't record ConnId operation start as reporter is null.");
            return null;
        }
    }

    ConnIdOperation recordIcfOperationStart(
            UcfExecutionContext reporter, ProvisioningOperation operation, ResourceObjectDefinition objectDefinition) {
        return recordIcfOperationStart(reporter, operation, objectDefinition, null);
    }

    void recordIcfOperationResume(UcfExecutionContext reporter, ConnIdOperation operation) {
        if (reporter != null) {
            reporter.recordIcfOperationResume(operation);
        } else {
            LOGGER.warn("Couldn't record ConnId operation resume as reporter is null.");
        }
    }

    void recordIcfOperationSuspend(UcfExecutionContext reporter, ConnIdOperation operation) {
        if (reporter != null) {
            reporter.recordIcfOperationSuspend(operation);
        } else {
            LOGGER.warn("Couldn't record ConnId operation suspension as reporter is null.");
        }
    }

    void recordIcfOperationEnd(UcfExecutionContext reporter, ConnIdOperation operation, Throwable ex) {
        if (reporter != null) {
            reporter.recordIcfOperationEnd(operation, ex);
        } else {
            LOGGER.warn("Couldn't record ConnId operation end as reporter is null.");
        }
    }

    private OperationOptions createConnIdOptions(ConnectorOperationOptions options, Collection<Operation> changes)
            throws SchemaException {
        OperationOptionsBuilder connIdOptionsBuilder = new OperationOptionsBuilder();
        if (options != null) {
            ResourceObjectIdentification<?> runAsIdentification = options.getRunAsIdentification();
            if (runAsIdentification != null) {
                connIdOptionsBuilder.setRunAsUser(getRunAsNameValue(runAsIdentification));
                // We are going to figure out what the runAsPassword may be.
                // If there is a password change then there should be old value in the delta.
                // This is quite a black magic. But we do not have a better way now.
                for (Operation change : changes) {
                    if (change instanceof PropertyModificationOperation<?> propertyModificationOperation) {
                        PropertyDelta<?> propertyDelta = propertyModificationOperation.getPropertyDelta();
                        if (!propertyDelta.getPath().equivalent(SchemaConstants.PATH_PASSWORD_VALUE)) {
                            continue;
                        }
                        Collection<? extends PrismValue> oldValues = propertyDelta.getEstimatedOldValues();
                        if (oldValues == null || oldValues.isEmpty()) {
                            continue;
                        }
                        //noinspection unchecked
                        ProtectedStringType oldPassword =
                                ((PrismPropertyValue<ProtectedStringType>) (oldValues.iterator().next())).getValue();
                        if (oldPassword != null) {
                            var oldPasswordGs = ConnIdUtil.toGuardedString(oldPassword, "runAs password", b.protector);
                            connIdOptionsBuilder.setRunWithPassword(oldPasswordGs);
                        }
                    }
                }
            }
        }
        return connIdOptionsBuilder.build();
    }

    private String getRunAsNameValue(ResourceObjectIdentification<?> identification) throws SchemaException {
        var nameValue = getNameValue(identification);
        if (nameValue != null) {
            return nameValue;
        } else {
            ResourceObjectIdentifier<?> primaryIdentifier = identification.getPrimaryIdentifier();
            assert primaryIdentifier != null;
            return primaryIdentifier.getStringOrigValue();
        }
    }

    @Override
    public Boolean getConfiguredLegacySchema() {
        return configuredLegacySchema;
    }

    public boolean isLegacySchema() {
        //noinspection ReplaceNullCheck
        if (configuredLegacySchema != null) {
            return configuredLegacySchema;
        } else {
            return BooleanUtils.isNotFalse(nativeCapabilitiesAndSchema.legacySchema());
        }
    }

    public CapabilityCollectionType getCapabilities() {
        return nativeCapabilitiesAndSchema.capabilities();
    }
}
