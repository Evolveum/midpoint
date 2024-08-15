/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractWizardBasicPanel<AHD extends AssignmentHolderDetailsModel> extends BasePanel {


    private static final String ID_BREADCRUMB = "breadcrumb";
    private static final String ID_BC_NAME = "bcName";
    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";
    private static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_BUTTONS_CONTAINER = "buttonsContainer";

    private final AHD detailsModel;
    public AbstractWizardBasicPanel(String id, AHD detailsModel) {
        super(id);
        this.detailsModel = detailsModel;
    }

    public AHD getAssignmentHolderDetailsModel() {
        return detailsModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        addBreadcrumb();
        initLayout();
    }

    private void addBreadcrumb() {
        List<Breadcrumb> breadcrumbs = getBreadcrumb();
        IModel<String> breadcrumbLabelModel = getBreadcrumbLabel();
        String breadcrumbLabel = breadcrumbLabelModel.getObject();
        if (StringUtils.isEmpty(breadcrumbLabel)) {
            return;
        }

        if (breadcrumbs.isEmpty() && StringUtils.isNotEmpty(breadcrumbLabel)) {
            breadcrumbs.add(new Breadcrumb(breadcrumbLabelModel));
        return;
        }

        String lastBreadcrumbLabel = breadcrumbs.get(breadcrumbs.size() - 1).getLabel().getObject();
        if (StringUtils.isNotEmpty(lastBreadcrumbLabel)
                && StringUtils.isNotEmpty(breadcrumbLabel)
                && !lastBreadcrumbLabel.equals(breadcrumbLabel)) {
            breadcrumbs.add(new Breadcrumb(breadcrumbLabelModel));
        }
    }

    @NotNull protected abstract IModel<String> getBreadcrumbLabel();

    protected void removeLastBreadcrumb() {
        if (!getBreadcrumb().isEmpty()) {
            int index = getBreadcrumb().size() - 1;
            getBreadcrumb().remove(index);
        }
    }

    private void initLayout() {

        IModel<List<Breadcrumb>> breadcrumb = () -> getBreadcrumb();
        ListView<Breadcrumb> breadcrumbs = new ListView<>(ID_BREADCRUMB, breadcrumb) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Breadcrumb> item) {
                int index = item.getIndex() + 1;
                if (index == getList().size()) {
                    item.add(AttributeAppender.append("class", "text-primary"));
                }

                Label bcName = new Label(ID_BC_NAME, item.getModelObject().getLabel());
                item.add(bcName);

                item.add(new VisibleBehaviour(() -> item.getModelObject().isVisible()));
            }
        };
        add(breadcrumbs);
        breadcrumbs.add(new VisibleBehaviour(() -> getBreadcrumb().size() > 1));

        Label mainText = new Label(ID_TEXT, getTextModel());
        mainText.add(new VisibleBehaviour(() -> getTextModel() != null && getTextModel().getObject() != null));
        add(mainText);

        Label secondaryText = new Label(ID_SUBTEXT, getSubTextModel());
        secondaryText.add(new VisibleBehaviour(() -> getSubTextModel() != null && getSubTextModel().getObject() != null));
        add(secondaryText);

        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        feedbackContainer.setOutputMarkupPlaceholderTag(true);
        add(feedbackContainer);
        feedbackContainer.add(AttributeAppender.append("class", getCssForWidthOfFeedbackPanel()));

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        feedbackContainer.add(feedbackList);

        WebMarkupContainer buttonsContainer = new WebMarkupContainer(ID_BUTTONS_CONTAINER);
        buttonsContainer.setOutputMarkupId(true);
        add(buttonsContainer);

        RepeatingView buttons = new RepeatingView(ID_BUTTONS);

        AjaxIconButton back = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fas fa-arrow-left"),
                getExitLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onExitPerformed(target);
            }
        };
        back.showTitleAsLabel(true);
        back.add(new VisibleBehaviour(() -> isBackButtonVisible()));
        back.add(AttributeAppender.append("class", "text-primary"));
        buttons.add(back);

        AjaxIconButton exit = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fas fa-right-from-bracket fa-rotate-180"),
                getExitLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onExitPerformed(target);
            }
        };
        exit.showTitleAsLabel(true);
        exit.add(new VisibleBehaviour(() -> isExitButtonVisible()));
        exit.add(AttributeAppender.append("class", "btn-default"));
        buttons.add(exit);

        addCustomButtons(buttons);
        buttonsContainer.add(buttons);

        AjaxIconButton saveButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of(getSubmitIcon()),
                getSubmitLabelModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onSubmitPerformed(target);
            }
        };
        saveButton.showTitleAsLabel(true);
        saveButton.add(new VisibleBehaviour(() -> isSubmitButtonVisible()));
        saveButton.add(AttributeAppender.append("class", "btn-success"));
        buttons.add(saveButton);
    }

    protected boolean isBackButtonVisible() {
        return false;
    }

    protected String getCssForWidthOfFeedbackPanel() {
        return "col-12";
    }

    protected boolean isSubmitButtonVisible() {
        return false;
    }

    protected void onSubmitPerformed(AjaxRequestTarget target) {
    }

    protected String getSubmitIcon() {
        return "fa fa-check";
    }

    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("WizardPanel.submit");
    }

    protected boolean isExitButtonVisible() {
        return true;
    }

    protected WebMarkupContainer getButtonsContainer() {
        return (WebMarkupContainer) get(ID_BUTTONS_CONTAINER);
    }

    private List<Breadcrumb> getBreadcrumb() {
        PageBase page = getPageBase();
        if (page instanceof PageAssignmentHolderDetails) {
            return ((PageAssignmentHolderDetails)page).getWizardBreadcrumbs();
        }
        return List.of();
    }

    protected IModel<String> getExitLabel() {
        return getPageBase().createStringResource("WizardPanel.exit");
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
        removeLastBreadcrumb();
        getPageBase().getPageParameters().remove(WizardModel.PARAM_STEP);
    }

    protected void addCustomButtons(RepeatingView buttons) {
    }

    protected IModel<String> getSubTextModel(){
        return getPageBase().createStringResource(getClass().getSimpleName() + ".text");
    };

    protected IModel<String> getTextModel(){
        return getPageBase().createStringResource(getClass().getSimpleName() + ".subText");
    }

    protected WebMarkupContainer getFeedback() {
        return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
    }
}
