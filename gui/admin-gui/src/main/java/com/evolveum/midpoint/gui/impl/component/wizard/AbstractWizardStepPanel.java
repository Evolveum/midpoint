/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardStepPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
public abstract class AbstractWizardStepPanel<ODM extends ObjectDetailsModels>
        extends BasicWizardStepPanel {

    private static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FEEDBACK = "feedback";

    private final ODM detailsModel;
    public AbstractWizardStepPanel(ODM model){
        this.detailsModel = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initFeedbackContainer();
    }

    private void initFeedbackContainer() {
        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        feedbackContainer.setOutputMarkupPlaceholderTag(true);
        add(feedbackContainer);

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        feedbackContainer.add(feedbackList);
    }

    public ODM getDetailsModel() {
        return detailsModel;
    }

    protected void onSubmitPerformed(AjaxRequestTarget target) {
        target.add(getFeedback());
    }

    @Override
    public String appendCssToWizard() {
        return "mt-5 mx-auto col-8";
    }

    protected IModel<String> getFormTitle() {
        return getTitle();
    }

    protected WebMarkupContainer getFeedback() {
        return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
    }

    @Override
    protected void updateFeedbackPanels(AjaxRequestTarget target) {
        target.add(getFeedback());
    }

    @Override
    public VisibleEnableBehaviour getStepsBehaviour() {
        if (getWizard().getSteps().size() <= 1) {
            return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
        }
        return super.getStepsBehaviour();
    }

    protected ContainerPanelConfigurationType getContainerConfiguration(String panelType) {
        return WebComponentUtil.getContainerConfiguration(
                (GuiObjectDetailsPageType) getDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                panelType);
    }
}
