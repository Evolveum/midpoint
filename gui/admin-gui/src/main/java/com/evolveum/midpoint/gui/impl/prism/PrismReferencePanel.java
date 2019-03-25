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
package com.evolveum.midpoint.gui.impl.prism;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.factory.PrismReferencePanelContext;
import com.evolveum.midpoint.gui.impl.factory.PrismReferenceValueWrapper;
import com.evolveum.midpoint.gui.impl.factory.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;

/**
 * @author katka
 *
 */
public class PrismReferencePanel<R extends Referencable> extends ItemPanel<PrismReferenceValueWrapper<R>, PrismReferenceWrapper<R>>{

	private static final long serialVersionUID = 1L;
	
	private static final String ID_HEADER = "header";

	public PrismReferencePanel(String id, IModel<PrismReferenceWrapper<R>> model) {
		super(id, model);
	}
	
	@Override
	protected Panel createHeaderPanel() {
		return new PrismReferenceHeaderPanel<R>(ID_HEADER, getModel());
	}

	@Override
	protected void createValuePanel(ListItem<PrismReferenceValueWrapper<R>> item, GuiComponentFactory componentFactory) {
		
		PrismReferencePanelContext<?> panelCtx = new PrismReferencePanelContext<>(getModel());
		panelCtx.setComponentId("value");
		panelCtx.setParentComponent(this);
		panelCtx.setRealValueModel((IModel)item.getModel());
		
		Panel panel = componentFactory.createPanel(panelCtx);
		item.add(panel);
	}

}
