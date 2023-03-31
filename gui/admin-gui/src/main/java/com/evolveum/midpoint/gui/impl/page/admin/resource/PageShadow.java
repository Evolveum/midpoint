/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.page.admin.resources.ShadowSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Collection;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/resources/shadow")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                label = "PageAdminResources.auth.resourcesAll.label",
                description = "PageAdminResources.auth.resourcesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCES_ACCOUNT_URL,
                label = "PageShadow.auth.resourcesAccount.label",
                description = "PageShadow.auth.resourcesAccount.description")})
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
    public Class<ShadowType> getType() {
        return ShadowType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<ShadowType> summaryModel) {
        return new ShadowSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }

    @Override
    protected ShadowDetailsModel createObjectDetailsModels(PrismObject<ShadowType> object) {
        return new ShadowDetailsModel(createPrismObjectModel(object), this);
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
                return createStringResourceStatic("PageAccount.title", accName, accName, name).getString();
            }
        };
    }
}
