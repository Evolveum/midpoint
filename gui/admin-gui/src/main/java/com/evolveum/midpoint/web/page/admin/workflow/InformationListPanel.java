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
import com.evolveum.midpoint.xml.ns._public.common.common_3.InformationType;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author mederly
 */
public class InformationListPanel extends BasePanel<List<InformationType>> {

	private static final String ID_INFORMATION_LIST = "informationList";
	private static final String ID_INFORMATION = "information";

	public InformationListPanel(String id, IModel<List<InformationType>> model) {
		super(id, model);
		initLayout();
	}

	private void initLayout() {
		ListView<InformationType> list = new ListView<InformationType>(ID_INFORMATION_LIST, getModel()) {
			@Override
			protected void populateItem(ListItem<InformationType> item) {
				item.add(new InformationPanel(ID_INFORMATION, item.getModel()));
			}
		};
		add(list);
	}
}
