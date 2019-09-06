/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author lazyman
 */
public class SceneItemPanel extends Panel {

    private static final String ID_ITEM_LINES = "itemLines";
    private static final String ID_ITEM_LINE = "itemLine";

    private static final Trace LOGGER = TraceManager.getTrace(SceneItemPanel.class);

    private boolean showHeader = true;

    public SceneItemPanel(String id, IModel<SceneItemDto> model) {
        super(id);

        initLayout(model);
    }

    private void initLayout(final IModel<SceneItemDto> model) {
		ListView<SceneItemLineDto> items = new ListView<SceneItemLineDto>(ID_ITEM_LINES,
            new PropertyModel<>(model, SceneItemDto.F_LINES)) {

			@Override
			protected void populateItem(ListItem<SceneItemLineDto> item) {
				SceneItemLinePanel panel = new SceneItemLinePanel(ID_ITEM_LINE, item.getModel());
				item.add(panel);
			}
		};
        items.setReuseItems(true);
        add(items);
    }

}
