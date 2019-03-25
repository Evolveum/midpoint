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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author katka
 *
 */
public class PrismContainerHeaderPanel<C extends Containerable> extends BasePanel<PrismContainerWrapper<C>> { //ItemHeaderPanel<PrismContainerWrapper<C>>{

	private static final long serialVersionUID = 1L;
	
	private static final String ID_ADD_BUTTON = "addButton";
	private static final String ID_EXPAND_COLLAPSE_FRAGMENT = "expandCollapseFragment";
    private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";
	
	public PrismContainerHeaderPanel(String id, IModel<PrismContainerWrapper<C>> model) {
		super(id, model);
	
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}
	
	private void initLayout() {
		setOutputMarkupId(true);
	
		initButtons();
//		initHeaderLabel();
	}

//	@Override
	protected void initButtons() {
		
		add(new VisibleEnableBehaviour() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isContainerMultivalue();
			}
		});
		
		 AjaxLink<?> addButton = new AjaxLink<Void>(ID_ADD_BUTTON) {
				private static final long serialVersionUID = 1L;

				@Override
	            public void onClick(AjaxRequestTarget target) {
	                addValue(target);
	            }
	        };
	        addButton.add(new VisibleEnableBehaviour() {
				
				private static final long serialVersionUID = 1L;

				@Override
				public boolean isVisible() {
					return isAddButtonVisible();
				}
			});
	        
	        add(addButton);

	}
	
	protected boolean isAddButtonVisible() {
		return getModelObject().isExpanded();
	}
	
	private void addValue(AjaxRequestTarget target) {
//		ContainerWrapperFactory cwf = new ContainerWrapperFactory(getPageBase());
//		PrismContainerWrapper<C> containerWrapper = getModelObject();
//		Task task = getPageBase().createSimpleTask("Creating new container");
//		ContainerValueWrapper<C> newContainerValue = cwf.createContainerValueWrapper(containerWrapper,
//				containerWrapper.getItem().createNewValue(), containerWrapper.getObjectStatus(), ValueStatus.ADDED,
//				containerWrapper.getPath(), task);
//		newContainerValue.setShowEmpty(true, false);
//		getModelObject().addValue(newContainerValue);
//		onButtonClick(target);
	}
	
	private boolean isContainerMultivalue(){
		return true;// getModelObject().isVisible() && getModelObject().getItemDefinition().isMultiValue();
	}
	
	
	public String getLabel() {
		return getModelObject() != null ? getModelObject().getDisplayName() : "";
	}
	
//	@Override
//	protected WebMarkupContainer initExpandCollapseButton(String contentAreaId) {
//		Fragment expandCollapseFragment = new Fragment(contentAreaId, ID_EXPAND_COLLAPSE_FRAGMENT, this);
//		
//		ToggleIconButton<?> expandCollapseButton = new ToggleIconButton<Void>(ID_EXPAND_COLLAPSE_BUTTON,
//				GuiStyleConstants.CLASS_ICON_EXPAND_CONTAINER, GuiStyleConstants.CLASS_ICON_COLLAPSE_CONTAINER) {
//			
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				onExpandClick(target);
//			}
//						
//			@Override
//			public boolean isOn() {
//				return PrismContainerHeaderPanel.this.getModelObject().isExpanded();
//			}
//        };
//        expandCollapseButton.setOutputMarkupId(true);
//        
//        expandCollapseFragment.add(expandCollapseButton);
//        
//        return expandCollapseFragment;
//	}
//	
//	private void onExpandClick(AjaxRequestTarget target) {
//		
//		PrismContainerWrapper<C> wrapper = getModelObject();
//		wrapper.setExpanded(!wrapper.isExpanded());
//		onButtonClick(target);
//	}
//	
//	@Override
//	protected void initHeaderLabel(){
//		WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
//        labelContainer.setOutputMarkupId(true);
//        
//        add(labelContainer);
//        
//		String displayName = getLabel();
//		if (org.apache.commons.lang3.StringUtils.isEmpty(displayName)) {
//			displayName = "displayName.not.set";
//		}
//		
//		StringResourceModel headerLabelModel = createStringResource(displayName);
//		AjaxButton labelComponent = new AjaxButton(ID_LABEL, headerLabelModel) {
//			private static final long serialVersionUID = 1L;
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				onExpandClick(target);
//			}
//		};
//		labelComponent.setOutputMarkupId(true);
//		labelComponent.add(AttributeAppender.append("style", "cursor: pointer;"));
//		labelContainer.add(labelComponent);
//		
//		labelContainer.add(getHelpLabel());
//
//	}
//	
//	@Override
//	protected String getHelpText() {
//		return WebComponentUtil.loadHelpText(new Model<ContainerWrapperImpl<C>>(getModelObject()), PrismContainerHeaderPanel.this);
//	}
//	
//	@Override
//	protected boolean isVisibleHelpText() {
//		return true;
//	}

}
