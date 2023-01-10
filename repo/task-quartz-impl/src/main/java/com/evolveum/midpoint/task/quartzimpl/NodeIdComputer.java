/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManagerConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.interpol.ConfigurationInterpolator;
import org.apache.commons.configuration2.interpol.InterpolatorSpecification;
import org.apache.commons.configuration2.interpol.Lookup;
import org.apache.commons.lang3.StringUtils;

/**
 * Determines node ID for the current node based on configuration and currently registered nodes.
 */
class NodeIdComputer {

    private static final Trace LOGGER = TraceManager.getTrace(NodeIdComputer.class);

    private static final String DEFAULT_NODE_ID = "DefaultNode";

    private static final int DEFAULT_SEQUENCE_START = 0;
    private static final int DEFAULT_SEQUENCE_END = 100;
    private static final String DEFAULT_SEQUENCE_FORMAT = "%d";

    private static final int MAX_ATTEMPTS_SAFEGUARD = 10000;

    private final PrismContext prismContext;
    private final RepositoryService repositoryService;

    NodeIdComputer(PrismContext prismContext, RepositoryService repositoryService) {
        this.prismContext = prismContext;
        this.repositoryService = repositoryService;
    }

    String determineNodeId(Configuration root, boolean clustered, OperationResult result) throws TaskManagerConfigurationException {
        String nodeIdExpression;
        Object nodeIdRaw = root.getProperty(MidpointConfiguration.MIDPOINT_NODE_ID_PROPERTY);  // this is a value without interpolation
        if (nodeIdRaw instanceof String && !"".equals(nodeIdRaw)) {
            nodeIdExpression = (String) nodeIdRaw;
        } else {
            // No nodeId. Let's try nodeIdSource and convert it into nodeId expression.
            String source = root.getString(MidpointConfiguration.MIDPOINT_NODE_ID_SOURCE_PROPERTY, null);
            if (StringUtils.isNotEmpty(source)) {
                nodeIdExpression = "${" + source + (source.contains(":") ? "" : ":") + "}";
            } else {
                nodeIdExpression = null;
            }
        }

        String nodeId;
        if (nodeIdExpression != null) {
            nodeId = getNodeIdFromExpression(root.getInterpolator(), nodeIdExpression, result);
        } else {
            nodeId = null;
        }

        if (StringUtils.isNotEmpty(nodeId)) {
            if (nodeId.equals(nodeIdExpression)) {
                LOGGER.info("Using configured node ID '{}'", nodeId);
            } else {
                LOGGER.info("Using node ID '{}' as provided by '{}' expression", nodeId, nodeIdExpression);
            }
            return nodeId;
        } else if (!clustered) {
            LOGGER.info("Using default node ID '{}'", DEFAULT_NODE_ID);
            return DEFAULT_NODE_ID;
        } else {
            throw new TaskManagerConfigurationException("Node ID must be set when running in clustered mode");
        }
    }

    /**
     * Expects variable names of start:end:format with the defaults of start=0, end=100, format=%d.
     */
    private static class SequenceLookup implements Lookup {

        private boolean iterationRequired;
        private int iterationCounter;

        private int end;
        private int number;

        @Override
        public String lookup(String variable) {
            LOGGER.trace("Lookup called with {}; iteration counter = {}", variable, iterationCounter);
            iterationRequired = true;

            int start = DEFAULT_SEQUENCE_START;
            end = DEFAULT_SEQUENCE_END;
            String format = DEFAULT_SEQUENCE_FORMAT;

            int firstColon = variable.indexOf(':');
            if (firstColon >= 0) {
                start = Integer.parseInt(variable.substring(0, firstColon));
                int secondColon = variable.indexOf(':', firstColon+1);
                if (secondColon >= 0) {
                    end = Integer.parseInt(variable.substring(firstColon+1, secondColon));
                    format = variable.substring(secondColon+1);
                } else {
                    end = Integer.parseInt(variable.substring(firstColon+1));
                }
            } else if (!variable.isEmpty()) {
                start = Integer.parseInt(variable);
            }
            number = start + iterationCounter;
            String rv = String.format(format, number);
            LOGGER.trace("Lookup exiting with {}; iteration counter = {}", rv, iterationCounter);
            return rv;
        }

        private void advance() {
            iterationCounter++;
        }

        private boolean isOutOfNumbers() {
            return number > end;
        }
    }

    private String getNodeIdFromExpression(ConfigurationInterpolator parentInterpolator, String expression,
            OperationResult result) {
        SequenceLookup sequenceLookup = new SequenceLookup();
        InterpolatorSpecification sequenceProvidingInterpolatorSpec = new InterpolatorSpecification.Builder()
                .withParentInterpolator(parentInterpolator)
                .withPrefixLookup("sequence", sequenceLookup)
                .create();
        ConfigurationInterpolator interpolator = ConfigurationInterpolator.fromSpecification(sequenceProvidingInterpolatorSpec);
        for (int attempt = 0; ; attempt++) {
            Object interpolationResult = interpolator.interpolate(expression);
            if (!(interpolationResult instanceof String)) {
                LOGGER.warn("Node ID expression '{}' returned null or non-String value: {}", expression, interpolationResult);
                return null;
            }
            String candidateNodeId = (String) interpolationResult;
            if (candidateNodeId.contains("${")) {
                // This is a bit of hack: it looks like the node was not resolved correctly.
                throw new SystemException("Looks like we couldn't resolve the node ID expression. The (partial) result is: '" + candidateNodeId + "'");
            }
            if (sequenceLookup.iterationRequired) {
                try {
                    // Let us try to create node with given name. If we fail we know we need to iterate.
                    // If we succeed, we will (later) replace the node with the correct content.
                    // Note that we set (fake) last check-in time here so this node will not be accidentally cleaned-up.
                    // TODO consider moving this addObject call to NodeRegistrar (requires cleanup of the mix of
                    //   Spring injected and manually created objects)
                    NodeType node = new NodeType()
                            .name(candidateNodeId)
                            .lastCheckInTime(XmlTypeConverter.createXMLGregorianCalendar());
                    repositoryService.addObject(node.asPrismObject(), null, result);
                } catch (ObjectAlreadyExistsException e) {
                    // We have a conflict. But the node might be - in fact - dead. So let's try to reclaim it if possible.
                    String nodeIdNorm = prismContext.getDefaultPolyStringNormalizer().normalize(candidateNodeId);
                    SearchResultList<PrismObject<NodeType>> existingNodes;
                    try {
                        existingNodes = repositoryService.searchObjects(
                                NodeType.class,
                                prismContext.queryFor(NodeType.class)
                                        .item(NodeType.F_NAME).eqPoly(candidateNodeId, nodeIdNorm).matchingNorm()
                                        .build(),
                                null, result);
                    } catch (SchemaException ex) {
                        throw new SystemException("Unexpected schema exception while looking for node '" + candidateNodeId
                                + "': " + e.getMessage(), e);
                    }
                    if (existingNodes.isEmpty()) {
                        // Strange. The node should have gone in the meanwhile. To be safe, let's try another one.
                        LOGGER.warn("Node name '{}' seemed to be already reserved. But it cannot be found now. Iterating to the"
                                + " next one (if possible).", candidateNodeId);
                        sequenceLookup.advance();
                    } else if (existingNodes.size() > 1) {
                        LOGGER.warn("Strange: More than one node with the name of '{}': {}. Trying next name in the sequence"
                                + "(if possible).", candidateNodeId, existingNodes);
                        sequenceLookup.advance();
                    } else {
                        NodeType existingNode = existingNodes.get(0).asObjectable();
                        if (existingNode.getOperationalState() == NodeOperationalStateType.DOWN) {
                            LOGGER.info("Considering using the node name of '{}' that already exists but is marked as being down"
                                    + " (OID {}). So deleting the node and trying again.", candidateNodeId, existingNode.getOid());
                            try {
                                repositoryService.deleteObject(NodeType.class, existingNode.getOid(), result);
                            } catch (ObjectNotFoundException ex) {
                                LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't delete the node {}. Probably someone"
                                        + " else is faster than us.", ex, existingNode);
                            }
                            // no advance here
                        } else {
                            LOGGER.debug("Node name '{}' is already reserved. Iterating to next one (if possible).", candidateNodeId);
                            sequenceLookup.advance();
                        }
                    }

                    if (attempt > MAX_ATTEMPTS_SAFEGUARD) {
                        throw new SystemException("Maximum attempts safeguard value of " + MAX_ATTEMPTS_SAFEGUARD + " has been reached. "
                                + "Something very strange must have happened.");
                    } else if (sequenceLookup.isOutOfNumbers()) {
                        throw new SystemException("Cannot acquire node name. The sequence upper border (" + sequenceLookup.end
                                + ") has been reached.");
                    } else {
                        continue;
                    }
                } catch (SchemaException e) {
                    throw new SystemException("Unexpected schema exception while creating temporary node: " + e.getMessage(), e);
                }
            }
            return candidateNodeId;
        }
    }
}
