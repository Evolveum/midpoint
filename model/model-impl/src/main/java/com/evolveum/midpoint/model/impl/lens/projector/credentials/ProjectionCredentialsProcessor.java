/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.stringpolicy.ObjectValuePolicyEvaluator;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SchemaFailableProcessor;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.Consumer;

import static com.evolveum.midpoint.prism.delta.ChangeType.MODIFY;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType.WEAK;

/**
 * Processor for projection credentials. Which at this moment means just the password.
 *
 * @author Radovan Semancik
 */
@Component
public class ProjectionCredentialsProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(ProjectionCredentialsProcessor.class);

	@Autowired(required=true)
	private PrismContext prismContext;

	@Autowired(required=true)
	private MappingFactory mappingFactory;

	@Autowired(required=true)
	private MappingEvaluator mappingEvaluator;

	@Autowired(required=true)
	private OperationalDataManager metadataManager;
	
	@Autowired(required=true)
	private ModelObjectResolver resolver;
	
	@Autowired(required=true)
	private ValuePolicyProcessor valuePolicyProcessor;
	
	@Autowired(required = true)
	private SecurityHelper securityHelper;
	
	@Autowired(required = true)
	Protector protector;
	
	public <F extends ObjectType> void processProjectionCredentials(LensContext<F> context,
			LensProjectionContext projectionContext, XMLGregorianCalendar now, Task task,
			OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
					SchemaException, PolicyViolationException {
		
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext != null && FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
			
			@SuppressWarnings("unchecked")
			LensContext<? extends FocusType> focusContextFocus = (LensContext<? extends FocusType>) context;
			processProjectionPassword(focusContextFocus, projectionContext, now, task, result);
			
		}
	}

	private <F extends FocusType> void processProjectionPassword(LensContext<F> context,
			final LensProjectionContext projCtx, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		LensFocusContext<F> focusContext = context.getFocusContext();

		PrismObject<F> userNew = focusContext.getObjectNew();
		if (userNew == null) {
			// This must be a user delete or something similar. No point in proceeding
			LOGGER.trace("userNew is null, skipping credentials processing");
			return;
		}

		PrismObjectDefinition<ShadowType> accountDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(ShadowType.class);
		PrismPropertyDefinition<ProtectedStringType> projPasswordPropertyDefinition = accountDefinition
				.findPropertyDefinition(SchemaConstants.PATH_PASSWORD_VALUE);

		ResourceShadowDiscriminator rsd = projCtx.getResourceShadowDiscriminator();

		RefinedObjectClassDefinition refinedProjDef = projCtx.getStructuralObjectClassDefinition();
		if (refinedProjDef == null) {
			LOGGER.trace("No RefinedObjectClassDefinition, therefore also no password outbound definition, skipping credentials processing for projection {}", rsd);
			return;
		}

		MappingType outboundMappingType = refinedProjDef.getPasswordOutbound();
		if (outboundMappingType == null) {
			LOGGER.trace("No outbound definition in password definition in credentials in account type {}, skipping credentials processing", rsd);
			return;
		}

		final ObjectDelta<ShadowType> projDelta = projCtx.getDelta();
		final PropertyDelta<ProtectedStringType> projPasswordDelta;
		if (projDelta != null && projDelta.getChangeType() == MODIFY) {
			projPasswordDelta = projDelta.findPropertyDelta(SchemaConstants.PATH_PASSWORD_VALUE);
		} else {
			projPasswordDelta = null;
		}
		checkExistingDeltaSanity(projCtx, projPasswordDelta);

		if (outboundMappingType.getStrength() == WEAK && projPasswordDelta != null) {
			LOGGER.trace("Outbound password is weak and a priori projection password delta exists; skipping credentials processing for {}", rsd);
			return;
		}

		final ItemDeltaItem<PrismPropertyValue<PasswordType>, PrismPropertyDefinition<ProtectedStringType>> userPasswordIdi = focusContext
				.getObjectDeltaObject().findIdi(SchemaConstants.PATH_PASSWORD_VALUE);

		StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
			@Override
			public void setOutputPath(ItemPath outputPath) {
			}
			@Override
			public void setOutputDefinition(ItemDefinition outputDefinition) {
			}
			@Override
			public StringPolicyType resolve() {
				ValuePolicyType passwordPolicy = determinePasswordPolicy(context, projCtx, now, task, result);
				if (passwordPolicy == null) {
					return null;
				}
				return passwordPolicy.getStringPolicy();
			}
		};

		Mapping<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>> mapping =
				mappingFactory.<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>>createMappingBuilder()
						.mappingType(outboundMappingType)
						.contextDescription("outbound password mapping in account type " + rsd)
						.defaultTargetDefinition(projPasswordPropertyDefinition)
						.defaultSource(new Source<>(userPasswordIdi, ExpressionConstants.VAR_INPUT))
						.sourceContext(focusContext.getObjectDeltaObject())
						.originType(OriginType.OUTBOUND)
						.originObject(projCtx.getResource())
						.stringPolicyResolver(stringPolicyResolver)
						.addVariableDefinitions(Utils.getDefaultExpressionVariables(context, projCtx).getMap())
						.build();

		if (!mapping.isApplicableToChannel(context.getChannel())) {
			return;
		}

		mappingEvaluator.evaluateMapping(mapping, context, task, result);
		final PrismValueDeltaSetTriple<PrismPropertyValue<ProtectedStringType>> outputTriple = mapping.getOutputTriple();

		// TODO review all this code !! MID-3156
		if (outputTriple == null) {
			LOGGER.trace("Credentials 'password' expression resulted in null output triple, skipping credentials processing for {}", rsd);
			return;
		}

		boolean projectionIsNew = projDelta != null && (projDelta.getChangeType() == ChangeType.ADD
				|| projCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.ADD);

		final Collection<PrismPropertyValue<ProtectedStringType>> newValues;
		if (outputTriple.hasPlusSet()) {
			newValues = outputTriple.getPlusSet();
		} else if (projectionIsNew) {
			// when adding new account, synchronize password regardless of whether the source data was changed or not.
			newValues = outputTriple.getNonNegativeValues();
		} else if (outputTriple.hasMinusSet()) {
			// Also, if the password value is to be removed, let's trigger its change (presumably to an empty value); except for WEAK mappings.
			// (We cannot check if the value being removed is the same as the current value. So let's take the risk and do it.)
			if (mapping.getStrength() != WEAK) {
				newValues = outputTriple.getNonNegativeValues();
			} else {
				LOGGER.trace("Credentials 'password' expression resulting in password deletion but the mapping is weak: skipping credentials processing for {}", rsd);
				return;
			}
		} else {
			// no plus set, no minus set
			LOGGER.trace("Credentials 'password' expression resulted in no change, skipping credentials processing for {}", rsd);
			return;
		}
		assert newValues != null;

		ItemDelta<?,?> projPasswordDeltaNew =
				DeltaBuilder.deltaFor(ShadowType.class, prismContext)
						.item(SchemaConstants.PATH_PASSWORD_VALUE).replace(newValues)
						.asItemDelta();
		LOGGER.trace("Adding new password delta for account {}", rsd);
		projCtx.swallowToSecondaryDelta(projPasswordDeltaNew);
	}
	
	private <F extends FocusType> ValuePolicyType determinePasswordPolicy(LensContext<F> context,
			final LensProjectionContext projCtx, XMLGregorianCalendar now, Task task, OperationResult result) {
		ValuePolicyType passwordPolicy = projCtx.getAccountPasswordPolicy();
		if (passwordPolicy != null) {
			return passwordPolicy;
		}
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return null;
		}
		return SecurityUtil.getPasswordPolicy(focusContext.getSecurityPolicy());
	}

	private void checkExistingDeltaSanity(LensProjectionContext projCtx,
			PropertyDelta<ProtectedStringType> passwordDelta) throws SchemaException {
		if (passwordDelta != null && (passwordDelta.isAdd() || passwordDelta.isDelete())) {
			throw new SchemaException("Password for projection " + projCtx.getResourceShadowDiscriminator()
					+ " cannot be added or deleted, it can only be replaced");
		}
	}

}
