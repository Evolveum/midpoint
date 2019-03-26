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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;

/**
 * @author skublik
 */
public class PrismPropertyColumn<C extends Containerable, T> extends AbstractItemWrapperColumn<C, PrismPropertyValueWrapper<T>> {

	private static final long serialVersionUID = 1L;
	
	private static final String ID_HEADER = "header";
	
	public PrismPropertyColumn(IModel<PrismContainerWrapper<C>> mainModel, ItemPath itemName, PageBase pageBase, boolean readonly) {
		super(mainModel, itemName, pageBase, readonly);
	}

	
	@Override
	public IModel<?> getDataModel(IModel<PrismContainerValueWrapper<C>> rowModel) {
		return Model.of(rowModel.getObject().findProperty(itemName));
	}

	@Override
	protected String createLabel(PrismPropertyValueWrapper<T> object) {
		return object.getRealValue().toString();
	}

	@Override
	protected Panel createValuePanel(IModel<?> headerModel, PrismPropertyValueWrapper<T> object) {
		GuiComponentFactory<PrismPropertyPanelContext<T>> factory = getPageBase().getRegistry().findValuePanelFactory((PrismPropertyWrapper<T>) headerModel.getObject());
		
		PrismPropertyPanelContext<T> panelCtx = new PrismPropertyPanelContext<>((IModel<PrismPropertyWrapper<T>>)headerModel);
		panelCtx.setForm(new Form("form"));
//		panelCtx.setFeedbackPanel(feedbackPanel);
		return factory.createPanel(panelCtx);
	}

	@Override
	protected Component createHeader(IModel<PrismContainerWrapper<C>> mainModel) {
		return new PrismPropertyHeaderPanel<>(ID_HEADER, Model.of(mainModel.getObject().findProperty(itemName)));
	}

}


