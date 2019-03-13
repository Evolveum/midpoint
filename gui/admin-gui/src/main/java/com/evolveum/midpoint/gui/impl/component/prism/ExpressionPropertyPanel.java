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
package com.evolveum.midpoint.gui.impl.component.prism;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.web.component.input.ExpressionValuePanel;
import com.evolveum.midpoint.web.component.prism.ExpressionWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * @author katka
 *
 */
public class ExpressionPropertyPanel extends PrismPropertyPanel<ExpressionWrapper> {

	private static final long serialVersionUID = 1L;
	
	/**
	 * @param id
	 * @param model
	 * @param form
	 * @param visibilityHandler
	 */
	public ExpressionPropertyPanel(String id, ExpressionWrapper model, Form form,
			ItemVisibilityHandler visibilityHandler) {
		super(id, model, form, visibilityHandler);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.component.prism.PrismPropertyPanel#createValuePanel(org.apache.wicket.model.IModel, org.apache.wicket.markup.html.panel.FeedbackPanel, org.apache.wicket.markup.html.form.Form)
	 */
	@Override
	protected <T> Component createValuePanel(IModel<ValueWrapperOld<T>> valueWrapperModel, FeedbackPanel feedback, Form form) {
		
//	      if ((itemWrapper.getPath().containsNameExactly(ConstructionType.F_ASSOCIATION) ||
//                itemWrapper.getPath().containsNameExactly(ConstructionType.F_ATTRIBUTE))&&
//        itemWrapper.getPath().containsNameExactly(ResourceObjectAssociationType.F_OUTBOUND) &&
//        itemWrapper.getPath().containsNameExactly(MappingType.F_EXPRESSION)){
		
		return new ExpressionValuePanel("input", new PropertyModel(valueWrapperModel, "value.value"),
              getModel().getObject().getConstruction(), getPageBase()){
          private static final long serialVersionUID = 1L;

          @Override
          protected boolean isAssociationExpression(){
              return ExpressionPropertyPanel.this.getModel().getObject().getPath().containsNameExactly(ConstructionType.F_ASSOCIATION);
          }
      };
	}
	

}
