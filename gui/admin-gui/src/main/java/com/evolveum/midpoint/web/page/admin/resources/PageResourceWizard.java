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

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.wizard.Wizard;
import com.evolveum.midpoint.web.component.wizard.resource.*;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
//@PageDescriptor(url = "/admin/resources/wizard", encoder = OnePageParameterEncoder.class, action = {
//        PageAdminResources.AUTHORIZATION_RESOURCE_ALL,
//        AuthorizationConstants.NS_AUTHORIZATION + "#resourceWizard"})
@PageDescriptor(url = "/admin/resources/wizard",
        action = {@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_DEVEL_URL)})
public class PageResourceWizard extends PageAdminResources {

    private static final String ID_WIZARD = "wizard";
    private IModel<PrismObject<ResourceType>> model;

    public PageResourceWizard() {
        model = new LoadableModel<PrismObject<ResourceType>>(false) {

            @Override
            protected PrismObject<ResourceType> load() {
                try {
                    if (!isResourceOidAvailable()) {
                        ResourceType resource = new ResourceType();
                        PageResourceWizard.this.getPrismContext().adopt(resource);

                        return resource.asPrismObject();
                    }

                    PrismObject<ResourceType> resource = WebModelUtils.loadObject(ResourceType.class, getResourceOid(),
                            null, PageResourceWizard.this);
                    if (resource == null) {
                        throw new RestartResponseException(PageError.class);
                    }

                    return resource;
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't load resource", ex);
                    throw new RestartResponseException(PageError.class);
                }
            }
        };

        initLayout();
    }

    @Override
    protected IModel<String> createPageSubTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                if (!isResourceOidAvailable()) {
                    return null;
                }

                return WebMiscUtil.getName(model.getObject());
            }
        };
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                if (!isResourceOidAvailable()) {
                    return PageResourceWizard.super.createPageTitleModel().getObject();
                }

                return new StringResourceModel("page.title.editResource", PageResourceWizard.this, null).getString();
            }
        };
    }

    private void initLayout() {
        WizardModel wizardModel = new WizardModel();
        wizardModel.add(new NameStep(model));
        wizardModel.add(new ConfigurationStep(model));
        wizardModel.add(new SchemaStep(model));
        wizardModel.add(new SchemaHandlingStep(model));
        wizardModel.add(new CapabilityStep(model));
        wizardModel.add(new SynchronizationStep(model));

        Wizard wizard = new Wizard(ID_WIZARD, new Model(wizardModel));
        add(wizard);
    }
}
