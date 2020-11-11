/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.cluster;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.Cache;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.TaskConstants;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.xml.datatype.XMLGregorianCalendar;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Takes care about node registration in repository.
 */
public class NodeRegistrar implements Cache {

    private static final Trace LOGGER = TraceManager.getTrace(NodeRegistrar.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(NodeRegistrar.class.getName() + ".content");

    private static final int SECRET_LENGTH = 20;
    private static final long SECRET_RENEWAL_PERIOD = 86400L * 1000L * 10L;

    private static final String OP_REFRESH_CACHED_LOCAL_NODE_OBJECT = NodeRegistrar.class.getName() + ".refreshCachedLocalNodeObject";

    private final TaskManagerQuartzImpl taskManager;
    private final ClusterManager clusterManager;
    private long lastDiscovery;

    private volatile NodeOperationalStatusType operationalStatus = NodeOperationalStatusType.STARTING;

    private static final long DISCOVERY_RETRY = 10000L;

    /**
     * Here we keep information synchronized with the one in repository.
     * Problem is if the object in repository gets corrupted (e.g. overwritten by some other node).
     * In such cases we keep last 'good' information here.
     *
     * So it should be non-null in all reasonable conditions.
     *
     * TODO Think about thread safety of this reference. E.g. what if it is replaced unexpectedly by some 'invalidate' call?
     *  See MID-6324.
     */
    private volatile PrismObject<NodeType> cachedLocalNodeObject;

    private String discoveredUrlScheme;
    private Integer discoveredHttpPort;

    NodeRegistrar(TaskManagerQuartzImpl taskManager, ClusterManager clusterManager) {
        this.taskManager = taskManager;
        this.clusterManager = clusterManager;

        discoverUrlSchemeAndPort();
    }

    void postConstruct() {
        taskManager.getCacheRegistry().registerCache(this);
    }

    void preDestroy() {
        taskManager.getCacheRegistry().unregisterCache(this);
    }

    /**
     * Executes node startup registration: if Node object with a give name (node ID) exists, deletes it.
     * Then creates a new Node with the information relevant to this node.
     *
     * @param result Node prism to be used for periodic re-registrations.
     */
    NodeType createOrUpdateNodeInRepo(OperationResult result) throws TaskManagerInitializationException {

        TaskManagerConfiguration configuration = taskManager.getConfiguration();

        NodeType nodeToBe = createLocalNodeObject(configuration);
        LOGGER.info("Registering this node in the repository as {} at {}", nodeToBe.getNodeIdentifier(), nodeToBe.getHostname());

        List<PrismObject<NodeType>> nodesInRepo;
        try {
            nodesInRepo = findNodesWithGivenName(result, PolyString.getOrig(nodeToBe.getName()));
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
            nodeToBe.setUrlOverride(applyDefault(nodeInRepo.asObjectable().getUrlOverride(), configuration.getUrl()));
            nodeToBe.setUrl(nodeInRepo.asObjectable().getUrl());        // URL is refreshed later, in cluster manager thread
            if (shouldRenewSecret(nodeInRepo.asObjectable())) {
                LOGGER.info("Renewing node secret for the current node");
            } else {
                nodeToBe.setSecret(nodeInRepo.asObjectable().getSecret());
                nodeToBe.setSecretUpdateTimestamp(nodeInRepo.asObjectable().getSecretUpdateTimestamp());
            }
            ObjectDelta<NodeType> nodeDelta = nodeInRepo.diff(nodeToBe.asPrismObject(), EquivalenceStrategy.DATA);
            LOGGER.debug("Applying delta to existing node object:\n{}", nodeDelta.debugDumpLazily());
            try {
                getRepositoryService().modifyObject(NodeType.class, nodeInRepo.getOid(), nodeDelta.getModifications(), result);
                LOGGER.debug("Node was successfully updated in the repository.");
                nodeToBe.setOid(nodeInRepo.getOid());
                setCachedLocalNodeObject(nodeToBe.asPrismObject());
                return nodeToBe;
            } catch (ObjectNotFoundException|SchemaException|ObjectAlreadyExistsException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update node object on system initialization; will re-create the node", e);
            }
        }

        // either there is no node, more nodes, or there was some problem during updating the node

        if (nodesInRepo.size() > 1) {
            LOGGER.warn("More than one node with the name of {}: removing all of them.", nodeToBe.getName());
        }

        for (PrismObject<NodeType> n : nodesInRepo) {
            LOGGER.debug("Removing existing NodeType with oid = {}, name = {}", n.getOid(), n.getName());
            try {
                getRepositoryService().deleteObject(NodeType.class, n.getOid(), result);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot remove NodeType with oid = {}, name = {}, because it does not exist.", e, n.getOid(), n.getElementName());
                // continue, because the error is not that severe (we hope so)
            }
        }

        try {
            String oid = getRepositoryService().addObject(nodeToBe.asPrismObject(), null, result);
            nodeToBe.setOid(oid);
            setCachedLocalNodeObject(nodeToBe.asPrismObject());
        } catch (ObjectAlreadyExistsException e) {
            taskManager.setNodeErrorStatus(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            throw new TaskManagerInitializationException("Cannot register this node, because it already exists (this should not happen, as nodes with such a name were just removed)", e);
        } catch (SchemaException e) {
            taskManager.setNodeErrorStatus(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            throw new TaskManagerInitializationException("Cannot register this node because of schema exception", e);
        }

        LOGGER.debug("Node was successfully registered (created) in the repository.");
        return nodeToBe;
    }

    private boolean shouldRenewSecret(NodeType nodeInRepo) {
        return nodeInRepo.getSecret() == null || nodeInRepo.getSecretUpdateTimestamp() == null ||
                System.currentTimeMillis() >= XmlTypeConverter.toMillis(nodeInRepo.getSecretUpdateTimestamp()) + SECRET_RENEWAL_PERIOD;
    }

    private <T> T applyDefault(T oldValue, T defaultValue) {
        return oldValue != null ? oldValue : defaultValue;
    }

    @NotNull
    private NodeType createLocalNodeObject(TaskManagerConfiguration configuration) {
        XMLGregorianCalendar currentTime = getCurrentTime();
        NodeType node = getPrismContext().createKnownObjectable(NodeType.class);
        String nodeId = configuration.getNodeId();
        node.setNodeIdentifier(nodeId);
        node.setName(new PolyStringType(nodeId));
        node.setHostname(getMyHostname());
        node.getIpAddress().addAll(getMyIpAddresses());
        node.setUrlOverride(configuration.getUrl());                 // overridden later (if already exists in repo)
        node.setJmxPort(configuration.getJmxPort());
        node.setClustered(configuration.isClustered());
        node.setRunning(true);
        node.setOperationalStatus(operationalStatus);
        node.setLastCheckInTime(currentTime);
        node.setBuild(getBuildInformation());
        node.setTaskExecutionLimitations(computeTaskExecutionLimitations(
                configuration.getTaskExecutionLimitations(), configuration.getNodeId()));
        generateInternalNodeIdentifier(node);
        node.setSecretUpdateTimestamp(currentTime);                 // overridden later (if already exists in repo)
        node.setSecret(generateNodeSecret());                       // overridden later (if already exists in repo)
        return node;
    }

    // public static because of testing
    @NotNull
    public static TaskExecutionLimitationsType computeTaskExecutionLimitations(TaskExecutionLimitationsType configuredLimitations,
            String nodeId) {
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
            secret = taskManager.getProtector().encryptString(plain);
        } catch (EncryptionException e) {
            throw new SystemException("Couldn't encrypt node secret: " + e.getMessage(), e);
        }
        return secret;
    }

    private BuildInformationType getBuildInformation() {
        BuildInformationType info = new BuildInformationType();
        LocalizationService localizationService = taskManager.getLocalizationService();
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

    private XMLGregorianCalendar getCurrentTime() {
        return XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
    }

    /**
     * Registers the node going down (sets running attribute to false).
     */
    void recordNodeShutdown(OperationResult result) {
        String nodeName = taskManager.getNodeId();
        String nodeOid = getLocalNodeObjectOid();
        LOGGER.trace("Registering this node shutdown (name {}, oid {})", taskManager.getNodeId(), nodeOid);
        try {
            setLocalNodeOperationalStatus(NodeOperationalStatusType.DOWN);
            List<ItemDelta<?, ?>> modifications = getPrismContext().deltaFor(NodeType.class)
                    .item(NodeType.F_RUNNING).replace(false)
                    .item(NodeType.F_OPERATIONAL_STATUS).replace(operationalStatus)
                    .item(NodeType.F_LAST_CHECK_IN_TIME).replace(getCurrentTime())
                    .asItemDeltas();
            getRepositoryService().modifyObject(NodeType.class, nodeOid, modifications, result);
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
        String nodeOid = getLocalNodeObjectOid();
        String nodeName = taskManager.getNodeId();
        try {
            refreshCachedLocalNodeObject(nodeOid, result);
            LOGGER.trace("Updating this node registration:\n{}", cachedLocalNodeObject.debugDumpLazily());

            XMLGregorianCalendar currentTime = getCurrentTime();
            String myUrl = getMyUrl();
            LOGGER.debug("My intra-cluster communication URL is '{}'", myUrl);
            List<ItemDelta<?, ?>> modifications = getPrismContext().deltaFor(NodeType.class)
                    .item(NodeType.F_HOSTNAME).replace(getMyHostname())
                    .item(NodeType.F_IP_ADDRESS).replaceRealValues(getMyIpAddresses())
                    .item(NodeType.F_LAST_CHECK_IN_TIME).replace(currentTime)
                    .item(NodeType.F_RUNNING).replace(true)
                    .item(NodeType.F_OPERATIONAL_STATUS).replace(operationalStatus)
                    .asItemDeltas();
            if (shouldRenewSecret(cachedLocalNodeObject.asObjectable())) {
                LOGGER.info("Renewing node secret for the current node");
                modifications.addAll(getPrismContext().deltaFor(NodeType.class)
                        .item(NodeType.F_SECRET).replace(generateNodeSecret())
                        .item(NodeType.F_SECRET_UPDATE_TIMESTAMP).replace(currentTime)
                        .asItemDeltas());
            }
            if (myUrl != null) {    // i.e. if known (might not be known during startup)
                modifications.add(getPrismContext().deltaFor(NodeType.class)
                        .item(NodeType.F_URL).replace(myUrl)
                        .asItemDelta());
                String oldUrl = cachedLocalNodeObject.asObjectable().getUrl();
                if (!myUrl.equals(oldUrl)) {
                    LOGGER.info("Changing node URL from {} to {}", oldUrl, myUrl);
                }
            }
            getRepositoryService().modifyObject(NodeType.class, nodeOid, modifications, result);
            LOGGER.trace("Node registration successfully updated.");
            refreshCachedLocalNodeObject(nodeOid, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot update registration of this node (name {}, oid {}), because it "
                            + "does not exist in repository. It is probably caused by cluster misconfiguration (other "
                            + "node rewriting the Node object?) Stopping the scheduler.", e, nodeName, nodeOid);
            if (taskManager.getLocalNodeErrorStatus() == NodeErrorStatusType.OK) {
                registerNodeError(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            }
        } catch (ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot update registration of this node (name {}, oid {}).", e, nodeName, nodeOid);
            if (taskManager.getLocalNodeErrorStatus() == NodeErrorStatusType.OK) {
                registerNodeError(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            }
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot update registration of this node (name {}, oid {}) due to schema exception. Stopping the scheduler.", e, nodeName, nodeOid);
            if (taskManager.getLocalNodeErrorStatus() == NodeErrorStatusType.OK) {
                registerNodeError(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            }
        }
    }

    private void refreshCachedLocalNodeObject(String nodeOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        setCachedLocalNodeObject(getRepositoryService().getObject(NodeType.class, nodeOid, null, result));
    }

    /**
     * Checks whether this Node object was not overwritten by another node (implying there is duplicate node ID in cluster).
     *
     * @return current node, if everything is OK
     */
    NodeType verifyNodeObject(OperationResult result) {
        PrismObject<NodeType> nodeInRepo;
        String oid = getLocalNodeObjectOid();
        String myName = taskManager.getNodeId();
        LOGGER.trace("Verifying node record with OID {}", oid);

        // first, let us check the record of this node - whether it exists and whether the internalNodeIdentifier is OK
        try {
            nodeInRepo = getRepositoryService().getObject(NodeType.class, oid, null, result);
        } catch (ObjectNotFoundException e) {
            if (doesNodeExist(result, myName)) {
                LoggingUtils.logException(LOGGER, "The record of this node cannot be read (OID {} not found), but " +
                        "another node record with the name '{}' exists. It seems that in this cluster " +
                        "there are two or more nodes with the same name '{}'. Stopping the scheduler " +
                        "to minimize the damage.", e, oid, myName, myName);
                registerNodeError(NodeErrorStatusType.DUPLICATE_NODE_ID_OR_NAME);
            } else {
                LoggingUtils.logException(LOGGER, "The record of this node cannot be read (OID {} not found). It  " +
                        "seems it was deleted in the meantime. Please check the reason. Stopping the scheduler " +
                        "to minimize the damage.", e, oid, myName, myName);
                // actually we could re-register the node, but it is safer (and easier for now :) to stop the node instead
                registerNodeError(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            }
            return null;
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot check the record of this node (OID = {}) because of schema exception. Stopping the scheduler.", e, oid);
            registerNodeError(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            return null;
        }

        // check the internalNodeIdentifier
        String existingId = cachedLocalNodeObject.asObjectable().getInternalNodeIdentifier();
        String idInRepo = nodeInRepo.asObjectable().getInternalNodeIdentifier();
        if (!existingId.equals(idInRepo)) {
            LOGGER.error("Internal node identifier has been overwritten in the repository. " +
                    "Probably somebody has overwritten it in the meantime, i.e. another node with the name of '" +
                    cachedLocalNodeObject.asObjectable().getName() + "' is running. Stopping the scheduler.");
            registerNodeError(NodeErrorStatusType.DUPLICATE_NODE_ID_OR_NAME);
            return null;
        }
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

        if (!taskManager.getConfiguration().isClustered() && all > 1) {
            LOGGER.error("This node is a non-clustered one, mixed with other nodes. In this system, there are " +
                    nonClustered.size() + " non-clustered nodes (" + nonClustered + ") and " +
                    clustered.size() + " clustered ones (" + clustered + "). Stopping this node.");
            registerNodeError(NodeErrorStatusType.NON_CLUSTERED_NODE_WITH_OTHERS);
        }

    }

    boolean isUpAndAlive(NodeType n) {
        return n.getOperationalStatus() == NodeOperationalStatusType.UP && isCheckingIn(n);
    }

    boolean isCheckingIn(NodeType n) {
        return n.getOperationalStatus() != NodeOperationalStatusType.DOWN && n.getLastCheckInTime() != null &&
                System.currentTimeMillis() - n.getLastCheckInTime().toGregorianCalendar().getTimeInMillis()
                        <= taskManager.getConfiguration().getNodeTimeout() * 1000L;
    }

    private boolean doesNodeExist(OperationResult result, String myName) {
        try {
            return !findNodesWithGivenName(result, myName).isEmpty();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Existence of a Node cannot be checked due to schema exception.", e);
            return false;
        }
    }

    private List<PrismObject<NodeType>> findNodesWithGivenName(OperationResult result, String name) throws SchemaException {
        ObjectQuery q = ObjectQueryUtil.createOrigNameQuery(name, getPrismContext());
        return getRepositoryService().searchObjects(NodeType.class, q, null, result);
    }

    /**
     * Sets node error status and shuts down the scheduler (used when an error occurs after initialization).
     *
     * @param status Error status to be set.
     */
    private void registerNodeError(NodeErrorStatusType status) {
        taskManager.setNodeErrorStatus(status);
        if (taskManager.getServiceThreadsActivationState()) {
            taskManager.getExecutionManager().stopSchedulerAndTasksLocally(0L, new OperationResult("nodeError"));
        }
        taskManager.getExecutionManager().shutdownLocalSchedulerChecked();
        LOGGER.warn("Scheduler stopped, please check your cluster configuration as soon as possible; kind of error = " + status);
    }

    @NotNull
    private String getMyHostname() {
        if (taskManager.getConfiguration().getHostName() != null) {
            return taskManager.getConfiguration().getHostName();
        } else if (taskManager.getConfiguration().getJmxHostName() != null) {
            return taskManager.getConfiguration().getJmxHostName();
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
        Integer portOverride = taskManager.getConfiguration().getHttpPort();
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
        NodeType localNode = cachedLocalNodeObject != null ? cachedLocalNodeObject.asObjectable() : null;
        String path = taskManager.getWebContextPath();
        if (localNode != null && localNode.getUrlOverride() != null) {
            return localNode.getUrlOverride();
        } else if (taskManager.getIntraClusterHttpUrlPattern() != null) {
            String url = taskManager.getIntraClusterHttpUrlPattern()
                    .replace("$host", getMyHostname());
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
            for (NetworkInterface netint : Collections.list(nets)) {
                for (InetAddress inetAddress: Collections.list(netint.getInetAddresses())) {
                    String hostAddress = inetAddress.getHostAddress();
                    String normalizedAddress = normalizeAddress(hostAddress);
                    if (!isLocalAddress(normalizedAddress) || taskManager.isLocalNodeClusteringEnabled()) {
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
    private boolean isLocalAddress(String addr) {
        if (addr.startsWith("127.")) {
            return true;
        }
        if (addr.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        if (addr.equals("::1")) {
            return true;
        }
        return false;
    }

    PrismObject<NodeType> getCachedLocalNodeObject() {
        return cachedLocalNodeObject;
    }

    private String getLocalNodeObjectOid() {
        return cachedLocalNodeObject.getOid();
    }

    private void setCachedLocalNodeObject(PrismObject<NodeType> cachedLocalNodeObject) {
        this.cachedLocalNodeObject = cachedLocalNodeObject;
    }

    boolean isCurrentNode(PrismObject<NodeType> node) {
        return taskManager.getNodeId().equals(node.asObjectable().getNodeIdentifier());
    }

    boolean isCurrentNode(String nodeIdentifier) {
        return nodeIdentifier == null || taskManager.getNodeId().equals(nodeIdentifier);
    }

    private RepositoryService getRepositoryService() {
        return taskManager.getRepositoryService();
    }

    private PrismContext getPrismContext() {
        return taskManager.getPrismContext();
    }

    void deleteNode(String nodeOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(NodeRegistrar.class.getName() + ".deleteNode");
        result.addParam("nodeOid", nodeOid);

        PrismObject<NodeType> nodePrism = clusterManager.getNode(nodeOid, result);

        if (isUpAndAlive(nodePrism.asObjectable())) {
            result.recordFatalError("Node " + nodeOid + " cannot be deleted, because it is currently up.");
        } else {
            try {
                taskManager.getRepositoryService().deleteObject(NodeType.class, nodePrism.getOid(), result);
                result.recordSuccess();
            } catch (ObjectNotFoundException e) {
                throw new SystemException("Unexpected ObjectNotFoundException when deleting a node", e);
            }
        }
    }

    private void setLocalNodeOperationalStatus(NodeOperationalStatusType newState) {
        LOGGER.debug("Setting local node operational state to {}", newState);
        operationalStatus = newState;
    }

    void registerNodeUp(OperationResult result) {
        setLocalNodeOperationalStatus(NodeOperationalStatusType.UP);
        updateNodeObject(result);
    }

    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        // TODO is it a good idea to fetch local node immediately on invalidation?
        //  Maybe we could postpone it to next call of 'get local node'
        //  See MID-6324.
        PrismObject<NodeType> currentNode = this.cachedLocalNodeObject;
        if (currentNode == null) {
            return; // nothing to invalidate
        }
        if (oid != null) {
            if (oid.equals(currentNode.getOid())) {
                refreshCachedLocalNodeObject(currentNode.getOid());
            }
        } else {
            if (type == null || type.isAssignableFrom(NodeType.class)) {
                refreshCachedLocalNodeObject(currentNode.getOid());
            }
        }
    }

    private void refreshCachedLocalNodeObject(String oid) {
        OperationResult result = new OperationResult(OP_REFRESH_CACHED_LOCAL_NODE_OBJECT);
        try {
            refreshCachedLocalNodeObject(oid, result);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't refresh cached local node object on invalidation", t);
        }
    }

    @Override
    public @NotNull Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(new SingleCacheStateInformationType(taskManager.getPrismContext())
                .name(NodeRegistrar.class.getName())
                .size(cachedLocalNodeObject != null ? 1 : 0));
    }

    @Override
    public void dumpContent() {
        LOGGER_CONTENT.info("Current node:\n{}", DebugUtil.debugDumpLazily(cachedLocalNodeObject));
    }
}
