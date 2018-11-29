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

package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.AssignmentPopup;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.users.component.AllAssignmentsPreviewDialog;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * Created by honchar.
 */
public class AbstractRoleAssignmentPanel extends AssignmentPanel {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(AssignmentPanel.class);

	private static final String ID_NEW_ITEM_BUTTON = "newItemButton";
    private static final String ID_SHOW_ALL_ASSIGNMENTS_BUTTON = "showAllAssignmentsButton";
    private static final String ID_BUTTON_TOOLBAR_FRAGMENT = "buttonToolbarFragment";

    protected static final String DOT_CLASS = AbstractRoleAssignmentPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_TARGET_REF_OBJECT = DOT_CLASS + "loadAssignmentTargetRefObject";

    public AbstractRoleAssignmentPanel(String id, IModel<ContainerWrapper<AssignmentType>> assignmentContainerWrapperModel){
    	super(id, assignmentContainerWrapperModel);
    }

    protected Fragment initCustomButtonToolbar(String contentAreaId){
    	Fragment searchContainer = new Fragment(contentAreaId, ID_BUTTON_TOOLBAR_FRAGMENT, this);
    	
    	AjaxIconButton newObjectIcon = getMultivalueContainerListPanel().getNewItemButton(ID_NEW_ITEM_BUTTON);
    	searchContainer.add(newObjectIcon);

    	AjaxIconButton showAllAssignmentsButton = new AjaxIconButton(ID_SHOW_ALL_ASSIGNMENTS_BUTTON, new Model<>("fa fa-address-card"),
                createStringResource("AssignmentTablePanel.menu.showAllAssignments")) {

        	private static final long serialVersionUID = 1L;

			@Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                showAllAssignments(ajaxRequestTarget);
            }
        };
        searchContainer.addOrReplace(showAllAssignmentsButton);
        showAllAssignmentsButton.setOutputMarkupId(true);
        showAllAssignmentsButton.add(new VisibleEnableBehaviour(){

            private static final long serialVersionUID = 1L;

            public boolean isVisible(){
                return showAllAssignmentsVisible();
            }
        });
        return searchContainer;
    }

    protected void showAllAssignments(AjaxRequestTarget target) {
        PageBase pageBase = getPageBase();
        List<AssignmentInfoDto> previewAssignmentsList;
        if (pageBase instanceof PageAdminFocus) {
            previewAssignmentsList = ((PageAdminFocus<?>) pageBase).showAllAssignmentsPerformed(target);
        } else {
	        previewAssignmentsList = Collections.emptyList();
        }
        AllAssignmentsPreviewDialog assignmentsDialog = new AllAssignmentsPreviewDialog(pageBase.getMainPopupBodyId(), previewAssignmentsList,
		        pageBase);
        pageBase.showMainPopup(assignmentsDialog, target);
    }

    @Override
    protected void newAssignmentClickPerformed(AjaxRequestTarget target) {
           AssignmentPopup popupPanel = new AssignmentPopup(getPageBase().getMainPopupBodyId()) {

    		   private static final long serialVersionUID = 1L;

               @Override
               protected void addPerformed(AjaxRequestTarget target, List newAssignmentsList) {
                   super.addPerformed(target, newAssignmentsList);
                   addSelectedAssignmentsPerformed(target, newAssignmentsList);
               }

               @Override
               protected List<ObjectTypes> getAvailableObjectTypesList(){
                   return getObjectTypesList();
               }
               
               @Override
            protected <F extends FocusType> PrismObject<F> getTargetedObject() {
				ObjectWrapper<F> w = AbstractRoleAssignmentPanel.this.getModelObject().getObjectWrapper();
				if (w == null) {
					return null;
				}
            	return w.getObject();
            }
               
           };
           popupPanel.setOutputMarkupId(true);
           getPageBase().showMainPopup(popupPanel, target);
    }

    protected void addSelectedAssignmentsPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList){
        if (newAssignmentsList == null || newAssignmentsList.isEmpty()) {
            warn(getParentPage().getString("AssignmentTablePanel.message.noAssignmentSelected"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        boolean isAssignmentsLimitReached = isAssignmentsLimitReached(newAssignmentsList != null ? newAssignmentsList.size() : 0, true);
        if (isAssignmentsLimitReached) {
            warn(getParentPage().getString("AssignmentPanel.assignmentsLimitReachedWarning", assignmentsRequestsLimit));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        newAssignmentsList.forEach(assignment -> {
        	   PrismContainerDefinition<AssignmentType> definition = getModelObject().getItem().getDefinition();
        	   PrismContainerValue<AssignmentType> newAssignment;
			try {
				newAssignment = definition.instantiate().createNewValue();
	        	   AssignmentType assignmentType = newAssignment.asContainerable();

	        	   if (assignment.getConstruction() != null && assignment.getConstruction().getResourceRef() != null) {
	        		   assignmentType.setConstruction(assignment.getConstruction());
	        	   } else {
	        		   assignmentType.setTargetRef(assignment.getTargetRef());
	        	   }
	        	   getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newAssignment, getModel());
	        	   getMultivalueContainerListPanel().refreshTable(target);
	        	   getMultivalueContainerListPanel().reloadSavePreviewButtons(target);
			} catch (SchemaException e) {
				getSession().error("Cannot create new assignment " + e.getMessage());
				target.add(getPageBase().getFeedbackPanel());
				target.add(this);
			}
        	   
           });

           
       }

    protected List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<ContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(
                createStringResource("AbstractRoleAssignmentPanel.relationKindIntentColumn")) {
            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel) {
                AssignmentType assignment = assignmentModel.getObject().getContainerValue().asContainerable();
                if (assignment.getConstruction() != null){
                    item.add(new Label(componentId, getKindIntentLabelValue(assignment.getConstruction())));
                } else {
                    item.add(new Label(componentId, getRelationLabelValue(assignmentModel.getObject())));
                }
            }
        });

        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("AssignmentType.tenant")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
            	
                item.add(new Label(componentId, getTenantLabelModel(rowModel.getObject())));
            }
        });
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("AssignmentType.orgReferenceShorten")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
                item.add(new Label(componentId, getOrgRefLabelModel(rowModel.getObject())));
            }
        });
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("AbstractRoleAssignmentPanel.identifierLabel")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
                item.add(new Label(componentId, getIdentifierLabelModel(rowModel.getObject())));
            }
        });

        return columns;
    }

    private String getRelationLabelValue(ContainerValueWrapper<AssignmentType> assignmentWrapper){
        if (assignmentWrapper == null || assignmentWrapper.getContainerValue() == null || assignmentWrapper.getContainerValue().getValue() == null
                || assignmentWrapper.getContainerValue().getValue().getTargetRef() == null
                || assignmentWrapper.getContainerValue().getValue().getTargetRef().getRelation() == null){
            return "";
        }
        return assignmentWrapper.getContainerValue().getValue().getTargetRef().getRelation().getLocalPart();
    }

    private String getKindIntentLabelValue(ConstructionType constructionType){
        if (constructionType == null){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(constructionType.getKind() != null && !StringUtils.isEmpty(constructionType.getKind().value()) ?
                constructionType.getKind().value() : createStringResource("AssignmentEditorPanel.undefined").getString());
        sb.append("/");
        sb.append(!StringUtils.isEmpty(constructionType.getIntent()) ? constructionType.getIntent()
                : createStringResource("AssignmentEditorPanel.undefined").getString());
        return sb.toString();
    }

    protected boolean showAllAssignmentsVisible(){
        return true;
    }

    protected void initCustomPaging(){
    	getAssignmentsTabStorage().setPaging(ObjectPaging.createPaging(0, (int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE)));
    }

	@Override
	protected TableId getTableId() {
		return UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE;
	}

    protected ObjectQuery createObjectQuery() {
	    Collection<QName> delegationRelations = getParentPage().getRelationRegistry()
			    .getAllRelationsFor(RelationKindType.DELEGATION);
        ObjectFilter deputyFilter = QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
                .item(AssignmentType.F_TARGET_REF)
                .ref(delegationRelations.toArray(new QName[0]))
                .buildFilter();
        ObjectQuery query = QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
                .not()
                .exists(AssignmentType.F_POLICY_RULE)
                .build();
        query.addFilter(NotFilter.createNot(deputyFilter));
        return query;
    }

    private IModel<String> getTenantLabelModel(ContainerValueWrapper<AssignmentType> assignmentContainer){
	    if (assignmentContainer == null || assignmentContainer.getContainerValue() == null){
	        return Model.of("");
        }
	    PropertyOrReferenceWrapper policyRuleWrapper = assignmentContainer.findPropertyWrapper(
			    ItemPath.create(assignmentContainer.getPath(), AssignmentType.F_TENANT_REF));
	    return Model.of(WebComponentUtil.getReferencedObjectDisplayNamesAndNames((DefaultReferencableImpl)((ValueWrapper<DefaultReferencableImpl>)policyRuleWrapper.getValues().get(0)).getValue().getRealValue(), false));
    }

    private IModel<String> getOrgRefLabelModel(ContainerValueWrapper<AssignmentType> assignmentContainer){
	    if (assignmentContainer == null || assignmentContainer.getContainerValue() == null){
	        return Model.of("");
        }
	    PropertyOrReferenceWrapper policyRuleWrapper = assignmentContainer.findPropertyWrapper(ItemPath.create(assignmentContainer.getPath(), AssignmentType.F_ORG_REF));
	    return Model.of(WebComponentUtil.getReferencedObjectDisplayNamesAndNames((DefaultReferencableImpl)((ValueWrapper<DefaultReferencableImpl>)policyRuleWrapper.getValues().get(0)).getValue().getRealValue(), false));

    }

    private <O extends ObjectType> IModel<String> getIdentifierLabelModel(ContainerValueWrapper<AssignmentType> assignmentContainer){
        if (assignmentContainer == null || assignmentContainer.getContainerValue() == null){
            return Model.of("");
        }
        AssignmentType assignment = assignmentContainer.getContainerValue().asContainerable();
        if (assignment.getTargetRef() == null){
            return Model.of("");
        }

        PrismObject<O> object = WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(),
                getPageBase().createSimpleTask(OPERATION_LOAD_TARGET_REF_OBJECT), new OperationResult(OPERATION_LOAD_TARGET_REF_OBJECT));
        if (object == null || !(object.asObjectable() instanceof AbstractRoleType)){
            return Model.of("");
        }
        AbstractRoleType targetRefObject = (AbstractRoleType) object.asObjectable();
        if (StringUtils.isNotEmpty(targetRefObject.getIdentifier())){
            return Model.of(targetRefObject.getIdentifier());
        }
        if (targetRefObject.getDisplayName() != null && !targetRefObject.getName().getOrig().equals(targetRefObject.getDisplayName().getOrig())){
            return Model.of(targetRefObject.getName().getOrig());
        }
        return Model.of("");
    }

	protected List<ObjectTypes> getObjectTypesList(){
        return WebComponentUtil.createAssignableTypesList();
    }

	@Override
	protected Fragment getCustomSpecificContainers(String contentAreaId, ContainerValueWrapper<AssignmentType> modelObject) {
		Fragment specificContainers = new Fragment(contentAreaId, AssignmentPanel.ID_SPECIFIC_CONTAINERS_FRAGMENT, this);
		specificContainers.add(getSpecificContainerPanel(modelObject));
		return specificContainers;
	}
	
	@Override
	protected IModel<ContainerWrapper> getSpecificContainerModel(ContainerValueWrapper<AssignmentType> modelObject) {
		if (ConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(modelObject.getContainerValue().getValue()))) {
			ContainerWrapper<ConstructionType> constructionWrapper = modelObject.findContainerWrapper(ItemPath.create(modelObject.getPath(),
					AssignmentType.F_CONSTRUCTION));
			
			return Model.of(constructionWrapper);
		}
		
		if (PersonaConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(modelObject.getContainerValue().getValue()))) {
			ContainerWrapper<PolicyRuleType> personasWrapper = modelObject.findContainerWrapper(ItemPath.create(modelObject.getPath(),
					AssignmentType.F_PERSONA_CONSTRUCTION));

			return Model.of(personasWrapper);
		}
		return Model.of();
	}

	@Override
	protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
		List<SearchItemDefinition> defs = new ArrayList<>();
		
		SearchFactory.addSearchRefDef(containerDef, AssignmentType.F_TARGET_REF, defs, AreaCategoryType.ADMINISTRATION, getPageBase());
		SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
		SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), defs);
		SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), defs);
		
		defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));
		
		return defs;
	}
	
}
