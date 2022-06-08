/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.Tile;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.TilePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class ResourceTilePanel<O> extends BasePanel<TemplateTile<O>> {

    private static final long serialVersionUID = 1L;


    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";

    public ResourceTilePanel(String id, IModel<TemplateTile<O>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "selectable-tile flex-grow-1 d-flex flex-column align-items-center bg-white rounded p-3 pb-5 pt-4 h-100"));
        setOutputMarkupId(true);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getModelObject().getIcon()));
        add(icon);

        add(new Label(ID_TITLE, () -> {
            String title = getModelObject().getTitle();
            return title != null ? getString(title, null, title) : null;
        }));

        Label description = new Label(ID_DESCRIPTION, () -> {
            String title = getModelObject().getDescription();
            return title != null ? getString(title, null, title) : null;
        });
        description.add(new VisibleBehaviour(() -> getModelObject().getDescription() != null));

        add(description);

        add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                ResourceTilePanel.this.onClick(target);
            }
        });
    }

    protected void onClick(AjaxRequestTarget target) {
    }
}
