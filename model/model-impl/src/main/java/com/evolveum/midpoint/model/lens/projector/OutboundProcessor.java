/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.lens.projector;

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.lens.Construction;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * Processor that evaluates values of the outbound mappings. It does not create the deltas yet. It just collects the
 * evaluated mappings in account context.
 * 
 * @author Radovan Semancik
 */
@Component
public class OutboundProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundProcessor.class);

    private PrismContainerDefinition<ShadowAssociationType> associationContainerDefinition;

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private MappingFactory mappingFactory;

    public <F extends FocusType> void processOutbound(LensContext<F> context, LensProjectionContext projCtx, Task task, OperationResult result) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException {

        ResourceShadowDiscriminator discr = projCtx.getResourceShadowDiscriminator();
        ObjectDelta<ShadowType> projectionDelta = projCtx.getDelta();

        if (projectionDelta != null && projectionDelta.getChangeType() == ChangeType.DELETE) {
            LOGGER.trace("Processing outbound expressions for {} skipped, DELETE account delta", discr);
            // No point in evaluating outbound
            return;
        }

        LOGGER.trace("Processing outbound expressions for {} starting", discr);

        RefinedObjectClassDefinition rOcDef = projCtx.getRefinedAccountDefinition();
        if (rOcDef == null) {
            LOGGER.error("Definition for {} not found in the context, but it should be there, dumping context:\n{}", discr, context.dump());
            throw new IllegalStateException("Definition for " + discr + " not found in the context, but it should be there");
        }
        
        ObjectDeltaObject<F> focusOdo = context.getFocusContext().getObjectDeltaObject();
        ObjectDeltaObject<ShadowType> projectionOdo = projCtx.getObjectDeltaObject();
        
        Construction outboundConstruction = new Construction(null, projCtx.getResource());
        
        String operation = projCtx.getOperation().getValue();

        for (QName attributeName : rOcDef.getNamesOfAttributesWithOutboundExpressions()) {
			RefinedAttributeDefinition refinedAttributeDefinition = rOcDef.getAttributeDefinition(attributeName);
						
			final MappingType outboundMappingType = refinedAttributeDefinition.getOutboundMappingType();
			if (outboundMappingType == null) {
			    continue;
			}
			
			if (refinedAttributeDefinition.isIgnored(LayerType.MODEL)) {
				LOGGER.trace("Skipping processing outbound mapping for attribute {} because it is ignored", attributeName);
				continue;
			}
			
			Mapping<? extends PrismPropertyValue<?>> mapping = mappingFactory.createMapping(outboundMappingType, 
			        "outbound mapping for " + PrettyPrinter.prettyPrint(refinedAttributeDefinition.getName())
			        + " in " + rOcDef.getResourceType());

			Mapping<? extends PrismPropertyValue<?>> evaluatedMapping = evaluateMapping(mapping, attributeName, refinedAttributeDefinition, focusOdo, projectionOdo,
					operation, context, projCtx, task, result);
			
			if (evaluatedMapping != null) {
				outboundConstruction.addAttributeMapping(evaluatedMapping);
			}
        }
        
        for (QName assocName : rOcDef.getNamesOfAssociationsWithOutboundExpressions()) {
			ResourceObjectAssociationType associationDefinition = rOcDef.findAssociation(assocName);
						
			final MappingType outboundMappingType = associationDefinition.getOutbound();
			if (outboundMappingType == null) {
			    continue;
			}
			
//			if (associationDefinition.isIgnored(LayerType.MODEL)) {
//				LOGGER.trace("Skipping processing outbound mapping for attribute {} because it is ignored", assocName);
//				continue;
//			}
			
			Mapping<? extends PrismPropertyValue<?>> mapping = mappingFactory.createMapping(outboundMappingType, 
			        "outbound mapping for " + PrettyPrinter.prettyPrint(associationDefinition.getName())
			        + " in " + rOcDef.getResourceType());

			PrismContainerDefinition<ShadowAssociationType> outputDefinition = getAssociationContainerDefinition();
			Mapping<? extends PrismPropertyValue<?>> evaluatedMapping = evaluateMapping(mapping, assocName, outputDefinition, focusOdo, projectionOdo,
					operation, context, projCtx, task, result);
			
			if (evaluatedMapping != null) {
				outboundConstruction.addAssociationMapping(evaluatedMapping);
			}
        }
        
        projCtx.setOutboundConstruction(outboundConstruction);
    }
    
    private <F extends FocusType, V extends PrismValue> Mapping<V> evaluateMapping(final Mapping<V> mapping, QName mappingQName,
    		ItemDefinition targetDefinition, ObjectDeltaObject<F> focusOdo, ObjectDeltaObject<ShadowType> projectionOdo,
    		String operation,
    		LensContext<F> context, LensProjectionContext projCtx, Task task, OperationResult result) 
    				throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (!mapping.isApplicableToChannel(context.getChannel())) {
			LOGGER.trace("Skipping outbound mapping for {} because the channel does not match", mappingQName);
			return null;
		}
		
		// TODO: check access
		
		// This is just supposed to be an optimization. The consolidation should deal with the weak mapping
		// even if it is there. But in that case we do not need to evaluate it at all.
		if (mapping.getStrength() == MappingStrengthType.WEAK && projCtx.hasValueForAttribute(mappingQName)) {
			LOGGER.trace("Skipping outbound mapping for {} because it is weak", mappingQName);
			return null;
		}
		
		mapping.setDefaultTargetDefinition(targetDefinition);
		mapping.setSourceContext(focusOdo);
		mapping.setMappingQName(mappingQName);
		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, projectionOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, projectionOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_ITERATION, 
				LensUtil.getIterationVariableValue(projCtx));
		mapping.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, 
				LensUtil.getIterationTokenVariableValue(projCtx));
		mapping.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projCtx.getResource());
		mapping.addVariableDefinition(ExpressionConstants.VAR_OPERATION, operation);
		mapping.setRootNode(focusOdo);
		mapping.setOriginType(OriginType.OUTBOUND);
		
		StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
			private ItemPath outputPath;
			private ItemDefinition outputDefinition;
			@Override
			public void setOutputPath(ItemPath outputPath) {
				this.outputPath = outputPath;
			}
			
			@Override
			public void setOutputDefinition(ItemDefinition outputDefinition) {
				this.outputDefinition = outputDefinition;
			}
			
			@Override
			public StringPolicyType resolve() {
				
				if (mapping.getMappingType().getExpression() != null){
					List<JAXBElement<?>> evaluators = mapping.getMappingType().getExpression().getExpressionEvaluator();
					if (evaluators != null){
						for (JAXBElement jaxbEvaluator : evaluators){
							Object object = jaxbEvaluator.getValue();
							if (object != null && object instanceof GenerateExpressionEvaluatorType && ((GenerateExpressionEvaluatorType) object).getValuePolicyRef() != null){
								ObjectReferenceType ref = ((GenerateExpressionEvaluatorType) object).getValuePolicyRef();
								try{
								ValuePolicyType valuePolicyType = mappingFactory.getObjectResolver().resolve(ref, ValuePolicyType.class, "resolving value policy for generate attribute "+ outputDefinition.getName()+"value", new OperationResult("Resolving value policy"));
								if (valuePolicyType != null){
									return valuePolicyType.getStringPolicy();
								}
								} catch (Exception ex){
									throw new SystemException(ex.getMessage(), ex);
								}
							}
						}
						
					}
				}
				return null;
			}
		};
		mapping.setStringPolicyResolver(stringPolicyResolver);
		// TODO: other variables?
		
		// Set condition masks. There are used as a brakes to avoid evaluating to nonsense values in case user is not present
		// (e.g. in old values in ADD situations and new values in DELETE situations).
		if (focusOdo.getOldObject() == null) {
			mapping.setConditionMaskOld(false);
		}
		if (focusOdo.getNewObject() == null) {
			mapping.setConditionMaskNew(false);
		}
		
		LensUtil.evaluateMapping(mapping, context, task, result);
    	
		return mapping;
    }
    
    private PrismContainerDefinition<ShadowAssociationType> getAssociationContainerDefinition() {
		if (associationContainerDefinition == null) {
			PrismObjectDefinition<ShadowType> shadowDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
			associationContainerDefinition = shadowDefinition.findContainerDefinition(ShadowType.F_ASSOCIATION);
		}
		return associationContainerDefinition;
	}
}
