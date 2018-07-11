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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
public class ObjectPolicyConfigurationTabPanel extends BasePanel<ContainerWrapper<ObjectPolicyConfigurationType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(ObjectPolicyConfigurationTabPanel.class);
	
    private static final String ID_OBJECTS_POLICY = "objectsPolicy";
    private static final String ID_SEARCH_FRAGMENT = "searchFragment";
    private MultivalueContainerListPanel<ObjectPolicyConfigurationType> multivalueContainerListPanel;
    
    public ObjectPolicyConfigurationTabPanel(String id, IModel<ContainerWrapper<ObjectPolicyConfigurationType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
    		super.onInitialize();
    		initLayout();
    }
    
    protected void initLayout() {
    	
    	this.multivalueContainerListPanel = new MultivalueContainerListPanel<ObjectPolicyConfigurationType>(ID_OBJECTS_POLICY, getModel()) {
			
			@Override
			protected List<ContainerValueWrapper<ObjectPolicyConfigurationType>> postSearch(
					List<ContainerValueWrapper<ObjectPolicyConfigurationType>> items) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			protected void newAssignmentPerformed(AjaxRequestTarget target) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			protected void initPaging() {
				ObjectPolicyConfigurationTabPanel.this.initPaging(); 
			}
			
			@Override
			protected TableId getTableId() {
				return UserProfileStorage.TableId.OBJECT_POLICIES_TAB_TABLE;
			}
			
			@Override
			protected Fragment getSearchPanel(String contentAreaId) {
				return new Fragment(contentAreaId, ID_SEARCH_FRAGMENT, ObjectPolicyConfigurationTabPanel.this);
			}
			
			@Override
			protected PageStorage getPageStorage() {
				return ((PageBase)ObjectPolicyConfigurationTabPanel.this.getPage()).getSessionStorage().getObjectPoliciesConfigurationTabStorage();
			}
			
			@Override
			protected int getItemsPerPage() {
				return (int) ((PageBase)ObjectPolicyConfigurationTabPanel.this.getPage()).getItemsPerPage(UserProfileStorage.TableId.OBJECT_POLICIES_TAB_TABLE);
			}
			
			@Override
			protected boolean enableActionNewObject() {
				return false;
			}
			
			@Override
			protected ObjectQuery createQuery() {
			        return ObjectPolicyConfigurationTabPanel.this.createQuery();
			}
			
			@Override
			protected void createDetailsPanel(WebMarkupContainer itemsContainer) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			protected List<IColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>> createColumns() {
				return initBasicColumns();
			}
		};
		
		add(multivalueContainerListPanel);
		
		setOutputMarkupId(true);

//    	Form form = new Form<>("form");
//    	
//    	ContainerWrapperFromObjectWrapperModel<ObjectPolicyConfigurationType, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getModel(), new ItemPath(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION));
//		PrismContainerPanel<ObjectPolicyConfigurationType> panel = new PrismContainerPanel<>(ID_CONTAINER, model, true, form, null, getPageBase());
//		add(panel);
		
	}
    
    private ObjectQuery createQuery() {
    	TypeFilter filter = TypeFilter.createType(ObjectPolicyConfigurationType.COMPLEX_TYPE, new AllFilter());
    	return ObjectQuery.createObjectQuery(filter);
//        return QueryBuilder.queryFor(ObjectPolicyConfigurationType.class, getPageBase().getPrismContext())
//        		.build();
}
    
    private void initPaging() {
    	getPageBase().getSessionStorage().getObjectPoliciesConfigurationTabStorage().setPaging(ObjectPaging.createPaging(0, (int) ((PageBase)getPage()).getItemsPerPage(UserProfileStorage.TableId.OBJECT_POLICIES_TAB_TABLE)));
    }
    
    
    protected boolean isTypeVisible() {
    	return true;
    }
    
    private List<IColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>> initBasicColumns() {
		List<IColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>> columns = new ArrayList<>();

		columns.add(new CheckBoxHeaderColumn<>());

		columns.add(new IconColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE);
					}
				};
			}

		});
		
		columns.add(new AbstractColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>(createStringResource("AssignmentType.activation")){
            private static final long serialVersionUID = 1L;

            
			@Override
			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				String typeValue = WebComponentUtil.getValue(rowModel.getObject().getContainerValue(), ObjectPolicyConfigurationType.F_TYPE, String.class);
				LOGGER.info("XXXXXXXXXXXXXXXXXX typeValue: " + typeValue);
				item.add(new Label(componentId, Model.of(typeValue)));
			}
        });

//		columns.add(new AbstractColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>(createStringResource("AssignmentType.activation")){
//            private static final long serialVersionUID = 1L;
//
//			@Override
//			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
//									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
//				item.add(new Label(componentId, getActivationLabelModel(rowModel.getObject())));
//			}
//        });
//
//		columns.add(new AbstractColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>(createStringResource("AssignmentType.activation")){
//            private static final long serialVersionUID = 1L;
//
//			@Override
//			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
//									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
//				item.add(new Label(componentId, getActivationLabelModel(rowModel.getObject())));
//			}
//        });
//		
//		columns.add(new AbstractColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>(createStringResource("AssignmentType.activation")){
//            private static final long serialVersionUID = 1L;
//
//			@Override
//			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
//									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
//				item.add(new Label(componentId, getActivationLabelModel(rowModel.getObject())));
//			}
//        });
		
//        List<InlineMenuItem> menuActionsList = getAssignmentMenuActions();
//		columns.add(new InlineMenuButtonColumn<>(menuActionsList, menuActionsList.size(), getPageBase()));
        return columns;
	}
	
}

