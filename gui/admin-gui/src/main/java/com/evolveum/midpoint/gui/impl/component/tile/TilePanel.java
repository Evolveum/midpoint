/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TilePanel<T extends Tile<O>, O extends Serializable> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";

    private static final String ID_DESCRIPTION = "description";

    private boolean horizontal = true;

    public TilePanel(String id, IModel<T> model) {
        super(id, model);

        initLayout();
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }

    private void initLayout() {
        add(AttributeAppender.append("class", () -> horizontal ?
                "tile-panel d-flex flex-column align-items-center rounded p-3 justify-content-center" :
                "tile-panel d-flex flex-row vertical align-items-center rounded justify-content-left"));
        add(AttributeAppender.append("class", () -> getModelObject().isSelected() ? "active" : null));
        add(AttributeAppender.append("aria-checked", () -> getModelObject().isSelected() ? "true" : "false"));
        setOutputMarkupId(true);

        Component icon = createIconPanel(ID_ICON);
        add(icon);

        Label title = new Label(ID_TITLE, () -> {
            String str = getModelObject().getTitle();
            return str != null ? getString(str, null, str) : null;
        });
        title.add(AttributeAppender.append("class", () ->  horizontal ?
                "mt-4 text-center" :
                "ml-2"));
        add(title);

        Label description = new Label(ID_DESCRIPTION, () -> getModelObject().getDescription());
        description.add(AttributeAppender.replace("title", () -> getModelObject().getDescription()));
        description.add(new TooltipBehavior());
        description.add(getDescriptionBehaviour());
        add(description);

        add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                TilePanel.this.onClick(target);
            }
        });
    }

    protected VisibleEnableBehaviour getDescriptionBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    protected Component createIconPanel(String idIcon) {
        WebMarkupContainer icon = new WebMarkupContainer(idIcon);
        icon.add(AttributeAppender.append("class", () -> getModelObject().getIcon()));
        return icon;
    }

    protected void onClick(AjaxRequestTarget target) {
        getModelObject().toggle();
        target.add(this);
    }
}
