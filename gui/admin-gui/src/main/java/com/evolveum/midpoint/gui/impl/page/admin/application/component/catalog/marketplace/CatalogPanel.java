/*
 * ~ Copyright (c) 2025 Evolveum
 * ~
 * ~ This work is dual-licensed under the Apache License 2.0
 * ~ and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.data.provider.ObjectDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;

import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;

public abstract class CatalogPanel<O extends ObjectType, T extends TemplateTile<O>> extends BasePanel<T> {
    @Serial private static final long serialVersionUID = -9159471012370264087L;

    private IModel<Search<O>> searchModel;
    private IModel<ViewToggle> viewToggleModel;
    private Component results;
    private Component counter;
    private ISortableDataProvider<SelectableBean<O>, String> provider;

    public CatalogPanel(String id) {
        super(id);
    }

    abstract protected void onActionClick(AjaxRequestTarget target);
    abstract protected Component createTile(String id, IModel<T> itemModel);
    abstract protected List<IColumn<SelectableBean<O>, String>> createColumns();
    abstract protected Search<O> createSearch();
    abstract protected T createTileObject(SelectableBean<O> object);

    @Override
    protected void onInitialize() {
        super.onInitialize();

        searchModel = LambdaModel.of(this::createSearch);
        viewToggleModel = Model.of(ViewToggle.TILE);
        provider = new ObjectDataProvider<>(this, searchModel);

        add(new SearchBoxPanel("searchBox") {
            @Override
            protected void onSearch(AjaxRequestTarget target, String query) {
                searchModel.getObject().setFullText(query);
                target.add(results, counter);
            }
        });

        IModel<Long> countModel = LambdaModel.of(provider::size);
        counter = new Label("counter", createStringResource("IntegrationCatalog.counterStatus", countModel));
        counter.setOutputMarkupId(true);
        add(counter);

        results = createResultsPanel();
        results.setOutputMarkupId(true);
        add(results);

        add(new ViewTogglePanel("viewToggle", viewToggleModel) {
            @Override
            protected void onToggleChanged(AjaxRequestTarget target) {
                Component newResults = createResultsPanel();
                results.replaceWith(newResults);
                results = newResults;
                target.add(results);
            }
        });
    }

    private Component createTilesPanel() {
        return new PageableListView<T, SelectableBean<O>>("tiles", provider, null) {

            @Override
            protected void populateItem(ListItem<T> item) {
                Component tile = createTile("tile", item.getModel());
                item.add(tile);
            }

            @Override
            protected List<T> createItem(SelectableBean<O> object) {
                return List.of(createTileObject(object));
            }
        };
    }

    private Component createTablePanel() {
        return new BoxedTablePanel<>("table", provider, createColumns(), null).getDataTable();
    }

    private Component createResultsPanel() {
        final Fragment resultsFragment;

        if (viewToggleModel.getObject() == ViewToggle.TILE) {
            resultsFragment = new Fragment("results", "tilesViewFragment", this);
            resultsFragment.add(createTilesPanel());
        } else {
            resultsFragment = new Fragment("results", "tableViewFragment", this);
            resultsFragment.add(createTablePanel());
        }

        return resultsFragment;
    }

}
