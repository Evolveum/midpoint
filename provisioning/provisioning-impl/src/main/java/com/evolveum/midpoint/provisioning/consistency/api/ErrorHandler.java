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

package com.evolveum.midpoint.provisioning.consistency.api;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public abstract class ErrorHandler {
	
//	@Autowired(required = true)
//	protected TaskManager taskManager;
	@Autowired(required = true)
	protected ChangeNotificationDispatcher changeNotificationDispatcher;
//	@Autowired(required = true)
//	protected ResourceOperationListener operationListener;
	@Autowired
	protected PrismContext prismContext;
	
	public static enum FailedOperation{
		ADD, DELETE, MODIFY, GET;
	}
	
	protected boolean isPostpone(ResourceType resource){
		if (resource.getConsistency() == null){
			return true;
		}
		
		if (resource.getConsistency().isPostpone() == null){
			return true;
		}
		
		return resource.getConsistency().isPostpone();
	}
	
	public abstract <T extends ShadowType> T handleError(T shadow, FailedOperation op, Exception ex, 
			boolean doDiscovery, boolean compensate, Task task, OperationResult parentResult) 
					throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

	
	protected <T extends ShadowType> Collection<ItemDelta> createAttemptModification(T shadow,
			Collection<ItemDelta> modifications) {

		if (modifications == null) {
			modifications = new ArrayList<>();
		}
		PropertyDelta attemptDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
				ShadowType.F_ATTEMPT_NUMBER, getAttemptNumber(shadow));
		modifications.add(attemptDelta);
		return modifications;
	}

	protected Integer getAttemptNumber(ShadowType shadow) {
		Integer attemptNumber = (shadow.getAttemptNumber() == null ? 0 : shadow.getAttemptNumber()+1);
		return attemptNumber;
	}
	
	protected ResourceOperationDescription createOperationDescription(ShadowType shadowType, Exception ex, ResourceType resource, ObjectDelta delta, Task task, OperationResult result) {
		ResourceOperationDescription operationDescription = new ResourceOperationDescription();
		operationDescription.setCurrentShadow(shadowType.asPrismObject());
		if (resource != null){
			operationDescription.setResource(resource.asPrismObject());
		}
		if (task != null) {
			operationDescription.setSourceChannel(task.getChannel());
		}
		operationDescription.setObjectDelta(delta);

        // fill-in the message if necessary
        OperationResult storedResult = result != null ? result.clone() : new OperationResult("dummy");      // actually, the result shouldn't be null anyway
        storedResult.computeStatusIfUnknown();
        if (storedResult.getMessage() == null && ex != null) {
            storedResult.recordStatus(storedResult.getStatus(), ex.getMessage(), ex);
        }
        operationDescription.setResult(storedResult);

		return operationDescription;
	}

	
}
