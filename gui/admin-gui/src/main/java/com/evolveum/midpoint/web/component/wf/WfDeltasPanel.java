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

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.delta.DeltaPanel;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import java.util.List;

/**
 * @author mederly
 */
@Deprecated
public class WfDeltasPanel extends SimplePanel<TaskDto> {

    private static final String ID_DELTA_IN_LIST = "deltaInList";
    private static final String ID_DELTA_OUT_LIST = "deltaOutList";
    private static final String ID_DELTA_OUT_LIST_EMPTY = "deltaOutListEmpty";
    private static final String ID_DELTA_IN = "deltaIn";
    private static final String ID_DELTA_OUT = "deltaOut";

    public WfDeltasPanel(String id, IModel<TaskDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        add(new ListView<SceneDto>(ID_DELTA_IN_LIST, new PropertyModel<List<SceneDto>>(getModel(), TaskDto.F_WORKFLOW_DELTAS_IN)) {
            @Override
            protected void populateItem(ListItem<SceneDto> item) {
                item.add(new ScenePanel(ID_DELTA_IN, item.getModel()));
            }
        });

        ListView<SceneDto> deltaOutListView = new ListView<SceneDto>(ID_DELTA_OUT_LIST, new PropertyModel<List<SceneDto>>(getModel(), TaskDto.F_WORKFLOW_DELTAS_OUT)) {
            @Override
            protected void populateItem(ListItem<SceneDto> item) {
                item.add(new ScenePanel(ID_DELTA_OUT, item.getModel()));
            }
        };
        deltaOutListView.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !getModel().getObject().getWorkflowDeltasOut().isEmpty();
            }
        });
        add(deltaOutListView);

        Label deltaOutListEmpty = new Label(ID_DELTA_OUT_LIST_EMPTY, new ResourceModel("WfDeltasPanel.label.deltaOutListEmpty"));
        deltaOutListEmpty.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModel().getObject().getWorkflowDeltasOut().isEmpty();
            }
        });
        add(deltaOutListEmpty);

    }
}
