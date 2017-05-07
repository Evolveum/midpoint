/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.web.page.admin.server.handlers.dto.ExecuteChangesHandlerDto;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class ExecuteChangesHandlerPanel extends QueryBasedHandlerPanel<ExecuteChangesHandlerDto> {
	private static final long serialVersionUID = 1L;

	private static final String ID_CHANGE_CONTAINER = "changeContainer";
	private static final String ID_CHANGE = "change";
	private static final String ID_OPTIONS_CONTAINER = "optionsContainer";
	private static final String ID_OPTIONS = "options";

	public ExecuteChangesHandlerPanel(String id, IModel<ExecuteChangesHandlerDto> model) {
		super(id, model);
		initLayout();
		setOutputMarkupId(true);
	}
	
	private void initLayout() {
		WebMarkupContainer changeContainer = new WebMarkupContainer(ID_CHANGE_CONTAINER);
		TextArea change = new TextArea<>(ID_CHANGE, new PropertyModel<>(getModel(), ExecuteChangesHandlerDto.F_OBJECT_DELTA_XML));
		change.setEnabled(false);
		changeContainer.add(change);
		add(changeContainer);

		WebMarkupContainer optionsContainer = new WebMarkupContainer(ID_OPTIONS_CONTAINER);
		TextArea options = new TextArea<>(ID_OPTIONS, new PropertyModel<>(getModel(), ExecuteChangesHandlerDto.F_OPTIONS));
		options.setEnabled(false);
		optionsContainer.add(options);
		add(optionsContainer);
	}

}
