/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.task.quartzimpl.cluster;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.TaskConstants;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.Validate;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

/**
 * Takes care about node registration in repository.
 *
 * @author Pavol Mederly
 */
public class NodeRegistrar {

    private static final transient Trace LOGGER = TraceManager.getTrace(NodeRegistrar.class);

    private TaskManagerQuartzImpl taskManager;
    private ClusterManager clusterManager;

    /**
     * Here we keep information synchronized with the one in repository.
     * Problem is if the object in repository gets corrupted (e.g. overwritten by some other node).
     * In such cases we keep last 'good' information here.
     */
    private PrismObject<NodeType> cachedLocalNodeObject;

    private String localhostCanonicalHostName;
    private String localhostName;
    private String localhostAddress;
    private List<String> localhostIpAddresses;

    public NodeRegistrar(TaskManagerQuartzImpl taskManager, ClusterManager clusterManager) {
        Validate.notNull(taskManager);
        Validate.notNull(clusterManager);

        this.taskManager = taskManager;
        this.clusterManager = clusterManager;

        try {
            localhostIpAddresses = getMyIpAddresses();
            
            InetAddress localhost = InetAddress.getLocalHost();
            localhostCanonicalHostName = localhost.getCanonicalHostName();
            localhostName = localhost.getHostName();
            localhostAddress = localhost.getHostAddress();
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Executes node startup registration: if Node object with a give name (node ID) exists, deletes it.
     * Then creates a new Node with the information relevant to this node.
     *
     * @param result Node prism to be used for periodic re-registrations.
     */
    NodeType createOrUpdateNodeInRepo(OperationResult result) throws TaskManagerInitializationException {

        NodeType nodeToBe = createLocalNodeObject(taskManager.getConfiguration());
        LOGGER.info("Registering this node in the repository as " + nodeToBe.getNodeIdentifier() + " at " + nodeToBe.getHostname() + ":" + nodeToBe.getJmxPort());

        List<PrismObject<NodeType>> nodesInRepo;
        try {
            nodesInRepo = findNodesWithGivenName(result, PolyString.getOrig(nodeToBe.getName()));
        } catch (SchemaException e) {
            throw new TaskManagerInitializationException("Node registration failed because of schema exception", e);
        }

        if (nodesInRepo.size() == 1) {
            PrismObject<NodeType> nodeInRepo = nodesInRepo.get(0);
            // copy all information that need to be preserved from the repository
            nodeToBe.setTaskExecutionLimitations(nodeInRepo.asObjectable().getTaskExecutionLimitations());
            ObjectDelta<NodeType> nodeDelta = nodeInRepo.diff(nodeToBe.asPrismObject(), false, true);
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

    private NodeType createLocalNodeObject(TaskManagerConfiguration configuration) {
        NodeType node = getPrismContext().createKnownObjectable(NodeType.class);
	    String nodeId = configuration.getNodeId();
	    node.setNodeIdentifier(nodeId);
        node.setName(new PolyStringType(nodeId));
        node.setHostname(getMyHostname());
        node.getIpAddress().addAll(this.localhostIpAddresses);
        node.setJmxPort(configuration.getJmxPort());
        node.setClustered(configuration.isClustered());
        node.setRunning(true);
        node.setLastCheckInTime(getCurrentTime());
        node.setBuild(getBuildInformation());
        node.setTaskExecutionLimitations(
        		new TaskExecutionLimitationsType()
                    .groupLimitation(new TaskGroupExecutionLimitationType().groupName("").limit(null))
                    .groupLimitation(new TaskGroupExecutionLimitationType().groupName(nodeId).limit(null))
                    .groupLimitation(new TaskGroupExecutionLimitationType().groupName(TaskConstants.LIMIT_FOR_OTHER_GROUPS).limit(0)));
        generateInternalNodeIdentifier(node);
        return node;
    }

    private BuildInformationType getBuildInformation() {
        BuildInformationType info = new BuildInformationType();
        ResourceBundle bundle = ResourceBundle.getBundle(SchemaConstants.SCHEMA_LOCALIZATION_PROPERTIES_RESOURCE_BASE_PATH, Locale.getDefault());
        info.setVersion(bundle.getString("midPointVersion"));
        info.setRevision(bundle.getString("midPointRevision"));
        return info;
    }

    /**
     * Generates an identifier that is used to ensure that this Node object is not (by mistake) overwritten
     * by another node in cluster. ClusterManager thread periodically checks if this identifier has not been changed.
     */

    private void generateInternalNodeIdentifier(NodeType node) {
        String id = node.getNodeIdentifier() + ":" + node.getJmxPort() + ":" + Math.round(Math.random() * 10000000000000.0);
        LOGGER.trace("internal node identifier generated: " + id);
        node.setInternalNodeIdentifier(id);
    }

    private XMLGregorianCalendar getCurrentTime() {
        try {
            // AFAIK the DatatypeFactory is not thread safe, so we have to create an instance every time
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
        } catch (DatatypeConfigurationException e) {
            // this should not happen
            throw new SystemException("Cannot create DatatypeFactory (to create XMLGregorianCalendar instance).", e);
        }
    }

    /**
     * Registers the node going down (sets running attribute to false).
     */
    void recordNodeShutdown(OperationResult result) {
        String nodeName = taskManager.getNodeId();
        String nodeOid = getLocalNodeObjectOid();
        LOGGER.trace("Registering this node shutdown (name {}, oid {})", taskManager.getNodeId(), nodeOid);
        try {
            List<ItemDelta<?, ?>> modifications = DeltaBuilder.deltaFor(NodeType.class, getPrismContext())
                    .item(NodeType.F_RUNNING).replace(false)
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
     * Updates registration of this node (runs periodically within ClusterManager thread).
     */
    void updateNodeObject(OperationResult result) {
        String nodeOid = getLocalNodeObjectOid();
        String nodeName = taskManager.getNodeId();
        LOGGER.trace("Updating this node registration:\n{}", cachedLocalNodeObject.debugDumpLazily());
        try {
            List<ItemDelta<?, ?>> modifications = DeltaBuilder.deltaFor(NodeType.class, getPrismContext())
                    .item(NodeType.F_HOSTNAME).replace(getMyHostname())
                    .item(NodeType.F_IP_ADDRESS).replaceRealValues(this.localhostIpAddresses)
                    .item(NodeType.F_LAST_CHECK_IN_TIME).replace(getCurrentTime())
                    .asItemDeltas();
            getRepositoryService().modifyObject(NodeType.class, nodeOid, modifications, result);
            LOGGER.trace("Node registration successfully updated.");
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
                return null;
            } else {
                LoggingUtils.logException(LOGGER, "The record of this node cannot be read (OID {} not found). It  " +
                        "seems it was deleted in the meantime. Please check the reason. Stopping the scheduler " +
                        "to minimize the damage.", e, oid, myName, myName);
                // actually we could re-register the node, but it is safer (and easier for now :) to stop the node instead
                registerNodeError(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
                return null;
            }
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
            if (isUp(n)) {
                if (n.isClustered()) {
                    clustered.add(n.getNodeIdentifier());
                } else {
                    nonClustered.add(n.getNodeIdentifier());
                }
            }
        }

        LOGGER.trace("Clustered nodes: " + clustered);
        LOGGER.trace("Non-clustered nodes: " + nonClustered);

        int all = clustered.size() + nonClustered.size();

        if (!taskManager.getConfiguration().isClustered() && all > 1) {
            LOGGER.error("This node is a non-clustered one, mixed with other nodes. In this system, there are " +
                    nonClustered.size() + " non-clustered nodes (" + nonClustered + ") and " +
                    clustered.size() + " clustered ones (" + clustered + "). Stopping this node.");
            registerNodeError(NodeErrorStatusType.NON_CLUSTERED_NODE_WITH_OTHERS);
        }

    }

    boolean isUp(NodeType n) {
        return n.isRunning() && n.getLastCheckInTime() != null &&
                (System.currentTimeMillis() - n.getLastCheckInTime().toGregorianCalendar().getTimeInMillis())
                        <= (taskManager.getConfiguration().getNodeTimeout() * 1000L);
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

    private String getMyHostname() {

        if (taskManager.getConfiguration().getJmxHostName() != null) {
            return taskManager.getConfiguration().getJmxHostName();
        } else {
            try {
            	// Not entirely correct. But we have no other option here
            	// other than go native or execute a "hostname" shell command.
            	// We do not want to do neither.
            	
            	
            	if (localhostAddress == null) {
            		// Unix
                	String hostname = System.getenv("HOSTNAME");
                	if (hostname != null && !hostname.isEmpty()) {
                		return hostname;
                	}
                	
                	// Windows
                	hostname = System.getenv("COMPUTERNAME");
                	if (hostname != null && !hostname.isEmpty()) {
                		return hostname;
                	}
                	
            		LOGGER.error("Cannot get local IP address");
            		// Make sure this has special characters so it cannot be interpreted as valid hostname
                    return "(unknown-host)";
            	}
            	
            	String hostname = localhostCanonicalHostName;
            	if (hostname != null && !hostname.isEmpty()) {
            		return hostname;
            	}
            	
            	hostname = localhostName;
            	if (hostname != null && !hostname.isEmpty()) {
            		return hostname;
            	}
                return localhostAddress;
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Cannot get local hostname address", e);
                // Make sure this has special characters so it cannot be interpreted as valid hostname
                return "(unknown-host)";
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
					if (!isLocalAddress(normalizedAddress)) {
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

    public void deleteNode(String nodeOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {

        OperationResult result = parentResult.createSubresult(NodeRegistrar.class.getName() + ".deleteNode");
        result.addParam("nodeOid", nodeOid);

        PrismObject<NodeType> nodePrism = clusterManager.getNode(nodeOid, result);

        if (isUp(nodePrism.asObjectable())) {
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
}
