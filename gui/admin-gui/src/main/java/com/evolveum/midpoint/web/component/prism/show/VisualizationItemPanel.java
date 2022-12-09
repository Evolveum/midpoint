/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 */
public class VisualizationItemPanel extends Panel {

    private static final String ID_ITEM_LINES = "itemLines";
    private static final String ID_ITEM_LINE = "itemLine";

    private static final Trace LOGGER = TraceManager.getTrace(VisualizationItemPanel.class);

    private boolean showHeader = true;

    public VisualizationItemPanel(String id, IModel<VisualizationItemDto> model) {
        super(id);

        initLayout(model);
    }

    private void initLayout(final IModel<VisualizationItemDto> model) {
        ListView<VisualizationItemLineDto> items = new ListView<VisualizationItemLineDto>(ID_ITEM_LINES,
                new PropertyModel<>(model, VisualizationItemDto.F_LINES)) {

            @Override
            protected void populateItem(ListItem<VisualizationItemLineDto> item) {
                VisualizationItemLinePanel panel = new VisualizationItemLinePanel(ID_ITEM_LINE, item.getModel());
                item.add(panel);
            }
        };
        items.setReuseItems(true);
        add(items);
    }

}
