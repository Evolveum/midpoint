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
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Processor that takes password from user and synchronizes it to accounts.
 * <p/>
 * The implementation is very simple now. It only cares about password value, not
 * expiration or other password facets. It completely ignores other credential types.
 *
 * @author Radovan Semancik
 */
@Component
public class CredentialsProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(CredentialsProcessor.class);

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private MappingFactory valueConstructionFactory;
    
    @Autowired(required = true)
    private PasswordPolicyProcessor passwordPolicyProcessor;

    public <F extends ObjectType> void processCredentials(LensContext<F> context, LensProjectionContext projectionContext, Task task, OperationResult result)
    		throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException {	
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext != null && FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
    		processCredentialsFocal((LensContext<? extends FocusType>)context, projectionContext, task, result);
//    		return;
    	}
//    	if (focusContext.getObjectTypeClass() != UserType.class) {
//    		// We can do this only for user.
//    		return;
//    	} 	
    	
    	passwordPolicyProcessor.processPasswordPolicy(projectionContext, context, result);
    }
    
    
    public <F extends FocusType> void processCredentialsFocal(LensContext<F> context,
    		final LensProjectionContext accCtx, Task task, OperationResult result)
		throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
        ObjectDelta<F> userDelta = focusContext.getDelta();
        PropertyDelta<PasswordType> userPasswordValueDelta = null;
        if (userDelta != null) {
        	userPasswordValueDelta = userDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
        }

        PrismObject<F> userNew = focusContext.getObjectNew();
        if (userNew == null) {
            // This must be a user delete or something similar. No point in proceeding
            LOGGER.trace("userNew is null, skipping credentials processing");
            return;
        }
        
        PrismObjectDefinition<ShadowType> accountDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        PrismPropertyDefinition accountPasswordPropertyDefinition = accountDefinition.findPropertyDefinition(SchemaConstants.PATH_PASSWORD_VALUE);

        ResourceShadowDiscriminator rat = accCtx.getResourceShadowDiscriminator();

        ObjectDelta<ShadowType> accountDelta = accCtx.getDelta();
        PropertyDelta<ProtectedStringType> accountPasswordValueDelta = null;
        if (accountDelta != null) {
        	accountPasswordValueDelta = accountDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
        }
        if (accountDelta != null && accountDelta.getChangeType() == ChangeType.MODIFY) {
        	if (accountPasswordValueDelta != null && (accountPasswordValueDelta.isAdd() || accountDelta.isDelete())) {
        		throw new SchemaException("Password for account "+rat+" cannot be added or deleted, it can only be replaced");
        	}
        }
        if (accountDelta != null && (accountDelta.getChangeType() == ChangeType.ADD || accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD)) {
            // adding new account, synchronize password regardless whether the password was changed or not.
        } else if (userPasswordValueDelta != null) {
            // user password was changed. synchronize it regardless of the account change.
        } else {
            LOGGER.trace("No change in password and the account is not added, skipping credentials processing for account " + rat);
            return;
        }

        RefinedObjectClassDefinition refinedAccountDef = accCtx.getRefinedAccountDefinition();
        if (refinedAccountDef == null){
        	LOGGER.trace("No RefinedAccountDefinition, therefore also no password outbound definition, skipping credentials processing for account " + rat);
          return;
        }
        
        MappingType outboundMappingType = refinedAccountDef.getCredentialsOutbound();
        
        if (outboundMappingType == null) {
            LOGGER.trace("No outbound definition in password definition in credentials in account type {}, skipping credentials processing", rat);
            return;
        }
        
        Mapping<PrismPropertyValue<ProtectedStringType>> passwordMapping = valueConstructionFactory.createMapping(outboundMappingType, 
        		"outbound password mapping in account type " + rat);
        if (!passwordMapping.isApplicableToChannel(context.getChannel())) {
        	return;
        }
        
        passwordMapping.setDefaultTargetDefinition(accountPasswordPropertyDefinition);
        ItemDeltaItem<PrismPropertyValue<PasswordType>> userPasswordIdi = focusContext.getObjectDeltaObject().findIdi(SchemaConstants.PATH_PASSWORD_VALUE);
        Source<PrismPropertyValue<PasswordType>> source = new Source<PrismPropertyValue<PasswordType>>(userPasswordIdi, ExpressionConstants.VAR_INPUT);
		passwordMapping.setDefaultSource(source);
		passwordMapping.setOriginType(OriginType.OUTBOUND);
		passwordMapping.setOriginObject(accCtx.getResource());
		
		if (passwordMapping.getStrength() != MappingStrengthType.STRONG) {
        	if (accountPasswordValueDelta != null && !accountPasswordValueDelta.isEmpty()) {
        		return;
        	}
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
				ValuePolicyType passwordPolicy = accCtx.getEffectivePasswordPolicy();
				if (passwordPolicy == null) {
					return null;
				}
				return passwordPolicy.getStringPolicy();
			}
		};
		passwordMapping.setStringPolicyResolver(stringPolicyResolver);
		
		LensUtil.evaluateMapping(passwordMapping, context, task, result);
        
        PrismProperty<ProtectedStringType> accountPasswordNew = (PrismProperty) passwordMapping.getOutput();
        if (accountPasswordNew == null || accountPasswordNew.isEmpty()) {
            LOGGER.trace("Credentials 'password' expression resulted in null, skipping credentials processing for {}", rat);
            return;
        }
        PropertyDelta<ProtectedStringType> accountPasswordDeltaNew = new PropertyDelta<ProtectedStringType>(SchemaConstants.PATH_PASSWORD_VALUE, accountPasswordPropertyDefinition, prismContext);
        accountPasswordDeltaNew.setValuesToReplace(accountPasswordNew.getClonedValues());
        LOGGER.trace("Adding new password delta for account {}", rat);
        accCtx.swallowToSecondaryDelta(accountPasswordDeltaNew);

    }


}
