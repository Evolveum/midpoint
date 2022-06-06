/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TileTablePanel<T extends Serializable> extends BasePanel<T> {

    private static final String ID_TILES_CONTAINER = "tilesContainer";
    private static final String ID_TILES = "tiles";

    private static final String ID_TILES_HEADER = "tilesHeader";
    private static final String ID_TILE = "tile";
    private static final String ID_TABLE = "table";

    private static final String ID_TILES_PAGING = "tilesPaging";

    private IModel<ViewToggle> viewToggleModel = Model.of(ViewToggle.TILE);

    public TileTablePanel(String id, ISortableDataProvider provider, List<IColumn> columns) {
        super(id);

        initLayout(provider, columns);
    }

    public IModel<ViewToggle> getViewToggleModel() {
        return viewToggleModel;
    }

    private void initLayout(ISortableDataProvider provider, List<IColumn> columns) {
        setOutputMarkupId(true);

        add(createTilesHeader(ID_TILES_HEADER));

        WebMarkupContainer tilesContainer = new WebMarkupContainer(ID_TILES_CONTAINER);
        tilesContainer.add(new VisibleBehaviour(() -> viewToggleModel.getObject() == ViewToggle.TILE));
        add(tilesContainer);

        PageableListView<CatalogTile, SelectableBean<ObjectType>> tiles = new PageableListView<CatalogTile, SelectableBean<ObjectType>>(ID_TILES, provider) {

            @Override
            protected void populateItem(ListItem<CatalogTile> item) {
                CatalogTilePanel tile = new CatalogTilePanel(ID_TILE, item.getModel());
                item.add(tile);
            }

            @Override
            protected CatalogTile createItem(SelectableBean<ObjectType> object) {
                // todo improve
                CatalogTile t = new CatalogTile("fas fa-building", WebComponentUtil.getName(object.getValue()));
                t.setLogo("fas fa-utensils fa-2x");
                t.setDescription(object.getValue().getDescription());

                return t;
            }
        };
        tilesContainer.setOutputMarkupId(true);
        tilesContainer.add(tiles);

        NavigatorPanel tilesPaging = new NavigatorPanel(ID_TILES_PAGING, tiles, true);
        add(tilesPaging);

        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns) {

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                return TileTablePanel.this.createTableButtonToolbar(id);
            }
        };
        table.add(new VisibleBehaviour(() -> viewToggleModel.getObject() == ViewToggle.TABLE));
        add(table);
    }

    protected WebMarkupContainer createTilesHeader(String id) {
        return new WebMarkupContainer(id);
    }

    protected WebMarkupContainer createTableButtonToolbar(String id) {
        return new WebMarkupContainer(id);
    }
}
