/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.evaluator.context.NodeAuthenticationContext;
import com.evolveum.midpoint.authentication.impl.module.authentication.NodeAuthenticationTokenImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@Component
public class NodeAuthenticationEvaluatorImpl extends AuthenticationEvaluatorImpl<NodeAuthenticationContext, NodeAuthenticationTokenImpl> {

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
    @Autowired private TaskManager taskManager;
    @Autowired private Protector protector;

    private static final Trace LOGGER = TraceManager.getTrace(NodeAuthenticationEvaluatorImpl.class);

    private static final String OPERATION_SEARCH_NODE = NodeAuthenticationEvaluatorImpl.class.getName() + ".searchNode";

    @Override
    public NodeAuthenticationTokenImpl authenticate(ConnectionEnvironment connEnv, NodeAuthenticationContext authnCtx) throws BadCredentialsException, AuthenticationCredentialsNotFoundException, DisabledException, LockedException, CredentialsExpiredException, AuthenticationServiceException, AccessDeniedException, UsernameNotFoundException {
        String remoteName = authnCtx.getRemoteName();
        String remoteAddress = authnCtx.getUsername();
        LOGGER.debug("Checking if {} ({}) is a known node", remoteName, remoteAddress);
        OperationResult result = new OperationResult(OPERATION_SEARCH_NODE);

        try {
            List<PrismObject<NodeType>> allNodes = repositoryService.searchObjects(NodeType.class, null, null, result);
            List<PrismObject<NodeType>> matchingNodes = getMatchingNodes(allNodes, remoteName, remoteAddress);

            if (matchingNodes.isEmpty()) {
                LOGGER.debug("Authenticity cannot be established: No matching nodes for remote name '{}' and remote address '{}'",
                        remoteName, remoteAddress);
            } else if (matchingNodes.size() > 1 && !taskManager.isLocalNodeClusteringEnabled()) {
                LOGGER.debug("Authenticity cannot be established: More than one matching node for remote name '{}' and "
                        + "remote address '{}' with local-node clustering disabled: {}", remoteName, remoteAddress, matchingNodes);
            } else {
                assert matchingNodes.size() == 1 || matchingNodes.size() > 1 && taskManager.isLocalNodeClusteringEnabled();
                LOGGER.trace(
                        "Matching result: Node(s) {} recognized as known (remote host name {} or IP address {} matched).",
                        matchingNodes, remoteName, remoteAddress);
                PrismObject<NodeType> actualNode = determineCurrentNode(matchingNodes, authnCtx.getEnteredCredential());
                if (actualNode != null) {
                    LOGGER.trace("Established authenticity for remote {}", actualNode);
                    auditAuthenticationSuccess(actualNode.asObjectable(), connEnv);
                    NodeAuthenticationTokenImpl token = new NodeAuthenticationTokenImpl(actualNode, remoteAddress,
                            Collections.emptyList());
                    token.setAuthenticated(true);
                    return token;
                } else {
                    LOGGER.debug("Authenticity for {} couldn't be established: none of the secrets match", matchingNodes);
                }
            }
        } catch (RuntimeException | SchemaException e) {
            LOGGER.error("Unhandled exception when listing nodes");
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing nodes", e);
        }
        auditAuthenticationFailure(remoteName != null ? remoteName : remoteAddress, connEnv, "Failed to authenticate node.");
        throw new AuthenticationServiceException("web.security.flexAuth.cluster.auth.null");
    }

    private PrismObject<NodeType> determineCurrentNode(List<PrismObject<NodeType>> matchingNodes, String credentials) {
        for (PrismObject<NodeType> matchingNode : matchingNodes) {
            ProtectedStringType encryptedSecret = matchingNode.asObjectable().getSecret();
            if (encryptedSecret != null) {
                String plainSecret;
                try {
                    plainSecret = protector.decryptString(encryptedSecret);
                } catch (EncryptionException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't decrypt node secret for {}", e, matchingNode);
                    continue;
                }
                if (credentials.equals(plainSecret)) {
                    LOGGER.debug("Node secret matches for {}", matchingNode);
                    return matchingNode;
                } else {
                    LOGGER.debug("Node secret does not match for {}", matchingNode);
                }
            } else {
                LOGGER.debug("No secret known for node {}", matchingNode);
            }
        }
        return null;
    }

    private List<PrismObject<NodeType>> getMatchingNodes(List<PrismObject<NodeType>> knownNodes, String remoteName,
            String remoteAddress) {
        LOGGER.trace("Selecting matching node(s) for remote name '{}' and remote address '{}'", remoteName, remoteAddress);
        List<PrismObject<NodeType>> matchingNodes = new ArrayList<>();
        for (PrismObject<NodeType> node : knownNodes) {
            NodeType actualNode = node.asObjectable();
            if (actualNode.getOperationalState() == NodeOperationalStateType.DOWN) {
                // Note that we consider nodes that are STARTING as eligible for authentication (they can issue REST calls)
                LOGGER.trace("Skipping {} because it has operationalState=DOWN", actualNode);
            } else if (remoteName != null && remoteName.equalsIgnoreCase(actualNode.getHostname())) {
                LOGGER.trace("The node {} was recognized as a known node (remote host name {} matched).",
                        actualNode.getName(), actualNode.getHostname());
                matchingNodes.add(node);
            } else if (actualNode.getIpAddress().contains(remoteAddress)) {
                LOGGER.trace("The node {} was recognized as a known node (remote host address {} matched).",
                        actualNode.getName(), remoteAddress);
                matchingNodes.add(node);
            }
        }
        // We should eliminate "not checking in" nodes if there are more possibilities
        if (matchingNodes.size() > 1) {
            List<PrismObject<NodeType>> up = matchingNodes.stream()
                    .filter(node -> taskManager.isCheckingIn(node.asObjectable()))
                    .collect(Collectors.toList());
            LOGGER.trace("Tried to eliminate nodes that are not checking in; found {} node(s) that are up: {}", up.size(), up);
            if (up.size() == 1) {
                return up;
            }
            // Nothing reasonable can be done here. Let's return all the nodes.
        }
        return matchingNodes;
    }
}
