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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * Created by honchar
 */
public abstract class AbstractAssignmentDetailsPanel extends BasePanel<AssignmentDto>{
    private static final long serialVersionUID = 1L;

    private final static String ID_DESCRIPTION = "description";
    private final static String ID_TYPE_IMAGE = "typeImage";
    private final static String ID_ASSIGNMENT_NAME = "assignmentName";
    private final static String ID_PROPERTIES_PANEL = "propertiesPanel";
    private final static String ID_ACTIVATION_PANEL = "activationPanel";
//    private final static String ID_DONE_BUTTON = "doneButton";
    
private static final String ID_RELATION = "relation";
	
	private static final String ID_TENANT = "tenant";
	private static final String ID_PROJECT = "project";
	private static final String ID_POLICY_SITUATIONS = "policySituations";
	private static final String ID_POLICY_SITUATION = "policySituation";

    protected PageBase pageBase;

    public AbstractAssignmentDetailsPanel(String id, IModel<AssignmentDto> assignmentModel, PageBase pageBase){
        super(id, assignmentModel);
        this.pageBase= pageBase;
        initLayout();
    }

    protected void initLayout(){
        WebMarkupContainer typeImage = new WebMarkupContainer(ID_TYPE_IMAGE);
        typeImage.setOutputMarkupId(true);
        typeImage.add(AttributeModifier.append("class", createImageModel()));
        typeImage.add(AttributeModifier.append("class", getAdditionalNameLabelStyleClass()));
        add(typeImage);

        Label name = new Label(ID_ASSIGNMENT_NAME, createHeaderModel());
        name.add(AttributeModifier.append("class", getAdditionalNameLabelStyleClass()));
        name.setOutputMarkupId(true);
        add(name);

        add(new Label(ID_DESCRIPTION, new PropertyModel<String>(getModel(), AssignmentDto.F_VALUE + "." + AssignmentType.F_DESCRIPTION.getLocalPart())));

        //add(initPropertiesContainer(ID_PROPERTIES_PANEL));
        
        WebMarkupContainer properties = new WebMarkupContainer(ID_PROPERTIES_PANEL);
		add(properties);
		properties.setOutputMarkupId(true);
		
		AssignmentDto assignmentDto = getModelObject();
		
		DropDownChoicePanel<RelationTypes> relation = WebComponentUtil.createEnumPanel(RelationTypes.class, ID_RELATION,
                WebComponentUtil.createReadonlyModelFromEnum(RelationTypes.class), new PropertyModel(getModel(), AssignmentDto.F_RELATION_TYPE), this, true);
        relation.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            	target.add(AbstractAssignmentDetailsPanel.this);
            }
        });
        properties.add(relation);
        
        AssignmentType assignmentType = assignmentDto.getAssignment();
        
        ChooseTypePanel<OrgType> tenantChooser = createParameterChooserPanel(ID_TENANT, assignmentType.getTenantRef(), true);
        properties.add(tenantChooser);
        
        ChooseTypePanel<OrgType> projectChooser = createParameterChooserPanel(ID_PROJECT, assignmentType.getTenantRef(), true);
        properties.add(projectChooser);
        
        ListView<String> policySituations = new ListView<String>(ID_POLICY_SITUATIONS, new PropertyModel<List<String>>(getModel(), AssignmentDto.F_VALUE + "." + AssignmentType.F_POLICY_SITUATION.getLocalPart())) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<String> item) {
				TextPanel<String> textPanel = new TextPanel<String>(ID_POLICY_SITUATION, item.getModel());
				textPanel.setOutputMarkupId(true);
				item.add(textPanel);
				
			}
		};
        policySituations.setOutputMarkupId(true);
        properties.add(policySituations);
        

        AssignmentActivationPopupablePanel activationPanel = new AssignmentActivationPopupablePanel(ID_ACTIVATION_PANEL, new PropertyModel<ActivationType>(getModel(), AssignmentDto.F_VALUE + "." + AssignmentType.F_ACTIVATION.getLocalPart())){
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean getButtonsPanelVisibility() {
                return false;
            }
        };
        activationPanel.setOutputMarkupId(true);
        add(activationPanel);

//        AjaxButton doneButton = new AjaxButton(ID_DONE_BUTTON, createStringResource("AbstractAssignmentDetailsPanel.doneButton")) {
//            private static final long serialVersionUID = 1L;
//            @Override
//            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
//                redirectBack(ajaxRequestTarget);
//
//            }
//        };
//        add(doneButton);
    }

    
    private ChooseTypePanel<OrgType> createParameterChooserPanel(String id, ObjectReferenceType ref, boolean isTenant){
    	ChooseTypePanel<OrgType> orgSelector = new ChooseTypePanel<OrgType>(id, ref) {
    		
    		private static final long serialVersionUID = 1L;

    		@Override
    		protected void executeCustomAction(AjaxRequestTarget target, OrgType object) {
    			if (isTenant) {
    				AbstractAssignmentDetailsPanel.this.getModelObject().getAssignment().setTenantRef(ObjectTypeUtil.createObjectRef(object));
    			} else {
    				AbstractAssignmentDetailsPanel.this.getModelObject().getAssignment().setOrgRef(ObjectTypeUtil.createObjectRef(object));
    			}
    			target.add(AbstractAssignmentDetailsPanel.this);
    		}
    		
    		@Override
    		protected void executeCustomRemoveAction(AjaxRequestTarget target) {
    			if (isTenant) {
    				AbstractAssignmentDetailsPanel.this.getModelObject().getAssignment().setTenantRef(null);
    			} else {
    				AbstractAssignmentDetailsPanel.this.getModelObject().getAssignment().setOrgRef(null);
    			}
    			target.add(AbstractAssignmentDetailsPanel.this);
    		}
    		
    		@Override
    		protected ObjectQuery getChooseQuery() {
    			ObjectFilter tenantFilter = QueryBuilder.queryFor(OrgType.class, getPageBase().getPrismContext()).item(OrgType.F_TENANT).eq(true).buildFilter();
    			
    			if (isTenant) {
    				return ObjectQuery.createObjectQuery(tenantFilter);
    			} 
    			return ObjectQuery.createObjectQuery(NotFilter.createNot(tenantFilter));
    			
    		}
    		
    		@Override
    		protected boolean isSearchEnabled() {
    			return true;
    		}
    		
    		@Override
    		public Class<OrgType> getObjectTypeClass() {
    			return OrgType.class;
    		}
    		
    	};
    	orgSelector.setOutputMarkupId(true);
    	return orgSelector;
    		
    	}
    
    
    protected IModel<String> getAdditionalNameLabelStyleClass(){
        return Model.of("");
    }

//    protected  abstract Component initPropertiesContainer(String id);

    protected abstract IModel<String> createHeaderModel();

    protected abstract IModel<String> createImageModel();

//    protected abstract void redirectBack(AjaxRequestTarget target){
//    }
}
