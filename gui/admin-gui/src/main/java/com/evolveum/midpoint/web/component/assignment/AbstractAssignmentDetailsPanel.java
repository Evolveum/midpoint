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
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public abstract class AbstractAssignmentDetailsPanel extends BasePanel<AssignmentEditorDto>{
    private static final long serialVersionUID = 1L;

    protected final static String ID_TYPE_IMAGE = "typeImage";
    protected final static String ID_ASSIGNMENT_NAME = "assignmentName";
    protected final static String ID_PROPERTIES_PANEL = "propertiesPanel";
    protected final static String ID_ACTIVATION_PANEL = "activationPanel";

    public AbstractAssignmentDetailsPanel(String id, IModel<AssignmentEditorDto> assignmentModel){
        super(id, assignmentModel);
        initLayout();
    }

    protected void initLayout(){
        WebMarkupContainer typeImage = new WebMarkupContainer(ID_TYPE_IMAGE);
        typeImage.add(AttributeModifier.append("class", createImageModel()));
        add(typeImage);

        Label name = new Label(ID_ASSIGNMENT_NAME, createHeaderModel());
        add(name);

    }

    protected abstract IModel<String> createHeaderModel();

    protected abstract IModel<String> createImageModel();

}
