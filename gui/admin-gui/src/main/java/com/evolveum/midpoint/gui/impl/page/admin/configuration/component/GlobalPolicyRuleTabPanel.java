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
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ContainerValuePanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapperFactory;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
public class GlobalPolicyRuleTabPanel extends BasePanel<ContainerWrapper<GlobalPolicyRuleType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(GlobalPolicyRuleTabPanel.class);
	
    private static final String ID_GLOBAL_POLICY_RULE = "globalPolicyRule";
    private static final String ID_SEARCH_FRAGMENT = "searchFragment";
    protected static final String ID_SPECIFIC_CONTAINERS_FRAGMENT = "specificContainersFragment";
    protected static final String ID_SPECIFIC_CONTAINER = "specificContainers";
    
    private List<ContainerValueWrapper<GlobalPolicyRuleType>> detailsPanelObjectPoliciesList = new ArrayList<>();
    
    public GlobalPolicyRuleTabPanel(String id, IModel<ContainerWrapper<GlobalPolicyRuleType>> model) {
        super(id, model);
		
    }

    @Override
    protected void onInitialize() {
    		super.onInitialize();
    		initLayout();
    }
    
    protected void initLayout() {
    	
    	TableId tableId = UserProfileStorage.TableId.OBJECT_POLICIES_TAB_TABLE;
    	int itemPerPage = (int) ((PageBase)GlobalPolicyRuleTabPanel.this.getPage()).getItemsPerPage(UserProfileStorage.TableId.OBJECT_POLICIES_TAB_TABLE);
    	PageStorage pageStorage = ((PageBase)GlobalPolicyRuleTabPanel.this.getPage()).getSessionStorage().getObjectPoliciesConfigurationTabStorage();
    	
    	MultivalueContainerListPanel<GlobalPolicyRuleType> multivalueContainerListPanel = new MultivalueContainerListPanel<GlobalPolicyRuleType>(ID_GLOBAL_POLICY_RULE, getModel(),
    			tableId, itemPerPage, pageStorage) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected List<ContainerValueWrapper<GlobalPolicyRuleType>> postSearch(
					List<ContainerValueWrapper<GlobalPolicyRuleType>> items) {
				return items;
			}
			
			@Override
			protected void newItemPerformed(AjaxRequestTarget target) {
				newGlobalPolicuRuleClickPerformed(target);
			}
			
			@Override
			protected void initPaging() {
				GlobalPolicyRuleTabPanel.this.initPaging(); 
			}
			
			@Override
			protected Fragment getSearchPanel(String contentAreaId) {
				return new Fragment(contentAreaId, ID_SEARCH_FRAGMENT, GlobalPolicyRuleTabPanel.this);
			}
			
			@Override
			protected boolean enableActionNewObject() {
				return true;
			}
			
			@Override
			protected ObjectQuery createQuery() {
			        return GlobalPolicyRuleTabPanel.this.createQuery();
			}
			
			@Override
			protected List<IColumn<ContainerValueWrapper<GlobalPolicyRuleType>, String>> createColumns() {
				return initBasicColumns();
			}

			@Override
			protected MultivalueContainerDetailsPanel<GlobalPolicyRuleType> getMultivalueContainerDetailsPanel(
					ListItem<ContainerValueWrapper<GlobalPolicyRuleType>> item) {
				return GlobalPolicyRuleTabPanel.this.getMultivalueContainerDetailsPanel(item);
			}
		};
		
		add(multivalueContainerListPanel);
		
		setOutputMarkupId(true);
	}
    
    protected void newGlobalPolicuRuleClickPerformed(AjaxRequestTarget target) {
        PrismContainerValue<GlobalPolicyRuleType> newObjectPolicy = getModel().getObject().getItem().createNewValue();
        ContainerValueWrapper<GlobalPolicyRuleType> newObjectPolicyWrapper = getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newObjectPolicy, getModel());
        newObjectPolicyWrapper.setShowEmpty(true, false);
        newObjectPolicyWrapper.computeStripes();
        getMultivalueContainerListPanel().itemDetailsPerformed(target, Arrays.asList(newObjectPolicyWrapper));
	}
    
    private MultivalueContainerDetailsPanel<GlobalPolicyRuleType> getMultivalueContainerDetailsPanel(
			ListItem<ContainerValueWrapper<GlobalPolicyRuleType>> item) {
    	MultivalueContainerDetailsPanel<GlobalPolicyRuleType> detailsPanel = new  MultivalueContainerDetailsPanel<GlobalPolicyRuleType>(MultivalueContainerListPanel.ID_ITEM_DETAILS, item.getModel()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected DisplayNamePanel<GlobalPolicyRuleType> createDisplayNamePanel(String displayNamePanelId) {
				IModel<GlobalPolicyRuleType> displayNameModel = new AbstractReadOnlyModel<GlobalPolicyRuleType>() {

		    		private static final long serialVersionUID = 1L;

					@Override
		    		public GlobalPolicyRuleType getObject() {
		    			return item.getModelObject().getContainerValue().getValue();
		    		}

		    	};
				return new DisplayNamePanel<GlobalPolicyRuleType>(displayNamePanelId, displayNameModel);
			}

			@Override
			protected  Fragment getSpecificContainers(String contentAreaId) {
				Fragment specificContainers = new Fragment(contentAreaId, ID_SPECIFIC_CONTAINERS_FRAGMENT, GlobalPolicyRuleTabPanel.this);
				return specificContainers;
			}
		};
		return detailsPanel;
	}
    
	private MultivalueContainerListPanel<GlobalPolicyRuleType> getMultivalueContainerListPanel(){
		return ((MultivalueContainerListPanel<GlobalPolicyRuleType>)get(ID_GLOBAL_POLICY_RULE));
	}
    
    private ObjectQuery createQuery() {
    	TypeFilter filter = TypeFilter.createType(GlobalPolicyRuleType.COMPLEX_TYPE, new AllFilter());
    	return ObjectQuery.createObjectQuery(filter);
    }
    
    private void initPaging() {
    	getPageBase().getSessionStorage().getGlobalPolicyRulesTabStorage().setPaging(ObjectPaging.createPaging(0, (int) ((PageBase)getPage()).getItemsPerPage(UserProfileStorage.TableId.GLOBAL_POLICY_RULES_TAB_TABLE)));
    }
    
    private List<IColumn<ContainerValueWrapper<GlobalPolicyRuleType>, String>> initBasicColumns() {
		List<IColumn<ContainerValueWrapper<GlobalPolicyRuleType>, String>> columns = new ArrayList<>();

		columns.add(new CheckBoxHeaderColumn<>());

		columns.add(new IconColumn<ContainerValueWrapper<GlobalPolicyRuleType>>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(IModel<ContainerValueWrapper<GlobalPolicyRuleType>> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE);
					}
				};
			}

		});
		
		columns.add(new LinkColumn<ContainerValueWrapper<GlobalPolicyRuleType>>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<ContainerValueWrapper<GlobalPolicyRuleType>> rowModel) {
            	GlobalPolicyRuleType policyRuleType = rowModel.getObject().getContainerValue().getValue();
            	String name = policyRuleType.getName();
           		if (StringUtils.isBlank(name)) {
            		return createStringResource("AssignmentPanel.noName");
            	}
            	return Model.of(name);
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ContainerValueWrapper<GlobalPolicyRuleType>> rowModel) {
            	getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
            }
        });
		
        columns.add(new AbstractColumn<ContainerValueWrapper<GlobalPolicyRuleType>, String>(createStringResource("PolicyRulesPanel.constraintsColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<GlobalPolicyRuleType>>> cellItem, String componentId,
                                     final IModel<ContainerValueWrapper<GlobalPolicyRuleType>> rowModel) {
            	GlobalPolicyRuleType policyRuleType = rowModel.getObject().getContainerValue().getValue();
                String constraints = PolicyRuleTypeUtil.toShortString(policyRuleType.getPolicyConstraints());
                cellItem.add(new MultiLineLabel(componentId, Model.of(constraints != null && !constraints.equals("null") ? constraints : "")));
            }

        });
        columns.add(new AbstractColumn<ContainerValueWrapper<GlobalPolicyRuleType>, String>(createStringResource("PolicyRulesPanel.situationColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<GlobalPolicyRuleType>>> cellItem, String componentId,
                                     final IModel<ContainerValueWrapper<GlobalPolicyRuleType>> rowModel) {
            	GlobalPolicyRuleType globalPolicyRuleTypeModel = rowModel.getObject().getContainerValue().getValue();
                String situationValue = globalPolicyRuleTypeModel == null ? "" : globalPolicyRuleTypeModel.getPolicySituation();
                cellItem.add(new Label(componentId, Model.of(situationValue)));
            }

        });
        columns.add(new AbstractColumn<ContainerValueWrapper<GlobalPolicyRuleType>, String>(createStringResource("PolicyRulesPanel.actionColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<GlobalPolicyRuleType>>> cellItem, String componentId,
                                     final IModel<ContainerValueWrapper<GlobalPolicyRuleType>> rowModel) {
            	GlobalPolicyRuleType globalPolicyRuleTypeModel = rowModel.getObject().getContainerValue().getValue();
            	String action = PolicyRuleTypeUtil.toShortString(globalPolicyRuleTypeModel.getPolicyActions(), new ArrayList<>());
                cellItem.add(new MultiLineLabel(componentId, Model.of(action != null && !action.equals("null") ? action : "")));
            }

        });
		
		List<InlineMenuItem> menuActionsList = getMultivalueContainerListPanel().getDefaultMenuActions();
		columns.add(new InlineMenuButtonColumn<>(menuActionsList, menuActionsList.size(), getPageBase()));
		
        return columns;
	}
    
//    protected Fragment getSearchPanel(String contentAreaId){
//    	Fragment searchContainer = new Fragment(contentAreaId, ID_SEARCH_FRAGMENT, this);
//    	
//    	WebMarkupContainer typeContainer = new WebMarkupContainer(ID_TYPE_CONTAINER);
//        
//        typeContainer.setOutputMarkupId(true);
//        typeContainer.add(new VisibleEnableBehaviour() {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible() {
//                return ObjectPolicyConfigurationTabPanel.this.isTypeVisible();
//            }
//
//        });
//        searchContainer.addOrReplace(typeContainer);
//
//        this.model.getObject().findContainerWrapper(new ItemPath(ObjectPolicyConfigurationType.F_TYPE));
//    	DropDownChoicePanel<QName> type = new DropDownChoicePanel(ID_TYPE, 
//    			new IModel<QName>() {
//            		@Override
//            		public QName getObject() {
//            			return objectTypeValue;
//            		}
//
//            		@Override
//            		public void setObject(QName objectType) {
//            			objectTypeValue = objectType;
//            		}
//
//            		@Override
//            		public void detach() {
//
//            		}
//    			},
//    			new AbstractReadOnlyModel<List<QName>>() {
//
//					private static final long serialVersionUID = 1L;
//			
//					@Override
//					public List<QName> getObject() {
//						return WebComponentUtil.createFocusTypeList();
//					}
//				});
//        type.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected void onUpdate(AjaxRequestTarget target) {
////            	refreshTable(target);
//            }
//        });
//        type.setOutputMarkupId(true);
//        type.setOutputMarkupPlaceholderTag(true);
//        typeContainer.addOrReplace(type);
//    	
//    	return searchContainer;
//    }
}

