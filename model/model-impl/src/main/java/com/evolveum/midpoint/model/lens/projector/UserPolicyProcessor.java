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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.ModelObjectResolver;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * Processor to handle user template and possible also other user "policy"
 * elements.
 * 
 * @author Radovan Semancik
 * 
 */
@Component
public class UserPolicyProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(UserPolicyProcessor.class);

	@Autowired(required = true)
	private MappingFactory mappingFactory;

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private PasswordPolicyProcessor passwordPolicyProcessor;
	
	@Autowired(required = true)
	private ModelObjectResolver modelObjectResolver;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

	<F extends ObjectType, P extends ObjectType> void processUserPolicy(LensContext<F,P> context, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, PolicyViolationException {

		LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
    	
    	ObjectDelta<F> focusDelta = focusContext.getDelta();
    	if (focusDelta != null && focusDelta.isDelete()) {
    		return;
    	}
    	
		LensContext<UserType, ShadowType> usContext = (LensContext<UserType, ShadowType>) context;
    	//check user password if satisfies policies
		
//		PrismProperty<PasswordType> password = getPasswordValue((LensFocusContext<UserType>)focusContext);
		
//		if (password != null) {
			passwordPolicyProcessor.processPasswordPolicy((LensFocusContext<UserType>) focusContext, usContext, result);
//		}

		applyUserTemplate(usContext, result);
				
	}

	
	private void applyUserTemplate(LensContext<UserType, ShadowType> context, OperationResult result) 
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();

		ObjectTemplateType userTemplate = context.getUserTemplate();

		if (userTemplate == null) {
			// No applicable template
			LOGGER.trace("Skipping processing of user template: no user template");
			return;
		}
		
		LOGGER.trace("Applying " + userTemplate + " to " + focusContext.getObjectNew());

		ObjectDelta<UserType> userSecondaryDelta = focusContext.getProjectionWaveSecondaryDelta();
		ObjectDelta<UserType> userPrimaryDelta = focusContext.getProjectionWavePrimaryDelta();
		ObjectDeltaObject<UserType> userOdo = focusContext.getObjectDeltaObject();
		PrismObjectDefinition<UserType> userDefinition = getUserDefinition();
		
		Map<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap 
			= new HashMap<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>>();
		
		collectTripleFromTemplate(context, userTemplate, userOdo, outputTripleMap, userTemplate.toString(), result);		
		
		for (Entry<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> entry: outputTripleMap.entrySet()) {
			ItemPath itemPath = entry.getKey();
			DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>> outputTriple = entry.getValue();
			
			ItemDelta<? extends PrismValue> apropriItemDelta = null;
			
			ItemDelta<? extends PrismValue> itemDelta = LensUtil.consolidateTripleToDelta(itemPath, (DeltaSetTriple)outputTriple,
					userDefinition.findItemDefinition(itemPath), apropriItemDelta, userOdo.getNewObject(), null, 
					true, true, "user template "+userTemplate, true);
			
			itemDelta.simplify();
			itemDelta.validate("user template "+userTemplate);
			
			if (itemDelta != null && !itemDelta.isEmpty()) {
				if (userPrimaryDelta == null || !userPrimaryDelta.containsModification(itemDelta)) {
					if (userSecondaryDelta == null) {
						userSecondaryDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, prismContext);
						if (focusContext.getObjectNew() != null && focusContext.getObjectNew().getOid() != null){
							userSecondaryDelta.setOid(focusContext.getObjectNew().getOid());
						}
						focusContext.setProjectionWaveSecondaryDelta(userSecondaryDelta);
					}
					userSecondaryDelta.mergeModification(itemDelta);
				}
			}
		}

	}

	private void collectTripleFromTemplate(LensContext<UserType, ShadowType> context,
			ObjectTemplateType objectTemplateType, ObjectDeltaObject<UserType> userOdo,
			Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap,
			String contextDesc, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		// Process includes
		for (ObjectReferenceType includeRef: objectTemplateType.getIncludeRef()) {
			PrismObject<ObjectTemplateType> includeObject = includeRef.asReferenceValue().getObject();
			if (includeObject == null) {
				ObjectTemplateType includeObjectType = modelObjectResolver.resolve(includeRef, ObjectTemplateType.class, "include reference in "+objectTemplateType + " in " + contextDesc, result);
				includeObject = includeObjectType.asPrismObject();
				// Store resolved object for future use (e.g. next waves).
				includeRef.asReferenceValue().setObject(includeObject);
			}
			LOGGER.trace("Including template {}", includeObject);
			ObjectTemplateType includeObjectType = includeObject.asObjectable();
			collectTripleFromTemplate(context, includeObjectType, userOdo, outputTripleMap, "include "+includeObject+" in "+objectTemplateType + " in " + contextDesc, result);
		}
		
		// Process own mappings
		Collection<MappingType> mappings = objectTemplateType.getMapping();
		collectTripleFromMappings(mappings, context, objectTemplateType, userOdo, outputTripleMap, contextDesc, result);
	}
	
	
	private void collectTripleFromMappings(Collection<MappingType> mappings, LensContext<UserType, ShadowType> context,
			ObjectTemplateType objectTemplateType, ObjectDeltaObject<UserType> userOdo,
			Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap,
			String contextDesc, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		for (MappingType mappingType : mappings) {
			collectTripleFromMapping(context, mappingType, objectTemplateType, userOdo, outputTripleMap, contextDesc, result);
		}
	}

	private <V extends PrismValue> void collectTripleFromMapping(final LensContext<UserType, ShadowType> context, 
			MappingType mappingType, ObjectTemplateType userTemplate, ObjectDeltaObject<UserType> userOdo, 
			Map<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<? extends PrismValue>>> outputTripleMap, 
			String contextDesc, OperationResult result) 
					throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		
		Mapping<V> mapping = evaluateMapping(context, mappingType, userTemplate, userOdo, contextDesc, result);
		if (mapping == null) {
			return;
		}
		ItemPath itemPath = mapping.getOutputPath();
		DeltaSetTriple<ItemValueWithOrigin<V>> outputTriple = ItemValueWithOrigin.createOutputTriple(mapping);
		if (outputTriple == null) {
			return;
		}
		DeltaSetTriple<ItemValueWithOrigin<V>> mapTriple = (DeltaSetTriple<ItemValueWithOrigin<V>>) outputTripleMap.get(itemPath);
		if (mapTriple == null) {
			outputTripleMap.put(itemPath, outputTriple);
		} else {
			mapTriple.merge(outputTriple);
		}
		
	}
	
	private <V extends PrismValue> Mapping<V> evaluateMapping(final LensContext<UserType, ShadowType> context, final MappingType mappingType, ObjectTemplateType userTemplate, 
			ObjectDeltaObject<UserType> userOdo, String contextDesc, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		Mapping<V> mapping = mappingFactory.createMapping(mappingType,
				"object template mapping in " + contextDesc
				+ " while processing user " + userOdo.getAnyObject());
		
		if (!mapping.isApplicableToChannel(context.getChannel())) {
			return null;
		}
		
		mapping.setSourceContext(userOdo);
		mapping.setTargetContext(getUserDefinition());
		mapping.setRootNode(userOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, userOdo);
		mapping.setOriginType(OriginType.USER_POLICY);
		mapping.setOriginObject(userTemplate);

		ItemDefinition outputDefinition = mapping.getOutputDefinition();
		ItemPath itemPath = mapping.getOutputPath();
		
		Item<V> existingUserItem = (Item<V>) userOdo.getNewObject().findItem(itemPath);
		if (existingUserItem != null && !existingUserItem.isEmpty() 
				&& mapping.getStrength() == MappingStrengthType.WEAK) {
			// This valueConstruction only applies if the property does not have a value yet.
			// ... but it does
			return null;
		}

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
				if (outputDefinition.getName().equals(PasswordType.F_VALUE)) {
					ValuePolicyType passwordPolicy = context.getGlobalPasswordPolicy();
					if (passwordPolicy == null) {
						return null;
					}
					return passwordPolicy.getStringPolicy();
				}
				if (mappingType.getExpression() != null){
					List<JAXBElement<?>> evaluators = mappingType.getExpression().getExpressionEvaluator();
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

		mapping.evaluate(result);
		
		return mapping;
	}


	private <V extends PrismValue> boolean hasValue(Item<V> existingUserItem, V newValue) {
		if (existingUserItem == null) {
			return false;
		}
		return existingUserItem.contains(newValue, true);
	}


	private PrismObjectDefinition<UserType> getUserDefinition() {
		return prismContext.getSchemaRegistry().getObjectSchema()
				.findObjectDefinitionByCompileTimeClass(UserType.class);
	}

}
