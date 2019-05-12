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

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.impl.prism.*;
import com.evolveum.midpoint.web.component.input.AssociationExpressionValuePanel;
import com.evolveum.midpoint.web.component.input.SimpleValueExpressionPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author katka
 *
 */
public class ExpressionPropertyPanel extends PrismPropertyPanel<ExpressionType> {

	private static final long serialVersionUID = 1L;

	private static final String ID_EXPRESSION_PANEL = "expressionPanel";

	public ExpressionPropertyPanel(String id, IModel<PrismPropertyWrapper<ExpressionType>> model, ItemVisibilityHandler visibilitytHandler) {
		super(id, model, visibilitytHandler);
	}

	@Override
	protected void createValuePanel(ListItem<PrismPropertyValueWrapper<ExpressionType>> item, GuiComponentFactory factory, ItemVisibilityHandler visibilityHandler) {
		ExpressionWrapper expressionWrapper = (ExpressionWrapper) getModelObject();
		Component expressionPanel = null;
		if (expressionWrapper.isAttributeExpression()) {
			expressionPanel = new SimpleValueExpressionPanel(ID_EXPRESSION_PANEL, Model.of(getModelObject().getItem().getRealValue()));
		} else if (expressionWrapper.isAssociationExpression()){
			expressionPanel = new AssociationExpressionValuePanel(ID_EXPRESSION_PANEL, Model.of(getModelObject().getItem().getRealValue()),
					expressionWrapper.getConstruction());
		} else {
			expressionPanel = new TextPanel<ExpressionType>(ID_EXPRESSION_PANEL, Model.of(getModelObject().getItem().getRealValue()));
		}
		expressionPanel.setOutputMarkupId(true);
		item.add(expressionPanel);
	}

}
