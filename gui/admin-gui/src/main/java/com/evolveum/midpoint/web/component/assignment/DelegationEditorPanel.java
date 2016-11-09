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

import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

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

    public DelegationEditorPanel(String id, IModel<AssignmentEditorDto> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        Label arrowIcon = new Label(ID_ARROW_ICON);
        headerRow.add(arrowIcon);


        AssignmentEditorDto dto = getModelObject();
        dto.getTargetRef();
        WebMarkupContainer typeImage = new WebMarkupContainer(ID_DELEGATED_TO_IMAGE);
//        typeImage.add(AttributeModifier.append("class", createImageTypeModel(getModel())));
        headerRow.add(typeImage);

        AjaxLink delegatedToName = new AjaxLink(ID_DELEGATED_TO) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
//                delegatedToClickPerformed(target);
            }
        };
        headerRow.add(delegatedToName);

        Label delegatedToNameLabel = new Label(ID_DELEGATED_TO_LABEL, createTargetModel());
        delegatedToNameLabel.setOutputMarkupId(true);
        delegatedToName.add(delegatedToNameLabel);

        DateInput validFrom = new DateInput(ID_DELEGATION_VALID_FROM,
                createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validFrom")));
        headerRow.add(validFrom);

        DateInput validTo = new DateInput(ID_DELEGATION_VALID_TO,
                createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validTo")));
        headerRow.add(validTo);
    }

}
