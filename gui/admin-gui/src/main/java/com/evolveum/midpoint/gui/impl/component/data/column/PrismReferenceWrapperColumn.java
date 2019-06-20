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

package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismReferenceWrapperHeaderModel;
import com.evolveum.midpoint.web.model.PrismReferenceWrapperModel;

/**
 * @author skublik
 */
public class PrismReferenceWrapperColumn<C extends Containerable, R extends Referencable> extends AbstractItemWrapperColumn<C, PrismValueWrapper<R, PrismReferenceValue>> {

	private static final long serialVersionUID = 1L;
	
	private static final String ID_HEADER = "header";
	private static final String ID_INPUT = "input";

	private PageBase pageBase;
	
	public PrismReferenceWrapperColumn(IModel<? extends PrismContainerDefinition<C>> mainModel, ItemPath itemName, ColumnType columnType, PageBase pageBase) {
		super(mainModel, itemName, columnType);
		this.pageBase = pageBase;
	}

	
	@Override
	public IModel<?> getDataModel(IModel<PrismContainerValueWrapper<C>> rowModel) {
		return PrismReferenceWrapperModel.fromContainerValueWrapper(rowModel, itemName);
	}

	@Override
	protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<C>> mainModel) {
		return new PrismReferenceHeaderPanel<>(componentId, new PrismReferenceWrapperHeaderModel(mainModel, itemName, pageBase));
	}


	@Override
	protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
		return new PrismReferenceWrapperColumnPanel<R>(componentId, (IModel<PrismReferenceWrapper<R>>)rowModel, getColumnType()) {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected void onClick(AjaxRequestTarget target, PrismContainerValueWrapper<?> rowModel) {
				PrismReferenceWrapperColumn.this.onClick(target, (IModel) Model.of(rowModel));
			}
		};
	}

	protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> model) {
		
	}
}


