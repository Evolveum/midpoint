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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConfirmationNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RegistrationConfirmationMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author katkav
 */
@Component
public class ConfirmationNotifier extends GeneralNotifier {

	private static final Trace LOGGER = TraceManager.getTrace(ConfirmationNotifier.class);

	@Autowired
	private MidpointFunctions midpointFunctions;

	@Autowired
	private NotificationFunctionsImpl notificationsUtil;


	@PostConstruct
	public void init() {
		register(ConfirmationNotifierType.class);
	}

	@Override
	protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType,
			OperationResult result) {
		if (!(event instanceof ModelEvent)) {
			logNotApplicable(event, "wrong event type");
			return false;
		} else {
			return true;
		}
	}

	public String getConfirmationLink(UserType userType) {
		throw new UnsupportedOperationException("Please implement in concrete notifier");
	}

	protected String createConfirmationLink(UserType userType, GeneralNotifierType generalNotifierType, OperationResult result) {


		ConfirmationNotifierType userRegistrationNotifier = (ConfirmationNotifierType) generalNotifierType;

		RegistrationConfirmationMethodType confirmationMethod = userRegistrationNotifier.getConfirmationMethod();

		if (confirmationMethod == null) {
			return null;
		}
		ExpressionEnvironment expressionEnv = new ExpressionEnvironment();
		expressionEnv.setCurrentResult(result);
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(expressionEnv);

		try {

			switch (confirmationMethod) {
				case LINK:
					String confirmationLink = getConfirmationLink(userType);
					return confirmationLink;
				case PIN:
					throw new UnsupportedOperationException("PIN confirmation not supported yes");
	//				return getNonce(userType);
				default:
					break;
			}

		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}

		return null;

	}

	protected UserType getUser(Event event){
		ModelEvent modelEvent = (ModelEvent) event;
		//noinspection unchecked
		PrismObject<UserType> newUser = (PrismObject<UserType>) modelEvent.getFocusContext().getObjectNew();
		return newUser.asObjectable();
	}


	@Override
	protected Trace getLogger() {
		return LOGGER;
	}

	public MidpointFunctions getMidpointFunctions() {
		return midpointFunctions;
	}
}
