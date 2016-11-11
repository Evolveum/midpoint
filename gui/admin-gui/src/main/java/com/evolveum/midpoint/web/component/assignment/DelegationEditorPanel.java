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
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Created by honchar.
 */
public class DelegationEditorPanel extends AssignmentEditorPanel {
    private static final String ID_DELEGATION_VALID_FROM = "delegationValidFrom";
    private static final String ID_DELEGATION_VALID_TO = "delegationValidTo";
    private static final String ID_ARROW_ICON = "arrowIcon";
    private static final String ID_DELEGATED_TO_IMAGE = "delegatedToImage";
    private static final String ID_DELEGATED_TO = "delegatedTo";
    private static final String ID_DELEGATED_TO_LABEL = "delegatedToLabel";
    private static final String ID_SELECTED = "selected";
    private static final String ID_TYPE_IMAGE = "typeImage";
    private static final String ID_NAME_LABEL = "nameLabel";
    private static final String ID_NAME = "name";
    private static final String ID_HEADER_ROW = "headerRow";

    private boolean delegatedToMe;

    public DelegationEditorPanel(String id, IModel<AssignmentEditorDto> delegationTargetObjectModel,
                                 boolean delegatedToMe, PageBase pageBase) {
        super(id, delegationTargetObjectModel, pageBase);
//        this.delegatedToMe = delegatedToMe;
//        this.pageBase = pageBase;
//        super.initLayout();
    }

//    @Override
//    protected void initLayout() {

//        DateInput validFrom = new DateInput(ID_DELEGATION_VALID_FROM,
//                createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
//                        AssignmentEditorDto.F_ACTIVATION + ".validFrom")));
//        headerRow.add(validFrom);
//
//        DateInput validTo = new DateInput(ID_DELEGATION_VALID_TO,
//                createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
//                        AssignmentEditorDto.F_ACTIVATION + ".validTo")));
//        headerRow.add(validTo);
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
//                nameClickPerformed(target);
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
}
