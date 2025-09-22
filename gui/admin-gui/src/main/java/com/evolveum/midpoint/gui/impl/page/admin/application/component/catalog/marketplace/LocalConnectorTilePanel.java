/*
 * ~ Copyright (c) 2025 Evolveum
 * ~
 * ~ This work is dual-licensed under the Apache License 2.0
 * ~ and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace;

import com.evolveum.midpoint.gui.impl.component.tile.TemplateTilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

public class LocalConnectorTilePanel extends TemplateTilePanel<ConnectorType, TemplateTile<ConnectorType>> {
    private static final Trace LOGGER = TraceManager.getTrace(LocalConnectorTilePanel.class);

    public LocalConnectorTilePanel(String id, IModel<TemplateTile<ConnectorType>> model) {
        super(id, model);
    }

    public void onMoreDetailsClick(AjaxRequestTarget target) {
        LOGGER.trace("More Details button clicked: " + getPath());
    }

    public void onAddApplicationClick(AjaxRequestTarget target) {
        LOGGER.trace("Add Application button clicked: " + getPath());
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(AttributeAppender.replace("class", "d-flex flex-column align-items-start gap-3 p-4 m-0 shadow-sm border-0 rounded-xl h-100 bg-light"));
        add(new AjaxLink<Void>("moreDetails") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                LocalConnectorTilePanel.this.onMoreDetailsClick(target);
            }
        });
        add(new AjaxLink<Void>("addApplication") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                LocalConnectorTilePanel.this.onAddApplicationClick(target);
            }
        });
    }


}
