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
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDtoType;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;

import java.util.*;

/**
 * @author lazyman
 */
public class AssignmentEditorPanel extends BasePanel<AssignmentEditorDto> {

    private static final String ID_SELECTED = "selected";
    private static final String ID_TYPE_IMAGE = "typeImage";
    private static final String ID_NAME_LABEL = "nameLabel";
    private static final String ID_NAME = "name";
    private static final String ID_ACTIVATION = "activation";
    private static final String ID_BODY = "body";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ENABLED = "enabled";
    private static final String ID_VALID_FROM = "validFrom";
    private static final String ID_VALID_TO = "validTo";
    private static final String ID_SHOW_EMPTY = "showEmpty";
    private static final String ID_SHOW_EMPTY_LABEL = "showEmptyLabel";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_AC_ATTRIBUTE = "acAttribute";
    private static final String ID_TARGET = "target";
    private static final String ID_TARGET_CONTAINER = "targetContainer";
    private static final String ID_CONSTRUCTION_CONTAINER = "constructionContainer";


    public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        CheckBox selected = new CheckBox(ID_SELECTED,
                new PropertyModel<Boolean>(getModel(), AssignmentEditorDto.F_SELECTED));
        add(selected);

        Image typeImage = new Image(ID_TYPE_IMAGE,
                createImageTypeModel(new PropertyModel<UserAssignmentDtoType>(getModel(), AssignmentEditorDto.F_TYPE)));
        add(typeImage);

        AjaxLink name = new AjaxLink(ID_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                nameClickPerformed(target);
            }
        };
        add(name);

        Label nameLabel = new Label(ID_NAME_LABEL, new PropertyModel<String>(getModel(), AssignmentEditorDto.F_NAME));
        name.add(nameLabel);

        Label activation = new Label(ID_ACTIVATION, createActivationModel());
        add(activation);

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.setOutputMarkupId(true);
        body.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto editorDto = AssignmentEditorPanel.this.getModel().getObject();
                return !editorDto.isMinimized();
            }
        });
        add(body);

        initBodyLayout(body);
    }

    //todo [lazyman] change and move i18n keys
    private IModel<String> createActivationModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                AssignmentEditorDto dto = getModel().getObject();
                ActivationType activation = dto.getActivation();
                if (activation == null) {
                    return "-";
                }

                Boolean enabled = activation.isEnabled();
                String strEnabled;
                if (enabled != null) {
                    strEnabled = enabled ? getString("pageUser.assignment.activation.active")
                            : getString("pageUser.assignment.activation.inactive");
                } else {
                    strEnabled = getString("pageUser.assignment.activation.undefined");
                }

                if (activation.getValidFrom() != null && activation.getValidTo() != null) {
                    return getString("pageUser.assignment.activation.enabledFromTo", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()), MiscUtil.asDate(activation.getValidTo()));
                } else if (activation.getValidFrom() != null) {
                    return getString("pageUser.assignment.activation.enabledFrom", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()));
                } else if (activation.getValidTo() != null) {
                    return getString("pageUser.assignment.activation.enabledTo", strEnabled,
                            MiscUtil.asDate(activation.getValidTo()));
                }

                return "-";
            }
        };
    }

    private void initBodyLayout(WebMarkupContainer body) {
        TextField description = new TextField(ID_DESCRIPTION,
                new PropertyModel(getModel(), UserAssignmentDto.F_DESCRIPTION));
        body.add(description);

        CheckBox enabled = new CheckBox(ID_ENABLED);//todo model
        body.add(enabled);

        DateTextField validFrom = DateTextField.forDatePattern(ID_VALID_FROM, null, "dd/MMM/yyyy"); //todo model
        validFrom.add(new DatePicker());
        body.add(validFrom);

        DateTextField validTo = DateTextField.forDatePattern(ID_VALID_TO, null, "dd/MMM/yyyy"); //todo model
        validTo.add(new DatePicker());
        body.add(validTo);

        WebMarkupContainer targetContainer = new WebMarkupContainer(ID_TARGET_CONTAINER);
        body.add(targetContainer);

        Label target = new Label(ID_TARGET); //todo model
        targetContainer.add(target);

        WebMarkupContainer constructionContainer = new WebMarkupContainer(ID_CONSTRUCTION_CONTAINER);
        body.add(constructionContainer);

        AjaxLink showEmpty = new AjaxLink(ID_SHOW_EMPTY) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showEmptyPerformed(target);
            }
        };
        constructionContainer.add(showEmpty);

        Label showEmptyLabel = new Label(ID_SHOW_EMPTY_LABEL, createShowEmptyLabel());
        showEmptyLabel.setOutputMarkupId(true);
        showEmpty.add(showEmptyLabel);

        initAttributesLayout(constructionContainer);
    }

    private void initAttributesLayout(WebMarkupContainer constructionContainer) {
        WebMarkupContainer attributes = new WebMarkupContainer(ID_ATTRIBUTES);
        attributes.setOutputMarkupId(true);
        attributes.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                return UserAssignmentDtoType.ACCOUNT_CONSTRUCTION.equals(dto.getType());
            }
        });
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
                final IModel<ACAttributeDto> attrModel = listItem.getModel();
                ACAttributePanel acAttribute = new ACAttributePanel(ID_AC_ATTRIBUTE, attrModel);
                acAttribute.setRenderBodyOnly(true);
                listItem.add(acAttribute);

                listItem.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        AssignmentEditorDto editorDto = AssignmentEditorPanel.this.getModel().getObject();
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

        //todo extension
    }

    private IModel<String> createShowEmptyLabel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                AssignmentEditorDto dto = AssignmentEditorPanel.this.getModel().getObject();

                if (dto.isShowEmpty()) {
                    return getString("AssignmentEditorPanel.hideEmpty");
                } else {
                    return getString("AssignmentEditorPanel.showEmpty");
                }
            }
        };
    }

    private void showEmptyPerformed(AjaxRequestTarget target) {
        AssignmentEditorDto dto = AssignmentEditorPanel.this.getModel().getObject();
        dto.setShowEmpty(!dto.isShowEmpty());

        target.add(get(createComponentPath(ID_BODY, ID_CONSTRUCTION_CONTAINER, ID_ATTRIBUTES)),
                get(createComponentPath(ID_BODY, ID_CONSTRUCTION_CONTAINER, ID_SHOW_EMPTY, ID_SHOW_EMPTY_LABEL)));
    }

    private List<ACAttributeDto> loadAttributes() {
        List<ACAttributeDto> attributes = new ArrayList<ACAttributeDto>();
        try {
            AssignmentEditorDto dto = getModel().getObject();

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

    private IModel<ResourceReference> createImageTypeModel(final IModel<UserAssignmentDtoType> model) {
        return new AbstractReadOnlyModel<ResourceReference>() {

            @Override
            public ResourceReference getObject() {
                UserAssignmentDtoType type = model.getObject();
                switch (type) {
                    case ROLE:
                        return new SharedResourceReference(ImgResources.class, ImgResources.USER_SUIT);
                    case ORG_UNIT:
                        //todo [miso] change picture to org. unit icon
                        return new SharedResourceReference(ImgResources.class, ImgResources.USER_SUIT);
                    case ACCOUNT_CONSTRUCTION:
                    default:
                        return new SharedResourceReference(ImgResources.class, ImgResources.DRIVE);
                }
            }
        };
    }

    private void nameClickPerformed(AjaxRequestTarget target) {
        AssignmentEditorDto dto = getModel().getObject();
        dto.setMinimized(!dto.isMinimized());

        target.add(get(ID_BODY));

        //todo implement;
    }
}
