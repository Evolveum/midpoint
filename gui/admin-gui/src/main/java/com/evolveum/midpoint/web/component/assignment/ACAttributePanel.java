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

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.util.BasePanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class ACAttributePanel extends BasePanel<ACAttributeDto> {

    private static final String ID_ATTRIBUTE_LABEL = "attributeLabel";
    private static final String ID_ATTRIBUTE_VALUE = "attributeValue";
    private static final String ID_SHOW_EXPR_EDITOR = "showExprEditor";

    public ACAttributePanel(String id, IModel<ACAttributeDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        Label attributeLabel = new Label(ID_ATTRIBUTE_LABEL, new PropertyModel(getModel(), ACAttributeDto.F_NAME));
        add(attributeLabel);

        TextField attributeValue = new TextField(ID_ATTRIBUTE_VALUE, new PropertyModel(getModel(), ACAttributeDto.F_VALUE));
        add(attributeValue);

        AjaxLink showExprEditor = new AjaxLink(ID_SHOW_EXPR_EDITOR) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showExprEditorPerformed(target);
            }
        };
        add(showExprEditor);
    }

    private void showExprEditorPerformed(AjaxRequestTarget target) {
        //todo implement
    }
}
