/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardHeader extends BasePanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_BACK = "back";
    private static final String ID_TITLE = "title";
    private static final String ID_CONTENT = "content";
    private static final String ID_NEXT = "next";
    private static final String ID_NEXT_LABEL = "nextLabel";

    private WizardModel model;

    private int activeStepIndex = -1;

    public WizardHeader(String id, WizardModel model) {
        super(id);

        this.model = model;

        IModel<String> currentPanelTitle = () -> model.getActiveStep().getTitle().getObject();
        IModel<String> nextPanelTitle = () -> {
            WizardStep next = model.getNextPanel();
            return next != null ? next.getTitle().getObject() : null;
        };

        initLayout(currentPanelTitle, nextPanelTitle);
    }

    @Override
    protected void onBeforeRender() {
        addOrReplace(createHeaderContent(ID_CONTENT));

        super.onBeforeRender();
    }

    private void initLayout(IModel<String> currentPanelTitle, IModel<String> nextPanelTitle) {
        add(AttributeAppender.append("class", "d-flex align-items-center flex-wrap gap-3 mb-3"));

        AjaxLink back = createBackButton(ID_BACK);
        add(back);
        AjaxLink next = createNextButton(ID_NEXT, nextPanelTitle);
        add(next);

        add(new Label(ID_TITLE, currentPanelTitle));
    }

    protected Component createHeaderContent(String id) {
        return new WebMarkupContainer(id);
    }

    protected AjaxLink createNextButton(String id, IModel<String> nextPanelTitle) {
        AjaxLink next = new AjaxLink<>(id) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onNextPerformed(target);
            }
        };
        next.setOutputMarkupId(true);
        next.setOutputMarkupPlaceholderTag(true);
        WebComponentUtil.addDisabledClassBehavior(next);

        next.add(new BehaviourDelegator(() -> getNextVisibilityBehaviour()));

        next.add(new Label(ID_NEXT_LABEL, nextPanelTitle));
        return next;
    }

    @NotNull
    protected VisibleEnableBehaviour getNextVisibilityBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    protected AjaxLink createBackButton(String id) {
        AjaxLink back = new AjaxLink<>(id) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onBackPerformed(target);
            }
        };
        back.setOutputMarkupId(true);
        back.setOutputMarkupPlaceholderTag(true);
        WebComponentUtil.addDisabledClassBehavior(back);

        back.add(new BehaviourDelegator(() -> getBackVisibilityBehaviour()));

        return back;
    }

    @NotNull
    protected VisibleEnableBehaviour getBackVisibilityBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    protected void onBackPerformed(AjaxRequestTarget target) {

    }

    protected void onNextPerformed(AjaxRequestTarget target) {

    }
}
