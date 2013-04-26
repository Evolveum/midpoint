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

package com.evolveum.midpoint.web.component.wf.history;

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class WfHistoryPanel extends SimplePanel<List<WfHistoryEventDto>> {

    private static final String ID_HISTORY_TABLE = "historyTable";

    public WfHistoryPanel(String id, IModel<List<WfHistoryEventDto>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        List<IColumn<WorkItemDto, String>> columns = new ArrayList<IColumn<WorkItemDto, String>>();
        columns.add(new PropertyColumn(createStringResource("WfHistoryPanel.label.timestamp"), WfHistoryEventDto.F_TIMESTAMP_FORMATTED));
        columns.add(new PropertyColumn(createStringResource("WfHistoryPanel.label.event"), WfHistoryEventDto.F_EVENT));

        ISortableDataProvider provider = new ListDataProvider(this, getModel());
        add(new TablePanel<WorkItemDto>(ID_HISTORY_TABLE, provider, columns));
    }
}
