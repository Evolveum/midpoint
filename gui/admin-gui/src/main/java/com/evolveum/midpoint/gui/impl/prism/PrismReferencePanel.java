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

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.factory.PrismReferencePanelContext;
import com.evolveum.midpoint.gui.impl.factory.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * @author katka
 *
 */
public class PrismReferencePanel<R extends Referencable> extends ItemPanel<PrismReferenceValueWrapperImpl<R>, PrismReferenceWrapper<R>>{

	private static final long serialVersionUID = 1L;
	
	private static final String ID_HEADER = "header";
	private static final String ID_VALUE = "value";
	private static final String ID_FEEDBACK = "feedback";

	public PrismReferencePanel(String id, IModel<PrismReferenceWrapper<R>> model, ItemVisibilityHandler visibilityHandler) {
		super(id, model, visibilityHandler);
	}
	
	@Override
	protected Panel createHeaderPanel() {
		return new PrismReferenceHeaderPanel<R>(ID_HEADER, getModel());
	}

	@Override
	protected void createValuePanel(ListItem<PrismReferenceValueWrapperImpl<R>> item, GuiComponentFactory componentFactory, ItemVisibilityHandler visibilityHandler) {
		if (componentFactory != null) {
			PrismReferencePanelContext<?> panelCtx = new PrismReferencePanelContext<>(getModel());
			panelCtx.setComponentId(ID_VALUE);
			panelCtx.setParentComponent(this);
			panelCtx.setRealValueModel((IModel)item.getModel());
			
			Panel panel = componentFactory.createPanel(panelCtx);
			item.add(panel);
		} else {
			FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
			feedback.setOutputMarkupId(true);
			item.add(feedback);
			ValueChoosePanel<R> panel = new ValueChoosePanel<R>(ID_VALUE, new ItemRealValueModel<>(item.getModel())) {

				private static final long serialVersionUID = 1L;
				
				@Override
				protected ObjectFilter createCustomFilter() {
					return PrismReferencePanel.this.getModelObject().getFilter();
				}

//				@Override
//				protected boolean isEditButtonEnabled() {
//					if (getModel() == null) {
//						return true;
//					}
//					
//					//TODO only is association
//					return getModelObject() == null;
//					
//				}

				@Override
				public List<QName> getSupportedTypes() {
					List<QName> targetTypeList = PrismReferencePanel.this.getModelObject().getTargetTypes();
					if (targetTypeList == null || WebComponentUtil.isAllNulls(targetTypeList)) {
						return Arrays.asList(ObjectType.COMPLEX_TYPE);
					}
					return targetTypeList;
				}

				@Override
				protected <O extends ObjectType> Class<O> getDefaultType(List<QName> supportedTypes) {
					if (AbstractRoleType.COMPLEX_TYPE.equals(PrismReferencePanel.this.getModelObject().getTargetTypeName())) {
						return (Class<O>) RoleType.class;
					} else {
						return super.getDefaultType(supportedTypes);
					}
				}

			};
			
			feedback.setFilter(new ComponentFeedbackMessageFilter(panel));
			item.add(panel);
		}
	}

}
