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

import com.evolveum.midpoint.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
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
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The processor that takes care of user activation mapping to an account (outbound direction).
 * 
 * @author Radovan Semancik
 */
@Component
public class ActivationProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ActivationProcessor.class);

    @Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private MappingFactory valueConstructionFactory;

    public <F extends ObjectType, P extends ObjectType> void processActivation(LensContext<F,P> context, LensProjectionContext<P> projectionContext, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
    	LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
    	processActivationUser((LensContext<UserType,ShadowType>) context, (LensProjectionContext<ShadowType>)projectionContext, result);
    }

    public void processActivationUser(LensContext<UserType,ShadowType> context, LensProjectionContext<ShadowType> accCtx, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        ObjectDelta<UserType> userDelta = context.getFocusContext().getDelta();
        PropertyDelta<Boolean> userEnabledValueDelta = null;
        if (userDelta != null) {
        	userEnabledValueDelta = userDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        }

        PrismObject<UserType> userNew = context.getFocusContext().getObjectNew();
        if (userNew == null) {
            // This must be a user delete or something similar. No point in proceeding
            LOGGER.trace("userNew is null, skipping activation processing");
            return;
        }

        PrismObjectDefinition<ShadowType> accountDefinition = 
        	prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        PrismPropertyDefinition accountEnabledPropertyDefinition = 
        	accountDefinition.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);

        ResourceShadowDiscriminator rat = accCtx.getResourceShadowDiscriminator();
        
        SynchronizationPolicyDecision policyDecision = accCtx.getSynchronizationPolicyDecision();
        if (policyDecision != null && (policyDecision == SynchronizationPolicyDecision.DELETE || policyDecision == SynchronizationPolicyDecision.UNLINK)) {
            LOGGER.trace("Activation processing skipped for " + rat + ", account is being deleted or unlinked");
            return;
        }

        ObjectDelta<ShadowType> accountDelta = accCtx.getDelta();
        PropertyDelta<Boolean> accountEnabledValueDelta = null;
        if (accountDelta != null) {
        	accountEnabledValueDelta = accountDelta.findPropertyDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        }
        if (accCtx.isAdd()) {
            // adding new account, synchronize activation regardless whether the user activation was changed or not.
        } else if (userEnabledValueDelta != null) {
            // user activation was changed. synchronize it regardless of the account change.
        } else {
            LOGGER.trace("No change in activation and the account is not added, skipping activation processing for account " + rat);
            return;
        }

        ResourceObjectTypeDefinitionType resourceAccountDefType = accCtx.getResourceAccountTypeDefinitionType();
        if (resourceAccountDefType == null) {
            LOGGER.trace("No ResourceAccountTypeDefinition, therefore also no activation outbound definition, skipping activation processing for account " + rat);
            return;
        }
        ResourceActivationDefinitionType activationType = resourceAccountDefType.getActivation();
        if (activationType == null) {
            LOGGER.trace("No activation definition in account type {}, skipping activation processing", rat);
            return;
        }
        ResourceActivationEnableDefinitionType enabledType = activationType.getEnabled();
        if (enabledType == null) {
            LOGGER.trace("No 'enabled' definition in activation in account type {}, skipping activation processing", rat);
            return;
        }
        MappingType outbound = enabledType.getOutbound();
        if (outbound == null) {
            LOGGER.trace("No outbound definition in 'enabled' definition in activation in account type {}, skipping activation processing", rat);
            return;
        }
        
        Mapping<PrismPropertyValue<Boolean>> enabledMapping =
        	valueConstructionFactory.createMapping(outbound, 
        		"outbound activation mapping in account type " + rat);

        if (!enabledMapping.isApplicableToChannel(context.getChannel())) {
        	return;
        }
        
        enabledMapping.setDefaultTargetDefinition(accountEnabledPropertyDefinition);
        ItemDeltaItem<PrismPropertyValue<Boolean>> sourceIdi = context.getFocusContext().getObjectDeltaObject().findIdi(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        Source<PrismPropertyValue<Boolean>> source = new Source<PrismPropertyValue<Boolean>>(sourceIdi, ExpressionConstants.VAR_INPUT);
		enabledMapping.setDefaultSource(source);
		
		if (enabledMapping.getStrength() != MappingStrengthType.STRONG) {
        	if (accountEnabledValueDelta != null && !accountEnabledValueDelta.isEmpty()) {
        		return;
        	}
        }
		
        enabledMapping.setOriginType(OriginType.OUTBOUND);
        enabledMapping.setOriginObject(accCtx.getResource());
        enabledMapping.evaluate(result);
        PrismProperty<Boolean> accountEnabledNew = (PrismProperty<Boolean>) enabledMapping.getOutput();
        if (accountEnabledNew == null || accountEnabledNew.isEmpty()) {
            LOGGER.trace("Activation 'enable' expression resulted in null or empty value, skipping activation processing for {}", rat);
            return;
        }
        PropertyDelta accountEnabledDelta = PropertyDelta.createDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ShadowType.class, prismContext);
        accountEnabledDelta.setValuesToReplace(PrismValue.cloneCollection(accountEnabledNew.getValues()));
        LOGGER.trace("Adding new 'enabled' delta for account {}: {}", rat, accountEnabledNew.getValues());
        accCtx.addToSecondaryDelta(accountEnabledDelta);

    }

}
