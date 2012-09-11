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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDtoType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.*;

/**
 * @author lazyman
 */
public class AssignmentEditorPanel extends BasePanel<UserAssignmentDto> {

    private static final String ID_BODY = "body";
    private static final String ID_MINIMIZE_BUTTON = "minimizeButton";
    private static final String ID_SHOW_EMPTY_BUTTON = "showEmptyButton";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_AC_ATTRIBUTE = "acAttribute";
    private static final String ID_EXTENSION = "extension";
    private static final String ID_ACTIVATION = "activation";

    private IModel<AssignmentEditorDto> editorModel;

    public AssignmentEditorPanel(String id, IModel<UserAssignmentDto> model) {
        this(id, model, new LoadableModel<AssignmentEditorDto>(false) {

            @Override
            protected AssignmentEditorDto load() {
                return new AssignmentEditorDto();
            }
        });
    }

    public AssignmentEditorPanel(String id, IModel<UserAssignmentDto> model, IModel<AssignmentEditorDto> editorModel) {
        super(id, model);
        setOutputMarkupId(true);

        Validate.notNull(editorModel, "Editor model must not be null.");
        this.editorModel = editorModel;
    }

    protected void initLayout() {
        Label name = new Label("name", new PropertyModel<Object>(getModel(), UserAssignmentDto.F_NAME));
        add(name);

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto editorDto = editorModel.getObject();
                return !editorDto.isMinimized();
            }
        });
        add(body);

        TextField description = new TextField(ID_DESCRIPTION,
                new PropertyModel(getModel(), UserAssignmentDto.F_DESCRIPTION));
        body.add(description);

        WebMarkupContainer attributes = new WebMarkupContainer(ID_ATTRIBUTES);
        attributes.setOutputMarkupId(true);
        attributes.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                UserAssignmentDto dto = getModel().getObject();
                return UserAssignmentDtoType.ACCOUNT_CONSTRUCTION.equals(dto.getType());
            }
        });
        body.add(attributes);

        ListView<ACAttributeDto> attribute = new ListView<ACAttributeDto>(ID_ATTRIBUTE,
                new LoadableModel<List<ACAttributeDto>>(false) {

                    @Override
                    protected List<ACAttributeDto> load() {
                        return loadAttributes();
                    }
                }) {

            @Override
            protected void populateItem(ListItem<ACAttributeDto> listItem) {
                final IModel<ACAttributeDto> attrModel = listItem.getModel();
                ACAttributePanel acAttribute = new ACAttributePanel(ID_AC_ATTRIBUTE, attrModel);
                acAttribute.setRenderBodyOnly(true);
                listItem.add(acAttribute);

                listItem.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        AssignmentEditorDto editorDto = editorModel.getObject();
                        if (!editorDto.isMinimized()) {
                            return true;
                        }

                        ACAttributeDto dto = attrModel.getObject();
                        return StringUtils.isNotEmpty(dto.getValue()) || StringUtils.isNotEmpty(dto.getExpression());
                    }
                });
            }
        };
        attributes.add(attribute);

        //todo extension and activation

        initButtons();
    }

    public IModel<AssignmentEditorDto> getEditorModel() {
        return editorModel;
    }

    private void initButtons() {
        //todo images and CSS
        AjaxLink showEmptyButton = new AjaxLink(ID_SHOW_EMPTY_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                AssignmentEditorDto dto = editorModel.getObject();
                dto.setShowEmpty(!dto.isShowEmpty());

                target.add(AssignmentEditorPanel.this.get(ID_BODY + ":" + ID_ATTRIBUTES));
            }
        };
        add(showEmptyButton);

        AjaxLink minimizeButton = new AjaxLink(ID_MINIMIZE_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                AssignmentEditorDto dto = editorModel.getObject();
                dto.setMinimized(!dto.isMinimized());

                target.add(AssignmentEditorPanel.this);
            }
        };
        add(minimizeButton);
    }

    private List<ACAttributeDto> loadAttributes() {
        List<ACAttributeDto> attributes = new ArrayList<ACAttributeDto>();
        try {
            UserAssignmentDto dto = getModel().getObject();

            AssignmentType assignment = dto.getAssignment();
            AccountConstructionType construction = assignment.getAccountConstruction();
            ResourceType resource = construction.getResource();


            RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource.asPrismObject(),
                    getPageBase().getPrismContext());
            PrismContainerDefinition definition = refinedSchema.getAccountDefinition(construction.getType());

            Collection<ItemDefinition> definitions = definition.getDefinitions();
            for (ItemDefinition attrDef : definitions) {
                if (!(attrDef instanceof PrismPropertyDefinition)) {
                    //log skipping or something...
                    continue;
                }
                attributes.add(new ACAttributeDto((PrismPropertyDefinition) attrDef, null, null));
            }
        } catch (Exception ex) {
            //todo error handling
            ex.printStackTrace();
        }

        Collections.sort(attributes, new Comparator<ACAttributeDto>() {

            @Override
            public int compare(ACAttributeDto a1, ACAttributeDto a2) {
                return String.CASE_INSENSITIVE_ORDER.compare(a1.getName(), a2.getName());
            }
        });

        return attributes;
    }
}
