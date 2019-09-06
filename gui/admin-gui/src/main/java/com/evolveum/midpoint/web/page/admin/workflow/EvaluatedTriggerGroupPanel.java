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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class EvaluatedTriggerGroupPanel extends BasePanel<EvaluatedTriggerGroupDto> {

	private static final String ID_TRIGGERS = "triggers";
	private static final String ID_TRIGGER = "trigger";

	public EvaluatedTriggerGroupPanel(String id, IModel<EvaluatedTriggerGroupDto> model) {
		super(id, model);
		initLayout();
	}

	private void initLayout() {
		ListView<EvaluatedTriggerDto> list = new ListView<EvaluatedTriggerDto>(ID_TRIGGERS,
				new PropertyModel<>(getModel(), EvaluatedTriggerGroupDto.F_TRIGGERS)) {
			@Override
			protected void populateItem(ListItem<EvaluatedTriggerDto> item) {
				EvaluatedTriggerDto trigger = item.getModelObject();
				item.add(new EvaluatedTriggerPanel(ID_TRIGGER, Model.of(trigger)));
			}
		};
		add(list);
	}
}
