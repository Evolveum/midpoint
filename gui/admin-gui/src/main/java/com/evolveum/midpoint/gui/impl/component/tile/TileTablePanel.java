/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchPanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TileTablePanel<T extends Tile, O extends Serializable> extends BasePanel<O> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TILES_CONTAINER = "tilesContainer";
    private static final String ID_TILES = "tiles";

    private static final String ID_TILES_HEADER = "tilesHeader";
    private static final String ID_TILE = "tile";
    private static final String ID_TABLE = "table";

    private static final String ID_TILES_PAGING = "tilesPaging";

    private IModel<ViewToggle> viewToggleModel;

    private IModel<Search<? extends ObjectType>> searchModel;

    private UserProfileStorage.TableId tableId;

    public TileTablePanel(String id, ISortableDataProvider provider) {
        this(id, provider, List.of(), null, null);
    }

    public TileTablePanel(String id, ISortableDataProvider provider, List<IColumn<O, String>> columns, IModel<ViewToggle> viewToggle, UserProfileStorage.TableId tableId) {
        super(id);

        if (viewToggle == null) {
            viewToggle = Model.of(ViewToggle.TILE);
        }
        this.viewToggleModel = viewToggle;
        this.tableId = tableId;

        initModels();
        initLayout(provider, columns);
    }

    private void initModels() {
        searchModel = createSearchModel();
    }

    public IModel<ViewToggle> getViewToggleModel() {
        return viewToggleModel;
    }

    private void initLayout(ISortableDataProvider<O, String> provider, List<IColumn<O, String>> columns) {
        setOutputMarkupId(true);

        add(createTilesHeader(ID_TILES_HEADER));

        WebMarkupContainer tilesContainer = new WebMarkupContainer(ID_TILES_CONTAINER);
        tilesContainer.add(new VisibleBehaviour(() -> viewToggleModel.getObject() == ViewToggle.TILE));
        add(tilesContainer);

        PageableListView<T, O> tiles = new PageableListView<>(ID_TILES, provider, tableId) {

            @Override
            protected void populateItem(ListItem<T> item) {
                item.add(AttributeAppender.append("class", () -> getTileCssClasses()));

                Component tile = createTile(ID_TILE, item.getModel());
                item.add(tile);
            }

            @Override
            protected T createItem(O object) {
                return createTileObject(object);
            }
        };
        tilesContainer.setOutputMarkupId(true);
        tilesContainer.add(tiles);

        NavigatorPanel tilesPaging = new NavigatorPanel(ID_TILES_PAGING, tiles, true) {

            @Override
            protected String getPaginationCssClass() {
                return null;
            }
        };
        add(tilesPaging);

        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns, tableId) {

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                return TileTablePanel.this.createTableButtonToolbar(id);
            }

            @Override
            protected Component createHeader(String headerId) {
                return createTableHeader(headerId);
            }

            @Override
            protected String getPaginationCssClass() {
                return null;
            }
        };
        table.add(new VisibleBehaviour(() -> viewToggleModel.getObject() == ViewToggle.TABLE));
        add(table);
    }

    public ISortableDataProvider<O, String> getProvider() {
        PageableListView view = (PageableListView) get(ID_TILES_CONTAINER).get(ID_TILES);
        return view.getProvider();
    }

    protected String getTileCssClasses() {
        return null;
    }

    protected Component createTile(String id, IModel<T> model) {
        return new CatalogTilePanel(id, model);
    }

    protected T createTileObject(O object) {
        return null;
    }

    public void refresh(AjaxRequestTarget target) {
        target.add(getPageBase().getFeedbackPanel());

        if (viewToggleModel.getObject() == ViewToggle.TABLE) {
            target.add(get(ID_TABLE));
        } else {
            target.add(get(ID_TILES_CONTAINER), get(ID_TILES_PAGING));
        }
    }

    protected IModel<Search<? extends ObjectType>> createSearchModel() {
        return null;
    }

    protected Component createTilesHeader(String id) {
        return createHeader(id);
    }

    protected Component createTableHeader(String id) {
        return createHeader(id);
    }

    protected Component createHeader(String id) {
        if (searchModel == null) {
            return new WebMarkupContainer(id);
        }

        return new SearchPanel(id, searchModel) {

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                onSearchPerformed(target);
            }
        };
    }

    protected WebMarkupContainer createTableButtonToolbar(String id) {
        return new WebMarkupContainer(id);
    }

    private void onSearchPerformed(AjaxRequestTarget target) {
        refresh(target);
    }
}
