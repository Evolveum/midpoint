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

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author mederly
 */
public interface SynchronizationService extends ResourceObjectChangeListener {
	ObjectSynchronizationType determineSynchronizationPolicy(ResourceType resourceType,
			PrismObject<? extends ShadowType> currentShadow, PrismObject<SystemConfigurationType> configuration, Task task, OperationResult result) throws
			SchemaException, ObjectNotFoundException, ExpressionEvaluationException;

	<F extends FocusType> boolean matchUserCorrelationRule(PrismObject<ShadowType> shadow, PrismObject<F> focus,
			ResourceType resourceType, PrismObject<SystemConfigurationType> configuration, Task task, OperationResult result) throws
			ConfigurationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException;
}
