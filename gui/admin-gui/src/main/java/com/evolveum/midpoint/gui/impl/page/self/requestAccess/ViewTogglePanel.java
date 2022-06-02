/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ViewTogglePanel extends BasePanel<ViewToggle> {

    private static final long serialVersionUID = 1L;

    private static final String ID_AS_LIST = "asList";
    private static final String ID_AS_TILE = "asTile";

    public ViewTogglePanel(String id, IModel<ViewToggle> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", () -> "btn-group"));
        setOutputMarkupId(true);

        AjaxLink asList = new AjaxLink<>(ID_AS_LIST) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onTogglePerformed(target, ViewToggle.TABLE);
            }
        };
        asList.add(AttributeAppender.append("class", () -> getModelObject() == ViewToggle.TABLE ? "active" : null));

        add(asList);

        AjaxLink asTile = new AjaxLink<>(ID_AS_TILE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onTogglePerformed(target, ViewToggle.TILE);
            }
        };
        asTile.add(AttributeAppender.append("class", () -> getModelObject() == ViewToggle.TILE ? "active" : null));
        add(asTile);
    }

    protected void onTogglePerformed(AjaxRequestTarget target, ViewToggle newState) {
        ViewToggle current = getModelObject();
        if (current == newState) {
            return;
        }

        getModel().setObject(newState);
        target.add(this);
    }
}
