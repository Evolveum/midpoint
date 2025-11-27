/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.withnavigation;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.CollapsedInfoPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.apache.commons.lang3.Strings;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;

import com.evolveum.midpoint.gui.api.component.wizard.*;

import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

public class WizardWithNavigationPanel<AH extends AssignmentHolderType, ADM extends AssignmentHolderDetailsModel<AH>> extends BasePanel implements WizardListener {

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_HEADER = "header";
    private static final String ID_SAVE_FRAGMENT = "saveFragment";
    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_SUMMARY = "summary";
    private static final String ID_CARD = "card";
    private static final String ID_STEP_LABEL = "stepLabel";
    private static final String ID_STEP_BADGE = "stepBadge";
    private static final String ID_STEP_IN_PROGRESS = "stepInProgress";
    private static final String ID_PARENT_STEP_LABEL = "parentStepLabel";
    private static final String ID_CONTENT_BODY = "contentBody";
    private static final String ID_COLLAPSED_INFO_PANEL = "collapsedInfoPanel";

    private final AbstractWizardController<AH, ADM> controller;

    public WizardWithNavigationPanel(String id, AbstractWizardController<AH, ADM> controller) {
        super(id);
        this.controller = controller;
        this.controller.setPanel(this);

        controller.addWizardListener(this);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        customizeHeader(response);
    }

    private void customizeHeader(IHeaderResponse response) {
        response.render(OnDomReadyHeaderItem.forScript(
                "MidPointTheme.updatePageUrlParameter('" + WizardModelBasic.PARAM_STEP + "', '" + controller.getActiveStep().getStepId() + "');"));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        this.controller.init(getPage());
    }

    private void initLayout() {
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

        AjaxLink<?> summaryButton = new AjaxLink<>(ID_SUMMARY) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                getController().showSummaryPanel();
                getController().fireActiveStepChanged(getController().getActiveStep());
                target.add(getController().getPanel());
            }
        };
        summaryButton.setOutputMarkupId(true);
        summaryButton.add(AttributeAppender.append("class", () -> getController().isShowedSummary() ? "btn-primary" : "btn-default"));
        navigation.add(summaryButton);

        IModel<List<WizardParentStep>> modelParentsView = () -> {
            List list = new ArrayList<>(getController().getAllParentSteps());
            return list;
        };
        ListView<WizardParentStep> parentsView = new ListView<>(ID_CARD, modelParentsView) {
            @Override
            protected void populateItem(ListItem<WizardParentStep> listItem) {
                populateCard(
                        listItem,
                        getController().getInProgressParentStepIndex(),
                        getController().getActiveParentStepIndex(),
                        getController().getInProgressChildrenSteps().isEmpty(),
                        !getController().isShowedSummary());
            }
        };
        navigation.add(parentsView);

        WebMarkupContainer stepInProgress = new WebMarkupContainer(ID_STEP_IN_PROGRESS);
        stepInProgress.setOutputMarkupId(true);
        stepInProgress.add(new VisibleBehaviour(() -> !getController().getInProgressChildrenSteps().isEmpty()));
        navigation.add(stepInProgress);

        stepInProgress.add(new Label(
                ID_PARENT_STEP_LABEL,
                () -> getController().getActiveParentStep() != null ? getController().getActiveParentStep().getTitle().getObject() : ""));

        IModel<List<WizardStep>> modelStepsView = () -> new ArrayList<>(getController().getInProgressChildrenSteps());
        ListView<WizardStep> stepsView = new ListView<>(ID_CARD, modelStepsView) {
            @Override
            protected void populateItem(ListItem<WizardStep> listItem) {
                populateCard(
                        listItem,
                        getController().getInProgressStepIndex(),
                        getController().getActiveStepIndex(),
                        true,
                        !getController().isShowedSummary()
                                && getController().getActiveParentStepIndex() != -1
                                && getController().getInProgressParentStepIndex() == getController().getActiveParentStepIndex());

            }
        };
        stepInProgress.add(stepsView);

        form.add(new WebMarkupContainer(ID_CONTENT_BODY));

        CollapsedInfoPanel collapsedInfoPanel = new CollapsedInfoPanel(ID_COLLAPSED_INFO_PANEL, getController());
        collapsedInfoPanel.setOutputMarkupId(true);
        form.add(collapsedInfoPanel);
    }

    protected IModel<String> getTitleModel() {
        return Model.of();
    }

    protected void onBackRedirect() {
        getPageBase().redirectBack();
    }

    private void populateCard(
            ListItem<? extends WizardStep> listItem,
            int lastShowedIndex,
            int activeIndex,
            boolean acceptEquals,
            boolean setSelectedItem) {
        listItem.add(AttributeAppender.append("class", "menu-item"));

        listItem.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                if (!Strings.CS.equals(listItem.getModelObject().getStepId(), getController().getActiveStep().getStepId())) {
                    getController().setActiveStepById(listItem.getModelObject().getStepId());
                    getController().fireActiveStepChanged(getController().getActiveStep());
                    target.add(getController().getPanel());
                }
            }
        });

        if (lastShowedIndex != -1 && (!Boolean.TRUE.equals(listItem.getModelObject().isStepVisible().getObject())
                || (!acceptEquals && listItem.getIndex() >= lastShowedIndex)
                || (acceptEquals && listItem.getIndex() > lastShowedIndex))) {
            listItem.add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        }

        listItem.add(new Label(ID_STEP_LABEL, listItem.getModelObject().getTitle()));

        String keySuffix = "complete";
        String badgeClass = "badge-success";
        if (listItem.getIndex() == lastShowedIndex) {
            keySuffix = "inProgress";
            badgeClass = "badge-info";
        } else if (setSelectedItem && listItem.getIndex() == activeIndex) {
            if (getController().isStepWithError(listItem.getModelObject().getStepId())) {
                keySuffix = "fixing";
                badgeClass = "badge-danger";
            } else {
                keySuffix = "edited";
                badgeClass = "badge-primary";
            }
        }

        if (setSelectedItem && listItem.getIndex() == activeIndex) {
            listItem.add(AttributeAppender.append("class", "border border-info"));
        }

        Label badge = new Label(ID_STEP_BADGE, createStringResource("WizardWithNavigationPanel.navigation.step.status." + keySuffix));
        badge.setOutputMarkupId(true);
        badge.add(AttributeAppender.append("class", "badge " + badgeClass));
        listItem.add(badge);
    }

    private WizardModelWithParentSteps getController() {
        return controller;
    }

    @Override
    public void onStepChanged(WizardStep newStep) {
        WizardStep step = getController().getActiveStep();
        ((Component)step).add(AttributeAppender.append("class", () -> getController().getActiveStep().appendCssToWizard()));

        ((MidpointForm)get(ID_MAIN_FORM)).addOrReplace((Component) step);
    }
}
