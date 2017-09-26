/**
 * Copyright (c) 2015 Evolveum
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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValuePanel;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.model.ContainerValueWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Created by honchar
 */
public abstract class AbstractAssignmentDetailsPanel<F extends FocusType> extends BasePanel<AssignmentDto>{
    private static final long serialVersionUID = 1L;

    private final static String ID_DISPLAY_NAME = "displayName";
    private final static String ID_ACTIVATION_PANEL = "activationPanel";
    private final static String ID_CONTAINERS = "otherContainers";

    public AbstractAssignmentDetailsPanel(String id, Form<?> form, IModel<AssignmentDto> assignmentModel){
        super(id, assignmentModel);
        
        
    }
    
    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	initLayout();
    }

    protected <C extends Containerable> void initLayout(){

    	DisplayNamePanel<C> displayNamePanel = new DisplayNamePanel<C>(ID_DISPLAY_NAME, new AbstractReadOnlyModel<C>() {

    		private static final long serialVersionUID = 1L;

			@Override
    		public C getObject() {
    			AssignmentDto assignemtn = getModelObject();
    			if (assignemtn.isAssignableObject()) {
    				Task task = getPageBase().createSimpleTask("Load target");
    				com.evolveum.midpoint.schema.result.OperationResult result = task.getResult();
    				return (C) WebModelServiceUtils.loadObject(getModelObject().getAssignment().getTargetRef(), getPageBase(), task, result).asObjectable();
    			}
    			AssignmentType assignmentType = assignemtn.getAssignment();
    			if (assignmentType.getConstruction() != null) {
    				return (C) assignmentType.getConstruction();
    			} else if (assignmentType.getPersonaConstruction() != null) {
    				return (C) assignmentType.getPersonaConstruction();
    			} else if (assignmentType.getPolicyRule() !=null) {
    				return (C) assignmentType.getPolicyRule();
    			}

    			return null;


    		}

    	});

    	displayNamePanel.setOutputMarkupId(true);
    	add(displayNamePanel);

		
		PageAdminObjectDetails<F> pageBase = (PageAdminObjectDetails<F>)getPageBase();
		
		ItemPath assignmentPath = getAssignmentPath();
		ContainerValueWrapperFromObjectWrapperModel<AssignmentType, F> assignmentModel = 
				new ContainerValueWrapperFromObjectWrapperModel<AssignmentType, F>(pageBase.getObjectModel(), assignmentPath);
		
		Form form = new Form<>("form");
		
		ContainerValuePanel<AssignmentType> assignmentPanel = new ContainerValuePanel("basic", assignmentModel, true, form, itemWrapper -> getAssignmentBasicTabVisibity(itemWrapper, assignmentPath), pageBase);
		add(assignmentPanel);
		
		
		ContainerWrapperFromObjectWrapperModel<ActivationType, F> activationModel = new ContainerWrapperFromObjectWrapperModel<ActivationType, F>(pageBase.getObjectModel(), assignmentPath.append(AssignmentType.F_ACTIVATION));
		PrismContainerPanel<ActivationType> acitvationContainer = new PrismContainerPanel<>(ID_ACTIVATION_PANEL, activationModel, false, form, itemWrapper -> getActivationVisibileItems(itemWrapper.getPath(), assignmentPath), pageBase);
		add(acitvationContainer);
		
		
		PrismPanel<AssignmentType> containers = new PrismPanel<>(ID_CONTAINERS, new ContainerWrapperListFromObjectWrapperModel<AssignmentType, F>(pageBase.getObjectModel(), collectContainersToShow()),
				null, form, null, pageBase) ;
		add(containers);
		
    }
    
    protected ItemPath getAssignmentPath() {
    	return getModel().getObject().getAssignment().asPrismContainerValue().getPath();
    }
    
    protected abstract List<ItemPath> collectContainersToShow();
    
    private boolean getAssignmentBasicTabVisibity(ItemWrapper itemWrapper, ItemPath parentAssignmentPath) {
    	AssignmentType assignment = getModelObject().getAssignment();
		ObjectReferenceType targetRef = assignment.getTargetRef();
		List<ItemPath> pathsToHide = new ArrayList<>();
		QName targetType = null;
		if (targetRef != null) {
			targetType = targetRef.getType();
		}
		pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_TARGET_REF));
		
		if (OrgType.COMPLEX_TYPE.equals(targetType)) {
			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_TENANT_REF));
			pathsToHide.add(parentAssignmentPath.append(AssignmentType.F_ORG_REF));
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
