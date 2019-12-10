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
import com.evolveum.midpoint.task.quartzimpl.cluster.NodeRegistrar;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.template.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Determines node ID for the current node based on configuration and currently registered nodes.
 */
class NodeIdComputer {

    private static final transient Trace LOGGER = TraceManager.getTrace(NodeIdComputer.class);

    private static final double DEFAULT_RANDOM_RANGE = 1000000000.0;
    private static final String DEFAULT_NODE_ID = "DefaultNode";

    private static final String NODE_ID_SOURCE_RANDOM = "random";
    private static final String NODE_ID_SOURCE_HOSTNAME = "hostname";
    private static final String NODE_ID_EXPRESSION_SEQUENCE = "sequence";

    private static final int MAX_ITERATIONS = 100;

    private final PrismContext prismContext;
    private final RepositoryService repositoryService;

    NodeIdComputer(PrismContext prismContext, RepositoryService repositoryService) {
        this.prismContext = prismContext;
        this.repositoryService = repositoryService;
    }

    String determineNodeId(Configuration c, boolean clustered, OperationResult result) throws TaskManagerConfigurationException {
        String id = c.getString(MidpointConfiguration.MIDPOINT_NODE_ID_PROPERTY, null);
        if (StringUtils.isNotEmpty(id)) {
            LOGGER.info("Using explicitly provided node ID of '{}'", id);
            return id;
        }

        String expression = c.getString(MidpointConfiguration.MIDPOINT_NODE_ID_EXPRESSION_PROPERTY, null);
        if (StringUtils.isNotEmpty(expression)) {
            String idFromExpression = getNodeIdFromExpression(expression, result);
            if (StringUtils.isNotEmpty(idFromExpression)) {
                LOGGER.info("Using node ID of '{}' as provided by the expression '{}'", idFromExpression, expression);
                return idFromExpression;
            } else {
                LOGGER.warn("Node ID expression '{}' returned no value, continuing with other options", expression);
            }
        }

        String source = c.getString(MidpointConfiguration.MIDPOINT_NODE_ID_SOURCE_PROPERTY, null);
        if (StringUtils.isNotEmpty(source)) {
            String idFromSource = getNodeIdFromSource(source);
            if (StringUtils.isNotEmpty(idFromSource)) {
                LOGGER.info("Using node ID of '{}' as determined by the '{}' source", idFromSource, source);
                return idFromSource;
            } else {
                LOGGER.warn("Node ID source '{}' provided no value, continuing with other options", source);
            }
        }

        if (!clustered) {
            LOGGER.info("Using default node ID of '{}'", DEFAULT_NODE_ID);
            return DEFAULT_NODE_ID;
        } else {
            throw new TaskManagerConfigurationException("Node ID must be set when running in clustered mode");
        }
    }

    private class BuiltinResolver extends AbstractChainedResolver {

        private boolean iterationRequired = false;
        private int iterationCounter = 0;

        private BuiltinResolver(ReferenceResolver upstreamResolver, boolean actsAsDefault) {
            super(upstreamResolver, actsAsDefault);
        }

        @Override
        protected String resolveLocally(String reference, List<String> parameters) {
            if (NODE_ID_SOURCE_RANDOM.equals(reference)) {
                double range;
                if (parameters.isEmpty()) {
                    range = DEFAULT_RANDOM_RANGE;
                } else if (parameters.size() == 1) {
                    range = Double.parseDouble(parameters.get(0));
                } else {
                    throw new IllegalArgumentException("Too many parameters for 'random' expression: " + parameters);
                }
                return getNodeIdAsRandomValue(range);
            } else if (NODE_ID_SOURCE_HOSTNAME.equals(reference)) {
                return getNodeIdAsHostName();
            } else if (NODE_ID_EXPRESSION_SEQUENCE.equals(reference)) {
                iterationRequired = true;
                int iteration = iterationCounter++;
                String format;
                int base;
                if (parameters.size() >= 1) {
                    format = parameters.get(0);
                } else {
                    format = "%d";
                }
                if (parameters.size() >= 2) {
                    base = Integer.parseInt(parameters.get(1));
                } else {
                    base = 0;
                }
                if (parameters.size() >= 3) {
                    throw new IllegalArgumentException("Too many parameters for 'sequence' expression: " + parameters);
                }
                return String.format(format, base + iteration);
            } else {
                return null;
            }
        }

        @NotNull
        @Override
        protected Collection<String> getScopes() {
            return Collections.singleton("");
        }
    }

    @NotNull
    private String getNodeIdFromExpression(String expression, OperationResult result) {
        BuiltinResolver builtinResolver = new BuiltinResolver(null, true);
        AbstractChainedResolver propertiesResolver = new JavaPropertiesResolver(builtinResolver, true);
        AbstractChainedResolver osEnvironmentResolver = new OsEnvironmentResolver(propertiesResolver, true);
        TemplateEngine engine = new TemplateEngine(osEnvironmentResolver, true, true);

        for (;;) {
            String candidateNodeId = engine.expand(expression);
            if (builtinResolver.iterationRequired) {
                try {
                    // Let us try to create node with given name. If we fail we know we need to iterate.
                    // If we succeed, we will (later) replace the node with the correct content.
                    // Note that we set (fake) last check-in time here so this node will not be accidentally cleaned-up.
                    // TODO consider moving this addObject call to NodeRegistrar (requires cleanup of the mix of
                    //   Spring injected and manually created objects)
                    NodeType node = new NodeType(prismContext)
                            .name(candidateNodeId)
                            .lastCheckInTime(XmlTypeConverter.createXMLGregorianCalendar());
                    repositoryService.addObject(node.asPrismObject(), null, result);
                } catch (ObjectAlreadyExistsException e) {
                    // We have a conflict. But the node might be - in fact - dead. So let's try to reclaim it if possible.
                    String nodeIdNorm = prismContext.getDefaultPolyStringNormalizer().normalize(candidateNodeId);
                    SearchResultList<PrismObject<NodeType>> existingNodes;
                    try {
                        existingNodes = repositoryService.searchObjects(NodeType.class,
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
                        LOGGER.info("Node name '{}' seemed to be already reserved. But it cannot be found now. Iterating to the"
                                + " next one (if possible).", candidateNodeId);
                    } else if (existingNodes.size() > 1) {
                        LOGGER.warn("Strange: More than one node with the name of '{}': {}. Trying next name in the sequence"
                                + "(if possible).", candidateNodeId, existingNodes);
                    } else {
                        NodeType existingNode = existingNodes.get(0).asObjectable();
                        if (Boolean.FALSE.equals(existingNode.isRunning())) {
                            LOGGER.info("Considering using the node name of '{}' that already exists but is marked as being down"
                                    + " (OID {}). So deleting the node and trying again.", candidateNodeId, existingNode.getOid());
                            try {
                                repositoryService.deleteObject(NodeType.class, existingNode.getOid(), result);
                            } catch (ObjectNotFoundException ex) {
                                LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't delete the node {}. Probably someone"
                                        + " else is faster than us.", ex, existingNode);
                            }
                            builtinResolver.iterationCounter--;     // will retry this node
                        } else {
                            LOGGER.info("Node name '{}' is already reserved. Iterating to next one (if possible).", candidateNodeId);
                        }
                    }

                    if (builtinResolver.iterationCounter < MAX_ITERATIONS) {
                        continue;
                    } else {
                        throw new SystemException("Cannot acquire node name. Maximum number of iterations ("
                                + MAX_ITERATIONS + ") has been reached.");
                    }
                } catch (SchemaException e) {
                    throw new SystemException("Unexpected schema exception while creating temporary node: " + e.getMessage(), e);
                }
            }
            return candidateNodeId;
        }
    }

    @NotNull
    private String getNodeIdFromSource(@NotNull String source) {
        switch (source) {
            case NODE_ID_SOURCE_RANDOM:
                return getNodeIdAsRandomValue(DEFAULT_RANDOM_RANGE);
            case NODE_ID_SOURCE_HOSTNAME:
                return getNodeIdAsHostName();
            default:
                throw new IllegalArgumentException("Unsupported node ID source: " + source);
        }
    }

    @NotNull
    private String getNodeIdAsRandomValue(double range) {
        return "node-" + Math.round(Math.random() * range);
    }

    @NotNull
    private String getNodeIdAsHostName() {
        try {
            String hostName = NodeRegistrar.getLocalHostNameFromOperatingSystem();
            if (hostName != null) {
                return hostName;
            } else {
                LOGGER.error("Couldn't determine nodeId as host name couldn't be obtained from the operating system");
                throw new SystemException(
                        "Couldn't determine nodeId as host name couldn't be obtained from the operating system");
            }
        } catch (UnknownHostException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine nodeId as host name couldn't be obtained from the operating system", e);
            throw new SystemException(
                    "Couldn't determine nodeId as host name couldn't be obtained from the operating system", e);
        }
    }
}