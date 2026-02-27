/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component;

import java.io.Serial;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;

public class HelpTextPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTENT = "content";
    private static final String ID_MORE = "more";

    private boolean alwaysShowAll = false;

    private IModel<Boolean> showAll;

    public HelpTextPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        Label content = new Label(ID_CONTENT, getModel());
        content.setRenderBodyOnly(true);
        add(content);

        AjaxButton more = new AjaxButton(ID_MORE, createMoreModel()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onMoreClicked(target);
            }
        };
        add(more);
    }

    private void onMoreClicked(AjaxRequestTarget target) {
        // todo implement
    }

    private IModel<String> createMoreModel() {
        return () -> {
            return "";
        };
    }

    private IModel<String> getContentModel() {
        return () -> {
            String content = getModelObject();
            if (content == null) {
                return null;
            }

            return null;
        };
    }
}
