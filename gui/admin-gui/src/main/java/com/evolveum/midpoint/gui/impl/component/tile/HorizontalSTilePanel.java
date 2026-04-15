/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;

public class HorizontalSTilePanel<T extends Tile<O>, O extends Serializable> extends TilePanel<T, O> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";

    private static final String ID_DESCRIPTION = "description";


    public HorizontalSTilePanel(String id, IModel<T> model) {
        super(id, model);
    }

    protected void initLayout() {
        add(AttributeAppender.append("class", () -> "tile-panel d-flex flex-row vertical align-items-center rounded justify-content-left selectable"));
        add(AttributeAppender.append("class", () -> getModelObject().isSelected() ? "active" : null));
        add(AttributeAppender.append("aria-checked", () -> getModelObject().isSelected() ? "true" : "false"));
        setOutputMarkupId(true);

        Component icon = createIconPanel(ID_ICON);
        add(icon);

        Label title = new Label(ID_TITLE, () -> {
            String str = getModelObject().getTitle();
            return str != null ? getString(str, null, str) : null;
        });
        add(title);

        Label description = new Label(ID_DESCRIPTION, () -> getModelObject().getDescription());
        description.add(AttributeAppender.replace("title", () -> getModelObject().getDescription()));
        description.add(new TooltipBehavior());
        description.add(getDescriptionBehaviour());
        add(description);

        add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                HorizontalSTilePanel.this.onClick(target);
            }
        });
    }
}
