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
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.password.PasswordPolicyUtils;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

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
    	
		LensContext<UserType, AccountShadowType> usContext = (LensContext<UserType, AccountShadowType>) context;
    	//check user password if satisfies policies
		
//		PrismProperty<PasswordType> password = getPasswordValue((LensFocusContext<UserType>)focusContext);
		
//		if (password != null) {
			passwordPolicyProcessor.processPasswordPolicy((LensFocusContext<UserType>) focusContext, usContext, result);
//		}

		applyUserTemplate(usContext, result);
				
	}

	
	private void applyUserTemplate(LensContext<UserType, AccountShadowType> context,
			OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();

		UserTemplateType userTemplate = context.getUserTemplate();

		if (userTemplate == null) {
			// No applicable template
			LOGGER.trace("Skipping processing of user template: no user template");
			return;
		}
		
		LOGGER.trace("Applying " + userTemplate + " to " + focusContext.getObjectNew());

		ObjectDelta<UserType> userSecondaryDelta = focusContext.getWaveSecondaryDelta();
		ObjectDelta<UserType> userPrimaryDelta = focusContext.getWavePrimaryDelta();
		ObjectDeltaObject<UserType> userOdo = focusContext.getObjectDeltaObject();
		for (MappingType mappingType : userTemplate.getMapping()) {
			ItemDelta<PrismValue> itemDelta = evaluateMapping(context, mappingType, userTemplate, userOdo, result);
			
			if (userPrimaryDelta == null || !userPrimaryDelta.containsModification(itemDelta)) {
				if (itemDelta != null && !itemDelta.isEmpty()) {
					if (userSecondaryDelta == null) {
						userSecondaryDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY, prismContext);
						focusContext.setWaveSecondaryDelta(userSecondaryDelta);
					}
					userSecondaryDelta.mergeModification(itemDelta);	
				}
			}
		}

	}

	private <V extends PrismValue> ItemDelta<V> evaluateMapping(final LensContext<UserType, AccountShadowType> context, MappingType mappingType, UserTemplateType userTemplate, 
			ObjectDeltaObject<UserType> userOdo, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		Mapping<V> mapping = mappingFactory.createMapping(mappingType,
				"user template mapping in " + userTemplate
				+ " while processing user " + userOdo.getAnyObject());
		mapping.setSourceContext(userOdo);
		mapping.setTargetContext(getUserDefinition());
		mapping.setRootNode(userOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, userOdo);

		ItemDefinition outputDefinition = mapping.getOutputDefinition();
		PropertyPath itemPath = mapping.getOutputPath();
		
		PrismProperty<?> existingUserProperty = userOdo.getNewObject().findProperty(itemPath);
		if (existingUserProperty != null && !existingUserProperty.isEmpty() && mapping.isInitial()) {
			// This valueConstruction only applies if the property does not have a value yet.
			// ... but it does
			return null;
		}

		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, userOdo);
		// TODO: more variables?
		
		StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
			private PropertyPath outputPath;
			private ItemDefinition outputDefinition;
			@Override
			public void setOutputPath(PropertyPath outputPath) {
				this.outputPath = outputPath;
			}
			
			@Override
			public void setOutputDefinition(ItemDefinition outputDefinition) {
				this.outputDefinition = outputDefinition;
			}
			
			@Override
			public StringPolicyType resolve() {
				if (!outputDefinition.getName().equals(PasswordType.F_VALUE)) {
					return null;
				}
				PasswordPolicyType passwordPolicy = context.getGlobalPasswordPolicy();
				if (passwordPolicy == null) {
					return null;
				}
				return passwordPolicy.getStringPolicy();
			}
		};
		mapping.setStringPolicyResolver(stringPolicyResolver);

		mapping.evaluate(result);

		PrismValueDeltaSetTriple<V> outputTriple = mapping.getOutputTriple();
		ItemDelta<V> itemDelta = null;
		if (outputTriple != null) {
			itemDelta = mapping.createEmptyDelta(itemPath);
			Collection<V> nonNegativeValues = outputTriple.getNonNegativeValues();
			if (outputDefinition.isMultiValue()) {
				for (V value: nonNegativeValues) {
					if (!hasValue(existingUserProperty, value)) {
						itemDelta.addValueToAdd((V) value.clone());
					}
				}
				// TODO: remove values
			} else {
				if (nonNegativeValues.size() > 1) {
					throw new SchemaException("Attempt to store "+nonNegativeValues.size()+" values in single-valued user property "+itemPath);
				}
				if (nonNegativeValues.size() == 0) {
					if (existingUserProperty != null && !existingUserProperty.isEmpty()) {
						// Empty set in replace value will cause the property to remove all existing values.
						itemDelta.setValuesToReplace(nonNegativeValues);
					}
				} else {
					PrismValue value = nonNegativeValues.iterator().next();
					if (!hasValue(existingUserProperty, value)) {
						itemDelta.setValueToReplace((V) value.clone());
					}
				}
			}
		}

		return itemDelta;
	}


	private boolean hasValue(PrismProperty existingUserProperty, PrismValue newValue) {
		if (existingUserProperty == null) {
			return false;
		}
		// TODO: is this OK? Will it not mess with value meta-data?
		return existingUserProperty.contains(newValue);
	}


	private PrismObjectDefinition<UserType> getUserDefinition() {
		return prismContext.getSchemaRegistry().getObjectSchema()
				.findObjectDefinitionByCompileTimeClass(UserType.class);
	}

}
