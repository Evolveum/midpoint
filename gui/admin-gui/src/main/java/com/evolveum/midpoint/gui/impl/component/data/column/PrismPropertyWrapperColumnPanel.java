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
package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;

/**
 * @author katka
 *
 */
public class PrismPropertyWrapperColumnPanel<T> extends AbstractItemWrapperColumnPanel<PrismPropertyWrapper<T>, PrismPropertyValueWrapper<T>> {

	private static final long serialVersionUID = 1L;
	private static final transient Trace LOGGER = TraceManager.getTrace(PrismPropertyWrapperColumnPanel.class);
	
	PrismPropertyWrapperColumnPanel(String id, IModel<PrismPropertyWrapper<T>> model, ColumnType columnType) {
		super(id, model, columnType);
	}
	
	@Override
	protected String createLabel(PrismPropertyValueWrapper<T> object) {
		return object.toShortString(); 
		
	}

	@Override
	protected Panel createValuePanel(String id, IModel<PrismPropertyWrapper<T>> model, PrismPropertyValueWrapper<T> object) {
		
		model.getObject().setColumn(true);
		
		Panel panel;
		try {
			panel = getPageBase().initPanel(id, model.getObject().getTypeName(), model, false);
		} catch (SchemaException e) {
			LOGGER.error("Cannot create panel for {}", model.getObject());
			getSession().error("Cannot create panel for: " + model.getObject());
			return new ErrorPanel(id, createStringResource("PropertyPropertyWrapperColumnPanel.cannot.create.panel"));
		}
		
		return panel;
	}

	@Override
	protected Panel createLink(String id, IModel<PrismPropertyValueWrapper<T>> object) {
		LinkPanel linkPanel = new LinkPanel(id,
				new ItemRealValueModel(object)) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				PrismPropertyWrapperColumnPanel.this.onClick(target, getModelObject().getParent());
			}

		};
		return linkPanel;
	}
	
	protected void onClick(AjaxRequestTarget target, PrismContainerValueWrapper<?> rowModel) {
	}
}
