/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.List;

public class SingleTileChoicePanel<T extends Serializable> extends BasePanel<Tile<T>> {

    private static final String ID_TILES = "tiles";
    private static final String ID_TILE = "tile";
    private final IModel<List<Tile<T>>> choices;

    public SingleTileChoicePanel(String id, IModel<Tile<T>> selected, IModel<List<Tile<T>>> choices) {
        super(id, selected);
        this.choices = choices;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ListView<Tile<T>> tiles = new ListView<>(ID_TILES, choices) {
            @Override
            protected void populateItem(ListItem<Tile<T>> item) {
                item.add(createTilePanel(item.getModel()));
            }
        };
        add(tiles);
    }

    private TilePanel<Tile<T>, T> createTilePanel(IModel<Tile<T>> model) {
        TilePanel<Tile<T>, T> tilePanel = new TilePanel<>(ID_TILE, model) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                SingleTileChoicePanel.this.getModel().setObject(model.getObject());
                target.add(SingleTileChoicePanel.this);
            }
        };
        tilePanel.add(AttributeAppender.append("class", () -> SingleTileChoicePanel.this.getModelObject().equals(model.getObject()) ? "active" : null));
        return tilePanel;
    }
}
