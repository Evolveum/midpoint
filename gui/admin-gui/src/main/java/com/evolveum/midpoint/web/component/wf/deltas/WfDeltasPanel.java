/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.wf.deltas;

import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.delta.DeltaPanel;
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

        add(new ListView<DeltaDto>(ID_DELTA_IN_LIST, new PropertyModel<List<DeltaDto>>(getModel(), TaskDto.F_WORKFLOW_DELTAS_IN)) {
            @Override
            protected void populateItem(ListItem<DeltaDto> item) {
                item.add(new DeltaPanel(ID_DELTA_IN, item.getModel()));
            }
        });

        ListView<DeltaDto> deltaOutListView = new ListView<DeltaDto>(ID_DELTA_OUT_LIST, new PropertyModel<List<DeltaDto>>(getModel(), TaskDto.F_WORKFLOW_DELTAS_OUT)) {
            @Override
            protected void populateItem(ListItem<DeltaDto> item) {
                item.add(new DeltaPanel(ID_DELTA_OUT, item.getModel()));
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
