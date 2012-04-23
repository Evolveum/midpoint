/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.synchronizer;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.IterationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceAccountTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * @author semancik
 *
 */
@Component
public class AccountValuesProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(AccountValuesProcessor.class);
	
	@Autowired(required = true)
    private OutboundProcessor outboundProcessor;
	
	@Autowired(required = true)
    private ConsolidationProcessor consolidationProcessor;
	
	@Autowired(required = true)
    private AssignmentProcessor assignmentProcessor;
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	RepositoryService repositoryService;
	
	@Autowired(required = true)
	private PrismContext prismContext;

	
	public void process(SyncContext context, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException {
		
		
		for (AccountSyncContext accountContext: context.getAccountContexts()) {
			
			int maxIterations = determineMaxIterations(accountContext);
			int iteration = 0;
			while (true) {
				
				accountContext.setIteration(iteration);
				String iterationToken = formatIterationToken(iteration);
				accountContext.setIterationToken(iterationToken);
				
				assignmentProcessor.processAssignmentsAccountValues(accountContext, result);
				context.recomputeNew();
				outboundProcessor.processOutbound(context, accountContext, result);
				context.recomputeNew();
				consolidationProcessor.consolidateValues(context, accountContext, result);
		        context.recomputeNew();
		 
		        SynchronizerUtil.traceContext("values", context, true);
		        
		        if (satisfiesConstraints(accountContext, result)) {
		        	break;
		        }
		        
		        iteration++;
		        if (iteration > maxIterations) {
		        	throw new ObjectAlreadyExistsException("Too many iterations ("+iteration+") for account "
		        			+ accountContext.getResourceAccountType() + ", cannot determine valuest that satisfy constraints");
		        }
		        
		        cleanupContext(accountContext);
			} 
			
		}
		
	}
	
	private int determineMaxIterations(AccountSyncContext accountContext) {
		ResourceAccountTypeDefinitionType accDef = accountContext.getResourceAccountTypeDefinitionType();
		if (accDef != null) {
			IterationSpecificationType iteration = accDef.getIteration();
			if (iteration != null) {
				return iteration.getMaxIterations();
			}
		}
		return 0;
	}

	private String formatIterationToken(int iteration) {
		// TODO: flexible token format
		if (iteration == 0) {
			return "";
		}
		return Integer.toString(iteration);
	}

	private boolean satisfiesConstraints(AccountSyncContext accountContext, OperationResult result) throws SchemaException {
		
		RefinedAccountDefinition accountDefinition = accountContext.getRefinedAccountDefinition();
		PrismObject<AccountShadowType> accountNew = accountContext.getAccountNew();
		if (accountNew == null) {
			// This must be delete
			return true;
		}
		PrismContainer<?> attributesContainer = accountNew.findContainer(AccountShadowType.F_ATTRIBUTES);
		Collection<ResourceAttributeDefinition> uniqueAttributeDefs = MiscUtil.union(accountDefinition.getIdentifiers(),
				accountDefinition.getSecondaryIdentifiers());
		LOGGER.trace("Secondary IDs {}", accountDefinition.getSecondaryIdentifiers());
		for (ResourceAttributeDefinition attrDef: uniqueAttributeDefs) {
			PrismProperty<?> attr = attributesContainer.findProperty(attrDef.getName());
			LOGGER.trace("Attempt to check uniquness of {} (def {})", attr, attrDef);
			if (attr == null) {
				continue;
			}
			boolean unique = checkAttributeUniqueness(attr, accountDefinition, accountContext.getResource(), 
					accountContext.getOid(), result);
			if (!unique) {
				return false;
			}
		}
		return true;
	}

	private boolean checkAttributeUniqueness(PrismProperty<?> identifier, RefinedAccountDefinition accountDefinition,
			ResourceType resourceType, String oid, OperationResult result) throws SchemaException {
		QueryType query = QueryUtil.createAttributeQuery(identifier, accountDefinition.getObjectClassDefinition().getTypeName(),
				resourceType, prismContext);
		List<PrismObject<AccountShadowType>> foundObjects = repositoryService.searchObjects(AccountShadowType.class, query, null, result);
		LOGGER.trace("Uniquness check of {} resulted in {} results, using query:\n{}",
				new Object[]{identifier, foundObjects.size(), DOMUtil.serializeDOMToString(query.getFilter())});
		if (foundObjects.isEmpty()) {
			return true;
		}
		if (foundObjects.size() > 1) {
			return false;
		}
		return foundObjects.get(0).getOid().equals(oid);
	}
	
	/**
	 * Remove the intermediate results of values processing such as secondary deltas.
	 */
	private void cleanupContext(AccountSyncContext accountContext) throws SchemaException {
		accountContext.setAccountSecondaryDelta(null);
		accountContext.clearAttributeValueDeltaSetTripleMap();
		accountContext.recomputeAccountNew();
	}

	
}
