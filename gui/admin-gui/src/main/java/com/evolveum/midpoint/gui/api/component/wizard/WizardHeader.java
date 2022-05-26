/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

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

    public WizardHeader(String id, IModel<String> currentPanelTitle, IModel<String> nextPanelTitle) {
        super(id);

        initLayout(currentPanelTitle, nextPanelTitle);
    }

    private void initLayout(IModel<String> currentPanelTitle, IModel<String> nextPanelTitle) {
        add(AttributeAppender.append("class", "d-flex align-items-center flex-wrap gap-2"));

        AjaxLink back = createBackButton(ID_BACK);
        add(back);
        AjaxLink next = createNextButton(ID_NEXT, nextPanelTitle);
        add(next);

        add(new Label(ID_TITLE, currentPanelTitle));

        add(new WebMarkupContainer(ID_CONTENT));

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

        next.add(new Label(ID_NEXT_LABEL, nextPanelTitle));

        return next;
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

        return back;
    }

    protected void onBackPerformed(AjaxRequestTarget target) {

    }

    protected void onNextPerformed(AjaxRequestTarget target) {

    }
}
