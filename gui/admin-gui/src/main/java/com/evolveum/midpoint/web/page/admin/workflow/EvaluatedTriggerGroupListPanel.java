/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;

public class EvaluatedTriggerGroupListPanel extends BasePanel<List<EvaluatedTriggerGroupDto>> {

    private static final String ID_TRIGGER_GROUP_LIST = "triggerGroupList";
    private static final String ID_TRIGGER_GROUP = "triggerGroup";

    public EvaluatedTriggerGroupListPanel(String id, IModel<List<EvaluatedTriggerGroupDto>> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        ListView<EvaluatedTriggerGroupDto> list = new ListView<EvaluatedTriggerGroupDto>(ID_TRIGGER_GROUP_LIST, getModel()) {
            @Override
            protected void populateItem(ListItem<EvaluatedTriggerGroupDto> item) {
                item.add(new EvaluatedTriggerGroupPanel(ID_TRIGGER_GROUP, item.getModel()));
            }
        };
        add(list);
    }
}
