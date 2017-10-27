/*
 * Copyright (c) 2014-2017 Evolveum
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
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationFromLinkExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.collections4.CollectionUtils;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;

/**
 * @author Radovan Semancik
 *
 */
public class AssociationFromLinkExpressionEvaluator 
						implements ExpressionEvaluator<PrismContainerValue<ShadowAssociationType>,
						                               PrismContainerDefinition<ShadowAssociationType>> {
	
	private static final Trace LOGGER = TraceManager.getTrace(AssociationFromLinkExpressionEvaluator.class);

	private AssociationFromLinkExpressionEvaluatorType evaluatorType;
	private PrismContainerDefinition<ShadowAssociationType> outputDefinition;
	private ObjectResolver objectResolver;
	private PrismContext prismContext;

	AssociationFromLinkExpressionEvaluator(AssociationFromLinkExpressionEvaluatorType evaluatorType, 
			PrismContainerDefinition<ShadowAssociationType> outputDefinition, ObjectResolver objectResolver, PrismContext prismContext) {
		this.evaluatorType = evaluatorType;
		this.outputDefinition = outputDefinition;
		this.objectResolver = objectResolver;
		this.prismContext = prismContext;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluate(java.util.Collection, java.util.Map, boolean, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public PrismValueDeltaSetTriple<PrismContainerValue<ShadowAssociationType>> evaluate(ExpressionEvaluationContext context) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {
				            
		String desc = context.getContextDescription();
		Object orderOneObject = context.getVariables().get(ExpressionConstants.VAR_ORDER_ONE_OBJECT);
		if (orderOneObject == null) {
			throw new ExpressionEvaluationException("No order one object variable in "+desc+"; the expression may be used in a wrong place. It is only supposed to work in a role.");
		}
		if (!(orderOneObject instanceof AbstractRoleType)) {
			throw new ExpressionEvaluationException("Order one object variable in "+desc+" is not a role, it is "+orderOneObject.getClass().getName()
					+"; the expression may be used in a wrong place. It is only supposed to work in a role.");
		}
		AbstractRoleType thisRole = (AbstractRoleType)orderOneObject;
		
		LOGGER.trace("Evaluating association from link on: {}", thisRole); 
		
		RefinedObjectClassDefinition rAssocTargetDef = (RefinedObjectClassDefinition) context.getVariables().get(ExpressionConstants.VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION);
		if (rAssocTargetDef == null) {
			throw new ExpressionEvaluationException("No association target object class definition variable in "+desc+"; the expression may be used in a wrong place. It is only supposed to create an association.");
		}		
		
		ShadowDiscriminatorType projectionDiscriminator = evaluatorType.getProjectionDiscriminator();
		if (projectionDiscriminator == null) {
			throw new ExpressionEvaluationException("No projectionDiscriminator in "+desc);
		}
		ShadowKindType kind = projectionDiscriminator.getKind();
		if (kind == null) {
			throw new ExpressionEvaluationException("No kind in projectionDiscriminator in "+desc);
		}
		String intent = projectionDiscriminator.getIntent();
		
		PrismContainer<ShadowAssociationType> output = outputDefinition.instantiate();
		
		QName assocName = context.getMappingQName();
		String resourceOid = rAssocTargetDef.getResourceType().getOid();
		Collection<SelectorOptions<GetOperationOptions>> options = null;

		List<String> candidateShadowOidList = new ArrayList<>();

		// Always process the first role (myself) regardless of recursion setting
		gatherCandidateShadowsFromAbstractRole(thisRole, candidateShadowOidList);
		
		if (thisRole instanceof OrgType && matchesForRecursion((OrgType)thisRole)) {
			gatherCandidateShadowsFromAbstractRoleRecurse((OrgType)thisRole, candidateShadowOidList, options, desc, context);
		}

		LOGGER.trace("Candidate shadow OIDs: {}", candidateShadowOidList);

		selectMatchingShadows(candidateShadowOidList, output, resourceOid, kind, intent, assocName, context);
		
		return ItemDelta.toDeltaSetTriple(output, null);
	}

	private void selectMatchingShadows(List<String> candidateShadowsOidList,
			PrismContainer<ShadowAssociationType> output, String resourceOid, ShadowKindType kind,
			String intent, QName assocName, ExpressionEvaluationContext params)
			throws SchemaException {

		S_AtomicFilterExit filter = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.id(candidateShadowsOidList.toArray(new String[0]))
				.and().item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
				.and().item(ShadowType.F_KIND).eq(kind);
		if (intent != null) {
			filter = filter.and().item(ShadowType.F_INTENT).eq(intent);
		}
		ObjectQuery query = filter.build();

		ResultHandler<ShadowType> handler = (object, parentResult) -> {
			PrismContainerValue<ShadowAssociationType> newValue = output.createNewValue();
			ShadowAssociationType shadowAssociationType = newValue.asContainerable();
			shadowAssociationType.setName(assocName);
			shadowAssociationType.setShadowRef(new ObjectReferenceType().oid(object.getOid()).type(ShadowType.COMPLEX_TYPE));
			return true;
		};
		try {
			objectResolver.searchIterative(ShadowType.class, query, createNoFetchCollection(), handler, params.getTask(), params.getResult());
		} catch (CommonException e) {
			throw new SystemException("Couldn't search for relevant shadows: " + e.getMessage(), e);
		}
	}

	private void gatherCandidateShadowsFromAbstractRole(AbstractRoleType thisRole, List<String> candidateShadowsOidList) {
		for (ObjectReferenceType linkRef: thisRole.getLinkRef()) {
			CollectionUtils.addIgnoreNull(candidateShadowsOidList, linkRef.getOid());
		}
	}

	private void gatherCandidateShadowsFromAbstractRoleRecurse(OrgType thisOrg, List<String> candidateShadowsOidList,
			Collection<SelectorOptions<GetOperationOptions>> options, String desc, ExpressionEvaluationContext params)
			throws SchemaException, ObjectNotFoundException {
		for (ObjectReferenceType parentOrgRef: thisOrg.getParentOrgRef()) {
			OrgType parent = objectResolver.resolve(parentOrgRef, OrgType.class, options, desc, params.getTask(), params.getResult());
			if (matchesForRecursion(parent)) {
				gatherCandidateShadowsFromAbstractRole(parent, candidateShadowsOidList);
				gatherCandidateShadowsFromAbstractRoleRecurse(parent, candidateShadowsOidList, options, desc, params);
			}
		}
	}
	
	private boolean matchesForRecursion(OrgType thisOrg) {
		for (String recurseUpOrgType: evaluatorType.getRecurseUpOrgType()) {
			if (thisOrg.getOrgType().contains(recurseUpOrgType)) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "associationFromLink";
	}

}
