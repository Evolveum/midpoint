/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.wizard.Badge;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.gui.impl.component.search.SearchPanel;

import com.evolveum.midpoint.web.session.RoleCatalogStorage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleCatalogPanel extends BasePanel implements WizardPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_VIEW_TOGGLE = "viewToggle";
    private static final String ID_MENU = "menu";
    private static final String ID_TILES_CONTAINER = "tilesContainer";
    private static final String ID_TILES = "tiles";

    private static final String ID_TILES_SEARCH = "tilesSearch";
    private static final String ID_TILE = "tile";
    private static final String ID_TABLE = "table";
    private static final String ID_TABLE_FOOTER_FRAGMENT = "tableFooterFragment";
    private static final String ID_ADD_SELECTED = "addSelected";
    private static final String ID_ADD_ALL = "addAll";

    private IModel<ViewToggle> viewToggleModel = Model.of(ViewToggle.TILE);

    public RoleCatalogPanel(String id) {
        super(id);

        initLayout();
    }

    @Override
    public IModel<List<Badge>> getTitleBadges() {
        return Model.ofList(List.of(
                new Badge("badge badge-info", "Requesting for 4 users"),
                new Badge("badge badge-danger", "1 fatal conflict"),
                new Badge("badge badge-danger", "fa fa-exclamation-triangle", "1 fatal conflict")));
    }

    @Override
    public String appendCssToWizard() {
        return "w-100";
    }

    @Override
    public IModel<String> getTitle() {
        return () -> getString("RoleCatalogPanel.title");
    }

    private void initLayout() {
        setOutputMarkupId(true);

        ViewTogglePanel viewToggle = new ViewTogglePanel(ID_VIEW_TOGGLE, viewToggleModel) {

            @Override
            protected void onTogglePerformed(AjaxRequestTarget target, ViewToggle newState) {
                super.onTogglePerformed(target, newState);
                target.add(RoleCatalogPanel.this);
            }
        };
        add(viewToggle);
        DetailsMenuPanel menu = new DetailsMenuPanel(ID_MENU);
        add(menu);

        WebMarkupContainer tilesContainer = new WebMarkupContainer(ID_TILES_CONTAINER);
        tilesContainer.add(new VisibleBehaviour(() -> viewToggleModel.getObject() == ViewToggle.TILE));
        add(tilesContainer);

        IModel<Search> searchModel = new LoadableModel<>(false) {
            @Override
            protected Search load() {
                return SearchFactory.createSearch(RoleType.class, getPageBase());
            }
        };
        SearchPanel tilesSearch = new SearchPanel(ID_TILES_SEARCH, searchModel) {

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                // todo implement
            }
        };
        tilesContainer.add(tilesSearch);

        List<CatalogTile> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CatalogTile t = new CatalogTile("fas fa-building", "Canteen");
            t.setLogo("fas fa-utensils fa-2x");
            t.setDescription("Grants you access to canteen services, coffee bar and vending machines");
            list.add(t);
        }

        IModel<List<CatalogTile>> model = Model.ofList(list);

        ListView<CatalogTile> tiles = new ListView<>(ID_TILES, model) {

            @Override
            protected void populateItem(ListItem<CatalogTile> item) {
                CatalogTilePanel tile = new CatalogTilePanel(ID_TILE, item.getModel());
                item.add(tile);
            }
        };
        tilesContainer.add(tiles);

        ISortableDataProvider provider = new ListDataProvider(this, () -> new ArrayList<>());
        List<IColumn> columns = createColumns();
        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns) {

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, ID_TABLE_FOOTER_FRAGMENT, RoleCatalogPanel.this);
                fragment.add(new AjaxLink<>(ID_ADD_SELECTED) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addSelectedPerformed(target);
                    }
                });

                fragment.add(new AjaxLink<>(ID_ADD_ALL) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        addAllPerformed(target);
                    }
                });

                return fragment;
            }
        };
        table.add(new VisibleBehaviour(() -> viewToggleModel.getObject() == ViewToggle.TABLE));
        add(table);
    }

    private List<IColumn> createColumns() {
        List<IColumn> columns = new ArrayList<>();
        columns.add(new CheckBoxHeaderColumn());
        columns.add(new IconColumn(null) {
            @Override
            protected DisplayType getIconDisplayType(IModel rowModel) {
                return new DisplayType();
            }
        });
        columns.add(new PropertyColumn(createStringResource("ObjectType.name"), "name"));
        columns.add(new PropertyColumn(createStringResource("ObjectType.description"), "name"));
        columns.add(new LinkColumn(createStringResource("RoleCatalogPanel.details")));

        return columns;
    }

    protected void addSelectedPerformed(AjaxRequestTarget target) {

    }

    protected void addAllPerformed(AjaxRequestTarget target) {

    }
}
