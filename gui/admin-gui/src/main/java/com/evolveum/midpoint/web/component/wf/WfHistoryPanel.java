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

import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

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

        List<IColumn<WfHistoryEventDto, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn(createStringResource("WfHistoryPanel.label.timestamp"), WfHistoryEventDto.F_TIMESTAMP_FORMATTED));
        columns.add(new PropertyColumn(createStringResource("WfHistoryPanel.label.event"), WfHistoryEventDto.F_EVENT));

        ISortableDataProvider provider = new ListDataProvider(this, getModel());
        add(new TablePanel<>(ID_HISTORY_TABLE, provider, columns));
    }
}
