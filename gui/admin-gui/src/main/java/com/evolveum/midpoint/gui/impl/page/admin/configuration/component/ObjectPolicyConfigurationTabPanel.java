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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
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
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
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
public class ObjectPolicyConfigurationTabPanel extends BasePanel<ObjectWrapper<SystemConfigurationType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(ObjectPolicyConfigurationTabPanel.class);
	
    private static final String ID_OBJECTS_POLICY = "objectsPolicy";
    private static final String ID_SEARCH_FRAGMENT = "searchFragment";
    protected static final String ID_SPECIFIC_CONTAINERS_FRAGMENT = "specificContainersFragment";
    protected static final String ID_SPECIFIC_CONTAINER = "specificContainers";
    
    private LoadableModel<ContainerWrapper<ObjectPolicyConfigurationType>> model;
    private List<ContainerValueWrapper<ObjectPolicyConfigurationType>> detailsPanelObjectPoliciesList = new ArrayList<>();
    
    public ObjectPolicyConfigurationTabPanel(String id, IModel<ObjectWrapper<SystemConfigurationType>> model) {
        super(id, model);
        this.model = new LoadableModel<ContainerWrapper<ObjectPolicyConfigurationType>>(false) {

			private static final long serialVersionUID = 1L;
			
			@Override
			protected ContainerWrapper<ObjectPolicyConfigurationType> load() {
				
				ContainerWrapperFromObjectWrapperModel<ObjectPolicyConfigurationType, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getModel(), 
						new ItemPath(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION));
				
				return model.getObject();
			}
			
		};
		
    }

    @Override
    protected void onInitialize() {
    		super.onInitialize();
    		initLayout();
    }
    
    protected void initLayout() {
    	
    	TableId tableId = UserProfileStorage.TableId.OBJECT_POLICIES_TAB_TABLE;
    	int itemPerPage = (int) ((PageBase)ObjectPolicyConfigurationTabPanel.this.getPage()).getItemsPerPage(UserProfileStorage.TableId.OBJECT_POLICIES_TAB_TABLE);
    	PageStorage pageStorage = ((PageBase)ObjectPolicyConfigurationTabPanel.this.getPage()).getSessionStorage().getObjectPoliciesConfigurationTabStorage();
    	
    	MultivalueContainerListPanel<ObjectPolicyConfigurationType> multivalueContainerListPanel = new MultivalueContainerListPanel<ObjectPolicyConfigurationType>(ID_OBJECTS_POLICY, model,
    			tableId, itemPerPage, pageStorage) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected List<ContainerValueWrapper<ObjectPolicyConfigurationType>> postSearch(
					List<ContainerValueWrapper<ObjectPolicyConfigurationType>> items) {
				return items;
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
			protected Fragment getSearchPanel(String contentAreaId) {
				return new Fragment(contentAreaId, ID_SEARCH_FRAGMENT, ObjectPolicyConfigurationTabPanel.this);
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
		};
		
		add(multivalueContainerListPanel);
		
		setOutputMarkupId(true);
	}
    
    protected void newObjectPolicyClickPerformed(AjaxRequestTarget target) {
        PrismContainerValue<ObjectPolicyConfigurationType> newObjectPolicy = model.getObject().getItem().createNewValue();
        ContainerValueWrapper<ObjectPolicyConfigurationType> newObjectPolicyWrapper = getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newObjectPolicy, model);
        newObjectPolicyWrapper.setShowEmpty(true, false);
        newObjectPolicyWrapper.computeStripes();
        getMultivalueContainerListPanel().itemDetailsPerformed(target, Arrays.asList(newObjectPolicyWrapper));
	}
    
    private MultivalueContainerDetailsPanel<ObjectPolicyConfigurationType> getMultivalueContainerDetailsPanel(
			ListItem<ContainerValueWrapper<ObjectPolicyConfigurationType>> item) {
    	MultivalueContainerDetailsPanel<ObjectPolicyConfigurationType> detailsPanel = new  MultivalueContainerDetailsPanel<ObjectPolicyConfigurationType>(MultivalueContainerListPanel.ID_ITEM_DETAILS, item.getModel()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected DisplayNamePanel<ObjectPolicyConfigurationType> createDisplayNamePanel(String displayNamePanelId) {
				IModel<ObjectPolicyConfigurationType> displayNameModel = new AbstractReadOnlyModel<ObjectPolicyConfigurationType>() {

		    		private static final long serialVersionUID = 1L;

					@Override
		    		public ObjectPolicyConfigurationType getObject() {
		    			return item.getModelObject().getContainerValue().getValue();
		    		}

		    	};
				return new DisplayNamePanel<ObjectPolicyConfigurationType>(displayNamePanelId, displayNameModel);
			}

			@Override
			protected  Fragment getSpecificContainers(String contentAreaId) {
				Fragment specificContainers = new Fragment(contentAreaId, ID_SPECIFIC_CONTAINERS_FRAGMENT, ObjectPolicyConfigurationTabPanel.this);
				
//				Form form = this.findParent(Form.class);
//				
//				ContainerWrapper constraintsContainer = item.getModelObject().findContainerWrapper(new ItemPath(item.getModelObject().getPath(), ObjectPolicyConfigurationType.F_PROPERTY_CONSTRAINT));
////				if (constraintsContainer != null){
////					constraintsContainer.getValues().forEach(value ->
////							((ContainerValueWrapper)value).getItems().forEach(
////									constraintContainerItem -> {
////										if (constraintContainerItem instanceof ContainerWrapper && ((ContainerWrapper) constraintContainerItem).getItemDefinition().isMultiValue()){
////											((ContainerWrapper) constraintContainerItem).setRemoveContainerButtonVisible(true);
////										}
////									}
////							));
////				}
//				constraintsContainer.setShowEmpty(true, false);
//				constraintsContainer.setAddContainerButtonVisible(true);
//				
//				
//				IModel<ContainerWrapper<ConflictResolutionType>> conflictResolutionModel = new AbstractReadOnlyModel<ContainerWrapper<ConflictResolutionType>>() {
//
//		    		private static final long serialVersionUID = 1L;
//
//					@Override
//		    		public ContainerWrapper<ConflictResolutionType> getObject() {
//		    			return constraintsContainer;
//		    		}
//
//		    	};
//				
//				PrismContainerPanel<ConflictResolutionType> specificContainer = new PrismContainerPanel<ConflictResolutionType>(ID_SPECIFIC_CONTAINER, conflictResolutionModel, true, form, null, getPageBase());
//				specificContainer.setVisible(true);
////				PrismPanel<SystemConfigurationType> panel = new PrismPanel<SystemConfigurationType>(ID_SYSTEM_CONFIG, 
////				new ContainerWrapperListFromObjectWrapperModel(getModel(), getVisibleContainers()), null, form, null, getPageBase());
////				add(panel);
////				
////				ItemPath assignmentPath = getModelObject().getContainerValue().getValue().asPrismContainerValue().getPath();
////				ContainerWrapperFromObjectWrapperModel<ActivationType, FocusType> activationModel = new ContainerWrapperFromObjectWrapperModel<>(((PageAdminObjectDetails<FocusType>)getPageBase()).getObjectModel(), assignmentPath.append(AssignmentType.F_ACTIVATION));
////				PrismContainerPanel<ActivationType> acitvationContainer = new PrismContainerPanel(ID_ACTIVATION_PANEL, Model.of(activationModel), true, form, itemWrapper -> getActivationVisibileItems(itemWrapper.getPath(), assignmentPath), getPageBase());
//				specificContainers.add(specificContainer);
				
				return specificContainers;
			}
			
			@Override
			protected ContainerValuePanel<ObjectPolicyConfigurationType> getBasicContainerValuePanel(
					String idPanel) {
				
				Form form = new Form<>("form");
		    	ItemPath itemPath = getModelObject().getPath();
		    	ContainerValueWrapper<ObjectPolicyConfigurationType> modelObject = item.getModelObject();
		    	modelObject.setShowEmpty(true, true);
		    	
		    	ContainerWrapper<PropertyConstraintType> propertyConstraintModel = modelObject.findContainerWrapper(new ItemPath(modelObject.getPath(), ObjectPolicyConfigurationType.F_PROPERTY_CONSTRAINT));
		    	propertyConstraintModel.setShowEmpty(true, false);
//		    	propertyConstraintModel.setAddContainerButtonVisible(true);
		    	
				return new ContainerValuePanel<ObjectPolicyConfigurationType>(idPanel, Model.of(modelObject), true, form,
						itemWrapper -> super.getBasicTabVisibity(itemWrapper, itemPath), getPageBase());
			}
		
		};
		return detailsPanel;
	}
    
	private MultivalueContainerListPanel<ObjectPolicyConfigurationType> getMultivalueContainerListPanel(){
		return ((MultivalueContainerListPanel<ObjectPolicyConfigurationType>)get(ID_OBJECTS_POLICY));
	}
    
    private ObjectQuery createQuery() {
    	TypeFilter filter = TypeFilter.createType(ObjectPolicyConfigurationType.COMPLEX_TYPE, new AllFilter());
    	return ObjectQuery.createObjectQuery(filter);
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
				return new AbstractReadOnlyModel<String>() {

					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE);
					}
				};
			}

		});
		
		columns.add(new AbstractColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>(createStringResource("ObjectPolicyConfigurationTabPanel.type")){
            private static final long serialVersionUID = 1L;

            
			@Override
			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				QName typeValue = WebComponentUtil.getValue(rowModel.getObject().getContainerValue(), ObjectPolicyConfigurationType.F_TYPE, QName.class);
				item.add(new Label(componentId, Model.of(typeValue != null ? typeValue.getLocalPart() : "")));
			}
        });

		columns.add(new AbstractColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>(createStringResource("ObjectPolicyConfigurationTabPanel.subtype")){
            private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				String subtypeValue = WebComponentUtil.getValue(rowModel.getObject().getContainerValue(), ObjectPolicyConfigurationType.F_SUBTYPE, String.class);
				item.add(new Label(componentId, Model.of(subtypeValue)));
			}
        });

		columns.add(new AbstractColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>(createStringResource("ObjectPolicyConfigurationTabPanel.objectTemplate")){
            private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				
				ObjectReferenceType objectTemplate = rowModel.getObject().getContainerValue().getValue().getObjectTemplateRef();
				
				if(objectTemplate != null) {
					String objectTemplateNameValue = WebModelServiceUtils.resolveReferenceName(objectTemplate, getPageBase());
					item.add(new Label(componentId, Model.of(objectTemplateNameValue)));
				} else {
					item.add(new Label(componentId, Model.of("")));
				}
			}
        });
		
		columns.add(new AbstractColumn<ContainerValueWrapper<ObjectPolicyConfigurationType>, String>(createStringResource("ObjectPolicyConfigurationTabPanel.lifecycleState")){
            private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<ContainerValueWrapper<ObjectPolicyConfigurationType>>> item, String componentId,
									 final IModel<ContainerValueWrapper<ObjectPolicyConfigurationType>> rowModel) {
				
				ContainerWrapper<Containerable> lifecycleState = rowModel.getObject().findContainerWrapper(ObjectPolicyConfigurationType.F_LIFECYCLE_STATE_MODEL);
				if(lifecycleState == null) {
					item.add(new Label(componentId, createStringResource("ObjectPolicyConfigurationTabPanel.lifecycleState.value.no")));
				} else {
					item.add(new Label(componentId, createStringResource("ObjectPolicyConfigurationTabPanel.lifecycleState.value.yes")));
				}
			}
        });
		
		List<InlineMenuItem> menuActionsList = getObjectPolicyMenuActions();
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
    
    private List<InlineMenuItem> getObjectPolicyMenuActions() {
		List<InlineMenuItem> menuItems = new ArrayList<>();
		PrismObject obj = getMultivalueContainerListPanel().getFocusObject();
//		LOGGER.info("XXXXXXXXXXXXXXXXXXX obj.name: " + obj.getName().toString());
		menuItems.add(new InlineMenuItem(createStringResource("PageBase.button.unassign"), new Model<>(true),
			new Model<>(true), false, getMultivalueContainerListPanel().createDeleteColumnAction(), 0, GuiStyleConstants.CLASS_DELETE_MENU_ITEM,
			DoubleButtonColumn.BUTTON_COLOR_CLASS.DANGER.toString()));

		menuItems.add(new InlineMenuItem(createStringResource("PageBase.button.edit"), new Model<>(true),
            new Model<>(true), false, getMultivalueContainerListPanel().createEditColumnAction(), 1, GuiStyleConstants.CLASS_EDIT_MENU_ITEM,
			DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString()));
		return menuItems;
	}
}

