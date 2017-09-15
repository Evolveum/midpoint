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

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.LocalizableMessageModel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InformationPartType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InformationType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

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
