/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismContainer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManagedConnectorInstance;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.repo.api.Cache;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Manages `ConnectorType` objects in repository.
 *
 * It creates new `ConnectorType` objects when a new local connector is
 * discovered, takes care of remote connector discovery, etc.
 *
 * Also manages the cache of instantiated connectors.
 *
 * @author Radovan Semancik
 */
@Component
public class ConnectorManager implements Cache, ConnectorDiscoveryListener {

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired ApplicationContext springContext;
    @Autowired private PrismContext prismContext;
    @Autowired CacheRegistry cacheRegistry;

    @PostConstruct
    public void register() {
        cacheRegistry.registerCache(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCache(this);
    }

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorManager.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(ConnectorManager.class.getName() + ".content");

    public static final String CONNECTOR_INSTANCE_CACHE_NAME = ConnectorManager.class.getName() + ".connectorInstanceCache";
    private static final String CONNECTOR_BEAN_CACHE_NAME = ConnectorManager.class.getName() + ".connectorBeanCache";

    private Collection<ConnectorFactory> connectorFactories;

    /**
     * Contains configured and initialized connector instances.
     */
    @NotNull private Map<ConfiguredConnectorCacheKey, ConfiguredConnectorInstanceEntry> connectorInstanceCache =
            new ConcurrentHashMap<>();

    /**
     * Contains IMMUTABLE connector objects with connector hosts resolved and schemas parsed.
     */
    @NotNull private Map<String, ConnectorWithSchema> connectorBeanCache = new ConcurrentHashMap<>();

    @VisibleForTesting
    private Consumer<ConnectorType> notInRepoConsumer;

    // TODO synchronization?
    public Collection<ConnectorFactory> getConnectorFactories() {
        if (connectorFactories == null) {
            String[] connectorFactoryBeanNames = springContext.getBeanNamesForType(ConnectorFactory.class);
            LOGGER.debug("Connector factories bean names: {}", Arrays.toString(connectorFactoryBeanNames));
            connectorFactories = new ArrayList<>(connectorFactoryBeanNames.length);
            for (String connectorFactoryBeanName : connectorFactoryBeanNames) {
                Object bean = springContext.getBean(connectorFactoryBeanName);
                if (bean instanceof ConnectorFactory connFactory) {
                    connectorFactories.add(connFactory);
                    connFactory.registerDiscoveryListener(this);
                } else {
                    LOGGER.error("Bean {} is not instance of ConnectorFactory, it is {}, skipping",
                            connectorFactoryBeanName, bean.getClass());
                }
            }
        }
        return connectorFactories;
    }

    private ConnectorFactory determineConnectorFactory(ConnectorType connectorBean) {
        return connectorBean != null ? determineConnectorFactory(connectorBean.getFramework()) : null;
    }

    private ConnectorFactory determineConnectorFactory(String frameworkIdentifier) {
        for (ConnectorFactory connectorFactory : getConnectorFactories()) {
            if (connectorFactory.supportsFramework(frameworkIdentifier)) {
                return connectorFactory;
            }
        }
        return null;
    }

    @NotNull ConnectorInstance getConfiguredAndInitializedConnectorInstance(
            @NotNull ConnectorSpec connectorSpec,
            boolean forceFresh,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
        return getConfiguredAndInitializedConnectorInstance(connectorSpec, forceFresh, true, result);
    }

    /**
     * Production use of {@code false} means that we will not initialize the connector, nor cache it.
     */
    @NotNull ConnectorInstance getConfiguredAndInitializedConnectorInstance(
            @NotNull ConnectorSpec connectorSpec,
            boolean forceFresh,
            boolean productionUse,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {

        try {
            var connectorCacheEntry = getOrCreateConnectorInstanceCacheEntry(connectorSpec, result);
            var connectorInstance = connectorCacheEntry.getConnectorInstance();

            if (connectorCacheEntry.isConfigured()) {
                if (!doesNeedReconfiguration(connectorCacheEntry, forceFresh, connectorSpec)) {
                    // Already configured and initialized, nothing to do here.
                    return connectorInstance;
                } else if (productionUse) {
                    // Configured but needs reconfiguration (is not fresh, or forcing reconfiguration)
                    // Note that the connector is already cached, so we don't need to cache it.
                    // The reconfiguration affects the cache immediately (unlike the state before 4.9).
                    configureAndInitializeConnectorInstance(connectorInstance, connectorSpec, true, result);
                    return connectorInstance;
                } else {
                    LOGGER.trace("Non-fresh connector instance found, so the reconfiguration would be expected. "
                            + "But we need the connector instance for non-production use, so we keep the cached one intact, "
                            + "and create a new (uncached) instance: {}", connectorSpec);
                    var newConnectorInstance = createConnectorInstance(connectorSpec, result);
                    configureAndInitializeConnectorInstance(newConnectorInstance, connectorSpec, false, result);
                    return newConnectorInstance;
                }
            } else {
                // not configured and initialized yet
                LOGGER.trace("Configuring new connector instance for: {}", connectorSpec);
                configureAndInitializeConnectorInstance(connectorInstance, connectorSpec, productionUse, result);
                if (productionUse) {
                    cacheConfiguredAndInitializedConnectorInstance(connectorCacheEntry, connectorSpec);
                }
                return connectorInstance;
            }

        } catch (GenericFrameworkException e) {
            throw SystemException.unexpected(e, "when configuring or initializing connector instance");
        }
    }

    private static boolean doesNeedReconfiguration(
            ConfiguredConnectorInstanceEntry connectorCacheEntry, boolean forceFresh, ConnectorSpec connectorSpec) {
        if (forceFresh) {
            LOGGER.debug("FORCE in connector cache: reconfiguring cached connector {}", connectorSpec);
            return true;
        } else if (!connectorCacheEntry.isFreshRegardingSpec(connectorSpec)) {
            LOGGER.trace("Reconfiguring connector {} because the configuration is not fresh", connectorSpec);
            return true;
        } else {
            LOGGER.trace("Connector {} is fresh, does not need reconfiguring", connectorSpec);
            return false;
        }
    }

    /** The returned instance is configured but may or may not be initialized. It is not put into the cache. */
    @NotNull ConnectorInstance getNonProductionConnectorInstance(
            @NotNull ConnectorSpec connectorSpec,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
        return getConfiguredAndInitializedConnectorInstance(connectorSpec, false, false, result);
    }

    /**
     * Returns new, not configured, not initialized, and not cached connector instance.
     *
     * @throws ObjectNotFoundException A required object (e.g. connector or connector host) does not exist
     */
    @NotNull ConnectorInstance getUnconfiguredConnectorInstance(
            @NotNull String connectorOid,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        var connectorBean = getConnectorWithSchema(connectorOid, result).getConnector();
        return createConnectorInstance(connectorBean, connectorBean.getName().toString(), connectorBean.toString());
    }

    /**
     * Returns connector cache entry with connector instance.
     *
     * The entry may come from the cache or it may be just created and not yet cached.
     * In the latter case the connector instance is not yet configured. This is indicated by cacheEntry.configuration == null.
     *
     * No attempt is made to configure the connector instance here nor to check whether the configuration is fresh.
     * Therefore, un-configured or mis-configured connector instance may be returned.
     *
     * This method visible outside of this class to allow creating and configuring connector instances during the resource
     * test operation ({@link ResourceTestOperation}).
     *
     * @throws ObjectNotFoundException A required object (e.g. connector or connector host) does not exist
     */
    ConfiguredConnectorInstanceEntry getOrCreateConnectorInstanceCacheEntry(ConnectorSpec connectorSpec, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        ConfiguredConnectorCacheKey cacheKey = connectorSpec.getCacheKey();
        ConfiguredConnectorInstanceEntry existingCacheEntry = connectorInstanceCache.get(cacheKey);

        if (existingCacheEntry != null) {
            if (existingCacheEntry.matchesConnectorOid(connectorSpec.getConnectorOidRequired())) {
                LOGGER.trace("HIT in connector cache: returning configured connector {} from cache; it may or may not be fresh",
                        connectorSpec);
                return existingCacheEntry;
            } else {
                // This is the case that connectorRef in resource has changed. In this case we can do quite a destructive
                // changes. The operations in progress may be affected.
                LOGGER.debug("CRITICAL MISS in connector cache: found entry, but connector does not match. "
                        + "Disposing of old connector: {}", existingCacheEntry);
                connectorInstanceCache.remove(cacheKey);
                existingCacheEntry.getConnectorInstance().dispose();
            }
        }

        LOGGER.debug("MISS in connector cache: creating new unconfigured connector instance {}", connectorSpec);

        // No usable connector in cache. Let's create it - unconfigured.
        return new ConfiguredConnectorInstanceEntry(
                connectorSpec.getConnectorOidRequired(),
                createConnectorInstance(connectorSpec, result));
    }

    // Used by the tests. Does not change anything.
    @Nullable ConnectorInstance getConfiguredConnectorInstanceFromCache(ConnectorSpec connectorSpec) {
        ConfiguredConnectorCacheKey cacheKey = connectorSpec.getCacheKey();
        ConfiguredConnectorInstanceEntry cacheEntry = connectorInstanceCache.get(cacheKey);
        return cacheEntry != null ? cacheEntry.getConnectorInstance() : null;
    }

    // should only be used by this class and testConnection in Resource manager
    void cacheConfiguredAndInitializedConnectorInstance(ConfiguredConnectorInstanceEntry cacheEntry, ConnectorSpec connectorSpec) {
        // Execute some consistency checks
        stateCheck(
                cacheEntry.matchesConnectorOid(connectorSpec.getConnectorOidRequired()),
                "Connector OID mismatch: %s vs %s", cacheEntry, connectorSpec.getConnectorOid());
        var connectorConfiguration = stateNonNull(
                cacheEntry.getConnectorInstance().getCurrentConfiguration(),
                "Unconfigured connector? %s", cacheEntry);
        stateCheck(connectorConfiguration.equivalent(connectorSpec.getConnectorConfiguration()),
                "Mismatch in connector configuration? %s", cacheEntry);
        LOGGER.trace("Caching connector entry: {}", cacheEntry);
        connectorInstanceCache.put(connectorSpec.getCacheKey(), cacheEntry);
    }

    private @NotNull ConnectorInstance createConnectorInstance(ConnectorSpec connectorSpec, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        var resourceBean = connectorSpec.getResource();
        var connectorBean = getConnectorWithSchema(connectorSpec, result).getConnector();
        var connectorInstance = createConnectorInstance(
                connectorBean,
                resourceBean.getName().toString(),
                connectorSpec.toString());
        // FIXME temporary -- remove when no longer needed (MID-5931)
        if (connectorInstance instanceof AbstractManagedConnectorInstance managedConnectorInstance) {
            managedConnectorInstance.setResourceOid(resourceBean.getOid());
        }
        return connectorInstance;
    }

    private @NotNull ConnectorInstance createConnectorInstance(
            ConnectorType connectorBean, String instanceName, String instanceDesc)
            throws ObjectNotFoundException, SchemaException {
        ConnectorFactory connectorFactory = determineConnectorFactory(connectorBean);

        InternalMonitor.recordCount(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);

        ConnectorInstance connectorInstance = connectorFactory.createConnectorInstance(connectorBean, instanceName, instanceDesc);

        // This log message should be INFO level. It happens only occasionally.
        // If it happens often, it may be an indication of a problem. Therefore it is good for admin to see it.
        LOGGER.info("Created new connector instance for {}: {} v{}",
                instanceDesc, connectorBean.getConnectorType(), connectorBean.getConnectorVersion());

        return connectorInstance;
    }

    /** If production use is {@code false}, the connector is only configured, not initialized. */
    void configureAndInitializeConnectorInstance(
            ConnectorInstance connector, ConnectorSpec connectorSpec, boolean productionUse, OperationResult result)
            throws SchemaException, CommunicationException, GenericFrameworkException, ConfigurationException {

        // We generally do not consider empty configuration as a problem.
        PrismContainer<ConnectorConfigurationType> configurationContainer = connectorSpec.getConnectorConfigurationContainer();
        PrismContainerValue<ConnectorConfigurationType> configurationPcv =
                configurationContainer != null ?
                        configurationContainer.getValue() :
                        PrismContext.get().itemFactory().createContainerValue(); // TODO or should UCF accept null config?

        InternalMonitor.recordCount(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);

        ResourceType resource = connectorSpec.getResource();

        connector.configure(
                new ConnectorConfiguration(
                        configurationPcv,
                        ResourceTypeUtil.getSchemaGenerationConstraints(resource)),
                new ConnectorConfigurationOptions()
                        .doNotCache(!productionUse),
                result);

        if (productionUse) {
            connector.initialize(
                    ResourceSchemaFactory.getNativeSchema(resource),
                    connector.fetchCapabilities(result),    // fix for #10676 and #10644; when the connector is initialized the first time, we want
                                                            // to get not only native capabilities, but parsed by midPoint capabilities as well
                    result);
        }
    }

    /**
     * @return Connector bean with attached parsed schema. The connector may be immutable (if returned from cache).
     */
    @NotNull ConnectorWithSchema getConnectorWithSchema(ConnectorSpec connectorSpec, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        // TODO what about runtime-resolved connector OIDs (e.g. XmlImportTest.test033)?
        //  Currently, we need to throw a ConfigurationException here for the test to pass.
        //  E.g., IllegalStateException won't work.
        return getConnectorWithSchema(
                MiscUtil.configNonNull(
                        connectorSpec.getConnectorOid(),
                        "Connector OID missing in %s", connectorSpec),
                result);
    }

    private @NotNull ConnectorWithSchema getConnectorWithSchema(String connOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        ConnectorWithSchema cachedConnectorWithSchema = connectorBeanCache.get(connOid);
        if (cachedConnectorWithSchema != null) {
            if (!cachedConnectorWithSchema.getConnector().isImmutable()) {
                throw new IllegalStateException("Cached connector bean is not immutable: " + cachedConnectorWithSchema);
            }
            return cachedConnectorWithSchema;
        }

        PrismObject<ConnectorType> connector = repositoryService.getObject(ConnectorType.class, connOid, null, result);
        ConnectorType connectorBean = connector.asObjectable();

        if (connectorBean.getConnectorHostRef() != null) {
            // We need to resolve the connector host
            String connectorHostOid = connectorBean.getConnectorHostRef().getOid();
            PrismObject<ConnectorHostType> connectorHost =
                    repositoryService.getObject(ConnectorHostType.class, connectorHostOid, null, result);
            connectorBean.getConnectorHostRef().asReferenceValue().setObject(connectorHost);
        }

        InternalMonitor.recordCount(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
        var connectorSchema =
                MiscUtil.requireNonNull(
                        ConnectorTypeUtil.parseConnectorSchema(connectorBean),
                        () -> "No connector schema in " + connectorBean);

        ConnectorWithSchema connectorWithSchema = new ConnectorWithSchema(
                connectorBean.asPrismObject().createImmutableClone().asObjectable(),
                connectorSchema);

        connectorBeanCache.put(connOid, connectorWithSchema);
        return connectorWithSchema;
    }

    public Set<ConnectorType> discoverLocalConnectors(OperationResult result) {
        try {
            // Postpone discovery
            inactivateLocalConnectors(result);
            return discoverConnectors(null, result);
        } catch (CommunicationException e) {
            // This should never happen as no remote operation is executed -> convert to runtime exception
            throw new SystemException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Lists local connectors and makes sure that appropriate ConnectorType
     * objects for them exist in repository.
     *
     * It will never delete any repository object, even if the corresponding
     * connector cannot be found. The connector may temporarily removed, may be
     * present on a different node, manual upgrade may be needed etc.
     *
     * @return set of discovered connectors (new connectors found)
     */
    public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult)
            throws CommunicationException {

        Set<ConnectorType> discoveredConnectors = new HashSet<>();

        OperationResult result = parentResult.createSubresult(ConnectorManager.class.getName() + ".discoverConnectors");
        result.addParam("host", hostType);
        try {

            // Make sure that the provided host has an OID.
            // We need the host to have OID, so we can properly link connectors to
            // it
            if (hostType != null && hostType.getOid() == null) {
                throw new SystemException("Discovery attempt with non-persistent " + hostType);
            }

            for (ConnectorFactory connectorFactory : getConnectorFactories()) {

                Set<ConnectorType> foundConnectors;
                try {
                    foundConnectors = connectorFactory.listConnectors(hostType, result);
                } catch (CommunicationException ex) {
                    throw new CommunicationException("Discovery failed: " + ex.getMessage(), ex);
                }

                if (foundConnectors == null) {
                    LOGGER.trace("Connector factory {} discovered null connectors, skipping", connectorFactory);
                    continue;
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Got {} connectors from {}: {}", foundConnectors.size(), hostType, foundConnectors);
                }

                for (ConnectorType foundConnector : foundConnectors) {
                    LOGGER.trace("Examining connector {}", foundConnector);
                    String oid = findRepoOid(foundConnector, hostType, result);
                    if (oid != null) {
                        LOGGER.trace("Connector {} is in the repository, marking active", foundConnector);
                        // mark active
                        updateConnectorStatus(oid, true, result);
                    } else {
                        if (notInRepoConsumer != null) {
                            notInRepoConsumer.accept(foundConnector);
                        }
                        if (addConnectorToRepo(foundConnector, result, hostType)) {
                            discoveredConnectors.add(foundConnector);
                            LOGGER.info("Discovered new connector {}", foundConnector);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
        return discoveredConnectors;
    }

    private void inactivateLocalConnectors(OperationResult result) {
        // Walk all connectors, mark them inactive

        SearchResultList<PrismObject<ConnectorType>> allConnectors;
        try {
            allConnectors = repositoryService.searchObjects(ConnectorType.class, null, null, result);

        } catch (SchemaException e) {
            // FIXME: Fail properly
            throw new SystemException(e);
        }
        for (PrismObject<ConnectorType> connector : allConnectors) {
            if (connector.asObjectable().getConnectorHostRef() == null) {
                // Inactivate only if connector is local
                updateConnectorStatus(connector.getOid(), false, result);
            }
        }
    }

    private void updateConnectorStatus(String oid, boolean status, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult("updateConnectorStatus");
        try {
            LOGGER.debug("Updating connector {} availability to {}", oid, status);
            repositoryService.modifyObject(ConnectorType.class, oid, activeStatusDelta(status), result);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            // Is this skippable error?
            result.muteError();
        } finally {
            result.close();
        }
    }

    private Collection<? extends ItemDelta<?, ?>> activeStatusDelta(boolean status) {
        try {
            return prismContext.deltaFor(ConnectorType.class)
                    .item(ConnectorType.F_AVAILABLE)
                    .replace(status)
                    .asItemDeltas();
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
    }

    /**
     *
     * @return true if connector was not present in repo and was added to it
     */
    private boolean addConnectorToRepo(ConnectorType foundConnector, OperationResult result, ConnectorHostType hostType) {
        LOGGER.trace("Connector {} not in the repository, adding", foundConnector);

        if (foundConnector.getSchema() == null) {
            LOGGER.warn("Connector {} haven't provided configuration schema", foundConnector);
        }

        // Sanitize framework-supplied OID
        if (StringUtils.isNotEmpty(foundConnector.getOid())) {
            LOGGER.warn("Provisioning framework {} supplied OID for connector {}", foundConnector.getFramework(), foundConnector);
            foundConnector.setOid(null);
        }

        // Store the connector object
        String oid;
        try {
            prismContext.adopt(foundConnector);
            if (hostType == null) {
                // Its local connector, set availability to true
                foundConnector.setAvailable(true);
            }

            oid = repositoryService.addObject(foundConnector.asPrismObject(), null, result);
        } catch (ObjectAlreadyExistsException e) {
            if (isInRepo(foundConnector, hostType, result)) {
                return false;
            }
            throw new SystemException("Connector was not present in repository, but add failed", e);
        } catch (SchemaException e) {
            // If there is a schema error it must be a bug. Convert to
            // runtime exception
            LOGGER.error("Got SchemaException while not expecting it: {}", e.getMessage(), e);
            result.recordFatalError("Got SchemaException while not expecting it: " + e.getMessage(), e);
            throw new SystemException("Got SchemaException while not expecting it: " + e.getMessage(), e);
        }
        foundConnector.setOid(oid);

        // We need to "embed" connectorHost to the connectorType. The UCF does not
        // have access to repository, therefore it cannot resolve it for itself
        if (hostType != null) {
            foundConnector.getConnectorHostRef().asReferenceValue().setObject(hostType.asPrismObject());
        }
        return true;

    }

    private boolean isInRepo(ConnectorType connectorType, ConnectorHostType hostType, OperationResult result) {
        return findRepoOid(connectorType, hostType, result) != null;
    }

    private String findRepoOid(ConnectorType connectorType, ConnectorHostType hostType, OperationResult result) {
        ObjectQuery query;
        if (hostType == null) {
            query = prismContext.queryFor(ConnectorType.class)
                    .item(SchemaConstants.C_CONNECTOR_FRAMEWORK).eq(connectorType.getFramework())
                    .and().item(SchemaConstants.C_CONNECTOR_CONNECTOR_TYPE).eq(connectorType.getConnectorType())
                    .and().item(ConnectorType.F_CONNECTOR_HOST_REF).isNull()
                    .build();
        } else {
            query = prismContext.queryFor(ConnectorType.class)
                    .item(SchemaConstants.C_CONNECTOR_FRAMEWORK).eq(connectorType.getFramework())
                    .and().item(SchemaConstants.C_CONNECTOR_CONNECTOR_TYPE).eq(connectorType.getConnectorType())
                    .and().item(ConnectorType.F_CONNECTOR_HOST_REF).ref(hostType.getOid(), ConnectorHostType.COMPLEX_TYPE)
                    .build();
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Looking for connector in repository:\n{}", query.debugDump(1));
        }

        List<PrismObject<ConnectorType>> foundConnectors;
        try {
            foundConnectors = repositoryService.searchObjects(ConnectorType.class, query, null, result);
        } catch (SchemaException e) {
            // If there is a schema error it must be a bug. Convert to runtime exception
            LOGGER.error("Got SchemaException while not expecting it: " + e.getMessage(), e);
            result.recordFatalError("Got SchemaException while not expecting it: " + e.getMessage(), e);
            throw new SystemException("Got SchemaException while not expecting it: " + e.getMessage(), e);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Found repository connectors:\n{}", DebugUtil.debugDump(foundConnectors, 1));
        }

        if (foundConnectors.isEmpty()) {
            // Nothing found, the connector is not in the repo
            return null;
        }

        String foundOid = null;
        for (PrismObject<ConnectorType> foundConnector : foundConnectors) {
            if (compareConnectors(connectorType.asPrismObject(), foundConnector)) {
                if (foundOid != null) {
                    // More than one connector matches. Inconsistent repo state. Log error.
                    result.recordPartialError("Found more than one connector that matches " + connectorType.getFramework()
                            + " : " + connectorType.getConnectorType() + " : " + connectorType.getVersion() + ". OIDs "
                            + foundConnector.getOid() + " and " + foundOid + ". Inconsistent database state.");
                    LOGGER.error("Found more than one connector that matches " + connectorType.getFramework() + " : "
                            + connectorType.getConnectorType() + " : " + connectorType.getVersion() + ". OIDs "
                            + foundConnector.getOid() + " and " + foundOid + ". Inconsistent database state.");
                    // But continue working otherwise. This is probably not critical.
                    return foundOid;
                }
                foundOid = foundConnector.getOid();
            }
        }

        return foundOid;
    }

    private boolean compareConnectors(PrismObject<ConnectorType> prismA, PrismObject<ConnectorType> prismB) {
        ConnectorType a = prismA.asObjectable();
        ConnectorType b = prismB.asObjectable();
        if (!a.getFramework().equals(b.getFramework())) {
            return false;
        }
        if (!a.getConnectorType().equals(b.getConnectorType())) {
            return false;
        }
        if (!compareConnectorHost(a, b)) {
            return false;
        }
        if (a.getConnectorVersion() == null && b.getConnectorVersion() == null) {
            // Both connectors without version. This is OK.
            return true;
        }
        if (a.getConnectorVersion() != null && b.getConnectorVersion() != null) {
            // Both connectors with version. This is OK.
            return a.getConnectorVersion().equals(b.getConnectorVersion());
        }
        // One connector has version and other does not. This is inconsistency
        LOGGER.error("Inconsistent representation of ConnectorType, one has connectorVersion and other does not. OIDs: "
                + a.getOid() + " and " + b.getOid());
        // Obviously they don't match
        return false;
    }

    private boolean compareConnectorHost(ConnectorType a, ConnectorType b) {
        if (a.getConnectorHostRef() == null && b.getConnectorHostRef() == null) {
            return true;
        }
        if (a.getConnectorHostRef() == null || b.getConnectorHostRef() == null) {
            return false;
        }
        return a.getConnectorHostRef().getOid().equals(b.getConnectorHostRef().getOid());
    }

    public String getConnIdFrameworkVersion() {
        ConnectorFactory connIdConnectorFactory = stateNonNull(
                determineConnectorFactory(SchemaConstants.ICF_FRAMEWORK_URI),
                "ConnId connector factory not present");
        return connIdConnectorFactory.getFrameworkVersion();
    }

    public void connectorFrameworkSelfTest(OperationResult parentTestResult, Task ignored) {
        for (ConnectorFactory connectorFactory: getConnectorFactories()) {
            connectorFactory.selfTest(parentTestResult);
        }
    }

    private void dispose() {
        Iterator<Entry<ConfiguredConnectorCacheKey, ConfiguredConnectorInstanceEntry>> i = connectorInstanceCache.entrySet().iterator();
        while (i.hasNext()) {
            Entry<ConfiguredConnectorCacheKey, ConfiguredConnectorInstanceEntry> connectorInstanceCacheEntry = i.next();
            ConnectorInstance connectorInstance = connectorInstanceCacheEntry.getValue().getConnectorInstance();
            i.remove();
            connectorInstance.dispose();
        }
    }

    public void shutdown() {
        dispose();
        if (connectorFactories != null) {
            // Skip this in the very rare case that we are shutting down before we were fully
            // initialized. This should not happen under normal circumstances.
            // Generally, do not call getConnectorFactories() from here. This is
            // spring "destroy" method. We should not work with spring context here.
            for (ConnectorFactory connectorFactory: connectorFactories) {
                connectorFactory.shutdown();
            }
        }
    }

    // TODO assess thread-safety of these invalidation methods
    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        if (type == null || type.isAssignableFrom(ConnectorType.class) || type.isAssignableFrom(ConnectorHostType.class)) {
            if (StringUtils.isEmpty(oid)) {
                dispose();
                connectorInstanceCache = new ConcurrentHashMap<>();
                connectorBeanCache = new ConcurrentHashMap<>();
            } else if (ConnectorType.class.equals(type)) {
                invalidateConnectorInstancesByOid(oid);
                invalidateConnectorBeansByOid(oid);
            } else if (ConnectorHostType.class.equals(type)) {
                List<String> connectorOids = getConnectorsForConnectorHost(oid);
                LOGGER.trace("Invalidating connectors {} because of invalidation of connector host {}", connectorOids, oid);
                for (String connectorOid : connectorOids) {
                    invalidateConnectorInstancesByOid(connectorOid);
                    invalidateConnectorBeansByOid(connectorOid);
                }
            } else {
                LOGGER.trace("Unsupported OID-specified invalidation of type={}, OID={}", type, oid);
            }
        }
    }

    private List<String> getConnectorsForConnectorHost(@NotNull String oid) {
        return connectorBeanCache.entrySet().stream()
                .filter(entry -> oid.equals(entry.getValue().getConnectorHostOid()))
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }

    private void invalidateConnectorInstancesByOid(String oid) {
        Iterator<Entry<ConfiguredConnectorCacheKey, ConfiguredConnectorInstanceEntry>> iterator =
                connectorInstanceCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<ConfiguredConnectorCacheKey, ConfiguredConnectorInstanceEntry> entry = iterator.next();
            ConfiguredConnectorInstanceEntry value = entry.getValue();
            if (value.matchesConnectorOid(oid)) {
                LOGGER.trace("Removing connector instance entry for OID={} because of its invalidation", oid);
                iterator.remove();
                value.getConnectorInstance().dispose();
            }
        }
    }

    private void invalidateConnectorBeansByOid(String oid) {
        if (connectorBeanCache.remove(oid) != null) {
            LOGGER.trace("Removed connector object with OID={} because of its invalidation", oid);
        }
    }

    @NotNull
    @Override
    public Collection<SingleCacheStateInformationType> getStateInformation() {
        return Arrays.asList(
                new SingleCacheStateInformationType()
                        .name(CONNECTOR_INSTANCE_CACHE_NAME)
                        .size(connectorInstanceCache.size()),
                new SingleCacheStateInformationType()
                        .name(CONNECTOR_BEAN_CACHE_NAME)
                        .size(connectorBeanCache.size())
        );
    }

    @Override
    public void dumpContent() {
        if (LOGGER_CONTENT.isInfoEnabled()) {
            connectorInstanceCache.forEach((k, v) -> LOGGER_CONTENT.info("Cached connector instance: {}: {}", k, v));
            connectorBeanCache.forEach((k, v) -> LOGGER_CONTENT.info("Cached connector bean: {}: {}", k, v));
        }
    }

    @Override
    public void newConnectorDiscovered(ConnectorHostType host) {
        try {
            discoverConnectors(host, new OperationResult("connectorDiscovered"));
        } catch (CommunicationException e) {
            LOGGER.error("Error occurred during discovery of connectors");
        }
    }

    @VisibleForTesting
    public void setNotFoundInRepoConsumer(Consumer<ConnectorType> consumer) {
        this.notInRepoConsumer = consumer;
    }
}
