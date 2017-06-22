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
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.BuildInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeErrorStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.Validate;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    private PrismObject<NodeType> nodePrism;

    public NodeRegistrar(TaskManagerQuartzImpl taskManager, ClusterManager clusterManager) {
        Validate.notNull(taskManager);
        Validate.notNull(clusterManager);

        this.taskManager = taskManager;
        this.clusterManager = clusterManager;
    }

    /**
     * Executes node startup registration: if Node object with a give name (node ID) exists, deletes it.
     * Then creates a new Node with the information relevant to this node.
     *
     * @param result Node prism to be used for periodic re-registrations.
     */
    void createNodeObject(OperationResult result) throws TaskManagerInitializationException {

        nodePrism = createNodePrism(taskManager.getConfiguration());
        NodeType node = nodePrism.asObjectable();

        LOGGER.info("Registering this node in the repository as " + node.getNodeIdentifier() + " at " + node.getHostname() + ":" + node.getJmxPort());

        List<PrismObject<NodeType>> nodes;
        try {
            nodes = findNodesWithGivenName(result, node.getName());
        } catch (SchemaException e) {
            throw new TaskManagerInitializationException("Node registration failed because of schema exception", e);
        }

        if (nodes.size() == 1) {
            PrismObject<NodeType> currentNode = nodes.get(0);
            ObjectDelta<NodeType> nodeDelta = currentNode.diff(nodePrism, false, true);
            LOGGER.debug("Applying delta to existing node object:\n{}", nodeDelta.debugDumpLazily());
            try {
                getRepositoryService().modifyObject(NodeType.class, currentNode.getOid(), nodeDelta.getModifications(), result);
                LOGGER.debug("Node was successfully updated in the repository.");
                nodePrism.setOid(currentNode.getOid());
                return;
            } catch (ObjectNotFoundException|SchemaException|ObjectAlreadyExistsException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update node object on system initialization; will re-create the node", e);
            }
        }

        // either there is no node, more nodes, or there was some problem during updating the node

        if (nodes.size() > 1) {
            LOGGER.warn("More than one node with the name of {}: removing all of them.", node.getName());
        }

        for (PrismObject<NodeType> n : nodes) {
            LOGGER.debug("Removing existing NodeType with oid = {}, name = {}", n.getOid(), n.getName());
            try {
                getRepositoryService().deleteObject(NodeType.class, n.getOid(), result);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot remove NodeType with oid = {}, name = {}, because it does not exist.", e, n.getOid(), n.getElementName());
                // continue, because the error is not that severe (we hope so)
            }
        }

        try {
            String oid = getRepositoryService().addObject(nodePrism, null, result);
            nodePrism.setOid(oid);
        } catch (ObjectAlreadyExistsException e) {
            taskManager.setNodeErrorStatus(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            throw new TaskManagerInitializationException("Cannot register this node, because it already exists (this should not happen, as nodes with such a name were just removed)", e);
        } catch (SchemaException e) {
            taskManager.setNodeErrorStatus(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            throw new TaskManagerInitializationException("Cannot register this node because of schema exception", e);
        }

        LOGGER.debug("Node was successfully registered (created) in the repository.");
    }

    private PrismObject<NodeType> createNodePrism(TaskManagerConfiguration configuration) {

        PrismObjectDefinition<NodeType> nodeTypeDef = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(NodeType.class);
        PrismObject<NodeType> nodePrism;
		try {
			nodePrism = nodeTypeDef.instantiate();
		} catch (SchemaException e) {
			throw new SystemException(e.getMessage(), e);
		}

        NodeType node = nodePrism.asObjectable();

        node.setNodeIdentifier(configuration.getNodeId());
        node.setName(new PolyStringType(configuration.getNodeId()));
        node.setHostname(getMyAddress());
        node.setJmxPort(configuration.getJmxPort());
        node.setClustered(configuration.isClustered());
        node.setRunning(true);
        node.setLastCheckInTime(getCurrentTime());
        node.setBuild(getBuildInformation());

        generateInternalNodeIdentifier(node);

        return nodePrism;
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
     *
     * @param node
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

    private PropertyDelta<XMLGregorianCalendar> createCheckInTimeDelta() {
        return PropertyDelta.createReplaceDelta(nodePrism.getDefinition(), NodeType.F_LAST_CHECK_IN_TIME, getCurrentTime());
    }


    /**
     * Registers the node going down (sets running attribute to false).
     *
     * @param result
     */
    void recordNodeShutdown(OperationResult result) {

        LOGGER.trace("Registering this node shutdown (name {}, oid {})", nodePrism.asObjectable().getName(), nodePrism.getOid());

        List<PropertyDelta<?>> modifications = new ArrayList<PropertyDelta<?>>();
        modifications.add(PropertyDelta.createReplaceDelta(nodePrism.getDefinition(), NodeType.F_RUNNING, false));
        modifications.add(createCheckInTimeDelta());

        try {
            getRepositoryService().modifyObject(NodeType.class, nodePrism.getOid(), modifications, result);
            LOGGER.trace("Node shutdown successfully registered.");
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot register shutdown of this node (name {}, oid {}), because it does not exist.", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
            // we do not set error flag here, because we hope that on a node startup the registration would (perhaps) succeed
        } catch (ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot register shutdown of this node (name {}, oid {}).", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot register shutdown of this node (name {}, oid {}) due to schema exception.", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
        }
    }

    /**
     * Updates registration of this node (runs periodically within ClusterManager thread).
     *
     * @param result
     */
    void updateNodeObject(OperationResult result) {

        LOGGER.trace("Updating this node registration (name {}, oid {})", nodePrism.asObjectable().getName(), nodePrism.getOid());

        List<PropertyDelta<?>> modifications = new ArrayList<PropertyDelta<?>>();
        modifications.add(PropertyDelta.createReplaceDelta(nodePrism.getDefinition(), NodeType.F_HOSTNAME, getMyAddress()));
        modifications.add(createCheckInTimeDelta());

        try {
            getRepositoryService().modifyObject(NodeType.class, nodePrism.getOid(), modifications, result);
            LOGGER.trace("Node registration successfully updated.");
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot update registration of this node (name {}, oid {}), because it does not exist in repository. It is probably caused by cluster misconfiguration (other node rewriting the Node object?) Stopping the scheduler.", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
            if (taskManager.getLocalNodeErrorStatus() == NodeErrorStatusType.OK) {
                registerNodeError(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            }
        } catch (ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot update registration of this node (name {}, oid {}).", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
            if (taskManager.getLocalNodeErrorStatus() == NodeErrorStatusType.OK) {
                registerNodeError(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            }
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot update registration of this node (name {}, oid {}) due to schema exception. Stopping the scheduler.", e,
                    nodePrism.asObjectable().getName(), nodePrism.getOid());
            if (taskManager.getLocalNodeErrorStatus() == NodeErrorStatusType.OK) {
                registerNodeError(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            }
        }
    }

    /**
     * Checks whether this Node object was not overwritten by another node (implying there is duplicate node ID in cluster).
     *
     * @param result
     */
    void verifyNodeObject(OperationResult result) {

        PrismObject<NodeType> nodeInRepo;

        String oid = nodePrism.getOid();
        PolyStringType myName = nodePrism.asObjectable().getName();
        LOGGER.trace("Verifying node record with OID = " + oid);

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
                return;
            } else {
                LoggingUtils.logException(LOGGER, "The record of this node cannot be read (OID {} not found). It  " +
                        "seems it was deleted in the meantime. Please check the reason. Stopping the scheduler " +
                        "to minimize the damage.", e, oid, myName, myName);
                // actually we could re-register the node, but it is safer (and easier for now :) to stop the node instead
                registerNodeError(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
                return;
            }
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot check the record of this node (OID = {}) because of schema exception. Stopping the scheduler.", e, oid);
            registerNodeError(NodeErrorStatusType.NODE_REGISTRATION_FAILED);
            return;
        }

        // check the internalNodeIdentifier
        String existingId = nodePrism.asObjectable().getInternalNodeIdentifier();
        String idInRepo = nodeInRepo.asObjectable().getInternalNodeIdentifier();
        if (!existingId.equals(idInRepo)) {
            LOGGER.error("Internal node identifier has been overwritten in the repository. " +
                    "Probably somebody has overwritten it in the meantime, i.e. another node with the name of '" +
                    nodePrism.asObjectable().getName() + "' is running. Stopping the scheduler.");
            registerNodeError(NodeErrorStatusType.DUPLICATE_NODE_ID_OR_NAME);
            return;
        }
    }

    /**
     * There may be either exactly one non-clustered node (and no other nodes), or clustered nodes only.
     * @param result
     */
    public void checkNonClusteredNodes(OperationResult result) {

        LOGGER.trace("Checking non-clustered nodes.");

        List<String> clustered = new ArrayList<String>();
        List<String> nonClustered = new ArrayList<String>();

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


    private boolean doesNodeExist(OperationResult result, PolyStringType myName) {
        try {
            List<PrismObject<NodeType>> nodes = findNodesWithGivenName(result, myName);
            return nodes != null && !nodes.isEmpty();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Existence of a Node cannot be checked due to schema exception.", e);
            return false;
        }
    }

    private List<PrismObject<NodeType>> findNodesWithGivenName(OperationResult result, PolyStringType name) throws SchemaException {

        ObjectQuery q = ObjectQueryUtil.createOrigNameQuery(name, getPrismContext());
//    	ObjectQuery q = ObjectQuery.createObjectQuery(EqualsFilter.createEqual(NodeType.F_NAME, NodeType.class, getPrismContext(), null, name));
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

    private String getMyAddress() {

        if (taskManager.getConfiguration().getJmxHostName() != null) {
            return taskManager.getConfiguration().getJmxHostName();
        } else {
            try {
                InetAddress address = InetAddress.getLocalHost();
                return address.getHostAddress();
            } catch (UnknownHostException e) {
                LoggingUtils.logException(LOGGER, "Cannot get local IP address", e);
                return "unknown-host";
            }
        }
    }

    public PrismObject<NodeType> getNodePrism() {
        return nodePrism;
    }

    public String getNodeId() {
        return nodePrism.asObjectable().getNodeIdentifier();
    }

    public boolean isCurrentNode(PrismObject<NodeType> node) {
        return getNodeId().equals(node.asObjectable().getNodeIdentifier());
    }

    boolean isCurrentNode(String nodeIdentifier) {
        return nodeIdentifier == null || getNodeId().equals(nodeIdentifier);
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
