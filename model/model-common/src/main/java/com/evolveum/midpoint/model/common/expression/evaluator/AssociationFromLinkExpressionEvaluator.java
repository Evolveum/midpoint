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

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
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
		String resourceOid = rAssocTargetDef.getResourceOid();
		Collection<SelectorOptions<GetOperationOptions>> options = null;

		// Always process the first role (myself) regardless of recursion setting
		gatherAssociationsFromAbstractRole(thisRole, output, resourceOid, kind, intent, assocName, options, desc, context);

		if (thisRole instanceof OrgType && matchesForRecursion((OrgType)thisRole)) {
			gatherAssociationsFromAbstractRoleRecurse((OrgType)thisRole, output, resourceOid, kind, intent, assocName, options, desc,
					context);
		}

		return ItemDelta.toDeltaSetTriple(output, null);
	}

	private void gatherAssociationsFromAbstractRole(AbstractRoleType thisRole,
			PrismContainer<ShadowAssociationType> output, String resourceOid, ShadowKindType kind,
			String intent, QName assocName, Collection<SelectorOptions<GetOperationOptions>> options,
			String desc, ExpressionEvaluationContext params) throws SchemaException {
		for (ObjectReferenceType linkRef: thisRole.getLinkRef()) {
			ShadowType shadowType;
			try {
				shadowType = objectResolver.resolve(linkRef, ShadowType.class, options, desc, params.getTask(), params.getResult());
			} catch (ObjectNotFoundException e) {
				// Linked shadow not found. This may happen e.g. if the account is deleted and model haven't got
				// the chance to react yet. Just ignore such shadow.
				LOGGER.trace("Ignoring shadow "+linkRef.getOid()+" linked in "+thisRole+" because it no longer exists");
				continue;
			}
			if (ShadowUtil.matches(shadowType, resourceOid, kind, intent)) {
				PrismContainerValue<ShadowAssociationType> newValue = output.createNewValue();
				ShadowAssociationType shadowAssociationType = newValue.asContainerable();
				shadowAssociationType.setName(assocName);
				ObjectReferenceType shadowRef = new ObjectReferenceType();
				shadowRef.setOid(linkRef.getOid());
				shadowAssociationType.setShadowRef(shadowRef);
			}
		}
	}

	private void gatherAssociationsFromAbstractRoleRecurse(OrgType thisOrg,
			PrismContainer<ShadowAssociationType> output, String resourceOid, ShadowKindType kind,
			String intent, QName assocName, Collection<SelectorOptions<GetOperationOptions>> options,
			String desc, ExpressionEvaluationContext params) throws SchemaException, ObjectNotFoundException {

		gatherAssociationsFromAbstractRole(thisOrg, output, resourceOid, kind, intent, assocName, options, desc, params);

		for (ObjectReferenceType parentOrgRef: thisOrg.getParentOrgRef()) {
			OrgType parent = objectResolver.resolve(parentOrgRef, OrgType.class, options, desc, params.getTask(), params.getResult());
			if (matchesForRecursion(parent)) {
				gatherAssociationsFromAbstractRoleRecurse(parent, output, resourceOid, kind, intent, assocName, options, desc, params);
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
