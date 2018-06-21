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

import com.evolveum.midpoint.web.page.admin.server.handlers.dto.DeleteHandlerDto;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class DeleteHandlerPanel extends QueryBasedHandlerPanel<DeleteHandlerDto> {

	private static final long serialVersionUID = 1L;

	private static final String ID_RAW_CONTAINER = "rawContainer";
	private static final String ID_RAW = "raw";

	public DeleteHandlerPanel(String id, IModel<DeleteHandlerDto> handlerDtoModel) {
		super(id, handlerDtoModel);
		initLayout();
	}

	private void initLayout() {
		WebMarkupContainer rawContainer = new WebMarkupContainer(ID_RAW_CONTAINER);
		CheckBox raw = new CheckBox(ID_RAW, new PropertyModel<>(getModelObject(), DeleteHandlerDto.F_RAW));
		raw.setEnabled(false);
		rawContainer.add(raw);
		add(rawContainer);
	}

}
