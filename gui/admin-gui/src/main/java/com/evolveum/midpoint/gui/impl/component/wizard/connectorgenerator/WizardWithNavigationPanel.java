/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;

import com.evolveum.midpoint.gui.api.component.wizard.*;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

public class WizardWithNavigationPanel extends WizardPanel {

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
        WebMarkupContainer navigation = new WebMarkupContainer(ID_NAVIGATION);
        navigation.setOutputMarkupId(true);
        add(navigation);

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
        add(new WebMarkupContainer(ID_CONTENT_BODY));
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
}
