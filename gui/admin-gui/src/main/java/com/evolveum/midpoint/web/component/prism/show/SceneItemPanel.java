/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
                new PropertyModel<List<SceneItemLineDto>>(model, SceneItemDto.F_LINES)) {

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
