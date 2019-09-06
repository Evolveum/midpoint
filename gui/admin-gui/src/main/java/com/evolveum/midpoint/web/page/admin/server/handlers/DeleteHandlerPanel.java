/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
