package com.evolveum.midpoint.model.impl.security;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.model.api.authentication.NodeAuthenticationEvaluator;

@Component
public class NodeAuthenticationEvaluatorImpl implements NodeAuthenticationEvaluator {
	
	@Autowired 
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	@Autowired private PrismContext prismContext;
	

	@Autowired SecurityHelper securityHelper;
	
	private static final Trace LOGGER = TraceManager.getTrace(NodeAuthenticationEvaluatorImpl.class);
	
	private static final String OPERATION_SEARCH_NODE = NodeAuthenticationEvaluatorImpl.class.getName() + ".searchNode";
	
	public boolean authenticate(String remoteName, String remoteAddress, String operation) {
		LOGGER.debug("Checking if {} is a known node", remoteName);
		OperationResult result = new OperationResult(OPERATION_SEARCH_NODE);
		
		ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_REST_URI);
		
		try {

			List<PrismObject<NodeType>> knownNodes = repositoryService.searchObjects(NodeType.class,
					null, null, result);

			List<PrismObject<NodeType>> matchingNodes = getMatchingNodes(knownNodes, remoteName, remoteAddress, operation);
			
			if (matchingNodes.size() == 1) {
				PrismObject<NodeType> actualNode = knownNodes.iterator().next();
				LOGGER.trace(
						"The node {} was recognized as a known node (remote host name {} matched). Attempting to execute the requested operation: {} ",
						actualNode.asObjectable().getName(), actualNode.asObjectable().getHostname(), operation);
				NodeAuthenticationToken authNtoken = new NodeAuthenticationToken(actualNode, remoteAddress,
						CollectionUtils.EMPTY_COLLECTION);
				SecurityContextHolder.getContext().setAuthentication(authNtoken);
				securityHelper.auditLoginSuccess(actualNode.asObjectable(), connEnv);
				return true;
			}
			
		} catch (RuntimeException | SchemaException e) {
			LOGGER.error("Unhandled exception when listing nodes");
			LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing nodes", e);
		}
		securityHelper.auditLoginFailure(remoteName != null ? remoteName : remoteAddress, null, connEnv, "Failed to authneticate node.");
		return false;
	}
	
private List<PrismObject<NodeType>> getMatchingNodes(List<PrismObject<NodeType>> knownNodes, String remoteName, String remoteAddress, String operation) {
	List<PrismObject<NodeType>> matchingNodes = new ArrayList<>();
	for (PrismObject<NodeType> node : knownNodes) {
        NodeType actualNode = node.asObjectable();
        if (remoteName != null && remoteName.equalsIgnoreCase(actualNode.getHostname())) {
            LOGGER.trace("The node {} was recognized as a known node (remote host name {} matched). Attempting to execute the requested operation: {} ",
                    actualNode.getName(), actualNode.getHostname(), operation);
            matchingNodes.add(node);
            continue;
        }
        if (actualNode.getIpAddress().contains(remoteAddress)) {
            LOGGER.trace("The node {} was recognized as a known node (remote host address {} matched). Attempting to execute the requested operation: {} ",
                    actualNode.getName(), remoteAddress, operation);
            matchingNodes.add(node);
            continue;
        }
    }
	
	return matchingNodes;
}

	
	
}
