/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lskublik
 */
public class BasicWizardPanel extends WizardStepPanel {

    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";
    private static final String ID_BACK = "back";
    private static final String ID_NEXT = "next";
    private static final String ID_NEXT_LABEL = "nextLabel";

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {

        add(new Label(ID_TEXT, getTextModel()));
        add(new Label(ID_SUBTEXT, getSubTextModel()));

        AjaxLink back = new AjaxLink<>(ID_BACK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onBackPerformed(target);
            }
        };
        back.add(getBackBehaviour());
        back.setOutputMarkupId(true);
        back.setOutputMarkupPlaceholderTag(true);
        add(back);

        AjaxSubmitButton next = new AjaxSubmitButton(ID_NEXT) {

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                onNextPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                updateFeedbackPanels(target);
            }
        };
        next.add(getNextBehaviour());
        next.setOutputMarkupId(true);
        next.setOutputMarkupPlaceholderTag(true);
        next.setDefaultFormProcessing(true);
        add(next);

        Label nextLabel = new Label(ID_NEXT_LABEL, createNextStepLabel());
        next.add(nextLabel);

    }

    protected void updateFeedbackPanels(AjaxRequestTarget target) {
    }

    protected WebMarkupContainer createContentPanel(String id) {
        return new WebMarkupContainer(id);
    }

    protected IModel<?> getTextModel() {
        return Model.of();
    }

    protected IModel<?> getSubTextModel() {
        return Model.of();
    }

    private IModel<?> createNextStepLabel() {
        WizardStep nextPanel = getWizard().getNextPanel();
        if (nextPanel != null){
            return nextPanel.getTitle();
        }
        return Model.of();
    }

    private void onNextPerformed(AjaxRequestTarget target) {
        getWizard().next();
        target.add(getParent());
    }

    private void onBackPerformed(AjaxRequestTarget target) {
        int index = getWizard().getActiveStepIndex();
        if (index > 0) {
            getWizard().previous();
            target.add(getParent());
            return;
        }
        onBackBeforeWizardPerformed(target);
    }

    protected void onBackBeforeWizardPerformed(AjaxRequestTarget target) {
        getPageBase().redirectBack();
    }

    @Override
    public VisibleEnableBehaviour getHeaderBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}
