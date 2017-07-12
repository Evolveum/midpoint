/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.input.TriStateComboPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.ClassUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.List;

/**
 * @author lazyman
 */
public class ACAttributeValuePanel extends BasePanel<ACValueConstructionDto> {

    private static final String ID_INPUT = "input";
    private static final String ID_ADD = "add";
    private static final String ID_REMOVE = "remove";

    public ACAttributeValuePanel(String id, IModel<ACValueConstructionDto> iModel,
                                 boolean ignoreMandatoryAttributes, Form form) {
        super(id, iModel);

        initLayout(form, ignoreMandatoryAttributes);
    }

    private void initLayout(Form form, boolean ignoreMandatoryAttributes) {
        ACValueConstructionDto dto = getModel().getObject();
        PrismPropertyDefinition definition = dto.getAttribute().getDefinition();

        InputPanel input = createTypedInputComponent(ID_INPUT, definition);
        for (FormComponent comp : input.getFormComponents()) {
            comp.setLabel(new PropertyModel(dto.getAttribute(), ACAttributeDto.F_NAME));
            if (!ignoreMandatoryAttributes){
                comp.setRequired(definition.getMinOccurs() > 0);
            }

            comp.add(new AjaxFormComponentUpdatingBehavior("blur") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {}
            });
        }

        add(input);

        AjaxLink addLink = new AjaxLink(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };
        add(addLink);
        addLink.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isAddVisible();
            }
        });

        AjaxLink removeLink = new AjaxLink(ID_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removePerformed(target);
            }
        };
        add(removeLink);
        removeLink.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isRemoveVisible();
            }
        });
    }

    private InputPanel createTypedInputComponent(String id, PrismPropertyDefinition definition) {
        QName valueType = definition.getTypeName();

        final String baseExpression = ACValueConstructionDto.F_VALUE;

        InputPanel panel;
        if (DOMUtil.XSD_DATETIME.equals(valueType)) {
            panel = new DatePanel(id, new PropertyModel<XMLGregorianCalendar>(getModel(), baseExpression));
        } else if (ProtectedStringType.COMPLEX_TYPE.equals(valueType)) {
            panel = new PasswordPanel(id, new PropertyModel<ProtectedStringType>(getModel(), baseExpression));
        } else if (DOMUtil.XSD_BOOLEAN.equals(valueType)) {
            panel = new TriStateComboPanel(id, new PropertyModel<Boolean>(getModel(), baseExpression));
        } else if (SchemaConstants.T_POLY_STRING_TYPE.equals(valueType)) {
            panel = new TextPanel<String>(id, new PropertyModel<String>(getModel(), baseExpression + ".orig"), String.class);
        } else {
            Class type = XsdTypeMapper.getXsdToJavaMapping(valueType);
            if (type != null && type.isPrimitive()) {
                type = ClassUtils.primitiveToWrapper(type);
            }
            panel = new TextPanel<String>(id, new PropertyModel<String>(getModel(), baseExpression),
                    type);

            if (ObjectType.F_NAME.equals(definition.getName())) {
                panel.getBaseFormComponent().setRequired(true);
            }
        }

        return panel;
    }

    private boolean isAddVisible() {
        ACValueConstructionDto dto = getModel().getObject();
        ACAttributeDto attributeDto = dto.getAttribute();
        PrismPropertyDefinition def = attributeDto.getDefinition();

        List<ACValueConstructionDto> values = attributeDto.getValues();
        if (def.getMaxOccurs() != -1 && values.size() >= def.getMaxOccurs()) {
            return false;
        }

        //we want to show add on last item only
        if (values.indexOf(dto) + 1 != values.size()) {
            return false;
        }

        return true;
    }

    private boolean isRemoveVisible() {
        ACValueConstructionDto dto = getModel().getObject();
        ACAttributeDto attributeDto = dto.getAttribute();
        PrismPropertyDefinition def = attributeDto.getDefinition();

        List<ACValueConstructionDto> values = attributeDto.getValues();
        if (values.size() <= 1) {
            return false;
        }

        if (values.size() <= def.getMinOccurs()) {
            return false;
        }

        return true;
    }

    private void addPerformed(AjaxRequestTarget target) {
        ACValueConstructionDto dto = getModel().getObject();
        ACAttributeDto attributeDto = dto.getAttribute();
        attributeDto.getValues().add(new ACValueConstructionDto(attributeDto, null));

        target.add(findParent(ACAttributePanel.class).getParent());

        //todo implement add to account construction
    }

    private void removePerformed(AjaxRequestTarget target) {
        ACValueConstructionDto dto = getModel().getObject();
        ACAttributeDto attributeDto = dto.getAttribute();
        attributeDto.getValues().remove(dto);
        //todo implement remove from acctount construction

        target.add(findParent(ACAttributePanel.class).getParent());
    }
}
