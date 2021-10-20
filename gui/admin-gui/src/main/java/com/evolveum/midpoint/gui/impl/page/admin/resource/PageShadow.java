/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.AjaxTabbedPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.resources.ShadowDetailsTabPanel;
import com.evolveum.midpoint.web.page.admin.resources.ShadowSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
//TODO unify with PageAdminObjectDetails!
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/resources/shadow")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                label = "PageAdminResources.auth.resourcesAll.label",
                description = "PageAdminResources.auth.resourcesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_ACCOUNT_URL,
                label = "PageAccount.auth.resourcesAccount.label",
                description = "PageAccount.auth.resourcesAccount.description")})
public class PageShadow extends AbstractPageObjectDetails<ShadowType, ShadowDetailsModel> {

    public PageShadow(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getOperationOptions() {
        return getOperationOptionsBuilder()
                .item(ShadowType.F_RESOURCE_REF).resolve()
                .build();
    }

    @Override
    protected Class<ShadowType> getType() {
        return ShadowType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, LoadableModel<ShadowType> summaryModel) {
        return new ShadowSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }

    @Override
    protected ShadowDetailsModel createObjectDetailsModels(PrismObject<ShadowType> object) {
        return new ShadowDetailsModel(createPrismObejctModel(object), this);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                ShadowType account = getObjectDetailsModels().getObjectType();
                String accName = WebComponentUtil.getName(account);

                ResourceType resource = (ResourceType) account.getResourceRef().asReferenceValue().getObject().asObjectable();
                String name = WebComponentUtil.getName(resource);

                //TODO: refactor
                return createStringResourceStatic(PageShadow.this, "PageAccount.title", accName, name).getString();
            }
        };
    }
}
