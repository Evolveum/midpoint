/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.gui.impl.prism.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.input.ExpressionValuePanel;
import com.evolveum.midpoint.gui.impl.prism.ExpressionWrapper;

/**
 * @author katka
 *
 */
public class ExpressionPropertyPanel extends BasePanel<ExpressionWrapper> {

	private static final long serialVersionUID = 1L;

	private static final String ID_EXPRESSION_VALUE_PANEL = "expressionValuePanel";

	public ExpressionPropertyPanel(String id, IModel<ExpressionWrapper> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize(){
		super.onInitialize();
		add(new ExpressionValuePanel(ID_EXPRESSION_VALUE_PANEL, Model.of(getModelObject().getItem().getRealValue()), getModelObject().getConstruction()));
	}
	

}
