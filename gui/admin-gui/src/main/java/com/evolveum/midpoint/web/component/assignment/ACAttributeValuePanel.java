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

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.PasswordPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.input.ThreeStateCheckPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;
import org.apache.commons.lang.ClassUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class ACAttributeValuePanel extends BasePanel<ACValueConstructionDto> {

    private static final String ID_INPUT = "input";
    private static final String ID_ADD = "add";
    private static final String ID_REMOVE = "remove";

    public ACAttributeValuePanel(String id, IModel<ACValueConstructionDto> iModel) {
        super(id, iModel);
    }

    @Override
    protected void initLayout() {
        ACValueConstructionDto dto = getModel().getObject();
        PrismPropertyDefinition definition = dto.getAttribute().getDefinition();

        InputPanel input = createTypedInputComponent(ID_INPUT, definition);
        add(input);

        AjaxLink add = new AjaxLink(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };
        add(add);

        AjaxLink remove = new AjaxLink(ID_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removePerformed(target);
            }
        };
        add(remove);
    }

    private InputPanel createTypedInputComponent(String id, PrismPropertyDefinition definition) {
        QName valueType = definition.getTypeName();

        final String baseExpression = ACValueConstructionDto.F_VALUE;

        InputPanel panel;
        if (DOMUtil.XSD_DATETIME.equals(valueType)) {
            panel = new DatePanel(id, new PropertyModel<XMLGregorianCalendar>(getModel(), baseExpression));
        } else if (ProtectedStringType.COMPLEX_TYPE.equals(valueType)) {
            panel = new PasswordPanel(id, new PropertyModel<String>(getModel(), baseExpression + ".clearValue"));
        } else if (DOMUtil.XSD_BOOLEAN.equals(valueType)) {
            panel = new ThreeStateCheckPanel(id, new PropertyModel<Boolean>(getModel(), baseExpression));
        } else if (SchemaConstants.T_POLY_STRING_TYPE.equals(valueType)) {
            panel = new TextPanel<String>(id, new PropertyModel<String>(getModel(), baseExpression + ".orig"), String.class);
        } else {
            Class type = XsdTypeMapper.getXsdToJavaMapping(valueType);
            if (type != null && type.isPrimitive()) {
                type = ClassUtils.primitiveToWrapper(type);
            }
            panel = new TextPanel<String>(id, new PropertyModel<String>(getModel(), baseExpression),
                    type);

            if (SchemaConstantsGenerated.C_NAME.equals(definition.getName())) {
                panel.getBaseFormComponent().setRequired(true);
            }
        }

        return panel;
    }

    private void addPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void removePerformed(AjaxRequestTarget target) {
        //todo implement
    }
}
