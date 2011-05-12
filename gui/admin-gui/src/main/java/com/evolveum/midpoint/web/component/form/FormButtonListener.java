/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.component.form;

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

/**
 *
 * @author lazyman
 */
public class FormButtonListener extends AbstractStateHolder implements ActionListener {

    private ValueExpression bean;
    private FormButtonType type;
    private int attributeIndex;
    private int valueIndex;

    public FormButtonListener() {
    }

    public FormButtonListener(ValueExpression bean, FormButtonType type, int attributeIndex, int valueIndex) {
        this.bean = bean;
        this.attributeIndex = attributeIndex;
        this.valueIndex = valueIndex;
        this.type = type;
    }

    @Override
    public void processAction(ActionEvent event) throws AbortProcessingException {
        FormObject form = getBean();
        FormAttribute attribute = form.getAttributes().get(attributeIndex);
        switch (type) {
            case ADD:
                attribute.addValue(event);
                break;
            case DELETE:
                attribute.removeValue(valueIndex);
                break;
            case HELP:
//                showHelpPopup(attribute.getDefinition().getDescription());
                break;
        }
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] object = new Object[5];
        object[0] = super.saveState(context);
        object[1] = attributeIndex;
        object[2] = valueIndex;
        object[3] = type;
        object[4] = bean;

        return object;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] object = (Object[]) state;
        super.restoreState(context, object[0]);
        attributeIndex = (Integer) object[1];
        valueIndex = (Integer) object[2];
        type = (FormButtonType) object[3];
        bean = (ValueExpression) object[4];
    }

    private FormObject getBean() {
        if (bean == null) {
            return null;
        }

        return (FormObject) bean.getValue(FacesContext.getCurrentInstance().getELContext());
    }
}
