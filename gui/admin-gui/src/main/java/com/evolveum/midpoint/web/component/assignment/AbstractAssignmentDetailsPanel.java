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
import com.evolveum.midpoint.web.component.AjaxButton;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Created by honchar
 */
public abstract class AbstractAssignmentDetailsPanel extends BasePanel<AssignmentEditorDto>{
    private static final long serialVersionUID = 1L;

    private final static String ID_DESCRIPTION = "description";
    private final static String ID_TYPE_IMAGE = "typeImage";
    private final static String ID_ASSIGNMENT_NAME = "assignmentName";
    private final static String ID_PROPERTIES_PANEL = "propertiesPanel";
    private final static String ID_ACTIVATION_PANEL = "activationPanel";
    private final static String ID_DONE_BUTTON = "doneButton";

    protected PageBase pageBase;

    public AbstractAssignmentDetailsPanel(String id, IModel<AssignmentEditorDto> assignmentModel, PageBase pageBase){
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

        add(new Label(ID_DESCRIPTION, Model.of(getModelObject().getDescription())));

        add(initPropertiesContainer(ID_PROPERTIES_PANEL));

        AssignmentActivationPopupablePanel activationPanel = new AssignmentActivationPopupablePanel(ID_ACTIVATION_PANEL, getModel()){
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean getButtonsPanelVisibility() {
                return false;
            }
        };
        activationPanel.setOutputMarkupId(true);
        add(activationPanel);

        AjaxButton doneButton = new AjaxButton(ID_DONE_BUTTON, createStringResource("AbstractAssignmentDetailsPanel.doneButton")) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                redirectBack(ajaxRequestTarget);

            }
        };
        add(doneButton);
    }

    protected IModel<String> getAdditionalNameLabelStyleClass(){
        return Model.of("");
    }

    protected  abstract Component initPropertiesContainer(String id);

    protected abstract IModel<String> createHeaderModel();

    protected abstract IModel<String> createImageModel();

    protected void redirectBack(AjaxRequestTarget target){
    }
}
