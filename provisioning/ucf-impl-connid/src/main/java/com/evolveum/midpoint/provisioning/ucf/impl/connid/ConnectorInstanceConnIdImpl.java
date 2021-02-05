/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdUtil.processConnIdException;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.*;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.Validate;
import org.identityconnectors.common.pooling.ObjectPoolConfiguration;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.QualifiedUid;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SortKey;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.impl.api.APIConfigurationImpl;
import org.identityconnectors.framework.impl.api.local.LocalConnectorInfoImpl;
import org.identityconnectors.framework.impl.api.local.ObjectPool;
import org.identityconnectors.framework.impl.api.local.ObjectPool.Statistics;
import org.identityconnectors.framework.impl.api.local.operations.ConnectorOperationalContext;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.PoolableConnector;

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.query.FilterInterpreter;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.schema.util.ActivationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.jetbrains.annotations.NotNull;

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
public class ConnectorInstanceConnIdImpl implements ConnectorInstance {

    private static final com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory CAPABILITY_OBJECT_FACTORY
        = new com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory();

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorInstanceConnIdImpl.class);

    private static final String OP_FETCH_CHANGES = ConnectorInstance.class.getName() + ".fetchChanges";

    private final ConnectorInfo connectorInfo;
    private final ConnectorType connectorType;
    private ConnectorFacade connIdConnectorFacade;
    private final String resourceSchemaNamespace;
    private final PrismSchema connectorSchema;
    private APIConfiguration apiConfig = null;

    private final Protector protector;
    final PrismContext prismContext;
    final ConnIdNameMapper connIdNameMapper;
    final ConnIdConvertor connIdConvertor;

    private List<QName> generateObjectClasses = null;

    /**
     * Builder and holder object for parsed ConnId schema and capabilities. By using
     * this class the ConnectorInstance always has a consistent schema, even during
     * reconfigure and fetch operations.
     * There is either old schema or new schema, but there is no partially-parsed schema.
     */
    private ConnIdCapabilitiesAndSchemaParser parsedCapabilitiesAndSchema;

    private ResourceSchema resourceSchema = null;
    private Collection<Object> capabilities = null;
    private Boolean legacySchema = null;

    private String description;
    private String instanceName; // resource name
    private boolean caseIgnoreAttributeNames = false;

    ConnectorInstanceConnIdImpl(ConnectorInfo connectorInfo, ConnectorType connectorType,
            String schemaNamespace, PrismSchema connectorSchema, Protector protector,
            PrismContext prismContext, LocalizationService localizationService) {
        this.connectorInfo = connectorInfo;
        this.connectorType = connectorType;
        this.resourceSchemaNamespace = schemaNamespace;
        this.connectorSchema = connectorSchema;
        this.protector = protector;
        this.prismContext = prismContext;
        connIdNameMapper = new ConnIdNameMapper(schemaNamespace);
        connIdConvertor = new ConnIdConvertor(protector, resourceSchemaNamespace, localizationService, connIdNameMapper);
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

    public String getSchemaNamespace() {
        return resourceSchemaNamespace;
    }

    public void setResourceSchema(ResourceSchema resourceSchema) {
        this.resourceSchema = resourceSchema;
        connIdNameMapper.setResourceSchema(resourceSchema);
    }

    public void resetResourceSchema() {
        setResourceSchema(null);
    }

    @Override
    public synchronized void configure(@NotNull PrismContainerValue<?> configurationOriginal, List<QName> generateObjectClasses, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_CONFIGURE);

        LOGGER.trace("Configuring connector {}, provided configuration:\n{}", connectorType, configurationOriginal.debugDumpLazily(1));

        try {
            this.generateObjectClasses = generateObjectClasses;
            // Get default configuration for the connector. This is important,
            // as it contains types of connector configuration properties.

            // Make sure that the proper configuration schema is applied. This
            // will cause that all the "raw" elements are parsed
            PrismContainerValue<?> configurationCloned = configurationOriginal.clone();
            configurationCloned.applyDefinition(getConfigurationContainerDefinition());

            ConnIdConfigurationTransformer configTransformer = new ConnIdConfigurationTransformer(connectorType, connectorInfo, protector);
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

            result.recordSuccess();

            PrismProperty<Boolean> legacySchemaConfigProperty = configurationCloned.findProperty(new ItemName(
                    SchemaConstants.NS_ICF_CONFIGURATION,
                    ConnectorFactoryConnIdImpl.CONNECTOR_SCHEMA_LEGACY_SCHEMA_XML_ELEMENT_NAME));
            if (legacySchemaConfigProperty != null) {
                legacySchema = legacySchemaConfigProperty.getRealValue();
            }
            LOGGER.trace("Legacy schema (config): {}", legacySchema);

        } catch (Throwable ex) {
            Throwable midpointEx = processConnIdException(ex, this, result);
            result.computeStatus("Removing attribute values failed");
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

    private void validateConnectorFacade(OperationResult result) {
        if (connIdConnectorFacade == null) {
            result.recordFatalError("Attempt to use unconfigured connector");
            throw new IllegalStateException("Attempt to use unconfigured connector "
                    + connectorType);
        }
    }

    @Override
    public void initialize(ResourceSchema resourceSchema, Collection<Object> capabilities, boolean caseIgnoreAttributeNames, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException {

        // Result type for this operation
        OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_INITIALIZE);
        result.addContext("connector", connectorType);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ConnectorFactoryConnIdImpl.class);

        validateConnectorFacade(parentResult);

        updateSchema(resourceSchema);
        this.capabilities = capabilities;
        this.caseIgnoreAttributeNames = caseIgnoreAttributeNames;

        if (resourceSchema == null || capabilities == null) {
            try {

                retrieveAndParseResourceCapabilitiesAndSchema(generateObjectClasses, result);

                this.legacySchema = parsedCapabilitiesAndSchema.getLegacySchema();
                setResourceSchema(parsedCapabilitiesAndSchema.getResourceSchema());
                this.capabilities = parsedCapabilitiesAndSchema.getCapabilities();

            } catch (CommunicationException ex) {
                // This is in fact fatal. There is not schema. Not even the pre-cached one.
                // The connector will not be able to work.
                result.recordFatalError(ex);
                throw ex;
            } catch (ConfigurationException ex) {
                result.recordFatalError(ex);
                throw ex;
            } catch (GenericFrameworkException ex) {
                result.recordFatalError(ex);
                throw ex;
            }
        }

        result.recordSuccess();
    }

    public void updateSchema(ResourceSchema resourceSchema) {
        setResourceSchema(resourceSchema);

        if (resourceSchema != null && legacySchema == null) {
            legacySchema = detectLegacySchema(resourceSchema);
        }
    }

    private boolean detectLegacySchema(ResourceSchema resourceSchema) {
        ComplexTypeDefinition accountObjectClass = resourceSchema.findComplexTypeDefinitionByType(
                new QName(resourceSchemaNamespace, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME));
        return accountObjectClass != null;
    }

    @Override
    public synchronized ResourceSchema fetchResourceSchema(OperationResult parentResult) throws CommunicationException,
            GenericFrameworkException, ConfigurationException, SchemaException {

        // Result type for this operation
        OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName() + ".fetchResourceSchema");
        result.addContext("connector", connectorType);

        try {

            if (parsedCapabilitiesAndSchema == null) {
                retrieveAndParseResourceCapabilitiesAndSchema(generateObjectClasses, result);
            }

            legacySchema = parsedCapabilitiesAndSchema.getLegacySchema();
            setResourceSchema(parsedCapabilitiesAndSchema.getResourceSchema());

        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        }

        if (resourceSchema == null) {
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Connector does not support schema");
        } else {
            result.recordSuccess();
        }

        return resourceSchema;
    }

    @Override
    public synchronized Collection<Object> fetchCapabilities(OperationResult parentResult)
        throws CommunicationException, GenericFrameworkException, ConfigurationException, SchemaException {

        // Result type for this operation
        OperationResult result = parentResult.createMinorSubresult(ConnectorInstance.class.getName() + ".fetchCapabilities");
        result.addContext("connector", connectorType);

        try {

            // Always refresh capabilities and schema here, even if we have already parsed that before.
            // This method is used in "test connection". We want to force fresh fetch here
            // TODO: later: clean up this mess. Make fresh fetch somehow explicit?
            retrieveAndParseResourceCapabilitiesAndSchema(generateObjectClasses, result);

            legacySchema = parsedCapabilitiesAndSchema.getLegacySchema();
            setResourceSchema(parsedCapabilitiesAndSchema.getResourceSchema());
            capabilities = parsedCapabilitiesAndSchema.getCapabilities();

        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        }

        result.recordSuccess();

        return capabilities;
    }


    private void retrieveAndParseResourceCapabilitiesAndSchema(List<QName> generateObjectClasses, OperationResult parentResult) throws CommunicationException, ConfigurationException, GenericFrameworkException, SchemaException {

        ConnIdCapabilitiesAndSchemaParser parser = new ConnIdCapabilitiesAndSchemaParser();
        parser.setConnectorHumanReadableName(getHumanReadableName());
        parser.setConnIdConnectorFacade(connIdConnectorFacade);
        parser.setConnIdNameMapper(connIdNameMapper);
        parser.setPrismContext(prismContext);
        parser.setResourceSchemaNamespace(resourceSchemaNamespace);
        parser.setLegacySchema(legacySchema);

        LOGGER.debug("Retrieving and parsing schema and capabilities for {}", getHumanReadableName());
        parser.retrieveResourceCapabilitiesAndSchema(generateObjectClasses, parentResult);

        parsedCapabilitiesAndSchema = parser;
    }

     private synchronized <C extends CapabilityType> C getCapability(Class<C> capClass) {
        if (capabilities == null) {
            return null;
        }
        for (Object cap: capabilities) {
            if (capClass.isAssignableFrom(cap.getClass())) {
                return (C) cap;
            }
        }
        return null;
    }

    @Override
    public PrismObject<ShadowType> fetchObject(ResourceObjectIdentification resourceObjectIdentification,
            AttributesToReturn attributesToReturn, StateReporter reporter, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
            SchemaException, SecurityViolationException, ConfigurationException {

        Validate.notNull(resourceObjectIdentification, "Null primary identifiers");
        ObjectClassComplexTypeDefinition objectClassDefinition = resourceObjectIdentification.getObjectClassDefinition();

        OperationResult result = parentResult.createMinorSubresult(ConnectorInstance.class.getName() + ".fetchObject");
        result.addArbitraryObjectAsParam("resourceObjectDefinition", objectClassDefinition);
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

            ObjectClass icfObjectClass = connIdNameMapper
                    .objectClassToConnId(objectClassDefinition, getSchemaNamespace(), connectorType, legacySchema);
            if (icfObjectClass == null) {
                throw new IllegalArgumentException("Unable to determine object class from QName "
                        + objectClassDefinition.getTypeName()
                        + " while attempting to fetch object identified by " + resourceObjectIdentification + " from "
                        + description);
            }

            OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
            convertToIcfAttrsToGet(objectClassDefinition, attributesToReturn, optionsBuilder);
            optionsBuilder.setAllowPartialResults(true);
            OperationOptions options = optionsBuilder.build();

            ConnectorObject co;
            //noinspection CaughtExceptionImmediatelyRethrown
            try {
                co = fetchConnectorObject(reporter, objectClassDefinition, icfObjectClass, uid, options, result);
            } catch (CommunicationException | RuntimeException | SchemaException | GenericFrameworkException |
                    ConfigurationException | SecurityViolationException ex) {
                // This is fatal. No point in continuing. Just re-throw the exception.
                throw ex;
            } catch (ObjectNotFoundException ex) {
                throw new ObjectNotFoundException(
                        "Object identified by " + resourceObjectIdentification + " (ConnId UID " + uid + "), objectClass "
                                + objectClassDefinition.getTypeName() + "  was not found in " + description);
            }

            if (co == null) {
                throw new ObjectNotFoundException(
                        "Object identified by " + resourceObjectIdentification + " (ConnId UID " + uid + "), objectClass "
                                + objectClassDefinition.getTypeName() + " was not in " + description);
            }

            PrismObjectDefinition<ShadowType> shadowDefinition = toShadowDefinition(objectClassDefinition);
            // TODO configure error reporting method
            PrismObject<ShadowType> shadow = connIdConvertor
                    .convertToResourceObject(co, shadowDefinition, false, caseIgnoreAttributeNames,
                            legacySchema, FetchErrorReportingMethodType.DEFAULT, result);

            result.recordSuccess();
            return shadow;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    static PrismObjectDefinition<ShadowType> toShadowDefinition(
            ObjectClassComplexTypeDefinition objectClassDefinition) {
        ResourceAttributeContainerDefinition resourceAttributeContainerDefinition = objectClassDefinition
                .toResourceAttributeContainerDefinition(ShadowType.F_ATTRIBUTES);
        return resourceAttributeContainerDefinition.toShadowDefinition();
    }

    /**
     * Returns null if nothing is found.
     */
    private ConnectorObject fetchConnectorObject(StateReporter reporter, ObjectClassComplexTypeDefinition objectClassDefinition, ObjectClass icfObjectClass, Uid uid,
                                                 OperationOptions options, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SecurityViolationException, SchemaException, ConfigurationException {

        // Connector operation cannot create result for itself, so we need to
        // create result for it
        OperationResult icfResult = parentResult.createMinorSubresult(ConnectorFacade.class.getName()
                + ".getObject");
        icfResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);
        icfResult.addParam("uid", uid.getUidValue());
        icfResult.addArbitraryObjectAsParam("options", options);
        icfResult.addContext("connector", connIdConnectorFacade.getClass());

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Fetching connector object ObjectClass={}, UID={}, options={}",
                    icfObjectClass, uid, ConnIdUtil.dumpOptions(options));
        }

        ConnectorObject co = null;
        try {

            // Invoke the ConnId connector
            InternalMonitor.recordConnectorOperation("getObject");
            recordIcfOperationStart(reporter, ProvisioningOperation.ICF_GET, objectClassDefinition, uid);
            co = connIdConnectorFacade.getObject(icfObjectClass, uid, options);
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET, objectClassDefinition, uid);

            icfResult.recordSuccess();
        } catch (Throwable ex) {
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET, objectClassDefinition, ex, uid);
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

    private void convertToIcfAttrsToGet(ObjectClassComplexTypeDefinition objectClassDefinition,
            AttributesToReturn attributesToReturn, OperationOptionsBuilder optionsBuilder) throws SchemaException {
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
                for (ResourceAttributeDefinition attributeDef: objectClassDefinition.getAttributeDefinitions()) {
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
    public AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> addObject(PrismObject<? extends ShadowType> shadow, StateReporter reporter, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException {
        validateShadow(shadow, "add", false);
        ShadowType shadowType = shadow.asObjectable();

        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);
        OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
                + ".addObject");
        result.addParam("resourceObject", shadow);

        ObjectClassComplexTypeDefinition ocDef;
        ResourceAttributeContainerDefinition attrContDef = attributesContainer.getDefinition();
        if (attrContDef != null) {
            ocDef = attrContDef.getComplexTypeDefinition();
        } else {
            ocDef = resourceSchema.findObjectClassDefinition(shadow.asObjectable().getObjectClass());
            if (ocDef == null) {
                throw new SchemaException("Unknown object class "+shadow.asObjectable().getObjectClass());
            }
        }

        // getting icf object class from resource object class
        ObjectClass icfObjectClass = connIdNameMapper.objectClassToConnId(shadow, getSchemaNamespace(), connectorType, BooleanUtils.isNotFalse(legacySchema));

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

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("ConnId attributes after conversion:\n{}", ConnIdUtil.dump(attributes));
            }
        } catch (SchemaException | RuntimeException ex) {
            result.recordFatalError(
                    "Error while converting resource object attributes. Reason: " + ex.getMessage(), ex);
            throw new SchemaException("Error while converting resource object attributes. Reason: "
                    + ex.getMessage(), ex);
        }

        if (attributes == null) {
            result.recordFatalError("Couldn't set attributes for icf.");
            throw new IllegalStateException("Couldn't set attributes for icf.");
        }

        List<String> icfAuxiliaryObjectClasses = new ArrayList<>();
        for (QName auxiliaryObjectClass: shadowType.getAuxiliaryObjectClass()) {
            icfAuxiliaryObjectClasses.add(
                    connIdNameMapper.objectClassToConnId(auxiliaryObjectClass, resourceSchemaNamespace,
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

        OperationResult connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".create");
        connIdResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);
        connIdResult.addArbitraryObjectCollectionAsParam("auxiliaryObjectClasses", icfAuxiliaryObjectClasses);
        connIdResult.addArbitraryObjectCollectionAsParam("attributes", attributes);
        connIdResult.addArbitraryObjectAsParam("options", options);
        connIdResult.addContext("connector", connIdConnectorFacade.getClass());

        Uid uid = null;
        try {

            // CALL THE ConnId FRAMEWORK
            InternalMonitor.recordConnectorOperation("create");
            InternalMonitor.recordConnectorModification("create");
            recordIcfOperationStart(reporter, ProvisioningOperation.ICF_CREATE, ocDef, null);        // TODO provide object name
            uid = connIdConnectorFacade.create(icfObjectClass, attributes, options);
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_CREATE, ocDef, uid);

        } catch (Throwable ex) {
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_CREATE, ocDef, ex, null);        // TODO name
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

        Collection<ResourceAttribute<?>> identifiers = ConnIdUtil.convertToIdentifiers(uid,
                attributesContainer.getDefinition().getComplexTypeDefinition(), resourceSchema);
        for (ResourceAttribute<?> identifier: identifiers) {
            attributesContainer.getValue().addReplaceExisting(identifier);
        }
        connIdResult.recordSuccess();

        result.computeStatus();
        return AsynchronousOperationReturnValue.wrap(attributesContainer.getAttributes(), result);
    }

    private void validateShadow(PrismObject<? extends ShadowType> shadow, String operation,
            boolean requireUid) {
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
    public AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> modifyObject(
                    ResourceObjectIdentification identification,
                    PrismObject<ShadowType> shadow,
                    @NotNull Collection<Operation> changes,
                    ConnectorOperationOptions options,
                    StateReporter reporter,
                    OperationResult parentResult)
                            throws ObjectNotFoundException, CommunicationException,
                                GenericFrameworkException, SchemaException, SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException {

        OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName() + ".modifyObject");
        result.addArbitraryObjectAsParam("identification", identification);
        result.addArbitraryObjectCollectionAsParam("changes", changes);

        if (changes.isEmpty()) {
            LOGGER.info("No modifications for connector object specified. Skipping processing.");
            result.recordNotApplicableIfUnknown();
            return AsynchronousOperationReturnValue.wrap(new ArrayList<>(0), result);
        }

        ObjectClass objClass = connIdNameMapper.objectClassToConnId(identification.getObjectClassDefinition(), getSchemaNamespace(), connectorType, legacySchema);

        Uid uid;
        try {
            uid = getUid(identification);
        } catch (SchemaException e) {
            result.recordFatalError(e);
            throw e;
        }

        if (uid == null) {
            result.recordFatalError("Cannot detemine UID from identification: " + identification);
            throw new IllegalArgumentException("Cannot detemine UID from identification: " + identification);
        }

        if (supportsDeltaUpdateOp()) {
            return modifyObjectDelta(identification, objClass, uid, shadow, changes, options, reporter, result);
        } else {
            return modifyObjectUpdate(identification, objClass, uid, shadow, changes, options, reporter, result);
        }
    }

    /**
     * Modifies object by using new delta update operations.
     */
    private AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> modifyObjectDelta(
                    ResourceObjectIdentification identification,
                    ObjectClass objClass,
                    Uid uid,
                    PrismObject<ShadowType> shadow,
                    Collection<Operation> changes,
                    ConnectorOperationOptions options,
                    StateReporter reporter,
                    OperationResult result)
                            throws ObjectNotFoundException, CommunicationException,
                                GenericFrameworkException, SchemaException, SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException {

        ObjectClassComplexTypeDefinition objectClassDef = identification.getObjectClassDefinition();
        Set<AttributeDelta> sideEffect = new HashSet<>();
        String originalUid = uid.getUidValue();

        DeltaModificationConverter converter = new DeltaModificationConverter();
        converter.setChanges(changes);
        converter.setConnectorDescription(description);
        converter.setConnectorType(connectorType);
        converter.setConnIdNameMapper(connIdNameMapper);
        converter.setObjectClassDef(objectClassDef);
        converter.setProtector(protector);
        converter.setResourceSchema(resourceSchema);
        converter.setResourceSchemaNamespace(resourceSchemaNamespace);
        converter.setOptions(options);

        try {

            converter.convert();

        } catch (SchemaException | RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("converted attributesDelta:\n {}", converter.debugDump(1));
        }

        OperationResult connIdResult = null;

        Set<AttributeDelta> attributesDelta = converter.getAttributesDelta();
        if (!attributesDelta.isEmpty()) {
            OperationOptions connIdOptions = createConnIdOptions(options, changes);
            connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".updateDelta");
            connIdResult.addParam("objectClass", objectClassDef.toString());
            connIdResult.addParam("uid", uid==null? "null":uid.getUidValue());
            connIdResult.addParam("attributesDelta", attributesDelta.toString());
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", connIdConnectorFacade.getClass());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Invoking ICF update(), objectclass={}, uid={}, attributes delta: {}",
                        objClass, uid, dumpAttributesDelta(attributesDelta));
            }

            try {
                InternalMonitor.recordConnectorOperation("update");
                InternalMonitor.recordConnectorModification("update");
                recordIcfOperationStart(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);

                // Call ConnId
                sideEffect = connIdConnectorFacade.updateDelta(objClass, uid, attributesDelta, connIdOptions);

                recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, null, uid);
                connIdResult.recordSuccess();
            } catch (Throwable ex) {
                recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, ex, uid);
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
        result.computeStatus();

        Collection<PropertyModificationOperation> sideEffectChanges = new ArrayList<>();
        if(sideEffect == null || sideEffect.isEmpty()) {
            return AsynchronousOperationReturnValue.wrap(sideEffectChanges, result);
        } else {
            boolean changeUid = false, changeName = false;
            for (AttributeDelta attrDeltaSideEffect : sideEffect){
                String name = attrDeltaSideEffect.getName();
                if(name.equals(Uid.NAME)){
                    Uid newUid = new Uid((String)attrDeltaSideEffect.getValuesToReplace().get(0));
                    PropertyDelta<String> uidDelta = createUidDelta(newUid, getUidDefinition(identification));
                    PropertyModificationOperation uidMod = new PropertyModificationOperation(uidDelta);
                    sideEffectChanges.add(uidMod);

                    replaceUidValue(identification, newUid);
                } else if(name.equals(Name.NAME)){
                        Name newName = new Name((String)attrDeltaSideEffect.getValuesToReplace().get(0));
                        PropertyDelta<String> nameDelta = createNameDelta(newName, getNameDefinition(identification));
                        PropertyModificationOperation nameMod = new PropertyModificationOperation(nameDelta);
                        sideEffectChanges.add(nameMod);

                        replaceNameValue(identification, new Name((String)attrDeltaSideEffect.getValuesToReplace().get(0)));
                } else {
                    ResourceAttributeDefinition definition = objectClassDef.findAttributeDefinition(name);

                    if(definition == null){
                        throw new ObjectNotFoundException("Returned delta attribute with name: "+ name +" for which, has not been found ResourceAttributeDefinition.");
                    }
                    PropertyDelta<Object> delta = prismContext.deltaFactory().property().create(ItemPath.create(ShadowType.F_ATTRIBUTES,
                            definition.getItemName()), definition);
                    if(attrDeltaSideEffect.getValuesToReplace() != null){
                        delta.setRealValuesToReplace(attrDeltaSideEffect.getValuesToReplace().get(0));
                    } else {
                        if(attrDeltaSideEffect.getValuesToAdd() != null){
                            for(Object value : attrDeltaSideEffect.getValuesToAdd()){
                                delta.addRealValuesToAdd(value);
                            }

                        }
                        if(attrDeltaSideEffect.getValuesToRemove() != null){
                            for(Object value : attrDeltaSideEffect.getValuesToRemove()){
                                delta.addRealValuesToDelete(value);
                            }
                        }
                    }
                    PropertyModificationOperation modification = new PropertyModificationOperation(delta);
                    sideEffectChanges.add(modification);

                }
            }
        }

        return AsynchronousOperationReturnValue.wrap(sideEffectChanges, result);
    }

    /**
     * Modifies object by using old add/delete/replace attribute operations.
     */
    private AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> modifyObjectUpdate(
            ResourceObjectIdentification identification,
            ObjectClass objClass,
            Uid uid,
            PrismObject<ShadowType> shadow,
            Collection<Operation> changes,
            ConnectorOperationOptions options,
            StateReporter reporter,
            OperationResult result)
                    throws ObjectNotFoundException, CommunicationException,
                        GenericFrameworkException, SchemaException, SecurityViolationException, ObjectAlreadyExistsException, PolicyViolationException {


        ObjectClassComplexTypeDefinition objectClassDef = identification.getObjectClassDefinition();
        String originalUid = uid.getUidValue();

        UpdateModificationConverter converter = new UpdateModificationConverter();
        converter.setChanges(changes);
        converter.setConnectorDescription(description);
        converter.setConnectorType(connectorType);
        converter.setConnIdNameMapper(connIdNameMapper);
        converter.setObjectClassDef(objectClassDef);
        converter.setProtector(protector);
        converter.setResourceSchema(resourceSchema);
        converter.setResourceSchemaNamespace(resourceSchemaNamespace);

        try {

            converter.convert();

        } catch (SchemaException | RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("converted attributes:\n{}", converter.debugDump(1));
        }

        // Needs three complete try-catch blocks because we need to create
        // icfResult for each operation
        // and handle the faults individually

        OperationResult connIdResult = null;
        Set<Attribute> attributesToAdd = converter.getAttributesToAdd();
        if (!attributesToAdd.isEmpty()) {

            OperationOptions connIdOptions = createConnIdOptions(options, changes);
            connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".addAttributeValues");
            connIdResult.addArbitraryObjectAsParam("objectClass", objectClassDef);
            connIdResult.addParam("uid", uid.getUidValue());
            connIdResult.addArbitraryObjectAsParam("attributes", attributesToAdd);
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", connIdConnectorFacade.getClass());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Invoking ConnId addAttributeValues(), objectclass={}, uid={}, attributes: {}",
                        objClass, uid, dumpAttributes(attributesToAdd));
            }

            InternalMonitor.recordConnectorOperation("addAttributeValues");
            InternalMonitor.recordConnectorModification("addAttributeValues");

            try {
                // Invoking ConnId
                recordIcfOperationStart(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);
                uid = connIdConnectorFacade.addAttributeValues(objClass, uid, attributesToAdd, connIdOptions);
                recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, null, uid);

                connIdResult.recordSuccess();

            } catch (Throwable ex) {
                recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, ex, uid);
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
            connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".update");
            connIdResult.addArbitraryObjectAsParam("objectClass", objectClassDef);
            connIdResult.addParam("uid", uid==null?"null":uid.getUidValue());
            connIdResult.addArbitraryObjectAsParam("attributes", attributesToUpdate);
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", connIdConnectorFacade.getClass());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Invoking ConnId update(), objectclass={}, uid={}, attributes: {}",
                        objClass, uid, dumpAttributes(attributesToUpdate));
            }

            try {

                // Call ConnId
                InternalMonitor.recordConnectorOperation("update");
                InternalMonitor.recordConnectorModification("update");
                recordIcfOperationStart(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);
                uid = connIdConnectorFacade.update(objClass, uid, attributesToUpdate, connIdOptions);
                recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, null, uid);

                connIdResult.recordSuccess();
            } catch (Throwable ex) {
                recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, ex, uid);
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
            connIdResult = result.createSubresult(ConnectorFacade.class.getName() + ".removeAttributeValues");
            connIdResult.addArbitraryObjectAsParam("objectClass", objectClassDef);
            connIdResult.addParam("uid", uid.getUidValue());
            connIdResult.addArbitraryObjectAsParam("attributes", attributesToRemove);
            connIdResult.addArbitraryObjectAsParam("options", connIdOptions);
            connIdResult.addContext("connector", connIdConnectorFacade.getClass());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Invoking ConnId removeAttributeValues(), objectclass={}, uid={}, attributes: {}",
                        objClass, uid, dumpAttributes(attributesToRemove));
            }

            InternalMonitor.recordConnectorOperation("removeAttributeValues");
            InternalMonitor.recordConnectorModification("removeAttributeValues");
            recordIcfOperationStart(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, uid);

            try {

                uid = connIdConnectorFacade.removeAttributeValues(objClass, uid, attributesToRemove, connIdOptions);
                recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, null, uid);
                connIdResult.recordSuccess();

            } catch (Throwable ex) {
                recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_UPDATE, objectClassDef, ex, uid);
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

        Collection<PropertyModificationOperation> sideEffectChanges = new ArrayList<>();
        if (!originalUid.equals(uid.getUidValue())) {
            // UID was changed during the operation, this is most likely a
            // rename
            PropertyDelta<String> uidDelta = createUidDelta(uid, getUidDefinition(identification));
            PropertyModificationOperation uidMod = new PropertyModificationOperation(uidDelta);
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

    private PropertyDelta<String> createNameDelta(Name name, ResourceAttributeDefinition nameDefinition) {
        PropertyDelta<String> uidDelta = prismContext.deltaFactory().property().create(ItemPath.create(ShadowType.F_ATTRIBUTES, nameDefinition.getItemName()),
                nameDefinition);
        uidDelta.setRealValuesToReplace(name.getNameValue());
        return uidDelta;
    }

    private PropertyDelta<String> createUidDelta(Uid uid, ResourceAttributeDefinition uidDefinition) {
        PropertyDelta<String> uidDelta = prismContext.deltaFactory().property().create(ItemPath.create(ShadowType.F_ATTRIBUTES, uidDefinition.getItemName()),
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
    public AsynchronousOperationResult deleteObject(ObjectClassComplexTypeDefinition objectClass, PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, StateReporter reporter,
                             OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException {
        Validate.notNull(objectClass, "No objectclass");

        OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
                + ".deleteObject");
        result.addArbitraryObjectCollectionAsParam("identifiers", identifiers);

        ObjectClass objClass = connIdNameMapper.objectClassToConnId(objectClass, getSchemaNamespace(), connectorType, legacySchema);
        Uid uid;
        try {
            uid = getUid(objectClass, identifiers);
        } catch (SchemaException e) {
            result.recordFatalError(e);
            throw e;
        }

        OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".delete");
        icfResult.addArbitraryObjectAsParam("uid", uid);
        icfResult.addArbitraryObjectAsParam("objectClass", objClass);
        icfResult.addContext("connector", connIdConnectorFacade.getClass());

        try {

            InternalMonitor.recordConnectorOperation("delete");
            InternalMonitor.recordConnectorModification("delete");
            recordIcfOperationStart(reporter, ProvisioningOperation.ICF_DELETE, objectClass, uid);
            connIdConnectorFacade.delete(objClass, uid, new OperationOptionsBuilder().build());
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_DELETE, objectClass, null, uid);

            icfResult.recordSuccess();

        } catch (Throwable ex) {
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_DELETE, objectClass, ex, uid);
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
    public PrismProperty<?> deserializeToken(Object serializedToken) {
        return TokenUtil.createTokenPropertyFromRealValue(serializedToken, prismContext);
    }

    @Override
    public <T> PrismProperty<T> fetchCurrentToken(ObjectClassComplexTypeDefinition objectClassDef, StateReporter reporter,
            OperationResult parentResult) throws CommunicationException, GenericFrameworkException {

        OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
                + ".fetchCurrentToken");
        result.addArbitraryObjectAsParam("objectClass", objectClassDef);

        ObjectClass icfObjectClass;
        if (objectClassDef == null) {
            icfObjectClass = ObjectClass.ALL;
        } else {
            icfObjectClass = connIdNameMapper.objectClassToConnId(objectClassDef, getSchemaNamespace(), connectorType, legacySchema);
        }

        OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".sync");
        icfResult.addContext("connector", connIdConnectorFacade.getClass());
        icfResult.addArbitraryObjectAsParam("icfObjectClass", icfObjectClass);

        SyncToken syncToken;
        try {
            InternalMonitor.recordConnectorOperation("getLatestSyncToken");
            recordIcfOperationStart(reporter, ProvisioningOperation.ICF_GET_LATEST_SYNC_TOKEN, objectClassDef);
            syncToken = connIdConnectorFacade.getLatestSyncToken(icfObjectClass);
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_LATEST_SYNC_TOKEN, objectClassDef);
            icfResult.recordSuccess();
            icfResult.addReturn("syncToken", syncToken==null?null:String.valueOf(syncToken.getValue()));
        } catch (Throwable ex) {
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_GET_LATEST_SYNC_TOKEN, objectClassDef, ex);
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

        if (syncToken == null) {
            result.recordWarning("Resource have not provided a current sync token");
            return null;
        }

        result.recordSuccess();
        return TokenUtil.createTokenProperty(syncToken, prismContext);
    }

    @Override
    public UcfFetchChangesResult fetchChanges(ObjectClassComplexTypeDefinition objectClass, PrismProperty<?> initialTokenProperty,
            AttributesToReturn attrsToReturn, Integer maxChanges, StateReporter reporter,
            @NotNull UcfLiveSyncChangeListener changeListener, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException {

        OperationResult result = parentResult.subresult(OP_FETCH_CHANGES)
                .addArbitraryObjectAsContext("objectClass", objectClass)
                .addArbitraryObjectAsParam("initialToken", initialTokenProperty)
                .build();
        try {
            SyncToken initialToken = TokenUtil.getSyncToken(initialTokenProperty);
            LOGGER.trace("Initial token: {}", initialToken == null ? null : initialToken.getValue());

            // get icf object class
            ObjectClass requestConnIdObjectClass;
            if (objectClass == null) {
                requestConnIdObjectClass = ObjectClass.ALL;
            } else {
                requestConnIdObjectClass = connIdNameMapper
                        .objectClassToConnId(objectClass, getSchemaNamespace(), connectorType, legacySchema);
            }

            OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
            if (objectClass != null) {
                convertToIcfAttrsToGet(objectClass, attrsToReturn, optionsBuilder);
            }
            OperationOptions options = optionsBuilder.build();

            AtomicInteger deltasProcessed = new AtomicInteger(0);

            Thread callerThread = Thread.currentThread();

            SyncDeltaConverter changeConverter = new SyncDeltaConverter(this, objectClass);

            AtomicBoolean allChangesFetched = new AtomicBoolean(true);
            UcfFetchChangesResult fetchChangesResult;

            OperationResult connIdResult = result.subresult(ConnectorFacade.class.getName() + ".sync")
                    .addContext("connector", connIdConnectorFacade.getClass())
                    .addArbitraryObjectAsParam("objectClass", requestConnIdObjectClass)
                    .addArbitraryObjectAsParam("initialToken", initialToken)
                    .build();
            try {

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

                    recordIcfOperationSuspend(reporter, ProvisioningOperation.ICF_SYNC, objectClass);
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

                        boolean doContinue = canContinue && canRun(reporter) && (maxChanges == null || maxChanges == 0 || sequentialNumber < maxChanges);
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
                        recordIcfOperationResume(reporter, ProvisioningOperation.ICF_SYNC, objectClass);
                    }
                };

                SyncToken finalToken;
                try {
                    InternalMonitor.recordConnectorOperation("sync");
                    recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SYNC, objectClass);
                    finalToken = connIdConnectorFacade.sync(requestConnIdObjectClass, initialToken, syncHandler, options);
                    // Note that finalToken value is not quite reliable. The SyncApiOp documentation is not clear on its semantics;
                    // it is only from SyncTokenResultsHandler (SPI) documentation and SyncImpl class that we know this value is
                    // non-null when all changes were fetched. And some of the connectors return null even then.
                    LOGGER.trace("connector sync method returned: {}", finalToken);
                    connIdResult.computeStatus();
                    connIdResult.cleanupResult();
                    connIdResult.addReturn(OperationResult.RETURN_COUNT, deltasProcessed.get());
                    recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SYNC, objectClass);
                } catch (Throwable ex) {
                    recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SYNC, objectClass, ex);
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
                if (!canRun(reporter)) {
                    result.recordStatus(OperationResultStatus.SUCCESS, "Interrupted by task suspension");
                }

                if (allChangesFetched.get()) {
                    // We might consider finalToken value here. I.e. it it's non null, we could declare all changes to be fetched.
                    // But as mentioned above, this is not supported explicitly in SyncApiOp. So let's be a bit conservative.
                    LOGGER.trace("All changes were fetched; with finalToken = {}", finalToken);
                    fetchChangesResult = new UcfFetchChangesResult(true, TokenUtil.createTokenProperty(finalToken, prismContext));
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

    private boolean canRun(StateReporter reporter) {
        RunningTask task = reporter != null && reporter.getTask() instanceof RunningTask
                ? ((RunningTask) reporter.getTask()) : null;
        return task == null || task.canRun();
    }

    @Override
    public void test(OperationResult parentResult) {

        OperationResult connectionResult = parentResult
                .createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
        connectionResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ConnectorInstanceConnIdImpl.class);
        connectionResult.addContext("connector", connectorType);

        try {
            InternalMonitor.recordConnectorOperation("test");
            connIdConnectorFacade.test();
            connectionResult.recordSuccess();
        } catch (UnsupportedOperationException ex) {
            // Connector does not support test connection.
            connectionResult.recordStatus(OperationResultStatus.NOT_APPLICABLE,
                    "Operation not supported by the connector", ex);
            // Do not rethrow. Recording the status is just OK.
        } catch (Throwable icfEx) {
            Throwable midPointEx = processConnIdException(icfEx, this, connectionResult);
            connectionResult.recordFatalError(midPointEx);
        }
    }

    @Override
    public SearchResultMetadata search(final ObjectClassComplexTypeDefinition objectClassDefinition,
            final ObjectQuery query, final ShadowResultHandler handler, AttributesToReturn attributesToReturn,
            PagedSearchCapabilityType pagedSearchConfiguration, SearchHierarchyConstraints searchHierarchyConstraints,
            FetchErrorReportingMethodType errorReportingMethod, StateReporter reporter, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SecurityViolationException, SchemaException,
                        ObjectNotFoundException {

        // Result type for this operation
        final OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
                + ".search");
        result.addArbitraryObjectAsParam("objectClass", objectClassDefinition);
        result.addContext("connector", connectorType);

        validateConnectorFacade(result);

        if (objectClassDefinition == null) {
            result.recordFatalError("Object class not defined");
            throw new IllegalArgumentException("objectClass not defined");
        }

        ObjectClass icfObjectClass = connIdNameMapper.objectClassToConnId(objectClassDefinition, getSchemaNamespace(), connectorType, legacySchema);
        if (icfObjectClass == null) {
            IllegalArgumentException ex = new IllegalArgumentException(
                    "Unable to determine object class from QName " + objectClassDefinition
                            + " while attempting to search objects by "
                            + ObjectTypeUtil.toShortString(connectorType));
            result.recordFatalError("Unable to determine object class", ex);
            throw ex;
        }
        final PrismObjectDefinition<ShadowType> objectDefinition = toShadowDefinition(objectClassDefinition);

        if (pagedSearchConfiguration == null) {
            pagedSearchConfiguration = getCapability(PagedSearchCapabilityType.class);
        }

        boolean useConnectorPaging = pagedSearchConfiguration != null;
        if (!useConnectorPaging && query != null && query.getPaging() != null &&
                (query.getPaging().getOffset() != null || query.getPaging().getMaxSize() != null)) {
            InternalMonitor.recordCount(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
        }

        final Holder<Integer> countHolder = new Holder<>(0);

        ResultsHandler connIdHandler = new ResultsHandler() {
            @Override
            public boolean handle(ConnectorObject connectorObject) {
                Validate.notNull(connectorObject, "null connector object"); // todo apply error reporting method?

                recordIcfOperationSuspend(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);
                try {
                    int count = countHolder.getValue();
                    countHolder.setValue(count + 1);
                    if (!useConnectorPaging) {
                        // TODO allow offset or maxSize be null
                        if (query != null && query.getPaging() != null && query.getPaging().getOffset() != null
                                && query.getPaging().getMaxSize() != null) {
                            if (count < query.getPaging().getOffset()) {
                                return true;
                            }
                            if (count == (query.getPaging().getOffset() + query.getPaging().getMaxSize())) {
                                return false;
                            }
                        }
                    }

                    PrismObject<ShadowType> resourceObject = connIdConvertor.convertToResourceObject(connectorObject,
                            objectDefinition, false, caseIgnoreAttributeNames, legacySchema, errorReportingMethod, result);
                    return handler.handle(resourceObject);

                } catch (SchemaException e) {
                    throw new IntermediateException(e);
                } finally {
                    recordIcfOperationResume(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);
                }
            }

            @Override
            public String toString() {
                return "(midPoint searching result handler)";
            }
        };

        OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();

        try {
            convertToIcfAttrsToGet(objectClassDefinition, attributesToReturn, optionsBuilder);
            if (query != null && query.isAllowPartialResults()) {
                optionsBuilder.setAllowPartialResults(query.isAllowPartialResults());
            }
            // preparing paging-related options
            if (useConnectorPaging && query != null && query.getPaging() != null) {
                ObjectPaging paging = query.getPaging();
                if (paging.getOffset() != null) {
                    optionsBuilder.setPagedResultsOffset(paging.getOffset() + 1);       // ConnId API says the numbering starts at 1
                }
                if (paging.getMaxSize() != null) {
                    optionsBuilder.setPageSize(paging.getMaxSize());
                }
                QName orderByAttributeName;
                boolean isAscending;
                ItemPath orderByPath = paging.getOrderBy();
                String desc;
                if (orderByPath != null && !orderByPath.isEmpty()) {
                    orderByAttributeName = ShadowUtil.getAttributeName(orderByPath, "OrderBy path");
                    if (SchemaConstants.C_NAME.equals(orderByAttributeName)) {
                        orderByAttributeName = SchemaConstants.ICFS_NAME;
                    }
                    isAscending = paging.getDirection() != OrderDirection.DESCENDING;
                    desc = "(explicitly specified orderBy attribute)";
                } else {
                    orderByAttributeName = pagedSearchConfiguration.getDefaultSortField();
                    isAscending = pagedSearchConfiguration.getDefaultSortDirection() != OrderDirectionType.DESCENDING;
                    desc = "(default orderBy attribute from capability definition)";
                }
                if (orderByAttributeName != null) {
                    String orderByIcfName = connIdNameMapper.convertAttributeNameToConnId(orderByAttributeName, objectClassDefinition, desc);
                    optionsBuilder.setSortKeys(new SortKey(orderByIcfName, isAscending));
                }
            }
            if (searchHierarchyConstraints != null) {
                ResourceObjectIdentification baseContextIdentification = searchHierarchyConstraints.getBaseContext();
                if (baseContextIdentification != null) {
                    // Only LDAP connector really supports base context. And this one will work better with
                    // DN. And DN is secondary identifier (__NAME__). This is ugly, but practical. It works around ConnId problems.
                    ResourceAttribute<?> secondaryIdentifier = baseContextIdentification.getSecondaryIdentifier();
                    if (secondaryIdentifier == null) {
                        SchemaException e = new SchemaException("No secondary identifier in base context identification "+baseContextIdentification);
                        result.recordFatalError(e);
                        throw e;
                    }
                    String secondaryIdentifierValue = secondaryIdentifier.getRealValue(String.class);
                    ObjectClass baseContextIcfObjectClass = connIdNameMapper.objectClassToConnId(baseContextIdentification.getObjectClassDefinition(), getSchemaNamespace(), connectorType, legacySchema);
                    QualifiedUid containerQualifiedUid = new QualifiedUid(baseContextIcfObjectClass, new Uid(secondaryIdentifierValue));
                    optionsBuilder.setContainer(containerQualifiedUid);
                }
                SearchHierarchyScope scope = searchHierarchyConstraints.getScope();
                if (scope != null) {
                    optionsBuilder.setScope(scope.getScopeString());
                }
            }

        } catch (SchemaException e) {
            result.recordFatalError(e);
            throw e;
        }

        // Relax completeness requirements. This is a search, not get. So it is OK to
        // return incomplete member lists and similar attributes.
        optionsBuilder.setAllowPartialAttributeValues(true);

        OperationOptions options = optionsBuilder.build();

        Filter filter;
        try {
            filter = convertFilterToIcf(query, objectClassDefinition);
        } catch (SchemaException | RuntimeException e) {
            result.recordFatalError(e);
            throw e;
        }

        // Connector operation cannot create result for itself, so we need to
        // create result for it
        OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".search");
        icfResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);

        SearchResult connIdSearchResult;
        try {

            InternalMonitor.recordConnectorOperation("search");
            recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);
            connIdSearchResult = connIdConnectorFacade.search(icfObjectClass, filter, connIdHandler, options);
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);

            icfResult.recordSuccess();
        } catch (IntermediateException inex) {
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition, inex);
            SchemaException ex = (SchemaException) inex.getCause();
            icfResult.recordFatalError(ex);
            result.recordFatalError(ex);
            throw ex;
        } catch (Throwable ex) {
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition, ex);
            Throwable midpointEx = processConnIdException(ex, this, icfResult);
            result.computeStatus();
            // Do some kind of acrobatics to do proper throwing of checked
            // exception
            if (midpointEx instanceof CommunicationException) {
                throw (CommunicationException) midpointEx;
            } else if (midpointEx instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) midpointEx;
            } else if (midpointEx instanceof GenericFrameworkException) {
                throw (GenericFrameworkException) midpointEx;
            } else if (midpointEx instanceof SchemaException) {
                throw (SchemaException) midpointEx;
            } else if (midpointEx instanceof SecurityViolationException) {
                throw (SecurityViolationException) midpointEx;
            } else if (midpointEx instanceof RuntimeException) {
                throw (RuntimeException) midpointEx;
            } else if (midpointEx instanceof Error) {
                throw (Error) midpointEx;
            } else {
                throw new SystemException("Got unexpected exception: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }

        SearchResultMetadata metadata = null;
        if (connIdSearchResult != null) {
            metadata = new SearchResultMetadata();
            metadata.setPagingCookie(connIdSearchResult.getPagedResultsCookie());
            int remainingPagedResults = connIdSearchResult.getRemainingPagedResults();
            if (remainingPagedResults >= 0) {
                int offset = 0;
                Integer connIdOffset = options.getPagedResultsOffset();
                if (connIdOffset != null && connIdOffset > 0) {
                    offset = connIdOffset - 1;
                }
                int allResults = remainingPagedResults + offset + countHolder.getValue();
                metadata.setApproxNumberOfAllResults(allResults);
            }
            if (!connIdSearchResult.isAllResultsReturned()) {
                metadata.setPartialResults(true);
            }
        }

        if (result.isUnknown()) {
            result.recordSuccess();
        }

        return metadata;
    }

    @Override
    public int count(ObjectClassComplexTypeDefinition objectClassDefinition, final ObjectQuery query, PagedSearchCapabilityType pagedSearchCapabilityType, StateReporter reporter,
                     OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, SchemaException, UnsupportedOperationException {

        // Result type for this operation
        final OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
                + ".count");
        result.addArbitraryObjectAsParam("objectClass", objectClassDefinition);
        result.addContext("connector", connectorType);

        if (objectClassDefinition == null) {
            result.recordFatalError("Object class not defined");
            throw new IllegalArgumentException("objectClass not defined");
        }

        ObjectClass icfObjectClass = connIdNameMapper.objectClassToConnId(objectClassDefinition, getSchemaNamespace(), connectorType, legacySchema);
        if (icfObjectClass == null) {
            IllegalArgumentException ex = new IllegalArgumentException(
                    "Unable to determine object class from QName " + objectClassDefinition
                            + " while attempting to search objects by "
                            + ObjectTypeUtil.toShortString(connectorType));
            result.recordFatalError("Unable to determine object class", ex);
            throw ex;
        }
        final boolean useConnectorPaging = pagedSearchCapabilityType != null;
        if (!useConnectorPaging) {
            throw new UnsupportedOperationException("ConnectorInstanceIcfImpl.count operation is supported only in combination with connector-implemented paging");
        }

        OperationOptionsBuilder optionsBuilder = new OperationOptionsBuilder();
        optionsBuilder.setAttributesToGet(Name.NAME);
        optionsBuilder.setPagedResultsOffset(1);
        optionsBuilder.setPageSize(1);
        if (pagedSearchCapabilityType.getDefaultSortField() != null) {
            String orderByIcfName = connIdNameMapper.convertAttributeNameToConnId(pagedSearchCapabilityType.getDefaultSortField(), objectClassDefinition, "(default sorting field)");
            boolean isAscending = pagedSearchCapabilityType.getDefaultSortDirection() != OrderDirectionType.DESCENDING;
            optionsBuilder.setSortKeys(new SortKey(orderByIcfName, isAscending));
        }
        OperationOptions options = optionsBuilder.build();

        // Connector operation cannot create result for itself, so we need to
        // create result for it
        OperationResult icfResult = result.createSubresult(ConnectorFacade.class.getName() + ".search");
        icfResult.addArbitraryObjectAsParam("objectClass", icfObjectClass);
        icfResult.addContext("connector", connIdConnectorFacade.getClass());

        int retval;

        try {

            Filter filter = convertFilterToIcf(query, objectClassDefinition);
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
            InternalMonitor.recordConnectorOperation("search");
            recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);
            SearchResult searchResult = connIdConnectorFacade.search(icfObjectClass, filter, connIdHandler, options);
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition);

            if (searchResult == null || searchResult.getRemainingPagedResults() == -1) {
                throw new UnsupportedOperationException("Connector does not seem to support paged searches or does not provide object count information");
            } else {
                retval = fetched.getValue() + searchResult.getRemainingPagedResults();
            }

            icfResult.recordSuccess();
        } catch (IntermediateException inex) {
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition, inex);
            SchemaException ex = (SchemaException) inex.getCause();
            icfResult.recordFatalError(ex);
            result.recordFatalError(ex);
            throw ex;
        } catch (UnsupportedOperationException uoe) {
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition, uoe);
            icfResult.recordFatalError(uoe);
            result.recordFatalError(uoe);
            throw uoe;
        } catch (Throwable ex) {
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SEARCH, objectClassDefinition, ex);
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

    private Filter convertFilterToIcf(ObjectQuery query, ObjectClassComplexTypeDefinition objectClassDefinition) throws SchemaException {
        Filter filter = null;
        if (query != null && query.getFilter() != null) {
            FilterInterpreter interpreter = new FilterInterpreter(objectClassDefinition);
            LOGGER.trace("Start to convert filter: {}", query.getFilter().debugDump());
            filter = interpreter.interpret(query.getFilter(), connIdNameMapper);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("ConnId filter: {}", ConnIdUtil.dump(filter));
            }
        }
        return filter;
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
        if (secondaryIdentifiers == null) {
            return null;
        }
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
            throw new SchemaException("More than one secondary indentifier in "+resourceObjectIdentification+", cannot detemine ConnId __NAME__");
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
    private Uid getUid(ObjectClassComplexTypeDefinition objectClass, Collection<? extends ResourceAttribute<?>> identifiers) throws SchemaException {
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
            if (objectClass.isPrimaryIdentifier(attr.getElementName())) {
                uidValue = ((ResourceAttribute<String>) attr).getValue().getValue();
            }
            if (objectClass.isSecondaryIdentifier(attr.getElementName())) {
                ResourceAttributeDefinition<?> attrDef = objectClass.findAttributeDefinition(attr.getElementName());
                String frameworkAttributeName = attrDef.getFrameworkAttributeName();
                if (Name.NAME.equals(frameworkAttributeName)) {
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

    private ResourceAttributeDefinition getNameDefinition(ResourceObjectIdentification identification) throws SchemaException {
        ResourceAttribute<String> secondaryIdentifier = identification.getSecondaryIdentifier();
        if (secondaryIdentifier == null) {
            // fallback, compatibility
            for (ResourceAttribute<?> attr : identification.getAllIdentifiers()) {
                if (attr.getElementName().equals(SchemaConstants.ICFS_NAME)) {
                    return attr.getDefinition();
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
    public Object executeScript(ExecuteProvisioningScriptOperation scriptOperation, StateReporter reporter, OperationResult parentResult) throws CommunicationException, GenericFrameworkException {

        OperationResult result = parentResult.createSubresult(ConnectorInstance.class.getName()
                + ".executeScript");

        Object output = null;
        try {

            output = executeScriptIcf(reporter, scriptOperation, result);

        } catch (CommunicationException e) {
            result.recordFatalError(e);
            throw e;
        } catch (GenericFrameworkException e) {
            result.recordFatalError(e);
            throw e;
        } catch (RuntimeException e) {
            result.recordFatalError(e);
            throw e;
        }

        result.computeStatus();

        return output;
    }

    private Object executeScriptIcf(StateReporter reporter, ExecuteProvisioningScriptOperation scriptOperation, OperationResult parentResult) throws CommunicationException, GenericFrameworkException {

        String icfOpName = null;
        if (scriptOperation.isConnectorHost()) {
            icfOpName = "runScriptOnConnector";
        } else if (scriptOperation.isResourceHost()) {
            icfOpName = "runScriptOnResource";
        } else {
            parentResult.recordFatalError("Where to execute the script?");
            throw new IllegalArgumentException("Where to execute the script?");
        }

        // convert execute script operation to the script context required from
        // the connector
        ScriptContext scriptContext = convertToScriptContext(scriptOperation);

        OperationResult icfResult = parentResult.createSubresult(ConnectorFacade.class.getName() + "." + icfOpName);
        icfResult.addContext("connector", connIdConnectorFacade.getClass());

        Object output = null;

        try {

            LOGGER.trace("Running script ({})", icfOpName);

            recordIcfOperationStart(reporter, ProvisioningOperation.ICF_SCRIPT, null);
            if (scriptOperation.isConnectorHost()) {
                InternalMonitor.recordConnectorOperation("runScriptOnConnector");
                output = connIdConnectorFacade.runScriptOnConnector(scriptContext, new OperationOptionsBuilder().build());
            } else if (scriptOperation.isResourceHost()) {
                InternalMonitor.recordConnectorOperation("runScriptOnResource");
                output = connIdConnectorFacade.runScriptOnResource(scriptContext, new OperationOptionsBuilder().build());
            }
            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SCRIPT, null);

            icfResult.recordSuccess();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Finished running script ({}), script result: {}", icfOpName, PrettyPrinter.prettyPrint(output));
            }

        } catch (Throwable ex) {

            recordIcfOperationEnd(reporter, ProvisioningOperation.ICF_SCRIPT, null, ex);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Finished running script ({}), ERROR: {}", icfOpName, ex.getMessage());
            }

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
        ScriptContext scriptContext = new ScriptContext(executeOp.getLanguage(), executeOp.getTextCode(),
                scriptArguments);
        return scriptContext;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ConnectorInstanceIcfImpl(" + connectorType + ")";
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

    private void recordIcfOperationStart(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition, Uid uid) {
        if (reporter != null) {
            reporter.recordIcfOperationStart(operation, objectClassDefinition, uid==null?null:uid.getUidValue());
        } else {
            LOGGER.warn("Couldn't record ConnId operation start as reporter is null.");
        }
    }

    private void recordIcfOperationStart(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition) {
        if (reporter != null) {
            reporter.recordIcfOperationStart(operation, objectClassDefinition, null);
        } else {
            LOGGER.warn("Couldn't record ConnId operation start as reporter is null.");
        }
    }

    private void recordIcfOperationResume(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition) {
        if (reporter != null) {
            reporter.recordIcfOperationResume(operation, objectClassDefinition);
        } else {
            LOGGER.warn("Couldn't record ConnId operation resume as reporter is null.");
        }
    }

    private void recordIcfOperationSuspend(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition) {
        if (reporter != null) {
            reporter.recordIcfOperationSuspend(operation, objectClassDefinition);
        } else {
            LOGGER.warn("Couldn't record ConnId operation suspension as reporter is null.");
        }
    }

    private void recordIcfOperationEnd(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition, Uid uid) {
        if (reporter != null) {
            reporter.recordIcfOperationEnd(operation, objectClassDefinition, null, uid==null?null:uid.getUidValue());
        } else {
            LOGGER.warn("Couldn't record ConnId operation end as reporter is null.");
        }
    }

    private void recordIcfOperationEnd(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition, Throwable ex) {
        if (reporter != null) {
            reporter.recordIcfOperationEnd(operation, objectClassDefinition, ex, null);
        } else {
            LOGGER.warn("Couldn't record ConnId operation end as reporter is null.");
        }
    }

    private void recordIcfOperationEnd(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition, Throwable ex, Uid uid) {
        if (reporter != null) {
            reporter.recordIcfOperationEnd(operation, objectClassDefinition, ex, uid==null?null:uid.getUidValue());
        } else {
            LOGGER.warn("Couldn't record ConnId operation end as reporter is null.");
        }
    }


    private void recordIcfOperationEnd(StateReporter reporter, ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDefinition) {
        if (reporter != null) {
            reporter.recordIcfOperationEnd(operation, objectClassDefinition, null, null);
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
                        PropertyDelta propertyDelta = ((PropertyModificationOperation)change).getPropertyDelta();
                        if (!propertyDelta.getPath().equivalent(SchemaConstants.PATH_PASSWORD_VALUE)) {
                            continue;
                        }
                        Collection<PrismPropertyValue<ProtectedStringType>> oldValues = propertyDelta.getEstimatedOldValues();
                        if (oldValues == null || oldValues.isEmpty()) {
                            continue;
                        }
                        ProtectedStringType oldPassword = oldValues.iterator().next().getValue();
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

    ResourceSchema getResourceSchema() {
        return resourceSchema;
    }

    boolean isCaseIgnoreAttributeNames() {
        return caseIgnoreAttributeNames;
    }
}
