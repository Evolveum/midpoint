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

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.wizard.Wizard;
import com.evolveum.midpoint.web.component.wizard.resource.*;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/resources/wizard", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminResources.AUTH_RESOURCE_ALL,
            label = PageAdminResources.AUTH_RESOURCE_ALL_LABEL,
            description = PageAdminResources.AUTH_RESOURCE_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_EDIT_URL,
            label = "PageResourceWizard.auth.resource.label",
            description = "PageResourceWizard.auth.resource.description")})
public class PageResourceWizard extends PageAdminResources {

    private static final String ID_WIZARD = "wizard";
    private LoadableModel<PrismObject<ResourceType>> model;
    private String editedResourceOid;

    public PageResourceWizard(@NotNull PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);						// to be available in the constructor as well

        editedResourceOid = getResourceOid();

        model = new LoadableModel<PrismObject<ResourceType>>(false) {

            @Override
            protected PrismObject<ResourceType> load() {
                try {
                    if (editedResourceOid == null) {
                        ResourceType resource = new ResourceType();
                        PageResourceWizard.this.getPrismContext().adopt(resource);
                        return resource.asPrismObject();
                    }

					PrismObject<ResourceType> resource = loadResource();
					if (resource == null) {
						throw new RestartResponseException(PageError.class);
					}

					// TODO try raw if some problems

					resource.revive(PageResourceWizard.this.getPrismContext());

                    return resource;
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't load resource", ex);
                    throw new RestartResponseException(PageError.class);
                }
            }
        };

        initLayout();
    }

	public String getEditedResourceOid() {
		return editedResourceOid;
	}

	public void setEditedResourceOid(String editedResourceOid) {
		this.editedResourceOid = editedResourceOid;
	}

	public PrismObject<ResourceType> loadResource() {
		Task task = createSimpleTask("loadResource");
		Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<>();
		options.add(SelectorOptions.create(GetOperationOptions.createNoFetch()));
		options.add(SelectorOptions.create(ResourceType.F_CONNECTOR_REF, GetOperationOptions.createResolve()));
		return WebModelServiceUtils.loadObject(ResourceType.class, editedResourceOid, options, this, task, task.getResult());
	}

	@Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                if (!isResourceOidAvailable()) {
                    return PageResourceWizard.super.createPageTitleModel().getObject();
                }

                String name = WebComponentUtil.getName(model.getObject());
                return createStringResource("PageResourceWizard.title.edit", name).getString();
            }
        };
    }

    private void initLayout() {
        WizardModel wizardModel = new WizardModel();
        wizardModel.add(new NameStep(model, this));
        wizardModel.add(new ConfigurationStep(model, this));
        wizardModel.add(new SchemaStep(model, this));
        wizardModel.add(new SchemaHandlingStep(model, this));
        wizardModel.add(new CapabilityStep(model, this));
        wizardModel.add(new SynchronizationStep(model, this));

        Wizard wizard = new Wizard(ID_WIZARD, new Model(wizardModel));
        wizard.setOutputMarkupId(true);
        add(wizard);
    }

	// questionable
    public boolean isNewResource() {
        return editedResourceOid != null;
    }
}
