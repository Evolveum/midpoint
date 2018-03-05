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

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;

import org.apache.wicket.model.IModel;

/**
 * @author mederly
 */
@PageDescriptor(url = "/admin/requestsAboutMe", action = {
        @AuthorizationAction(actionUri = PageAdminWorkItems.AUTH_APPROVALS_ALL,
                label = PageAdminWorkItems.AUTH_APPROVALS_ALL_LABEL,
                description = PageAdminWorkItems.AUTH_APPROVALS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REQUESTS_ABOUT_ME_URL,
                label = "PageProcessInstancesRequestedFor.auth.requestsAboutMe.label",
                description = "PageProcessInstancesRequestedFor.auth.requestsAboutMe.description")})
public class PageProcessInstancesRequestedFor extends PageProcessInstances {

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageProcessInstancesRequestedFor.title");
    }

    public PageProcessInstancesRequestedFor() {
        super(false, true);
    }

}
