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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by honchar.
 */
public class InducedEntitlementsPanel extends InducementsPanel{

    private static final long serialVersionUID = 1L;
    
    private static final String ID_ASSIGNMENT_DETAILS = "assignmentDetails";

    private static final Trace LOGGER = TraceManager.getTrace(InducedEntitlementsPanel.class);
    
    private static final String DOT_CLASS = InducedEntitlementsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_SHADOW_DISPLAY_NAME = DOT_CLASS + "loadShadowDisplayName";
    private static final String OPERATION_LOAD_SHADOW_OBJECT = DOT_CLASS + "loadReferencedShadowObject";
    private static final String OPERATION_LOAD_RESOURCE_OBJECT = DOT_CLASS + "loadResourceObject";

    public InducedEntitlementsPanel(String id, IModel<ContainerWrapper<AssignmentType>> inducementContainerWrapperModel){
        super(id, inducementContainerWrapperModel);

    }

    @Override
    protected void initCustomPaging() {
        getInducedEntitlementsTabStorage().setPaging(ObjectPaging.createPaging(0, ((int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.INDUCED_ENTITLEMENTS_TAB_TABLE))));
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.INDUCED_ENTITLEMENTS_TAB_TABLE;
    }

    private ObjectTabStorage getInducedEntitlementsTabStorage(){
        return getParentPage().getSessionStorage().getInducedEntitlementsTabStorage();
    }

    @Override
    protected List<IColumn<ContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<ContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("ConstructionType.kind")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
                item.add(new Label(componentId, getKindLabelModel(rowModel.getObject())));
            }
        });
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("ConstructionType.intent")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
                item.add(new Label(componentId, getIntentLabelModel(rowModel.getObject())));
            }
        });
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("ConstructionType.association")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
                String assocLabel = getAssociationLabel(rowModel.getObject());
                //in case when association label contains "-" symbol, break-all words property will
                //wrap the label text incorrectly. In order to avoid this, we add additional style
                if (assocLabel != null && assocLabel.contains("-")){
                    item.add(AttributeModifier.append("style", "white-space: pre-line"));
                }
                item.add(new Label(componentId, Model.of(assocLabel)));
            }
        });
        columns.add(new AbstractColumn<ContainerValueWrapper<AssignmentType>, String>(createStringResource("InducedEntitlements.value")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
                List<ShadowType> shadowsList =
                        WebComponentUtil.loadReferencedObjectList(ExpressionUtil.getShadowRefValue(WebComponentUtil.getAssociationExpression(rowModel.getObject())),
                                OPERATION_LOAD_SHADOW_OBJECT, InducedEntitlementsPanel.this.getPageBase());
                MultiValueChoosePanel<ShadowType> valuesPanel = new MultiValueChoosePanel<ShadowType>(componentId,
                        Model.ofList(shadowsList), Arrays.asList(ShadowType.class), false){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected ObjectFilter getCustomFilter(){
                        ConstructionType construction = rowModel.getObject().getContainerValue().asContainerable().getConstruction();
                        return WebComponentUtil.getShadowTypeFilterForAssociation(construction, OPERATION_LOAD_RESOURCE_OBJECT,
                                InducedEntitlementsPanel.this.getPageBase());
                    }

                    @Override
                    protected void removePerformedHook(AjaxRequestTarget target, ShadowType shadow) {
                        if (shadow != null && StringUtils.isNotEmpty(shadow.getOid())){
                            ExpressionType expression = WebComponentUtil.getAssociationExpression(rowModel.getObject());
                            ExpressionUtil.removeShadowRefEvaluatorValue(expression, shadow.getOid(), getPageBase().getPrismContext());
                        }
                    }

                    @Override
                    protected void choosePerformedHook(AjaxRequestTarget target, List<ShadowType> selectedList) {
                        ShadowType shadow = selectedList != null && selectedList.size() > 0 ? selectedList.get(0) : null;
                        if (shadow != null && StringUtils.isNotEmpty(shadow.getOid())){
                            ExpressionType expression = WebComponentUtil.getAssociationExpression(rowModel.getObject(), true);
                            ExpressionUtil.addShadowRefEvaluatorValue(expression, shadow.getOid(),
                                    InducedEntitlementsPanel.this.getPageBase().getPrismContext());
                        }
                    }

                    @Override
                    protected void selectPerformed(AjaxRequestTarget target, List<ShadowType> chosenValues) {
                        addPerformed(target, chosenValues);
                    }

                };
                valuesPanel.setOutputMarkupId(true);
                item.add(valuesPanel);
            }
        });

        return columns;
    }

    @Override
    protected ObjectQuery createObjectQuery() {
        return QueryBuilder.queryFor(AssignmentType.class, getParentPage().getPrismContext())
                .exists(AssignmentType.F_CONSTRUCTION)
                .build();
    }
    
    @Override
	protected Fragment getCustomSpecificContainers(String contentAreaId, ContainerValueWrapper<AssignmentType> modelObject) {
		Fragment specificContainers = new Fragment(contentAreaId, AssignmentPanel.ID_SPECIFIC_CONTAINERS_FRAGMENT, this);
		specificContainers.add(getConstructionAssociationPanel(modelObject));

		specificContainers.add(super.getBasicContainerPanel(ID_ASSIGNMENT_DETAILS, new Model(modelObject)));
		return specificContainers;
	}
    
    
    
    @Override
    protected ContainerValuePanel getBasicContainerPanel(String idPanel,
    		IModel<ContainerValueWrapper<AssignmentType>> model) {
    	ContainerValuePanel panel = super.getBasicContainerPanel(idPanel, model);
    	panel.add(new VisibleEnableBehaviour() {
    		@Override
    		public boolean isVisible() {
    			return false;
    		}
    	});
    	return panel;
    }
    
    private ConstructionAssociationPanel getConstructionAssociationPanel(ContainerValueWrapper<AssignmentType> modelObject) {
    	ContainerWrapper<ConstructionType> constructionContainer = modelObject.findContainerWrapper(modelObject.getPath().append((AssignmentType.F_CONSTRUCTION)));
        ConstructionAssociationPanel constructionDetailsPanel = new ConstructionAssociationPanel(AssignmentPanel.ID_SPECIFIC_CONTAINER, Model.of(constructionContainer));
        constructionDetailsPanel.setOutputMarkupId(true);
        return constructionDetailsPanel;
    }

    protected List<ObjectTypes> getObjectTypesList(){
        return Arrays.asList(ObjectTypes.RESOURCE);
    }

    private IModel<String> getKindLabelModel(ContainerValueWrapper<AssignmentType> assignmentWrapper){
        if (assignmentWrapper == null){
            return Model.of("");
        }
        AssignmentType assignment = assignmentWrapper.getContainerValue().asContainerable();
        ConstructionType construction = assignment.getConstruction();
        if (construction == null || construction.getKind() == null){
            return Model.of("");
        }
        return WebComponentUtil.createLocalizedModelForEnum(construction.getKind(), InducedEntitlementsPanel.this);

    }

    private IModel<String> getIntentLabelModel(ContainerValueWrapper<AssignmentType> assignmentWrapper){
        if (assignmentWrapper == null){
            return Model.of("");
        }
        AssignmentType assignment = assignmentWrapper.getContainerValue().asContainerable();
        ConstructionType construction = assignment.getConstruction();
        if (construction == null || construction.getIntent() == null){
            return Model.of("");
        }
        return Model.of(construction.getIntent());

    }

    private String getAssociationLabel(ContainerValueWrapper<AssignmentType> assignmentWrapper){
        if (assignmentWrapper == null){
            return "";
        }
        ContainerWrapper<ConstructionType> constructionWrapper = assignmentWrapper.findContainerWrapper(assignmentWrapper.getPath()
                .append(AssignmentType.F_CONSTRUCTION));
        if (constructionWrapper == null || constructionWrapper.findContainerValueWrapper(constructionWrapper.getPath()) == null){
            return null;
        }
        ContainerWrapper<ResourceObjectAssociationType> associationWrapper = constructionWrapper.findContainerValueWrapper(constructionWrapper.getPath())
                .findContainerWrapper(constructionWrapper.getPath().append(ConstructionType.F_ASSOCIATION));
        if (associationWrapper == null || associationWrapper.getValues() == null || associationWrapper.getValues().size() == 0){
            return null;
        }

        //for now only use case with single association is supported
        ContainerValueWrapper<ResourceObjectAssociationType> associationValueWrapper = associationWrapper.getValues().get(0);
        ResourceObjectAssociationType association = associationValueWrapper.getContainerValue().asContainerable();

        return association != null ?
                (StringUtils.isNotEmpty(association.getDisplayName()) ? association.getDisplayName() : association.getRef().toString())
                : null;

    }

    @Override
    protected boolean isEntitlementAssignment(){
        return true;
    }

    @Override
    protected List<ContainerValueWrapper<AssignmentType>> customPostSearch(List<ContainerValueWrapper<AssignmentType>> assignments) {
        List<ContainerValueWrapper<AssignmentType>> filteredAssignments = new ArrayList<>();
        if (assignments == null){
            return filteredAssignments;
        }
        assignments.forEach(assignmentWrapper -> {
                AssignmentType assignment = assignmentWrapper.getContainerValue().asContainerable();
                if (assignment.getConstruction() != null && assignment.getConstruction().getAssociation() != null) {
                    List<ResourceObjectAssociationType> associations = assignment.getConstruction().getAssociation();
                    if (associations.size() == 0 && ValueStatus.ADDED.equals(assignmentWrapper.getStatus())){
                        filteredAssignments.add(assignmentWrapper);
                        return;
                    }
                    associations.forEach(association -> {
                        if (!filteredAssignments.contains(assignmentWrapper)) {
                            if (association.getOutbound() == null && ValueStatus.ADDED.equals(assignmentWrapper.getStatus())) {
                                filteredAssignments.add(assignmentWrapper);
                                return;
                            }
                            if (association.getOutbound() != null && association.getOutbound().getExpression() != null) {
                                List<ObjectReferenceType> shadowRefList = ExpressionUtil.getShadowRefValue(association.getOutbound().getExpression());
                                if (shadowRefList == null){
                                    return;
                                }
                                for (ObjectReferenceType shadowRef : shadowRefList){
                                    if ((shadowRef != null || ValueStatus.ADDED.equals(assignmentWrapper.getStatus()))) {
                                        filteredAssignments.add(assignmentWrapper);
                                        break;
                                    }
                                }
                            }
                        }
                    });
            }
        });
        return filteredAssignments;
    }
}
