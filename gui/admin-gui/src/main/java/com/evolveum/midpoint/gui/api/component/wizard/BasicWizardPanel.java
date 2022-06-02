/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lskublik
 */
public class BasicWizardPanel extends BasePanel implements WizardPanel{

    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";

    private static final String ID_CONTENT = "content";
    private static final String ID_BACK = "back";
    private static final String ID_NEXT = "next";
    private static final String ID_NEXT_LABEL = "nextLabel";

    public BasicWizardPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {

        add(new Label(ID_TEXT, getTextModel()));
        add(new Label(ID_SUBTEXT, getSubTextModel()));

        add(createContentPanel(ID_CONTENT));

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

        AjaxLink next = new AjaxLink<>(ID_NEXT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onNextPerformed(target);
            }
        };
        next.add(getNextBehaviour());
        next.setOutputMarkupId(true);
        next.setOutputMarkupPlaceholderTag(true);
        add(next);

        Label nextLabel = new Label(ID_NEXT_LABEL, createNextStepLabel());
        next.add(nextLabel);

    }

    protected Component createContentPanel(String id) {
        return new WebMarkupContainer(id);
    }

    protected IModel<?> getTextModel() {
        return Model.of();
    }

    protected IModel<?> getSubTextModel() {
        return Model.of();
    }

    private IModel<?> createNextStepLabel() {
        MarkupContainer parent = getParent();
        if (parent instanceof WizardBorder) {
            WizardPanel panel = ((WizardBorder) parent).getNextPanel();
            return panel.getTitle();
        }
        return Model.of();
    }

    private void onNextPerformed(AjaxRequestTarget target) {
        MarkupContainer parent = getParent();
        if (parent instanceof WizardBorder) {
            ((WizardBorder) parent).nextStep(target);
        }
    }

    protected void onBackPerformed(AjaxRequestTarget target) {
        MarkupContainer parent = getParent();
        if (parent instanceof WizardBorder) {
            WizardBorder wizard = (WizardBorder) parent;
            int index = wizard.getModel().getObject().getActiveStepIndex();
            if (index > 0) {
                wizard.previousStep(target);
                return;
            }
        }
        getPageBase().redirectBack();
    }

    @Override
    public VisibleEnableBehaviour getHeaderBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}
