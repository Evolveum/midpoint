/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.ResourceOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.BasicSettingStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ConfigurationStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.DiscoveryStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceTemplateStepPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.page.admin.resources.ResourceSummaryPanel;

import org.jetbrains.annotations.NotNull;

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
    private static final String ID_SELECTION = "selection";
    private static final String ID_BASIC_SETTINGS = "basicSettings";
    private static final String ID_CONFIGURATION = "configuration";

    public PageResource(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageResource(PrismObject<ResourceType> resource) {
        super(resource);
    }

    @Override
    protected void initLayout() {
        if (isAdd()) {
            Fragment fragment;
            if (isApplicableTemplate()) {
                fragment = createTemplateFragment();
            } else {
                fragment = createWizardFragment();
            }
            add(fragment);
        } else {
            super.initLayout();
        }
    }

    @Override
    public Class<ResourceType> getType() {
        return ResourceType.class;
    }

    @Override
    protected Fragment createFragmentAfterChoseTemplate() {
        return createWizardFragment();
    }

    private Fragment createWizardFragment() {
        return new DetailsFragment(ID_DETAILS_VIEW, ID_WIZARD_VIEW, PageResource.this) {

            @Override
            protected void initFragmentLayout() {
                Form mainForm = new Form(ID_MAIN_FORM);
                add(mainForm);
                WizardPanel wizard = new WizardPanel(ID_WIZARD, new WizardModel(PageResource.this.createSteps())){
                    @Override
                    protected @NotNull VisibleEnableBehaviour getVisibilityOfStepsHeader() {
                        return new VisibleEnableBehaviour(() -> getWizardModel().getActiveStepIndex() > 0);
                    }
                };
                wizard.setOutputMarkupId(true);
                mainForm.add(wizard);
            }
        };
    }

    private List<WizardStep> createSteps() {

        ResourceTemplateStepPanel selection = new ResourceTemplateStepPanel(getObjectDetailsModels()) {
            @Override
            public PageBase getPageBase() {
                return PageResource.this;
            }
        };

        BasicSettingStepPanel basicSettings = new BasicSettingStepPanel(getObjectDetailsModels());

        ConfigurationStepPanel configuration = new ConfigurationStepPanel(getObjectDetailsModels());

        DiscoveryStepPanel discover = new DiscoveryStepPanel(getObjectDetailsModels());

        return List.of(selection, basicSettings, configuration, discover);
    }

    private WizardPanel getWizardPanel() {
        return (WizardPanel) get(createComponentPath(ID_DETAILS_VIEW, ID_MAIN_FORM, ID_WIZARD));
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
