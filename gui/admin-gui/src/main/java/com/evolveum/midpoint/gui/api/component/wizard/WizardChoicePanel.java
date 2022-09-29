/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;

import java.util.ArrayList;
import java.util.List;

public abstract class WizardChoicePanel<T extends TileEnum> extends AbstractWizardBasicPanel {

    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";

    private LoadableModel<List<Tile<T>>> tiles;

    private final Class<T> tileTypeClass;

    /**
    * @param tileTypeClass have to be Enum class
    **/
    public WizardChoicePanel(String id, ResourceDetailsModel resourceModel, Class<T> tileTypeClass) {
        super(id, resourceModel);
        Validate.isAssignableFrom(Enum.class, tileTypeClass);
        this.tileTypeClass = tileTypeClass;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        tiles = initTilesModel();
        initLayout();
    }

    protected LoadableModel<List<Tile<T>>> initTilesModel() {
        return new LoadableModel<>(false) {

            @Override
            protected List<Tile<T>> load() {
                List<Tile<T>> list = new ArrayList<>();

                for (T type : tileTypeClass.getEnumConstants()) {
                    Tile tile = new Tile(type.getIcon(), getString((Enum) type));
                    tile.setValue(type);
                    list.add(tile);
                }
                addDefaultTile(list);

                return list;
            }
        };
    }

    protected void addDefaultTile(List<Tile<T>> list) {
    }

    private void initLayout() {
        ListView<Tile<T>> list = new ListView<>(ID_LIST, tiles) {

            @Override
            protected void populateItem(ListItem<Tile<T>> item) {
                TilePanel tp = new TilePanel(ID_TILE, item.getModel()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        Tile<T> tile = item.getModelObject();
                        onTileClick(tile.getValue(), target);
                    }
                };
                item.add(tp);
            }
        };
        add(list);
    }

    protected abstract void onTileClick(T value, AjaxRequestTarget target);
}
