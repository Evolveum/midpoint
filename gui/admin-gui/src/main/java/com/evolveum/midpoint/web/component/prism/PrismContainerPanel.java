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

package com.evolveum.midpoint.web.component.prism;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lazyman
 * @author semancik
 */
public class PrismContainerPanel<C extends Containerable> extends BasePanel<ContainerWrapper<C>> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PrismContainerPanel.class);
    private static final String ID_HEADER = "header";
    private static final String STRIPED_CLASS = "striped";

    private PageBase pageBase;
    
    public PrismContainerPanel(String id, final IModel<ContainerWrapper<C>> model, boolean showHeader, Form form, ItemVisibilityHandler isPanelVisible, PageBase pageBase) {
    	this(id, model, showHeader, form, isPanelVisible, pageBase, true);
    }

    public PrismContainerPanel(String id, final IModel<ContainerWrapper<C>> model, boolean showHeader, Form form, ItemVisibilityHandler isPanelVisible, PageBase pageBase, boolean isModelOnTopLevel) {
        super(id, model);
        setOutputMarkupId(true); 
		this.pageBase = pageBase;
		
        if(model.getObject() != null) {
			model.getObject().setShowOnTopLevel(isModelOnTopLevel);
		}
        
        LOGGER.trace("Creating container panel for {}", model.getObject());

        //TODO: visible behaviour??
        add( new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
        	public boolean isVisible() {
        		return isPanelVisible(isPanelVisible, model);
        	}
        });
        
        initLayout(model, form, isPanelVisible, showHeader);
        
        if(model.getObject() != null && model.getObject().getItemDefinition() != null && model.getObject().getItemDefinition().isMultiValue()) {
        	add(AttributeModifier.append("class", "prism-multivalue-container"));
        }
        
    }
    
    @Override
    protected void onConfigure() {
    	
    	if(getModel().getObject() != null && getModel().getObject().getPath() != null) {
        	setExtendedForEmptyContainers(getModel());
        }

    	super.onConfigure();
    }

    private void setExtendedForEmptyContainers(IModel<ContainerWrapper<C>> model) {
    	Collection<QName> qNames = QNameUtil.createCollection(
    			SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
    			SystemConfigurationType.F_GLOBAL_POLICY_RULE,
    			SystemConfigurationType.F_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS,
    			SystemConfigurationType.F_CLEANUP_POLICY,
    			SystemConfigurationType.F_PROFILING_CONFIGURATION,
    			SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION,
    			SystemConfigurationType.F_WORKFLOW_CONFIGURATION,
    			SystemConfigurationType.F_ROLE_MANAGEMENT,
    			SystemConfigurationType.F_INTERNALS,
    			SystemConfigurationType.F_DEPLOYMENT_INFORMATION,
    			SystemConfigurationType.F_ACCESS_CERTIFICATION,
    			SystemConfigurationType.F_INFRASTRUCTURE,
    			SystemConfigurationType.F_FULL_TEXT_SEARCH);
    	
    	if(qNames.contains(model.getObject().getPath().firstToNameOrNull())) {
    		model.getObject().getValues().forEach(value -> setExpandedForContainerValueWrapper(value));
    	}
	}

	private void setExpandedForContainerValueWrapper(ContainerValueWrapper<C> value) {
		
		value.setExpanded(true);
		if(WebModelServiceUtils.isContainerValueWrapperEmpty(value) && getModelObject().isShowOnTopLevel()) {
			value.setShowEmpty(true, false);
		}
		
		boolean expandingHigherLevelContainerValue = true;
		if(WebModelServiceUtils.isContainerValueWrapperEmpty(value) && !getModelObject().isShowOnTopLevel()) {
			value.setExpanded(false);
			value.setShowEmpty(true, false);
			expandingHigherLevelContainerValue = false;
		}
		
		for(ItemWrapper itemWrapper: value.getItems()) {
			if(itemWrapper instanceof ContainerWrapper) {
				if(!((ContainerWrapper<C>)itemWrapper).isEmpty()) {
					((ContainerWrapper<C>)itemWrapper).getValues().forEach(containerValue -> setExpandedForContainerValueWrapper(containerValue));
					((ContainerWrapper<C>)itemWrapper).setExpanded(true);
					continue;
				} 
				if(!getModelObject().isShowOnTopLevel()) {
					((ContainerWrapper<C>)itemWrapper).setExpanded(true);
				} else {
					((ContainerWrapper<C>)itemWrapper).setExpanded(expandingHigherLevelContainerValue);
				}
			} 
		}
	}

	public boolean isPanelVisible(ItemVisibilityHandler isPanelVisible, IModel<ContainerWrapper<C>> model) {
    	if (isPanelVisible != null && model.getObject() != null) {
			ItemVisibility visible = isPanelVisible.isVisible(model.getObject());
			if (visible != null) {
				switch (visible) {
    				case VISIBLE:
    					return true;
    				case HIDDEN:
    					return false;
    				default:
    					// automatic, go on ...
    			}
			}
		}
        return model.getObject() != null && model.getObject().isVisible();
    }
    
    private void initLayout(final IModel<ContainerWrapper<C>> model, final Form form, ItemVisibilityHandler isPanelVisible, boolean showHeader) {
    	PrismContainerHeaderPanel<C> header = new PrismContainerHeaderPanel<C>(ID_HEADER, model) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onButtonClick(AjaxRequestTarget target) {
				addOrReplaceProperties(model, form, isPanelVisible, true);
				target.add(PrismContainerPanel.this.getParent());
			}


    	};
        header.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return model.getObject().getItemDefinition().isMultiValue();
            }
        });
        header.setOutputMarkupId(true);
        add(header);

        addOrReplaceProperties(model, form, isPanelVisible, false);
    }

    public PageBase getPageBase(){
        return pageBase;
    }

    private IModel<String> createStyleClassModel(final IModel<ItemWrapper> wrapper) {
        return new AbstractReadOnlyModel<String>() {
        	private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
            	ItemWrapper property = wrapper.getObject();
                return property.isStripe() ? "stripe" : null;
            }
        };
    }

    private void addOrReplaceProperties(IModel<ContainerWrapper<C>> model, final Form form, ItemVisibilityHandler isPanelVisible, boolean isToBeReplaced){
    	ListView<ContainerValueWrapper<C>> values = new ListView<ContainerValueWrapper<C>>("values", new PropertyModel<List<ContainerValueWrapper<C>>>(model, "values")) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<ContainerValueWrapper<C>> item) {
                    ContainerValuePanel<C> containerPanel = new ContainerValuePanel<C>("value", item.getModel(), true, form, isPanelVisible, pageBase);
                    containerPanel.setOutputMarkupId(true);
                    item.add(new VisibleEnableBehaviour() {
                    	@Override
                    	public boolean isVisible() {
                    		if(!model.getObject().isExpanded()) {
    							return false;
    						}
    						return containerPanel.isVisible();
                    	}
                    });
                    
                    item.add(containerPanel);
                    item.setOutputMarkupId(true);
                    containerPanel.add(AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {
    					
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
