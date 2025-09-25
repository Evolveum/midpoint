/*
 * ~ Copyright (c) 2025 Evolveum
 * ~
 * ~ This work is dual-licensed under the Apache License 2.0
 * ~ and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.List;

public class MarketplaceCatalogPanel extends CatalogPanel<ConnectorType, TemplateTile<ConnectorType>> {
    @Serial private static final long serialVersionUID = -9159471012370264087L;
    private static final Trace LOGGER = TraceManager.getTrace(MarketplaceCatalogPanel.class);

    public MarketplaceCatalogPanel(String id) {
        super(id);
        setOutputMarkupId(true);
    }

    @Override
    public void onActionClick(AjaxRequestTarget target) {

    }

    @Override
    protected Component createTile(String id, IModel<TemplateTile<ConnectorType>> itemModel) {
        return new ConnectorTilePanel<>(id, itemModel) {
            @Override
            public void onLinkClick(AjaxRequestTarget target) {
                super.onLinkClick(target);
            }

            @Override
            public void onActionClick(AjaxRequestTarget target) {
                MarketplaceCatalogPanel.this.onActionClick(target);
            }
        };
    }

    @Override
    protected List<IColumn<SelectableBean<ConnectorType>, String>> createColumns() {
        return List.of(
                new PropertyColumn(createStringResource("LocalConnectorTilePanel.name"), "title") {
                    @Override
                    public void populateItem(Item item, String componentId, IModel rowModel) {
                        super.populateItem(item, componentId, rowModel);
                    }
                },
                new AbstractColumn<>(null) {
                    @Override
                    public void populateItem(Item item, String id, IModel model) {
                        item.add(new AjaxLinkPanel(id, createStringResource("LocalConnectorTilePanel.addApplication")) {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                MarketplaceCatalogPanel.this.onActionClick(target);
                            }
                        });
                    }
                }
        );
    }

    @Override
    protected TemplateTile<ConnectorType> createTileObject(SelectableBean<ConnectorType> object) {
        return null;
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
