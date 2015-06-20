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
import com.evolveum.midpoint.web.page.PageBase;
import org.apache.wicket.model.IModel;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 28.9.2012
 * Time: 14:11
 * To change this template use File | Settings | File Templates.
 */
@PageDescriptor(url = "/admin/workItems/myRequests", action = {
        @AuthorizationAction(actionUri = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL,
                label = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL_LABEL,
                description = PageAdminWorkItems.AUTH_WORK_ITEMS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_WORK_ITEMS_MY_REQUESTS_URL,
                label = "PageProcessInstancesRequestedBy.auth.workItemsMyRequests.label",
                description = "PageProcessInstancesRequestedBy.auth.workItemsMyRequests.description")})
public class PageProcessInstancesRequestedBy extends PageProcessInstances {

    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageProcessInstancesRequestedBy.title");
    }

    public PageProcessInstancesRequestedBy() {
        super(true, false);
    }

    @Override
    public PageBase reinitialize() {
        return new PageProcessInstancesRequestedBy();
    }
}
