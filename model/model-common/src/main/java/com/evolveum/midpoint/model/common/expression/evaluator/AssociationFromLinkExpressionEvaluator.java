/*
 * Copyright (c) 2014 Evolveum
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
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.policy.ValuePolicyGenerator;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowDiscriminatorExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;

/**
 * @author Radovan Semancik
 *
 */
public class AssociationFromLinkExpressionEvaluator 
						implements ExpressionEvaluator<PrismContainerValue<ShadowAssociationType>> {
	
	private static final Trace LOGGER = TraceManager.getTrace(AssociationFromLinkExpressionEvaluator.class);

	private ShadowDiscriminatorExpressionEvaluatorType evaluatorType;
	private PrismContainerDefinition<ShadowAssociationType> outputDefinition;
	private ObjectResolver objectResolver;
	private PrismContext prismContext;

	AssociationFromLinkExpressionEvaluator(ShadowDiscriminatorExpressionEvaluatorType evaluatorType, 
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
	public PrismValueDeltaSetTriple<PrismContainerValue<ShadowAssociationType>> evaluate(ExpressionEvaluationContext params) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException {
				            
		String desc = params.getContextDescription();
		Object orderOneObject = params.getVariables().get(ExpressionConstants.VAR_ORDER_ONE_OBJECT);
		if (orderOneObject == null) {
			throw new ExpressionEvaluationException("No order one object variable in "+desc+"; the expression may be used in a wrong place. It is only supposed to work in a role.");
		}
		if (!(orderOneObject instanceof AbstractRoleType)) {
			throw new ExpressionEvaluationException("Order one object variable in "+desc+" is not a role, it is "+orderOneObject.getClass().getName()
					+"; the expression may be used in a wrong place. It is only supposed to work in a role.");
		}
		AbstractRoleType thisRole = (AbstractRoleType)orderOneObject;
		
		RefinedObjectClassDefinition rAssocTargetDef = (RefinedObjectClassDefinition) params.getVariables().get(ExpressionConstants.VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION);
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
		
		QName assocName = params.getMappingQName();
		String resourceOid = rAssocTargetDef.getResourceType().getOid();
		Collection<SelectorOptions<GetOperationOptions>> options = null;
		for (ObjectReferenceType linkRef: thisRole.getLinkRef()) {
			ShadowType shadowType;
			try {
				shadowType = objectResolver.resolve(linkRef, ShadowType.class, options, desc, params.getResult());
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
		
		return ItemDelta.toDeltaSetTriple(output, null);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "associationFromLink";
	}

}
