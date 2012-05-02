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

package com.evolveum.midpoint.web.component.prism.input;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class PasswordPanel extends InputPanel {

    public PasswordPanel(String id, IModel<String> model) {
        super(id);

        initLayout(model);
    }

    private void initLayout(IModel<String> model) {
        PasswordTextField password1 = new PasswordTextField("password1", model);
        add(password1);

        PasswordTextField password2 = new PasswordTextField("password2");
        add(password2);

        Form form = findParent(Form.class);
//        form.add(new EqualPasswordInputValidator(password1, password2)); todo what with this?
    }

    @Override
    public FormComponent getComponent() {
        return (FormComponent) get("password1");
    }
}
