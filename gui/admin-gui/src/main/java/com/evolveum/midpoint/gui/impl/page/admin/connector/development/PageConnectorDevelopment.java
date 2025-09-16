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
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator.WizardParentStep;
import com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator.WizardWithNavigationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.NextStepsConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic.BasicInformationConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection.ConnectionConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.ObjectClassConnectorStepPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;

import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/connectorGenerator", matchUrlForSecurity = "/admin/connectorGenerator")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_APPLICATIONS_ALL_URL,
                        label = "PageAdminUsers.auth.connectorGenerator.label",
                        description = "PageAdminUsers.auth.connectorGenerator.description")
        })
public class PageConnectorDevelopment extends PageAssignmentHolderDetails<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> {

    @Override
    protected DetailsFragment createWizardFragment() {
        getObjectDetailsModels().reset();
        DetailsFragment fragment = new DetailsFragment(ID_DETAILS_VIEW, ID_WIZARD_FRAGMENT, PageConnectorDevelopment.this) {

            @Override
            protected void initFragmentLayout() {
                WizardWithNavigationPanel wizardPanel = new WizardWithNavigationPanel(ID_WIZARD, new WizardModelWithParentSteps(createSteps())){
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

    private @NotNull List<WizardParentStep> createSteps() {
        WizardPanelHelper<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> helper = createObjectWizardPanelHelper();
        return new ArrayList<>(
                List.of(
                        new BasicInformationConnectorStepPanel(helper),
                        new ConnectionConnectorStepPanel(helper),
                        new ObjectClassConnectorStepPanel(helper),
                        new NextStepsConnectorStepPanel(helper)));
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
