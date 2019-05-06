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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public class PrismObjectValuePanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {

	private static final long serialVersionUID = 1L;
	
	private static final String ID_VALUE = "value";
	
	private ItemVisibilityHandler visibilityHandler;

	public PrismObjectValuePanel(String id, IModel<PrismObjectWrapper<O>> model, ItemVisibilityHandler visibilityHandler) {
		super(id, model);
		this.visibilityHandler = visibilityHandler;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
		setOutputMarkupId(true);
	}
	
	private void initLayout() {
//		createHeaderPanel();
		
		createValuePanel(ID_VALUE, new PropertyModel<>(getModel(), "value"));
	}
	
//	private Panel createHeaderPanel() {
//		return new PrismContainerHeaderPanel(ID_HEADER, getModel());
////		return header;
//	}
//	
	
	protected void createValuePanel(String panelId, IModel<PrismObjectValueWrapper<O>> valueModel) {
		
		PrismContainerValuePanel<O, PrismObjectValueWrapper<O>> valueWrapper = new PrismContainerValuePanel<>(panelId, valueModel, visibilityHandler);
		valueWrapper.setOutputMarkupId(true);
		add(valueWrapper);
		
	}
	
//	private <IW extends ItemWrapper> void createProperties() {
//		WebMarkupContainer propertiesLabel = new WebMarkupContainer(ID_PROPERTIES_LABEL);
//    	propertiesLabel.setOutputMarkupId(true);
//    	
//		ListView<IW> properties = new ListView<IW>("properties",
//	            new PropertyModel<>(getModel(), "nonContainers")) {
//				private static final long serialVersionUID = 1L;
//
//				@Override
//	            protected void populateItem(final ListItem<IW> item) {
//					item.setOutputMarkupId(true);
//					IW itemWrapper = item.getModelObject();
//					Class<?> panelClass = getPageBase().getWrapperPanel(itemWrapper.getTypeName());
//					
//					Constructor<?> constructor;
//					try {
//						constructor = panelClass.getConstructor(String.class, IModel.class);
//						Panel panel = (Panel) constructor.newInstance("property", item.getModel());
//						item.add(panel);
//					} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//						throw new SystemException("Cannot instantiate " + panelClass);
//					}
//					
//					
//					
//		            item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));
//	            }
//	        };
//	        properties.setReuseItems(true);
//	        properties.setOutputMarkupId(true);
//	        add(propertiesLabel);
//	       	propertiesLabel.add(properties);
//	       	
//	        AjaxButton labelShowEmpty = new AjaxButton(ID_SHOW_EMPTY_BUTTON) {
//				private static final long serialVersionUID = 1L;
//				@Override
//				public void onClick(AjaxRequestTarget target) {
//					onShowEmptyClick(target);
//					target.add(PrismContainerValuePanel.this);
//				}
//				
//				@Override
//				public IModel<?> getBody() {
//					return getNameOfShowEmptyButton();
//				}
//			};
//			labelShowEmpty.setOutputMarkupId(true);
//			labelShowEmpty.add(AttributeAppender.append("style", "cursor: pointer;"));
//			labelShowEmpty.add(new VisibleEnableBehaviour() {
//				
//				private static final long serialVersionUID = 1L;
//
//				@Override
//				public boolean isVisible() {
//					return getModelObject().isExpanded();// && !model.getObject().isShowEmpty();
//				}
//			});
//			add(labelShowEmpty);
//	}
//	
}
