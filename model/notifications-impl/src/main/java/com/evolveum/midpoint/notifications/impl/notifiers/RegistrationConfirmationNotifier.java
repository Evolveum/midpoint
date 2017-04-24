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

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RegistrationConfirmationNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@Component
public class RegistrationConfirmationNotifier extends ConfirmationNotifier {

	private static final Trace LOGGER = TraceManager.getTrace(ConfirmationNotifier.class);
	
	@Override
	public void init() {
		register(RegistrationConfirmationNotifierType.class);
	}
	
	@Override
	protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType,
			OperationResult result) {
		if (!(super.checkApplicability(event, generalNotifierType, result)) || !((ModelEvent) event).hasFocusOfType(UserType.class)) {
			LOGGER.trace(
					"RegistrationConfirmationNotifier is not applicable for this kind of event, continuing in the handler chain; event class = "
							+ event.getClass());
			return false;
		} else {
			return true;
		}
	}

	@Override
	protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType,
			OperationResult result) {
		if (!event.isSuccess()) {
			LOGGER.trace("Operation was not successful, exiting.");
			return false;
		}

		ModelEvent modelEvent = (ModelEvent) event;
		if (modelEvent.getFocusDeltas().isEmpty()) {
			LOGGER.trace("No user deltas in event, exiting.");
			return false;
		}
		if (SchemaConstants.CHANNEL_GUI_SELF_REGISTRATION_URI.equals(modelEvent.getChannel())) {
			LOGGER.trace("Found change from registration channel.");
			return true;
		} else {
			LOGGER.trace("No registration present in delta. Skip sending notifications.");
			return false;
		}
	}
	
	@Override
	protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport,
			Task task, OperationResult result) {
		return "Registration confirmation";
	}
	
	@Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {

      UserType userType = getUser(event);
        
		String plainTextPassword = "IhopeYouRememberYourPassword";
		try {
			plainTextPassword = getMidpointFunctions().getPlaintextUserPassword(userType);
		} catch (EncryptionException e) {
			//ignore...????
		}
		
        StringBuilder messageBuilder = new StringBuilder("Dear ");
        messageBuilder.append(userType.getGivenName()).append(",\n")
        .append("your account was successfully created. To activate your account click on the following confiramtion link. ")
        .append("\n")
        .append(createConfirmationLink(userType, generalNotifierType, result))
        .append("\n\n")
        .append("After your account is activated, use following credentials to log in: \n")
        .append("username: ")
        .append(userType.getName().getOrig())
        .append("password: ")
        .append(plainTextPassword);
        
        return messageBuilder.toString();
    }
	
	@Override
	public String getConfirmationLink(UserType userType) {
		return SchemaConstants.REGISTRATION_CONFIRAMTION_PREFIX + "?" + SchemaConstants.USER_ID + "=" + userType.getName().getOrig() + "&" + SchemaConstants.TOKEN + "=" + getNonce(userType);
		
	}
}
