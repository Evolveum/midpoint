/*
 * Copyright (c) 2018 Evolveum
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

package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.model.DefaultReferencableImplSingleValueContainerValueWrapperModel;
import com.evolveum.midpoint.gui.impl.model.PropertyWrapperFromContainerModel;
import com.evolveum.midpoint.gui.impl.model.ContainerRealValueModel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.page.admin.configuration.PageSystemConfiguration;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
public class ObjectPolicyConfigurationTabPanel extends BasePanel<ContainerWrapper<ObjectPolicyConfigurationType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(ObjectPolicyConfigurationTabPanel.class);
	
    private static final String ID_OBJECTS_POLICY = "objectsPolicy";
    
    public ObjectPolicyConfigurationTabPanel(String id, IModel<ContainerWrapper<ObjectPolicyConfigurationType>> model) {
        super(id, model);
        
        
    }

    @Override
    protected void onInitialize() {
    		super.onInitialize();
    		
    		PageParameters params = getPage().getPageParameters();
    		StringValue val = params.get(PageSystemConfiguration.SELECTED_TAB_INDEX);
    		if (val != null && !val.isNull()) {
    			params.remove(params.getPosition(PageSystemConfiguration.SELECTED_TAB_INDEX));
    		} 
    		params.set(PageSystemConfiguration.SELECTED_TAB_INDEX, PageSystemConfiguration.CONFIGURATION_TAB_OBJECT_POLICY);
    		
    		initLayout();
    }
    
    protected void initLayout() {
    	
    	TableId tableId = UserProfileStorage.TableId.OBJECT_POLICIES_TAB_TABLE;
    	PageStorage pageStorage = getPageBase().getSessionStorage().getObjectPoliciesConfigurationTabStorage();
    	
    	MultivalueContainerListPanelWithDetailsPanel<ObjectPolicyConfigurationType> multivalueContainerListPanel = new MultivalueContainerListPanelWithDetailsPanel<ObjectPolicyConfigurationType>(ID_OBJECTS_POLICY, getModel(),
    			tableId, pageStorage) {
			
			private static final long serialVersionUID = 1L;
			
			@Override
			protected List<ContainerValueWrapper<ObjectPolicyConfigurationType>> postSearch(
					List<ContainerValueWrapper<ObjectPolicyConfigurationType>> items) {
				return getObjects();
			}
			
			@Override
			protected void newItemPerformed(AjaxRequestTarget target) {
				newObjectPolicyClickPerformed(target);
			}
			
			@Override
			protected void initPaging() {
				ObjectPolicyConfigurationTabPanel.this.initPaging(); 
			}
			
			@Override
			protected boolean enableActionNewObject() {
				return true;
			}
			
			@Override
			protected ObjectQuery createQuery() {
			        return ObjectPolicyConfigurationTabPanel.this.createQuery();
			}
			
			@Override
			protected List<IColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>> createColumns() {
				return initBasicColumns();
			}

			@Override
			protected MultivalueContainerDetailsPanel<ObjectPolicyConfigurationType> getMultivalueContainerDetailsPanel(
					ListItem<ContainerValueWrapper<ObjectPolicyConfigurationType>> item) {
				return ObjectPolicyConfigurationTabPanel.this.getMultivalueContainerDetailsPanel(item);
			}

			@Override
			protected List<SearchItemDefinition> initSearchableItems(
					PrismContainerDefinition<ObjectPolicyConfigurationType> containerDef) {
				List<SearchItemDefinition> defs = new ArrayList<>();
				
				SearchFactory.addSearchRefDef(containerDef, new ItemPath(ObjectPolicyConfigurationType.F_OBJECT_TEMPLATE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
				SearchFactory.addSearchPropertyDef(containerDef, new ItemPath(ObjectPolicyConfigurationType.F_SUBTYPE), defs);
				SearchFactory.addSearchPropertyDef(containerDef, new ItemPath(ObjectPolicyConfigurationType.F_LIFECYCLE_STATE_MODEL, LifecycleStateModelType.F_STATE, LifecycleStateType.F_NAME), defs);
				
				return defs;
			}
		};
		add(multivalueContainerListPanel);
		setOutputMarkupId(true);
	}
    
    private List<ContainerValueWrapper<ObjectPolicyConfigurationType>> getObjects() {
    	return getModelObject().getValues();
    }
    
    protected void newObjectPolicyClickPerformed(AjaxRequestTarget target) {
        PrismContainerValue<ObjectPolicyConfigurationType> newObjectPolicy = getModel().getObject().getItem().createNewValue();
        ContainerValueWrapper<ObjectPolicyConfigurationType> newObjectPolicyWrapper = getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newObjectPolicy, getModel());
        newObjectPolicyWrapper.setShowEmpty(true, false);
        newObjectPolicyWrapper.computeStripes();
        getMultivalueContainerListPanel().itemDetailsPerformed(target, Arrays.asList(newObjectPolicyWrapper));
	}
    
    private MultivalueContainerDetailsPanel<ObjectPolicyConfigurationType> getMultivalueContainerDetailsPanel(
			ListItem<ContainerValueWrapper<ObjectPolicyConfigurationType>> item) {
    	MultivalueContainerDetailsPanel<ObjectPolicyConfigurationType> detailsPanel = new  MultivalueContainerDetailsPanel<ObjectPolicyConfigurationType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected DisplayNamePanel<ObjectPolicyConfigurationType> createDisplayNamePanel(String displayNamePanelId) {
				ContainerRealValueModel<ObjectPolicyConfigurationType> displayNameModel = 
						new ContainerRealValueModel<ObjectPolicyConfigurationType>(item.getModel().getObject());
				return new DisplayNamePanel<ObjectPolicyConfigurationType>(displayNamePanelId, displayNameModel);
			}
		};
		return detailsPanel;
	}
    
	private MultivalueContainerListPanelWithDetailsPanel<ObjectPolicyConfigurationType> getMultivalueContainerListPanel(){
		return ((MultivalueContainerListPanelWithDetailsPanel<ObjectPolicyConfigurationType>)get(ID_OBJECTS_POLICY));
	}
    
    private ObjectQuery createQuery() {
    	return QueryBuilder.queryFor(ObjectPolicyConfigurationType.class, getPageBase().getPrismContext())
    			.all()
                .build();
    }
    
    private void initPaging() {
    	getPageBase().getSessionStorage().getObjectPoliciesConfigurationTabStorage().setPaging(ObjectPaging.createPaging(0, (int) ((PageBase)getPage()).getItemsPerPage(UserProfileStorage.TableId.OBJECT_POLICIES_TAB_TABLE)));
    }
    
    private List<IColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>> initBasicColumns() {
		List<IColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>> columns = new ArrayList<>();

		columns.add(new CheckBoxHeaderColumn<>());
		columns.add(new IconColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				return Model.of(WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE));
			}
		});
		
		columns.add(new LinkColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>>(createStringResource("ObjectPolicyConfigurationTabPanel.type")){
            private static final long serialVersionUID = 1L;

            
			@Override
			public IModel<String> createLinkModel(IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				PropertyWrapperFromContainerModel<QName, ObjectPolicyConfigurationType> property =
            			new PropertyWrapperFromContainerModel<QName, ObjectPolicyConfigurationType>(rowModel.getObject(), ObjectPolicyConfigurationType.F_TYPE);
            	ItemRealValueModel<QName> typeValue = null;
            	if(property.getObject() != null) {
            		typeValue = new ItemRealValueModel<QName>(property.getObject()) {
            			
            			private static final long serialVersionUID = 1L;

    					@Override
    					protected String objectToString(QName object) {
    						return object.getLocalPart();
    					}
            		};
            	}
				return Model.of(typeValue != null && !StringUtils.isBlank(typeValue.getAsString()) ? typeValue.getAsString() : "");
			}
			
			@Override
            public void onClick(AjaxRequestTarget target, IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
            	getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
            }
        });

		columns.add(new AbstractColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>(createStringResource("ObjectPolicyConfigurationTabPanel.subtype")){
            private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				PropertyWrapperFromContainerModel<String, ObjectPolicyConfigurationType> property =
            			new PropertyWrapperFromContainerModel<String, ObjectPolicyConfigurationType>(rowModel.getObject(), ObjectPolicyConfigurationType.F_SUBTYPE);
            	ItemRealValueModel<String> subtype = null;
            	if(property.getObject() != null) {
            		subtype = new ItemRealValueModel<String>(property.getObject());
            	}
            	item.add(new Label(componentId, Model.of(StringUtils.isBlank(subtype.getAsString()) ? "" : subtype.getAsString())));
			}
        });

		columns.add(new AbstractColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>(createStringResource("ObjectPolicyConfigurationTabPanel.objectTemplate")){
            private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				
				DefaultReferencableImplSingleValueContainerValueWrapperModel<ObjectPolicyConfigurationType> defaultReferencableImpl = 
						new DefaultReferencableImplSingleValueContainerValueWrapperModel<ObjectPolicyConfigurationType>(rowModel, ObjectPolicyConfigurationType.F_OBJECT_TEMPLATE_REF);
				item.add(new Label(componentId, Model.of(WebComponentUtil.getReferencedObjectDisplayNamesAndNames(defaultReferencableImpl.getObject(), false))));
			}
        });
		
		columns.add(new AbstractColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>(createStringResource("ObjectPolicyConfigurationTabPanel.lifecycleState")){
            private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				
				ContainerRealValueModel<LifecycleStateModelType> lifecycleStateModel =
            			new ContainerRealValueModel<LifecycleStateModelType>(rowModel.getObject(),
            					new ItemPath(rowModel.getObject().getPath(), ObjectPolicyConfigurationType.F_LIFECYCLE_STATE_MODEL));
				
				if (lifecycleStateModel == null || lifecycleStateModel.getObject() == null
						|| lifecycleStateModel.getObject().getState() == null || lifecycleStateModel.getObject().getState().isEmpty()) {
					item.add(new Label(componentId, createStringResource("ObjectPolicyConfigurationTabPanel.lifecycleState.value.no")));
				} else {
					item.add(new Label(componentId, createStringResource("ObjectPolicyConfigurationTabPanel.lifecycleState.value.yes")));
				}
			}
        });
		
		List<InlineMenuItem> menuActionsList = getMultivalueContainerListPanel().getDefaultMenuActions();
		columns.add(new InlineMenuButtonColumn<>(menuActionsList, getPageBase()));
		
        return columns;
	}
}

