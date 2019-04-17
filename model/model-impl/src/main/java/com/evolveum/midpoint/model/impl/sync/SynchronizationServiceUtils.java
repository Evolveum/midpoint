/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.model.impl.sync;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class SynchronizationServiceUtils {

	private static final Trace LOGGER = TraceManager.getTrace(SynchronizationServiceUtils.class);
	
	
	public static <F extends FocusType> boolean isPolicyApplicable(ObjectSynchronizationType synchronizationPolicy, ObjectSynchronizationDiscriminatorType discriminator, ExpressionFactory expressionFactory, SynchronizationContext<F> syncCtx) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		boolean isApplicablePolicy = false;
		if (discriminator != null) {
			isApplicablePolicy = isPolicyApplicable(discriminator, synchronizationPolicy, syncCtx.getResource());
		} else {
			isApplicablePolicy = isPolicyApplicable(synchronizationPolicy, syncCtx);
		}
		
		if (isApplicablePolicy) {
			Boolean conditionResult = evaluateSynchronizationPolicyCondition(synchronizationPolicy, syncCtx, expressionFactory);
			return conditionResult != null ? conditionResult : true;
		}
		
		return isApplicablePolicy;
		
	}
	
	private static <F extends FocusType> boolean isPolicyApplicable(ObjectSynchronizationType synchronizationPolicy, SynchronizationContext<F> syncCtx)
					throws SchemaException {
		ShadowType currentShadowType = syncCtx.getApplicableShadow().asObjectable();

		// objectClass
		QName shadowObjectClass = currentShadowType.getObjectClass();
		Validate.notNull(shadowObjectClass, "No objectClass in currentShadow");
		
		return SynchronizationUtils.isPolicyApplicable(shadowObjectClass, currentShadowType.getKind(), currentShadowType.getIntent(), synchronizationPolicy, syncCtx.getResource());
		
	}
	
	private static boolean isPolicyApplicable(ObjectSynchronizationDiscriminatorType synchronizationDiscriminator,
			ObjectSynchronizationType synchronizationPolicy, PrismObject<ResourceType> resource)
					throws SchemaException {
		ShadowKindType kind = synchronizationDiscriminator.getKind();
		String intent = synchronizationDiscriminator.getIntent();
		if (kind == null && intent == null) {
			throw new SchemaException(
					"Illegal state, object synchronization discriminator type must have kind/intent specified. Current values are: kind="
							+ kind + ", intent=" + intent);
		}
		return SynchronizationUtils.isPolicyApplicable(null, kind, intent, synchronizationPolicy, resource);
		
	}

	private static <F extends FocusType> Boolean evaluateSynchronizationPolicyCondition(ObjectSynchronizationType synchronizationPolicy,
			SynchronizationContext<F> syncCtx, ExpressionFactory expressionFactory)
					throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (synchronizationPolicy.getCondition() == null) {
			return null;
		}
		ExpressionType conditionExpressionType = synchronizationPolicy.getCondition();
		String desc = "condition in object synchronization " + synchronizationPolicy.getName();
		ExpressionVariables variables = ModelImplUtils.getDefaultExpressionVariables(null, syncCtx.getApplicableShadow(), null,
				syncCtx.getResource(), syncCtx.getSystemConfiguration(), null, syncCtx.getPrismContext());
		try {
			ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(syncCtx.getTask(), syncCtx.getResult()));
			PrismPropertyValue<Boolean> evaluateCondition = ExpressionUtil.evaluateCondition(variables,
					conditionExpressionType, syncCtx.getExpressionProfile(), expressionFactory, desc, syncCtx.getTask(), syncCtx.getResult());
			return evaluateCondition.getValue();
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
	}
}
