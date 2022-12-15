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
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.IResource;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.column.RoundedImagePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CatalogTilePanel<T extends Serializable> extends FocusTilePanel<T, CatalogTile<T>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ADD = "add";
    private static final String ID_INFO = "info";
    private static final String ID_CHECK = "check";

    public CatalogTilePanel(String id, IModel<CatalogTile<T>> model) {
        super(id, model);
    }

    protected void initLayout() {
        super.initLayout();

        add(AttributeAppender.append("class", "catalog-tile-panel d-flex flex-column align-items-center bordered p-4"));
        add(AttributeAppender.append("class", () -> getModelObject().isSelected() ? "active" : null));

        RoundedIconPanel check = new RoundedIconPanel(ID_CHECK, () -> "fa fa-check", () -> getModelObject().getCheckState(), () -> getModelObject().getCheckTitle());
        add(check);

        Label info = new Label(ID_INFO);
        info.add(AttributeAppender.append("title", () -> getModelObject().getInfo()));
        info.add(new TooltipBehavior());
        info.add(new VisibleBehaviour(() -> getModelObject().getInfo() != null));
        add(info);

        add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                CatalogTilePanel.this.onClick(target);
            }
        });

        Component add = createAddButton(ID_ADD);
        add(add);
    }

    protected Component createAddButton(String id) {
        return new AjaxLink<>(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                CatalogTilePanel.this.onAdd(target);
            }
        };
    }

    protected void onAdd(AjaxRequestTarget target) {

    }
}
