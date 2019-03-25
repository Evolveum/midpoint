/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.prism;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.apache.wicket.validation.validator.RangeValidator;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.impl.component.prism.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * @author lazyman
 * @author semancik
 */
public class ContainerValuePanel<C extends Containerable> extends BasePanel<ContainerValueWrapper<C>> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ContainerValuePanel.class);
    private static final String ID_HEADER = "header";
    private static final String ID_PROPERTIES_LABEL = "propertiesLabel";
    private static final String STRIPED_CLASS = "striped";
    private static final String ID_SHOW_EMPTY_BUTTON = "showEmptyButton";

    private PageBase pageBase;
//    boolean isVisibleShowMoreButton;

    public ContainerValuePanel(String id, final IModel<ContainerValueWrapper<C>> model, boolean showHeader, Form form, ItemVisibilityHandler isPanelVisible, PageBase pageBase) {
        super(id, model);
        setOutputMarkupId(true);
		this.pageBase = pageBase;
		
		add(new VisibleEnableBehaviour() {
			
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isVisible() {
				return model.getObject().isVisible();
			}
		});
		
        LOGGER.trace("Creating container panel for {}", model.getObject());

        initLayout(model, form, isPanelVisible, showHeader);
    }

    private void initLayout(final IModel<ContainerValueWrapper<C>> model, final Form form, ItemVisibilityHandler isPanelVisible, boolean showHeader) {
        addOrReplacePropertiesAndContainers(model, form, isPanelVisible, false);
        
        AjaxButton labelShowEmpty = new AjaxButton(ID_SHOW_EMPTY_BUTTON) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target) {
				onShowEmptyClick(target, model, form, isPanelVisible);
				target.add(ContainerValuePanel.this);
			}
			
			@Override
			public IModel<?> getBody() {
				return getNameOfShowEmptyButton(model);
			}
		};
		labelShowEmpty.setOutputMarkupId(true);
		labelShowEmpty.add(AttributeAppender.append("style", "cursor: pointer;"));
		labelShowEmpty.add(new VisibleEnableBehaviour() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return model.getObject().isExpanded();// && !model.getObject().isShowEmpty();
			}
		});
		add(labelShowEmpty);
		
		PrismContainerValueHeaderPanel<C> header = new PrismContainerValueHeaderPanel<C>(ID_HEADER, model, isPanelVisible) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onButtonClick(AjaxRequestTarget target) {
				target.add(ContainerValuePanel.this);
				target.add(getPageBase().getFeedbackPanel());
			}

        };
        
        
        header.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return showHeader && (hasAnyProperty() || !getModelObject().getContainer().isShowOnTopLevel());// && !model.getObject().isMain();
            }
        });
        add(header);
        header.setOutputMarkupId(true);

    }
    
    public boolean hasAnyProperty() {
		for(ItemWrapperOld item : getModelObject().getItems()) {
			if(item instanceof PropertyOrReferenceWrapper) {
				return true;
			}
		}
		return false;
	}
    
    private StringResourceModel getNameOfShowEmptyButton(IModel<ContainerValueWrapper<C>> model) {
    	if(!model.getObject().isShowEmpty()) {
    		return pageBase.createStringResource("ShowEmptyButton.showMore");
    	}
    	return pageBase.createStringResource("ShowEmptyButton.showLess");
    }
    
    private void onShowEmptyClick(AjaxRequestTarget target, IModel<ContainerValueWrapper<C>> model, Form form, ItemVisibilityHandler isPanelVisible) {
		
		ContainerValueWrapper<C> wrapper = model.getObject();
		wrapper.setShowEmpty(!wrapper.isShowEmpty(), false);
			
		wrapper.computeStripes();
		target.add(ContainerValuePanel.this);
		target.add(getPageBase().getFeedbackPanel());
	}

    public PageBase getPageBase(){
        return pageBase;
    }

    private IModel<String> createStyleClassModel(final IModel<ItemWrapperOld> wrapper) {
        return new IModel<String>() {
        	private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
            	ItemWrapperOld property = wrapper.getObject();
//                return property.isStripe() ? "stripe" : null;
            	return "";
            }
        };
    }

    private <IW extends ItemWrapperOld> void addOrReplacePropertiesAndContainers(IModel<ContainerValueWrapper<C>> model, final Form form, ItemVisibilityHandler isPanaleVisible, boolean isToBeReplaced){
    	addOrReplaceProperties(model, form, isPanaleVisible, isToBeReplaced);
        addOrReplaceContainers(model, form, isPanaleVisible, isToBeReplaced);
    }
    
    private <IW extends ItemWrapperOld> WebMarkupContainer addOrReplaceProperties(IModel<ContainerValueWrapper<C>> model, final Form form, ItemVisibilityHandler isPanalVisible, boolean isToBeReplaced){
    	//isVisibleShowMoreButton = false;
    	
    	WebMarkupContainer propertiesLabel = new WebMarkupContainer(ID_PROPERTIES_LABEL);
    	propertiesLabel.setOutputMarkupId(true);
    	
    	ListView<IW> properties = new ListView<IW>("properties",
            new PropertyModel<>(model, "properties")) {
			private static final long serialVersionUID = 1L;

			@Override
            protected void populateItem(final ListItem<IW> item) {
				item.setOutputMarkupId(true);
				if (item.getModelObject() instanceof PropertyOrReferenceWrapper) {
					Panel propertyPanel = item.getModelObject().createPanel("property", form, isPanalVisible);
	                item.add(propertyPanel);
	                item.add(AttributeModifier.append("class", createStyleClassModel((IModel<ItemWrapperOld>) item.getModel())));
	                return;
				}
				WebMarkupContainer property = new WebMarkupContainer("property");
				item.add(new VisibleEnableBehaviour() {
					@Override
					public boolean isVisible() {
						return false;
					}
				});
				item.add(property);
            }
        };
        properties.setReuseItems(true);
        properties.setOutputMarkupId(true);
        add(propertiesLabel);
       	propertiesLabel.add(properties);
        return propertiesLabel;
    }
    
    private <IW extends ItemWrapperOld> void addOrReplaceContainers(IModel<ContainerValueWrapper<C>> model, final Form form, ItemVisibilityHandler isPanalVisible, boolean isToBeReplaced){
    	ListView<IW> containers = new ListView<IW>("containers",
                new PropertyModel<>(model, "properties")) {
    			private static final long serialVersionUID = 1L;

    			@Override
                protected void populateItem(final ListItem<IW> item) {
    				item.setOutputMarkupId(true);
    				if (item.getModel().getObject() instanceof ContainerWrapperImpl) {
    					PrismContainerPanelOld<C> containerPanel = (PrismContainerPanelOld<C>) item.getModelObject().createPanel("container", form, isPanalVisible);
    					item.add(containerPanel);
    					containerPanel.add(new VisibleEnableBehaviour() {
    						
    					
    						@Override
    						public boolean isVisible() {
    							if(!model.getObject().isExpanded() && !model.getObject().getContainer().isShowOnTopLevel()) {
    								return false;
    							}
    							
//    							if( ((ContainerWrapper)item.getModelObject() != null && ((ContainerWrapper)item.getModelObject()).getItemDefinition() != null 
//    	    							&& ((ContainerWrapper)item.getModelObject()).getItemDefinition().getTypeName() != null 
//    	    							&& ((ContainerWrapper)item.getModelObject()).getItemDefinition().getTypeName().equals(MetadataType.COMPLEX_TYPE) )
//    									&& ( ((ContainerWrapper)item.getModelObject()).getValues() != null  && ((ContainerWrapper)item.getModelObject()).getValues().get(0) != null 
//    									&&  !((ContainerWrapper<MetadataType>)item.getModelObject()).getValues().get(0).isVisible() ) ){
//    								return false;
//    							}
    							
    							if (model.getObject().containsMultipleMultivalueContainer(isPanalVisible)
    									&& item.getModelObject().getItemDefinition().isMultiValue()
    									&& CollectionUtils.isEmpty(item.getModelObject().getValues())) {
    								return false;
    							}
    							
//    							return containerPanel.isPanelVisible(isPanalVisible, item.getModel());
    							return true;
    							
    						}
    					});
    					return;
    				}
    				WebMarkupContainer container = new WebMarkupContainer("container");
    				item.add(new VisibleEnableBehaviour() {
    					@Override
    					public boolean isVisible() {
    						return false;
    					}
    				});
    				item.add(container);
                }
            };
            containers.setReuseItems(true);
            containers.setOutputMarkupId(true);
            if (isToBeReplaced) {
                replace(containers);
            } else {
                add(containers);
            }
    }
}
