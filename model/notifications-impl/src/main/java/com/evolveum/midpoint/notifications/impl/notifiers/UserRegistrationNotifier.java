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

package com.evolveum.midpoint.notifications.impl.notifiers;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RegistrationConfirmationMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserRegistrationNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author mederly
 */
@Component
public class UserRegistrationNotifier extends GeneralNotifier {

	private static final Trace LOGGER = TraceManager.getTrace(UserRegistrationNotifier.class);

	@Autowired
	private MidpointFunctions midpointFunctions;

	@Autowired
	private NotificationFunctionsImpl notificationsUtil;
	
	
	private static String CONFIRMATION_LINK = "/confirm/";

	@PostConstruct
	public void init() {
		register(UserRegistrationNotifierType.class);
	}

	@Override
	protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType,
			OperationResult result) {
		if (!(event instanceof ModelEvent) || !((ModelEvent) event).hasFocusOfType(UserType.class)) {
			LOGGER.trace(
					"UserPasswordNotifier is not applicable for this kind of event, continuing in the handler chain; event class = "
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
			plainTextPassword = midpointFunctions.getPlaintextUserPassword(userType);
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

	private String createConfirmationLink(UserType userType, GeneralNotifierType generalNotifierType, OperationResult result){
		
			
		UserRegistrationNotifierType userRegistrationNotifier = (UserRegistrationNotifierType) generalNotifierType;
		
		RegistrationConfirmationMethodType confirmationMethod = userRegistrationNotifier.getConfirmationMethod();
		
		if (confirmationMethod == null) {
			return null;
		}
	
		switch (confirmationMethod) {
			case LINK:
				SystemConfigurationType systemConfiguration = notificationsUtil.getSystemConfiguration(result);
				if (systemConfiguration == null) {
					LOGGER.trace("No system configuration defined. Skipping link generation.");
					return null;
				}
				String defaultHostname = systemConfiguration.getDefaultHostname();
				StringBuilder confirmLinkBuilder = new StringBuilder(defaultHostname + CONFIRMATION_LINK);
				confirmLinkBuilder.append(SchemaConstants.REGISTRATION_ID+"/").append(userType.getName().getOrig())
				.append("/"+SchemaConstants.REGISTRATION_TOKEN+"/").append(getNonce(userType));
				return confirmLinkBuilder.toString();
			case PIN:
				return getNonce(userType);
			default:
				break;
		}
		
		return null;
		
	}
	
	private UserType getUser(Event event){
		ModelEvent modelEvent = (ModelEvent) event;
        PrismObject<UserType> newUser = modelEvent.getFocusContext().getObjectNew();
        UserType userType = newUser.asObjectable();
        return userType;
	}
	
	private String getNonce(UserType user) {
		if (user.getCredentials() == null) {
			return null;
		}
		
		if (user.getCredentials().getNonce() == null) {
			return null;
		}
		
		if (user.getCredentials().getNonce().getValue() == null) {
			return null;
		}
		
		try {
			return midpointFunctions.getPlaintext(user.getCredentials().getNonce().getValue());
		} catch (EncryptionException e) {
			return null;
		}
	}
	
	@Override
	protected String getBodyFromExpression(Event event, GeneralNotifierType generalNotifierType,
			ExpressionVariables variables, Task task, OperationResult result) {
		UserType userType = getUser(event);
		
		String body = super.getBodyFromExpression(event, generalNotifierType, variables, task, result);
		if (body  != null ) {
			return body + "\n" + createConfirmationLink(userType, generalNotifierType, result);
		}
		
		return body;
	}
	
	@Override
	protected Trace getLogger() {
		return LOGGER;
	}

}
