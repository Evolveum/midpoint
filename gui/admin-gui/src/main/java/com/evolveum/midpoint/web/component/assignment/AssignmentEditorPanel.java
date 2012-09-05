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
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AssignmentEditorPanel extends BasePanel<AssignmentEditorDto> {

    private static final String ID_TARGET_CONTAINER = "targetContainer";
    private static final String ID_CONSTRUCTION_CONTAINER = "constructionContainer";

    private static final String ID_TARGET_NAME = "targetName";
    private static final String ID_BROWSE_TARGET = "browseTarget";

    private static final String ID_RESOURCE_NAME = "resourceName";
    private static final String ID_BROWSE_RESOURCE = "browseResource";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_AC_ATTRIBUTE = "acAttribute";

    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXTENSION = "extension";
    private boolean initialized;

    public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model) {
        super(id, model);
    }

    protected void initLayout() {
        TextField description = new TextField(ID_DESCRIPTION,
                new PropertyModel(getModel(), AssignmentEditorDto.F_DESCRIPTION));
        add(description);

        WebMarkupContainer targetContainer = new WebMarkupContainer(ID_TARGET_CONTAINER);
        targetContainer.setOutputMarkupId(true);
        targetContainer.add(createContainerVisibleBehaviour(UserAssignmentDto.Type.TARGET));
        add(targetContainer);

        initTargetContainer(targetContainer);

        WebMarkupContainer constructionContainer = new WebMarkupContainer(ID_CONSTRUCTION_CONTAINER);
        constructionContainer.setOutputMarkupId(true);
        targetContainer.add(createContainerVisibleBehaviour(UserAssignmentDto.Type.ACCOUNT_CONSTRUCTION));
        add(constructionContainer);

        initConstructionContainer(constructionContainer);

        //todo extension and activation
//        TextArea extension = new TextArea(ID_EXTENSION, new PropertyModel(model, AssignmentEditorDto.F_EXTENSION));
//        assignmentForm.add(extension);
    }

    private VisibleEnableBehaviour createContainerVisibleBehaviour(final UserAssignmentDto.Type type) {
        return new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                if (type.equals(dto.getType())) {
                    return true;
                }

                return false;
            }
        };
    }

    private void initConstructionContainer(WebMarkupContainer constructionContainer) {
        Label resourceName = new Label(ID_RESOURCE_NAME, createResourceNameModel());
        resourceName.setOutputMarkupId(true);
        constructionContainer.add(resourceName);

        AjaxLinkButton browseResource = new AjaxLinkButton(ID_BROWSE_RESOURCE,
                createStringResource("AssignmentEditorPanel.button.browse")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                browseResourcePerformed(target);
            }
        };
        constructionContainer.add(browseResource);

        WebMarkupContainer attributes = new WebMarkupContainer(ID_ATTRIBUTES);
        attributes.setOutputMarkupId(true);
        constructionContainer.add(attributes);

        ListView<ACAttributeDto> attribute = new ListView<ACAttributeDto>(ID_ATTRIBUTE,
                new LoadableModel<List<ACAttributeDto>>(false) {

                    @Override
                    protected List<ACAttributeDto> load() {
                        return loadAttributes();
                    }
                }) {

            @Override
            protected void populateItem(ListItem<ACAttributeDto> listItem) {
                ACAttributePanel acAttribute = new ACAttributePanel(ID_AC_ATTRIBUTE, listItem.getModel());
                acAttribute.setRenderBodyOnly(true);
                listItem.add(acAttribute);
            }
        };
        attributes.add(attribute);
    }

    private void initTargetContainer(WebMarkupContainer targetContainer) {
        Label targetName = new Label(ID_TARGET_NAME); //todo model
        targetName.setOutputMarkupId(true);
        targetContainer.add(targetName);

        AjaxLinkButton browseResource = new AjaxLinkButton(ID_BROWSE_TARGET,
                createStringResource("AssignmentEditorPanel.button.browse")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                browseTargetPerformed(target);
            }
        };
        targetContainer.add(browseResource);
    }

    private List<ACAttributeDto> loadAttributes() {
        //todo implement
        return new ArrayList<ACAttributeDto>();
    }

    private void browseTargetPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void browseResourcePerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private IModel createResourceNameModel() {
        return new AbstractReadOnlyModel() {

            @Override
            public Object getObject() {
                AssignmentEditorDto dto = getModel().getObject();
                PrismObject<ResourceType> resource = dto.getResource();
                if (resource == null) {
                    return null;
                }
                return WebMiscUtil.getName(resource);
            }
        };
    }
}
