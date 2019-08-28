/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.gui.impl.prism;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author katkav
 */
public class ResourceAttributeDefinitionPanel<T> extends PrismPropertyPanel<T> {
	
	private static final long serialVersionUID = 1L;
	private static final Trace LOGGER = TraceManager.getTrace(ResourceAttributeDefinitionPanel.class);
	
	private static final String ID_HEADER = "header";
	
	private static final String ID_FEEDBACK = "feedback";
	private static final String ID_VALUE_CONTAINER = "valueContainer";
	
	private static final String ID_FORM = "form";
	private static final String ID_INPUT = "input";

	
	/**
	 * @param id
	 * @param model
	 */
	public ResourceAttributeDefinitionPanel(String id, IModel<ResourceAttributeWrapper<T>> model, ItemPanelSettings settings) {
		super(id, (IModel)model, settings);
	}
	
	@Override
	protected Panel createHeaderPanel() {
		return new ResourceAttributeDefinitionHeaderPanel<>(ID_HEADER, getResourceAttributeDefinitionModel());
	}
	
	private IModel<ResourceAttributeWrapper<T>> getResourceAttributeDefinitionModel(){
		return (IModel)getModel();
	}

}
