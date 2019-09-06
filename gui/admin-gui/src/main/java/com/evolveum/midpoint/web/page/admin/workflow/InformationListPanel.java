/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
