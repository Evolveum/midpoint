/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources.content;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.resources.PageAdminResources;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
public class PageContentEntitlements extends PageAdminResources {

    private IModel<PrismObject<ResourceType>> resourceModel;

    public static final String PARAM_RESOURCE_ID = "entResourceOid";

    public PageContentEntitlements() {
        resourceModel = new LoadableModel<PrismObject<ResourceType>>(false) {

            @Override
            protected PrismObject<ResourceType> load() {
                if (!isResourceOidAvailable()) {
                    getSession().error(getString("pageContentAccounts.message.resourceOidNotDefined"));
                    throw new RestartResponseException(PageResources.class);
                }
                return loadResource(null);
            }
        };
    }

    //todo


    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                String name = WebMiscUtil.getName(resourceModel.getObject());
                return new StringResourceModel("page.title", PageContentEntitlements.this, null, null, name).getString();
            }
        };
    }
}
