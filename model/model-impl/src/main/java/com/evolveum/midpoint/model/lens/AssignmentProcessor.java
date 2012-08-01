/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.model.lens;

import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ExclusionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author semancik
 */
@Component
public class AssignmentProcessor {

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired(required = true)
    private ObjectResolver objectResolver;

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private ValueConstructionFactory valueConstructionFactory;
    
    @Autowired(required = true)
    private ProvisioningService provisioningService;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentProcessor.class);

    public <F extends ObjectType, P extends ObjectType> void processAssignmentsProjections(LensContext<F,P> context, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
    	processAssignmentsAccounts((LensContext<UserType,AccountShadowType>) context, result);
    }
    
    public void processAssignmentsAccounts(LensContext<UserType,AccountShadowType> context, OperationResult result) throws SchemaException,
    		ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	LensFocusContext<UserType> focusContext = context.getFocusContext();
        AccountSynchronizationSettingsType accountSynchronizationSettings = context.getAccountSynchronizationSettings();
        if (accountSynchronizationSettings != null) {
            AssignmentPolicyEnforcementType assignmentPolicyEnforcement = accountSynchronizationSettings.getAssignmentPolicyEnforcement();
            if (assignmentPolicyEnforcement == AssignmentPolicyEnforcementType.NONE) {
                // No assignment processing
                LOGGER.trace("Assignment enforcement policy set to NONE, skipping assignment processing");

                // But mark all accounts as assigned, so they will be synchronized as expected
                for (LensProjectionContext<AccountShadowType> accCtx : context.getProjectionContexts()) {
                    accCtx.setAssigned(true);
                }

                return;
            }
        }
        
        Collection<PrismContainerValue<AssignmentType>> assignmentsOld = new ArrayList<PrismContainerValue<AssignmentType>>();
        if (focusContext.getObjectOld() != null) {
            PrismContainer<AssignmentType> assignmentContainer = focusContext.getObjectOld().findContainer(UserType.F_ASSIGNMENT);
            if (assignmentContainer != null) {
            	assignmentsOld.addAll(assignmentContainer.getValues());
            }
        }

        ContainerDelta<AssignmentType> assignmentDelta = focusContext.getAssignmentDelta();

        LOGGER.trace("Assignment delta {}", assignmentDelta.dump());

        // TODO: preprocess assignment delta. If it is replace, then we need to convert it to: delete all existing assignments, add all new assignments
        Collection<PrismContainerValue<AssignmentType>> changedAssignments = assignmentDelta.getValues(AssignmentType.class);

        AssignmentEvaluator assignmentEvaluator = new AssignmentEvaluator();
        assignmentEvaluator.setRepository(repositoryService);
        assignmentEvaluator.setUserOdo(focusContext.getObjectDeltaObject());
        assignmentEvaluator.setObjectResolver(objectResolver);
        assignmentEvaluator.setPrismContext(prismContext);
        assignmentEvaluator.setValueConstructionFactory(valueConstructionFactory);

        Map<ResourceAccountType, Collection<PrismPropertyValue<AccountConstruction>>> zeroAccountMap = new HashMap<ResourceAccountType, Collection<PrismPropertyValue<AccountConstruction>>>();
        Map<ResourceAccountType, Collection<PrismPropertyValue<AccountConstruction>>> plusAccountMap = new HashMap<ResourceAccountType, Collection<PrismPropertyValue<AccountConstruction>>>();
        Map<ResourceAccountType, Collection<PrismPropertyValue<AccountConstruction>>> minusAccountMap = new HashMap<ResourceAccountType, Collection<PrismPropertyValue<AccountConstruction>>>();

        LOGGER.trace("Old assignments {}", SchemaDebugUtil.prettyPrint(assignmentsOld));
        LOGGER.trace("Changed assignments {}", SchemaDebugUtil.prettyPrint(changedAssignments));

        ObjectType source = null;
        if (focusContext.getObjectOld() != null) {
            source = focusContext.getObjectOld().asObjectable();
        } else if (focusContext.getObjectNew() != null){
            source = focusContext.getObjectNew().asObjectable();
        }
        
        Collection<Assignment> evaluatedAssignmentsZero = new ArrayList<Assignment>();
        Collection<Assignment> evaluatedAssignmentsPlus = new ArrayList<Assignment>();
        
        Collection<PrismContainerValue<AssignmentType>> allAssignments = MiscUtil.union(assignmentsOld, changedAssignments);
        for (PrismContainerValue<AssignmentType> propertyValue : allAssignments) {
            AssignmentType assignmentType = propertyValue.asContainerable();
            
            boolean isAssignmentChanged = containsRealValue(changedAssignments,propertyValue);
            String assignmentPlacementDesc;
            if (isAssignmentChanged) {
            	assignmentPlacementDesc = "delta for "+source;
            } else {
            	assignmentPlacementDesc = source.toString();
            }

            LOGGER.trace("Processing assignment {}", SchemaDebugUtil.prettyPrint(assignmentType));

            Assignment evaluatedAssignment = assignmentEvaluator.evaluate(assignmentType, source, assignmentPlacementDesc, result);
            
            context.rememberResources(evaluatedAssignment.getResources(result));

            // Sort assignments to sets: unchanged (zero), added (plus), removed (minus)
            if (isAssignmentChanged) {
                // There was some change

                if (assignmentDelta.isValueToAdd(propertyValue)) {
                	if (containsRealValue(assignmentsOld, propertyValue)) {
                		// Phantom add: adding assignment that is already there
                        collectToAccountMap(zeroAccountMap, evaluatedAssignment, result);
                        evaluatedAssignmentsZero.add(evaluatedAssignment);
                	}
                    collectToAccountMap(plusAccountMap, evaluatedAssignment, result);
                    evaluatedAssignmentsPlus.add(evaluatedAssignment);
                }
                if (assignmentDelta.isValueToDelete(propertyValue)) {
                    collectToAccountMap(minusAccountMap, evaluatedAssignment, result);
                }

            } else {
                // No change in assignment
                collectToAccountMap(zeroAccountMap, evaluatedAssignment, result);
                evaluatedAssignmentsZero.add(evaluatedAssignment);
            }
        }
        
        checkExclusions(context, evaluatedAssignmentsZero, evaluatedAssignmentsPlus);
        checkExclusions(context, evaluatedAssignmentsPlus, evaluatedAssignmentsPlus);
        
        if (LOGGER.isTraceEnabled()) {
            // Dump the maps
            LOGGER.trace("Account maps:\nZERO:\n{}\nPLUS:\n{}\nMINUS:\n{}\n", new Object[]{dumpAccountMap(zeroAccountMap),
                    dumpAccountMap(plusAccountMap), dumpAccountMap(minusAccountMap)});
        }

        Collection<ResourceAccountType> allAccountTypes = MiscUtil.union(zeroAccountMap.keySet(), plusAccountMap.keySet(), minusAccountMap.keySet());
        for (ResourceAccountType rat : allAccountTypes) {

            if (rat.getResourceOid() == null) {
                throw new IllegalStateException("Resource OID null in ResourceAccountType during assignment processing");
            }
            if (rat.getAccountType() == null) {
                throw new IllegalStateException("Account type is null in ResourceAccountType during assignment processing");
            }

            if (zeroAccountMap.containsKey(rat)) {
                LensProjectionContext<AccountShadowType> accountSyncContext = context.findProjectionContext(rat);
                if (accountSyncContext == null) {
                	// The account should exist before the change but it does not
                	// This happens during reconciliation if there is an inconsistency. Pretend that the assignment was just added. That should do.
                	accountSyncContext = getOrCreateAccountContext(context, rat, result);
                	markPolicyDecision(accountSyncContext, PolicyDecision.ADD);
                	accountSyncContext.setAssigned(true);
                } else {
                	// The account existed before the change and should still exist
	                accountSyncContext.setAssigned(true);
	                markPolicyDecision(accountSyncContext, PolicyDecision.KEEP);
                }

            } else if (plusAccountMap.containsKey(rat) && minusAccountMap.containsKey(rat)) {
            	context.findProjectionContext(rat).setAssigned(true);
                // Account was removed and added in the same operation, therefore keep its original state
                // TODO
                throw new UnsupportedOperationException("add+delete of account is not supported yet");
                //continue;

            } else if (plusAccountMap.containsKey(rat)) {
                // Account added
            	if (accountExists(context,rat)) {
            		LensProjectionContext<AccountShadowType> accountContext = getOrCreateAccountContext(context, rat, result);
            		markPolicyDecision(accountContext, PolicyDecision.KEEP);
            	} else {
            		LensProjectionContext<AccountShadowType> accountContext = getOrCreateAccountContext(context, rat, result);
            		markPolicyDecision(accountContext, PolicyDecision.ADD);
            	}
                context.findProjectionContext(rat).setAssigned(true);

            } else if (minusAccountMap.containsKey(rat)) {
            	if (accountExists(context,rat)) {
            		LensProjectionContext<AccountShadowType> accountContext = getOrCreateAccountContext(context, rat, result);
                	accountContext.setAssigned(false);
                    // Account removed
                    markPolicyDecision(accountContext, PolicyDecision.DELETE);
            	} else {
            		// We have to delete something that is not there. Nothing to do.
            	}

            } else {
                throw new IllegalStateException("Account " + rat + " went looney");
            }

            PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountDeltaSetTriple = 
            		new PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>>(zeroAccountMap.get(rat),
                    plusAccountMap.get(rat), minusAccountMap.get(rat));
            LensProjectionContext<AccountShadowType> accountContext = context.findProjectionContext(rat);
            if (accountContext != null) {
            	// This can be null in a exotic case if we delete already deleted account
            	accountContext.setAccountConstructionDeltaSetTriple(accountDeltaSetTriple);
            }

        }
        
        finishProplicyDecisions(context);
        
    }
    
	/**
	 * Set policy decisions for the accounts that does not have it already
	 */
	private void finishProplicyDecisions(LensContext<UserType,AccountShadowType> context) {
		for (LensProjectionContext<AccountShadowType> accountContext: context.getProjectionContexts()) {
			if (accountContext.getPolicyDecision() != null) {
				// already have decision
				continue;
			}
			ObjectDelta<AccountShadowType> accountSyncDelta = accountContext.getSyncDelta();
			if (accountSyncDelta != null) {
				if (accountSyncDelta.isDelete()) {
					accountContext.setPolicyDecision(PolicyDecision.UNLINK);
				} else {
					accountContext.setPolicyDecision(PolicyDecision.DELETE);
				}
			}
			// TODO: other cases?
		}
		
	}

	private boolean containsRealValue(Collection<PrismContainerValue<AssignmentType>> assignmentValuesCollection,
			PrismContainerValue<AssignmentType> assignmentValue) {
		for (PrismContainerValue<AssignmentType> colValue: assignmentValuesCollection) {
			if (colValue.equalsRealValue(assignmentValue)) {
				return true;
			}
		}
		return false;
	}

	public void processAssignmentsAccountValues(LensProjectionContext<AccountShadowType> accountContext, OperationResult result) throws SchemaException,
		ObjectNotFoundException, ExpressionEvaluationException {
            
		// TODO: reevaluate constructions
		
    }

    private void collectToAccountMap(
            Map<ResourceAccountType, Collection<PrismPropertyValue<AccountConstruction>>> accountMap,
            Assignment evaluatedAssignment, OperationResult result) throws ObjectNotFoundException, SchemaException {
        for (AccountConstruction accountConstruction : evaluatedAssignment.getAccountConstructions()) {
            String resourceOid = accountConstruction.getResource(result).getOid();
            String accountType = accountConstruction.getAccountType();
            ResourceAccountType rat = new ResourceAccountType(resourceOid, accountType);
            Collection<PrismPropertyValue<AccountConstruction>> constructions = null;
            if (accountMap.containsKey(rat)) {
                constructions = accountMap.get(rat);
            } else {
                constructions = new ArrayList<PrismPropertyValue<AccountConstruction>>();
                accountMap.put(rat, constructions);
            }
            constructions.add(new PrismPropertyValue<AccountConstruction>(accountConstruction));
        }
    }

    private String dumpAccountMap(Map<ResourceAccountType, Collection<PrismPropertyValue<AccountConstruction>>> accountMap) {
        StringBuilder sb = new StringBuilder();
        Set<Entry<ResourceAccountType, Collection<PrismPropertyValue<AccountConstruction>>>> entrySet = accountMap.entrySet();
        Iterator<Entry<ResourceAccountType, Collection<PrismPropertyValue<AccountConstruction>>>> i = entrySet.iterator();
        while (i.hasNext()) {
            Entry<ResourceAccountType, Collection<PrismPropertyValue<AccountConstruction>>> entry = i.next();
            sb.append(entry.getKey()).append(": ");
            sb.append(SchemaDebugUtil.prettyPrint(entry.getValue()));
            if (i.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private boolean accountExists(LensContext<UserType,AccountShadowType> context, ResourceAccountType rat) {
    	LensProjectionContext<AccountShadowType> accountSyncContext = context.findProjectionContext(rat);
    	if (accountSyncContext == null) {
    		return false;
    	}
    	if (accountSyncContext.getObjectOld() == null) {
    		return false;
    	}
    	return true;
    }
    
    private LensProjectionContext<AccountShadowType> getOrCreateAccountContext(LensContext<UserType,AccountShadowType> context, 
    		ResourceAccountType rat, OperationResult result) 
    		throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
    	return LensUtil.getOrCreateAccountContext(context, rat, provisioningService, result);
    }
    
    private void markPolicyDecision(LensProjectionContext<AccountShadowType> accountSyncContext, PolicyDecision decision) {
        if (accountSyncContext.getPolicyDecision() == null) {
            accountSyncContext.setPolicyDecision(decision);
        }
    }

	private void checkExclusions(LensContext<UserType,AccountShadowType> context, Collection<Assignment> assignmentsA,
			Collection<Assignment> assignmentsB) throws PolicyViolationException {
		for (Assignment assignmentA: assignmentsA) {
			checkExclusion(context, assignmentA, assignmentsB);
		}
	}

	private void checkExclusion(LensContext<UserType,AccountShadowType> context, Assignment assignmentA,
			Collection<Assignment> assignmentsB) throws PolicyViolationException {
		for (Assignment assignmentB: assignmentsB) {
			checkExclusion(context, assignmentA, assignmentB);
		}
	}

	private void checkExclusion(LensContext<UserType,AccountShadowType> context, Assignment assignmentA, Assignment assignmentB) throws PolicyViolationException {
		if (assignmentA == assignmentB) {
			// Same thing, this cannot exclude itself
			return;
		}
		for(AccountConstruction constructionA: assignmentA.getAccountConstructions()) {
			for(AccountConstruction constructionB: assignmentB.getAccountConstructions()) {
				checkExclusion(constructionA, assignmentA, constructionB, assignmentB);
			}
		}
	}

	private void checkExclusion(AccountConstruction constructionA, Assignment assignmentA,
			AccountConstruction constructionB, Assignment assignmentB) throws PolicyViolationException {
		AssignmentPath pathA = constructionA.getAssignmentPath();
		AssignmentPath pathB = constructionB.getAssignmentPath();
		for (AssignmentPathSegment segmentA: pathA.getSegments()) {
			if (segmentA.getTarget() != null && segmentA.getTarget() instanceof RoleType) {
				for (AssignmentPathSegment segmentB: pathB.getSegments()) {
					if (segmentB.getTarget() != null && segmentB.getTarget() instanceof RoleType) {
						checkExclusion((RoleType)segmentA.getTarget(), (RoleType)segmentB.getTarget());
					}
				}
			}
		}
	}

	private void checkExclusion(RoleType roleA, RoleType roleB) throws PolicyViolationException {
		checkExclusionOneWay(roleA, roleB);
		checkExclusionOneWay(roleB, roleA);
	}

	private void checkExclusionOneWay(RoleType roleA, RoleType roleB) throws PolicyViolationException {
		for (ExclusionType exclusionA :roleA.getExclusion()) {
			ObjectReferenceType targetRef = exclusionA.getTargetRef();
			if (roleB.getOid().equals(targetRef.getOid())) {
				throw new PolicyViolationException("Violation of SoD policy: "+roleA+" excludes "+roleB+
						", they cannot be assigned at the same time");
			}
		}
	}

}
