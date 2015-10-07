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
package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.home.PageAdminHome;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Viliam Repan (lazyman)
 * @author Radovan Semancik
 */
@PageDescriptor(url = {"/self/profile"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_PROFILE_URL,
                label = "PageSelfProfile.auth.profile.label",
                description = "PageSelfProfile.auth.profile.description")})
public class PageSelfProfile extends PageUser {
	
	private static final Trace LOGGER = TraceManager.getTrace(PageSelfProfile.class);

	@Override
	protected String getFocusOidParameter() {
		return WebModelUtils.getLoggedInUserOid();
	}
	
	@Override
	protected void setSpecificResponsePage() {
		setResponsePage(PageSelfProfile.class);
	}
	
	@Override
	protected ExecuteChangeOptionsPanel initOptions(final Form mainForm) {
		ExecuteChangeOptionsPanel optionsPanel = super.initOptions(mainForm);
		optionsPanel.setVisible(false);
		return optionsPanel;
	}
	
}
