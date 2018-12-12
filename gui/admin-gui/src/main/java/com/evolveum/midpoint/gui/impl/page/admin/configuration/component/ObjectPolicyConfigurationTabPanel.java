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
import org.apache.wicket.AttributeModifier;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.EditablePrismPropertyColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LinkPrismPropertyColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.StaticPrismPropertyColumn;
import com.evolveum.midpoint.gui.impl.component.prism.StaticItemWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.model.PropertyOrReferenceWrapperFromContainerModel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
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
				
				SearchFactory.addSearchRefDef(containerDef, ObjectPolicyConfigurationType.F_OBJECT_TEMPLATE_REF, defs, AreaCategoryType.ADMINISTRATION, getPageBase());
				SearchFactory.addSearchPropertyDef(containerDef, ObjectPolicyConfigurationType.F_SUBTYPE, defs);
				SearchFactory.addSearchPropertyDef(containerDef, ItemPath
						.create(ObjectPolicyConfigurationType.F_LIFECYCLE_STATE_MODEL, LifecycleStateModelType.F_STATE, LifecycleStateType.F_NAME), defs);
				
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
				ItemRealValueModel<ObjectPolicyConfigurationType> displayNameModel = 
						new ItemRealValueModel<ObjectPolicyConfigurationType>(item.getModel().getObject());
				return new DisplayNamePanel<ObjectPolicyConfigurationType>(displayNamePanelId, displayNameModel);
			}
		};
		return detailsPanel;
	}
    
	private MultivalueContainerListPanelWithDetailsPanel<ObjectPolicyConfigurationType> getMultivalueContainerListPanel(){
		return ((MultivalueContainerListPanelWithDetailsPanel<ObjectPolicyConfigurationType>)get(ID_OBJECTS_POLICY));
	}
    
    private ObjectQuery createQuery() {
    	return getPageBase().getPrismContext().queryFor(ObjectPolicyConfigurationType.class)
    			.all()
                .build();
    }
    
    private void initPaging() {
    	getPageBase().getSessionStorage().getObjectPoliciesConfigurationTabStorage().setPaging(
    			getPrismContext().queryFactory().createPaging(0, (int) ((PageBase)getPage()).getItemsPerPage(UserProfileStorage.TableId.OBJECT_POLICIES_TAB_TABLE)));
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
		
		columns.add(new LinkPrismPropertyColumn(getModel(), ObjectPolicyConfigurationType.F_TYPE, getPageBase()) {
			
			@Override
			protected void onClick(AjaxRequestTarget target, IModel rowModel) {
				getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
			}
			
			@Override
			protected IModel populatePropertyItem(ValueWrapper object) {
				return new IModel<String>() {

					@Override
					public String getObject() {
						ItemRealValueModel<QName> value =  new ItemRealValueModel<QName>(object);
						return value == null ? null : value.getObject().getLocalPart();
					}
				};
			}
		});
		
		columns.add(new StaticPrismPropertyColumn(getModel(), ObjectPolicyConfigurationType.F_SUBTYPE, getPageBase()));
		
		columns.add(new StaticPrismPropertyColumn(getModel(), ObjectPolicyConfigurationType.F_OBJECT_TEMPLATE_REF, getPageBase()));

		columns.add(new AbstractItemWrapperColumn<ObjectPolicyConfigurationType>(getModel(), ObjectPolicyConfigurationType.F_LIFECYCLE_STATE_MODEL, getPageBase()){
            private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				ContainerWrapper lifecycleStateModel = rowModel.getObject().findContainerWrapperByName(ObjectPolicyConfigurationType.F_LIFECYCLE_STATE_MODEL);
				
				Label label = null;
				if (lifecycleStateModel == null || lifecycleStateModel.getValues() == null || lifecycleStateModel.getValues().isEmpty()) {
					item.add(new Label(componentId, ""));
				} else {
					ContainerWrapper lifecycleState = lifecycleStateModel.findContainerWrapperByName(LifecycleStateModelType.F_STATE);
					if (lifecycleState == null || lifecycleState.getValues() == null || lifecycleState.getValues().isEmpty()) {
						item.add(new Label(componentId, ""));
					} else {
						item.add(new StaticItemWrapperColumnPanel(componentId, new PropertyModel(lifecycleState, ""), new Form("form"), null, getPageBase()) {
							protected IModel populateContainerItem(ContainerValueWrapper object) {
								return Model.of(object != null ? WebComponentUtil.getDisplayName(object.getContainerValue()) : "");
							};
						});
					}
				}
			}
        });
		
		List<InlineMenuItem> menuActionsList = getMultivalueContainerListPanel().getDefaultMenuActions();
		columns.add(new InlineMenuButtonColumn<>(menuActionsList, getPageBase()));
		
        return columns;
	}
}

