/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.schema.result.OperationResult.HANDLE_OBJECT_FOUND;

import static java.util.Collections.emptySet;
import static org.apache.commons.collections4.SetUtils.emptyIfNull;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdUtil.processConnIdException;
import static com.evolveum.midpoint.schema.reporting.ConnIdOperation.getIdentifier;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.ObjectFilter;

import com.evolveum.midpoint.schema.reporting.ConnIdOperation;

import com.evolveum.midpoint.provisioning.ucf.api.UcfExecutionContext;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.identityconnectors.common.pooling.ObjectPoolConfiguration;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
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

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.query.FilterInterpreter;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.schema.util.ActivationUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.Nullable;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * Implementation of ConnectorInstance for ConnId connectors.
 * <p/>
 * This class implements the ConnectorInstance interface. The methods are
 * converting the data from the "midPoint semantics" as seen by the
 * ConnectorInstance interface to the "ConnId semantics" as seen by the ConnId
 * framework.
 *
 * @author Radovan Semancik
 */
@VisibleForTesting // Used when testing operation results & performance recording
public class ConnectorInstanceConnIdImpl implements ConnectorInstance {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorInstanceConnIdImpl.class);

    @VisibleForTesting
    public static final String FACADE_OP_GET_OBJECT = ConnectorFacade.class.getName() + ".getObject";
    private static final String FACADE_OP_CREATE = ConnectorFacade.class.getName() + ".create";
    private static final String FACADE_OP_UPDATE_DELTA = ConnectorFacade.class.getName() + ".updateDelta";
    private static final String FACADE_OP_ADD_ATTRIBUTE_VALUES = ConnectorFacade.class.getName() + ".addAttributeValues";
    private static final String FACADE_OP_UPDATE = ConnectorFacade.class.getName() + ".update";
    private static final String FACADE_OP_REMOVE_ATTRIBUTE_VALUES = ConnectorFacade.class.getName() + ".removeAttributeValues";
    private static final String FACADE_OP_DELETE = ConnectorFacade.class.getName() + ".delete";
    private static final String FACADE_OP_SYNC = ConnectorFacade.class.getName() + ".sync";
    @VisibleForTesting
    public static final String FACADE_OP_SEARCH = ConnectorFacade.class.getName() + ".search";
    @VisibleForTesting
    public static final String OP_HANDLE_OBJECT_FOUND = ConnectorInstanceConnIdImpl.class.getName() + "." + HANDLE_OBJECT_FOUND;
    private static final String FACADE_OP_RUN_SCRIPT_ON_CONNECTOR = ConnectorFacade.class.getName() + ".runScriptOnConnector";
    private static final String FACADE_OP_RUN_SCRIPT_ON_RESOURCE = ConnectorFacade.class.getName() + ".runScriptOnResource";

    private final ConnectorInfo connectorInfo;
    private final ConnectorType connectorType;
    private ConnectorFacade connIdConnectorFacade;
    private final PrismSchema connectorSchema;
    private APIConfiguration apiConfig = null;

    private final Protector protector;
    final ConnIdNameMapper connIdNameMapper;
    final ConnIdConvertor connIdConvertor;

    /** If not null and not empty, specifies what object classes should be put into schema (empty means "no limitations"). */
    @Nullable private List<QName> generateObjectClasses;

    /**
     * Builder and holder object for parsed ConnId schema and capabilities. By using
     * this class the ConnectorInstance always has a consistent schema, even during
     * reconfigure and fetch operations.
     * There is either old schema or new schema, but there is no partially-parsed schema.
     */
    private ConnIdCapabilitiesAndSchemaParser parsedCapabilitiesAndSchema;

    /**
     * The resource schema here is raw.
     */
    private ResourceSchema rawResourceSchema = null;
    private CapabilityCollectionType capabilities = null;
    private Boolean legacySchema = null;

    private String description;
    private String instanceName; // resource name
    private boolean caseIgnoreAttributeNames = false;

    ConnectorInstanceConnIdImpl(ConnectorInfo connectorInfo, ConnectorType connectorType,
            PrismSchema connectorSchema, Protector protector, LocalizationService localizationService) {
        this.connectorInfo = connectorInfo;
        this.connectorType = connectorType;
        this.connectorSchema = connectorSchema;
        this.protector = protector;
        connIdNameMapper = new ConnIdNameMapper();
        connIdConvertor = new ConnIdConvertor(protector, localizationService, connIdNameMapper);
    }

    /**
     * Complex description for development diagnostics, e.g. ConnectorSpec(resource OID....)
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Simple instance name for system administrator (name of the resource)
     */
    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setRawResourceSchema(ResourceSchema rawResourceSchema) {
        this.rawResourceSchema = rawResourceSchema;
        connIdNameMapper.setResourceSchema(rawResourceSchema);
    }

    public void resetResourceSchema() {
        setRawResourceSchema(null);
    }

    @Override
    public synchronized void configure(
            @NotNull PrismContainerValue<?> configurationOriginal,
            @Nullable ConnectorConfigurationOptions options,
            @NotNull OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_CONFIGURE);

        LOGGER.trace("Configuring connector {}, provided configuration:\n{}", connectorType, configurationOriginal.debugDumpLazily(1));

        try {
            generateObjectClasses = options != null ? options.getGenerateObjectClasses() : null;
            // Get default configuration for the connector. This is important,
            // as it contains types of connector configuration properties.

            // Make sure that the proper configuration schema is applied. This
            // will cause that all the "raw" elements are parsed
            PrismContainerValue<?> configurationCloned = configurationOriginal.clone();
            configurationCloned.applyDefinition(getConfigurationContainerDefinition());

            ConnIdConfigurationTransformer configTransformer =
                    new ConnIdConfigurationTransformer(connectorType, connectorInfo, protector, options);
            // Transform XML configuration from the resource to the ConnId connector configuration
            try {
                apiConfig = configTransformer.transformConnectorConfiguration(configurationCloned);
            } catch (SchemaException e) {
                result.recordFatalError(e.getMessage(), e);
                throw e;
            }

            logTransformedConfiguration();

            apiConfig.setInstanceName(getInstanceName());

            ConnectorFacade oldConnIdConnectorFacade = connIdConnectorFacade;

            // Create new connector instance using the transformed configuration
            connIdConnectorFacade = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

            if (oldConnIdConnectorFacade != null) {
                // Make sure old connector instance is disposed. We do not want to waste resources.
                // In case that old and new facade are the same, this will cause all existing
                // ConnId connector instances to dispose (i.e. connector pool is emptied).
                // But this is exactly what we want on reconfigure. We want the connections to
                // be closed and re-opened.
                LOGGER.debug("Disposing old ConnId ConnectorFacade for instance: {} (connector reconfiguration)", instanceName);
                oldConnIdConnectorFacade.dispose();
            }

            PrismProperty<Boolean> legacySchemaConfigProperty = configurationCloned.findProperty(new ItemName(
                    SchemaConstants.NS_ICF_CONFIGURATION,
                    ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_LEGACY_SCHEMA_XML_ELEMENT_NAME));
            if (legacySchemaConfigProperty != null) {
                legacySchema = legacySchemaConfigProperty.getRealValue();
            }
            LOGGER.trace("Legacy schema (config): {}", legacySchema);

        } catch (Throwable ex) {
            Throwable midpointEx = processConnIdException(ex, this, result);
            result.computeStatus("Configuration operation failed");
            // Do some kind of acrobatics to do proper throwing of checked
            // exception
            if (midpointEx instanceof CommunicationException) {
                throw (CommunicationException) midpointEx;
            } else if (midpointEx instanceof GenericFrameworkException) {
                throw (GenericFrameworkException) midpointEx;
            } else if (midpointEx instanceof SchemaException) {
                throw (SchemaException) midpointEx;
            } else if (midpointEx instanceof ConfigurationException) {
                throw (ConfigurationException) midpointEx;
            } else if (midpointEx instanceof RuntimeException) {
                throw (RuntimeException) midpointEx;
            } else if (midpointEx instanceof Error) {
                throw (Error) midpointEx;
            } else {
                throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        } finally {
            result.close();
        }
    }

    private void logTransformedConfiguration() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Configuring connector {}, transformed configuration:", connectorType);
            for (String propName : apiConfig.getConfigurationProperties().getPropertyNames()) {
                LOGGER.trace("P: {} = {}", propName, apiConfig.getConfigurationProperties().getProperty(propName).getValue());
            }
        }
    }

    private PrismContainerDefinition<?> getConfigurationContainerDefinition() throws SchemaException {
        QName configContainerQName = new QName(connectorType.getNamespace(),
                ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
        PrismContainerDefinition<?> configContainerDef = connectorSchema
                .findContainerDefinitionByElementName(configContainerQName);
        if (configContainerDef == null) {
            throw new SchemaException("No definition of container " + configContainerQName
                    + " in configuration schema for connector " + this);
        }
        return configContainerDef;
    }

    @Override
    public ConnectorOperationalStatus getOperationalStatus() throws ObjectNotFoundException {

        if (!(connectorInfo instanceof LocalConnectorInfoImpl)) {
            LOGGER.trace("Cannot get operational status of a remote connector {}", connectorType);
            return null;
        }

        if (apiConfig == null) {
            LOGGER.trace("Cannot get operational status of a connector {}: connector not yet configured", connectorType);
            throw new IllegalStateException("Connector "+connectorType+" not yet configured");
        }

        ConnectorOperationalStatus status = new ConnectorOperationalStatus();

        ConnectorOperationalContext connectorOperationalContext = new ConnectorOperationalContext((LocalConnectorInfoImpl) connectorInfo, (APIConfigurationImpl) apiConfig);

        Class<? extends Connector> connectorClass = connectorOperationalContext.getConnectorClass();
        if (connectorClass != null) {
            status.setConnectorClassName(connectorClass.getName());
        }

        ObjectPoolConfiguration poolConfiguration = apiConfig.getConnectorPoolConfiguration();
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

    private void validateConnectorFacade() {
        if (connIdConnectorFacade == null) {
            throw new IllegalStateException("Attempt to use unconfigured connector " + connectorType);
        }
    }

    @Override
    public void initialize(ResourceSchema resourceSchema, CapabilityCollectionType capabilities, boolean caseIgnoreAttributeNames, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException {

        // Result type for this operation
        OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_INITIALIZE);
        result.addContext("connector", connectorType);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ConnectorFactoryConnIdImpl.class);
        try {
            validateConnectorFacade();

            updateSchema(resourceSchema);
            this.capabilities = capabilities;
            this.caseIgnoreAttributeNames = caseIgnoreAttributeNames;

            if (resourceSchema == null || capabilities == null) {
                retrieveAndParseResourceCapabilitiesAndSchema(result);

                this.legacySchema = parsedCapabilitiesAndSchema.getLegacySchema();
                setRawResourceSchema(parsedCapabilitiesAndSchema.getRawResourceSchema());
                this.capabilities = parsedCapabilitiesAndSchema.getCapabilities();
            }
        } catch (Throwable t) {
            // Note that even communication exception while retrieving the schema is in fact fatal, because
            // there is no schema. Not even the pre-cached one. The connector will not be able to work.
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    public void updateSchema(ResourceSchema resourceSchema) {
        setRawResourceSchema(resourceSchema);

        if (resourceSchema != null && legacySchema == null) {
            legacySchema = detectLegacySchema(resourceSchema);
        }
    }

    private boolean detectLegacySchema(ResourceSchema resourceSchema) {
        ComplexTypeDefinition accountObjectClass =
                resourceSchema.findComplexTypeDefinitionByType(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS);
        return accountObjectClass != null;
    }

    @Override
    public synchronized ResourceSchema fetchResourceSchema(OperationResult parentResult) throws CommunicationException,
            GenericFrameworkException, ConfigurationException, SchemaException {

        // Result type for this operation
        OperationResult result = parentResult.createSubresult(OP_FETCH_RESOURCE_SCHEMA);
        result.addContext("connector", connectorType);

        try {

            if (parsedCapabilitiesAndSchema == null) {
                retrieveAndParseResourceCapabilitiesAndSchema(result);
            }

            legacySchema = parsedCapabilitiesAndSchema.getLegacySchema();
            setRawResourceSchema(parsedCapabilitiesAndSchema.getRawResourceSchema());

            if (rawResourceSchema == null) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Connector does not support schema");
            } else {
                result.recordSuccess();
            }

            return rawResourceSchema;

        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        } finally {
            result.close();
        }
    }

    @Override
    public synchronized CapabilityCollectionType fetchCapabilities(OperationResult parentResult)
        throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException {

        // Result type for this operation
        OperationResult result = parentResult.createMinorSubresult(OP_FETCH_CAPABILITIES);
        result.addContext("connector", connectorType);

        try {

            // Always refresh capabilities and schema here, even if we have already parsed that before.
            // This method is used in "test connection". We want to force fresh fetch here
            // TODO: later: clean up this mess. Make fresh fetch somehow explicit?
            retrieveAndParseResourceCapabilitiesAndSchema(result);

            legacySchema = parsedCapabilitiesAndSchema.getLegacySchema();
            setRawResourceSchema(parsedCapabilitiesAndSchema.getRawResourceSchema());
            capabilities = parsedCapabilitiesAndSchema.getCapabilities();

        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        }

        result.recordSuccess();

        return capabilities;
    }

    private void retrieveAndParseResourceCapabilitiesAndSchema(OperationResult result)
            throws CommunicationException, ConfigurationException, GenericFrameworkException, SchemaException {

        ConnIdCapabilitiesAndSchemaParser parser =
                new ConnIdCapabilitiesAndSchemaParser(
                        connIdNameMapper,
                        connIdConnectorFacade,
                        getHumanReadableName());
        parser.setLegacySchema(legacySchema);

        LOGGER.debug("Retrieving and parsing schema and capabilities for {}", getHumanReadableName());
        parser.retrieveResourceCapabilitiesAndSchema(generateObjectClasses, result);

        parsedCapabilitiesAndSchema = parser;
    }

     @SuppressWarnings("SameParameterValue")
     private synchronized <C extends CapabilityType> C getCapability(Class<C> capClass) {
         return CapabilityUtil.getCapability(capabilities, capClass);
    }

    @Override
    public PrismObject<ShadowType> fetchObject(ResourceObjectIdentification resourceObjectIdentification,
            AttributesToReturn attributesToReturn, UcfExecutionContext ctx, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, SecurityViolationException, ConfigurationException {

        Validate.notNull(resourceObjectIdentification, "Null primary identifiers");
        ResourceObjectDefinition objectDefinition = resourceObjectIdentification.getResourceObjectDefinition();

        OperationResult result = parentResult.createMinorSubresult(OP_FETCH_OBJECT);
        result.addArbitraryObjectAsParam("resourceObjectDefinition", objectDefinition);
        result.addArbitraryObjectAsParam("identification", resourceObjectIdentification);
        result.addContext("connector", connectorType);
        try {
            if (connIdConnectorFacade == null) {
                throw new IllegalStateException("Attempt to use unconfigured connector " + connectorType + " " + description);
            }

            // Get UID from the set of identifiers
            Uid uid = getUid(resourceObjectIdentification);
            if (uid == null) {
                throw new IllegalArgumentException(
                        "Required attribute UID not found in identification set while attempting to fetch object identified by "
                                + resourceObjectIdentification + " from " + description);
            }

            ObjectClass icfObjectClass = objectClassToConnId(objectDefinition);

            OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
            convertToIcfAttrsToGet(objectDefinition, attributesToReturn, optionsBuilder);
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

            PrismObjectDefinition<ShadowType> shadowDefinition = toShadowDefinition(objectDefinition);
            // TODO configure error reporting method
            PrismObject<ShadowType> shadow = connIdConvertor
                    .convertToUcfObject(co, shadowDefinition, false, caseIgnoreAttributeNames,
                            legacySchema, UcfFetchErrorReportingMethod.EXCEPTION, result)
                    .getResourceObject();

            result.recordSuccess();
            return shadow;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @NotNull
    static PrismObjectDefinition<ShadowType> toShadowDefinition(ResourceObjectDefinition resourceObjectDefinition) {
        return resourceObjectDefinition
                .toResourceAttributeContainerDefinition(ShadowType.F_ATTRIBUTES)
                .toShadowDefinition();
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
        icfResult.addContext("connector", connIdConnectorFacade.getClass());

        InternalMonitor.recordConnectorOperation("getObject");
        ConnIdOperation operation = recordIcfOperationStart(reporter, ProvisioningOperation.ICF_GET, objectDefinition, uid);

        LOGGER.trace("Fetching connector object ObjectClass={}, UID={}, operation id={}, options={}",
                icfObjectClass, uid, getIdentifier(operation), ConnIdUtil.dumpOptionsLazily(options));

        ConnectorObject co;
        try {
            // Invoke the ConnId connector
            co = connIdConnectorFacade.getObject(icfObjectClass, uid, options);
            recordIcfOperationEnd(reporter, operation, null);
            icfResult.recordSuccess();
        } catch (Throwable ex) {
            recordIcfOperationEnd(reporter, operation, ex);
            String desc = this.getHumanReadableName() + " while getting object identified by ConnId UID '"+uid.getUidValue()+"'";
            Throwable midpointEx = processConnIdException(ex, desc, icfResult);
            icfResult.computeStatus("Add object failed");

            // Do some kind of acrobatics to do proper throwing of checked
            // exception
            if (midpointEx instanceof CommunicationException) {
                icfResult.muteError();
                throw (CommunicationException) midpointEx;
            } else if (midpointEx instanceof GenericFrameworkException) {
                throw (GenericFrameworkException) midpointEx;
            } else if (midpointEx instanceof ConfigurationException) {
                throw (ConfigurationException) midpointEx;
            } else if (midpointEx instanceof SecurityViolationException) {
                throw (SecurityViolationException) midpointEx;
            } else if (midpointEx instanceof ObjectNotFoundException) {
                LOGGER.trace("Got ObjectNotFoundException while looking for resource object ConnId UID: {}", uid);
                return null;
            } else if (midpointEx instanceof RuntimeException) {
                throw (RuntimeException)midpointEx;
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
            AttributesToReturn attributesToReturn,
            OperationOptionsBuilder optionsBuilder) throws SchemaException {
        if (attributesToReturn == null) {
            return;
        }
        Collection<? extends ResourceAttributeDefinition> attrs = attributesToReturn.getAttributesToReturn();
        if (attributesToReturn.isReturnDefaultAttributes() && !attributesToReturn.isReturnPasswordExplicit()
                && (attrs == null || attrs.isEmpty())) {
            return;
        }
        List<String> icfAttrsToGet = new ArrayList<>();
        if (attributesToReturn.isReturnDefaultAttributes()) {
            if (supportsReturnDefaultAttributes()) {
                optionsBuilder.setReturnDefaultAttributes(true);
            } else {
                // Add all the attributes that are defined as "returned by default" by the schema
                for (ResourceAttributeDefinition attributeDef: resourceObjectDefinition.getAttributeDefinitions()) {
                    if (attributeDef.isReturnedByDefault()) {
                        String attrName = connIdNameMapper.convertAttributeNameToConnId(attributeDef);
                        icfAttrsToGet.add(attrName);
                    }
                }
            }
        }
        if (attributesToReturn.isReturnPasswordExplicit()
                || (attributesToReturn.isReturnDefaultAttributes() && passwordReturnedByDefault())) {
            icfAttrsToGet.add(OperationalAttributes.PASSWORD_NAME);
        }
        if (attributesToReturn.isReturnAdministrativeStatusExplicit()
                || (attributesToReturn.isReturnDefaultAttributes() && enabledReturnedByDefault())) {
            icfAttrsToGet.add(OperationalAttributes.ENABLE_NAME);
        }
        if (attributesToReturn.isReturnLockoutStatusExplicit()
                || (attributesToReturn.isReturnDefaultAttributes() && lockoutReturnedByDefault())) {
            icfAttrsToGet.add(OperationalAttributes.LOCK_OUT_NAME);
        }
        if (attributesToReturn.isReturnValidFromExplicit()
                || (attributesToReturn.isReturnDefaultAttributes() && validFromReturnedByDefault())) {
            icfAttrsToGet.add(OperationalAttributes.ENABLE_DATE_NAME);
        }
        if (attributesToReturn.isReturnValidToExplicit()
                || (attributesToReturn.isReturnDefaultAttributes() && validToReturnedByDefault())) {
            icfAttrsToGet.add(OperationalAttributes.DISABLE_DATE_NAME);
        }

        if (attrs != null) {
            for (ResourceAttributeDefinition attrDef: attrs) {
                String attrName = connIdNameMapper.convertAttributeNameToConnId(attrDef);
                if (!icfAttrsToGet.contains(attrName)) {
                    icfAttrsToGet.add(attrName);
                }
            }
        }
        // Log full list here. ConnId is shortening it and it cannot be seen in logs.
        LOGGER.trace("Converted attributes to return: {}\n to ConnId attibutesToGet: {}", attributesToReturn, icfAttrsToGet);
        optionsBuilder.setAttributesToGet(icfAttrsToGet);
    }

    private synchronized boolean supportsReturnDefaultAttributes() {
        ReadCapabilityType capability = CapabilityUtil.getCapability(capabilities, ReadCapabilityType.class);
        if (capability == null) {
            return false;
        }
        return Boolean.TRUE.equals(capability.isReturnDefaultAttributesOption());
    }

    private synchronized boolean passwordReturnedByDefault() {
        CredentialsCapabilityType capability = CapabilityUtil.getCapability(capabilities, CredentialsCapabilityType.class);
        return CapabilityUtil.isPasswordReturnedByDefault(capability);
    }

    private synchronized boolean enabledReturnedByDefault() {
        ActivationCapabilityType capability = CapabilityUtil.getCapability(capabilities, ActivationCapabilityType.class);
        return CapabilityUtil.isActivationStatusReturnedByDefault(capability);
    }

    private synchronized boolean lockoutReturnedByDefault() {
        ActivationCapabilityType capability = CapabilityUtil.getCapability(capabilities, ActivationCapabilityType.class);
        return CapabilityUtil.isActivationLockoutStatusReturnedByDefault(capability);
    }

    private synchronized boolean validFromReturnedByDefault() {
        ActivationCapabilityType capability = CapabilityUtil.getCapability(capabilities, ActivationCapabilityType.class);
        return CapabilityUtil.isActivationValidFromReturnedByDefault(capability);
    }

    private synchronized boolean validToReturnedByDefault() {
        ActivationCapabilityType capability = CapabilityUtil.getCapability(capabilities, ActivationCapabilityType.class);
        return CapabilityUtil.isActivationValidToReturnedByDefault(capability);
    }

    private synchronized boolean supportsDeltaUpdateOp() {
        UpdateCapabilityType capability = CapabilityUtil.getCapability(capabilities, UpdateCapabilityType.class);
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
    public AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> addObject(
            PrismObject<? extends ShadowType> shadow, UcfExecutionContext ctx, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException {
        UcfExecutionContext.checkExecutionFullyPersistent(ctx);
        validateShadow(shadow, "add", false);
        ShadowType shadowType = shadow.asObjectable();

        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);
        OperationResult result = parentResult.createSubresult(OP_ADD_OBJECT);
        result.addParam("resourceObject", shadow);

        ResourceObjectDefinition ocDef;
        ResourceAttributeContainerDefinition attrContDef = attributesContainer.getDefinition();
        if (attrContDef != null) {
            ocDef = attrContDef.getComplexTypeDefinition();
        } else {
            ocDef = rawResourceSchema.findObjectClassDefinition(shadow.asObjectable().getObjectClass());
            if (ocDef == null) {
                throw new SchemaException("Unknown object class "+shadow.asObjectable().getObjectClass());
            }
        }

        // getting icf object class from resource object class
        ObjectClass icfObjectClass = connIdNameMapper.objectClassToConnId(shadow, connectorType, BooleanUtils.isNotFalse(legacySchema));

        if (icfObjectClass == null) {
            result.recordFatalError("Couldn't get icf object class from " + shadow);
            throw new IllegalArgumentException("Couldn't get icf object class from " + shadow);
        }

        // setting ifc attributes from resource object attributes
        Set<Attribute> attributes;
        try {
            LOGGER.trace("midPoint object before conversion:\n{}", attributesContainer.debugDumpLazily());
            attributes = connIdConvertor.convertFromResourceObjectToConnIdAttributes(attributesContainer, ocDef);

            if (shadowType.getCredentials() != null && shadowType.getCredentials().getPassword() != null) {
                PasswordType password = shadowType.getCredentials().getPassword();
                ProtectedStringType protectedString = password.getValue();
                GuardedString guardedPassword = ConnIdUtil.toGuardedString(protectedString, "new password", protector);
                if (guardedPassword != null) {
                    attributes.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, guardedPassword));
                }
            }

            if (ActivationUtil.hasAdministrativeActivation(shadowType)){
                attributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, ActivationUtil.isAdministrativeEnabled(shadowType)));
            }

            if (ActivationUtil.hasValidFrom(shadowType)){
                attributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_DATE_NAME, XmlTypeConverter.toMillis(shadowType.getActivation().getValidFrom())));
            }

            if (ActivationUtil.hasValidTo(shadowType)){
                attributes.add(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME, XmlTypeConverter.toMillis(shadowType.getActivation().getValidTo())));
            }

            if (ActivationUtil.hasLockoutStatus(shadowType)){
                attributes.add(AttributeBuilder.build(OperationalAttributes.LOCK_OUT_NAME, ActivationUtil.isLockedOut(shadowType)));
            }

            LOGGER.trace("ConnId attributes after conversion:\n{}", lazy(() -> ConnIdUtil.dump(attributes)));

        } catch (SchemaException | RuntimeException ex) {
            result.recordFatalError(
                    "Error while converting resource object attributes. Reason: " + ex.getMessage(), ex);
            throw new SchemaException("Error while converting resource object attributes. Reason: "
                    + ex.getMessage(), ex);
        }

        List<String> icfAuxiliaryObjectClasses = new ArrayList<>();
        for (QName auxiliaryObjectClass: shadowType.getAuxiliaryObjectClass()) {
            icfAuxiliaryObjectClasses.add(
                    connIdNameMapper.objectClassToConnId(auxiliaryObjectClass,
                            connectorType, false).getObjectClassValue());
        }
        if (!icfAuxiliaryObjectClasses.isEmpty()) {
            AttributeBuilder ab = new AttributeBuilder();
            ab.setName(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME);
            ab.addValue(icfAuxiliaryObjectClasses);
            attributes.add(ab.build());
        }

        OperationOptionsBuilder operationOptionsBuilder = new OperationOptionsBuilder();
        OperationOptions options = operationOptionsBuilder.build();

        OperationResult connIdResult = result.createSubresult(FACADE_OP_CREATE);
        connIdResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);
        connIdResult.addArbitraryObjectCollectionAsParam("auxiliaryObjectClasses", icfAuxiliaryObjectClasses);
        connIdResult.addArbitraryObjectCollectionAsParam("attributes", attributes);
        connIdResult.addArbitraryObjectAsParam("options", options);
        connIdResult.addContext("connector", connIdConnectorFacade.getClass());

        // CALL THE ConnId FRAMEWORK
        InternalMonitor.recordConnectorOperation("create");
        InternalMonitor.recordConnectorModification("create");
        ConnIdOperation operation = recordIcfOperationStart(ctx, ProvisioningOperation.ICF_CREATE, ocDef, null);

        Uid uid;
        try {

            LOGGER.trace("Calling ConnId create for {}", operation);
            uid = connIdConnectorFacade.create(icfObjectClass, attributes, options);
            if (operation != null && uid != null) {
                operation.setUid(uid.getUidValue());
            }
            recordIcfOperationEnd(ctx, operation, null);

        } catch (Throwable ex) {
            recordIcfOperationEnd(ctx, operation, ex);
            Throwable midpointEx = processConnIdException(ex, this, connIdResult);
            result.computeStatus("Add object failed");

            // Do some kind of acrobatics to do proper throwing of checked
            // exception
            if (midpointEx instanceof ObjectAlreadyExistsException) {
                throw (ObjectAlreadyExistsException) midpointEx;
            } else if (midpointEx instanceof CommunicationException) {
//                icfResult.muteError();
//                result.muteError();
                throw (CommunicationException) midpointEx;
            } else if (midpointEx instanceof GenericFrameworkException) {
                throw (GenericFrameworkException) midpointEx;
            } else if (midpointEx instanceof SchemaException) {
                throw (SchemaException) midpointEx;
            } else if (midpointEx instanceof ConfigurationException) {
                throw (ConfigurationException) midpointEx;
            } else if (midpointEx instanceof SecurityViolationException) {
                throw (SecurityViolationException) midpointEx;
            } else if (midpointEx instanceof PolicyViolationException) {
                throw (PolicyViolationException) midpointEx;
            } else if (midpointEx instanceof RuntimeException) {
                throw (RuntimeException) midpointEx;
            } else if (midpointEx instanceof Error) {
                throw (Error) midpointEx;
            } else {
                throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        if (uid == null || uid.getUidValue() == null || uid.getUidValue().isEmpty()) {
            connIdResult.recordFatalError("ConnId did not returned UID after create");
            result.computeStatus("Add object failed");
            throw new GenericFrameworkException("ConnId did not returned UID after create");
        }

        Collection<ResourceAttribute<?>> identifiers =
                ConnIdUtil.convertToIdentifiers(
                        uid,
                        attributesContainer.getDefinition().getComplexTypeDefinition(),
                        rawResourceSchema);
        for (ResourceAttribute<?> identifier: identifiers) {
            attributesContainer.getValue().addReplaceExisting(identifier);
        }
        connIdResult.recordSuccess();

        result.computeStatus();
        return AsynchronousOperationReturnValue.wrap(attributesContainer.getAttributes(), result);
    }

    private void validateShadow(PrismObject<? extends ShadowType> shadow, String operation, boolean requireUid) {
        if (shadow == null) {
            throw new IllegalArgumentException("Cannot " + operation + " null " + shadow);
        }
        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null) {
            throw new IllegalArgumentException("Cannot " + operation + " shadow without attributes container");
        }
        ResourceAttributeContainer resourceAttributesContainer = ShadowUtil
                .getAttributesContainer(shadow);
        if (resourceAttributesContainer == null) {
            throw new IllegalArgumentException("Cannot " + operation
                    + " shadow without attributes container of type ResourceAttributeContainer, got "
                    + attributesContainer.getClass());
        }
        if (requireUid) {
            Collection<ResourceAttribute<?>> identifiers = resourceAttributesContainer.getPrimaryIdentifiers();
            if (identifiers == null || identifiers.isEmpty()) {
                throw new IllegalArgumentException("Cannot " + operation + " shadow without identifiers");
            }
        }
    }

    // TODO [med] beware, this method does not obey its contract specified in the interface
    // (1) currently it does not return all the changes, only the 'side effect' changes
    // (2) it throws exceptions even if some of the changes were made
    // (3) among identifiers, only the UID value is updated on object rename
    //     (other identifiers are ignored on input and output of this method)

    @Override
    public AsynchronousOperationReturnValue<Collection<PropertyModificationOperation<?>>> modifyObject(
            ResourceObjectIdentification identification,
            PrismObject<ShadowType> shadow,
            @NotNull Collection<Operation> changes,
            ConnectorOperationOptions options,
            UcfExecutionContext ctx, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException,
            GenericFrameworkException, SchemaException, SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException {

        OperationResult result = parentResult.createSubresult(OP_MODIFY_OBJECT);
        result.addArbitraryObjectAsParam("identification", identification);
        result.addArbitraryObjectCollectionAsParam("changes", changes);
        result.addArbitraryObjectAsParam("options", options);

        if (changes.isEmpty()) {
            LOGGER.info("No modifications for connector object specified. Skipping processing.");
            result.recordNotApplicableIfUnknown();
            return AsynchronousOperationReturnValue.wrap(new ArrayList<>(0), result);
        }

        UcfExecutionContext.checkExecutionFullyPersistent(ctx);

        ObjectClass objClass = objectClassToConnId(identification.getResourceObjectDefinition());

        Uid uid;
        try {
            uid = getUid(identification);
        } catch (SchemaException e) {
            result.recordFatalError(e);
            throw e;
        }

        if (uid == null) {
            result.recordFatalError("Cannot determine UID from identification: " + identification);
            throw new IllegalArgumentException("Cannot determine UID from identification: " + identification);
        }

        if (supportsDeltaUpdateOp()) {
            return modifyObjectDelta(identification, objClass, uid, shadow, changes, options, ctx, result);
        } else {
            return modifyObjectUpdate(identification, objClass, uid, shadow, changes, options, ctx, result);
        }
    }

    /**
     * Modifies object by using new delta update operations.
     */
    private AsynchronousOperationReturnValue<Collection<PropertyModificationOperation<?>>> modifyObjectDelta(
            ResourceObjectIdentification identification,
            ObjectClass objClass,
            Uid uid,
            PrismObject<ShadowType> shadow,
            Collection<Operation> changes,
            ConnectorOperationOptions options,
            UcfExecutionContext reporter,
            OperationResult result)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException {

        ResourceObjectDefinition objectClassDef = identification.getResourceObjectDefinition();

        DeltaModificationConverter converter = new DeltaModificationConverter();
        converter.setChanges(changes);
        converter.setConnectorDescription(description);
        converter.setConnectorType(connectorType);
        converter.setConnIdNameMapper(connIdNameMapper);
        converter.setObjectDefinition(objectClassDef);
        converter.setProtector(protector);
        converter.setResourceSchema(rawResourceSchema);
        converter.setOptions(options);

        try {

            converter.convert();

        } catch (SchemaException | RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        }

        LOGGER.trace("converted attributesDelta:\n {}", converter.debugDumpLazily(1));

        OperationResult connIdResult;

        @NotNull Set<AttributeDelta> knownExecutedChanges; // May or may not cover all executed changes
        Set<AttributeDelta> attributesDelta = converter.getAttributesDelta();
        if (!attributesDelta.isEmpty()) {
            OperationOptions connIdOptions = createConnIdOptions(options, changes);
            connIdResult = result.createSubresult(FACADE_OP_UPDATE_DELTA);
            connIdResult.addParam("objectClass", objectClassDef.toString());
            connIdResult.addParam("uid", uid.getUidValue());
            connIdResult.addParam("attributesDelta", attributesDelta.toString());
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", connIdConnectorFacade.getClass());

            InternalMonitor.recordConnectorOperation("update");
            InternalMonitor.recordConnectorModification("update");
            ConnIdOperation operation = recordIcfOperationStart(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);

            LOGGER.trace("Invoking ICF update(), objectclass={}, uid={}, operation id={}, attributes delta: {}",
                    objClass, uid, getIdentifier(operation), lazy(() -> dumpAttributesDelta(attributesDelta)));
            try {
                knownExecutedChanges =
                        emptyIfNull(connIdConnectorFacade.updateDelta(objClass, uid, attributesDelta, connIdOptions));

                recordIcfOperationEnd(reporter, operation, null);
                connIdResult.recordSuccess();
            } catch (Throwable ex) {
                recordIcfOperationEnd(reporter, operation, ex);
                String desc = this.getHumanReadableName() + " while updating object identified by ConnId UID '"+uid.getUidValue()+"'";
                Throwable midpointEx = processConnIdException(ex, desc, connIdResult);
                result.computeStatus("Update failed");
                // Do some kind of acrobatics to do proper throwing of checked
                // exception
                if (midpointEx instanceof ObjectNotFoundException) {
                    throw (ObjectNotFoundException) midpointEx;
                } else if (midpointEx instanceof CommunicationException) {
                    //in this situation this is not a critical error, becasue we know to handle it..so mute the error and sign it as expected
                    result.muteError();
                    connIdResult.muteError();
                    throw (CommunicationException) midpointEx;
                } else if (midpointEx instanceof GenericFrameworkException) {
                    throw (GenericFrameworkException) midpointEx;
                } else if (midpointEx instanceof SchemaException) {
                    throw (SchemaException) midpointEx;
                } else if (midpointEx instanceof ObjectAlreadyExistsException) {
                    throw (ObjectAlreadyExistsException) midpointEx;
                } else if (midpointEx instanceof RuntimeException) {
                    throw (RuntimeException) midpointEx;
                } else if (midpointEx instanceof SecurityViolationException) {
                    throw (SecurityViolationException) midpointEx;
                } else if (midpointEx instanceof PolicyViolationException) {
                    throw (PolicyViolationException) midpointEx;
                } else if (midpointEx instanceof Error) {
                    throw (Error) midpointEx;
                } else {
                    throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        } else {
            knownExecutedChanges = emptySet();
        }
        result.computeStatus();

        Collection<PropertyModificationOperation<?>> knownExecutedOperations =
                convertToExecutedOperations(knownExecutedChanges, identification, objectClassDef);
        return AsynchronousOperationReturnValue.wrap(knownExecutedOperations, result);
    }

    private Collection<PropertyModificationOperation<?>> convertToExecutedOperations(Set<AttributeDelta> knownExecutedChanges,
            ResourceObjectIdentification identification, ResourceObjectDefinition objectClassDef) throws SchemaException {
        Collection<PropertyModificationOperation<?>> knownExecutedOperations = new ArrayList<>();
        for (AttributeDelta executedDelta : knownExecutedChanges) {
            String name = executedDelta.getName();
            if (name.equals(Uid.NAME)) {
                Uid newUid = new Uid((String)executedDelta.getValuesToReplace().get(0));
                PropertyDelta<String> uidDelta = createUidDelta(newUid, getUidDefinition(identification));
                PropertyModificationOperation<?> uidMod = new PropertyModificationOperation<>(uidDelta);
                knownExecutedOperations.add(uidMod);

                replaceUidValue(identification, newUid); // TODO why we are doing this? Do we do that for the caller?
            } else if (name.equals(Name.NAME)) {
                Name newName = new Name((String)executedDelta.getValuesToReplace().get(0));
                PropertyDelta<String> nameDelta = createNameDelta(newName, getNameDefinition(identification));
                PropertyModificationOperation<?> nameMod = new PropertyModificationOperation<>(nameDelta);
                knownExecutedOperations.add(nameMod);

                replaceNameValue(identification, new Name((String)executedDelta.getValuesToReplace().get(0)));  // TODO why?
            } else {
                //noinspection unchecked
                ResourceAttributeDefinition<Object> definition =
                        (ResourceAttributeDefinition<Object>) objectClassDef.findAttributeDefinition(name);

                if (definition == null) {
                    throw new SchemaException("Returned delta references attribute '" + name + "' that has no definition.");
                }
                PropertyDelta<Object> delta = PrismContext.get().deltaFactory().property()
                        .create(ItemPath.create(ShadowType.F_ATTRIBUTES, definition.getItemName()), definition);
                if (executedDelta.getValuesToReplace() != null) {
                    delta.setRealValuesToReplace(executedDelta.getValuesToReplace().get(0));
                } else {
                    if (executedDelta.getValuesToAdd() != null) {
                        for (Object value : executedDelta.getValuesToAdd()) {
                            delta.addRealValuesToAdd(value);
                        }
                    }
                    if (executedDelta.getValuesToRemove() != null) {
                        for (Object value : executedDelta.getValuesToRemove()) {
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
    private AsynchronousOperationReturnValue<Collection<PropertyModificationOperation<?>>> modifyObjectUpdate(
            ResourceObjectIdentification identification,
            ObjectClass objClass,
            Uid uid,
            PrismObject<ShadowType> shadow,
            Collection<Operation> changes,
            ConnectorOperationOptions options,
            UcfExecutionContext reporter,
            OperationResult result)
                    throws ObjectNotFoundException, CommunicationException,
                        GenericFrameworkException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, PolicyViolationException {


        ResourceObjectDefinition objectClassDef = identification.getResourceObjectDefinition();
        String originalUid = uid.getUidValue();

        UpdateModificationConverter converter = new UpdateModificationConverter();
        converter.setChanges(changes);
        converter.setConnectorDescription(description);
        converter.setConnectorType(connectorType);
        converter.setConnIdNameMapper(connIdNameMapper);
        converter.setObjectDefinition(objectClassDef);
        converter.setProtector(protector);
        converter.setResourceSchema(rawResourceSchema);

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
            connIdResult = result.createSubresult(FACADE_OP_ADD_ATTRIBUTE_VALUES);
            connIdResult.addArbitraryObjectAsParam("objectClass", objectClassDef);
            connIdResult.addParam("uid", uid.getUidValue());
            connIdResult.addArbitraryObjectAsParam("attributes", attributesToAdd);
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", connIdConnectorFacade.getClass());

            InternalMonitor.recordConnectorOperation("addAttributeValues");
            InternalMonitor.recordConnectorModification("addAttributeValues");
            @Nullable ConnIdOperation operation =
                    recordIcfOperationStart(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);

            LOGGER.trace(
                    "Invoking ConnId addAttributeValues(), objectclass={}, uid={}, operation id={}, attributes: {}",
                    objClass, uid, getIdentifier(operation), lazy(() -> dumpAttributes(attributesToAdd)));

            try {
                uid = connIdConnectorFacade.addAttributeValues(objClass, uid, attributesToAdd, connIdOptions);
                recordIcfOperationEnd(reporter, operation, null);

                connIdResult.recordSuccess();

            } catch (Throwable ex) {
                recordIcfOperationEnd(reporter, operation, ex);
                String desc = this.getHumanReadableName() + " while adding attribute values to object identified by ConnId UID '"+uid.getUidValue()+"'";
                Throwable midpointEx = processConnIdException(ex, desc, connIdResult);
                result.computeStatus("Adding attribute values failed");
                // Do some kind of acrobatics to do proper throwing of checked
                // exception
                if (midpointEx instanceof ObjectNotFoundException) {
                    throw (ObjectNotFoundException) midpointEx;
                } else if (midpointEx instanceof CommunicationException) {
                    //in this situation this is not a critical error, becasue we know to handle it..so mute the error and sign it as expected
                    result.muteError();
                    connIdResult.muteError();
                    throw (CommunicationException) midpointEx;
                } else if (midpointEx instanceof GenericFrameworkException) {
                    throw (GenericFrameworkException) midpointEx;
                } else if (midpointEx instanceof SchemaException) {
                    throw (SchemaException) midpointEx;
                } else if (midpointEx instanceof AlreadyExistsException) {
                    throw (AlreadyExistsException) midpointEx;
                } else if (midpointEx instanceof RuntimeException) {
                    throw (RuntimeException) midpointEx;
                } else if (midpointEx instanceof SecurityViolationException){
                    throw (SecurityViolationException) midpointEx;
                } else if (midpointEx instanceof PolicyViolationException) {
                    throw (PolicyViolationException) midpointEx;
                } else if (midpointEx instanceof Error){
                    throw (Error) midpointEx;
                } else {
                    throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        }

        Set<Attribute> attributesToUpdate = converter.getAttributesToUpdate();
        if (!attributesToUpdate.isEmpty()) {
            OperationOptions connIdOptions = createConnIdOptions(options, changes);
            connIdResult = result.createSubresult(FACADE_OP_UPDATE);
            connIdResult.addArbitraryObjectAsParam("objectClass", objectClassDef);
            connIdResult.addParam("uid", uid==null?"null":uid.getUidValue());
            connIdResult.addArbitraryObjectAsParam("attributes", attributesToUpdate);
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", connIdConnectorFacade.getClass());

            InternalMonitor.recordConnectorOperation("update");
            InternalMonitor.recordConnectorModification("update");
            @Nullable ConnIdOperation operation =
                    recordIcfOperationStart(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);

            LOGGER.trace("Invoking ConnId update(), objectclass={}, uid={}, operation id={}, attributes: {}",
                    objClass, uid, getIdentifier(operation), lazy(() -> dumpAttributes(attributesToUpdate)));

            try {
                uid = connIdConnectorFacade.update(objClass, uid, attributesToUpdate, connIdOptions);
                recordIcfOperationEnd(reporter, operation, null);

                connIdResult.recordSuccess();
            } catch (Throwable ex) {
                recordIcfOperationEnd(reporter, operation, ex);
                String desc = this.getHumanReadableName() + " while updating object identified by ConnId UID '"+uid.getUidValue()+"'";
                Throwable midpointEx = processConnIdException(ex, desc, connIdResult);
                result.computeStatus("Update failed");
                // Do some kind of acrobatics to do proper throwing of checked
                // exception
                if (midpointEx instanceof ObjectNotFoundException) {
                    throw (ObjectNotFoundException) midpointEx;
                } else if (midpointEx instanceof CommunicationException) {
                    //in this situation this is not a critical error, becasue we know to handle it..so mute the error and sign it as expected
                    result.muteError();
                    connIdResult.muteError();
                    throw (CommunicationException) midpointEx;
                } else if (midpointEx instanceof GenericFrameworkException) {
                    throw (GenericFrameworkException) midpointEx;
                } else if (midpointEx instanceof SchemaException) {
                    throw (SchemaException) midpointEx;
                } else if (midpointEx instanceof ObjectAlreadyExistsException) {
                    throw (ObjectAlreadyExistsException) midpointEx;
                } else if (midpointEx instanceof RuntimeException) {
                    throw (RuntimeException) midpointEx;
                } else if (midpointEx instanceof SecurityViolationException) {
                    throw (SecurityViolationException) midpointEx;
                } else if (midpointEx instanceof PolicyViolationException) {
                    throw (PolicyViolationException) midpointEx;
                } else if (midpointEx instanceof Error) {
                    throw (Error) midpointEx;
                } else {
                    throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        }

        Set<Attribute> attributesToRemove = converter.getAttributesToRemove();
        if (!attributesToRemove.isEmpty()) {

            OperationOptions connIdOptions = createConnIdOptions(options, changes);
            connIdResult = result.createSubresult(FACADE_OP_REMOVE_ATTRIBUTE_VALUES);
            connIdResult.addArbitraryObjectAsParam("objectClass", objectClassDef);
            connIdResult.addParam("uid", uid.getUidValue());
            connIdResult.addArbitraryObjectAsParam("attributes", attributesToRemove);
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", connIdConnectorFacade.getClass());

            InternalMonitor.recordConnectorOperation("removeAttributeValues");
            InternalMonitor.recordConnectorModification("removeAttributeValues");
            @Nullable ConnIdOperation operation =
                    recordIcfOperationStart(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);

            LOGGER.trace(
                    "Invoking ConnId removeAttributeValues(), objectclass={}, uid={}, operation id={}, attributes: {}",
                    objClass, uid, getIdentifier(operation), lazy(() -> dumpAttributes(attributesToRemove)));

            try {
                uid = connIdConnectorFacade.removeAttributeValues(objClass, uid, attributesToRemove, connIdOptions);
                recordIcfOperationEnd(reporter, operation, null);
                connIdResult.recordSuccess();
            } catch (Throwable ex) {
                recordIcfOperationEnd(reporter, operation, ex);
                String desc = this.getHumanReadableName() + " while removing attribute values from object identified by ConnId UID '"+uid.getUidValue()+"'";
                Throwable midpointEx = processConnIdException(ex, desc, connIdResult);
                result.computeStatus("Removing attribute values failed");
                // Do some kind of acrobatics to do proper throwing of checked
                // exception
                if (midpointEx instanceof ObjectNotFoundException) {
                    throw (ObjectNotFoundException) midpointEx;
                } else if (midpointEx instanceof CommunicationException) {
                    //in this situation this is not a critical error, becasue we know to handle it..so mute the error and sign it as expected
                    result.muteError();
                    connIdResult.muteError();
                    throw (CommunicationException) midpointEx;
                } else if (midpointEx instanceof GenericFrameworkException) {
                    throw (GenericFrameworkException) midpointEx;
                } else if (midpointEx instanceof SchemaException) {
                    throw (SchemaException) midpointEx;
                } else if (midpointEx instanceof ObjectAlreadyExistsException) {
                    throw (ObjectAlreadyExistsException) midpointEx;
                } else if (midpointEx instanceof RuntimeException) {
                    throw (RuntimeException) midpointEx;
                } else if (midpointEx instanceof SecurityViolationException) {
                    throw (SecurityViolationException) midpointEx;
                } else if (midpointEx instanceof PolicyViolationException) {
                    throw (PolicyViolationException) midpointEx;
                } else if (midpointEx instanceof Error) {
                    throw (Error) midpointEx;
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
            PropertyModificationOperation<?> uidMod = new PropertyModificationOperation<>(uidDelta);
            // TODO what about matchingRuleQName ?
            sideEffectChanges.add(uidMod);

            replaceUidValue(identification, uid);
        }
        return AsynchronousOperationReturnValue.wrap(sideEffectChanges, result);
    }

    private void replaceNameValue(ResourceObjectIdentification identification, Name newName) throws SchemaException {
        ResourceAttribute<String> secondaryIdentifier = identification.getSecondaryIdentifier();
        if (secondaryIdentifier == null) {
            // fallback, compatibility
            for (ResourceAttribute<?> attr : identification.getAllIdentifiers()) {
                if (attr.getElementName().equals(SchemaConstants.ICFS_NAME)) {
                    // expecting the NAME property is of type String
                    //noinspection unchecked
                    ((ResourceAttribute<String>) attr).setRealValue(newName.getNameValue());
                    return;
                }
            }
            throw new IllegalStateException("No identifiers");
        }
        secondaryIdentifier.setRealValue(newName.getNameValue());
    }

    private PropertyDelta<String> createNameDelta(Name name, ResourceAttributeDefinition<String> nameDefinition) {
        PropertyDelta<String> uidDelta =
                PrismContext.get().deltaFactory().property()
                        .create(ItemPath.create(ShadowType.F_ATTRIBUTES, nameDefinition.getItemName()),
                nameDefinition);
        uidDelta.setRealValuesToReplace(name.getNameValue());
        return uidDelta;
    }

    private PropertyDelta<String> createUidDelta(Uid uid, ResourceAttributeDefinition<String> uidDefinition) {
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
    public AsynchronousOperationResult deleteObject(
            ResourceObjectDefinition objectDefinition,
            PrismObject<ShadowType> shadow,
            Collection<? extends ResourceAttribute<?>> identifiers,
            UcfExecutionContext ctx,
            OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException {
        Validate.notNull(objectDefinition, "No objectclass");

        UcfExecutionContext.checkExecutionFullyPersistent(ctx);

        OperationResult result = parentResult.createSubresult(OP_DELETE_OBJECT);
        result.addArbitraryObjectCollectionAsParam("identifiers", identifiers);

        ObjectClass objClass = objectClassToConnId(objectDefinition);
        Uid uid;
        try {
            uid = getUid(objectDefinition, identifiers);
        } catch (SchemaException e) {
            result.recordFatalError(e);
            throw e;
        }

        OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".delete");
        icfResult.addArbitraryObjectAsParam("uid", uid);
        icfResult.addArbitraryObjectAsParam("objectClass", objClass);
        icfResult.addContext("connector", connIdConnectorFacade.getClass());

        InternalMonitor.recordConnectorOperation("delete");
        InternalMonitor.recordConnectorModification("delete");
        ConnIdOperation operation = recordIcfOperationStart(ctx, ProvisioningOperation.ICF_DELETE, objectDefinition, uid);

        LOGGER.trace("Invoking ConnId delete operation: {}", operation);
        try {

            connIdConnectorFacade.delete(objClass, uid, new OperationOptionsBuilder().build());
            recordIcfOperationEnd(ctx, operation, null);

            icfResult.recordSuccess();

        } catch (Throwable ex) {
            recordIcfOperationEnd(ctx, operation, ex);
            String desc = this.getHumanReadableName() + " while deleting object identified by ConnId UID '"+uid.getUidValue()+"'";
            Throwable midpointEx = processConnIdException(ex, desc, icfResult);
            result.computeStatus("Removing attribute values failed");
            // Do some kind of acrobatics to do proper throwing of checked
            // exception
            if (midpointEx instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) midpointEx;
            } else if (midpointEx instanceof CommunicationException) {
                throw (CommunicationException) midpointEx;
            } else if (midpointEx instanceof GenericFrameworkException) {
                throw (GenericFrameworkException) midpointEx;
            } else if (midpointEx instanceof SchemaException) {
                // Schema exception during delete? It must be a missing UID
                throw new IllegalArgumentException(midpointEx.getMessage(), midpointEx);
            } else if (midpointEx instanceof RuntimeException) {
                throw (RuntimeException) midpointEx;
            } else if (midpointEx instanceof Error) {
                throw (Error) midpointEx;
            } else {
                throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        result.computeStatus();
        return AsynchronousOperationResult.wrap(result);
    }

    @Override
    public UcfSyncToken fetchCurrentToken(ResourceObjectDefinition objectDefinition, UcfExecutionContext ctx,
            OperationResult parentResult) throws CommunicationException, GenericFrameworkException {

        OperationResult result = parentResult.createSubresult(OP_FETCH_CURRENT_TOKEN);
        result.addArbitraryObjectAsParam("objectClass", objectDefinition);

        ObjectClass icfObjectClass;
        if (objectDefinition == null) {
            icfObjectClass = ObjectClass.ALL;
        } else {
            icfObjectClass = objectClassToConnId(objectDefinition);
        }

        OperationResult icfResult = result.createSubresult(FACADE_OP_SYNC);
        icfResult.addContext("connector", connIdConnectorFacade.getClass());
        icfResult.addArbitraryObjectAsParam("icfObjectClass", icfObjectClass);

        SyncToken syncToken;
        InternalMonitor.recordConnectorOperation("getLatestSyncToken");
        ConnIdOperation operation = recordIcfOperationStart(ctx, ProvisioningOperation.ICF_GET_LATEST_SYNC_TOKEN, objectDefinition);
        LOGGER.trace("Invoking ConnId getLatestSyncToken operation: {}", operation);
        try {
            syncToken = connIdConnectorFacade.getLatestSyncToken(icfObjectClass);
            recordIcfOperationEnd(ctx, operation, null);
            icfResult.recordSuccess();
            icfResult.addReturn("syncToken", syncToken==null?null:String.valueOf(syncToken.getValue()));
        } catch (Throwable ex) {
            recordIcfOperationEnd(ctx, operation, ex);
            Throwable midpointEx = processConnIdException(ex, this, icfResult);
            result.computeStatus();
            // Do some kind of acrobatics to do proper throwing of checked
            // exception
            if (midpointEx instanceof CommunicationException) {
                throw (CommunicationException) midpointEx;
            } else if (midpointEx instanceof GenericFrameworkException) {
                throw (GenericFrameworkException) midpointEx;
            } else if (midpointEx instanceof RuntimeException) {
                throw (RuntimeException) midpointEx;
            } else if (midpointEx instanceof Error) {
                throw (Error) midpointEx;
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
            ResourceObjectDefinition objectDefinition,
            UcfSyncToken initialTokenValue,
            AttributesToReturn attrsToReturn,
            Integer maxChanges,
            UcfExecutionContext ctx,
            @NotNull UcfLiveSyncChangeListener changeListener,
            OperationResult parentResult)
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

            SyncDeltaConverter changeConverter = new SyncDeltaConverter(this, objectDefinition);

            AtomicBoolean allChangesFetched = new AtomicBoolean(true);
            UcfFetchChangesResult fetchChangesResult;

            OperationResult connIdResult = result.subresult(FACADE_OP_SYNC)
                    .addContext("connector", connIdConnectorFacade.getClass())
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
                        handleResult.cleanupResult();
                        connIdResult.summarize(true);
                        recordIcfOperationResume(ctx, operation);
                    }
                };

                LOGGER.trace("Invoking ConnId sync operation: {}", operation);
                SyncToken finalToken;
                try {
                    finalToken = connIdConnectorFacade.sync(requestConnIdObjectClass, initialToken, syncHandler, options);
                    // Note that finalToken value is not quite reliable. The SyncApiOp documentation is not clear on its semantics;
                    // it is only from SyncTokenResultsHandler (SPI) documentation and SyncImpl class that we know this value is
                    // non-null when all changes were fetched. And some of the connectors return null even then.
                    LOGGER.trace("connector sync method returned: {}", finalToken);
                    connIdResult.computeStatus();
                    connIdResult.cleanupResult();
                    connIdResult.addReturn(OperationResult.RETURN_COUNT, deltasProcessed.get());
                    recordIcfOperationEnd(ctx, operation, null);
                } catch (Throwable ex) {
                    recordIcfOperationEnd(ctx, operation, ex);
                    Throwable midpointEx = processConnIdException(ex, this, connIdResult);
                    connIdResult.computeStatusIfUnknown();
                    connIdResult.cleanupResult();
                    result.computeStatus();
                    // Do some kind of acrobatics to do proper throwing of checked exception
                    if (midpointEx instanceof CommunicationException) {
                        throw (CommunicationException) midpointEx;
                    } else if (midpointEx instanceof GenericFrameworkException) {
                        throw (GenericFrameworkException) midpointEx;
                    } else if (midpointEx instanceof SchemaException) {
                        throw (SchemaException) midpointEx;
                    } else if (midpointEx instanceof RuntimeException) {
                        throw (RuntimeException) midpointEx;
                    } else if (midpointEx instanceof Error) {
                        throw (Error) midpointEx;
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
        result.addContext("connector", connectorType);

        try {
            if (isPartialTest) {
                InternalMonitor.recordConnectorOperation("testPartialConfiguration");
                connIdConnectorFacade.testPartialConfiguration();
            } else {
                InternalMonitor.recordConnectorOperation("test");
                connIdConnectorFacade.test();
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
        result.addContext("connector", connectorType);

        InternalMonitor.recordConnectorOperation("discoverConfiguration");

        try {
            Map<String, SuggestedValues> suggestions = connIdConnectorFacade.discoverConfiguration();

            ConnIdConfigurationTransformer configTransformer =
                    new ConnIdConfigurationTransformer(connectorType, connectorInfo, protector, null);

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
            @Nullable AttributesToReturn attributesToReturn,
            @Nullable PagedSearchCapabilityType pagedSearchConfiguration,
            @Nullable SearchHierarchyConstraints searchHierarchyConstraints,
            @Nullable UcfFetchErrorReportingMethod ucfErrorReportingMethod,
            @NotNull UcfExecutionContext ctx,
            @NotNull OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SecurityViolationException, SchemaException,
                        ObjectNotFoundException {

        // Result type for this operation
        final OperationResult result = parentResult.createSubresult(OP_SEARCH);
        result.addArbitraryObjectAsParam("objectClass", objectDefinition);
        result.addContext("connector", connectorType);
        try {
            validateConnectorFacade();

            if (pagedSearchConfiguration == null) {
                pagedSearchConfiguration = getCapability(PagedSearchCapabilityType.class);
            }

            return new SearchExecutor(objectDefinition, query, handler, attributesToReturn,
                    pagedSearchConfiguration, searchHierarchyConstraints,
                    ucfErrorReportingMethod, ctx, this)
                    .execute(result);

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown(); // or should we simply record success if unknown?
        }
    }

    @Override
    public int count(ResourceObjectDefinition objectDefinition, final ObjectQuery query, PagedSearchCapabilityType pagedSearchCapabilityType,
            UcfExecutionContext ctx, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, UnsupportedOperationException {

        // Result type for this operation
        final OperationResult result = parentResult.createSubresult(OP_COUNT);
        result.addArbitraryObjectAsParam("objectClass", objectDefinition);
        result.addContext("connector", connectorType);

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
            String orderByIcfName = connIdNameMapper.convertAttributeNameToConnId(pagedSearchCapabilityType.getDefaultSortField(), objectDefinition, "(default sorting field)");
            boolean isAscending = pagedSearchCapabilityType.getDefaultSortDirection() != OrderDirectionType.DESCENDING;
            optionsBuilder.setSortKeys(new SortKey(orderByIcfName, isAscending));
        }
        OperationOptions options = optionsBuilder.build();

        // Connector operation cannot create result for itself, so we need to
        // create result for it
        OperationResult icfResult = result.createSubresult(FACADE_OP_SEARCH);
        icfResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);
        icfResult.addContext("connector", connIdConnectorFacade.getClass());

        int retval;

        InternalMonitor.recordConnectorOperation("search");
        ConnIdOperation operation = recordIcfOperationStart(ctx, ProvisioningOperation.ICF_SEARCH, objectDefinition);

        try {

            Filter filter = convertFilterToIcf(query, objectDefinition);
            final Holder<Integer> fetched = new Holder<>(0);

            ResultsHandler connIdHandler = new ResultsHandler() {
                @Override
                public boolean handle(ConnectorObject connectorObject) {
                    fetched.setValue(fetched.getValue() + 1); // actually, this should execute at most once
                    return false;
                }

                @Override
                public String toString() {
                    return "(midPoint counting result handler)";
                }
            };
            LOGGER.trace("Invoking ConnId search operation (to count objects): {}", operation);
            SearchResult searchResult = connIdConnectorFacade.search(icfObjectClass, filter, connIdHandler, options);
            recordIcfOperationEnd(ctx, operation, null);

            if (searchResult == null || searchResult.getRemainingPagedResults() == -1) {
                throw new UnsupportedOperationException("Connector does not seem to support paged searches or does not provide object count information");
            } else {
                retval = fetched.getValue() + searchResult.getRemainingPagedResults();
            }

            icfResult.recordSuccess();
        } catch (IntermediateException inex) {
            recordIcfOperationEnd(ctx, operation, inex);
            SchemaException ex = (SchemaException) inex.getCause();
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
            if (midpointEx instanceof CommunicationException) {
                throw (CommunicationException) midpointEx;
            } else if (midpointEx instanceof GenericFrameworkException) {
                throw (GenericFrameworkException) midpointEx;
            } else if (midpointEx instanceof SchemaException) {
                throw (SchemaException) midpointEx;
            } else if (midpointEx instanceof RuntimeException) {
                throw (RuntimeException) midpointEx;
            } else if (midpointEx instanceof Error) {
                throw (Error) midpointEx;
            } else {
                throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        if (result.isUnknown()) {
            result.recordSuccess();
        }

        return retval;
    }

    @NotNull
    ObjectClass objectClassToConnId(ResourceObjectDefinition objectDefinition) {
        return connIdNameMapper.objectClassToConnId(objectDefinition, connectorType, legacySchema);
    }

    Filter convertFilterToIcf(ObjectQuery query, ResourceObjectDefinition objectDefinition) throws SchemaException {
        ObjectFilter prismFilter = query != null ? query.getFilter() : null;
        if (prismFilter != null) {
            LOGGER.trace("Start to convert filter: {}", prismFilter.debugDumpLazily());
            FilterInterpreter interpreter = new FilterInterpreter(objectDefinition);
            Filter connIdFilter = interpreter.interpret(prismFilter, connIdNameMapper);
            LOGGER.trace("ConnId filter: {}", lazy(() -> ConnIdUtil.dump(connIdFilter)));
            return connIdFilter;
        } else {
            return null;
        }
    }

    // UTILITY METHODS

    private Uid getUid(ResourceObjectIdentification resourceObjectIdentification) throws SchemaException {
        ResourceAttribute<String> primaryIdentifier = resourceObjectIdentification.getPrimaryIdentifier();
        if (primaryIdentifier == null) {
            return null;
        }
        String uidValue = primaryIdentifier.getRealValue();
        String nameValue = getNameValue(resourceObjectIdentification);
        if (uidValue != null) {
            if (nameValue == null) {
                return new Uid(uidValue);
            } else {
                return new Uid(uidValue, new Name(nameValue));
            }
        }
        return null;
    }

    private String getNameValue(ResourceObjectIdentification resourceObjectIdentification) throws SchemaException {
        Collection<? extends ResourceAttribute<?>> secondaryIdentifiers = resourceObjectIdentification.getSecondaryIdentifiers();
        if (secondaryIdentifiers.size() == 1) {
            return (String) secondaryIdentifiers.iterator().next().getRealValue();
        } else if (secondaryIdentifiers.size() > 1) {
            for (ResourceAttribute<?> secondaryIdentifier : secondaryIdentifiers) {
                ResourceAttributeDefinition<?> definition = secondaryIdentifier.getDefinition();
                if (definition != null) {
                    if (Name.NAME.equals(definition.getFrameworkAttributeName())) {
                        return (String) secondaryIdentifier.getRealValue();
                    }
                }
            }
            throw new SchemaException(
                    "More than one secondary identifier in "+resourceObjectIdentification+", cannot determine ConnId __NAME__");
        }
        return null;
    }

    /**
     * Looks up ConnId Uid identifier in a (potentially multi-valued) set of
     * identifiers. Handy method to convert midPoint identifier style to an ConnId
     * identifier style.
     *
     * @param identifiers
     *            midPoint resource object identifiers
     * @return ConnId UID or null
     */
    private Uid getUid(ResourceObjectDefinition objectDefinition, Collection<? extends ResourceAttribute<?>> identifiers)
            throws SchemaException {
        if (identifiers.size() == 0) {
            return null;
        }
        if (identifiers.size() == 1) {
            try {
                return new Uid((String) identifiers.iterator().next().getRealValue());
            } catch (IllegalArgumentException e) {
                throw new SchemaException(e.getMessage(), e);
            }
        }
        String uidValue = null;
        String nameValue = null;
        for (ResourceAttribute<?> attr : identifiers) {
            if (objectDefinition.isPrimaryIdentifier(attr.getElementName())) {
                //noinspection unchecked
                uidValue = ((ResourceAttribute<String>) attr).getValue().getValue();
            }
            if (objectDefinition.isSecondaryIdentifier(attr.getElementName())) {
                ResourceAttributeDefinition<?> attrDef = objectDefinition.findAttributeDefinitionRequired(attr.getElementName());
                String frameworkAttributeName = attrDef.getFrameworkAttributeName();
                if (Name.NAME.equals(frameworkAttributeName)) {
                    //noinspection unchecked
                    nameValue = ((ResourceAttribute<String>) attr).getValue().getValue();
                }
            }
        }
        if (uidValue != null) {
            if (nameValue == null) {
                return new Uid(uidValue);
            } else {
                return new Uid(uidValue, new Name(nameValue));
            }
        }
        // fallback, compatibility
        for (ResourceAttribute<?> attr : identifiers) {
            if (attr.getElementName().equals(SchemaConstants.ICFS_UID)) {
                //noinspection unchecked
                return new Uid(((ResourceAttribute<String>) attr).getValue().getValue());
            }
        }
        return null;
    }

    private void replaceUidValue(ResourceObjectIdentification identification, Uid newUid) throws SchemaException {
        ResourceAttribute<String> primaryIdentifier = identification.getPrimaryIdentifier();
        if (primaryIdentifier == null) {
            // fallback, compatibility
            Collection<? extends ResourceAttribute<?>> identifiers = identification.getAllIdentifiers();
            for (ResourceAttribute<?> attr : identifiers) {
                if (attr.getElementName().equals(SchemaConstants.ICFS_UID)) {
                    // expecting the UID property is of type String
                    //noinspection unchecked
                    ((ResourceAttribute<String>) attr).setRealValue(newUid.getUidValue());
                    return;
                }
            }
            throw new IllegalStateException("No UID attribute in " + identifiers);
        }
        primaryIdentifier.setRealValue(newUid.getUidValue());
    }

    private ResourceAttributeDefinition<String> getNameDefinition(ResourceObjectIdentification identification) throws SchemaException {
        ResourceAttribute<String> secondaryIdentifier = identification.getSecondaryIdentifier();
        if (secondaryIdentifier == null) {
            // fallback, compatibility
            for (ResourceAttribute<?> attr : identification.getAllIdentifiers()) {
                if (attr.getElementName().equals(SchemaConstants.ICFS_NAME)) {
                    //noinspection unchecked
                    return (ResourceAttributeDefinition<String>) attr.getDefinition();
                }
            }
            return null;
        }
        return secondaryIdentifier.getDefinition();
    }

    private ResourceAttributeDefinition getUidDefinition(ResourceObjectIdentification identification) throws SchemaException {
        ResourceAttribute<String> primaryIdentifier = identification.getPrimaryIdentifier();
        if (primaryIdentifier == null) {
            // fallback, compatibility
            for (ResourceAttribute<?> attr : identification.getAllIdentifiers()) {
                if (attr.getElementName().equals(SchemaConstants.ICFS_UID)) {
                    return attr.getDefinition();
                }
            }
            return null;
        }
        return primaryIdentifier.getDefinition();
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
        String opName;
        if (scriptOperation.isConnectorHost()) {
            icfOpName = "runScriptOnConnector";
            opName = FACADE_OP_RUN_SCRIPT_ON_CONNECTOR;
        } else if (scriptOperation.isResourceHost()) {
            icfOpName = "runScriptOnResource";
            opName = FACADE_OP_RUN_SCRIPT_ON_RESOURCE;
        } else {
            throw new IllegalArgumentException("Where to execute the script?");
        }

        // convert execute script operation to the script context required from
        // the connector
        ScriptContext scriptContext = convertToScriptContext(scriptOperation);

        OperationResult icfResult = parentResult.createSubresult(opName);
        icfResult.addContext("connector", connIdConnectorFacade.getClass());

        ConnIdOperation operation = recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SCRIPT, null);

        Object output = null;

        try {

            LOGGER.trace("Running script ({}): {}", icfOpName, operation);

            if (scriptOperation.isConnectorHost()) {
                InternalMonitor.recordConnectorOperation("runScriptOnConnector");
                output = connIdConnectorFacade.runScriptOnConnector(scriptContext, new OperationOptionsBuilder().build());
            } else if (scriptOperation.isResourceHost()) {
                InternalMonitor.recordConnectorOperation("runScriptOnResource");
                output = connIdConnectorFacade.runScriptOnResource(scriptContext, new OperationOptionsBuilder().build());
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
                if (midpointEx instanceof CommunicationException) {
                    throw (CommunicationException) midpointEx;
                } else if (midpointEx instanceof GenericFrameworkException) {
                    throw (GenericFrameworkException) midpointEx;
                } else if (midpointEx instanceof SchemaException) {
                    // Schema exception during delete? It must be a missing UID
                    throw new IllegalArgumentException(midpointEx.getMessage(), midpointEx);
                } else if (midpointEx instanceof RuntimeException) {
                    throw (RuntimeException) midpointEx;
                } else if (midpointEx instanceof Error) {
                    throw (Error) midpointEx;
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
        return "ConnectorInstanceIcfImpl(" + connectorType + ")";
    }

    @Override
    public String getHumanReadableDescription() {
        return connectorType != null ?
                getOrig(connectorType.getName()) : // should be descriptive enough
                "no connector"; // can this even occur?
    }

    public String getHumanReadableName() {
        return connectorType.toString() + ": " + description;
    }

    @Override
    public void dispose() {
        if (connIdConnectorFacade != null) {
            LOGGER.debug("Disposing ConnId ConnectorFacade for instance: {} (dispose explicitly invoked on ConnectorInstance)", instanceName);
            connIdConnectorFacade.dispose();
            connIdConnectorFacade = null;
        }
    }

    @Override
    public @NotNull CapabilityCollectionType getNativeCapabilities(OperationResult result)
            throws CommunicationException, ConfigurationException, GenericFrameworkException {

        APIConfiguration apiConfig = connectorInfo.createDefaultAPIConfiguration();

        ConnectorFacade facade = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

        ConnIdCapabilitiesAndSchemaParser parser =
                new ConnIdCapabilitiesAndSchemaParser(
                        facade,
                        getHumanReadableName());
        parser.retrieveResourceCapabilities(result);

        return parser.getCapabilities();
    }

    @Contract("!null, _, _, _ -> !null; null, _, _, _ -> null")
    private @Nullable ConnIdOperation recordIcfOperationStart(UcfExecutionContext reporter, ProvisioningOperation operation,
            ResourceObjectDefinition objectDefinition, Uid uid) {
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

    private OperationOptions createConnIdOptions(ConnectorOperationOptions options, Collection<Operation> changes) throws SchemaException {
        OperationOptionsBuilder connIdOptionsBuilder = new OperationOptionsBuilder();
        if (options != null) {
            ResourceObjectIdentification runAsIdentification = options.getRunAsIdentification();
            if (runAsIdentification != null) {
                connIdOptionsBuilder.setRunAsUser(getNameValue(runAsIdentification));
                // We are going to figure out what the runAsPassword may be.
                // If there is a password change then there should be old value in the delta.
                // This is quite a black magic. But we do not have a better way now.
                for (Operation change : changes) {
                    if (change instanceof PropertyModificationOperation) {
                        PropertyDelta<?> propertyDelta = ((PropertyModificationOperation<?>)change).getPropertyDelta();
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
                            GuardedString oldPasswordGs = ConnIdUtil.toGuardedString(oldPassword, "runAs password", protector);
                            connIdOptionsBuilder.setRunWithPassword(oldPasswordGs);
                        }
                    }
                }
            }
        }
        return connIdOptionsBuilder.build();
    }

    boolean isLegacySchema() {
        return Boolean.TRUE.equals(legacySchema);
    }

    ResourceSchema getRawResourceSchema() {
        return rawResourceSchema;
    }

    boolean isCaseIgnoreAttributeNames() {
        return caseIgnoreAttributeNames;
    }

    ConnectorFacade getConnIdConnectorFacade() {
        return connIdConnectorFacade;
    }
}
