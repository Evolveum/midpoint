/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import java.util.Collection;
import java.util.Locale;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.ResourceOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPreviewPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
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

    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
    private static final String ID_WIZARD = "wizard";

    private static final String ID_WIZARD_PREVIEW_FRAGMENT = "wizardPreviewFragment";
    private static final String ID_PREVIEW = "preview";

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
        return new BasicResourceWizardPanel(id, getObjectDetailsModels()) {

            @Override
            protected void onFinishWizardPerformed(AjaxRequestTarget target) {
                ((PageResource) getPageBase()).savePerformed(target);
            }
        };
    }

    @Override
    protected void postProcessResult(OperationResult result, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, AjaxRequestTarget target) {
        if (isEditObject()) {
            super.postProcessResult(result, executedDeltas, target);
            return;
        }
        if (!result.isError()) {
            if (executedDeltas != null) {
                String resourceOid = ObjectDeltaOperation.findFocusDeltaOidInCollection(executedDeltas);
                if (resourceOid != null) {
                    Task task = createSimpleTask("load resource after save");
                    @Nullable PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(
                            ResourceType.class,
                            resourceOid,
                            PageResource.this,
                            task,
                            task.getResult());
                    if (resource != null) {
                        getObjectDetailsModels().reset();
                        getObjectDetailsModels().reloadPrismObjectModel(resource);
                    }
                }
            }

            result.computeStatusIfUnknown();
            new Toast()
                    .success()
                    .title(getString("PageResource.createResource"))
                    .icon("fas fa-circle-check")
                    .autohide(true)
                    .delay(10_000)
                    .body(getString("PageResource.createResourceText")).show(target);

            Fragment fragment = createWizardPreviewFragment();
            PageResource.this.replace(fragment);
            fragment.setOutputMarkupId(true);
            target.add(fragment);
        } else {
            target.add(getFeedbackPanel());
        }
    }

    private String createMessage(OperationResult result) {
        if (result.getUserFriendlyMessage() != null) {
            LocalizationService service = getLocalizationService();
            Locale locale = getSession().getLocale();

            return service.translate(result.getUserFriendlyMessage(), locale);
        }

        return result.getMessage();
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

    private Fragment createWizardPreviewFragment() {
        return new DetailsFragment(ID_DETAILS_VIEW, ID_WIZARD_PREVIEW_FRAGMENT, PageResource.this) {

            @Override
            protected void initFragmentLayout() {
                ResourceWizardPreviewPanel preview = new ResourceWizardPreviewPanel(ID_PREVIEW, getObjectDetailsModels());
                preview.setOutputMarkupId(true);
                add(preview);
            }
        };
    }
}
