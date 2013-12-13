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

import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.lens.AccountConstruction;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
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

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private MappingFactory mappingFactory;

    public <F extends FocusType> void processOutbound(LensContext<F> context, LensProjectionContext accCtx, Task task, OperationResult result) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException {

        ResourceShadowDiscriminator rat = accCtx.getResourceShadowDiscriminator();
        ObjectDelta<ShadowType> accountDelta = accCtx.getDelta();

        if (accountDelta != null && accountDelta.getChangeType() == ChangeType.DELETE) {
            LOGGER.trace("Processing outbound expressions for account {} skipped, DELETE account delta", rat);
            // No point in evaluating outbound
            return;
        }

        LOGGER.trace("Processing outbound expressions for account {} starting", rat);

        RefinedObjectClassDefinition rAccount = accCtx.getRefinedAccountDefinition();
        if (rAccount == null) {
            LOGGER.error("Definition for account type {} not found in the context, but it should be there, dumping context:\n{}", rat, context.dump());
            throw new IllegalStateException("Definition for account type " + rat + " not found in the context, but it should be there");
        }
        
        ObjectDeltaObject<F> focusOdo = context.getFocusContext().getObjectDeltaObject();
        ObjectDeltaObject<ShadowType> projectionOdo = accCtx.getObjectDeltaObject();
        
        AccountConstruction outboundAccountConstruction = new AccountConstruction(null, accCtx.getResource());
        
        String operation = accCtx.getOperation().getValue();

        for (QName attributeName : rAccount.getNamesOfAttributesWithOutboundExpressions()) {
			RefinedAttributeDefinition refinedAttributeDefinition = rAccount.getAttributeDefinition(attributeName);
						
			final MappingType outboundMappingType = refinedAttributeDefinition.getOutboundMappingType();
			if (outboundMappingType == null) {
			    continue;
			}
			
			if (refinedAttributeDefinition.isIgnored(LayerType.MODEL)) {
				LOGGER.trace("Skipping processing outbound mapping for attribute {} because it is ignored", attributeName);
				continue;
			}
			
			// TODO: check access
			
			Mapping<? extends PrismPropertyValue<?>> mapping = mappingFactory.createMapping(outboundMappingType, 
			        "outbound mapping for " + PrettyPrinter.prettyPrint(refinedAttributeDefinition.getName())
			        + " in " + ObjectTypeUtil.toShortString(rAccount.getResourceType()));
			
			if (!mapping.isApplicableToChannel(context.getChannel())) {
				LOGGER.trace("Skipping outbound mapping for {} because the channel does not match", attributeName);
				continue;
			}
			
			// This is just supposed to be an optimization. The consolidation should deal with the weak mapping
			// even if it is there. But in that case we do not need to evaluate it at all.
			if (mapping.getStrength() == MappingStrengthType.WEAK && accCtx.hasValueForAttribute(attributeName)) {
				LOGGER.trace("Skipping outbound mapping for {} because it is weak", attributeName);
				continue;
			}
			
			mapping.setDefaultTargetDefinition(refinedAttributeDefinition);
			mapping.setSourceContext(focusOdo);
			mapping.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo);
			mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo);
			mapping.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, projectionOdo);
			mapping.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, projectionOdo);
			mapping.addVariableDefinition(ExpressionConstants.VAR_ITERATION, 
					LensUtil.getIterationVariableValue(accCtx));
			mapping.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, 
					LensUtil.getIterationTokenVariableValue(accCtx));
			mapping.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, accCtx.getResource());
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
					
					if (outboundMappingType.getExpression() != null){
						List<JAXBElement<?>> evaluators = outboundMappingType.getExpression().getExpressionEvaluator();
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
			
			outboundAccountConstruction.addAttributeConstruction(mapping);
        }
        
        accCtx.setOutboundAccountConstruction(outboundAccountConstruction);
    }
}
