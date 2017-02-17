/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.Construction;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

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

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private MappingFactory mappingFactory;
    
    @Autowired
    private MappingEvaluator mappingEvaluator;

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

        RefinedObjectClassDefinition rOcDef = projCtx.getStructuralObjectClassDefinition();
        if (rOcDef == null) {
            LOGGER.error("Definition for {} not found in the context, but it should be there, dumping context:\n{}", discr, context.debugDump());
            throw new IllegalStateException("Definition for " + discr + " not found in the context, but it should be there");
        }
        
        ObjectDeltaObject<F> focusOdo = context.getFocusContext().getObjectDeltaObject();
        ObjectDeltaObject<ShadowType> projectionOdo = projCtx.getObjectDeltaObject();
        
        Construction<F> outboundConstruction = new Construction<>(null, projCtx.getResource());
        outboundConstruction.setRefinedObjectClassDefinition(rOcDef);
        
        Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions = rOcDef.getAuxiliaryObjectClassDefinitions();
        if (auxiliaryObjectClassDefinitions != null) {
        	for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
        		outboundConstruction.addAuxiliaryObjectClassDefinition(auxiliaryObjectClassDefinition);
        	}
        }
        
        String operation = projCtx.getOperation().getValue();

        for (QName attributeName : rOcDef.getNamesOfAttributesWithOutboundExpressions()) {
			RefinedAttributeDefinition<?> refinedAttributeDefinition = rOcDef.findAttributeDefinition(attributeName);
						
			final MappingType outboundMappingType = refinedAttributeDefinition.getOutboundMappingType();
			if (outboundMappingType == null) {
			    continue;
			}
			
			if (refinedAttributeDefinition.isIgnored(LayerType.MODEL)) {
				LOGGER.trace("Skipping processing outbound mapping for attribute {} because it is ignored", attributeName);
				continue;
			}
			
			Mapping.Builder<PrismPropertyValue<?>,RefinedAttributeDefinition<?>> builder = mappingFactory.createMappingBuilder(outboundMappingType,
			        "outbound mapping for " + PrettyPrinter.prettyPrint(refinedAttributeDefinition.getName())
			        + " in " + rOcDef.getResourceType());
			builder = builder.originObject(rOcDef.getResourceType())
					.originType(OriginType.OUTBOUND);
			Mapping<PrismPropertyValue<?>,RefinedAttributeDefinition<?>> evaluatedMapping = evaluateMapping(builder, attributeName, refinedAttributeDefinition,
					focusOdo, projectionOdo, operation, rOcDef, null, context, projCtx, task, result);
			
			if (evaluatedMapping != null) {
				outboundConstruction.addAttributeMapping(evaluatedMapping);
			}
        }
        
        for (QName assocName : rOcDef.getNamesOfAssociationsWithOutboundExpressions()) {
			RefinedAssociationDefinition associationDefinition = rOcDef.findAssociationDefinition(assocName);
						
			final MappingType outboundMappingType = associationDefinition.getOutboundMappingType();
			if (outboundMappingType == null) {
			    continue;
			}
			
//			if (associationDefinition.isIgnored(LayerType.MODEL)) {
//				LOGGER.trace("Skipping processing outbound mapping for attribute {} because it is ignored", assocName);
//				continue;
//			}
			
			Mapping.Builder<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> mappingBuilder = mappingFactory.createMappingBuilder(outboundMappingType,
			        "outbound mapping for " + PrettyPrinter.prettyPrint(associationDefinition.getName())
			        + " in " + rOcDef.getResourceType());

			PrismContainerDefinition<ShadowAssociationType> outputDefinition = getAssociationContainerDefinition();
			Mapping<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> evaluatedMapping = (Mapping) evaluateMapping(mappingBuilder,
					assocName, outputDefinition, focusOdo, projectionOdo, operation, rOcDef, 
					associationDefinition.getAssociationTarget(), context, projCtx, task, result);
			
			if (evaluatedMapping != null) {
				outboundConstruction.addAssociationMapping(evaluatedMapping);
			}
        }
        
        projCtx.setOutboundConstruction(outboundConstruction);
    }
    
    private <F extends FocusType, V extends PrismValue, D extends ItemDefinition> Mapping<V, D> evaluateMapping(final Mapping.Builder<V,D> mappingBuilder, QName mappingQName,
    		D targetDefinition, ObjectDeltaObject<F> focusOdo, ObjectDeltaObject<ShadowType> projectionOdo,
    		String operation, RefinedObjectClassDefinition rOcDef, RefinedObjectClassDefinition assocTargetObjectClassDefinition,
    		LensContext<F> context, LensProjectionContext projCtx, final Task task, OperationResult result)
    				throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		if (!mappingBuilder.isApplicableToChannel(context.getChannel())) {
			LOGGER.trace("Skipping outbound mapping for {} because the channel does not match", mappingQName);
			return null;
		}
		
		// TODO: check access
		
		// This is just supposed to be an optimization. The consolidation should deal with the weak mapping
		// even if it is there. But in that case we do not need to evaluate it at all.
		if (mappingBuilder.getStrength() == MappingStrengthType.WEAK && projCtx.hasValueForAttribute(mappingQName)) {
			LOGGER.trace("Skipping outbound mapping for {} because it is weak", mappingQName);
			return null;
		}
		
		mappingBuilder.setDefaultTargetDefinition(targetDefinition);
		mappingBuilder.setSourceContext(focusOdo);
		mappingBuilder.setMappingQName(mappingQName);
		mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo);
		mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo);
		mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, projectionOdo);
		mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_SHADOW, projectionOdo);
		mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, projectionOdo);
		mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, context.getSystemConfiguration());
		mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ITERATION,
				LensUtil.getIterationVariableValue(projCtx));
		mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN,
				LensUtil.getIterationTokenVariableValue(projCtx));
		mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projCtx.getResource());
		mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_OPERATION, operation);
		if (assocTargetObjectClassDefinition != null) {
			mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION, assocTargetObjectClassDefinition);
		}
		mappingBuilder.setRootNode(focusOdo);
		mappingBuilder.setOriginType(OriginType.OUTBOUND);
		mappingBuilder.setRefinedObjectClassDefinition(rOcDef);
		
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
				
				if (mappingBuilder.getMappingType().getExpression() != null) {
					List<JAXBElement<?>> evaluators = mappingBuilder.getMappingType().getExpression().getExpressionEvaluator();
					for (JAXBElement jaxbEvaluator : evaluators) {
						Object object = jaxbEvaluator.getValue();
						if (object instanceof GenerateExpressionEvaluatorType && ((GenerateExpressionEvaluatorType) object).getValuePolicyRef() != null) {
							ObjectReferenceType ref = ((GenerateExpressionEvaluatorType) object).getValuePolicyRef();
							try {
								ValuePolicyType valuePolicyType = mappingBuilder.getObjectResolver().resolve(ref, ValuePolicyType.class,
										null, "resolving value policy for generate attribute "+ outputDefinition.getName()+"value", task, new OperationResult("Resolving value policy"));
								if (valuePolicyType != null) {
									return valuePolicyType.getStringPolicy();
								}
							} catch (CommonException ex) {
								throw new SystemException(ex.getMessage(), ex);
							}
						}
					}
				}
				return null;
			}
		};
		mappingBuilder.setStringPolicyResolver(stringPolicyResolver);
		// TODO: other variables?
		
		// Set condition masks. There are used as a brakes to avoid evaluating to nonsense values in case user is not present
		// (e.g. in old values in ADD situations and new values in DELETE situations).
		if (focusOdo.getOldObject() == null) {
			mappingBuilder.setConditionMaskOld(false);
		}
		if (focusOdo.getNewObject() == null) {
			mappingBuilder.setConditionMaskNew(false);
		}

		Mapping<V,D> mapping = mappingBuilder.build();
		mappingEvaluator.evaluateMapping(mapping, context, task, result);
    	
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
