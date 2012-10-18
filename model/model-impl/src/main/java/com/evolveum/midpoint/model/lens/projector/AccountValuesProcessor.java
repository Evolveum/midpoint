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
package com.evolveum.midpoint.model.lens.projector;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.model.lens.ShadowConstraintsChecker;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.IterationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceAccountTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.model.ModelCompiletimeConfig.CONSISTENCY_CHECKS;

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

	
	public <F extends ObjectType, P extends ObjectType> void process(LensContext<F,P> context, LensProjectionContext<P> projectionContext, 
			String activityDescription, OperationResult result) 
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException {
		LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
    	processAccounts((LensContext<UserType,AccountShadowType>) context, (LensProjectionContext<AccountShadowType>)projectionContext, 
    			activityDescription, result);
	}
	
	public void processAccounts(LensContext<UserType,AccountShadowType> context, LensProjectionContext<AccountShadowType> accountContext, 
			String activityDescription, OperationResult result) 
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException {
		
		PolicyDecision policyDecision = accountContext.getPolicyDecision();
		if (policyDecision != null && policyDecision == PolicyDecision.UNLINK) {
			// We will not update accounts that are being unlinked.
			// we cannot skip deleted accounts here as the delete delta will be skipped as well
			return;
		}
		
		if (CONSISTENCY_CHECKS) context.checkConsistence();
		
		int maxIterations = determineMaxIterations(accountContext);
		int iteration = 0;
		while (true) {
			
			accountContext.setIteration(iteration);
			String iterationToken = formatIterationToken(iteration);
			accountContext.setIterationToken(iterationToken);
			
			if (CONSISTENCY_CHECKS) context.checkConsistence();
			assignmentProcessor.processAssignmentsAccountValues(accountContext, result);
			context.recompute();
			if (CONSISTENCY_CHECKS) context.checkConsistence();
			outboundProcessor.processOutbound(context, accountContext, result);
			context.recompute();
			if (CONSISTENCY_CHECKS) context.checkConsistence();
			consolidationProcessor.consolidateValues(context, accountContext, result);
			if (CONSISTENCY_CHECKS) context.checkConsistence();
	        context.recompute();
	        if (CONSISTENCY_CHECKS) context.checkConsistence();
	 
	        LensUtil.traceContext(LOGGER, activityDescription, "values", context, true);
	        
	        // Check constraints
	        ShadowConstraintsChecker checker = new ShadowConstraintsChecker(accountContext);
	        checker.setPrismContext(prismContext);
	        checker.setContext(context);
	        checker.setRepositoryService(repositoryService);
	        checker.check(result);
	        if (checker.isSatisfiesConstraints()) {
	        	break;
	        }
	        
	        iteration++;
	        if (iteration > maxIterations) {
	        	StringBuilder sb = new StringBuilder();
	        	if (iteration == 1) {
	        		sb.append("Error processing ");
	        	} else {
	        		sb.append("Too many iterations ("+iteration+") for ");
	        	}
	        	sb.append(accountContext.getHumanReadableName());
	        	if (iteration == 1) {
	        		sb.append(": constraint violation: ");
	        	} else {
	        		sb.append(": cannot determine valuest that satisfy constraints: ");
	        	}
	        	sb.append(checker.getMessages());
	        	throw new ObjectAlreadyExistsException(sb.toString());
	        }
	        
	        cleanupContext(accountContext);
	        if (CONSISTENCY_CHECKS) context.checkConsistence();
		} 
		
		if (CONSISTENCY_CHECKS) context.checkConsistence();
					
	}
	
	private int determineMaxIterations(LensProjectionContext<AccountShadowType> accountContext) {
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


	private boolean isInDelta(PrismProperty<?> attr, ObjectDelta<AccountShadowType> delta) {
		if (delta == null) {
			return false;
		}
		return delta.hasItemDelta(new PropertyPath(ResourceObjectShadowType.F_ATTRIBUTES, attr.getName()));
	}

	
	
	/**
	 * Remove the intermediate results of values processing such as secondary deltas.
	 */
	private void cleanupContext(LensProjectionContext<AccountShadowType> accountContext) throws SchemaException {
		accountContext.setSecondaryDelta(null);
		accountContext.clearIntermediateResults();
		accountContext.recompute();
	}

	
}
