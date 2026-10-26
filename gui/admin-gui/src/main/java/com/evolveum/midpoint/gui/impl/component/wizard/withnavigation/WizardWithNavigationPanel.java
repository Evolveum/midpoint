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
    private static final String ID_PARENT_CARD_CONTAINER = "parentCardContainer";
    private static final String ID_PARENT_CARD = "parentCard";
    private static final String ID_PARENT_EDIT_CONTAINER = "parentEditContainer";
    private static final String ID_PARENT_EDIT_STEP_LABEL = "parentEditStepLabel";
    private static final String ID_EDIT_CARD = "editCard";
    private static final String ID_STEP_LABEL = "stepLabel";
    private static final String ID_STEP_BADGE = "stepBadge";
    private static final String ID_STEP_IN_PROGRESS = "stepInProgress";
    private static final String ID_PARENT_IN_PROGRESS_STEP_LABEL = "parentInProgressStepLabel";
    private static final String ID_MORE_IN_PROGRESS_STEPS_LABEL = "moreInProgressStepsLabel";
    private static final String ID_IN_PROGRESS_CARD = "inProgressCard";
    private static final String ID_CONTENT_BODY = "contentBody";
    private static final String ID_DRAWER_INFO_PANEL = "drawerInfoPanel";

    private static final String DRAWER_WIDTH = "32rem";

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
                onBackRedirect(target);
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

        IModel<List<WizardParentStep>> modelParentsView = () -> new ArrayList<>(getController().getAllParentSteps());

        ListView<WizardParentStep> parentsView = new ListView<>(ID_PARENT_CARD_CONTAINER, modelParentsView) {
            @Override
            protected void populateItem(ListItem<WizardParentStep> listItem) {
                WebMarkupContainer parentCard = new WebMarkupContainer(ID_PARENT_CARD);
                parentCard.setOutputMarkupId(true);
                listItem.add(parentCard);

                populateCard(
                        listItem,
                        parentCard,
                        getController().getInProgressParentStepIndex(),
                        getController().getActiveParentStep() != null ? getController().getActiveParentStep().getStepId() : "",
                        getController().getInProgressChildrenSteps().isEmpty(),
                        getController().getActiveChildrenSteps().isEmpty(),
                        new VisibleBehaviour(() -> !isActiveParentWithChild(listItem.getModelObject().getStepId())));

                WebMarkupContainer parentEditContainer = new WebMarkupContainer(ID_PARENT_EDIT_CONTAINER);
                parentEditContainer.setOutputMarkupId(true);
                parentEditContainer.add(new VisibleBehaviour(() -> isActiveParentWithChild(listItem.getModelObject().getStepId())));
                listItem.add(parentEditContainer);

                parentEditContainer.add(new Label(
                        ID_PARENT_EDIT_STEP_LABEL,
                        () -> getController().getActiveParentStep() != null ? getController().getActiveParentStep().getTitle().getObject() : ""));

                IModel<List<WizardStep>> modelStepsView = () -> new ArrayList<>(
                        getController().getActiveChildrenSteps().stream()
                                .filter(step -> Boolean.TRUE.equals(step.isStepVisible().getObject()))
                                .toList());
                ListView<WizardStep> stepsView = new ListView<>(ID_EDIT_CARD, modelStepsView) {
                    @Override
                    protected void populateItem(ListItem<WizardStep> listItem) {
                        populateCard(
                                listItem,
                                listItem,
                                -1,
                                getController().getActiveStep() != null ? getController().getActiveStep().getStepId() : "",
                                true,
                                !getController().isShowedSummary()
                                        && getController().getActiveParentStepIndex() != -1,
                                new VisibleBehaviour(()-> true));
                    }
                };
                parentEditContainer.add(stepsView);
            }
        };
        navigation.add(parentsView);

        WebMarkupContainer stepInProgress = new WebMarkupContainer(ID_STEP_IN_PROGRESS);
        stepInProgress.setOutputMarkupId(true);
        stepInProgress.add(new VisibleBehaviour(() ->
                getController().getInProgressStepIndex() != -1
                        && !getController().getInProgressChildrenSteps().isEmpty()));
        navigation.add(stepInProgress);

        stepInProgress.add(new Label(
                ID_PARENT_IN_PROGRESS_STEP_LABEL,
                () -> getController().getInProgressParentStep() != null ? getController().getInProgressParentStep().getTitle().getObject() : ""));

        Label moreInProgressStepsLabel = new Label(ID_MORE_IN_PROGRESS_STEPS_LABEL, getMoreStepsLabelModel());

        moreInProgressStepsLabel.setOutputMarkupId(true);
        moreInProgressStepsLabel.add(new VisibleBehaviour(() -> !getController().isActiveAndInProgressParentSame() && getController().getInProgressStepIndex() > 0));
        stepInProgress.add(moreInProgressStepsLabel);

        IModel<List<WizardStep>> modelStepsView = () -> new ArrayList<>(getController().getInProgressChildrenSteps());
        ListView<WizardStep> stepsView = new ListView<>(ID_IN_PROGRESS_CARD, modelStepsView) {
            @Override
            protected void populateItem(ListItem<WizardStep> listItem) {
                int lastShowedIndex = getController().getInProgressStepIndex();
                populateCard(
                        listItem,
                        listItem,
                        lastShowedIndex,
                        getController().getActiveStep() != null ? getController().getActiveStep().getStepId() : "",
                        true,
                        !getController().isShowedSummary()
                                && getController().getActiveParentStepIndex() != -1
                                && getController().isActiveAndInProgressParentSame(),
                        new VisibleBehaviour(() -> getController().isActiveAndInProgressParentSame()
                                || listItem.getIndex() == lastShowedIndex));

            }
        };
        stepInProgress.add(stepsView);

        form.add(new WebMarkupContainer(ID_CONTENT_BODY));

        DrawerInfoPanel drawerInfoPanel = new DrawerInfoPanel(ID_DRAWER_INFO_PANEL, getController()) {
            @Override
            protected String getMinWidth() {
                return DRAWER_WIDTH;
            }
        };
        drawerInfoPanel.setOutputMarkupId(true);
        form.add(drawerInfoPanel);
    }

    private IModel<Object> getMoreStepsLabelModel() {
        return () -> {
            if (getController().getInProgressStepIndex() < 1) {
                return "";
            }

            List<WizardStep> beforeSteps = getController().getInProgressChildrenSteps().subList(0, getController().getInProgressStepIndex()).stream()
                    .filter(step -> Boolean.TRUE.equals(step.isStepVisible().getObject()))
                    .toList();
            int beforeStepsCount = beforeSteps.size();

            if (beforeStepsCount == 1) {
                return beforeSteps.get(0).getTitle().getObject();
            }
            return getString("WizardWithNavigationPanel.moreInProgressStepsLabel", beforeStepsCount);
        };
    }

    private boolean isActiveParentWithChild(String stepId) {
        if (getController().getActiveParentStep() == null) {
            return false;
        }
        return Strings.CS.equals(stepId, getController().getActiveParentStep().getStepId())
                && !getController().getActiveChildrenSteps().isEmpty();
    }

    protected IModel<String> getTitleModel() {
        return Model.of();
    }

    protected void onBackRedirect(AjaxRequestTarget target) {
        getPageBase().redirectBack();
    }

    private void populateCard(
            ListItem<? extends WizardStep> listItem,
            WebMarkupContainer parent,
            int lastShowedIndex,
            String activeStepId,
            boolean acceptEquals,
            boolean setSelectedItem,
            VisibleBehaviour additionalVisibility) {
        parent.add(AttributeAppender.append("class", "menu-item"));

        parent.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                String targetStepId = listItem.getModelObject().getStepId();
                String activeStepId = getController().getActiveStep().getStepId();
                boolean hasChanges = getController().hasUnsavedChanges();
                if (!Strings.CS.equals(targetStepId, activeStepId)) {
                    if (hasChanges) {
                        getController().showUnsavedStepChangesConfirmation(target, t -> jumpToStep(targetStepId, t));
                        return;
                    }
                    jumpToStep(targetStepId, target);
                }
            }
        });

        if (lastShowedIndex != -1 && (!Boolean.TRUE.equals(listItem.getModelObject().isStepVisible().getObject())
                || (!acceptEquals && listItem.getIndex() >= lastShowedIndex)
                || (acceptEquals && listItem.getIndex() > lastShowedIndex))) {
            parent.add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
            listItem.add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        } else {
            parent.add(additionalVisibility);
        }

        parent.add(new Label(ID_STEP_LABEL, listItem.getModelObject().getTitle()));

        String keySuffix = "complete";
        String badgeClass = "bg-success";
        if (listItem.getIndex() == lastShowedIndex) {
            keySuffix = "inProgress";
            badgeClass = "bg-info";
            setSelectedItem = true;
        } else if (setSelectedItem && Strings.CS.equals(activeStepId, listItem.getModelObject().getStepId())) {
            if (getController().isStepWithError(listItem.getModelObject().getStepId())) {
                keySuffix = "fixing";
                badgeClass = "bg-danger";
            } else {
                keySuffix = "edited";
                badgeClass = "bg-primary";
            }
        }

        if (setSelectedItem && Strings.CS.equals(activeStepId, listItem.getModelObject().getStepId())) {
            listItem.add(AttributeAppender.append("class", "border border-primary text-primary"));
        }

        Label badge = new Label(ID_STEP_BADGE, createStringResource("WizardWithNavigationPanel.navigation.step.status." + keySuffix));
        badge.setOutputMarkupId(true);
        badge.add(AttributeAppender.append("class", "badge " + badgeClass + " opaque"));
        parent.add(badge);
    }

    private AbstractWizardController<AH, ADM> getController() {
        return controller;
    }

    private void jumpToStep(String targetStepId, AjaxRequestTarget target) {
        getController().setActiveStepById(targetStepId);
        getController().fireActiveStepChanged(getController().getActiveStep());
        target.add(getController().getPanel());
    }

    @Override
    public void onStepChanged(WizardStep newStep) {
        WizardStep step = getController().getActiveStep();
        ((Component) step).add(AttributeAppender.append("class", () -> getController().getActiveStep().appendCssToWizard()));

        ((MidpointForm) get(ID_MAIN_FORM)).addOrReplace((Component) step);
        // Only now is onInitialize() guaranteed to have run (on a first-ever visit, it fires as a
        // side effect of addOrReplace adding the component to the live page tree) - applyState()
        // may depend on fields onInitialize() sets up.
        step.applyState();
    }
}
