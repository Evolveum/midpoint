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
package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.QueryBasedHandlerDto;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class QueryBasedHandlerPanel<D extends QueryBasedHandlerDto> extends BasePanel<D> {
	private static final long serialVersionUID = 1L;

	private static final String ID_OBJECT_TYPE_CONTAINER = "objectTypeContainer";
	private static final String ID_OBJECT_TYPE = "objectType";
	private static final String ID_OBJECT_QUERY_CONTAINER = "objectQueryContainer";
	private static final String ID_OBJECT_QUERY = "objectQuery";

	public QueryBasedHandlerPanel(String id, IModel<D> model) {
		super(id, model);
		initLayout();
		setOutputMarkupId(true);
	}
	
	private void initLayout() {
		WebMarkupContainer objectTypeContainer = new WebMarkupContainer(ID_OBJECT_TYPE_CONTAINER);
		Label objectType = new Label(ID_OBJECT_TYPE, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				final String key = getModelObject().getObjectTypeKey();
				return key != null ? getString(key) : null;
			}
		});
		objectTypeContainer.add(objectType);
		add(objectTypeContainer);

		WebMarkupContainer objectQueryContainer = new WebMarkupContainer(ID_OBJECT_QUERY_CONTAINER);
		TextArea objectQuery = new TextArea<>(ID_OBJECT_QUERY, new PropertyModel<>(getModel(), QueryBasedHandlerDto.F_OBJECT_QUERY));
		objectQuery.setEnabled(false);
		objectQueryContainer.add(objectQuery);
		add(objectQueryContainer);
	}

}
