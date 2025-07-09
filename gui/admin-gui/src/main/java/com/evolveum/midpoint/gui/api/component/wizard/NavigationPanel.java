/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NavigationPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_BACK = "back";
    private static final String ID_BACK_LABEL = "backLabel";
    private static final String ID_TITLE = "title";
    private static final String ID_CONTENT = "content";
    private static final String ID_NEXT = "next";
    private static final String ID_NEXT_FRAGMENT = "nextFragment";
    private static final String ID_NEXT_LINK = "nextLink";
    private static final String ID_NEXT_LABEL = "nextLabel";

    public NavigationPanel(String id) {
        super(id);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "d-flex align-items-center flex-wrap gap-3 mb-3"));

        AjaxLink back = createBackButton(ID_BACK, createBackTitleModel());
        add(back);
        Component next = createNextButton(ID_NEXT, createNextTitleModel());
        add(next);

        add(new Label(ID_TITLE, createTitleModel()));

        add(createHeaderContent());
    }

    protected Component createHeaderContent() {
        return createHeaderContent(ID_CONTENT);
    }

    protected Component createHeaderContent(String id) {
        return new WebMarkupContainer(id);
    }

    protected IModel<String> createNextTitleModel() {
        return () -> null;
    }

    protected IModel<String> createBackTitleModel() {
        return createStringResource("NavigationPanel.back");
    }

    protected IModel<String> createTitleModel() {
        return () -> null;
    }

    protected Component createNextButton(String id, IModel<String> nextTitle) {
        Fragment next = new Fragment(id, ID_NEXT_FRAGMENT, this);
        next.setRenderBodyOnly(true);

        AjaxLink nextLink = new AjaxLink<>(ID_NEXT_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onNextPerformed(target);
            }
        };
        nextLink.add(AttributeAppender.append("class", "btn btn-success"));
        nextLink.setOutputMarkupId(true);
        nextLink.setOutputMarkupPlaceholderTag(true);
        nextLink.add(new BehaviourDelegator(() -> getNextVisibilityBehaviour()));
        WebComponentUtil.addDisabledClassBehavior(nextLink);

        nextLink.add(new Label(ID_NEXT_LABEL, nextTitle));
        next.add(nextLink);

        return next;
    }

    @NotNull
    protected VisibleEnableBehaviour getNextVisibilityBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    protected AjaxLink createBackButton(String id, IModel<String> backTitle) {
        AjaxLink back = new AjaxLink<>(id) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onBackPerformed(target);
            }
        };
        back.setOutputMarkupId(true);
        back.setOutputMarkupPlaceholderTag(true);
        WebComponentUtil.addDisabledClassBehavior(back);
        back.add(new Label(ID_BACK_LABEL, backTitle));
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
