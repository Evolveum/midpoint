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
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
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
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

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

	// these models should be reset after each 'save' operation, in order to fetch current data (on demand)
	// each step should use corresponding model
	// these models have always non-null content
	@NotNull private final NonEmptyLoadableModel<PrismObject<ResourceType>> modelRaw;				// contains resolved connector as well
	@NotNull private final NonEmptyLoadableModel<PrismObject<ResourceType>> modelNoFetch;			// contains resolved connector as well
    @NotNull private final NonEmptyLoadableModel<PrismObject<ResourceType>> modelFull;

	// additional models that have to be reset after each 'save' operation
	@NotNull private final Collection<LoadableModel<?>> dependentModels = new HashSet<>();

	// for new resources: should be set after first save; for others: should be set on page creation
    private String editedResourceOid;

    public PageResourceWizard(@NotNull PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);						// to be available in the constructor as well

        editedResourceOid = getResourceOid();								// might be null at this moment

        modelRaw = createResourceModel(Arrays.asList(
				SelectorOptions.create(ResourceType.F_CONNECTOR_REF, GetOperationOptions.createResolve()),
				SelectorOptions.create(GetOperationOptions.createRaw())));
		modelNoFetch = createResourceModel(Arrays.asList(
				SelectorOptions.create(ResourceType.F_CONNECTOR_REF, GetOperationOptions.createResolve()),
				SelectorOptions.create(GetOperationOptions.createNoFetch())));
		modelFull = createResourceModel(null);

        initLayout();
    }

	@NotNull
	private NonEmptyLoadableModel<PrismObject<ResourceType>> createResourceModel(final Collection<SelectorOptions<GetOperationOptions>> options) {
		return new NonEmptyLoadableModel<PrismObject<ResourceType>>(false) {
			@Override
			protected PrismObject<ResourceType> load() {
				try {
					if (editedResourceOid == null) {
						return getPrismContext().createObject(ResourceType.class);
					}
					PrismObject<ResourceType> resource = loadResourceModelObject(options);
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
	}

	// named differently from "loadResource", as this is used in the superclass
	private PrismObject<ResourceType> loadResourceModelObject(Collection<SelectorOptions<GetOperationOptions>> options) {
		Task task = createSimpleTask("loadResource");
		return WebModelServiceUtils.loadObject(ResourceType.class, editedResourceOid, options, this, task, task.getResult());
	}


	public String getEditedResourceOid() {
		return editedResourceOid;
	}

	public void setEditedResourceOid(String editedResourceOid) {
		this.editedResourceOid = editedResourceOid;
	}

	@Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                if (editedResourceOid == null) {
                    return PageResourceWizard.super.createPageTitleModel().getObject();
                }
                String name = WebComponentUtil.getName(modelRaw.getObject());
                return createStringResource("PageResourceWizard.title.edit", name).getString();
            }
        };
    }

    private void initLayout() {
        WizardModel wizardModel = new ResourceWizardModel();
        wizardModel.add(new NameStep(modelRaw, this));
        wizardModel.add(new ConfigurationStep(modelNoFetch, this));
        wizardModel.add(new SchemaStep(modelFull, this));
        wizardModel.add(new SchemaHandlingStep(modelFull, this));
        wizardModel.add(new CapabilityStep(modelFull, this));
        wizardModel.add(new SynchronizationStep(modelFull, this));

        Wizard wizard = new Wizard(ID_WIZARD, new Model<IWizardModel>(wizardModel));
        wizard.setOutputMarkupId(true);
        add(wizard);
    }

	public void resetModels() {
		modelRaw.reset();
		modelNoFetch.reset();
		modelFull.reset();
		for (LoadableModel<?> model : dependentModels) {
			model.reset();
		}
	}

	public void registerDependentModel(@NotNull LoadableModel<?> model) {
		dependentModels.add(model);
	}

	// questionable
    public boolean isNewResource() {
        return editedResourceOid != null;
    }
}
