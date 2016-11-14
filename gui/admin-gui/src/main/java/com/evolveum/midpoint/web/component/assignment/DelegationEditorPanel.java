/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentsPreviewDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class DelegationEditorPanel extends AssignmentEditorPanel {
    private static final String ID_DELEGATION_VALID_FROM = "delegationValidFrom";
    private static final String ID_DELEGATION_VALID_TO = "delegationValidTo";
    private static final String ID_DESCRIPTION = "delegationDescription";
    private static final String ID_ARROW_ICON = "arrowIcon";
    private static final String ID_DELEGATED_TO_IMAGE = "delegatedToImage";
    private static final String ID_DELEGATED_TO = "delegatedTo";
    private static final String ID_DELEGATED_TO_LABEL = "delegatedToLabel";
    private static final String ID_SELECTED = "selected";
    private static final String ID_TYPE_IMAGE = "typeImage";
    private static final String ID_NAME_LABEL = "nameLabel";
    private static final String ID_NAME = "name";
    private static final String ID_HEADER_ROW = "headerRow";
    private static final String ID_PRIVILEGES_LIST = "privilegesList";
    private static final String ID_PRIVILEGE = "privilege";
    private static final String ID_LIMIT_PRIVILEGES_BUTTON = "limitPrivilegesButton";

    private boolean delegatedToMe;

    public DelegationEditorPanel(String id, IModel<AssignmentEditorDto> delegationTargetObjectModel,
                                 boolean delegatedToMe, List<AssignmentsPreviewDto> privilegesList, PageBase pageBase) {
        super(id, delegationTargetObjectModel, privilegesList, pageBase);
    }


//    @Override
//    protected void initLayout(){
//        getModelObject().setPrivilegeLimitationList();
//        super.initLayout();
//    }
    @Override
    protected void initHeaderRow(){
        AjaxCheckBox selected = new AjaxCheckBox(ID_SELECTED,
                new PropertyModel<Boolean>(getModel(), AssignmentEditorDto.F_SELECTED)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // do we want to update something?
            }
        };
        selected.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                return !getModel().getObject().isSimpleView();
            }
        });
        headerRow.add(selected);
        Label arrowIcon = new Label(ID_ARROW_ICON);
        headerRow.add(arrowIcon);

        WebMarkupContainer typeImage = new WebMarkupContainer(ID_TYPE_IMAGE);
        if (delegatedToMe){
            typeImage.add(AttributeModifier.append("class", createImageTypeModel(getModel())));
        } else {
            typeImage.add(AttributeModifier.append("class", AssignmentEditorDtoType.USER.getIconCssClass()));
        }
        headerRow.add(typeImage);

        AjaxLink name = new AjaxLink(ID_NAME) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                nameClickPerformed(target);
            }
        };
        headerRow.add(name);

        Label nameLabel;
        if (delegatedToMe) {
            nameLabel = new Label(ID_NAME_LABEL, createAssignmentNameLabelModel(false));
        } else {
            nameLabel = new Label(ID_NAME_LABEL, pageBase.createStringResource("DelegationEditorPanel.meLabel"));
        }
        nameLabel.setOutputMarkupId(true);
        name.add(nameLabel);

        AssignmentEditorDto dto = getModelObject();
        dto.getTargetRef();

        WebMarkupContainer delegatedToTypeImage = new WebMarkupContainer(ID_DELEGATED_TO_IMAGE);
        if (delegatedToMe){
            delegatedToTypeImage.add(AttributeModifier.append("class", AssignmentEditorDtoType.USER.getIconCssClass()));
        } else {
            delegatedToTypeImage.add(AttributeModifier.append("class", createImageTypeModel(getModel())));
        }
        headerRow.add(delegatedToTypeImage);

        AjaxLink delegatedToName = new AjaxLink(ID_DELEGATED_TO) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
//                delegatedToClickPerformed(target);
            }
        };
        headerRow.add(delegatedToName);

        Label delegatedToNameLabel;
        if (delegatedToMe) {
            delegatedToNameLabel = new Label(ID_NAME_LABEL, pageBase.createStringResource("DelegationEditorPanel.meLabel"));
        } else {
            delegatedToNameLabel = new Label(ID_DELEGATED_TO_LABEL, createTargetModel());
        }
        delegatedToNameLabel.setOutputMarkupId(true);
        delegatedToName.add(delegatedToNameLabel);
    }

    protected void initBodyLayout(WebMarkupContainer body) {
        DateInput validFrom = new DateInput(ID_DELEGATION_VALID_FROM,
                createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validFrom")));
        body.add(validFrom);

        DateInput validTo = new DateInput(ID_DELEGATION_VALID_TO,
                createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validTo")));
        body.add(validTo);

        TextArea<String> description = new TextArea<>(ID_DESCRIPTION,
                new PropertyModel<String>(getModel(), AssignmentEditorDto.F_DESCRIPTION));
        description.setEnabled(getModel().getObject().isEditable());
        body.add(description);

        List<String> privilegesNames = new ArrayList<>();
        privilegesNames = getPrivilagesList();
        ListView<String> privilegesList = new ListView<String>(ID_PRIVILEGES_LIST, privilegesNames){
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item) {
                Label privilageNameLabel = new Label(ID_PRIVILEGE, item);
                item.add(privilageNameLabel);
            }
        };
        body.add(privilegesList);

        AjaxButton limitPrivilegesButton = new AjaxButton(ID_LIMIT_PRIVILEGES_BUTTON, pageBase.createStringResource("")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
//                super.onSubmit(target, form);
            }
        };
        body.add(limitPrivilegesButton);
    };

    private List<String> getPrivilagesList(){
        List<AssignmentsPreviewDto> privilegesList = getModel().getObject().getPrivilegeLimitationList();
        List<String> privilegesNamesList = new ArrayList<>();
        if (privilegesList != null){
            for (AssignmentsPreviewDto assignmentsPreviewDto : privilegesList){
                    privilegesNamesList.add(assignmentsPreviewDto.getTargetName());
            }
        }
        return privilegesNamesList;
    }

}
