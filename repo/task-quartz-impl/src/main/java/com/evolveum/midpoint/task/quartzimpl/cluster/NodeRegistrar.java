/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.cluster;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.Freezable;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.Cache;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.TaskConstants;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.quartzimpl.LocalNodeState;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.execution.LocalExecutionManager;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.NetworkUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Takes care about registration of the local node in repository.
 *
 * @DependsOn was added to ensure that this bean is initialized after {@link NodeRegistrar}.
 * Without it there were NPEs during initialization under some circumstances (on jenkins in tests).
 * Problem is caused by spring bean circular dependencies and @PostConstruct methods using autowired
 * fields that aren't always initialized at the time of execution.
 *
 * TODO finish review of this class
 */
@DependsOn("taskManagerConfiguration")
@Component
public class NodeRegistrar implements Cache {

    private static final Trace LOGGER = TraceManager.getTrace(NodeRegistrar.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(NodeRegistrar.class.getName() + ".content");

    private static final int SECRET_LENGTH = 20;
    private static final long SECRET_RENEWAL_PERIOD = 86400L * 1000L * 10L;

    private static final String OP_REFRESH_CACHED_LOCAL_NODE_OBJECT_ON_INVALIDATION =
            NodeRegistrar.class.getName() + ".refreshCachedLocalNodeObjectOnInvalidation";

    @Autowired private TaskManagerQuartzImpl taskManager;
    @Autowired private TaskManagerConfiguration configuration;
    @Autowired private ClusterManager clusterManager;
    @Autowired private LocalNodeState localNodeState;
    @Autowired private LocalizationService localizationService;
    @Autowired private Protector protector;
    @Autowired private LocalExecutionManager localExecutionManager;
    @Autowired private LocalScheduler localScheduler;
    @Autowired private CacheRegistry cacheRegistry;
    @Autowired private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private SchemaService schemaService;

    private String webContextPath;

    private long lastDiscovery;

    private volatile NodeOperationalStateType operationalStatus = NodeOperationalStateType.STARTING;

    private static final long DISCOVERY_RETRY = 10000L;

    /**
     * Here we keep current information about the local node object, as it is stored in the repository.
     * It should be current enough, because the object is updated through this class each 10 seconds (by default).
     *
     * If the object in repository gets corrupted (e.g. overwritten by some other node), or deleted,
     * the we keep last 'good' information here.
     *
     * It is always not null after task manager initialization ({@link TaskManagerQuartzImpl#init()} (that
     * calls {@link #initializeNode(OperationResult)}).
     *
     * It is always immutable, to ensure thread safety.
     */
    private volatile PrismObject<NodeType> cachedLocalNodeObject;

    private String discoveredUrlScheme;
    private Integer discoveredHttpPort;

    @PostConstruct
    public void initialize() {
        discoverUrlSchemeAndPort();
        cacheRegistry.registerCache(this);
    }

    @PreDestroy
    void preDestroy() {
        cacheRegistry.unregisterCache(this);
    }

    /**
     * Executes node startup registration: if Node object with a give name (node ID) exists, deletes it.
     * Then creates a new Node with the information relevant to this node.
     *
     * @return Created or updated node object (immutable)
     */
    public NodeType initializeNode(OperationResult result) throws TaskManagerInitializationException {

        NodeType nodeToBe = createLocalNodeObject(configuration);
        LOGGER.info("Registering this node in the repository as {} at {}", nodeToBe.getNodeIdentifier(), nodeToBe.getHostname());

        List<PrismObject<NodeType>> nodesInRepo;
        try {
            nodesInRepo = findNodesWithGivenName(getOrig(nodeToBe.getName()), result);
        } catch (SchemaException e) {
            throw new TaskManagerInitializationException("Node registration failed because of schema exception", e);
        }

        if (nodesInRepo.size() == 1) {
            PrismObject<NodeType> nodeInRepo = nodesInRepo.get(0);
            // copy all information that need to be preserved from the repository
            if (configuration.getTaskExecutionLimitations() != null) {
                // In this (special) case, we overwrite repository information by statically configured values.
                LOGGER.info("Using statically-defined task execution limitations for the current node");
            } else {
                // But usually we take execution limitations from the repository.
                nodeToBe.setTaskExecutionLimitations(nodeInRepo.asObjectable().getTaskExecutionLimitations());
            }
            nodeToBe.setUrlOverride(
                    MiscUtil.getFirstNonNull(
                            nodeInRepo.asObjectable().getUrlOverride(),
                            configuration.getUrl()));
            nodeToBe.setUrl(nodeInRepo.asObjectable().getUrl()); // URL is refreshed later, in cluster manager thread
            if (shouldRenewSecret(nodeInRepo.asObjectable())) {
                LOGGER.info("Renewing node secret for the current node");
            } else {
                nodeToBe.setSecret(nodeInRepo.asObjectable().getSecret());
                nodeToBe.setSecretUpdateTimestamp(nodeInRepo.asObjectable().getSecretUpdateTimestamp());
            }
            ObjectDelta<NodeType> nodeDelta = nodeInRepo.diff(nodeToBe.asPrismObject(), EquivalenceStrategy.DATA);
            LOGGER.debug("Applying delta to existing node object:\n{}", nodeDelta.debugDumpLazily());
            try {
                repositoryService.modifyObject(NodeType.class, nodeInRepo.getOid(), nodeDelta.getModifications(), result);
                LOGGER.debug("Node was successfully updated in the repository.");
                nodeToBe.setOid(nodeInRepo.getOid());
                setCachedLocalNodeObject(nodeToBe.asPrismObject());
                return nodeToBe;
            } catch (ObjectNotFoundException|SchemaException|ObjectAlreadyExistsException e) {
                LoggingUtils.logUnexpectedException(
                        LOGGER, "Couldn't update node object on system initialization; will re-create the node", e);
            }
        }

        // either there is no node, more nodes, or there was some problem during updating the node

        if (nodesInRepo.size() > 1) {
            LOGGER.warn("More than one node with the name of {}: removing all of them.", nodeToBe.getName());
        }

        for (PrismObject<NodeType> n : nodesInRepo) {
            LOGGER.debug("Removing existing NodeType with oid = {}, name = {}", n.getOid(), n.getName());
            try {
                repositoryService.deleteObject(NodeType.class, n.getOid(), result);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot remove NodeType with oid = {}, name = {}, because it does not exist.", e, n.getOid(), n.getElementName());
                // continue, because the error is not that severe (we hope so)
            }
        }

        try {
            String oid = repositoryService.addObject(nodeToBe.asPrismObject(), null, result);
            nodeToBe.setOid(oid);
            setCachedLocalNodeObject(nodeToBe.asPrismObject());
            LOGGER.debug("Node was successfully registered (created) in the repository.");
            return nodeToBe;
        } catch (ObjectAlreadyExistsException e) {
            localNodeState.setErrorState(NodeErrorStateType.NODE_REGISTRATION_FAILED);
            throw new TaskManagerInitializationException("Cannot register this node, because it already exists (this should not happen, as nodes with such a name were just removed)", e);
        } catch (SchemaException e) {
            localNodeState.setErrorState(NodeErrorStateType.NODE_REGISTRATION_FAILED);
            throw new TaskManagerInitializationException("Cannot register this node because of schema exception", e);
        }
    }

    private boolean shouldRenewSecret(NodeType nodeInRepo) {
        return nodeInRepo.getSecret() == null
                || nodeInRepo.getSecretUpdateTimestamp() == null
                || System.currentTimeMillis() >=
                XmlTypeConverter.toMillis(nodeInRepo.getSecretUpdateTimestamp()) + SECRET_RENEWAL_PERIOD;
    }

    @NotNull
    private NodeType createLocalNodeObject(TaskManagerConfiguration configuration) {
        XMLGregorianCalendar currentTime = XmlTypeConverter.createXMLGregorianCalendar();
        NodeType node = new NodeType();
        String nodeId = configuration.getNodeId();
        node.setNodeIdentifier(nodeId);
        node.setName(new PolyStringType(nodeId));
        node.setHostname(getMyHostname());
        node.getIpAddress().addAll(getMyIpAddresses());
        node.setUrlOverride(configuration.getUrl()); // overridden later (if already exists in repo)
        node.setClustered(configuration.isClustered());
        node.setOperationalState(operationalStatus);
        node.setLastCheckInTime(currentTime);
        node.setBuild(getBuildInformation());
        node.setTaskExecutionLimitations(
                computeTaskExecutionLimitations(configuration.getTaskExecutionLimitations(), configuration.getNodeId()));
        generateInternalNodeIdentifier(node);
        node.setSecretUpdateTimestamp(currentTime); // overridden later (if already exists in repo)
        node.setSecret(generateNodeSecret()); // overridden later (if already exists in repo)
        return node;
    }

    // public static because of testing
    @NotNull
    public static TaskExecutionLimitationsType computeTaskExecutionLimitations(
            TaskExecutionLimitationsType configuredLimitations, String nodeId) {
        TaskExecutionLimitationsType rv = new TaskExecutionLimitationsType();
        boolean nullGroupPresent = false;
        boolean currentNodePresent = false;
        boolean otherGroupPresent = false;
        if (configuredLimitations != null) {
            for (TaskGroupExecutionLimitationType limitation : configuredLimitations.getGroupLimitation()) {
                TaskGroupExecutionLimitationType limitationToAdd;
                if (TaskConstants.LIMIT_FOR_CURRENT_NODE.equals(limitation.getGroupName())) {
                    limitationToAdd = new TaskGroupExecutionLimitationType(limitation);
                    limitationToAdd.setGroupName(nodeId);
                } else if (TaskConstants.LIMIT_FOR_NULL_GROUP.equals(limitation.getGroupName())) {
                    limitationToAdd = new TaskGroupExecutionLimitationType(limitation);
                    limitationToAdd.setGroupName("");
                } else {
                    limitationToAdd = limitation;
                }
                rv.getGroupLimitation().add(limitationToAdd);
                if (StringUtils.isEmpty(limitationToAdd.getGroupName())) {
                    nullGroupPresent = true;
                } else if (limitationToAdd.getGroupName().equals(nodeId)) {
                    currentNodePresent = true;
                } else if (limitationToAdd.getGroupName().equals(TaskConstants.LIMIT_FOR_OTHER_GROUPS)) {
                    otherGroupPresent = true;
                }
            }
        }
        if (!nullGroupPresent) {
            rv.getGroupLimitation().add(new TaskGroupExecutionLimitationType().groupName("").limit(null));
        }
        if (!currentNodePresent) {
            rv.getGroupLimitation().add(new TaskGroupExecutionLimitationType().groupName(nodeId).limit(null));
        }
        if (!otherGroupPresent) {
            rv.getGroupLimitation().add(new TaskGroupExecutionLimitationType().groupName(TaskConstants.LIMIT_FOR_OTHER_GROUPS).limit(0));
        }
        return rv;
    }

    private void discoverUrlSchemeAndPort() {
        try {
            if (System.currentTimeMillis() < lastDiscovery + DISCOVERY_RETRY) {
                LOGGER.debug("Skipping discovery because the retry interval was not yet reached");
                return;
            }
            lastDiscovery = System.currentTimeMillis();
            LOGGER.debug("Trying to discover URL scheme and port");
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            QueryExp subQuery1 = Query.match(Query.attr("protocol"), Query.value("HTTP/1.1"));
            QueryExp subQuery2 = Query.anySubString(Query.attr("protocol"), Query.value("Http11"));
            QueryExp query = Query.or(subQuery1, subQuery2);
            Set<ObjectName> objs = mbs.queryNames(new ObjectName("Tomcat:type=Connector,*"), query);
            if (objs.isEmpty()) {
                objs = mbs.queryNames(new ObjectName("Catalina:type=Connector,*"), query);
            }
            for (ObjectName obj : objs) {
                String scheme = mbs.getAttribute(obj, "scheme").toString();
                String port = obj.getKeyProperty("port");
                LOGGER.info("Found Tomcat JMX object '{}': scheme = '{}', port = '{}'", obj, scheme, port); // todo switch to DEBUG eventually
                if (discoveredUrlScheme == null || discoveredHttpPort == null) {
                    discoveredUrlScheme = scheme;
                    discoveredHttpPort = port != null ? Integer.parseInt(port) : null;
                }
            }
        } catch (Throwable t) {
            LoggingUtils.logException(LOGGER, "Couldn't get list of local Tomcat endpoints", t);
        }
    }

    private ProtectedStringType generateNodeSecret() {
        ProtectedStringType secret;
        try {
            String plain = RandomStringUtils.randomAlphanumeric(SECRET_LENGTH);
            secret = protector.encryptString(plain);
        } catch (EncryptionException e) {
            throw new SystemException("Couldn't encrypt node secret: " + e.getMessage(), e);
        }
        return secret;
    }

    private BuildInformationType getBuildInformation() {
        BuildInformationType info = new BuildInformationType();
        info.setVersion(localizationService.translate(LocalizableMessageBuilder.buildKey("midpoint.system.version"), Locale.getDefault()));
        info.setRevision(localizationService.translate(LocalizableMessageBuilder.buildKey("midpoint.system.build"), Locale.getDefault()));
        return info;
    }

    /**
     * Generates an identifier that is used to ensure that this Node object is not (by mistake) overwritten
     * by another node in cluster. ClusterManager thread periodically checks if this identifier has not been changed.
     */
    private void generateInternalNodeIdentifier(NodeType node) {
        String id = node.getNodeIdentifier() + ":" + Math.round(Math.random() * 10000000000000.0);
        LOGGER.trace("internal node identifier generated: {}", id);
        node.setInternalNodeIdentifier(id);
    }

    /**
     * Registers the node going down (sets running attribute to false).
     */
    void recordNodeShutdown(OperationResult result) {
        String nodeName = configuration.getNodeId();
        String nodeOid = getCachedLocalNodeObjectOid();
        LOGGER.trace("Registering this node shutdown (name {}, oid {})", nodeName, nodeOid);
        try {
            setLocalNodeOperationalStatus(NodeOperationalStateType.DOWN);
            List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(NodeType.class)
                    .item(NodeType.F_OPERATIONAL_STATE).replace(operationalStatus)
                    .item(NodeType.F_LAST_CHECK_IN_TIME).replace(XmlTypeConverter.createXMLGregorianCalendar())
                    .asItemDeltas();
            repositoryService.modifyObject(NodeType.class, nodeOid, modifications, result);
            LOGGER.trace("Node shutdown successfully registered.");
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot register shutdown of this node (name {}, oid {}), because it does not exist.", e, nodeName, nodeOid);
            // we do not set error flag here, because we hope that on a node startup the registration would (perhaps) succeed
        } catch (ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot register shutdown of this node (name {}, oid {}).", e, nodeName, nodeOid);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot register shutdown of this node (name {}, oid {}) due to schema exception.", e, nodeName, nodeOid);
        }
    }

    /**
     * Updates registration of this node (runs periodically within ClusterManager thread and on system startup).
     */
    void updateNodeObject(OperationResult result) {
        String nodeOid = getCachedLocalNodeObjectOid();
        String nodeName = configuration.getNodeId();
        try {
            refreshCachedLocalNodeObject(nodeOid, result);
            LOGGER.trace("Updating this node registration:\n{}", cachedLocalNodeObject.debugDumpLazily());

            XMLGregorianCalendar currentTime = XmlTypeConverter.createXMLGregorianCalendar();
            String myUrl = getMyUrl();
            LOGGER.debug("My intra-cluster communication URL is '{}'", myUrl);
            List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(NodeType.class)
                    .item(NodeType.F_HOSTNAME).replace(getMyHostname())
                    .item(NodeType.F_IP_ADDRESS).replaceRealValues(getMyIpAddresses())
                    .item(NodeType.F_LAST_CHECK_IN_TIME).replace(currentTime)
                    .item(NodeType.F_OPERATIONAL_STATE).replace(operationalStatus)
                    .asItemDeltas();
            if (shouldRenewSecret(cachedLocalNodeObject.asObjectable())) {
                LOGGER.info("Renewing node secret for the current node");
                modifications.addAll(prismContext.deltaFor(NodeType.class)
                        .item(NodeType.F_SECRET).replace(generateNodeSecret())
                        .item(NodeType.F_SECRET_UPDATE_TIMESTAMP).replace(currentTime)
                        .asItemDeltas());
            }
            if (myUrl != null) {    // i.e. if known (might not be known during startup)
                modifications.add(prismContext.deltaFor(NodeType.class)
                        .item(NodeType.F_URL).replace(myUrl)
                        .asItemDelta());
                String oldUrl = cachedLocalNodeObject.asObjectable().getUrl();
                if (!myUrl.equals(oldUrl)) {
                    LOGGER.info("Changing node URL from {} to {}", oldUrl, myUrl);
                }
            }
            repositoryService.modifyObject(NodeType.class, nodeOid, modifications, result);
            LOGGER.trace("Node registration successfully updated.");
            refreshCachedLocalNodeObject(nodeOid, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot update registration of this node (name {}, oid {}), because it "
                            + "does not exist in repository. It is probably caused by cluster misconfiguration (other "
                            + "node rewriting the Node object?) Stopping the scheduler.", e, nodeName, nodeOid);
            if (localNodeState.getErrorState() == NodeErrorStateType.OK) {
                registerNodeError(NodeErrorStateType.NODE_REGISTRATION_FAILED, result);
            }
        } catch (ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot update registration of this node (name {}, oid {}).", e, nodeName, nodeOid);
            if (localNodeState.getErrorState() == NodeErrorStateType.OK) {
                registerNodeError(NodeErrorStateType.NODE_REGISTRATION_FAILED, result);
            }
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot update registration of this node (name {}, oid {}) due to schema exception. Stopping the scheduler.", e, nodeName, nodeOid);
            if (localNodeState.getErrorState() == NodeErrorStateType.OK) {
                registerNodeError(NodeErrorStateType.NODE_REGISTRATION_FAILED, result);
            }
        }
    }

    private void refreshCachedLocalNodeObject(String nodeOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        setCachedLocalNodeObject(
                repositoryService.getObject(
                        NodeType.class,
                        nodeOid,
                        schemaService.getOperationOptionsBuilder().readOnly().build(),
                        result));
    }

    /**
     * Checks whether this Node object was not overwritten by another node (implying there is duplicate node ID in cluster).
     *
     * @return current node, if everything is OK (else null)
     */
    NodeType verifyNodeObject(OperationResult result) {
        PrismObject<NodeType> nodeInRepo;
        String oid = getCachedLocalNodeObjectOid();
        String myName = configuration.getNodeId();
        LOGGER.trace("Verifying node record with OID {}", oid);

        // first, let us check the record of this node - whether it exists and whether the internalNodeIdentifier is OK
        try {
            nodeInRepo = repositoryService.getObject(NodeType.class, oid, null, result);
        } catch (ObjectNotFoundException e) {
            if (doesNodeExist(result, myName)) {
                LoggingUtils.logException(LOGGER, "The record of this node cannot be read (OID {} not found), but " +
                        "another node record with the name '{}' exists. It seems that in this cluster " +
                        "there are two or more nodes with the same name '{}'. Stopping the scheduler " +
                        "to minimize the damage.", e, oid, myName, myName);
                registerNodeError(NodeErrorStateType.DUPLICATE_NODE_ID_OR_NAME, result);
            } else {
                LoggingUtils.logException(LOGGER, "The record of this node cannot be read (OID {} not found). It  " +
                        "seems it was deleted in the meantime. Please check the reason. Stopping the scheduler " +
                        "to minimize the damage.", e, oid, myName, myName);
                // actually we could re-register the node, but it is safer (and easier for now :) to stop the node instead
                registerNodeError(NodeErrorStateType.NODE_REGISTRATION_FAILED, result);
            }
            return null;
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot check the record of this node (OID = {}) because of schema exception. Stopping the scheduler.", e, oid);
            registerNodeError(NodeErrorStateType.NODE_REGISTRATION_FAILED, result);
            return null;
        }

        // check the internalNodeIdentifier
        String existingId = cachedLocalNodeObject.asObjectable().getInternalNodeIdentifier();
        String idInRepo = nodeInRepo.asObjectable().getInternalNodeIdentifier();
        if (!existingId.equals(idInRepo)) {
            LOGGER.error("Internal node identifier has been overwritten in the repository. " +
                    "Probably somebody has overwritten it in the meantime, i.e. another node with the name of '" +
                    cachedLocalNodeObject.asObjectable().getName() + "' is running. Stopping the scheduler.");
            registerNodeError(NodeErrorStateType.DUPLICATE_NODE_ID_OR_NAME, result);
            return null;
        }
        setCachedLocalNodeObject(nodeInRepo);
        return nodeInRepo.asObjectable();
    }

    /**
     * There may be either exactly one non-clustered node (and no other nodes), or clustered nodes only.
     */
    void checkNonClusteredNodes(OperationResult result) {

        LOGGER.trace("Checking non-clustered nodes.");

        List<String> clustered = new ArrayList<>();
        List<String> nonClustered = new ArrayList<>();

        List<PrismObject<NodeType>> allNodes = clusterManager.getAllNodes(result);
        for (PrismObject<NodeType> nodePrism : allNodes) {
            NodeType n = nodePrism.asObjectable();
            if (isUpAndAlive(n)) {
                if (n.isClustered()) {
                    clustered.add(n.getNodeIdentifier());
                } else {
                    nonClustered.add(n.getNodeIdentifier());
                }
            }
        }

        LOGGER.trace("Clustered nodes: {}", clustered);
        LOGGER.trace("Non-clustered nodes: {}", nonClustered);

        int all = clustered.size() + nonClustered.size();

        if (!configuration.isClustered() && all > 1) {
            LOGGER.error("This node is a non-clustered one, mixed with other nodes. In this system, there are " +
                    nonClustered.size() + " non-clustered nodes (" + nonClustered + ") and " +
                    clustered.size() + " clustered ones (" + clustered + "). Stopping this node.");
            registerNodeError(NodeErrorStateType.NON_CLUSTERED_NODE_WITH_OTHERS, result);
        }

    }

    boolean isUpAndAlive(NodeType n) {
        return n.getOperationalState() == NodeOperationalStateType.UP && isCheckingIn(n);
    }

    boolean isCheckingIn(NodeType n) {
        return n.getOperationalState() != NodeOperationalStateType.DOWN && n.getLastCheckInTime() != null &&
                System.currentTimeMillis() - n.getLastCheckInTime().toGregorianCalendar().getTimeInMillis()
                        <= configuration.getNodeTimeout() * 1000L;
    }

    private boolean doesNodeExist(OperationResult result, String myName) {
        try {
            return !findNodesWithGivenName(myName, result).isEmpty();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Existence of a Node cannot be checked due to schema exception.", e);
            return false;
        }
    }

    private List<PrismObject<NodeType>> findNodesWithGivenName(String name, OperationResult result) throws SchemaException {
        return repositoryService.searchObjects(
                NodeType.class,
                ObjectQueryUtil.createOrigNameQuery(name),
                null,
                result);
    }

    /**
     * Sets node error status and shuts down the scheduler (used when an error occurs after initialization).
     *
     * @param status Error status to be set.
     */
    private void registerNodeError(NodeErrorStateType status, OperationResult result) {
        localNodeState.setErrorState(status);
        try {
            localExecutionManager.stopSchedulerAndTasks(0L, result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't stop the scheduler, continuing with processing the node error", e);
        }
        localScheduler.shutdownScheduler();
        LOGGER.warn("Scheduler stopped, please check your cluster configuration as soon as possible; kind of error = " + status);
    }

    @NotNull
    private String getMyHostname() {
        if (configuration.getHostName() != null) {
            return configuration.getHostName();
        } else {
            try {
                String hostName = NetworkUtil.getLocalHostNameFromOperatingSystem();
                if (hostName != null) {
                    return hostName;
                } else {
                    LOGGER.error("Cannot get local host name");
                    // Make sure this has special characters so it cannot be interpreted as valid hostname
                    return "(unknown-host)";
                }
            } catch (UnknownHostException e) {
                LoggingUtils.logException(LOGGER, "Cannot get local hostname address", e);
                // Make sure this has special characters so it cannot be interpreted as valid hostname
                return "(unknown-host)";
            }
        }
    }

    private Integer getMyHttpPort() {
        Integer portOverride = configuration.getHttpPort();
        return portOverride != null ? portOverride : getDiscoveredHttpPort();
    }

    private Integer getDiscoveredHttpPort() {
        if (discoveredHttpPort == null) {
            discoverUrlSchemeAndPort();
        }
        return discoveredHttpPort;
    }

    private String getDiscoveredUrlScheme() {
        if (discoveredUrlScheme == null) {
            discoverUrlSchemeAndPort();
        }
        return discoveredUrlScheme;
    }

    private String getMyUrl() {
        NodeType localNode = cachedLocalNodeObject.asObjectable();
        if (localNode.getUrlOverride() != null) {
            return localNode.getUrlOverride();
        }

        String path = webContextPath;
        String intraClusterHttpUrlPattern = taskManager.getIntraClusterHttpUrlPattern();
        if (intraClusterHttpUrlPattern != null) {
            String url = intraClusterHttpUrlPattern.replace("$host", getMyHostname());
            if (url.contains("$port")) {
                Integer port = getMyHttpPort();
                if (port == null) {
                    LOGGER.debug("Temporarily postponing URL computation, as required $port variable is not yet known: {}", url);
                    return null;
                } else {
                    url = url.replace("$port", String.valueOf(port));
                }
            }
            if (url.contains("$path")) {
                if (path == null) {
                    LOGGER.debug("Temporarily postponing URL computation, as required $path variable is not yet known: {}", url);
                    return null;
                } else {
                    url = url.replace("$path", path);
                }
            }
            return url;
        } else {
            Integer port = getMyHttpPort();
            if (path == null) {
                LOGGER.debug("Temporarily postponing URL computation, as context path is not yet known");
                return null;
            } else if (port == null) {
                LOGGER.debug("Temporarily postponing URL computation, as HTTP port is not yet known");
                return null;
            } else {
                return getDiscoveredUrlScheme() + "://" + getMyHostname() + ":" + getMyHttpPort() + path;
            }
        }
    }

    private List<String> getMyIpAddresses() {
        List<String> addresses = new ArrayList<>();
        Enumeration<NetworkInterface> nets;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netInt : Collections.list(nets)) {
                for (InetAddress inetAddress: Collections.list(netInt.getInetAddresses())) {
                    String hostAddress = inetAddress.getHostAddress();
                    String normalizedAddress = normalizeAddress(hostAddress);
                    if (!isLocalAddress(normalizedAddress) || configuration.isLocalNodeClusteringEnabled()) {
                        addresses.add(normalizedAddress);
                    }
                }
            }
        } catch (SocketException e) {
            LoggingUtils.logException(LOGGER, "Cannot get local IP address", e);
            return addresses;
        }
        return addresses;
    }

    private String normalizeAddress(String hostAddress) {
        int i = hostAddress.indexOf('%');
        if (i < 0) {
            return hostAddress;
        } else {
            return hostAddress.substring(0, i);
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean isLocalAddress(String address) {
        if (address.startsWith("127.")) {
            return true;
        }
        if (address.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        if (address.equals("::1")) {
            return true;
        }
        return false;
    }

    public @NotNull PrismObject<NodeType> getCachedLocalNodeObjectRequired() {
        return MiscUtil.requireNonNull(
                cachedLocalNodeObject,
                () -> new IllegalStateException("No cached local node object: task manager was not initialized"));
    }

    public @NotNull String getCachedLocalNodeObjectOid() {
        return cachedLocalNodeObject.getOid();
    }

    private void setCachedLocalNodeObject(@NotNull PrismObject<NodeType> cachedLocalNodeObject) {
        this.cachedLocalNodeObject = Freezable.doFreeze(cachedLocalNodeObject);
    }

    boolean isCurrentNode(PrismObject<NodeType> node) {
        return configuration.getNodeId().equals(node.asObjectable().getNodeIdentifier());
    }

    boolean isCurrentNode(String nodeIdentifier) {
        return nodeIdentifier == null || configuration.getNodeId().equals(nodeIdentifier);
    }

    void deleteNode(String nodeOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(NodeRegistrar.class.getName() + ".deleteNode");
        result.addParam("nodeOid", nodeOid);

        PrismObject<NodeType> nodePrism = clusterManager.getNode(nodeOid, result);

        if (isUpAndAlive(nodePrism.asObjectable())) {
            result.recordFatalError("Node " + nodeOid + " cannot be deleted, because it is currently up.");
        } else {
            try {
                repositoryService.deleteObject(NodeType.class, nodePrism.getOid(), result);
                result.recordSuccess();
            } catch (ObjectNotFoundException e) {
                throw new SystemException("Unexpected ObjectNotFoundException when deleting a node", e);
            }
        }
    }

    private void setLocalNodeOperationalStatus(NodeOperationalStateType newState) {
        LOGGER.debug("Setting local node operational state to {}", newState);
        operationalStatus = newState;
    }

    void registerNodeUp(OperationResult result) {
        setLocalNodeOperationalStatus(NodeOperationalStateType.UP);
        updateNodeObject(result);
    }

    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        // We could do "lazy invalidation" by setting a "dirty" flag and re-reading on the next read attempt.
        // But this is perhaps simpler. The read attempt should be fast and safe. (But the invalidation should
        // be fast and safe as well... :)
        PrismObject<NodeType> currentNodeObject = cachedLocalNodeObject;
        if (currentNodeObject == null) {
            return; // nothing to invalidate (shouldn't occur, as the current node should be non-null)
        }
        String currentNodeOid = currentNodeObject.getOid();
        if (oid != null) {
            if (oid.equals(currentNodeOid)) {
                refreshCachedLocalNodeObjectOnInvalidation(currentNodeOid);
            }
        } else {
            if (type == null || type.isAssignableFrom(NodeType.class)) {
                refreshCachedLocalNodeObjectOnInvalidation(currentNodeOid);
            }
        }
    }

    public void setWebContextPath(String path) {
        LOGGER.debug("setting webContextPath to '{}'", path);
        webContextPath = path;
    }

    private void refreshCachedLocalNodeObjectOnInvalidation(String oid) {
        OperationResult result = new OperationResult(OP_REFRESH_CACHED_LOCAL_NODE_OBJECT_ON_INVALIDATION);
        try {
            refreshCachedLocalNodeObject(oid, result);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't refresh cached local node object on invalidation", t);
        }
    }

    @Override
    public @NotNull Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(new SingleCacheStateInformationType()
                .name(NodeRegistrar.class.getName())
                .size(cachedLocalNodeObject != null ? 1 : 0));
    }

    @Override
    public void dumpContent() {
        LOGGER_CONTENT.info("Current node:\n{}", DebugUtil.debugDumpLazily(cachedLocalNodeObject));
    }
}
