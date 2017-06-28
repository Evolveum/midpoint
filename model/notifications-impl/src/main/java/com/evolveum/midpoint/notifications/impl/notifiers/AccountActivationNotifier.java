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

package com.evolveum.midpoint.notifications.impl.notifiers;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccountActivationNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@Component
public class AccountActivationNotifier extends ConfirmationNotifier {
	
	private static final Trace LOGGER = TraceManager.getTrace(AccountActivationNotifier.class);
	
	@Override
	public void init() {
		register(AccountActivationNotifierType.class);
	}
	
	@Override
	protected Trace getLogger() {
		return LOGGER;
	}

	@Override
	protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType,
			OperationResult result) {
		if (!event.isSuccess()) {
			logNotApplicable(event, "operation was not successful");
			return false;
		}

		ModelEvent modelEvent = (ModelEvent) event;
		if (modelEvent.getFocusDeltas().isEmpty()) {
			logNotApplicable(event, "no user deltas in event");
			return false;
		}
		
		List<ShadowType> shadows = getShadowsToActivate(modelEvent);
		
		if (shadows.isEmpty()) { 
			logNotApplicable(event, "no shadows to activate found in model context");
			return false;
		} 
		
		LOGGER.trace("Found shadows to activate: {}. Processing notifications.", shadows);
		return true;
	}
	
	
	@Override
	protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport,
			Task task, OperationResult result) {
		return "Activate your accounts";
	}

	@Override
	protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport,
			Task task, OperationResult result) throws SchemaException {
		
		String message = "Your accounts was successully created. To activate your accounts, please click on the link bellow.";
		
		String accountsToActivate = "Shadow to be activated: \n";
		for (ShadowType shadow : getShadowsToActivate((ModelEvent) event)) {
			accountsToActivate = accountsToActivate + shadow.asPrismObject().debugDump() + "\n";
		}
		
		String body = message + "\n\n" + createConfirmationLink(getUser(event), generalNotifierType, result) + "\n\n" + accountsToActivate;
		
		return body;
	}
	
	private List<ShadowType> getShadowsToActivate(ModelEvent modelEvent) {
		Collection<ModelElementContext> projectionContexts = modelEvent.getProjectionContexts();
		return getMidpointFunctions().getShadowsToActivate(projectionContexts);
	}
	
	@Override
	public String getConfirmationLink(UserType userType) {
		return getMidpointFunctions().createAccountActivationLink(userType);
	}
}
