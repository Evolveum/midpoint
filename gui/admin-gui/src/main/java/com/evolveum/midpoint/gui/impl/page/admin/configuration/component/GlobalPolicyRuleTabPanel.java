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

import com.evolveum.midpoint.prism.path.ItemPath;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.model.RealContainerValueFromParentOfSingleValueContainerValueWrapperModel;
import com.evolveum.midpoint.gui.impl.model.RealValueOfSingleValuePropertyAsStringFromContainerValueWrapperModel;
import com.evolveum.midpoint.gui.impl.model.RealContainerValueFromContainerValueWrapperModel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
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
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
public class GlobalPolicyRuleTabPanel extends BasePanel<ContainerWrapper<GlobalPolicyRuleType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(GlobalPolicyRuleTabPanel.class);
	
    private static final String ID_GLOBAL_POLICY_RULE = "globalPolicyRule";
    
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
    	PageStorage pageStorage = ((PageBase)GlobalPolicyRuleTabPanel.this.getPage()).getSessionStorage().getObjectPoliciesConfigurationTabStorage();
    	
    	MultivalueContainerListPanelWithDetailsPanel<GlobalPolicyRuleType> multivalueContainerListPanel = new MultivalueContainerListPanelWithDetailsPanel<GlobalPolicyRuleType>(ID_GLOBAL_POLICY_RULE, getModel(),
    			tableId, pageStorage) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected List<ContainerValueWrapper<GlobalPolicyRuleType>> postSearch(
					List<ContainerValueWrapper<GlobalPolicyRuleType>> items) {
				return getObjects();
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

			@Override
			protected List<SearchItemDefinition> initSearchableItems(
					PrismContainerDefinition<GlobalPolicyRuleType> containerDef) {
				List<SearchItemDefinition> defs = new ArrayList<>();
				
				SearchFactory.addSearchPropertyDef(containerDef, ItemPath
						.create(GlobalPolicyRuleType.F_FOCUS_SELECTOR, ObjectSelectorType.F_SUBTYPE), defs);
				SearchFactory.addSearchRefDef(containerDef, 
						ItemPath.create(GlobalPolicyRuleType.F_POLICY_CONSTRAINTS,
								PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF), defs, AreaCategoryType.POLICY, getPageBase());
				
				defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));
				
				return defs;
			}
		};
		
		add(multivalueContainerListPanel);
		
		setOutputMarkupId(true);
	}
    
    private List<ContainerValueWrapper<GlobalPolicyRuleType>> getObjects() {
    	return getModelObject().getValues();
    }
    
    protected void newGlobalPolicuRuleClickPerformed(AjaxRequestTarget target) {
        PrismContainerValue<GlobalPolicyRuleType> newObjectPolicy = getModel().getObject().getItem().createNewValue();
        ContainerValueWrapper<GlobalPolicyRuleType> newObjectPolicyWrapper = getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newObjectPolicy, getModel());
        newObjectPolicyWrapper.setShowEmpty(true, true);
        newObjectPolicyWrapper.computeStripes();
        getMultivalueContainerListPanel().itemDetailsPerformed(target, Arrays.asList(newObjectPolicyWrapper));
	}
    
    private MultivalueContainerDetailsPanel<GlobalPolicyRuleType> getMultivalueContainerDetailsPanel(
			ListItem<ContainerValueWrapper<GlobalPolicyRuleType>> item) {
    	MultivalueContainerDetailsPanel<GlobalPolicyRuleType> detailsPanel = new  MultivalueContainerDetailsPanel<GlobalPolicyRuleType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected DisplayNamePanel<GlobalPolicyRuleType> createDisplayNamePanel(String displayNamePanelId) {
				RealContainerValueFromContainerValueWrapperModel<GlobalPolicyRuleType> displayNameModel = 
						new RealContainerValueFromContainerValueWrapperModel<GlobalPolicyRuleType>(item.getModel());
				return new DisplayNamePanel<GlobalPolicyRuleType>(displayNamePanelId, displayNameModel);
			}
			
		};
		return detailsPanel;
	}
    
	private MultivalueContainerListPanelWithDetailsPanel<GlobalPolicyRuleType> getMultivalueContainerListPanel(){
		return ((MultivalueContainerListPanelWithDetailsPanel<GlobalPolicyRuleType>)get(ID_GLOBAL_POLICY_RULE));
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
            	RealValueOfSingleValuePropertyAsStringFromContainerValueWrapperModel<String, GlobalPolicyRuleType> name = 
            			new RealValueOfSingleValuePropertyAsStringFromContainerValueWrapperModel<String, GlobalPolicyRuleType>(rowModel, GlobalPolicyRuleType.F_NAME);
           		if (name == null || StringUtils.isBlank(name.getObject())) {
            		return createStringResource("AssignmentPanel.noName");
            	}
            	return name;
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
            	RealContainerValueFromParentOfSingleValueContainerValueWrapperModel<PolicyConstraintsType, GlobalPolicyRuleType> constraintsModel =
			            new RealContainerValueFromParentOfSingleValueContainerValueWrapperModel<>(rowModel,
					            rowModel.getObject().getPath().append(GlobalPolicyRuleType.F_POLICY_CONSTRAINTS));
            	String constraints = "";
            	if(constraintsModel != null && constraintsModel.getObject() != null) {
            		constraints = PolicyRuleTypeUtil.toShortString(constraintsModel.getObject());
            	}
                cellItem.add(new MultiLineLabel(componentId, Model.of(constraints != null && !constraints.equals("null") ? constraints : "")));
            }

        });
        columns.add(new AbstractColumn<ContainerValueWrapper<GlobalPolicyRuleType>, String>(createStringResource("PolicyRulesPanel.situationColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<GlobalPolicyRuleType>>> cellItem, String componentId,
                                     final IModel<ContainerValueWrapper<GlobalPolicyRuleType>> rowModel) {
            	RealValueOfSingleValuePropertyAsStringFromContainerValueWrapperModel<String, GlobalPolicyRuleType> situationValue = 
            			new RealValueOfSingleValuePropertyAsStringFromContainerValueWrapperModel<String, GlobalPolicyRuleType>(rowModel, GlobalPolicyRuleType.F_POLICY_SITUATION);
                cellItem.add(new Label(componentId, Model.of(situationValue)));
            }

        });
        columns.add(new AbstractColumn<ContainerValueWrapper<GlobalPolicyRuleType>, String>(createStringResource("PolicyRulesPanel.actionColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<GlobalPolicyRuleType>>> cellItem, String componentId,
                                     final IModel<ContainerValueWrapper<GlobalPolicyRuleType>> rowModel) {
            	RealContainerValueFromParentOfSingleValueContainerValueWrapperModel<PolicyActionsType, GlobalPolicyRuleType> constraintsModel =
			            new RealContainerValueFromParentOfSingleValueContainerValueWrapperModel<>(rowModel,
					            rowModel.getObject().getPath().append(GlobalPolicyRuleType.F_POLICY_ACTIONS));
            	String action = "";
            	if(constraintsModel != null && constraintsModel.getObject() != null) {
            		action = PolicyRuleTypeUtil.toShortString(constraintsModel.getObject(),  new ArrayList<>());
            	}
                cellItem.add(new MultiLineLabel(componentId, Model.of(action != null && !action.equals("null") ? action : "")));
            }

        });
		
		List<InlineMenuItem> menuActionsList = getMultivalueContainerListPanel().getDefaultMenuActions();
		columns.add(new InlineMenuButtonColumn<>(menuActionsList, getPageBase()));
		
        return columns;
	}
}

