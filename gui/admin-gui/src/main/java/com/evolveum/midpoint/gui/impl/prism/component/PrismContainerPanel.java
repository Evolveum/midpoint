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

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerValuePanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.PrismContainerHeaderPanel;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanelOld;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author katka
 *
 */
public class PrismContainerPanel<C extends Containerable> extends BasePanel<PrismContainerWrapper<C>> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PrismContainerPanelOld.class);
    private static final String ID_HEADER = "header";
    private static final String STRIPED_CLASS = "striped";
    
    private ItemVisibilityHandler itemVisibility = null;
    private Form form;
    
    private PrismContainerWrapper<C> containerWrapper;
    

    public PrismContainerPanel(String id, final PrismContainerWrapper<C> model, Form form, ItemVisibilityHandler isPanelVisible) {
        super(id);
        setOutputMarkupId(true); 
		this.itemVisibility = isPanelVisible;
		this.form = form;
		this.containerWrapper = model;
		
        LOGGER.trace("Creating container panel for {}", model);

        //TODO: visible behaviour??
//        add( new VisibleEnableBehaviour() {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//        	public boolean isVisible() {
//        		return isPanelVisible(isPanelVisible, model);
//        	}
//        });
        
    }
    
    /* (non-Javadoc)
     * @see org.apache.wicket.MarkupContainer#onInitialize()
     */
    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	 initLayout(form, itemVisibility);
    }
    
//    @Override
//    protected void onConfigure() {
//    	
////    	if(getModel().getObject() != null && getModel().getObject().getPath() != null) {
////        	setExtendedForEmptyContainers();
////        }
//
//    	super.onConfigure();
//    }

//    private void setExtendedForEmptyContainers() {
//    	Collection<QName> qNames = QNameUtil.createCollection(
//    			SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
//    			SystemConfigurationType.F_GLOBAL_POLICY_RULE,
//    			SystemConfigurationType.F_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS,
//    			SystemConfigurationType.F_CLEANUP_POLICY,
//    			SystemConfigurationType.F_PROFILING_CONFIGURATION,
//    			SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION,
//    			SystemConfigurationType.F_WORKFLOW_CONFIGURATION,
//    			SystemConfigurationType.F_ROLE_MANAGEMENT,
//    			SystemConfigurationType.F_INTERNALS,
//    			SystemConfigurationType.F_DEPLOYMENT_INFORMATION,
//    			SystemConfigurationType.F_ACCESS_CERTIFICATION,
//    			SystemConfigurationType.F_INFRASTRUCTURE,
//    			SystemConfigurationType.F_FULL_TEXT_SEARCH);
//    	
//    	if(qNames.contains(model.getObject().getPath().firstToNameOrNull())) {
//    		containerWrapper.getValues().forEach(value -> setExpandedForContainerValueWrapper(value));
//    	}
//	}

//	private void setExpandedForContainerValueWrapper(ContainerValueWrapper<C> value) {
//		
//		value.setExpanded(true);
//		if(WebModelServiceUtils.isContainerValueWrapperEmpty(value) && getModelObject().isShowOnTopLevel()) {
//			value.setShowEmpty(true, false);
//		}
//		
//		boolean expandingHigherLevelContainerValue = true;
//		if(WebModelServiceUtils.isContainerValueWrapperEmpty(value) && !getModelObject().isShowOnTopLevel()) {
//			value.setExpanded(false);
//			value.setShowEmpty(false, false);
//			expandingHigherLevelContainerValue = false;
//		}
//		
//		for(ItemWrapperOld itemWrapper: value.getItems()) {
//			if(itemWrapper instanceof ContainerWrapperImpl) {
//				if(!((ContainerWrapperImpl<C>)itemWrapper).isEmpty()) {
//					((ContainerWrapperImpl<C>)itemWrapper).getValues().forEach(containerValue -> setExpandedForContainerValueWrapper(containerValue));
//					((ContainerWrapperImpl<C>)itemWrapper).setExpanded(true);
//					continue;
//				} 
//				if(!getModelObject().isShowOnTopLevel()) {
//					((ContainerWrapperImpl<C>)itemWrapper).setExpanded(true);
//				} else {
//					((ContainerWrapperImpl<C>)itemWrapper).setExpanded(expandingHigherLevelContainerValue);
//				}
//			} 
//		}
//	}

    
    private void initLayout(final Form form, ItemVisibilityHandler isPanelVisible) {
    	
    	PrismContainerHeaderPanel<C> header = containerWrapper.createHeader(ID_HEADER);
    	add(header);

        addOrReplaceProperties(form, isPanelVisible, false);
    }

    private void addOrReplaceProperties(final Form form, ItemVisibilityHandler isPanelVisible, boolean isToBeReplaced){
    	ListView<ContainerValueWrapper<C>> values = new ListView<ContainerValueWrapper<C>>("values", new PropertyModel<List<ContainerValueWrapper<C>>>(model, "values")) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<ContainerValueWrapper<C>> item) {
                    ContainerValuePanel<C> containerPanel = new ContainerValuePanel<C>("value", item.getModel(), true, form, isPanelVisible, getPageBase());
                    containerPanel.setOutputMarkupId(true);
                    item.add(new VisibleEnableBehaviour() {
                    	@Override
                    	public boolean isVisible() {
                    		if(!containerWrapper.isExpanded()) {
    							return false;
    						}
    						return containerPanel.isVisible();
                    	}
                    });
                    
                    item.add(containerPanel);
                    item.setOutputMarkupId(true);
                    containerPanel.add(AttributeModifier.append("class", new IModel<String>() {
    					
						private static final long serialVersionUID = 1L;

						@Override
						public String getObject() {
							return GuiImplUtil.getObjectStatus(((ContainerValueWrapper<Containerable>)item.getModelObject()));
						}
					}));
                    if(((ContainerValueWrapper)item.getModelObject()).getContainer() !=null && ((ContainerValueWrapper)item.getModelObject()).getContainer().isShowOnTopLevel()) {
						item.add(AttributeModifier.append("class", "top-level-prism-container"));
					}
                    
                    if(!containerPanel.hasAnyProperty()) {
                    	item.add(AttributeModifier.append("style", " border-top: none; padding-top: 0px; "));
                    }

			}
			
		};
    	values.setReuseItems(true);
    	values.setOutputMarkupId(true);
        if (isToBeReplaced) {
            replace(values);
        } else {
            add(values);
        }
    }

}
