/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class ContainerValuePanel extends BasePanel<ContainerValueDto> {

    private static final Trace LOGGER = TraceManager.getTrace(ModificationsPanel.class);

    private static final String ID_ITEM = "item";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_VALUE = "value";

    public ContainerValuePanel(String id, IModel<ContainerValueDto> model) {
        super(id, model);
		initLayout();
    }

    protected void initLayout() {

        add(new ListView<ContainerItemDto>(ID_ITEM, new PropertyModel(getModel(), ContainerValueDto.F_ITEM_LIST)) {
            @Override
            protected void populateItem(ListItem<ContainerItemDto> item) {
                item.add(new Label(ID_ATTRIBUTE, new PropertyModel(item.getModel(), ContainerItemDto.F_ATTRIBUTE)));
                if (item.getModelObject().getValue() instanceof ContainerValueDto) {
                    item.add(new ContainerValuePanel(ID_VALUE, new PropertyModel(item.getModel(), ContainerItemDto.F_VALUE)));
                } else {        // should be String
                    item.add(new Label(ID_VALUE, new PropertyModel(item.getModel(), ContainerItemDto.F_VALUE)));
                }
            }
        });
    }


}
