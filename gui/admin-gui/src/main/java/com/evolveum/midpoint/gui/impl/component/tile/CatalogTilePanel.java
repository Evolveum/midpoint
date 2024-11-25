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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CatalogTilePanel<T extends Serializable> extends ObjectTilePanel<T, CatalogTile<T>> {

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
        check.setOutputMarkupId(true);
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
        addAriaDescribedByForButton(add);
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

    @Override
    protected void addAriaDescribedByForButton(Component details) {
        super.addAriaDescribedByForButton(details);

        details.add(AttributeAppender.append(
                "aria-describedby",
                () -> RoundedIconPanel.State.NONE != getModelObject().getCheckState() ? CatalogTilePanel.this.get(ID_CHECK).getMarkupId() : null));
    }
}
