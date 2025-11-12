/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardWithNavigationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentController;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;

import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/connectorGenerator", matchUrlForSecurity = "/admin/connectorGenerator")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONNECTOR_DEVELOPMENTS_ALL_URL,
                        label = "PageAdminConnectorDevelopments.auth.connectorDevelopmentsAll.label",
                        description = "PageAdminConnectorDevelopments.auth.connectorDevelopmentsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONNECTOR_DEVELOPMENT_URL,
                        label = "PageAdminConnectorDevelopments.auth.connectorGenerator.label",
                        description = "PageAdminConnectorDevelopments.auth.connectorGenerator.description")
        })
public class PageConnectorDevelopment extends PageAssignmentHolderDetails<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> {

    public PageConnectorDevelopment() {
        super();
    }

    public PageConnectorDevelopment(PageParameters params) {
        super(params);
    }

    public PageConnectorDevelopment(PrismObject<ConnectorDevelopmentType> connectorDevelopment) {
        super(connectorDevelopment);

    }

    @Override
    protected DetailsFragment createWizardFragment() {
        getObjectDetailsModels().reset();
        DetailsFragment fragment = new DetailsFragment(ID_DETAILS_VIEW, ID_WIZARD_FRAGMENT, PageConnectorDevelopment.this) {

            @Override
            protected void initFragmentLayout() {
                WizardWithNavigationPanel<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> wizardPanel =
                        new WizardWithNavigationPanel<>(
                                ID_WIZARD,
                                new ConnectorDevelopmentController(createObjectWizardPanelHelper())) {
                            @Override
                            protected void onBackRedirect() {
                                redirectBack();
                            }

                            @Override
                            protected IModel<String> getTitleModel() {
                                return createPageTitleModel();
                            }
                        };
                wizardPanel.setOutputMarkupId(true);
                add(wizardPanel);
            }

        };
        fragment.setOutputMarkupId(true);

        return fragment;
    }

    @Override
    public Class<ConnectorDevelopmentType> getType() {
        return ConnectorDevelopmentType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<ConnectorDevelopmentType> summaryModel) {
        return null;
    }

    @Override
    protected boolean isShowedByWizard() {
        return true;
    }

    @Override
    protected ConnectorDevelopmentDetailsModel createObjectDetailsModels(PrismObject<ConnectorDevelopmentType> object) {
        return new ConnectorDevelopmentDetailsModel(createPrismObjectModel(object), this);
    }

    @Override
    protected boolean canShowWizard() {
        return true;
    }
}
