/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.PageConnectorDevelopment;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;

import com.evolveum.midpoint.gui.api.component.wizard.*;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

public class WizardWithNavigationPanel extends WizardPanel {

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_HEADER = "header";
    private static final String ID_SAVE_FRAGMENT = "saveFragment";
    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_CARD = "card";
    private static final String ID_STEP_LABEL = "stepLabel";
    private static final String ID_STEP_BADGE = "stepBadge";
    private static final String ID_STEP_IN_PROGRESS = "stepInProgress";
    private static final String ID_PARENT_STEP_LABEL = "parentStepLabel";

    public WizardWithNavigationPanel(String id, WizardModel wizardModel) {
        super(id, wizardModel);
    }

    @Override
    protected void customizeHeader(IHeaderResponse response) {
        //we don't need it
    }

    @Override
    protected void initLayout() {
        MidpointForm form = new MidpointForm<>(ID_MAIN_FORM);
        add(form);

        NavigationPanel header = new NavigationPanel(ID_HEADER) {
            @Override
            protected AjaxLink createBackButton(String id, IModel<String> backTitle) {
                AjaxLink back = super.createBackButton(id, backTitle);
                back.add(AttributeAppender.replace("class", "btn btn-link"));
                return back;
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                onBackRedirect();
            }

            @Override
            protected IModel<String> createTitleModel() {
                return getTitleModel();
            }

            @Override
            protected Component createNextButton(String id, IModel<String> nextTitle) {
                Fragment next = new Fragment(id, ID_SAVE_FRAGMENT, WizardWithNavigationPanel.this);
                next.setRenderBodyOnly(true);
                return next;
            }
        };
        form.add(header);

        WebMarkupContainer navigation = new WebMarkupContainer(ID_NAVIGATION);
        navigation.setOutputMarkupId(true);
        form.add(navigation);

        IModel<List<WizardParentStep>> modelParentsView = () -> {
            List list = new ArrayList<>(getWizardModel().getSteps());
            return list;
        };
        ListView<WizardParentStep> parentsView = new ListView<>(ID_CARD, modelParentsView) {
            @Override
            protected void populateItem(ListItem<WizardParentStep> listItem) {
                populateCard(
                        listItem,
                        getWizardModel().getActiveParentStepIndex(),
                        getWizardModel().getActiveChildrenSteps().isEmpty());
            }
        };
        navigation.add(parentsView);

        WebMarkupContainer stepInProgress = new WebMarkupContainer(ID_STEP_IN_PROGRESS);
        stepInProgress.setOutputMarkupId(true);
        stepInProgress.add(new VisibleBehaviour(() -> !getWizardModel().getActiveChildrenSteps().isEmpty()));
        navigation.add(stepInProgress);

        stepInProgress.add(new Label(ID_PARENT_STEP_LABEL, () -> getWizardModel().getActiveParentStep().getTitle().getObject()));

        IModel<List<WizardStep>> modelStepsView = () -> new ArrayList<>(getWizardModel().getActiveChildrenSteps());
        ListView<WizardStep> stepsView = new ListView<>(ID_CARD, modelStepsView) {
            @Override
            protected void populateItem(ListItem<WizardStep> listItem) {
                populateCard(listItem, getWizardModel().getActiveStepIndex(), true);
            }
        };
        stepInProgress.add(stepsView);
        form.add(new WebMarkupContainer(ID_CONTENT_BODY));
    }

    protected IModel<String> getTitleModel() {
        return Model.of();
    }

    protected void onBackRedirect() {
        getPageBase().redirectBack();
    }

    private void populateCard(ListItem<? extends WizardStep> listItem, int lastShowedIndex, boolean acceptEquals) {
        if (!Boolean.TRUE.equals(listItem.getModelObject().isStepVisible().getObject())
                || (!acceptEquals && listItem.getIndex() >= lastShowedIndex)
                || (acceptEquals && listItem.getIndex() > lastShowedIndex)) {
            listItem.add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        }

        listItem.add(new Label(ID_STEP_LABEL, listItem.getModelObject().getTitle()));

        String keySuffix = "complete";
        String badgeClass = "badge-success";
        if (listItem.getIndex() == lastShowedIndex) {
            keySuffix = "inProgress";
            listItem.add(AttributeAppender.append("class", "border border-info"));
            badgeClass = "badge-info";
        }

        Label badge = new Label(ID_STEP_BADGE, createStringResource("WizardWithNavigationPanel.navigation.step.status." + keySuffix));
        badge.setOutputMarkupId(true);
        badge.add(AttributeAppender.append("class", "badge " + badgeClass));
        listItem.add(badge);
    }

    @Override
    public WizardModelWithParentSteps getWizardModel() {
        return (WizardModelWithParentSteps) super.getWizardModel();
    }

    @Override
    public void onStepChanged(WizardStep newStep) {
        WizardStep step = getActiveStep();
        ((Component)step).add(AttributeAppender.append("class", () -> getActiveStep().appendCssToWizard()));

        ((MidpointForm)get(ID_MAIN_FORM)).addOrReplace((Component) step);
    }
}
