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

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyColumn;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.model.ContainerWrapperOnlyForHeaderModel;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValuePanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Arrays;
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

    public InducedEntitlementsPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> inducementContainerWrapperModel){
        super(id, inducementContainerWrapperModel);

    }

    @Override
    protected void initCustomPaging() {
        getInducedEntitlementsTabStorage().setPaging(getPrismContext().queryFactory()
                .createPaging(0, ((int) getParentPage().getItemsPerPage(UserProfileStorage.TableId.INDUCED_ENTITLEMENTS_TAB_TABLE))));
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.INDUCED_ENTITLEMENTS_TAB_TABLE;
    }

    private ObjectTabStorage getInducedEntitlementsTabStorage(){
        return getParentPage().getSessionStorage().getInducedEntitlementsTabStorage();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();
        
        columns.add(new PrismPropertyColumn<AssignmentType, String>(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_KIND), ColumnType.STRING));
//        columns.add(new PrismPropertyColumn<AssignmentType>(
//        		new ContainerWrapperOnlyForHeaderModel(getModel(), AssignmentType.F_CONSTRUCTION,getPageBase()),
//        		ConstructionType.F_KIND, getPageBase()) {
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
//						item.add(new Label(componentId, getKindLabelModel(rowModel.getObject())));
//					}
//        });

        columns.add(new PrismPropertyColumn<AssignmentType, String>(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_INTENT), ColumnType.STRING));
//        columns.add(new PrismPropertyColumn<AssignmentType>(
//        		new ContainerWrapperOnlyForHeaderModel(getModel(), AssignmentType.F_CONSTRUCTION,getPageBase()),
//        		ConstructionType.F_INTENT, getPageBase()) {
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
//                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
//						item.add(new Label(componentId, getIntentLabelModel(rowModel.getObject())));
//					}
//        });
        
        columns.add(new PrismContainerWrapperColumn<>(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION)));
        
//        columns.add(new AbstractItemWrapperColumn<AssignmentType>(
//        		new ContainerWrapperOnlyForHeaderModel(getModel(), AssignmentType.F_CONSTRUCTION,getPageBase()),
//        		ConstructionType.F_ASSOCIATION, getPageBase()) {
//					private static final long serialVersionUID = 1L;
//
//					@Override
//					public void populateItem(Item<ICellPopulator<ContainerValueWrapper<AssignmentType>>> item, String componentId,
//                                     final IModel<ContainerValueWrapper<AssignmentType>> rowModel) {
//						String assocLabel = getAssociationLabel(rowModel.getObject());
//						//in case when association label contains "-" symbol, break-all words property will
//						//wrap the label text incorrectly. In order to avoid this, we add additional style
//						if (assocLabel != null && assocLabel.contains("-")){
//							item.add(AttributeModifier.append("style", "white-space: pre-line"));
//						}
//						item.add(new Label(componentId, Model.of(assocLabel)));
//					}
//        });
        
        
        
        columns.add(new AbstractColumn<PrismContainerValueWrapper<AssignmentType>, String>(createStringResource("InducedEntitlements.value")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> item, String componentId,
                                     final IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
              
				
	  
					List<ShadowType> shadowsList = WebComponentUtil.loadReferencedObjectList(ExpressionUtil.getShadowRefValue(WebComponentUtil.getAssociationExpression(rowModel.getObject(), getPageBase()),
					        InducedEntitlementsPanel.this.getPageBase().getPrismContext()),
					        OPERATION_LOAD_SHADOW_OBJECT, InducedEntitlementsPanel.this.getPageBase());
				
					MultiValueChoosePanel<ShadowType> valuesPanel = new MultiValueChoosePanel<ShadowType>(componentId,
                        Model.ofList(shadowsList), Arrays.asList(ShadowType.class), false){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected ObjectFilter getCustomFilter(){
                        ConstructionType construction = rowModel.getObject().getRealValue().getConstruction();
                        return WebComponentUtil.getShadowTypeFilterForAssociation(construction, OPERATION_LOAD_RESOURCE_OBJECT,
                                InducedEntitlementsPanel.this.getPageBase());
                    }

                    @Override
                    protected void removePerformedHook(AjaxRequestTarget target, ShadowType shadow) {
                        if (shadow != null && StringUtils.isNotEmpty(shadow.getOid())){
                            ExpressionType expression = WebComponentUtil.getAssociationExpression(rowModel.getObject(), getPageBase());
                            ExpressionUtil.removeShadowRefEvaluatorValue(expression, shadow.getOid(), getPageBase().getPrismContext());
                        }
                    }

                    @Override
                    protected void choosePerformedHook(AjaxRequestTarget target, List<ShadowType> selectedList) {
                        ShadowType shadow = selectedList != null && selectedList.size() > 0 ? selectedList.get(0) : null;
                        if (shadow != null && StringUtils.isNotEmpty(shadow.getOid())){
                            ExpressionType expression = WebComponentUtil.getAssociationExpression(rowModel.getObject(), true,
                                    InducedEntitlementsPanel.this.getPageBase().getPrismContext(), getPageBase());
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
        return getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_CONSTRUCTION)
                .build();
    }
    
//    @Override
//	protected Fragment getCustomSpecificContainers(String contentAreaId, IModel<PrismContainerValueWrapper<AssignmentType>> modelObject) {
//		Fragment specificContainers = new Fragment(contentAreaId, AssignmentPanel.ID_SPECIFIC_CONTAINERS_FRAGMENT, this);
//		specificContainers.add(getConstructionAssociationPanel(modelObject));
//
//		specificContainers.add(super.getBasicContainerPanel(ID_ASSIGNMENT_DETAILS, new Model(modelObject)));
//		return specificContainers;
//	}
    
    @Override
    protected void addCustomSpecificContainers(Fragment specificContainers,
    		IModel<PrismContainerValueWrapper<AssignmentType>> modelObject) {
    	specificContainers.add(getConstructionAssociationPanel(modelObject));
    	specificContainers.add(super.getBasicContainerPanel(ID_ASSIGNMENT_DETAILS, new Model(modelObject)));
   
    }
    
    
    @Override
    protected Panel getBasicContainerPanel(String idPanel,
    		IModel<PrismContainerValueWrapper<AssignmentType>> model) {
    	Panel panel = super.getBasicContainerPanel(idPanel, model);
    	panel.add(new VisibleEnableBehaviour() {
    		@Override
    		public boolean isVisible() {
    			return false;
    		}
    	});
    	return panel;
    }
    
    private ConstructionAssociationPanel getConstructionAssociationPanel(IModel<PrismContainerValueWrapper<AssignmentType>> model) {
    	IModel<PrismContainerWrapper<ConstructionType>> constructionModel = PrismContainerWrapperModel.fromContainerValueWrapper(model, AssignmentType.F_CONSTRUCTION);
//    	PrismContainerWrapper<ConstructionType> constructionContainer = modelObject.findContainer(modelObject.getPath().append((AssignmentType.F_CONSTRUCTION)));
        ConstructionAssociationPanel constructionDetailsPanel = new ConstructionAssociationPanel(AssignmentPanel.ID_SPECIFIC_CONTAINER, constructionModel);
        constructionDetailsPanel.setOutputMarkupId(true);
        return constructionDetailsPanel;
    }

    protected List<ObjectTypes> getObjectTypesList(){
        return Arrays.asList(ObjectTypes.RESOURCE);
    }

//    private IModel<String> getKindLabelModel(PrismContainerValueWrapper<AssignmentType> assignmentWrapper){
//        if (assignmentWrapper == null){
//            return Model.of("");
//        }
//        AssignmentType assignment = new ItemRealValueModel<AssignmentType>(assignmentWrapper).getObject();
//        ConstructionType construction = assignment.getConstruction();
//        if (construction == null || construction.getKind() == null){
//            return Model.of("");
//        }
//        return WebComponentUtil.createLocalizedModelForEnum(construction.getKind(), InducedEntitlementsPanel.this);
//
//    }
//
//    private IModel<String> getIntentLabelModel(ContainerValueWrapper<AssignmentType> assignmentWrapper){
//        if (assignmentWrapper == null){
//            return Model.of("");
//        }
//        AssignmentType assignment = new ItemRealValueModel<AssignmentType>(assignmentWrapper).getObject();
//        ConstructionType construction = assignment.getConstruction();
//        if (construction == null || construction.getIntent() == null){
//            return Model.of("");
//        }
//        return Model.of(construction.getIntent());
//
//    }

    @Override
    protected boolean isEntitlementAssignment(){
        return true;
    }

    @Override
    protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(List<PrismContainerValueWrapper<AssignmentType>> assignments) {
        List<PrismContainerValueWrapper<AssignmentType>> filteredAssignments = new ArrayList<>();
        if (assignments == null){
            return filteredAssignments;
        }
        assignments.forEach(assignmentWrapper -> {
                AssignmentType assignment = assignmentWrapper.getRealValue();
                if (assignment.getConstruction() != null && assignment.getConstruction().getAssociation() != null) {
                    List<ResourceObjectAssociationType> associations = assignment.getConstruction().getAssociation();
                    if (associations.size() == 0 && ValueStatus.ADDED.equals(assignmentWrapper.getStatus())){
                        filteredAssignments.add(assignmentWrapper);
                        return;
                    }
                    associations.forEach(association -> {
                        if (!filteredAssignments.contains(assignmentWrapper)) {
                            if (association.getRef() != null && association.getRef().getItemPath() != null &&
                                    !association.getRef().getItemPath().isEmpty()){
                                filteredAssignments.add(assignmentWrapper);
                            }
                        }
                    });
            }
        });
        return filteredAssignments;
    }
}
