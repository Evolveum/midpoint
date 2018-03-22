/**
 * Copyright (c) 2015-2017 Evolveum
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
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;

/**
 * Created by honchar
 */
public abstract class AbstractAssignmentDetailsPanel<F extends FocusType> extends BasePanel<ContainerValueWrapper<AssignmentType>> {
    private static final long serialVersionUID = 1L;

    private final static String ID_DISPLAY_NAME = "displayName";
    private final static String ID_ACTIVATION_PANEL = "activationPanel";
    private final static String ID_BASIC_ASSIGNMENT_PANEL = "basicAssignmentPanel";
    protected final static String ID_SPECIFIC_CONTAINERS = "specificContainers";

    public AbstractAssignmentDetailsPanel(String id, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel){
        super(id, assignmentModel);
    }
    
    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	initLayout();
    }

    protected <C extends Containerable> void initLayout(){
    	
    	final AbstractReadOnlyModel<C> displayNameModel = new AbstractReadOnlyModel<C>() {

    		private static final long serialVersionUID = 1L;

			@Override
    		public C getObject() {
				AssignmentType assignment = getModelObject().getContainerValue().getValue();
    			if (assignment.getTargetRef() != null) {
    				Task task = getPageBase().createSimpleTask("Load target");
    				com.evolveum.midpoint.schema.result.OperationResult result = task.getResult();
    				return (C) WebModelServiceUtils.loadObject(assignment.getTargetRef(), getPageBase(), task, result).asObjectable();
    			}
    			if (assignment.getConstruction() != null && assignment.getConstruction().getResourceRef() != null) {
					Task task = getPageBase().createSimpleTask("Load resource");
					com.evolveum.midpoint.schema.result.OperationResult result = task.getResult();
					return (C) WebModelServiceUtils.loadObject(assignment.getConstruction().getResourceRef(), getPageBase(), task, result).asObjectable();
    			} else if (assignment.getPersonaConstruction() != null) {
    				return (C) assignment.getPersonaConstruction();
    			} else if (assignment.getPolicyRule() !=null) {
    				return (C) assignment.getPolicyRule();
    			}

    			return null;
    		}

    	};

    	DisplayNamePanel<C> displayNamePanel = new DisplayNamePanel<C>(ID_DISPLAY_NAME, displayNameModel) {
    		
	    	@Override
			protected QName getRelation() {
	    		// TODO: is this correct ?
				AssignmentType assignment = AbstractAssignmentDetailsPanel.this.getModelObject().getContainerValue().getValue();
				if (assignment.getTargetRef() != null) {
					return assignment.getTargetRef().getRelation();
				} else {
					return null;
				}
			}

			@Override
			protected IModel<String> getKindIntentLabelModel() {
				AssignmentType assignment = AbstractAssignmentDetailsPanel.this.getModelObject().getContainerValue().getValue();
				if (assignment.getConstruction() != null){
					return createStringResource("DisplayNamePanel.kindIntentLabel", assignment.getConstruction().getKind(),
							assignment.getConstruction().getIntent());
				}
				return Model.of();
			}

		};

    	displayNamePanel.setOutputMarkupId(true);
    	add(displayNamePanel);


		PageAdminObjectDetails<F> pageBase = (PageAdminObjectDetails<F>)getPageBase();

		ItemPath assignmentPath = getModelObject().getPath();

		Form form = new Form<>("form");

		ContainerValueWrapper<AssignmentType> containerWrapper = getModelObject();
		if (containerWrapper == null){}

		ContainerValuePanel<AssignmentType> assignmentPanel = new ContainerValuePanel(ID_BASIC_ASSIGNMENT_PANEL, getModel(), true, form,
				itemWrapper -> getAssignmentBasicTabVisibity(itemWrapper, assignmentPath), pageBase);
		add(assignmentPanel);


		ContainerWrapperFromObjectWrapperModel<ActivationType, F> activationModel = new ContainerWrapperFromObjectWrapperModel<>(pageBase.getObjectModel(), assignmentPath.append(AssignmentType.F_ACTIVATION));
		PrismContainerPanel<ActivationType> acitvationContainer = new PrismContainerPanel<>(ID_ACTIVATION_PANEL, activationModel, true, form, itemWrapper -> getActivationVisibileItems(itemWrapper.getPath(), assignmentPath), pageBase);
		add(acitvationContainer);
		
		initContainersPanel(form, pageBase);
    }

    protected void initContainersPanel(Form form, PageAdminObjectDetails<F> pageBase) {
		ItemPath assignmentPath = getModelObject().getPath();
		PrismContainerPanel<PolicyRuleType> constraintsContainerPanel = new PrismContainerPanel(ID_SPECIFIC_CONTAINERS,
				getSpecificContainerModel(), false, form,
				itemWrapper -> getSpecificContainersItemsVisibility(itemWrapper, assignmentPath), pageBase);
		constraintsContainerPanel.setOutputMarkupId(true);
		add(constraintsContainerPanel);
	}
    
    protected ItemPath getAssignmentPath() {
    	return getModelObject().getContainerValue().getValue().asPrismContainerValue().getPath();
    }
    
    protected abstract IModel<ContainerWrapper> getSpecificContainerModel();
    
    protected boolean getSpecificContainersItemsVisibility(ItemWrapper itemWrapper, ItemPath parentAssignmentPath) {
		if (ContainerWrapper.class.isAssignableFrom(itemWrapper.getClass())){
			return true;
		}
		List<ItemPath> pathsToHide = new ArrayList<>();
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_RESOURCE_REF));
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_AUXILIARY_OBJECT_CLASS));
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION).append(ConstructionType.F_STRENGTH));
		return PropertyOrReferenceWrapper.class.isAssignableFrom(itemWrapper.getClass()) && !WebComponentUtil.isItemVisible(pathsToHide, itemWrapper.getPath());
	}

    private boolean getAssignmentBasicTabVisibity(ItemWrapper itemWrapper, ItemPath parentAssignmentPath) {
    	if (itemWrapper.getPath().equals(getAssignmentPath().append(AssignmentType.F_METADATA))){
    		return true;
		}
    	AssignmentType assignment = getModelObject().getContainerValue().getValue();
		ObjectReferenceType targetRef = assignment.getTargetRef();
		List<ItemPath> pathsToHide = new ArrayList<>();
		QName targetType = null;
		if (targetRef != null) {
			targetType = targetRef.getType();
		}
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_TARGET_REF));
		
		if (OrgType.COMPLEX_TYPE.equals(targetType) || AssignmentsUtil.isPolicyRuleAssignment(getModelObject().getContainerValue().asContainerable())) {
			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_TENANT_REF));
			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_ORG_REF));
		}
		if (AssignmentsUtil.isPolicyRuleAssignment(getModelObject().getContainerValue().asContainerable())){
			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_FOCUS_TYPE));
		}
		
		if (assignment.getConstruction() == null) {
			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_CONSTRUCTION));
		}
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_PERSONA_CONSTRUCTION));
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_POLICY_RULE));
		
		
    	return PropertyOrReferenceWrapper.class.isAssignableFrom(itemWrapper.getClass()) && !WebComponentUtil.isItemVisible(pathsToHide, itemWrapper.getPath()); 
    }
    

    private boolean getActivationVisibileItems(ItemPath pathToCheck, ItemPath assignmentPath) {
    	if (assignmentPath.append(new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP)).equivalent(pathToCheck)){
    		return false;
    	}
    	
    	if (assignmentPath.append(new ItemPath(AssignmentType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS)).equivalent(pathToCheck)){
    		return false;
    	}
    	
    	return true;
    }


    protected IModel<String> getAdditionalNameLabelStyleClass(){
        return Model.of("");
    }

}
