/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

public class EvaluatedTriggerGroupPanel extends BasePanel<EvaluatedTriggerGroupDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TRIGGERS = "triggers";
    private static final String ID_TRIGGER = "trigger";

    public EvaluatedTriggerGroupPanel(String id, IModel<EvaluatedTriggerGroupDto> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        ListView<EvaluatedTriggerDto> list = new ListView<>(ID_TRIGGERS, () -> getModelObject().getTriggers()) {

            @Override
            protected void populateItem(ListItem<EvaluatedTriggerDto> item) {
                item.add(new EvaluatedTriggerPanel(ID_TRIGGER, item.getModel()));
            }
        };
        add(list);
    }
}
