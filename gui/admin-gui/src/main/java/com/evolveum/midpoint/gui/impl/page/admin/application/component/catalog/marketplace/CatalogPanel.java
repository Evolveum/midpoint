/*
 * ~ Copyright (c) 2025 Evolveum
 * ~
 * ~ This work is dual-licensed under the Apache License 2.0
 * ~ and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.data.provider.ObjectTileProvider;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;

public abstract class CatalogPanel<O extends Serializable, T extends Tile<O>> extends BasePanel<T> {
    @Serial private static final long serialVersionUID = -9159471012370264087L;

    protected IModel<Search<T>> searchModel;
    protected IModel<ViewToggle> viewToggleModel;

    public CatalogPanel(String id) {
        super(id);
    }

    abstract public void onActionClick(AjaxRequestTarget target);

    protected abstract Search<T> createSearch();

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Search<T> search = createSearch();
        searchModel = Model.of(search);
        viewToggleModel = Model.of(ViewToggle.TILE);

        // TODO jak zjistit count
        add(new Label("searchState", Model.of(createStringResource("IntegrationCatalog.counterStatus", 0))));
        add(new WebMarkupContainer("viewToggle"));
        add(createContent("tilesTable"));
    }

    protected TileTablePanel<T, O> createContent(String id) {
        return new TileTablePanel<T, O>(id, viewToggleModel, null) {
            @Override
            protected void onInitialize() {
                super.onInitialize();
                getTable().setShowAsCard(false);
            }

            @Override
            protected List<IColumn<TemplateTile<ConnectorType>, String>> createColumns() {
                return List.of(
                        new IconColumn<TemplateTile<ConnectorType>>(Model.of()) {
                            @Override
                            protected DisplayType getIconDisplayType(IModel<TemplateTile<ConnectorType>> rowModel) {
                                return new DisplayType().beginIcon().cssClass("fa fa-microchip").end();
                            }
                        },
                        new PropertyColumn(createStringResource("LocalConnectorTilePanel.name"), "title") {
                            @Override
                            public void populateItem(Item item, String componentId, IModel rowModel) {
                                super.populateItem(item, componentId, rowModel);
                            }
                        },
                        new AbstractColumn<>(null) {
                            @Override
                            public void populateItem(Item item, String id, IModel model) {
                                item.add(new AjaxLinkPanel(id, createStringResource("LocalConnectorTilePanel.moreDetails")) {
                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        // More Details
                                    }
                                });
                            }
                        },
                        new AbstractColumn<>(null) {
                            @Override
                            public void populateItem(Item item, String id, IModel model) {
                                item.add(new AjaxLinkPanel(id, createStringResource("LocalConnectorTilePanel.addApplication")) {
                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        CatalogPanel.this.onActionClick(target);
                                    }
                                });
                            }
                        }
                );
            }

            @Override
            protected TemplateTile<ConnectorType> createTileObject(TemplateTile<ConnectorType> object) {
                if (object != null && object.getValue() != null) {
                    object.setIcon(GuiStyleConstants.CLASS_OBJECT_CONNECTOR_ICON);
                    object.setDescription("Here goes a compact, informative summary outlining the integration application’s main features...");
                }
                return object;
            }

            @Override
            protected Component createTile(String id, IModel<TemplateTile<ConnectorType>> model) {
                return new ConnectorTilePanel<>(id, model) {
                    @Override
                    public void onLinkClick(AjaxRequestTarget target) {
                        super.onLinkClick(target);
                    }

                    @Override
                    public void onActionClick(AjaxRequestTarget target) {
                        super.onActionClick(target);
                        CatalogPanel.this.onTileAction(target);
                    }
                };
            }

            @Override
            protected ISortableDataProvider<TemplateTile<String>, String> createProvider() {
                var searchModel = getSearchModel();
                return new ObjectTileProvider(this, searchModel);
            }

            @Override
            protected IModel<Search> createSearchModel() {
                return (Search<>) LocalConnectorCatalogPanel.super.searchModel;
            }

            @Override
            protected String getTileContainerCssClass() {
                return "d-flex flex-wrap justify-content-left py-4 m-n2";
            }

            @Override
            protected String getTileCssClasses() {
                return "col-12 col-md-6 col-xl-4 col-xxl-3 p-2";
            }

            @Override
            protected String getTileCssStyle() {
                return "min-height: 23.75rem";
            }

            @Override
            protected String getTilesHeaderCssClasses() {
                return isTileViewVisible() ? "pt-3 pr-3 pb-0 pl-3 tiles-view" : "pb-3 table-view";
            }

            @Override
            protected String getTilesContainerAdditionalClass() {
                return "card-body " + super.getTilesFooterCssClasses();
            }

            @Override
            protected Component createHeader(String id) {
                Fragment headerContainer = new Fragment(id, "customHeaderFragment", CatalogPanel.this);
                SearchBoxPanel searchBoxPanel = new SearchBoxPanel("searchBoxPanel", createStringResource("SearchBoxPanel.placeholder")) {
                    @Override
                    protected void onSearch(AjaxRequestTarget target, String query) {
                        Optional.ofNullable(getSearchModel())
                                .map(IModel::getObject)
                                .ifPresent((search) -> {
                                    search.setFullText(query);
                                    onSearchPerformed(target);
                                });
                    }
                };
                IModel<String> counterModel = IModel.of(() -> {
                    long count = getProvider().size();
                    return createStringResource("IntegrationCatalog.counterStatus", count).getString();
                });

                Label counterLabel = new Label("counter", counterModel);
                counterLabel.setOutputMarkupId(true);

                ViewTogglePanel viewToggle = new ViewTogglePanel("viewToggle", getViewToggleModel()) {
                    @Override
                    protected void onToggleChanged(AjaxRequestTarget target) {
                        target.add(CatalogPanel.this);
                    }
                };

                headerContainer.add(searchBoxPanel);
                headerContainer.add(counterLabel);
                headerContainer.add(viewToggle);

                return headerContainer;
            }

            private void onSearchPerformed(AjaxRequestTarget target) {
                if (isTableVisible()) {
                    target.add(get("table:tableContainer"));
                    getTable().refreshSearch();
                } else {
                    target.add(get("tileView:tilesContainer"));
                }

                Component header = get("tileView:header");
                if (header != null) {
                    Component counter = header.get("counter");
                    if (counter != null) {
                        target.add(counter);
                    }
                }
            }
        };;
    }
}
