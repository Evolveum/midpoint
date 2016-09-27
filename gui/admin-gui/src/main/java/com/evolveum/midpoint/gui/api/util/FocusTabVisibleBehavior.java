/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class FocusTabVisibleBehavior<T extends ObjectType> extends VisibleEnableBehaviour {

    private IModel<PrismObject<T>> objectModel;
    private String uiAuthorizationUrl;

    public FocusTabVisibleBehavior(IModel<PrismObject<T>> objectModel, String uiAuthorizationUrl) {
        this.objectModel = objectModel;
        this.uiAuthorizationUrl = uiAuthorizationUrl;
    }

    private SecurityEnforcer getEnforcer() {
        return ((MidPointApplication) MidPointApplication.get()).getSecurityEnforcer();
    }

    @Override
    public boolean isVisible() {
        if (1 == 1) {
            return true;
        }

        //todo implement proper authorization

        PrismObject obj = objectModel.getObject();

        try {
//            ObjectTypes type = ObjectTypes.getObjectType(obj.getCompileTimeClass());
//            boolean allowAll = false;
//            switch (type) {
//                case USER:
//                    allowAll = securityEnforcer.isAuthorized(authorization, AuthorizationPhaseType.REQUEST, obj, null,
//                            null, null);
//                    break;
//                case ROLE:
//
//                    break;
//                case ORG:
//
//                    break;
//                case SERVICE:
//
//                    break;
//                default:
//            }

            boolean objectCreateBare = getEnforcer().isAuthorized(AuthorizationConstants.AUTZ_UI_OBJECT_CREATE_BARE_URL,
                    AuthorizationPhaseType.REQUEST, obj, null, null, null);
            boolean objectDetailsBare = getEnforcer().isAuthorized(AuthorizationConstants.AUTZ_UI_OBJECT_DETAILS_BARE_URL,
                    AuthorizationPhaseType.REQUEST, obj, null, null, null);

            boolean tabEnabled = getEnforcer().isAuthorized(uiAuthorizationUrl,
                    AuthorizationPhaseType.REQUEST, obj, null, null, null);

            return tabEnabled;
        } catch (SchemaException ex) {
            throw new SystemException(ex);
        }
    }
}
