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

import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * Created by honchar
 */
public abstract class AbstractAssignmentDetailsPanel extends BasePanel<AssignmentDto>{
    private static final long serialVersionUID = 1L;

    private final static String ID_DISPLAY_NAME = "displayName";
    protected final static String ID_PROPERTIES_PANEL = "propertiesPanel";
    private final static String ID_ACTIVATION_PANEL = "activationPanel";
//    private final static String ID_DONE_BUTTON = "doneButton";

//    private static final String ID_RELATION_CONTAINER = "relationContainer";
//    private static final String ID_RELATION = "relation";
//
//	private static final String ID_TENANT_CONTAINER = "tenantContainer";
//	private static final String ID_TENANT = "tenant";
//	private static final String ID_PROJECT_CONTAINER = "projectContainer";
//	private static final String ID_PROJECT = "project";
//	private static final String ID_POLICY_SITUATIONS = "policySituations";
//	private static final String ID_POLICY_SITUATION = "policySituation";

//	private static final String ID_POLICY_RULE = "policyRule";

    protected PageBase pageBase;

    public AbstractAssignmentDetailsPanel(String id, Form<?> form, IModel<AssignmentDto> assignmentModel, PageBase pageBase){
        super(id, assignmentModel);
        this.pageBase= pageBase;
        initLayout(form);
    }

    protected <C extends Containerable> void initLayout(Form form){

    	DisplayNamePanel<C> displayNamePanel = new DisplayNamePanel<C>(ID_DISPLAY_NAME, new AbstractReadOnlyModel<C>() {

    		private static final long serialVersionUID = 1L;

			@Override
    		public C getObject() {
    			AssignmentDto assignemtn = getModelObject();
    			if (assignemtn.isAssignableObject()) {
    				Task task = AbstractAssignmentDetailsPanel.this.pageBase.createSimpleTask("Load target");
    				com.evolveum.midpoint.schema.result.OperationResult result = task.getResult();
    				return (C) WebModelServiceUtils.loadObject(getModelObject().getAssignment().getTargetRef(), AbstractAssignmentDetailsPanel.this.pageBase, task, result).asObjectable();
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


        WebMarkupContainer properties = new WebMarkupContainer(ID_PROPERTIES_PANEL);
		add(properties);
		properties.setOutputMarkupId(true);

		initPropertiesPanel(properties);


        AssignmentActivationPopupablePanel activationPanel = new AssignmentActivationPopupablePanel(ID_ACTIVATION_PANEL, new PropertyModel<ActivationType>(getModel(), AssignmentDto.F_VALUE + "." + AssignmentType.F_ACTIVATION.getLocalPart())){
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean getButtonsPanelVisibility() {
                return false;
            }
        };
        activationPanel.setOutputMarkupId(true);
        add(activationPanel);

    }




    protected IModel<String> getAdditionalNameLabelStyleClass(){
        return Model.of("");
    }

    protected boolean isVisible(Object path) {
    	return !getHiddenItems().contains(path);
    }

    protected abstract List getHiddenItems();

    protected abstract void initPropertiesPanel(WebMarkupContainer propertiesPanel);

}
