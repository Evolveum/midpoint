/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.ScriptExecutionHandlerDto;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class ScriptExecutionHandlerPanel extends BasePanel<ScriptExecutionHandlerDto> {
	private static final long serialVersionUID = 1L;

	private static final String ID_SCRIPT_CONTAINER = "scriptContainer";
	private static final String ID_SCRIPT = "script";

	public ScriptExecutionHandlerPanel(String id, IModel<ScriptExecutionHandlerDto> model) {
		super(id, model);
		initLayout();
		setOutputMarkupId(true);
	}

	private void initLayout() {
		WebMarkupContainer scriptContainer = new WebMarkupContainer(ID_SCRIPT_CONTAINER);
		TextArea script = new TextArea<>(ID_SCRIPT, new PropertyModel<>(getModel(), ScriptExecutionHandlerDto.F_SCRIPT));
		script.setEnabled(false);
		scriptContainer.add(script);
		add(scriptContainer);
	}

}
