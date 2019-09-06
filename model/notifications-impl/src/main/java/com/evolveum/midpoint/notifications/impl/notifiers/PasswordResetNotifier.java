/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordResetNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@Component
public class PasswordResetNotifier extends ConfirmationNotifier {

	private static final Trace LOGGER = TraceManager.getTrace(ConfirmationNotifier.class);

	@Override
	public void init() {
		register(PasswordResetNotifierType.class);
	}

	@Override
	protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType,
			OperationResult result) {
		if (!(super.quickCheckApplicability(event, generalNotifierType, result)) || !((ModelEvent) event).hasFocusOfType(UserType.class)) {
			LOGGER.trace(
					"PasswordResetNotifier is not applicable for this kind of event, continuing in the handler chain; event class = "
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
		if (SchemaConstants.CHANNEL_GUI_RESET_PASSWORD_URI.equals(modelEvent.getChannel())) {
			LOGGER.trace("Found change from reset password channel.");
			return true;
		} else {
			LOGGER.trace("No password reset present in delta. Skip sending notifications.");
			return false;
		}
	}

	@Override
	protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport,
			Task task, OperationResult result) {
		return "Password reset";
	}

	@Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {

		UserType userType = getUser(event);

      return "Did you request password reset? If yes, click on the link bellow \n\n" + createConfirmationLink(userType, generalNotifierType, result);

    }

	@Override
	public String getConfirmationLink(UserType userType) {
		return getMidpointFunctions().createPasswordResetLink(userType);
	}
}
