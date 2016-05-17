/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationActionType;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class SynchronizationActionTypeDto implements Serializable{

    public enum HandlerUriActions {
        ACTION_LINK("http://midpoint.evolveum.com/xml/ns/public/model/action-3#link"),
        ACTION_UNLINK("http://midpoint.evolveum.com/xml/ns/public/model/action-3#unlink"),
        ACTION_ADD_FOCUS("http://midpoint.evolveum.com/xml/ns/public/model/action-3#addFocus"),
        ACTION_DELETE_FOCUS("http://midpoint.evolveum.com/xml/ns/public/model/action-3#deleteFocus"),
        ACTION_INACTIVATE_FOCUS("http://midpoint.evolveum.com/xml/ns/public/model/action-3#inactivateFocus"),
        ACTION_DELETE_SHADOW("http://midpoint.evolveum.com/xml/ns/public/model/action-3#deleteShadow"),
        ACTION_INACTIVATE_SHADOW("http://midpoint.evolveum.com/xml/ns/public/model/action-3#inactivateShadow"),
        ACTION_SYNCHRONIZE("http://midpoint.evolveum.com/xml/ns/public/model/action-3#synchronize", true),
        ACTION_ADD_USER("http://midpoint.evolveum.com/xml/ns/public/model/action-3#addUser", true),
        ACTION_MODIFY_USER("http://midpoint.evolveum.com/xml/ns/public/model/action-3#modifyUser", true),
        ACTION_DISABLE_USER("http://midpoint.evolveum.com/xml/ns/public/model/action-3#disableUser", true),
        ACTION_DELETE_USER("http://midpoint.evolveum.com/xml/ns/public/model/action-3#deleteUser", true),
        ACTION_LINK_ACCOUNT("http://midpoint.evolveum.com/xml/ns/public/model/action-3#linkAccount", true),
        ACTION_UNLINK_ACCOUNT("http://midpoint.evolveum.com/xml/ns/public/model/action-3#unlinkAccount", true),
        ACTION_DELETE_ACCOUNT("http://midpoint.evolveum.com/xml/ns/public/model/action-3#deleteAccount", true),
        ACTION_DISABLE_ACCOUNT("http://midpoint.evolveum.com/xml/ns/public/model/action-3#disableAccount", true);

        protected String action;
		protected boolean deprecated;

		HandlerUriActions(String action) {
			this(action, false);
		}

        HandlerUriActions(String action, boolean deprecated) {
            this.action = action;
			this.deprecated = deprecated;
        }

        public String getAction() {
            return action;
        }

		public boolean isDeprecated() {			// implemented for future use (currently the deprecation flag is implemented via GUI properties)
			return deprecated;
		}
	}

    public static final String F_ACTION_OBJECT = "actionObject";
    public static final String F_HANDLER_URI = "handlerUri";

    private SynchronizationActionType actionObject;
    private HandlerUriActions handlerUri;

	public SynchronizationActionTypeDto(SynchronizationActionType action) {
		if (action != null) {
			actionObject = action;
		} else {
			actionObject = new SynchronizationActionType();
		}

		HandlerUriActions[] actions = HandlerUriActions.values();
		if (actionObject.getHandlerUri() != null) {
			for (HandlerUriActions uriAction : actions) {
				if (uriAction.getAction().equals(actionObject.getHandlerUri())) {
					handlerUri = uriAction;
					break;
				}
			}
		}
	}

	public SynchronizationActionType prepareDtoToSave() {
		if (actionObject == null) {
			actionObject = new SynchronizationActionType();
		}
		if (handlerUri != null) {
			actionObject.setHandlerUri(handlerUri.action);
		}
		return actionObject;
	}

    public SynchronizationActionType getActionObject() {
        return actionObject;
    }

    public void setActionObject(SynchronizationActionType actionObject) {
        this.actionObject = actionObject;
    }

    public HandlerUriActions getHandlerUri() {
        return handlerUri;
    }

    public void setHandlerUri(HandlerUriActions handlerUri) {
        this.handlerUri = handlerUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SynchronizationActionTypeDto that = (SynchronizationActionTypeDto) o;

        if (actionObject != null ? !actionObject.equals(that.actionObject) : that.actionObject != null) return false;
        if (handlerUri != null ? !handlerUri.equals(that.handlerUri) : that.handlerUri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = actionObject != null ? actionObject.hashCode() : 0;
        result = 31 * result + (handlerUri != null ? handlerUri.hashCode() : 0);
        return result;
    }
}
