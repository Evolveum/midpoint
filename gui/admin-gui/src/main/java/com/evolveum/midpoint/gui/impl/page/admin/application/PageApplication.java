/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.application;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.PageAbstractRole;
import com.evolveum.midpoint.gui.impl.page.admin.application.component.ApplicationSummaryPanel;
import com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.IntegrationCatalogPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/application")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_APPLICATIONS_ALL_URL,
                        label = "PageApplication.auth.applicationsAll.label",
                        description = "PageApplication.auth.applicationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_APPLICATION_URL,
                        label = "PageApplication.auth.application.label",
                        description = "PageApplication.auth.application.description")
        })
public class PageApplication extends PageAbstractRole<ApplicationType, AbstractRoleDetailsModel<ApplicationType>> {

    public PageApplication() {
        super();
    }

    public PageApplication(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageApplication(PrismObject<ApplicationType> application) {
        super(application);
    }

    @Override
    public Class<ApplicationType> getType() {
        return ApplicationType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<ApplicationType> summaryModel) {
        return new ApplicationSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }

    @Override
    protected DetailsFragment createWizardFragment() {
        getObjectDetailsModels().reset();
        return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageApplication.this) {
            @Override
            protected void initFragmentLayout() {
                add(new IntegrationCatalogPanel(ID_TEMPLATE));
            }
        };
    }

    @Override
    protected boolean canShowWizard() {
        return isAdd();
    }
}
