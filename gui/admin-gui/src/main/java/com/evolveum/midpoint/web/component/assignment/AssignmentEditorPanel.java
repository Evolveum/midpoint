/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
public class AssignmentEditorPanel extends Panel {

    private static final String ID_ASSIGNMENT_FORM = "assignmentForm";
    private static final String ID_RESOURCE_NAME = "resourceName";
    private static final String ID_BROWSE_RESOURCE = "browseResource";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXTENSION = "extension";
    private boolean initialized;

    private IModel<AssignmentEditorDto> model;

    public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model) {
        super(id);

        Validate.notNull(model, "Assignment dto model must not be null.");
        this.model = model;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        if (initialized) {
            return;
        }

        initLayout();
        initialized = true;
    }

    private void initLayout() {
        Form assignmentForm = new Form(ID_ASSIGNMENT_FORM);
        add(assignmentForm);

        Label resourceName = new Label(ID_RESOURCE_NAME, createResourceNameModel());
        resourceName.setOutputMarkupId(true);
        assignmentForm.add(resourceName);

        AjaxLinkButton browseResource = new AjaxLinkButton(ID_BROWSE_RESOURCE,
                createStringResource("AssignmentEditorPanel.button.browseResource")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                browseResourcePerformed(target);
            }
        };
        assignmentForm.add(browseResource);

        TextField description = new TextField(ID_DESCRIPTION,
                new PropertyModel(model, AssignmentEditorDto.F_DESCRIPTION));
        assignmentForm.add(description);

//        TextArea extension = new TextArea(ID_EXTENSION, new PropertyModel(model, AssignmentEditorDto.F_EXTENSION));
//        assignmentForm.add(extension);
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    private void browseResourcePerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private IModel createResourceNameModel() {
        return new AbstractReadOnlyModel() {

            @Override
            public Object getObject() {
                AssignmentEditorDto dto = model.getObject();
                PrismObject<ResourceType> resource = dto.getResource();
                if (resource == null) {
                    return null;
                }
                return WebMiscUtil.getName(resource);
            }
        };
    }
}
