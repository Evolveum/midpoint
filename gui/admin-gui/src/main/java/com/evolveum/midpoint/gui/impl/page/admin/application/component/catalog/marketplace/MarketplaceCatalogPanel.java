/*
 * ~ Copyright (c) 2025 Evolveum
 * ~
 * ~ This work is dual-licensed under the Apache License 2.0
 * ~ and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.data.provider.ObjectTileProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
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

import java.io.Serial;
import java.util.List;
import java.util.Optional;

public class MarketplaceCatalogPanel extends CatalogPanel<ConnectorType> {
    @Serial private static final long serialVersionUID = -9159471012370264087L;
    private static final Trace LOGGER = TraceManager.getTrace(LocalConnectorCatalogPanel.class);

    public MarketplaceCatalogPanel(String id) {
        super(id);
        setOutputMarkupId(true);
    }

    @Override
    protected WebMarkupContainer createContent(String id) {
        return new TileTablePanel<TemplateTile<ConnectorType>, TemplateTile<ConnectorType>>(id, viewToggleModel, null) {
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
                                        MarketplaceCatalogPanel.this.onAddApplication(target);
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
                        MarketplaceCatalogPanel.this.onAddApplication(target);
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
                // TODO zlobí typy, opavits
                return (IModel<Search>) (Object) MarketplaceCatalogPanel.this.searchModel;
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
                return "p-3";
            }

            @Override
            protected String getTilesContainerAdditionalClass() {
                return "card-body " + super.getTilesFooterCssClasses();
            }

            @Override
            protected Component createHeader(String id) {
                Fragment headerContainer = new Fragment(id, "customHeaderFragment", MarketplaceCatalogPanel.this);
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
                        target.add(MarketplaceCatalogPanel.this);
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
        };
    }

    @Override
    protected Search<ConnectorType> createSearch() {
        return new SearchBuilder<>(ConnectorType.class)
                .modelServiceLocator(getPageBase())
                .setFullTextSearchEnabled(true)
                .build();
    }

    private void onAddApplication(AjaxRequestTarget target) {
        var page = getPageBase();
        ConfirmationPanel dialog = new ConfirmationPanel(
                page.getMainPopupBodyId(),
                createStringResource("PersonOfInterestPanel.confirmShoppingCartDataRecalculation")
        ) {

        };
        page.showMainPopup(dialog, target);
    }
}
