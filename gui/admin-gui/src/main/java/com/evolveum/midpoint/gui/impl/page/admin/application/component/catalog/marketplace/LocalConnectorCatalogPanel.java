/*
 * ~ Copyright (c) 2025 Evolveum
 * ~
 * ~ This work is dual-licensed under the Apache License 2.0
 * ~ and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.*;

public class LocalConnectorCatalogPanel extends CatalogPanel<ConnectorType, TemplateTile<ConnectorType>> {
    @Serial private static final long serialVersionUID = 8850627686387495224L;
    private static final Trace LOGGER = TraceManager.getTrace(LocalConnectorCatalogPanel.class);

    public LocalConnectorCatalogPanel(String id) {
        super(id);
    }

    @Override
    public void onActionClick(AjaxRequestTarget target) {
        LOGGER.debug("Click");
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
                LocalConnectorCatalogPanel.this.onActionClick(target);
            }
        };
    }

    @Override
    protected TemplateTile<ConnectorType> createTileObject(SelectableBean<ConnectorType> object) {
        String icon = GuiStyleConstants.CLASS_OBJECT_CONNECTOR_ICON;
        String title = getTitle(object);
        String description = getDescription(object);
        List<DisplayType> tags = getTags(object);

        return new TemplateTile<>(icon, title, object.getValue())
                .description(description)
                .addTags(tags);
    }


    @Override
    protected Search<ConnectorType> createSearch() {
        return new SearchBuilder<>(ConnectorType.class)
                .modelServiceLocator(getPageBase())
                .setFullTextSearchEnabled(true)
                .build();
    }

    @Override
    protected List<IColumn<SelectableBean<ConnectorType>, String>> createColumns() {
        return List.of(
                new LambdaColumn<>(createStringResource("LocalConnectorTilePanel.name"), this::getTitle),
                new LambdaColumn<>(createStringResource("LocalConnectorTilePanel.description"), this::getDescription),
                new AbstractColumn<>(createStringResource("LocalConnectorTilePanel.action")) {
                    @Override
                    public void populateItem(Item item, String id, IModel model) {
                        item.add(new AjaxLinkPanel(id, createStringResource("LocalConnectorTilePanel.addApplication")) {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                LocalConnectorCatalogPanel.this.onActionClick(target);
                            }
                        });
                    }
                }
        );
    }

    private String getTitle(SelectableBean<ConnectorType> value) {
        return Optional.ofNullable(value.getValue().getName())
                .or(() -> Optional.ofNullable(value.getValue().getDisplayName()))
                .map(PolyStringType::getOrig)
                .orElse(null);
    }

    private String getDescription(SelectableBean<ConnectorType> value) {
        return value.getValue().getDescription();
    }

    /* TODO handle tags */
    private List<DisplayType> getTags(SelectableBean<ConnectorType> value) {
        return List.of();
    }
}
