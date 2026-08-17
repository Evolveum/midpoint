/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.withnavigation;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.wizard.collapse.DrawerInfoPanel;

import com.evolveum.midpoint.gui.impl.event.FormComponentUpdatingEvent;

import org.apache.commons.lang3.Strings;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardListener;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModelBasic;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

public class WizardWithNavigationPanel<AH extends AssignmentHolderType, ADM extends AssignmentHolderDetailsModel<AH>> extends BasePanel implements WizardListener {

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_HEADER = "header";
    private static final String ID_SAVE_FRAGMENT = "saveFragment";
    private static final String ID_PROGRESS_CONTAINER = "progressContainer";
    private static final String ID_PROGRESS_ICON = "progressIcon";
    private static final String ID_PROGRESS_MESSAGE = "progressMessage";
    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_SUMMARY = "summary";
    private static final String ID_CARD = "card";
    private static final String ID_STEP_LABEL = "stepLabel";
    private static final String ID_STEP_BADGE = "stepBadge";
    private static final String ID_STEP_IN_PROGRESS = "stepInProgress";
    private static final String ID_PARENT_STEP_LABEL = "parentStepLabel";
    private static final String ID_CONTENT_BODY = "contentBody";
    private static final String ID_DRAWER_INFO_PANEL = "drawerInfoPanel";

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
                Fragment saveMessageFragment = new Fragment(id, ID_SAVE_FRAGMENT, WizardWithNavigationPanel.this) {
                    @Override
                    public void onEvent(IEvent<?> event) {
                        super.onEvent(event);

                        if (event.getPayload() instanceof FormComponentUpdatingEvent formComponentUpdatingEvent) {
                            AjaxRequestTarget target = formComponentUpdatingEvent.AjaxRequestTarget();
                            target.add(get(ID_PROGRESS_CONTAINER));
                        }
                    }
                };
                saveMessageFragment.add(new VisibleBehaviour(() -> getController().getHelper().getDetailsModel().isEditObject()));
                saveMessageFragment.setOutputMarkupId(true);

                WebMarkupContainer progressContainer = new WebMarkupContainer(ID_PROGRESS_CONTAINER);
                progressContainer.setOutputMarkupId(true);
                LoadableDetachableModel<String> progressMessageTitleModel = new LoadableDetachableModel<>() {
                    @Override
                    protected String load() {
                        if (getController().getHelper().getDetailsModel().hasDelta()) {
                            return getString("PageConnectorDevelopment.header.recognizedChanges.title");
                        }
                        return getString("PageConnectorDevelopment.header.save.title");
                    }
                };
                progressContainer.add(AttributeAppender.replace("title", progressMessageTitleModel));
                saveMessageFragment.add(progressContainer);

                WebMarkupContainer progressIcon = new WebMarkupContainer(ID_PROGRESS_ICON);
                progressIcon.setOutputMarkupId(true);
                progressIcon.add(
                        AttributeAppender.replace(
                                "class",
                                () -> getController().getHelper().getDetailsModel().hasDelta() ?
                                        "fa fa-info-circle text-info" : "fa fa-check-circle text-success"));
                progressContainer.add(progressIcon);

                LoadableDetachableModel<String> progressMessageModel = new LoadableDetachableModel<>() {
                    @Override
                    protected String load() {
                        if (getController().getHelper().getDetailsModel().hasDelta()) {
                            return getString("PageConnectorDevelopment.header.recognizedChanges");
                        }
                        return getString("PageConnectorDevelopment.header.save");
                    }
                };
                Label progressMessage = new Label(ID_PROGRESS_MESSAGE, progressMessageModel);
                progressMessage.setOutputMarkupId(true);
                progressMessage.add(
                        AttributeAppender.replace(
                                "class",
                                () -> getController().getHelper().getDetailsModel().hasDelta() ? "text-info" : "text-success"));
                progressContainer.add(progressMessage);

                return saveMessageFragment;
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
        summaryButton.add(AttributeAppender.append("class", () -> getController().isShowedSummary() ? "btn-primary" : "btn-light border"));
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

        DrawerInfoPanel drawerInfoPanel = new DrawerInfoPanel(ID_DRAWER_INFO_PANEL, getController());
        drawerInfoPanel.setOutputMarkupId(true);
        form.add(drawerInfoPanel);
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
        String badgeClass = "bg-success";
        if (listItem.getIndex() == lastShowedIndex) {
            keySuffix = "inProgress";
            badgeClass = "bg-info";
        } else if (setSelectedItem && listItem.getIndex() == activeIndex) {
            if (getController().isStepWithError(listItem.getModelObject().getStepId())) {
                keySuffix = "fixing";
                badgeClass = "bg-danger";
            } else {
                keySuffix = "edited";
                badgeClass = "bg-primary";
            }
        }

        if (setSelectedItem && listItem.getIndex() == activeIndex) {
            listItem.add(AttributeAppender.append("class", "border border-primary text-primary"));
        }

        Label badge = new Label(ID_STEP_BADGE, createStringResource("WizardWithNavigationPanel.navigation.step.status." + keySuffix));
        badge.setOutputMarkupId(true);
        badge.add(AttributeAppender.append("class", "badge " + badgeClass + " opaque"));
        listItem.add(badge);
    }

    private AbstractWizardController<AH, ADM> getController() {
        return controller;
    }

    @Override
    public void onStepChanged(WizardStep newStep) {
        WizardStep step = getController().getActiveStep();
        ((Component) step).add(AttributeAppender.append("class", () -> getController().getActiveStep().appendCssToWizard()));

        ((MidpointForm) get(ID_MAIN_FORM)).addOrReplace((Component) step);
    }
}
