/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.api.events.PolicyRuleEvent;
import com.evolveum.midpoint.notifications.api.events.TaskEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleActivationNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleUserNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class SimpleActivationNotifier extends SimplePolicyRuleNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleActivationNotifier.class);

    @PostConstruct
    public void init() {
        register(SimpleActivationNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
       if (!(event instanceof PolicyRuleEvent)) {
    	   return false;
       }
       PolicyRuleEvent modelEvent = (PolicyRuleEvent) event;
       
        return UserType.class.isAssignableFrom(modelEvent.getRequesteeObject().getClass());
    }
    
    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
    	PolicyRuleEvent modelEvent = (PolicyRuleEvent) event;
    	EvaluatedPolicyRule policyRuleType = modelEvent.getPolicyRule();
    	if (policyRuleType == null) {
    		return false;
    	}
    	
    	PolicyConstraintsType policyConstraints = policyRuleType.getPolicyConstraints();
    	if (policyConstraints == null) {
    		return false;
    	}
    	
    	if (policyConstraints.getTimeValidity() == null || policyConstraints.getTimeValidity().isEmpty()) {
    		return false;
    	}
    	
    	return true;
    	
    }
    
    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task,
    		OperationResult result) {
    	
    	
    	return "Planned deactivation of user " + getUserName(event);
    }
    
    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task,
    		OperationResult result) throws SchemaException {
    	
    	return "User " + getUserName(event) + " is going to be deactivated on " + getUser(event).getActivation().getValidTo();
    	
    }
    
    private String getUserName(Event event){
		UserType user = getUser(event);
		PolyStringType username = user.getName();
		return username.getOrig();
	}
    
    private UserType getUser(Event event){
    	PolicyRuleEvent taskEvent = (PolicyRuleEvent) event;
		UserType user = (UserType) taskEvent.getRequesteeObject();
		return user;
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
