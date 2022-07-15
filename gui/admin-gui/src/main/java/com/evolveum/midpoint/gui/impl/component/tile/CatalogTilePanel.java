/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CatalogTilePanel<T extends Serializable> extends BasePanel<CatalogTile<T>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_LOGO = "logo";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ADD = "add";
    private static final String ID_DETAILS = "details";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";

    public CatalogTilePanel(String id, IModel<CatalogTile<T>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "catalog-tile-panel d-flex flex-column align-items-center bordered p-4"));
        add(AttributeAppender.append("class", () -> getModelObject().isSelected() ? "active" : null));
        setOutputMarkupId(true);

        WebMarkupContainer logo = new WebMarkupContainer(ID_LOGO);
        logo.add(AttributeAppender.append("class", () -> getModelObject().getLogo()));
        add(logo);

        Label description = new Label(ID_DESCRIPTION, () -> getModelObject().getDescription());
        add(description);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getModelObject().getIcon()));
        add(icon);

        add(new Label(ID_TITLE, () -> {
            String title = getModelObject().getTitle();
            return title != null ? getString(title, null, title) : null;
        }));

        add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                CatalogTilePanel.this.onClick(target);
            }
        });

        Component add = createAddButton(ID_ADD);
        add(add);

        Component details = createDetailsButton(ID_DETAILS);
        add(details);
    }

    protected Component createAddButton(String id) {
        return new AjaxLink<>(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                CatalogTilePanel.this.onAdd(target);
            }
        };
    }

    protected Component createDetailsButton(String id) {
        return new AjaxLink<>(id) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                CatalogTilePanel.this.onDetails(target);
            }
        };
    }

    protected void onAdd(AjaxRequestTarget target) {

    }

    protected void onDetails(AjaxRequestTarget target) {

    }

    protected void onClick(AjaxRequestTarget target) {
        getModelObject().toggle();
        target.add(this);
    }
}
