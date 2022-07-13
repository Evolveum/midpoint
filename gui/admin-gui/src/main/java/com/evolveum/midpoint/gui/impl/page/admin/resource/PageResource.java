/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.ResourceOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.page.admin.resources.ResourceSummaryPanel;

import org.jetbrains.annotations.Nullable;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/resource")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                        label = "PageAdminResources.auth.resourcesAll.label",
                        description = "PageAdminResources.auth.resourcesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_URL,
                        label = "PageResource.auth.resource.label",
                        description = "PageResource.auth.resource.description")
        })
public class PageResource extends PageAssignmentHolderDetails<ResourceType, ResourceDetailsModel> {

    private static final String ID_WIZARD_VIEW = "wizardView";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_WIZARD = "wizard";

    public PageResource(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageResource(PrismObject<ResourceType> resource) {
        super(resource);
    }

    @Override
    public Class<ResourceType> getType() {
        return ResourceType.class;
    }

    protected boolean isApplicableTemplate() {
        return true;
    }

    protected WebMarkupContainer createTemplatePanel(String id) {
        return new CreateResourceTemplatePanel(id) {

            @Override
            protected void onTemplateChosePerformed(PrismObject<ResourceType> newObject, AjaxRequestTarget target) {
                reloadObjectDetailsModel(newObject);
                Fragment fragment = createWizardFragment();
                fragment.setOutputMarkupId(true);
                PageResource.this.replace(fragment);
                target.add(fragment);
            }
        };
    }

    private Fragment createWizardFragment() {
        return new DetailsFragment(ID_DETAILS_VIEW, ID_WIZARD_VIEW, PageResource.this) {

            @Override
            protected void initFragmentLayout() {
                Form mainForm = new Form(ID_MAIN_FORM);
                add(mainForm);
                WizardPanel wizard = new WizardPanel(ID_WIZARD, new WizardModel(PageResource.this.createSteps()));
                wizard.setOutputMarkupId(true);
                mainForm.add(wizard);
            }
        };
    }

    private List<WizardStep> createSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new BasicSettingStepPanel(getObjectDetailsModels()) {

            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                Fragment fragment = createTemplateFragment();
                fragment.setOutputMarkupId(true);
                PageResource.this.replace(fragment);
                target.add(fragment);

                return false;
            }
        });



        PrismObject<ConnectorType> connector = WebModelServiceUtils.loadObject(getModelObjectType().getConnectorRef(), PageResource.this);

        if (connector != null && SchemaConstants.ICF_FRAMEWORK_URI.equals(connector.asObjectable().getFramework())) {
            CapabilityCollectionType capabilities
                    = WebComponentUtil.getNativeCapabilities(getModelObjectType(), PageResource.this);

            if (capabilities.getDiscoverConfiguration() != null) {
                steps.add(new PartialConfigurationStepPanel(getObjectDetailsModels()));
                steps.add(new DiscoveryStepPanel(getObjectDetailsModels()));
            } else {
                steps.add(new ConfigurationStepPanel(getObjectDetailsModels()));
            }

            if (capabilities.getSchema() != null) {
                steps.add(new SelectObjectClassesStepPanel(getObjectDetailsModels()));
            }
        }

        return steps;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<ResourceType> summaryModel) {
        return new ResourceSummaryPanel(id,
                summaryModel, getSummaryPanelSpecification());
    }

    @Override
    protected ResourceOperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<ResourceType>> wrapperModel) {
        return new ResourceOperationalButtonsPanel(id, wrapperModel) {

            @Override
            protected void refreshStatus(AjaxRequestTarget target) {
                target.add(PageResource.this.get(ID_DETAILS_VIEW));
                PageResource.this.refresh(target);
            }

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                PageResource.this.savePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageResource.this.hasUnsavedChanges(target);
            }
        };
    }

    @Override
    protected ResourceDetailsModel createObjectDetailsModels(PrismObject<ResourceType> object) {
        return new ResourceDetailsModel(createPrismObjectModel(object), this);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getOperationOptions() {
        return getOperationOptionsBuilder()
                .noFetch()
                .item(ResourceType.F_CONNECTOR_REF).resolve()
                .build();
    }
}
